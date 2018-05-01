/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.channelmanager.extensions;

class CmsExtensionBean implements CmsExtension {

    private String id;
    private String displayName;
    private String urlPath;
    private CmsExtensionContext context;

    @Override
    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String getUrlPath() {
        return urlPath;
    }

    public void setUrlPath(final String urlPath) {
        this.urlPath = urlPath;
    }

    @Override
    public CmsExtensionContext getContext() {
        return context;
    }

    public void setContext(final CmsExtensionContext context) {
        this.context = context;
    }
}

