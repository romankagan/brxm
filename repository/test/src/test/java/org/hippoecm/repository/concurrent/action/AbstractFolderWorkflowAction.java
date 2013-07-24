/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.concurrent.action;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.RepositoryMap;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflowImpl;

/**
 * If your action performs its operation on FolderWorkflow then extend this class
 */
public abstract class AbstractFolderWorkflowAction extends AbstractWorkflowAction {

    private static final String[] REQUIRED_NODE_TYPES = new String[] {"hippostd:folder", "hippostd:directory"};
    private static final String WORKFLOW_CATEGORY = "threepane";
    private static final Class<FolderWorkflow> WORKFLOW_CLASS = FolderWorkflow.class;
    
    
    protected final Random random = new Random(System.currentTimeMillis());
    
    public AbstractFolderWorkflowAction(ActionContext context) {
        super(context);
    }
    
    @Override
    protected String getWorkflowCategory() {
        return WORKFLOW_CATEGORY;
    }

    @Override
    protected boolean isApplicableDocumentType(Node node) throws RepositoryException {
        for (String nodeType : REQUIRED_NODE_TYPES) {
            if (node.isNodeType(nodeType)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected Class<? extends Workflow> getWorkflowClass() {
        return WORKFLOW_CLASS;
    }
    
    protected final FolderWorkflow getFolderWorkflow(Node node) throws RepositoryException, RemoteException {
        return new FolderWorkflowImpl(new WorkflowContextImpl(node.getSession()), node.getSession(), node.getSession(), node);
    }

    private static class WorkflowContextImpl implements WorkflowContext {

        private Session session;

        private WorkflowContextImpl(Session session) {
            this.session = session;
        }

        @Override
        public WorkflowContext getWorkflowContext(final Object specification) throws MappingException, RepositoryException {
            return this;
        }

        @Override
        public Workflow getWorkflow(final String category) throws MappingException, WorkflowException, RepositoryException {
            return null;
        }

        @Override
        public Workflow getWorkflow(final String category, final Document document) throws MappingException, WorkflowException, RepositoryException {
            return null;
        }

        @Override
        public String getUserIdentity() {
            return null;
        }

        @Override
        public Session getUserSession() {
            return session;
        }

        @Override
        public Session getInternalWorkflowSession() {
            return session;
        }

        @Override
        public RepositoryMap getWorkflowConfiguration() {
            return new RepositoryMapImpl();
        }
    }

    private static class RepositoryMapImpl implements RepositoryMap {

        private Map map;

        private RepositoryMapImpl() {
            map = new HashMap();
            map.put("attic", "/content/attic");
        }

        @Override
        public boolean exists() {
            return true;
        }

        @Override
        public int size() {
            return map.size();
        }

        @Override
        public boolean isEmpty() {
            return map.isEmpty();
        }

        @Override
        public boolean containsKey(final Object key) {
            return map.containsKey(key);
        }

        @Override
        public boolean containsValue(final Object value) {
            return map.containsValue(value);
        }

        @Override
        public Object get(final Object key) {
            return map.get(key);
        }

        @Override
        public Object put(final Object key, final Object value) {
            return map.put(key, value);
        }

        @Override
        public Object remove(final Object key) {
            return map.remove(key);
        }

        @Override
        public void putAll(final Map m) {
            map.putAll(m);
        }

        @Override
        public void clear() {
            map.clear();
        }

        @Override
        public Set keySet() {
            return map.keySet();
        }

        @Override
        public Collection values() {
            return map.values();
        }

        @Override
        public Set entrySet() {
            return map.entrySet();
        }
    }
}
