/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.frontend.plugins.yui.datatable;

import org.hippoecm.frontend.plugins.yui.widget.WidgetSettings;

public class DataTableSettings extends WidgetSettings {
    final static String SVN_ID = "$Id$";

    private boolean cacheEnabled = true;
    private String autoWidthClassName;

    public String getAutoWidthClassName() {
        return autoWidthClassName;
    }

    public void setAutoWidthClassName(String autoWidthClassName) {
        this.autoWidthClassName = autoWidthClassName;
    }

    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }
}
