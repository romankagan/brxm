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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.security.auth.Subject;

import org.apache.jackrabbit.core.NamespaceRegistryImpl;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.SearchManager;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.config.WorkspaceConfig;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.persistence.PersistenceManager;
import org.apache.jackrabbit.core.security.AuthContext;
import org.apache.jackrabbit.core.security.SystemPrincipal;
import org.apache.jackrabbit.core.state.ISMLocking;
import org.apache.jackrabbit.core.state.ItemStateCacheFactory;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.SharedItemStateManager;
import org.apache.jackrabbit.spi.Name;
import org.hippoecm.repository.FacetedNavigationEngine;
import org.hippoecm.repository.FacetedNavigationEngineFirstImpl;
import org.hippoecm.repository.FacetedNavigationEngineWrapperImpl;
import org.hippoecm.repository.security.principals.AdminPrincipal;
import org.hippoecm.repository.security.principals.FacetAuthPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryImpl extends org.apache.jackrabbit.core.RepositoryImpl {
    private static Logger log = LoggerFactory.getLogger(RepositoryImpl.class);

    protected RepositoryImpl(RepositoryConfig repConfig) throws RepositoryException {
        super(repConfig);
    }

    private FacetedNavigationEngine facetedEngine;

    public FacetedNavigationEngine getFacetedNavigationEngine() {
        if (facetedEngine == null) {
            String msg = "Please configure your facetedEngine correctly. Application will fall back to default faceted engine, "
                    + "but this is a very inefficient one. In your repository.xml (or workspace.xml if you have started the repository"
                    + "already at least once) configure the correct class for SearchIndex. See Hippo ECM documentation 'SearchIndex configuration' "
                    + "for further information.";
            log.warn(msg);
            facetedEngine = new FacetedNavigationEngineWrapperImpl(new FacetedNavigationEngineFirstImpl());
        }
        return facetedEngine;
    }

    public void setFacetedNavigationEngine(FacetedNavigationEngine engine) {
        facetedEngine = engine;
    }

    void initializeLocalItemStateManager(HippoLocalItemStateManager stateMgr,
            org.apache.jackrabbit.core.SessionImpl session, Subject subject) {
        FacetedNavigationEngine facetedEngine = getFacetedNavigationEngine();
        Set principals = subject.getPrincipals(FacetAuthPrincipal.class);
        Map<Name, String[]> authorizationQuery = new HashMap<Name, String[]>();
        for (Iterator i = principals.iterator(); i.hasNext();) {
            FacetAuthPrincipal p = (FacetAuthPrincipal) i.next();
            log.info("FacetAuthPrincipal for authorizationQuery: " + p.getName());
            authorizationQuery.put(p.getFacet(), p.getValues());
        }
        FacetedNavigationEngine.Context facetedContext;

        // TODO: This is a TEMPORARY hack: it uses "null" for the authorizationQuery to allow everything for admin users
        if (!subject.getPrincipals(SystemPrincipal.class).isEmpty()
                || !subject.getPrincipals(AdminPrincipal.class).isEmpty()) {
            facetedContext = facetedEngine.prepare(session.getUserID(), null, null, session);
        } else {
            facetedContext = facetedEngine.prepare(session.getUserID(), authorizationQuery, null, session);
        }
        stateMgr.initialize(session.getNamespaceResolver(), session.getHierarchyManager(), facetedEngine, facetedContext);
    }

    public static RepositoryImpl create(RepositoryConfig config) throws RepositoryException {
        return new RepositoryImpl(config);
    }

    @Override
    protected SharedItemStateManager createItemStateManager(PersistenceManager persistMgr, NodeId rootNodeId,
            NodeTypeRegistry ntReg, boolean usesReferences, ItemStateCacheFactory cacheFactory, ISMLocking locking)
            throws ItemStateException {
        return new HippoSharedItemStateManager(this, persistMgr, rootNodeId, ntReg, true, cacheFactory, locking);
    }

    @Override
    protected org.apache.jackrabbit.core.SessionImpl createSessionInstance(AuthContext loginContext,
            WorkspaceConfig wspConfig) throws AccessDeniedException, RepositoryException {
        return new XASessionImpl(this, loginContext, wspConfig);
    }

    @Override
    protected org.apache.jackrabbit.core.SessionImpl createSessionInstance(Subject subject, WorkspaceConfig wspConfig)
            throws AccessDeniedException, RepositoryException {
        return new XASessionImpl(this, subject, wspConfig);
    }

    @Override
    protected NodeTypeRegistry getNodeTypeRegistry() {
        return super.getNodeTypeRegistry();
    }

    @Override
    protected NodeId getRootNodeId() {
        return super.getRootNodeId();
    }

    @Override
    protected FileSystem getFileSystem() {
        return super.getFileSystem();
    }

    @Override
    protected NamespaceRegistryImpl getNamespaceRegistry() {
        return super.getNamespaceRegistry();
    }

    public SearchManager getSearchManager(String workspaceName) throws NoSuchWorkspaceException, RepositoryException {
        return ((WorkspaceInfo) getWorkspaceInfo(workspaceName)).getSearchManager();
    }

    /**
     * Get the root/system session for a workspace
     * @param workspaceName if the workspaceName equals null the default namespace is taken
     * @return Session the rootSession
     * @throws RepositoryException
     */
    public Session getRootSession(String workspaceName) throws RepositoryException {
        if (workspaceName == null) {
            workspaceName = super.repConfig.getDefaultWorkspaceName();
        }
        return ((WorkspaceInfo) getWorkspaceInfo(workspaceName)).getRootSession();
    }

    protected WorkspaceInfo createWorkspaceInfo(WorkspaceConfig wspConfig) {
        return new WorkspaceInfo(wspConfig);
    }

    protected class WorkspaceInfo extends org.apache.jackrabbit.core.RepositoryImpl.WorkspaceInfo {

        protected WorkspaceInfo(WorkspaceConfig config) {
            super(config);
        }

        protected SearchManager getSearchManager() throws RepositoryException {
            return super.getSearchManager();
        }

        /**
         * Returns the system session for this workspace.
         *
         * @return the system session for this workspace
         * @throws RepositoryException if the system session could not be created
         */
        protected Session getRootSession() throws RepositoryException {
            return super.getSystemSession();
        }
    }

    /**
     * Wrapper for login, adds rootSession to credentials if credentials are of SimpleCredentials.
     * @return session the authenticated session
     */
    public Session login(Credentials credentials, String workspaceName) throws LoginException,
            NoSuchWorkspaceException, RepositoryException {
        if (credentials != null) {
            if (credentials instanceof SimpleCredentials) {
                SimpleCredentials sc = (SimpleCredentials) credentials;

                Session rootSession = getRootSession(workspaceName);
                if (rootSession == null) {
                    throw new RepositoryException("Unable to get the roorSession for workspace: " + workspaceName);
                }
                sc.setAttribute("rootSession", rootSession);
                return super.login(sc, workspaceName);
            }
        }
        return super.login(credentials, workspaceName);
    }

    /**
     * Calls <code>login(credentials, null)</code>.
     *
     * @return session
     * @see #login(Credentials, String)
     */
    public Session login(Credentials credentials) throws LoginException, RepositoryException {
        return login(credentials, null);
    }

    /**
     * Calls <code>login(null, workspaceName)</code>.
     *
     * @return session
     * @see #login(Credentials, String)
     */
    public Session login(String workspaceName) throws LoginException, NoSuchWorkspaceException, RepositoryException {
        return login(null, workspaceName);
    }

    /**
     * Calls <code>login(null, null)</code>.
     *
     * @return session
     * @see #login(Credentials, String)
     */
    public Session login() throws LoginException, RepositoryException {
        return login(null, null);
    }
}
