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
package org.hippoecm.cmsprototype.frontend.plugins.list;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IStyledColumn;
import org.apache.wicket.model.Model;
import org.hippoecm.cmsprototype.frontend.plugins.list.datatable.ICustomizableDocumentListingDataTable;
import org.hippoecm.frontend.UserSession;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Notification;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractListingPlugin extends Plugin {

    private static final String PROPERTYNAME_PROPERTY = "propertyname";

    private static final String COLUMNNAME_PROPERTY = "columnname";

    private static final String PAGESIZE_PROPERTY = "pagesize";
    private static final String VIEWSIZE_PROPERTY = "viewsize";

    static final Logger log = LoggerFactory.getLogger(AbstractListingPlugin.class);
    
    private static final long serialVersionUID = 1L;

    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int DEFAULT_VIEW_SIZE = 5;

    public int pageSize = DEFAULT_PAGE_SIZE;
    public int viewSize = DEFAULT_VIEW_SIZE;

    public static final String USER_PATH_PREFIX = "/hippo:configuration/hippo:users/"; 
   
    protected ICustomizableDocumentListingDataTable dataTable;
    protected List<IStyledColumn> columns;
    
    public AbstractListingPlugin(PluginDescriptor pluginDescriptor, JcrNodeModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);
        this.createTableColumns(pluginDescriptor, model);
    }
    

    @Override
    public void receive(Notification notification) {
        if ("select".equals(notification.getOperation())) {
            JcrNodeModel nodeModel = new JcrNodeModel(notification.getData());
            HippoNode node = nodeModel.getNode();
            try {
                if (!nodeModel.equals(getModel())
                        && !node.isNodeType(HippoNodeType.NT_DOCUMENT)
                        && !node.isNodeType(HippoNodeType.NT_HANDLE)) {
                    setModel(nodeModel);
                    remove((Component)dataTable);
                    addTable(nodeModel, pageSize, viewSize);
                    notification.getContext().addRefresh(this);
                }
            } catch(RepositoryException ex) {
                log.error("Repository exception: " + ex.getMessage());
            }
        }
        // don't propagate the notification to children
    }
    
    public void createTableColumns(PluginDescriptor pluginDescriptor, JcrNodeModel model) {
        UserSession session = (UserSession) Session.get();
        columns = new ArrayList<IStyledColumn>();
        String userPrefNodeLocation = USER_PATH_PREFIX + session.getJcrSession().getUserID() + "/" + getPluginUserPrefNodeName();
        try {
            Node userPrefNode = (Node) session.getJcrSession().getItem(userPrefNodeLocation);
            

            pageSize = getPropertyIntValue(userPrefNode, PAGESIZE_PROPERTY, DEFAULT_PAGE_SIZE);
            viewSize = getPropertyIntValue(userPrefNode, VIEWSIZE_PROPERTY, DEFAULT_VIEW_SIZE);
            
            NodeIterator nodeIt = userPrefNode.getNodes();
            if(nodeIt.getSize() == 0) {
                defaultColumns(columns,pluginDescriptor);
            }
            while(nodeIt.hasNext()) {
                Node n = nodeIt.nextNode();
                if(n.hasProperty(COLUMNNAME_PROPERTY) && n.hasProperty(PROPERTYNAME_PROPERTY)) {
                    String columnName = n.getProperty(COLUMNNAME_PROPERTY).getString();
                    String propertyName = n.getProperty(PROPERTYNAME_PROPERTY).getString();
                    columns.add(new NodeColumn(new Model(columnName), propertyName , pluginDescriptor.getIncoming()));
                }
            }
        } catch (PathNotFoundException e) {
            // Th user preference node for the current plugin does not exist: create node now with default settings:
            log.debug("No user doclisting preference node found. Creating default doclisting preference node."); 
            javax.jcr.Session jcrSession = session.getJcrSession();
            try {
                if(!jcrSession.itemExists(userPrefNodeLocation)) {
                    // User doesn't have a user folder yet
                    Node userNode = (Node) jcrSession.getItem(USER_PATH_PREFIX + session.getJcrSession().getUserID());
                    createDefaultPrefNodeSetting(userNode);
                    
                    columns.add(new NodeColumn(new Model("Name"), "name" , pluginDescriptor.getIncoming()));
                    columns.add(new NodeColumn(new Model("Type"), "jcr:primaryType" , pluginDescriptor.getIncoming()));
                }
                /*
                 * We do not save the added node. If a user calls session.save his preferences are
                 * saved. So, as long as a user does not save, these default preference nodes are 
                 * recreated.
                 */ 
            } catch (PathNotFoundException e1) {
                logError(e1.getMessage());
                defaultColumns(columns,pluginDescriptor);
            } catch (ItemExistsException e1) {
                logError(e1.getMessage());
                defaultColumns(columns,pluginDescriptor);
            } catch (VersionException e1) {
                logError(e1.getMessage());
                defaultColumns(columns,pluginDescriptor);
            } catch (ConstraintViolationException e1) {
                logError(e1.getMessage());
                defaultColumns(columns,pluginDescriptor);
            } catch (LockException e1) {
                logError(e1.getMessage());
                defaultColumns(columns,pluginDescriptor);
            } catch (ValueFormatException e1) {
                logError(e1.getMessage());
                defaultColumns(columns,pluginDescriptor);
            } catch (RepositoryException e1) {
                logError(e1.getMessage());
                defaultColumns(columns,pluginDescriptor);
            }
            
        } catch (RepositoryException e) {
            logError(e.getMessage());
            defaultColumns(columns,pluginDescriptor);
        }
        
        addTable(model, pageSize, viewSize);
    }


    private int getPropertyIntValue(Node userPrefNode,String property, int defaultValue) throws RepositoryException, ValueFormatException, PathNotFoundException {
        int value = 0;
        if(userPrefNode.hasProperty(property) && userPrefNode.getProperty(property).getValue().getType() == PropertyType.LONG ){
            value = (int)userPrefNode.getProperty(property).getLong();
            return value == 0 ? defaultValue : value; 
        }
        // TODO : make sure it cannot be a string value. Currently, saving through the console makes the number
        // a string. Also fix this in the repository.cnd
        else if(userPrefNode.hasProperty(property) && userPrefNode.getProperty(property).getValue().getType() == PropertyType.STRING ){
            String pageSizeString = userPrefNode.getProperty(property).getString();
            try {
                value = Integer.parseInt(pageSizeString);
                return value == 0 ? defaultValue : value; 
            } catch (NumberFormatException e) {
                // do nothing. Keep pageSize default
            }
        }
        return defaultValue;
    }


    private void createDefaultPrefNodeSetting(Node userNode) throws ItemExistsException, PathNotFoundException, NoSuchNodeTypeException, LockException, VersionException, ConstraintViolationException, RepositoryException, ValueFormatException {
        Node prefNode = userNode.addNode(getPluginUserPrefNodeName(), "hippo:usersettings");
        prefNode.setProperty(PAGESIZE_PROPERTY, DEFAULT_PAGE_SIZE);
        prefNode.setProperty(VIEWSIZE_PROPERTY, DEFAULT_VIEW_SIZE);
        Node pref = prefNode.addNode("name","hippo:usersettings");
        pref.setProperty(COLUMNNAME_PROPERTY, "Name");
        pref.setProperty(PROPERTYNAME_PROPERTY, "name");
        
        pref = prefNode.addNode("type","hippo:usersettings");
        pref.setProperty(COLUMNNAME_PROPERTY, "Type");
        pref.setProperty(PROPERTYNAME_PROPERTY, "jcr:primaryType");
    }
    
    private void defaultColumns(List<IStyledColumn> columns2,PluginDescriptor pluginDescriptor) {
        columns2.add(new NodeColumn(new Model("Name"), "name" , pluginDescriptor.getIncoming()));
        columns2.add(new NodeColumn(new Model("Type"), "jcr:primaryType" , pluginDescriptor.getIncoming()));
    }
    private void logError(String message) {
        log.error("error creating user doclisting preference " + message );
        log.error("default doclisting will be shown");
    }


    abstract void addTable(JcrNodeModel nodeModel, int pageSize, int viewSize);
    
    abstract String getPluginUserPrefNodeName();
}
