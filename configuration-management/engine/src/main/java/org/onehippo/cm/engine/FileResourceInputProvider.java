/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.engine;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.onehippo.cm.api.model.Source;

public class FileResourceInputProvider implements ResourceInputProvider {
    private final Path modulePath;

    public FileResourceInputProvider(final Path modulePath) {
        this.modulePath = modulePath;
    }

    public Path getModulePath() {
        return modulePath;
    }

    @Override
    public boolean hasResource(final Source source, final String resourcePath) {
        return Files.isRegularFile(FileConfigurationUtils.getResourcePath(modulePath, source, resourcePath));
    }

    @Override
    public InputStream getResourceInputStream(final Source source, final String resourcePath) throws IOException {
        return new FileInputStream(
                FileConfigurationUtils.getResourcePath(modulePath, source, resourcePath).toFile());
    }
}
