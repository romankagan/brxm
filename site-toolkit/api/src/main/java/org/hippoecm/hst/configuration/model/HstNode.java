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
package org.hippoecm.hst.configuration.model;

import java.util.List;

import org.hippoecm.hst.provider.ValueProvider;

public interface HstNode {

    /**
     * @return the value provider for this {@link HstNode}
     */
    ValueProvider getValueProvider();

    /**
     * @param relPath a path that does not start with a slash, for example 'foo' or 'foo/bar'
     * @return the descendant node at <code>relPath</code> or <code>null</code> if it does not exist
     * @throws IllegalArgumentException if <code>relPath</code> is not a valid relPath
     */
    HstNode getNode(String relPath) throws IllegalArgumentException;

    /**
     * @return List<{@link HstNode}> of all the child nodes
     */
    List<HstNode> getNodes();

    /**
     * @return List<{@link HstNode}> of all the child nodes with {@link #getNodeTypeName()} equals to <code>nodeTypeName</code>
     */
    List<HstNode> getNodes(String nodeTypeName);

    /**
     * @return the node type of the backing provider
     */
    String getNodeTypeName();

    /**
     * @return the parent of this <code>{@link HstNode}</code> or <code>null</code> when there is no parent.
     */
    HstNode getParent();

}