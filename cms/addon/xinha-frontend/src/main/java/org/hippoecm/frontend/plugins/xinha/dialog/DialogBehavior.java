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
package org.hippoecm.frontend.plugins.xinha.dialog;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.ModelService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IPluginControl;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;
import org.hippoecm.frontend.plugins.xinha.XinhaPlugin;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.PluginRequestTarget;

public abstract class DialogBehavior extends AbstractDefaultAjaxBehavior implements IDialogListener {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final String CONTENT_PANEL_SERVICE_ID = "service.modal.content";

    public static final String DIALOG_SERVICE_ID = "service.dialog.id";

    private IPluginControl control;
    protected IPluginContext context;
    protected IPluginConfig config;
    protected ModelService<IModel> modelService;
    protected IRenderService dialogRenderer;

    private IDialog dialog;

    protected String clusterServiceId;

    public DialogBehavior(IPluginContext context, IPluginConfig config, String serviceId) {
        this.context = context;
        this.config = config;
        this.clusterServiceId = serviceId;
        
        dialog = context.getService(serviceId + ".dialog", IDialog.class);
    }

    protected String createTitle() {
        return "ModalWindow[" + getComponent().getId() + "]";
    }

    @Override
    protected void respond(AjaxRequestTarget target) {
        dialog.setListener(this);
        dialog.getModal().setTitle(createTitle());
        String contentId = dialog.getModal().getContentId();
        dialog.getModal().setContent(createContentPanel(contentId, newDialogModel()));
        configureModal(dialog.getModal());
        dialog.show(target);
    }

    protected void configureModal(final ModalWindow modal) {
    }

    protected Component createContentPanel(String contentId, IModel model) {
        //Get PluginConfigService
        IPluginConfigService pluginConfigService = context.getService(IPluginConfigService.class.getName(),
                IPluginConfigService.class);

        //Lookup clusterConfig from IPluginContext
        IClusterConfig config = pluginConfigService.getCluster(getServiceId());

        //save modelServiceId and dialogServiceId in cluster config
        config.put(DIALOG_SERVICE_ID, clusterServiceId + ".dialog");
        String modelServiceId = clusterServiceId + ".model";
        config.put("model.id", modelServiceId);
        modelService = new ModelService<IModel>(modelServiceId, model);
        modelService.init(context);

        control = context.start(config);

        dialogRenderer = context.getService(CONTENT_PANEL_SERVICE_ID, IRenderService.class);
        dialogRenderer.bind((IRenderService) getComponent().findParent(XinhaPlugin.class), contentId);
        return dialogRenderer.getComponent();
    }

    public void render(PluginRequestTarget target) {
        if (dialogRenderer != null) {
            dialogRenderer.render(target);
        }
    }
    
    public void onDialogClose() {
        dialogRenderer = null;
        control.stopPlugin();
        modelService.destroy();
    }

    protected abstract String getServiceId();

    protected abstract IDialogModel newDialogModel();

}
