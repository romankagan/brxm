/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hippoecm.frontend.service;

import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.session.PluginUserSession;
import org.hippoecm.frontend.util.RequestUtils;

import static org.hippoecm.frontend.Main.PLUGIN_APPLICATION_VALUE_CMS;

public class NestedBrowserContextService implements INestedBrowserContextService, IClusterable {

    public static final String IFRAME_REQUEST_PARAMETER_NAME = "iframe";
    private final boolean hidePerspectiveMenu;

    public NestedBrowserContextService(final boolean hidePerspectiveMenu) {
        this.hidePerspectiveMenu = hidePerspectiveMenu;
    }

    @Override
    public boolean showNavigationApplication() {
        return PluginUserSession.get().getApplicationName().equals(PLUGIN_APPLICATION_VALUE_CMS)
                && RequestUtils.getQueryParameterValue(IFRAME_REQUEST_PARAMETER_NAME).isNull();
    }

    @Override
    public boolean hidePerspectiveMenu() {
        return hidePerspectiveMenu;
    }
}
