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
package org.hippoecm.repository.servicing.client;

import java.rmi.RemoteException;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.rmi.client.ClientSession;
import org.apache.jackrabbit.rmi.client.RemoteRepositoryException;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.servicing.remote.RemoteServicingSession;

public class ClientServicingSession extends ClientSession implements HippoSession {
    private RemoteServicingSession remote;

    public ClientServicingSession(Repository repository, RemoteServicingSession remote, LocalServicingAdapterFactory factory) {
        super(repository, remote, factory);
        this.remote = remote;
    }

    public Node copy(Node original, String absPath) throws RepositoryException {
        try {
            return getNode(this, remote.copy(original.getPath(), absPath));
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }
}
