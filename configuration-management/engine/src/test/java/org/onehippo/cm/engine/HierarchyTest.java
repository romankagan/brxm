/*
 *  Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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

import java.io.IOException;
import java.util.Map;

import org.junit.Test;
import org.onehippo.cm.api.model.ConfigDefinition;
import org.onehippo.cm.api.model.Configuration;
import org.onehippo.cm.api.model.ContentDefinition;
import org.onehippo.cm.api.model.DefinitionNode;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.NamespaceDefinition;
import org.onehippo.cm.api.model.NodeTypeDefinition;
import org.onehippo.cm.api.model.Project;
import org.onehippo.cm.api.model.PropertyOperation;
import org.onehippo.cm.api.model.Source;
import org.onehippo.cm.api.model.ValueType;
import org.onehippo.cm.engine.parser.ParserException;
import org.onehippo.cm.impl.model.ConfigurationImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class HierarchyTest extends AbstractBaseTest {

    @Test
    public void expect_hierarchy_test_loads() throws IOException, ParserException {
        final PathConfigurationReader.ReadResult result = readFromTestJar("/parser/hierarchy_test/"+Constants.REPO_CONFIG_YAML);
        final Map<String, ConfigurationImpl> configurations = result.getConfigurations();
        assertEquals(2, configurations.size());

        final Configuration base = assertConfiguration(configurations, "base", new String[0], 1);
        final Project project1 = assertProject(base, "project1", new String[0], 1);
        final Module module1 = assertModule(project1, "module1", new String[0], 3);
        final Source source1 = assertSource(module1, "config.yaml", 8);
        final Source contentSource1 = assertSource(module1, "content.yaml", 1);

        final NamespaceDefinition namespace = assertDefinition(source1, 0, NamespaceDefinition.class);
        assertEquals("myhippoproject", namespace.getPrefix());
        assertEquals("http://www.onehippo.org/myhippoproject/nt/1.0", namespace.getURI().toString());

        final NodeTypeDefinition nodeType = assertDefinition(source1, 1, NodeTypeDefinition.class);
        assertEquals(
                "<'hippo'='http://www.onehippo.org/jcr/hippo/nt/2.0.4'>\n" +
                        "<'myhippoproject'='http://www.onehippo.org/myhippoproject/nt/1.0'>\n" +
                        "[myhippoproject:basedocument] > hippo:document orderable\n",
                nodeType.getValue());
        assertEquals(false, nodeType.isResource());

        final NodeTypeDefinition nodeTypeFromResource = assertDefinition(source1, 2, NodeTypeDefinition.class);
        assertEquals("example.cnd", nodeTypeFromResource.getValue());
        assertEquals(true, nodeTypeFromResource.isResource());

        final ConfigDefinition source1definition1 = assertDefinition(source1, 3, ConfigDefinition.class);
        final DefinitionNode rootDefinition1 = assertNode(source1definition1, "/", "", source1definition1, 6, 1);
        assertProperty(rootDefinition1, "/root-level-property", "root-level-property",
                source1definition1, ValueType.STRING, "root-level-property-value");
        final DefinitionNode nodeWithSingleProperty = assertNode(rootDefinition1, "/node-with-single-property",
                "node-with-single-property", source1definition1, 0, 1);
        assertProperty(nodeWithSingleProperty, "/node-with-single-property/property", "property",
                source1definition1, ValueType.STRING, "node-with-single-property-value");
        final DefinitionNode nodeWithMultipleProperties = assertNode(rootDefinition1, "/node-with-multiple-properties",
                "node-with-multiple-properties", source1definition1, 0, 3);
        assertProperty(nodeWithMultipleProperties, "/node-with-multiple-properties/single", "single",
                source1definition1, ValueType.STRING, "value1");
        assertProperty(nodeWithMultipleProperties, "/node-with-multiple-properties/multiple", "multiple",
                source1definition1, ValueType.STRING, new String[]{"value2","value3"});
        assertProperty(nodeWithMultipleProperties, "/node-with-multiple-properties/empty-multiple", "empty-multiple",
                source1definition1, ValueType.STRING, new String[0]);
        final DefinitionNode nodeWithSubNode =
                assertNode(rootDefinition1, "/node-with-sub-node", "node-with-sub-node", source1definition1, 1, 0);
        final DefinitionNode subNode =
                assertNode(nodeWithSubNode, "/node-with-sub-node/sub-node", "sub-node", source1definition1, 0, 1);
        assertProperty(subNode, "/node-with-sub-node/sub-node/property", "property", source1definition1, ValueType.STRING, "sub-node-value");
        assertNode(rootDefinition1, "/node-delete", "node-delete", source1definition1, true, null, 0, 0);
        assertNode(rootDefinition1, "/node-order-before", "node-order-before", source1definition1, false, "node", 1, 1);
        assertNull(rootDefinition1.getNodes().get("node-order-before").getIgnoreReorderedChildren());
        assertTrue(rootDefinition1.getNodes().get("node-ignore-reordered-children").getIgnoreReorderedChildren());

        final ConfigDefinition source1definition2 = assertDefinition(source1, 4, ConfigDefinition.class);
        assertNode(source1definition2, "/path/to/node-delete", "node-delete", source1definition2, true, null, 0, 0);

        final ConfigDefinition source1definition3 = assertDefinition(source1, 5, ConfigDefinition.class);
        assertNode(source1definition3, "/path/to/node-order-before", "node-order-before", source1definition3, false, "node", 0, 0);

        final ConfigDefinition source1definition4 = assertDefinition(source1, 6, ConfigDefinition.class);
        final DefinitionNode node = assertNode(source1definition4, "/path/to/node", "node", source1definition4, 2, 5);
        assertProperty(node, "/path/to/node/delete-property", "delete-property", source1definition4,
                PropertyOperation.DELETE, ValueType.STRING, new String[0]);
        assertProperty(node, "/path/to/node/add-property", "add-property", source1definition4, PropertyOperation.ADD,
                ValueType.STRING, new String[]{"addme"});
        assertProperty(node, "/path/to/node/replace-property-single-string", "replace-property-single-string",
                source1definition4, PropertyOperation.REPLACE, ValueType.STRING, "single");
        assertProperty(node, "/path/to/node/replace-property-map", "replace-property-map", source1definition4,
                PropertyOperation.REPLACE, ValueType.BINARY, new String[]{"folder/image.png"}, true, false);
        assertProperty(node, "/path/to/node/override-property", "override-property", source1definition4,
                PropertyOperation.OVERRIDE, ValueType.STRING, "single");
        final DefinitionNode nodeWithNewType =
                assertNode(node, "/path/to/node/node-with-new-type", "node-with-new-type", source1definition4, 0, 2);
        assertProperty(nodeWithNewType, "/path/to/node/node-with-new-type/jcr:primaryType", "jcr:primaryType",
                source1definition4, PropertyOperation.OVERRIDE, ValueType.NAME, "some:type");
        assertProperty(nodeWithNewType, "/path/to/node/node-with-new-type/jcr:mixinTypes", "jcr:mixinTypes",
                source1definition4, PropertyOperation.OVERRIDE, ValueType.NAME, new String[]{"some:mixin"});
        final DefinitionNode nodeWithMixinAdd = assertNode(node, "/path/to/node/node-with-mixin-add", "node-with-mixin-add",
                source1definition4, 0, 1);
        assertProperty(nodeWithMixinAdd, "/path/to/node/node-with-mixin-add/jcr:mixinTypes", "jcr:mixinTypes",
                source1definition4, PropertyOperation.ADD, ValueType.NAME, new String[]{"some:mixin"});

        final ContentDefinition contentDefinition = assertDefinition(contentSource1, 0, ContentDefinition.class);
        assertNode(contentDefinition, "/content/documents/myhippoproject", "myhippoproject", contentDefinition, 0, 1);

        final Source source2 = assertSource(module1, "folder/resources.yaml", 2);
        final ConfigDefinition source2definition = assertDefinition(source2, 0, ConfigDefinition.class);
        final DefinitionNode resourceNode = assertNode(source2definition, "/resources", "resources", source2definition, 0, 3);
        assertProperty(resourceNode, "/resources/single-value-string-resource", "single-value-string-resource",
                source2definition, PropertyOperation.REPLACE, ValueType.STRING, "string.txt", true, false);
        assertProperty(resourceNode, "/resources/single-value-binary-resource", "single-value-binary-resource",
                source2definition, PropertyOperation.REPLACE, ValueType.BINARY, "image.png", true, false);
        assertProperty(resourceNode, "/resources/multi-value-resource", "multi-value-resource", source2definition,
                PropertyOperation.REPLACE, ValueType.STRING, new String[]{"/root.txt","folder/relative.txt"}, true, false);

//        final ConfigDefinition source2definition2 = assertDefinition(source2, 1, ConfigDefinition.class);
//        assertNode(source2definition2, "/hippo:configuration/hippo:queries/hippo:templates/new-image", "new-image", source2definition2, 1, 2);

        final Configuration myhippoproject = assertConfiguration(configurations, "myhippoproject", new String[]{"base"}, 1);
        final Project project2 = assertProject(myhippoproject, "project2", new String[]{"project1", "foo/bar"}, 1);
        final Module module2 = assertModule(project2, "module2", new String[0], 1);
        final Source baseSource = assertSource(module2, "config.yaml", 1);
        final ConfigDefinition baseDefinition = assertDefinition(baseSource, 0, ConfigDefinition.class);

        final DefinitionNode rootDefinition2 =
                assertNode(baseDefinition, "/node-with-sub-node/sub-node", "sub-node", baseDefinition, 0, 1);
        assertProperty(rootDefinition2, "/node-with-sub-node/sub-node/property", "property", baseDefinition, ValueType.STRING, "override");
    }

}
