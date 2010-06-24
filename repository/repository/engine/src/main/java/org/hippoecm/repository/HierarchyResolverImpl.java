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
package org.hippoecm.repository;

import java.util.Map;
import java.util.TreeMap;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.hippoecm.repository.api.HierarchyResolver;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HierarchyResolverImpl implements HierarchyResolver {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private final Logger logger = LoggerFactory.getLogger(HierarchyResolverImpl.class);

    public Item getItem(Node ancestor, String path, boolean isProperty, Entry last)
      throws InvalidItemStateException, RepositoryException {
        if(logger.isDebugEnabled()) {
            logger.debug("resolving path "+path+(isProperty?" as property":""));
        }
        if(last != null) {
            last.node = null;
            last.relPath = null;
        }
        Node node = ancestor;
        String[] pathElts = path.split("/");
        int pathEltsLength = pathElts.length;
        if(isProperty)
            --pathEltsLength;
        for(int pathIdx=0; pathIdx<pathEltsLength && node != null; pathIdx++) {
            String relPath = pathElts[pathIdx];
            if(logger.isDebugEnabled()) {
                logger.debug("resolving path element "+relPath);
            }
            if(relPath.startsWith("{.}")) {
                relPath = ancestor.getName() + relPath.substring("{.}".length());
            } else if(relPath.startsWith("{_name}")) {
                relPath = ancestor.getName() + relPath.substring("{_name}".length());
            } else if(relPath.startsWith("{_document}")) {
                if(node != null) {
                    for (NodeIterator iter=node.getNodes(); iter.hasNext(); ) {
                        Node child = iter.nextNode();
                        if(child.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                            relPath = child.getName();
                            if(child.getIndex() > 1) {
                                relPath += "[" + Integer.toString(child.getIndex()) + "]";
                            }
                        }
                    }
                }
            } else if(relPath.startsWith("{..}")) {
                relPath = ancestor.getParent().getName() + relPath.substring("{..}".length());
            } else if(relPath.startsWith("{_parent}")) {
                relPath = ancestor.getParent().getName() + relPath.substring("{_parent}".length());
            } else if(relPath.startsWith("{") && relPath.endsWith("}")) {
                String uuid = relPath.substring(1,relPath.length()-1);
                uuid = ancestor.getProperty(uuid).getString();
                node = node.getSession().getNodeByUUID(uuid);
                continue;
            }
            Map<String,String> conditions = null;
            String relIndex = null;
            if(relPath.contains("[") && relPath.endsWith("]")) {
                relIndex = relPath.substring(relPath.indexOf("[")+1,relPath.lastIndexOf("]"));
                try {
                    Integer.decode(relIndex);
                } catch(NumberFormatException ex) {
                    conditions = new TreeMap<String,String>();
                }
            }
            if (conditions != null) {
                String[] conditionElts = relIndex.split(",");
                for(int conditionIdx=0; conditionIdx<conditionElts.length; conditionIdx++) {
                    int pos = conditionElts[conditionIdx].indexOf("=");
                    if(pos >= 0) {
                        String key = conditionElts[conditionIdx].substring(0,pos);
                        if (key.startsWith("@")) {
                            key = key.substring(1);
                        }
                        String value = conditionElts[conditionIdx].substring(pos+1);
                        if(value.startsWith("'") && value.endsWith("'")) {
                            value = value.substring(1,value.length()-1);
                            conditions.put(key, value);
                        } else if(value.startsWith("{") && value.endsWith("}")) {
                            value = ancestor.getProperty(value.substring(1,value.length()-1)).getString();
                            conditions.put(key, value);
                        } else {
                            conditions.put(key, value);
                        }
                    } else {
                        if(conditionElts[conditionIdx].equals("{_similar}")) {
                            Node parent = ancestor.getParent();
                            if(parent.hasProperty(HippoNodeType.HIPPO_DISCRIMINATOR)) {
                                Value[] discriminators = parent.getProperty(HippoNodeType.HIPPO_DISCRIMINATOR).getValues();
                                for(int i=0; i<discriminators.length; i++) {
                                    conditions.put(discriminators[i].getString(),
                                                   ancestor.getProperty(discriminators[i].getString()).getString());
                                }
                            }
                        } else {
                            conditions.put(conditionElts[conditionIdx], null);
                        }
                    }
                }
                relPath = relPath.substring(0,relPath.indexOf("["));
            }
            if(last != null) {
                last.node = node;
                last.relPath = relPath;
            }
            if(conditions == null || conditions.size() == 0) {
                if(node.hasNode(relPath)) {
                    try {
                        node = node.getNode(relPath);
                        if(logger.isDebugEnabled()) {
                            logger.debug("resolved relPath as non-conditional item "+node.getPath());
                        }
                    } catch(PathNotFoundException ex) {
                        return null; // impossible because of hasNode statement
                    }
                } else {
                    if (pathIdx+1 == pathEltsLength && node.hasProperty(relPath)) {
                        try {
                            node = node.getNode(relPath);
                            if (logger.isDebugEnabled()) {
                                logger.debug("resolved relPath as non-conditional item " + node.getPath());
                            }
                        } catch (PathNotFoundException ex) {
                            return null; // impossible because of hasNode statement
                        }
                    }
                    if (logger.isDebugEnabled()) {
                        logger.debug("could not resolve non-conditional path " + relPath);
                    }
                    return null;
                }
            } else {
                Node child = null;
                for(NodeIterator iter = node.getNodes(relPath); iter.hasNext(); ) {
                    child = iter.nextNode();
                    if(logger.isDebugEnabled()) {
                        logger.debug("considering for conditional path "+relPath+" item "+child.getPath());
                    }
                    for(Map.Entry<String,String> condition: conditions.entrySet()) {
                        if(child.hasProperty(condition.getKey())) {
                            if(condition.getValue() != null) {
                                try {
                                    if(!child.getProperty(condition.getKey()).getString().equals(condition.getValue())) {
                                        if(logger.isDebugEnabled()) {
                                             logger.debug("child is not item because of key "+condition.getKey()+" expected "+condition.getValue()+" found "+child.getProperty(condition.getKey()).getString().equals(condition.getValue()));
                                        }
                                        child = null;
                                        break;
                                    }
                                } catch(PathNotFoundException ex) {
                                    child = null; // impossible because if hasProperty check
                                    break;
                                } catch(ValueFormatException ex) {
                                    if(logger.isDebugEnabled()) {
                                        logger.debug("child is not item because of key "+condition.getKey()+" was not or right type");
                                    }
                                    child = null;
                                    break;
                                }
                            }
                        } else {
                            if(logger.isDebugEnabled()) {
                                logger.debug("child is not item because of key "+condition.getKey()+" was not found");
                            }
                            child = null;
                            break;
                        }
                    }
                    if(child != null)
                        break;
                }
                if(child == null) {
                    if(logger.isDebugEnabled()) {
                        logger.debug("could not resolve conditional path "+relPath);
                    }
                    return null;
                } else
                    node = child;
            }
        }
        if(isProperty) {
            if(logger.isDebugEnabled()) {
                logger.debug("resolving final property path "+pathElts[pathEltsLength]);
            }
            if(node.hasProperty(pathElts[pathEltsLength])) {
                return node.getProperty(pathElts[pathEltsLength]);
            } else {
                if(last != null) {
                    last.node = node;
                    last.relPath = pathElts[pathEltsLength];
                }
                return null;
            }
        } else
            return node;
    }

    public Item getItem(Node node, String field) throws RepositoryException {
        return getItem(node, field, false, null);
    }

    public Property getProperty(Node node, String field) throws RepositoryException {
        return (Property) getItem(node, field, true, null);
    }

    public Property getProperty(Node node, String field, Entry last) throws RepositoryException {
        return (Property) getItem(node, field, true, last);
    }

    public Node getNode(Node node, String field) throws InvalidItemStateException, RepositoryException {
        return (Node) getItem(node, field, false, null);
    }
}
