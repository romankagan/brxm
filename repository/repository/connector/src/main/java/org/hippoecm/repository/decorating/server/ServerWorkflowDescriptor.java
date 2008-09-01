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
import java.rmi.server.UnicastRemoteObject;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.decorating.remote.RemoteWorkflowDescriptor;

public class ServerWorkflowDescriptor extends UnicastRemoteObject implements RemoteWorkflowDescriptor {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    WorkflowManager manager;
    WorkflowDescriptor descriptor;

    public ServerWorkflowDescriptor(WorkflowDescriptor descriptor, WorkflowManager manager) throws RemoteException {
        super();
        this.descriptor = descriptor;
        this.manager = manager;
    }

    public String getDisplayName() throws RepositoryException {
        return descriptor.getDisplayName();
    }

    public String getAttribute(String name) throws RepositoryException, RemoteException {
        return descriptor.getAttribute(name);
    }

    public Workflow getWorkflow() throws RepositoryException, RemoteException {
        return manager.getWorkflow(descriptor);
    }
}
