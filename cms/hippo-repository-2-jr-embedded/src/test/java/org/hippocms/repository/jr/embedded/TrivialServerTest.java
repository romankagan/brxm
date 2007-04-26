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
package org.hippocms.repository.jr.embedded;

import java.io.File;
import java.io.IOException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import junit.framework.TestCase;

/**
 * @version $Id$
 */
public class TrivialServerTest extends TestCase {

    private Server server;

    public void setUp() throws RepositoryException, IOException {
        File repoDir = File.createTempFile("repo", "", new File(System.getProperty("user.dir")));
        repoDir.delete();
        repoDir.mkdirs();
        server = new Server(repoDir.getPath());
    }

    public void tearDown() {
        server.close();
    }

    public void test() throws RepositoryException {
        Session session = server.login();
        Node root = session.getRootNode();

        root.addNode("x");
        root.addNode("y");
        root.addNode("z");

        assertNotNull(root.getNode("x"));
        assertNotNull(root.getNode("y"));
        assertNotNull(root.getNode("z"));

        session.save();
        session.logout();
    }
}
