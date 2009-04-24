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
package org.hippoecm.frontend.plugins.gallery.upload;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.apache.wicket.extensions.wizard.IWizardModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.gallery.Gallery;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.gallery.GalleryWorkflow;

public class UploadDialog extends AbstractDialog {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    UploadWizard wizard;
    IPluginConfig pluginConfig;
    IPluginContext pluginContext;

    public UploadDialog(IPluginContext context, IPluginConfig config) {
        this(context, config, null);
    }

    public UploadDialog(IPluginContext context, IPluginConfig config, IModel model) {
        super(model);
        ok.setVisible(false);
        cancel.setVisible(false);
        pluginConfig = config;
        pluginContext = context;
        add(wizard = new UploadWizard("wizard", this));
    }

    @Override
    public void setDialogService(IDialogService dialogService) {
        super.setDialogService(dialogService);
        wizard.setDialogService(dialogService);
    }

    String getWorkflowCategory() {
        String workflowCats = pluginConfig.getString("workflow.categories");
        return workflowCats;
    }

    IWizardModel getWizardModel() {
        return wizard.getWizardModel();
    }

    public IModel getTitle() {
        return new StringResourceModel(pluginConfig.getString("option.text", ""), this, null);
    }

    int getThumbnailSize() {
        return pluginConfig.getInt("gallery.thumbnail.size", Gallery.DEFAULT_THUMBNAIL_SIZE);
    }

    void setGalleryNode(Node node) {
        setModel(new JcrNodeModel(node));
    }

    Node getGalleryNode() {
        Object modelObject = getModelObject();
        Node node = null;
        try {
            if (modelObject != null && modelObject instanceof Node) {
                WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
                Workflow workflow = manager.getWorkflow(getWorkflowCategory(), (Node) modelObject);
                if (workflow instanceof GalleryWorkflow) {
                    return (Node) modelObject;
                }
            }
            String location = pluginConfig.getString("option.location");
            if (location != null) {
                while (location.startsWith("/")) {
                    location = location.substring(1);
                }
                javax.jcr.Session jcrSession = ((UserSession) Session.get()).getJcrSession();
                if (jcrSession.getRootNode().hasNode(location)) {
                    node = jcrSession.getRootNode().getNode(location);
                }
            }
        } catch (PathNotFoundException e) {
            Gallery.log.error("Cannot locate default upload directory " + e.getMessage());
        } catch (RepositoryException e) {
            Gallery.log.error("Error while accessing upload directory " + e.getMessage());
        }
        return node;
    }
}
