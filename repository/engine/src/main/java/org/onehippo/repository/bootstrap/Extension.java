/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.bootstrap;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.repository.bootstrap.util.BootstrapUtils;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_VERSION;
import static org.hippoecm.repository.api.HippoNodeType.INITIALIZE_PATH;
import static org.onehippo.repository.bootstrap.util.BootstrapConstants.INIT_FOLDER_PATH;
import static org.onehippo.repository.bootstrap.util.BootstrapConstants.TEMP_FOLDER_PATH;
import static org.onehippo.repository.bootstrap.util.BootstrapConstants.log;

public class Extension {

    private final Session session;
    private final URL extensionURL;
    private final List<InitializeItem> initializeItems = new ArrayList<>();

    Extension(final Session session, final URL extensionURL) {
        this.session = session;
        this.extensionURL = extensionURL;
    }

    void load() throws RepositoryException {
        if (log.isInfoEnabled()) {
            log.info("Loading extension {}", this);
        }
        final Node initializationFolder = session.getNode(INIT_FOLDER_PATH);
        final Node temporaryFolder = session.getNode(TEMP_FOLDER_PATH);
        try {
            BootstrapUtils.initializeNodecontent(session, TEMP_FOLDER_PATH, extensionURL);
            final Node tempInitFolderNode = temporaryFolder.getNode(INITIALIZE_PATH);
            for (final Node tempInitItemNode : new NodeIterable(tempInitFolderNode.getNodes())) {
                InitializeItem item = new InitializeItem(tempInitItemNode, this);
                item.initialize();
                initializeItems.add(item);
            }
            if(tempInitFolderNode.hasProperty(HIPPO_VERSION)) {
                updateVersionTags(initializationFolder, tempInitFolderNode);
            }
            tempInitFolderNode.remove();
            session.save();
        } catch (RepositoryException e) {
            throw new RepositoryException(String.format("Initializing extension %s failed", this), e);
        }
    }

    static void updateVersionTags(final Node initializationFolder, final Node tempInitFolderNode) throws RepositoryException {
        List<String> tags = new ArrayList<>();
        if (initializationFolder.hasProperty(HIPPO_VERSION)) {
            for (Value value : initializationFolder.getProperty(HIPPO_VERSION).getValues()) {
                tags.add(value.getString());
            }
        }
        Value[] added = tempInitFolderNode.getProperty(HIPPO_VERSION).getValues();
        for (Value value : added) {
            if (!tags.contains(value.getString())) {
                tags.add(value.getString());
            }
        }
        initializationFolder.setProperty(HIPPO_VERSION, tags.toArray(new String[tags.size()]));
    }

    String getModuleVersion() {
        String extensionURLString = extensionURL.toString();
        if (extensionURLString.contains("hippoecm-extension.xml")) {
            String manifestUrlString = StringUtils.substringBefore(extensionURLString, "hippoecm-extension.xml") + "META-INF/MANIFEST.MF";
            try {
                final Manifest manifest = new Manifest(new URL(manifestUrlString).openStream());
                return manifest.getMainAttributes().getValue(new Attributes.Name("Implementation-Build"));
            } catch (IOException ignore) {
            }
        }
        return null;
    }

    List<InitializeItem> getInitializeItems() {
        return initializeItems;
    }

    List<Node> getInitializeItemNodes() {
        List<Node> itemNodes = new ArrayList<>(initializeItems.size());
        for (InitializeItem initializeItem : initializeItems) {
            itemNodes.add(initializeItem.getItemNode());
        }
        return itemNodes;
    }

    URL getExtensionSource() {
        return extensionURL;
    }

    @Override
    public String toString() {
        final String[] split = extensionURL.toString().split("\\.jar!");
        if (split.length == 2) {
            final String jarFileName = StringUtils.substringAfterLast(split[0], "/") + ".jar";
            final String extensionFileName = split[1];
            return jarFileName + "!" + extensionFileName;
        }
        return extensionURL.toString();
    }
}
