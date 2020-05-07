/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.pagecomposer.jaxrs.services.repositorytests.fullrequestcycle;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import javax.servlet.ServletException;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.pagecomposer.jaxrs.AbstractPageComposerTest;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerRepresentation;
import org.hippoecm.repository.HippoStdPubWfNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.Test;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.onehippo.repository.testutils.RepositoryTestCase;
import org.springframework.mock.web.MockHttpServletResponse;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static javax.ws.rs.core.Response.Status.CREATED;
import static org.hippoecm.repository.util.JcrUtils.getStringProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class XPageContainerComponentResourceTest extends AbstractXPageComponentResourceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void modifying_live_or_draft_variants_not_allowed() throws Exception {
        final String mountId = getNodeId("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

        final String catalogId = getNodeId("/hst:hst/hst:configurations/hst:default/hst:catalog/testpackage/testitem");

        final String containerId = getNodeId(publishedExpPageVariant.getPath() + "/hst:page/body/container");

        failCreateAssertions(mountId, catalogId, containerId);


        final Session admin = createSession(ADMIN_CREDENTIALS);
        DocumentWorkflow documentWorkflow = getDocumentWorkflow(admin);
        documentWorkflow.obtainEditableInstance();

        final Node draft = getVariant(handle, "draft");

        final String containerIdDraft = getNodeId(draft.getPath() + "/hst:page/body/container");

        failCreateAssertions(mountId, containerIdDraft, containerId);
    }

    private void failCreateAssertions(final String mountId, final String catalogId, final String containerId) throws IOException, ServletException {
        final RequestResponseMock createRequestResponse = mockGetRequestResponse(
                "http", "localhost", "/_rp/" + containerId + "./" + catalogId, null,
                "POST");

        final MockHttpServletResponse createResponse = render(mountId, createRequestResponse, ADMIN_CREDENTIALS);
        final Map<String, String> createResponseMap = mapper.readerFor(Map.class).readValue(createResponse.getContentAsString());

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), createResponse.getStatus());
        assertEquals(false, createResponseMap.get("success"));
        assertTrue(createResponseMap.get("message").contains("Does not below to unpublished variant of Experience Page."));
    }

    @Test
    public void create_and_delete_container_item_as_admin() throws Exception {

        createAndDeleteItemAs(ADMIN_CREDENTIALS, true);

    }

    @Test
    public void create_and_delete_container_item_as_editor() throws Exception {

        createAndDeleteItemAs(EDITOR_CREDENTIALS, true);

    }


    /**
     * Note an author cannot modify hst config pages but *CAN* modify experience pages if the cms user as role author
     */
    @Test
    public void create_and_delete_container_item_as_author() throws Exception {

        // author is not allowed to do a GET on ContainerItemComponentResource.getVariant()
        createAndDeleteItemAs(AUTHOR_CREDENTIALS, true);
    }

    /**
     * Note an author who does not have role hippo:author on the experience page is not allowed to modify the hst:page
     * in the experience page document
     */
    @Test
    public void create_container_item_NOT_allowed_it_not_role_author() throws Exception {
        // for author user, temporarily remove the role 'hippo:author' : Without this role, (s)he should not be allowed
        // to invoked the XPageContainerComponentResource

        final Session admin = createSession(ADMIN_CREDENTIALS);
        Property privilegesProp = admin.getNode("/hippo:configuration/hippo:roles/author").getProperty("hipposys:privileges");
        Value[] before = privilegesProp.getValues();
        privilegesProp.remove();
        admin.save();

        try {
            // since author does not have privilege hippo:author anymore, expect a FORBIDDEN
            createAndDeleteItemAs(AUTHOR_CREDENTIALS, false);
        } finally {
            // restore privileges
            admin.getNode("/hippo:configuration/hippo:roles/author").setProperty("hipposys:privileges", before);
            admin.save();
        }
    }

    /**
     * The 'creds' are used to invoke the HST pipeline, the adminSession is used to invoke workflow from here (like
     * publish)
     */
    private void createAndDeleteItemAs(final SimpleCredentials creds, final boolean allowed)
            throws IOException, ServletException, RepositoryException, WorkflowException {

        final Session adminSession = createSession(ADMIN_CREDENTIALS);

        try {
            final String mountId = getNodeId(adminSession,"/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

            final String containerId = getNodeId(adminSession,unpublishedExpPageVariant.getPath() + "/hst:page/body/container");
            final String catalogId = getNodeId(adminSession,"/hst:hst/hst:configurations/hst:default/hst:catalog/testpackage/testitem");

            final DocumentWorkflow documentWorkflow = getDocumentWorkflow(adminSession);
            // since document got published and nothing yet changed, should not be published

            assertEquals("No changes yet in unpublished, hence not expected publication option",
                    FALSE, documentWorkflow.hints().get("publish"));

            final RequestResponseMock createRequestResponse = mockGetRequestResponse(
                    "http", "localhost", "/_rp/" + containerId + "./" + catalogId, null,
                    "POST");


            final MockHttpServletResponse createResponse = render(mountId, createRequestResponse, creds);
            final Map<String, String> createResponseMap = mapper.readerFor(Map.class).readValue(createResponse.getContentAsString());

            if (!allowed) {
                assertEquals("FORBIDDEN", createResponseMap.get("errorCode"));
                return;
            }


            assertEquals(CREATED.getStatusCode(), createResponse.getStatus());

            // assert modifying the preview did not create a draft variant!!! changes are directly on unpublished
            assertNull(getVariant(handle, "draft"));

            final String createdUUID = createResponseMap.get("id");

            // assertion on newly created item
            assertTrue(adminSession.nodeExists(unpublishedExpPageVariant.getPath() + "/hst:page/body/container/testitem"));
            assertTrue(adminSession.getNodeByIdentifier(createdUUID) != null);

            // assert document can now be published
            assertEquals("Unpublished has changes, publication should be enabled",
                    TRUE, documentWorkflow.hints().get("publish"));

            assertEquals("Expected unpublished to have been last modified by current user", creds.getUserID(),
                    unpublishedExpPageVariant.getProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_BY).getString());

            // now publish the document,
            documentWorkflow.publish();
            // assert that published variant now has extra container item 'testitem'
            assertTrue(adminSession.nodeExists(publishedExpPageVariant.getPath() + "/hst:page/body/container/testitem"));


            // now delete
            final RequestResponseMock deleteRequestResponse = mockGetRequestResponse(
                    "http", "localhost", "/_rp/" + containerId + "./" + createdUUID, null,
                    "DELETE");
            final MockHttpServletResponse deleteResponse = render(mountId, deleteRequestResponse, creds);
            assertEquals(Response.Status.OK.getStatusCode(), deleteResponse.getStatus());

            try {
                adminSession.getNodeByIdentifier(createdUUID);
                fail("Item expected to have been deleted again");
            } catch (ItemNotFoundException e) {
                // expected
            }

            // published variant still has the container item
            assertTrue(adminSession.nodeExists(publishedExpPageVariant.getPath() + "/hst:page/body/container/testitem"));

            // after delete, the unpublished has become publishable again
            assertEquals("Unpublished has changes, publication should be enabled",
                    TRUE, documentWorkflow.hints().get("publish"));

            documentWorkflow.publish();

            // published variant should not have the container item any more
            assertFalse(adminSession.nodeExists(publishedExpPageVariant.getPath() + "/hst:page/body/container/testitem"));

            // now assert an existing component item (or catalog) not from the current XPAGE cannot be deleted via
            // the container id

            final RequestResponseMock deleteRequestResponseInvalid = mockGetRequestResponse(
                    "http", "localhost", "/_rp/" + containerId + "./" + catalogId, null,
                    "DELETE");
            final MockHttpServletResponse invalidResponse = render(mountId, deleteRequestResponseInvalid, creds);
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), invalidResponse.getStatus());

        } finally {
            adminSession.logout();
        }

    }


    @Test
    public void create_item_before() throws Exception {

        final Session adminSession = createSession(ADMIN_CREDENTIALS);

        try {
            final String mountId = getNodeId(adminSession, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

            final String containerId = getNodeId(adminSession, unpublishedExpPageVariant.getPath() + "/hst:page/body/container");
            final String beforeItemId = getNodeId(adminSession, unpublishedExpPageVariant.getPath() + "/hst:page/body/container/banner");
            final String catalogId = getNodeId(adminSession, "/hst:hst/hst:configurations/hst:default/hst:catalog/testpackage/testitem");

            final RequestResponseMock createRequestResponse = mockGetRequestResponse(
                    "http", "localhost", "/_rp/" + containerId + "./" + catalogId + "/" + beforeItemId, null,
                    "POST");

            final MockHttpServletResponse createResponse = render(mountId, createRequestResponse, ADMIN_CREDENTIALS);

            assertEquals(CREATED.getStatusCode(), createResponse.getStatus());

            final Node container = adminSession.getNodeByIdentifier(containerId);

            final NodeIterator nodes = container.getNodes();
            assertEquals("Expected testitem to be created before banner", "testitem", nodes.nextNode().getName());
            assertEquals("Expected testitem to be created before banner", "banner", nodes.nextNode().getName());

        } finally {
            adminSession.logout();
        }
    }

    @Test
    public void create_item_before_non_existing_item_results_in_error() throws Exception {

        final String beforeItemId = UUID.randomUUID().toString();
        final String expectedMessage = String.format("Cannot find container item '%s'", beforeItemId);
        notAllowedcreateBeforeItem(beforeItemId, expectedMessage);
    }

    /**
     * the 'before item' is an item of another experience page
     * @throws Exception
     */
    @Test
    public void create_item_before_item_of_other_container_results_in_error() throws Exception {

        final String beforeItemId = getNodeId( EXPERIENCE_PAGE_2_HANDLE_PATH + "/expPage2/hst:page/body/container/banner");
        final String expectedMessage = String.format(String.format("Order before container item '%s' is of other experience page", beforeItemId));
        notAllowedcreateBeforeItem(beforeItemId, expectedMessage);
    }

    @Test
    public void create_item_before_node_not_of_type_container_item_results_in_error() throws Exception {
        final String beforeItemId = unpublishedExpPageVariant.getIdentifier();
        final String expectedMessage = String.format(String.format("The container item '%s' does not have the correct type", beforeItemId));
        notAllowedcreateBeforeItem(beforeItemId, expectedMessage);
    }


    private void notAllowedcreateBeforeItem(final String beforeItemId, final String expectedMessage) throws RepositoryException, IOException, ServletException {
        final String mountId = getNodeId("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

        final String containerId = getNodeId(unpublishedExpPageVariant.getPath() + "/hst:page/body/container");

        final String catalogId = getNodeId("/hst:hst/hst:configurations/hst:default/hst:catalog/testpackage/testitem");


        final RequestResponseMock createRequestResponse = mockGetRequestResponse(
                "http", "localhost", "/_rp/" + containerId + "./" + catalogId + "/" + beforeItemId, null,
                "POST");

        final MockHttpServletResponse createResponse = render(mountId, createRequestResponse, ADMIN_CREDENTIALS);

        final Map<String, String> createResponseMap = mapper.readerFor(Map.class).readValue(createResponse.getContentAsString());

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), createResponse.getStatus());
        assertEquals(false, createResponseMap.get("success"));

        assertEquals(expectedMessage , createResponseMap.get("message"));
    }


    @Test
    public void move_container_item_within_container() throws Exception {
        final Session session = createSession(ADMIN_CREDENTIALS);

        try {

            JcrUtils.copy(session, unpublishedExpPageVariant.getPath() + "/hst:page/body/container/banner",
                    unpublishedExpPageVariant.getPath() + "/hst:page/body/container/banner2");
            session.save();

            final String mountId = getNodeId(session, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

            final String containerId = getNodeId(session, unpublishedExpPageVariant.getPath() + "/hst:page/body/container");
            final String itemId1 = getNodeId(session, unpublishedExpPageVariant.getPath() + "/hst:page/body/container/banner");
            final String itemId2 = getNodeId(session, unpublishedExpPageVariant.getPath() + "/hst:page/body/container/banner2");

            final RequestResponseMock updateRequestResponse = mockGetRequestResponse(
                    "http", "localhost", "/_rp/" + containerId, null,
                    "PUT");

            final ContainerRepresentation containerRepresentation = new ContainerRepresentation();

            containerRepresentation.setId(containerId);
            // move item2 before item1
            containerRepresentation.setChildren(Stream.of(itemId2, itemId1).collect(Collectors.toList()));

            updateRequestResponse.getRequest().setContent(objectMapper.writeValueAsBytes(containerRepresentation));
            updateRequestResponse.getRequest().setContentType("application/json;charset=UTF-8");

            final MockHttpServletResponse updateResponse = render(mountId, updateRequestResponse, ADMIN_CREDENTIALS);

            assertEquals(Response.Status.OK.getStatusCode(), updateResponse.getStatus());

            // assert container items have been flipped
            final Node container = session.getNodeByIdentifier(containerId);

            final NodeIterator children = container.getNodes();

            assertEquals("banner2", children.nextNode().getName());
            assertEquals("banner", children.nextNode().getName());

            // assert 'container node' is not locked
            assertNull("Container nodes for experience pages should never get locked",
                    getStringProperty(container, HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY, null));

            final DocumentWorkflow documentWorkflow = getDocumentWorkflow(session);

            assertEquals("Unpublished has changes, publication should be enabled",
                    TRUE, documentWorkflow.hints().get("publish"));

        } finally {
            session.logout();
        }
    }

    @Test
    public void move_container_item_between_container_of_same_XPage() throws Exception {
        final Session session = createSession(ADMIN_CREDENTIALS);

        try {
             // first create a second container
            JcrUtils.copy(session, unpublishedExpPageVariant.getPath() + "/hst:page/body/container",
                    unpublishedExpPageVariant.getPath() + "/hst:page/body/container2");
            session.save();

            final String mountId = getNodeId(session, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

            final String targetContainerId = getNodeId(session, unpublishedExpPageVariant.getPath() + "/hst:page/body/container2");
            final String itemId = getNodeId(session, unpublishedExpPageVariant.getPath() + "/hst:page/body/container/banner");

            final RequestResponseMock updateRequestResponse = mockGetRequestResponse(
                    "http", "localhost", "/_rp/" + targetContainerId, null,
                    "PUT");

            final ContainerRepresentation containerRepresentation = new ContainerRepresentation();

            containerRepresentation.setId(targetContainerId);
            // move item to other container
            containerRepresentation.setChildren(Stream.of(itemId).collect(Collectors.toList()));

            updateRequestResponse.getRequest().setContent(objectMapper.writeValueAsBytes(containerRepresentation));
            updateRequestResponse.getRequest().setContentType("application/json;charset=UTF-8");

            final MockHttpServletResponse updateResponse = render(mountId, updateRequestResponse, ADMIN_CREDENTIALS);

            assertEquals(Response.Status.OK.getStatusCode(), updateResponse.getStatus());

            // assert  second container now has the item
            final Node targetContainer = session.getNodeByIdentifier(targetContainerId);

            final NodeIterator children = targetContainer.getNodes();
            // the container already got an item 'banner'
            assertEquals("banner", children.nextNode().getName());
            // the extra item moved into it should have a postfix to its name
            assertEquals("Expected postfix '1' to banner moved to container to avoid same name",
                    "banner1", children.nextNode().getName());

            final Node sourceContainer = session.getNode(unpublishedExpPageVariant.getPath() + "/hst:page/body/container");
            assertEquals(0l, sourceContainer.getNodes().getSize());

            final DocumentWorkflow documentWorkflow = getDocumentWorkflow(session);

            assertEquals("Unpublished has changes, publication should be enabled",
                    TRUE, documentWorkflow.hints().get("publish"));

        } finally {
            session.logout();
        }
    }


    @Test
    public void move_container_item_between_container_of_different_XPages_is_now_allowed() throws Exception {
        final Session session = createSession(ADMIN_CREDENTIALS);
        try {
            final String mountId = getNodeId(session, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

            // Container of a different XPage than itemId
            final String targetContainerId = getNodeId(session, "/unittestcontent/documents/unittestproject/experiences/expPage2/expPage2/hst:page/body/container");
            final String itemId = getNodeId(session, unpublishedExpPageVariant.getPath() + "/hst:page/body/container/banner");

            final RequestResponseMock updateRequestResponse = mockGetRequestResponse(
                    "http", "localhost", "/_rp/" + targetContainerId, null,
                    "PUT");

            final ContainerRepresentation containerRepresentation = new ContainerRepresentation();

            containerRepresentation.setId(targetContainerId);
            // move item to other container
            containerRepresentation.setChildren(Stream.of(itemId).collect(Collectors.toList()));

            updateRequestResponse.getRequest().setContent(objectMapper.writeValueAsBytes(containerRepresentation));
            updateRequestResponse.getRequest().setContentType("application/json;charset=UTF-8");

            final MockHttpServletResponse updateResponse = render(mountId, updateRequestResponse, ADMIN_CREDENTIALS);

            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), updateResponse.getStatus());

            final DocumentWorkflow documentWorkflow = getDocumentWorkflow(session);

            assertEquals("Unpublished should not have changes",
                    FALSE, documentWorkflow.hints().get("publish"));
        } finally {
            session.logout();
        }
    }


    /**
     * <p>
     *     It is not allowed to move a container item from HST Configuration to an XPage: A document has a different life
     *     cycle than HST configuration, thus if we would support such a move, we'd get problems if either the XPage or
     *     HST Config gets published
     * </p>
     * <p>
     *     This tests covers the move of an an HST Config container item to XPage container. The reverse test is covered in
     *     {@link ContainerComponentResourceTest}
     * </p>
     */
    @Test
    public void move_container_item_from_hst_config_to_XPage_is_not_allowed() throws Exception {

        // FIRST add a container item to HST CONFIG
        Session session = backupHstAndCreateWorkspace();

        try {
            // create a container and container item  in workspace
            String[] content = new String[]{
                    "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages/containertestpage", "hst:component",
                    "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages/containertestpage/main", "hst:component",
                    "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages/containertestpage/main/container", "hst:containercomponent",
                      "hst:xtype", "hst.vbox",
                    "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages/containertestpage/main/container/banner", "hst:containeritemcomponent",
                      "hst:componentclassname", "org.hippoecm.hst.pagecomposer.jaxrs.services.repositorytests.fullrequestcycle.BannerComponent",
            };

            RepositoryTestCase.build(content, session);

            session.save();

            final String mountId = getNodeId(session, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

            final String targetContainerId = getNodeId(session,unpublishedExpPageVariant.getPath() + "/hst:page/body/container");

            final String itemFromHstConfig = getNodeId(session,"/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages/containertestpage/main/container/banner");

            final RequestResponseMock updateContainerReqRes = mockGetRequestResponse(
                    "http", "localhost", "/_rp/" + targetContainerId, null, "PUT");

            final ContainerRepresentation containerRepresentation = new ContainerRepresentation();

            containerRepresentation.setId(targetContainerId);
            // try to move itemFromXPage to other container
            containerRepresentation.setChildren(Stream.of(itemFromHstConfig).collect(Collectors.toList()));

            updateContainerReqRes.getRequest().setContent(objectMapper.writeValueAsBytes(containerRepresentation));
            updateContainerReqRes.getRequest().setContentType("application/json;charset=UTF-8");

            final MockHttpServletResponse updateResponse = render(mountId, updateContainerReqRes, ADMIN_CREDENTIALS);

            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), updateResponse.getStatus());

        } finally {
            AbstractPageComposerTest.restoreHstConfigBackup(session);
            session.logout();
        }
    }

    @Test
    public void move_container_item_which_does_not_exist_is_bad_request() throws Exception {

        final String mountId = getNodeId("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

        final String containerId = getNodeId(unpublishedExpPageVariant.getPath() + "/hst:page/body/container");

        final RequestResponseMock updateRequestResponse = mockGetRequestResponse(
                "http", "localhost", "/_rp/" + containerId, null,
                "PUT");

        final ContainerRepresentation containerRepresentation = new ContainerRepresentation();

        containerRepresentation.setId(containerId);
        // move non existing item
        containerRepresentation.setChildren(Stream.of(UUID.randomUUID().toString()).collect(Collectors.toList()));

        updateRequestResponse.getRequest().setContent(objectMapper.writeValueAsBytes(containerRepresentation));
        updateRequestResponse.getRequest().setContentType("application/json;charset=UTF-8");

        final MockHttpServletResponse updateResponse = render(mountId, updateRequestResponse, ADMIN_CREDENTIALS);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), updateResponse.getStatus());
    }

    @Test
    public void move_invalid_container_item_id_is_bad_request() throws Exception {

        final String mountId = getNodeId("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

        final String containerId = getNodeId(unpublishedExpPageVariant.getPath() + "/hst:page/body/container");

        final RequestResponseMock updateRequestResponse = mockGetRequestResponse(
                "http", "localhost", "/_rp/" + containerId, null,
                "PUT");

        final ContainerRepresentation containerRepresentation = new ContainerRepresentation();

        containerRepresentation.setId(containerId);
        // move non existing item
        containerRepresentation.setChildren(Stream.of("invalid-UUID").collect(Collectors.toList()));

        updateRequestResponse.getRequest().setContent(objectMapper.writeValueAsBytes(containerRepresentation));
        updateRequestResponse.getRequest().setContentType("application/json;charset=UTF-8");

        final MockHttpServletResponse updateResponse = render(mountId, updateRequestResponse, ADMIN_CREDENTIALS);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), updateResponse.getStatus());
    }
}
