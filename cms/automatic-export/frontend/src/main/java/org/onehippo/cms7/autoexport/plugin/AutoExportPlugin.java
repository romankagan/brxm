/*
 *  Copyright 2011 Hippo.
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
package org.onehippo.cms7.autoexport.plugin;

import static org.onehippo.cms7.autoexport.Constants.CONFIG_NODE_PATH;
import static org.onehippo.cms7.autoexport.Constants.CONFIG_ENABLED_PROPERTY_NAME;
import static org.onehippo.cms7.autoexport.Constants.PROJECT_BASEDIR_PROPERTY;
import static org.onehippo.cms7.autoexport.Constants.LOGGER_NAME;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoExportPlugin extends RenderPlugin<Node> {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(LOGGER_NAME);

    public AutoExportPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        
        // set up label component
        final Label label = new Label("link-text", new Model<String>() {
            private static final long serialVersionUID = 1L;
            
            private final String unavailable = new StringResourceModel("unavailable", AutoExportPlugin.this, null).getObject();
            private final String disable = new StringResourceModel("disable", AutoExportPlugin.this, null).getObject();
            private final String enable = new StringResourceModel("enable", AutoExportPlugin.this, null).getObject();
            
            @Override public String getObject() {
                if (!isExportAvailable()) {
                    return unavailable;
                }
                return isExportEnabled() ? disable : enable;
            }
        });
        label.setOutputMarkupId(true);
        label.add(new AttributeModifier("style", true, new Model<String>() {
            private static final long serialVersionUID = 1L;
            @Override
            public String getObject() {
                if (!isExportAvailable()) {
                    return "color:grey";
                }
                return isExportEnabled() ? "color:green" : "color:red";
            }
        }));
        // set up icon component
        final Image icon = new Image("icon") {
            private static final long serialVersionUID = 1L;
            private final ResourceReference emptyGif = new ResourceReference(AutoExportPlugin.class, "empty.gif");
            private final ResourceReference on = new ResourceReference(AutoExportPlugin.class, "autoexport_on.png");
            private final ResourceReference off = new ResourceReference(AutoExportPlugin.class, "autoexport_off.png");
            @Override
            protected ResourceReference getImageResourceReference() {
                if (!isExportAvailable()) {
                    return emptyGif;
                }
                return isExportEnabled() ? on : off;
            }
        };
        icon.setOutputMarkupId(true);
        add(icon);
        AjaxLink<Void> link = new AjaxLink<Void>("link") {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                setExportEnabled(!isExportEnabled());
                target.addComponent(label);
                target.addComponent(icon);
            }
            
        };
        link.add(label);
        link.setEnabled(isExportAvailable());
        add(link);
    }
    
    private boolean isExportAvailable() {
        String configDir = System.getProperty(PROJECT_BASEDIR_PROPERTY);
        return configDir != null && !configDir.isEmpty();
    }

    private boolean isExportEnabled() {
        boolean enabled = true;
        try {
            Node node = getJcrSession().getNode(CONFIG_NODE_PATH);
            enabled = node.getProperty(CONFIG_ENABLED_PROPERTY_NAME).getBoolean();
        } catch (PathNotFoundException e) {
            log.warn("No such item: " + CONFIG_NODE_PATH + "/" + CONFIG_ENABLED_PROPERTY_NAME);
        } catch (RepositoryException e) {
            log.error("An error occurred reading export enabled flag", e);
        } 
        return enabled;
    }
    
    private void setExportEnabled(boolean enabled) {
        Session session = null;
        try {
            // we use a separate session in order that other changes made in the console
            // don't get persisted upon save() here
            session = getJcrSession().impersonate(new SimpleCredentials(getJcrSession().getUserID(),new char[] {}));
            Node node = session.getNode(CONFIG_NODE_PATH);
            node.setProperty(CONFIG_ENABLED_PROPERTY_NAME, enabled);
            session.save();
        } catch (PathNotFoundException e) {
            log.warn("No such item: " + CONFIG_NODE_PATH + "/" + CONFIG_ENABLED_PROPERTY_NAME);
        } catch (RepositoryException e) {
            log.error("An error occurred trying to set export enabled flag", e);
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }
    
    private Session getJcrSession() {
        return ((UserSession) org.apache.wicket.Session.get()).getJcrSession();
    }
}
