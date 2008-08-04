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
package org.hippoecm.frontend.editor.builder;

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.layout.YuiWireframeBehavior;
import org.hippoecm.frontend.service.render.RenderPlugin;

public class TemplateTypeLayout extends RenderPlugin {
    private static final long serialVersionUID = 1L;

    public TemplateTypeLayout(IPluginContext context, IPluginConfig config) {
        super(context, config);

        YuiWireframeBehavior wireframe = new YuiWireframeBehavior("template-type-wrapper", true);
        wireframe.addUnit("center", "id=template-type-center", "body=template-type-center-body", "scroll=true");
        wireframe.addUnit("right", "id=template-type-right", "width=250", "resize=true", "body=template-type-right-body", "scroll=true");
        add(wireframe);
    }

}
