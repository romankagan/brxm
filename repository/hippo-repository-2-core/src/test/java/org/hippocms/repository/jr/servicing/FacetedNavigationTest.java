/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippocms.repository.jr.servicing;

import junit.framework.TestCase;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.Item;

import org.hippocms.repository.jr.embedded.HippoRepository;
import org.hippocms.repository.jr.embedded.HippoRepositoryFactory;

public class FacetedNavigationTest extends TestCase {
    private final static String SVN_ID = "$Id$";

    public void testPaths() throws Exception {
        Exception firstException = null;
        HippoRepository repository = null;
        Node root, node;
        Item item;
        try {
            repository = HippoRepositoryFactory.getHippoRepository();
            assertNotNull(repository);
            Session session = repository.login();

            // Setup
            root = session.getRootNode();
            node = root.addNode("files");
            node = node.addNode("article");
            node.setProperty("author","berry");
            session.save();

            node = root.getNode("navigation").getNode("byAuthorSource").getNode("berry");
            assertNotNull(node);
            item = session.getItem("/navigation/byAuthorSource/berry/hippo:facets");
            assertNotNull(item);
            System.err.println("BERRYBERRYBERRY "+item.getClass().getName());
            assertFalse(item.isNode());

            session.logout();
        } catch (RepositoryException ex) {
            System.err.println("RepositoryException: "+ex.getMessage());
            ex.printStackTrace(System.err);
            fail("unexpected repository exception " + ex.getMessage());
            firstException = ex;
        } finally {
            boolean exceptionOccurred = false;
            try {
                if (repository != null) {
                    repository.close();
                    repository = null;
                }
            } catch (Exception ex) {
                if (firstException == null) {
                    firstException = ex;
                    exceptionOccurred = true;
                }
            }
            if (exceptionOccurred)
                throw firstException;
        }
    }
}
