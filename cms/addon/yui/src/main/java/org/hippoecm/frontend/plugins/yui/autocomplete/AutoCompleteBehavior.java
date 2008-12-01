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

package org.hippoecm.frontend.plugins.yui.autocomplete;

import org.apache.wicket.util.template.PackagedTextTemplate;
import org.hippoecm.frontend.plugins.yui.AbstractYuiAjaxBehavior;
import org.hippoecm.frontend.plugins.yui.HippoNamespace;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.hippoecm.frontend.plugins.yui.header.templates.HippoTextTemplate;
import org.hippoecm.frontend.plugins.yui.javascript.Settings;
import org.hippoecm.frontend.plugins.yui.webapp.IYuiManager;

public abstract class AutoCompleteBehavior extends AbstractYuiAjaxBehavior {
    private static final long serialVersionUID = 1L;

    private static final PackagedTextTemplate INIT_AUTOCOMPLETE = new PackagedTextTemplate(AutoCompleteBehavior.class,
            "init_autocomplete.js");

    protected final AutoCompleteSettings settings;

    public AutoCompleteBehavior(IYuiManager service, AutoCompleteSettings settings) {
        super(service, settings);
        this.settings = settings;
    }
    
    @Override
    public void addHeaderContribution(IYuiContext context) {
        context.addModule(HippoNamespace.NS, "autocompletemanager");
        context.addTemplate(new HippoTextTemplate(INIT_AUTOCOMPLETE, getClientClassname()) {
            private static final long serialVersionUID = 1L;

            @Override
            public String getId() {
                return getComponent().getMarkupId();
            }

            @Override
            public Settings getSettings() {
                return AutoCompleteBehavior.this.getSettings();
            }
        });
        context.addOnload("YAHOO.hippo.AutoCompleteManager.onLoad()");
    }

    protected AutoCompleteSettings getSettings() {
        updateAjaxSettings();
        StringBuffer buf = new StringBuffer();
        buf.append("function doCallBack").append(getComponent().getMarkupId(true)).append("(myCallbackUrl){ ");
        buf.append(generateCallbackScript("wicketAjaxGet(myCallbackUrl")).append(" }");
        settings.setCallbackFunction(buf.toString());
        return settings;
    }

    /**
     * Determines which javascript class is used
     * @return
     */
    protected String getClientClassname() {
        return "YAHOO.hippo.HippoAutoComplete";
    }

}
