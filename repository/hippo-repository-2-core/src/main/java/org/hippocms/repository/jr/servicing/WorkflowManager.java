/*
  THIS CODE IS UNDER CONSTRUCTION, please leave as is until
  work has proceeded to a stable level, at which time this comment
  will be removed.  -- Berry
*/

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

import java.rmi.RemoteException;

import javax.jcr.Session;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.ValueFormatException;
import javax.jcr.RepositoryException;

import org.hippocms.repository.jr.servicing.ServicingNode;

import org.hippocms.repository.workflow.Workflow;
import org.hippocms.repository.workflow.WorkflowDescriptor;

public interface WorkflowManager
{
  public Session getSession() throws RepositoryException;
  public WorkflowDescriptor getWorkflowDescriptor(String category, Node item) throws RepositoryException;
  public Workflow getWorkflow(String category, Node item) throws RepositoryException;
  public Workflow getWorkflow(WorkflowDescriptor descriptor) throws RepositoryException;
}
