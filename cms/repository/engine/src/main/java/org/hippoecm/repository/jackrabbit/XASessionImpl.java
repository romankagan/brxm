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
package org.hippoecm.repository.jackrabbit;

import java.io.File;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.security.auth.Subject;

import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.ItemManager;
import org.apache.jackrabbit.core.config.AccessManagerConfig;
import org.apache.jackrabbit.core.config.WorkspaceConfig;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.security.AuthContext;
import org.apache.jackrabbit.core.state.LocalItemStateManager;
import org.apache.jackrabbit.core.state.SessionItemStateManager;
import org.apache.jackrabbit.core.state.SharedItemStateManager;
import org.hippoecm.repository.security.AMContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XASessionImpl extends org.apache.jackrabbit.core.XASessionImpl {
    private static Logger log = LoggerFactory.getLogger(XASessionImpl.class);

    protected XASessionImpl(RepositoryImpl rep, AuthContext loginContext, WorkspaceConfig wspConfig)
            throws AccessDeniedException, RepositoryException {
        super(rep, loginContext, wspConfig);
        HippoLocalItemStateManager localISM = (HippoLocalItemStateManager) ((XAWorkspaceImpl)wsp).getItemStateManager();
        ((RepositoryImpl)rep).initializeLocalItemStateManager(localISM, this, loginContext.getSubject());
    }

    protected XASessionImpl(RepositoryImpl rep, Subject subject, WorkspaceConfig wspConfig) throws AccessDeniedException,
            RepositoryException {
        super(rep, subject, wspConfig);
        HippoLocalItemStateManager localISM = (HippoLocalItemStateManager) ((XAWorkspaceImpl)wsp).getItemStateManager();
        ((RepositoryImpl)rep).initializeLocalItemStateManager(localISM, this, subject);
    }

    @Override
    protected SessionItemStateManager createSessionItemStateManager(LocalItemStateManager manager) {
        return new HippoSessionItemStateManager(((RepositoryImpl) rep).getRootNodeId(), manager, this);
    }

    @Override
    protected org.apache.jackrabbit.core.WorkspaceImpl createWorkspaceInstance(WorkspaceConfig wspConfig,
          SharedItemStateManager stateMgr, org.apache.jackrabbit.core.RepositoryImpl rep,
          org.apache.jackrabbit.core.SessionImpl session) {
        return new XAWorkspaceImpl(wspConfig, stateMgr, rep, this);
    }

    @Override
    protected ItemManager createItemManager(SessionItemStateManager itemStateMgr, HierarchyManager hierMgr) {
        return  super.createItemManager(itemStateMgr, hierMgr);
    }

    @Override
    protected AccessManager createAccessManager(Subject subject, HierarchyManager hierMgr) throws AccessDeniedException, RepositoryException {
        AccessManagerConfig amConfig = rep.getConfig().getAccessManagerConfig();
        try {
            AMContext ctx = new AMContext(new File(((RepositoryImpl)rep).getConfig().getHomeDir()), ((RepositoryImpl)rep).getFileSystem(), subject, hierMgr, ((RepositoryImpl)rep).getNamespaceRegistry(), wsp.getName());
            AccessManager accessMgr = (AccessManager) amConfig.newInstance();
            accessMgr.init(ctx);
            return accessMgr;
        } catch (AccessDeniedException ex) {
            throw ex;
        } catch (Exception ex) {
            String msg = "failed to instantiate AccessManager implementation: " + amConfig.getClassName();
            log.error(msg, ex);
            throw new RepositoryException(msg, ex);
        }
    }
}
