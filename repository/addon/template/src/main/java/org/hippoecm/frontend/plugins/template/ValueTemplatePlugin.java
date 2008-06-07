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
package org.hippoecm.frontend.plugins.template;

import org.apache.wicket.markup.html.basic.Label;
import org.hippoecm.frontend.legacy.model.IPluginModel;
import org.hippoecm.frontend.legacy.plugin.Plugin;
import org.hippoecm.frontend.legacy.plugin.PluginDescriptor;
import org.hippoecm.frontend.legacy.plugin.parameters.ParameterValue;
import org.hippoecm.frontend.legacy.template.config.TemplateConfig;
import org.hippoecm.frontend.legacy.template.model.TemplateModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class ValueTemplatePlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ValueTemplatePlugin.class);

    private JcrPropertyValueModel valueModel;

    public ValueTemplatePlugin(PluginDescriptor pluginDescriptor, IPluginModel pluginModel, Plugin parentPlugin) {
        super(pluginDescriptor, new TemplateModel(pluginModel), parentPlugin);

        TemplateModel model = (TemplateModel) getPluginModel();
        valueModel = model.getJcrPropertyValueModel();

        String mode = model.getTemplateDescriptor().getMode();
        if (TemplateConfig.EDIT_MODE.equals(mode)) {
            TextFieldWidget widget = new TextFieldWidget("value", valueModel);
            if (pluginDescriptor.getParameter("size") != null) {
                ParameterValue parameter = pluginDescriptor.getParameter("size");
                if (parameter.getStrings().size() == 1) {
                    widget.setSize(parameter.getStrings().get(0));
                }
            }
            add(widget);
        } else {
            add(new Label("value", valueModel));
        }
        setOutputMarkupId(true);
    }

    @Override
    public void onDetach() {
        if (valueModel == null) {
            log.error("ValueModel is null: " + getPluginModel().toString());
        } else {
            valueModel.detach();
        }
        super.onDetach();
    }
}
