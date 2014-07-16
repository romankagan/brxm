/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.repository.upgrade;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;

import com.google.common.collect.Sets;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.reviewedactions.FullReviewedActionsWorkflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;
import org.onehippo.repository.util.JcrConstants;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

public class HardHandleUpdateVisitorTest extends RepositoryTestCase {

    private static int NO_OF_DOCS = 1;
    private static int NO_OF_VERSIONS = 4;

    private Node documents;
    private Node attic;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        documents = session.getNode("/content/documents");
        attic = session.getNode("/content/attic");
        createTestDocuments(NO_OF_DOCS);
        session.save();
    }

    @Override
    public void tearDown() throws Exception {
        removeDocuments();
        clearAttic();
        super.tearDown();
    }

    @Test
    public void testPublishedHandleMigration() throws Exception {
        editAndPublishTestDocuments();
        migrate();
        checkDocumentHistory();
        checkAvailability(Sets.newHashSet("live", "preview"));
    }

    @Test
    public void testChangedHandleMigration() throws Exception {
        editAndPublishTestDocuments();
        editDocuments();
        migrate();
        checkDocumentHistory();
        checkAvailability(Sets.newHashSet("live", "preview"));
    }

    @Test
    public void testDepublishedHandleMigration() throws Exception {
        editAndPublishTestDocuments();
        depublishDocuments();
        migrate();
        checkAvailability(Sets.newHashSet("preview"));
    }

    @Test
    public void testDeletedHandleMigration() throws Exception {
        editAndPublishTestDocuments();
        deleteDocuments();
        migrate();
        checkAtticDocumentHistory();
    }

    @Test
    public void testNeverPublishedHandleMigration() throws Exception {
        migrate();
        checkUnpublished();
    }

    private void deleteDocuments() throws Exception {
        for (Node handle : new NodeIterable(documents.getNodes())) {
            if (handle.isNodeType(HippoNodeType.NT_HANDLE)) {
                final Node document = handle.getNode(handle.getName());
                deleteTestDocument(document);
            }
        }
    }

    private void deleteTestDocument(final Node document) throws Exception {
        final FullReviewedActionsWorkflow workflow = getFullReviewedActionsWorkflow(document);
        workflow.depublish();
        workflow.delete();
    }

    private void checkAtticDocumentHistory() throws RepositoryException {
        for (Node handle : getAtticHandles()) {
            checkAtticHandle(handle);
        }
    }

    private List<Node> getAtticHandles() throws RepositoryException {
        final List<Node> handles = new ArrayList<Node>();
        attic.accept(new ItemVisitor() {
            @Override
            public void visit(final Property property) throws RepositoryException {
            }

            @Override
            public void visit(final Node node) throws RepositoryException {
                if (JcrUtils.isVirtual(node)) {
                    return;
                }
                if (node.isNodeType(HippoNodeType.NT_HARDHANDLE)) {
                    handles.add(node);
                    return;
                }
                for (Node child : new NodeIterable(node.getNodes())) {
                    visit(child);
                }
            }
        });
        return handles;
    }

    private void checkAtticHandle(final Node handle) throws RepositoryException {
        assertTrue("No hippo:deleted node under attic handle", handle.hasNode(handle.getName()));
        final Node deleted = handle.getNode(handle.getName());
        assertTrue(deleted.isNodeType(HippoNodeType.NT_DELETED));
        final VersionManager versionManager = session.getWorkspace().getVersionManager();
        final String documentPath = deleted.getPath();
        final VersionHistory versionHistory = versionManager.getVersionHistory(documentPath);
        final VersionIterator versions = versionHistory.getAllVersions();
        assertEquals("Unexpected number of versions", NO_OF_VERSIONS+2, versions.getSize());
    }

    private void migrate() throws RepositoryException {
        final HardHandleUpdateVisitor handleMigrator = new HardHandleUpdateVisitor();
        handleMigrator.initialize(session);
        handleMigrator.setLogger(log);
        for (Node handle : new NodeIterable(documents.getNodes())) {
            if (handle.isNodeType(HippoNodeType.NT_HANDLE)) {
                handleMigrator.doUpdate(handle);
            }
        }
        for (Node handle : getAtticHandles()) {
            handleMigrator.doUpdate(handle);
        }

    }

    private void checkDocumentHistory() throws Exception {
        for (int i = 0; i < NO_OF_DOCS; i++) {
            checkDocumentHistory(getUnpublished(i));
        }
    }

    private void checkDocumentHistory(Node document) throws Exception {
        checkUnpublished(document);
        final VersionManager versionManager = session.getWorkspace().getVersionManager();
        final String documentPath = document.getPath();
        final String documentIdentifier = document.getIdentifier();
        final VersionHistory versionHistory = versionManager.getVersionHistory(documentPath);
        final VersionIterator versions = versionHistory.getAllVersions();
        assertEquals("Unexpected number of versions", NO_OF_VERSIONS+1, versions.getSize());
        versionManager.restore(documentPath, "1.2", true);
        document = session.getNodeByIdentifier(documentIdentifier);
        assertEquals("Unexpected property value", "bar2", JcrUtils.getStringProperty(document, "foo", null));
        assertFalse("Preview still has harddocument mixin", document.isNodeType(HippoNodeType.NT_HARDDOCUMENT));
    }

    private void checkUnpublished() throws Exception {
        for (int i = 0; i < NO_OF_DOCS; i++) {
            checkUnpublished(getUnpublished(i));
        }
    }

    private void checkUnpublished(final Node document) throws RepositoryException {
        assertNotNull("No unpublished available", document);
        assertFalse("Unpublished still has harddocument mixin", document.isNodeType(HippoNodeType.NT_HARDDOCUMENT));
        assertTrue("Document is not versionable", document.isNodeType(JcrConstants.MIX_VERSIONABLE));
    }

    private void checkAvailability(Set<String> expected) throws Exception {
        for (int i = 0; i < NO_OF_DOCS; i++) {
            checkAvailability(getUnpublished(i), expected);
        }
    }

    private void checkAvailability(Node document, Set<String> expected) throws RepositoryException {
        Node handle = document.getParent();
        Set<String> availabilities = new HashSet<>();
        for (Node child : new NodeIterable(handle.getNodes(handle.getName()))) {
            assertTrue(child.hasProperty(HippoNodeType.HIPPO_AVAILABILITY));
            Value[] values = child.getProperty(HippoNodeType.HIPPO_AVAILABILITY).getValues();
            for (Value value : values) {
                availabilities.add(value.getString());
            }
        }
        assertEquals(expected, availabilities);
    }

    private void editAndPublishTestDocuments() throws Exception {
        for (Node handle : new NodeIterable(documents.getNodes())) {
            if (!handle.isNodeType(HippoNodeType.NT_HANDLE)) {
                continue;
            }
            for (int i = 0; i < NO_OF_VERSIONS; i++) {
                final Node document = handle.getNode(handle.getName());
                publishTestDocument(editTestDocument(document, i));
            }
        }
    }

    private void editDocuments() throws Exception {
        for (Node handle : new NodeIterable(documents.getNodes())) {
            if (!handle.isNodeType(HippoNodeType.NT_HANDLE)) {
                continue;
            }
            for (int i = 0; i < NO_OF_VERSIONS; i++) {
                final Node document = handle.getNode(handle.getName());
                editTestDocument(document, i);
            }
        }
    }

    private void depublishDocuments() throws Exception {
        for (Node handle : new NodeIterable(documents.getNodes())) {
            if (!handle.isNodeType(HippoNodeType.NT_HANDLE)) {
                continue;
            }
            getFullReviewedActionsWorkflow(handle.getNode(handle.getName())).depublish();
        }
    }

    private Node editTestDocument(final Node document, int i) throws Exception {
        final Node draft = getFullReviewedActionsWorkflow(document).obtainEditableInstance().getNode(session);
        draft.setProperty("foo", "bar" + i);
        draft.getSession().save();
        return getFullReviewedActionsWorkflow(draft).commitEditableInstance().getNode(session);
    }

    private void createTestDocuments(final int count) throws Exception {
        for (int i = 0; i < count; i++) {
            String path = createTestDocument(i);
            Node node = session.getNode(path).getParent();
            node.addMixin(HippoNodeType.NT_HARDHANDLE);
            node.addMixin("testcontent:mixin");
            node.addNode("testcontent:html");
        }
    }

    private void removeDocuments() throws RepositoryException {
        for (Node document : new NodeIterable(documents.getNodes())) {
            document.remove();
        }
        session.save();
    }

    private void clearAttic() throws RepositoryException {
        for (Node node : new NodeIterable(session.getNode("/content/attic").getNodes())) {
            node.remove();
        }
        session.save();
    }

    private void publishTestDocument(final Node document) throws Exception {
        getFullReviewedActionsWorkflow(document).publish();
    }

    private String createTestDocument(int index) throws Exception {
        return getFolderWorkflow(documents).add("legacy-document", "testcontent:news", "document" + index);
    }

    private Node getUnpublished(final int index) throws RepositoryException {
        final NodeIterator documents = session.getNode("/content/documents/document" + index).getNodes("document" + index);
        while (documents.hasNext()) {
            final Node variant = documents.nextNode();
            if (HippoStdNodeType.UNPUBLISHED.equals(JcrUtils.getStringProperty(variant, HippoStdNodeType.HIPPOSTD_STATE, null))) {
                return variant;
            }
        }
        return null;
    }

    private FullReviewedActionsWorkflow getFullReviewedActionsWorkflow(final Node document) throws RepositoryException {
        return (FullReviewedActionsWorkflow) getWorkflow("deprecated", document);
    }

    private FolderWorkflow getFolderWorkflow(final Node folder) throws RepositoryException {
        return (FolderWorkflow) getWorkflow("internal", folder);
    }

    private Workflow getWorkflow(final String category, final Node node) throws RepositoryException {
        final WorkflowManager workflowManager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
        return workflowManager.getWorkflow(category, node);
    }

}
