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
package org.hippoecm.frontend.plugin;

import java.lang.reflect.Constructor;

import org.apache.wicket.Session;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.PluginModel;
import org.hippoecm.frontend.plugin.error.ErrorPlugin;
import org.hippoecm.frontend.session.UserSession;

public class PluginFactory {

    public PluginFactory(PluginManager pluginManager) {
    }

    private IPluginModel getErrorModel(String message) {
        PluginModel error = new PluginModel();
        error.put("error", message);
        return error;
    }

    public Plugin createPlugin(PluginDescriptor descriptor, IPluginModel model, Plugin parentPlugin) {
        if (parentPlugin != null) {
            descriptor.connect(parentPlugin.getDescriptor().getOutgoing());
        }
        Plugin plugin;
        if (descriptor.getClassName() == null) {
            String message = "Implementation class name for plugin '" + descriptor
                    + "' could not be retrieved from configuration.";
            plugin = new ErrorPlugin(descriptor, getErrorModel(message), parentPlugin);
        } else {
            try {
                ClassLoader loader = ((UserSession) Session.get()).getClassLoader();
                Class clazz = Class.forName(descriptor.getClassName(), true, loader);
                Class[] formalArgs = new Class[] { PluginDescriptor.class, IPluginModel.class, Plugin.class };
                Constructor constructor = clazz.getConstructor(formalArgs);
                Object[] actualArgs = new Object[] { descriptor, model, parentPlugin };
                plugin = (Plugin) constructor.newInstance(actualArgs);
            } catch (Exception e) {
                String message = e.getClass().getName() + ": " + e.getMessage() + "\n"
                        + "Failed to instantiate plugin '" + descriptor.getClassName() + "' for id '" + descriptor
                        + "'.";
                plugin = new ErrorPlugin(descriptor, getErrorModel(message), parentPlugin);
            }
        }
        return plugin;
    }

}
