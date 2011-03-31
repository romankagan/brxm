/*
 *  Copyright 2011 Hippo.
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
package org.hippoecm.repository;

import java.rmi.RemoteException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.hippoecm.repository.util.Utilities;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TriggerWorkflowTest extends TestCase {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private Node root;
    private WorkflowManager manager;

    private String[] content = {
        "/test/counter",                  "hippo:handle",
        "jcr:mixinTypes",                 "hippo:hardhandle",
        "/test/counter/counter",          "hippo:triggercounter",
        "jcr:mixinTypes",                 "hippo:harddocument",
        "hippo:triggercounter",           "1",

        "/test/folder",                   "hippostd:folder",
        "jcr:mixinTypes",                 "hippo:harddocument",
        "/test/folder/document",          "hippo:handle",
        "jcr:mixinTypes",                 "hippo:hardhandle",
        "/test/folder/document/document", "hippo:document",
        "jcr:mixinTypes",                 "hippo:hardhandle",
        "/test/target",                   "hippostd:folder",
        "jcr:mixinTypes",                 "hippo:harddocument",
        "/hippo:configuration/hippo:queries/hippo:templates/test", "hippostd:templatequery",
        "jcr:mixinTypes",                 "hipposys:implementation",
        "hipposys:classname",             "org.hippoecm.repository.impl.query.DirectPath",
        "hippostd:modify",                "./_name",
        "hippostd:modify",                "$name",
        "hippostd:modify",                "./_node/_name",
        "hippostd:modify",                "$name",
        "jcr:language",                   "xpath",
        "jcr:statement",                  "/jcr:root/hippo:configuration/hippo:queries/hippo:templates/test/hippostd:templates/node()",
        "/hippo:configuration/hippo:queries/hippo:templates/test/hippostd:templates", "hippostd:templates",
        "/hippo:configuration/hippo:queries/hippo:templates/test/hippostd:templates/prototype", "hippo:handle",
        "jcr:mixinTypes",                 "hippo:hardhandle",
        "/hippo:configuration/hippo:queries/hippo:templates/test/hippostd:templates/prototype/prototype", "hippo:triggerdocument",
        "jcr:mixinTypes",                 "hippo:harddocument",
	"hippo:triggercounter",           "0",

        "/hippo:configuration/hippo:workflows/postprocess", "hipposys:workflowcategory",
        "/hippo:configuration/hippo:workflows/postprocess/create", "hipposys:workflow",
        "hipposys:nodetype", "hippo:triggerdocument",
        "hipposys:display", "triggerdocument",
        "hipposys:classname", "org.hippoecm.repository.test.PostProcessWorkflowImpl",

        "/hippo:configuration/hippo:workflows/triggers", "hipposys:workflowcategory",
        "/hippo:configuration/hippo:workflows/triggers/test", "hipposys:workflowsimplequerytrigger",
        "hipposys:nodetype", "hippo:document",
        "hipposys:display", "triggertest",
        "hipposys:classname", "org.hippoecm.repository.test.TriggerWorkflowImpl",
        "hipposys:triggerconditionoperator", "post\\pre",
        "hipposys:triggerdocument", "/test/counter/counter",
        "/hippo:configuration/hippo:workflows/triggers/test/hipposys:triggerprecondition", "nt:query",
        "jcr:mixinTypes",                 "mix:referenceable",
        "jcr:language", "JCR-SQL2",
        "jcr:statement", "SELECT child.[jcr:uuid] AS id FROM [hippo:hardhandle] AS child INNER JOIN [hippo:document] AS parent ON ISCHILDNODE(child,parent) WHERE parent.[jcr:uuid] = $subject",
        "/hippo:configuration/hippo:workflows/triggers/test/hipposys:triggerpostcondition", "nt:query",
        "jcr:mixinTypes",                 "mix:referenceable",
        "jcr:language", "JCR-SQL2",
        "jcr:statement", "SELECT child.[jcr:uuid] AS id FROM [hippo:hardhandle] AS child INNER JOIN [hippo:document] AS parent ON ISCHILDNODE(child,parent) WHERE parent.[jcr:uuid] = $subject"
    };

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        root = session.getRootNode();
        while (root.hasNode("test")) {
            root.getNode("test").remove();
        }
        if (session.getRootNode().hasNode("hippo:configuration/hippo:queries/hippo:templates/test")) {
            session.getRootNode().getNode("hippo:configuration/hippo:queries/hippo:templates/test").remove();
        }
        root = root.addNode("test");
        session.save();
        build(session, content);
        session.save();
        manager = ((HippoWorkspace)session.getWorkspace()).getWorkflowManager();
        session.save();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        root = session.getRootNode();
        while (root.hasNode("test")) {
            root.getNode("test").remove();
        }
        if (session.getRootNode().hasNode("hippo:configuration/hippo:queries/hippo:templates/test")) {
            session.getRootNode().getNode("hippo:configuration/hippo:queries/hippo:templates/test").remove();
        }
        if (session.getRootNode().hasNode("hippo:configuration/hippo:workflows/triggers")) {
            session.getRootNode().getNode("hippo:configuration/hippo:workflows/triggers").remove();
        }
        if (session.getRootNode().hasNode("hippo:configuration/hippo:workflows/postprocess")) {
            session.getRootNode().getNode("hippo:configuration/hippo:workflows/postprocess").remove();
        }
        session.save();
        super.tearDown();
    }

    @Test
    public void testTriggerFire() throws RepositoryException, WorkflowException, RemoteException {
        Node folder = root.getNode("folder");
        assertEquals(1L, root.getProperty("counter/counter/hippo:triggercounter").getLong());
        {
            FolderWorkflow workflow = (FolderWorkflow)manager.getWorkflow("internal", folder);
            assertEquals(1L, root.getProperty("counter/counter/hippo:triggercounter").getLong());
            String path = workflow.add("test", "prototype", "new");
            Node node = session.getRootNode().getNode(path.substring(1));
            node = node.getNode(node.getName());
            assertTrue(node.hasProperty("hippo:triggercounter"));
            assertEquals(1L, node.getProperty("hippo:triggercounter").getLong());
        } {
            FolderWorkflow workflow = (FolderWorkflow)manager.getWorkflow("internal", folder);
            assertEquals(2L, root.getProperty("counter/counter/hippo:triggercounter").getLong());
            String path = workflow.add("test", "prototype", "new");
            Node node = session.getRootNode().getNode(path.substring(1));
            node = node.getNode(node.getName());
            assertTrue(node.hasProperty("hippo:triggercounter"));
            assertEquals(2L, node.getProperty("hippo:triggercounter").getLong());
        }
        assertEquals(3L, root.getProperty("counter/counter/hippo:triggercounter").getLong());
    }
}
