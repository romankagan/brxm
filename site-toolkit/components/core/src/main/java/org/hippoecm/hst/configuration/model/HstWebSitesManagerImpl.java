/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.hst.configuration.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.QueryResult;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.core.container.RepositoryNotAvailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles the loading of {@link HstNodeImpl}'s. 
 */
public class HstWebSitesManagerImpl implements HstWebSitesManager {
    
    private static final Logger log = LoggerFactory.getLogger(HstWebSitesManagerImpl.class);

    private Repository repository;
    private Credentials credentials;
    
    /**
     * the list of node types that we know about for the hst configuration.
     */
    List<String> nodeTypeNames;
    
    /**
     * The root of the virtual hosts node. There should always be exactly one.
     */
    HstNode virtualHostsRepositoryNode; 

    /**
     * The map of all configurationRootNodes where the key is the path to the configuration
     */
    Map<String, HstNode> configurationRootNodes = new HashMap<String, HstNode>();

    /**
     * The map of all site nodes where the key is the path
     */
    Map<String, HstSiteRootNode> siteRootNodes = new HashMap<String, HstSiteRootNode>();
    
    public void setNodeTypeNames(List<String> nodeTypeNames) {
        this.nodeTypeNames = nodeTypeNames;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }
    
    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }
    
    public void populate() throws RepositoryNotAvailableException {
        Session session = null;
        try {
            if (this.credentials == null) {
                session = this.repository.login();
            } else {
                session = this.repository.login(this.credentials);
            }
            
            // session can come from a pooled event based pool so always refresh before building configuration:
            session.refresh(false);
            
            
           // get all the root hst virtualhosts node: there is only allowed to be exactly ONE
            {
                String xpath = "//element(*, "+HstNodeTypes.NODETYPE_HST_VIRTUALHOSTS+")";
                QueryResult result =  session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
                
                NodeIterator virtualHostNodes = result.getNodes();
                if(virtualHostNodes.getSize() != 1L) {
                    throw new RepositoryNotAvailableException("There must be exactly one node of type '"+HstNodeTypes.NODETYPE_HST_VIRTUALHOSTS+"' but there are "+virtualHostNodes.getSize()+" .");
                }
                // there is exactly one virtualHostsNode
                Node virtualHostsNode = virtualHostNodes.nextNode();
                virtualHostsRepositoryNode = new HstNodeImpl(virtualHostsNode, null, nodeTypeNames, true);
            }
            
            
            // get all the root hst configuration nodes
            {
                String xpath = "//element(*, "+HstNodeTypes.NODETYPE_HST_CONFIGURATION+")";
                QueryResult result =  session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
                NodeIterator configurationRootJcrNodes = result.getNodes();
                
                while(configurationRootJcrNodes.hasNext()) {
                    Node configurationRootNode = configurationRootJcrNodes.nextNode();
                    HstNode hstNode = new HstNodeImpl(configurationRootNode, null, nodeTypeNames, true);
                    configurationRootNodes.put(hstNode.getValueProvider().getPath(), hstNode);
                }
            }
            
            // get all the mount points
            String xpath = "//element(*, "+HstNodeTypes.NODETYPE_HST_SITE+")";
            QueryResult result =  session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
            NodeIterator siteRootJcrNodes = result.getNodes();
            
            while(siteRootJcrNodes.hasNext()) {
                Node rootSiteNode = siteRootJcrNodes.nextNode();
                HstSiteRootNode hstSiteRootNode = new HstSiteRootNodeImpl(rootSiteNode, null, nodeTypeNames);
                siteRootNodes.put(hstSiteRootNode.getValueProvider().getPath(), hstSiteRootNode);
            }
            
        } catch (RepositoryException e) {
            throw new RepositoryNotAvailableException("Exception during loading configuration nodes. ",e);
        } finally {
            if (session != null) {
                try { session.logout(); } catch (Exception ce) {}
            }
        }
        
    }
    
    public HstNode getVirtualHostsNode() {
        return virtualHostsRepositoryNode;
    }
    
    public Map<String, HstSiteRootNode> getHstSiteRootNodes(){
        return siteRootNodes;
    }

    public Map<String, HstNode> getConfigurationRootNodes() {
        return configurationRootNodes;
    }

    
}
