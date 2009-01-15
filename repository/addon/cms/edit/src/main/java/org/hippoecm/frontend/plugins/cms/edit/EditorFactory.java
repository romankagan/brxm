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
package org.hippoecm.frontend.plugins.cms.edit;

import org.apache.wicket.IClusterable;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditorFactory implements IClusterable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    public static final Logger log = LoggerFactory.getLogger(EditorFactory.class);

    private IPluginContext context;
    private String cluster;

    public EditorFactory(IPluginContext context, String cluster) {
        this.context = context;
        this.cluster = cluster;
    }

    public Editor newEditor(final IModel model) throws EditorException {
        return new Editor(context, cluster, model);
    }

}
