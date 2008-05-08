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
package org.hippoecm.frontend.plugin.editor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.core.Plugin;
import org.hippoecm.frontend.core.PluginContext;
import org.hippoecm.frontend.core.impl.PluginConfig;
import org.hippoecm.frontend.plugin.config.ConfigValue;
import org.hippoecm.frontend.plugin.parameters.ParameterValue;
import org.hippoecm.frontend.plugin.render.RenderPlugin;
import org.hippoecm.frontend.service.IEditService;
import org.hippoecm.frontend.service.IFactoryService;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.render.ModelReference;
import org.hippoecm.frontend.service.topic.Message;
import org.hippoecm.frontend.service.topic.MessageListener;
import org.hippoecm.frontend.service.topic.TopicService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiEditorPlugin implements Plugin, IEditService, IFactoryService, MessageListener, Serializable {
    private static final long serialVersionUID = 1L;

    public static final Logger log = LoggerFactory.getLogger(MultiEditorPlugin.class);

    public static final String EDITOR_ID = "editor";
    public static final String EDITOR_CLASS = "editor.class";

    private PluginContext context;
    private Map<String, ParameterValue> properties;
    private String factoryId;
    private String editorClass;
    private Map<IModel, Plugin> editors;
    private TopicService topic;
    private int editCount;

    public MultiEditorPlugin() {
        editors = new HashMap<IModel, Plugin>();
        editCount = 0;
    }

    public void start(PluginContext context) {
        this.context = context;
        properties = context.getProperties();

        topic = new TopicService(properties.get("editor.model").getStrings().get(0));
        topic.addListener(this);
        topic.init(context);

        if (properties.get(Plugin.FACTORY_ID) != null) {
            factoryId = properties.get(Plugin.FACTORY_ID).getStrings().get(0);
            if (factoryId != null) {
                context.registerService(this, factoryId);
            }
        } else {
            log.warn("No factory id ({}) defined", Plugin.FACTORY_ID);
        }

        if (properties.get(EDITOR_CLASS) != null) {
            editorClass = properties.get(EDITOR_CLASS).getStrings().get(0);
            try {
                Class clazz = Class.forName(editorClass);
                if (!IEditService.class.isAssignableFrom(clazz)) {
                    log.error("Specified editor class does not implement IEditService");
                }
                if (!IRenderService.class.isAssignableFrom(clazz)) {
                    log.error("Specified editor class does not implement IRenderService");
                }
            } catch (ClassNotFoundException ex) {
                log.error(ex.getMessage());
            }
        } else {
            log.error("No editor class ({}) defined", EDITOR_CLASS);
        }
    }

    public void stop() {
        if (factoryId != null) {
            context.unregisterService(this, factoryId);
            factoryId = null;
        }

        topic.destroy();
        for (Map.Entry<IModel, Plugin> entry : editors.entrySet()) {
            entry.getValue().stop();
            editors.remove(entry.getKey());
        }
    }

    public void edit(final IModel model) {
        Plugin plugin;
        if (!editors.containsKey(model)) {
            PluginConfig config = new PluginConfig();
            config.put(Plugin.SERVICE_ID, properties.get(Plugin.SERVICE_ID));
            config.put(RenderPlugin.DIALOG_ID, properties.get(RenderPlugin.DIALOG_ID));

            config.put(Plugin.FACTORY_ID, new ConfigValue(factoryId));
            config.put(Plugin.CLASSNAME, new ConfigValue(editorClass));

            String editorId = properties.get(EDITOR_ID).getStrings().get(0);
            String modelId = editorId + editCount + ".model";
            config.put(RenderPlugin.MODEL_ID, new ConfigValue(modelId));

            String decoratorId = editorId + editCount + ".decorator";
            config.put(RenderPlugin.DECORATOR_ID, new ConfigValue(decoratorId));

            plugin = context.start(config);
            if (plugin instanceof IEditService) {
                ((IEditService) plugin).edit(model);
            }

            editors.put(model, plugin);

            editCount++;
        } else {
            plugin = editors.get(model);
        }
        if (plugin instanceof IRenderService) {
            ((IRenderService) plugin).focus(null);
        }
    }

    public void onMessage(Message message) {
        switch (message.getType()) {
        case ModelReference.SET_MODEL:
            edit(((ModelReference.ModelMessage) message).getModel());
            break;
        }
    }

    public void delete(Serializable service) {
        if (editors.containsValue(service)) {
            for (Map.Entry<IModel, Plugin> entry : editors.entrySet()) {
                if (entry.getValue().equals(service)) {
                    entry.getValue().stop();
                    editors.remove(entry.getKey());
                    return;
                }
            }
            log.warn("editor " + service + " was not created by this plugin");
        } else {
            log.error("unknown editor " + service + " delete is ignored");
        }
    }

}
