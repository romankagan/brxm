/*
 * Copyright 2008 Hippo
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
package org.hippoecm.frontend.core.impl;

import java.io.Serializable;

import org.hippoecm.frontend.application.PluginPage;
import org.hippoecm.frontend.core.IPluginConfig;
import org.hippoecm.frontend.core.Plugin;
import org.hippoecm.frontend.core.PluginContext;
import org.hippoecm.frontend.core.ServiceListener;
import org.hippoecm.frontend.core.ServiceReference;

public class PluginContextImpl implements PluginContext, Serializable {
    private static final long serialVersionUID = 1L;

    private PluginPage page;
    private IPluginConfig properties;
    private transient PluginManager manager = null;

    public PluginContextImpl(PluginPage page, IPluginConfig config) {
        this.page = page;
        this.properties = config;
    }

    public IPluginConfig getProperties() {
        return properties;
    }

    public Plugin start(IPluginConfig config) {
        return getManager().start(config);
    }

    public <T extends Serializable> ServiceReference<T> getReference(T service) {
        return getManager().getReference(service);
    }

    public void registerService(Serializable service, String name) {
        getManager().registerService(service, name);
    }

    public void unregisterService(Serializable service, String name) {
        getManager().unregisterService(service, name);
    }

    public void registerListener(ServiceListener listener, String name) {
        getManager().registerListener(listener, name);
    }

    public void unregisterListener(ServiceListener listener, String name) {
        getManager().unregisterListener(listener, name);
    }

    private PluginManager getManager() {
        if (manager == null) {
            manager = page.getPluginManager();
        }
        return manager;
    }
}
