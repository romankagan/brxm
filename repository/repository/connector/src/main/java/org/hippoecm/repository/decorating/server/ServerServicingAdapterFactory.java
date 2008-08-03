/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.repository.decorating.server;

import java.rmi.RemoteException;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Workspace;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.apache.jackrabbit.api.XASession;
import org.apache.jackrabbit.rmi.remote.RemoteNode;
import org.apache.jackrabbit.rmi.remote.RemoteQueryManager;
import org.apache.jackrabbit.rmi.remote.RemoteSession;
import org.apache.jackrabbit.rmi.remote.RemoteWorkspace;
import org.apache.jackrabbit.rmi.remote.RemoteQuery;
import org.apache.jackrabbit.rmi.server.ServerAdapterFactory;

import org.hippoecm.repository.api.DocumentManager;
import org.hippoecm.repository.api.HierarchyResolver;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoQuery;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowManager;

import org.hippoecm.repository.decorating.remote.RemoteDocumentManager;
import org.hippoecm.repository.decorating.remote.RemoteHierarchyResolver;
import org.hippoecm.repository.decorating.remote.RemoteRepository;
import org.hippoecm.repository.decorating.remote.RemoteWorkflowManager;

public class ServerServicingAdapterFactory extends ServerAdapterFactory implements RemoteServicingAdapterFactory {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    
    public ServerServicingAdapterFactory() {
    }

    @Override
    public RemoteRepository getRemoteRepository(Repository repository) throws RemoteException {
        return new ServerRepository(repository, this);
    }
 
    @Override
    public RemoteSession getRemoteSession(Session session) throws RemoteException {
        if (session instanceof XASession) {
            return new ServerServicingXASession((XASession) session, this);
        } else {
            return new ServerServicingSession(session, this);
        }
    }

    @Override
    public RemoteWorkspace getRemoteWorkspace(Workspace workspace) throws RemoteException {
        if (workspace instanceof HippoWorkspace)
            return new ServerServicingWorkspace((HippoWorkspace) workspace, this);
        else
            return super.getRemoteWorkspace(workspace);
    }

    @Override
    public RemoteNode getRemoteNode(Node node) throws RemoteException {
        if (node instanceof HippoNode)
            return new ServerServicingNode((HippoNode) node, this);
        else
            return super.getRemoteNode(node);
    }

    public RemoteDocumentManager getRemoteDocumentManager(DocumentManager documentManager) throws RemoteException {
        return new ServerDocumentManager(documentManager, this);
    }

    public RemoteWorkflowManager getRemoteWorkflowManager(WorkflowManager workflowManager) throws RemoteException {
        return new ServerWorkflowManager(workflowManager, this);
    }

    public RemoteQueryManager getRemoteQueryManager(QueryManager manager, Session session) throws RemoteException {
        return new ServerQueryManager(manager, this, session);
    }

    @Override
    public RemoteQuery getRemoteQuery(Query query) throws RemoteException {
        return new ServerQuery((HippoQuery)query, this);
    }

    public RemoteHierarchyResolver getRemoteHierarchyResolver(HierarchyResolver hierarchyResolver, Session session)
        throws RemoteException {
        return new ServerHierarchyResolver(hierarchyResolver, this, session);
    }
}
