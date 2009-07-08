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
package org.hippoecm.editor.tools;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.JcrSessionModel;
import org.hippoecm.frontend.model.ocm.IStore;
import org.hippoecm.frontend.model.ocm.StoreException;
import org.hippoecm.frontend.types.BuiltinTypeStore;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.types.TypeLocator;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.ISO9075Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CndSerializer implements IClusterable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(CndSerializer.class);

    class TypeEntry {
        ITypeDescriptor oldType;
        ITypeDescriptor newType;

        TypeEntry(ITypeDescriptor oldType, ITypeDescriptor newType) {
            this.oldType = oldType;
            this.newType = newType;
        }

        ITypeDescriptor getOldType() {
            return oldType;
        }

        ITypeDescriptor getNewType() {
            return newType;
        }

        TypeUpdate getUpdate() throws StoreException {
            if (oldType == null) {
                return null;
            }

            TypeUpdate update = new TypeUpdate();

            if (newType != null) {
                update.newName = newType.getName();
            } else {
                update.newName = oldType.getName();
            }

            update.renames = new HashMap<FieldIdentifier, FieldIdentifier>();
            for (Map.Entry<String, IFieldDescriptor> entry : oldType.getFields().entrySet()) {
                IFieldDescriptor origField = entry.getValue();
                FieldIdentifier oldId = new FieldIdentifier();
                oldId.path = origField.getPath();
                oldId.type = locator.locate(origField.getType()).getType();

                if (newType != null) {
                    IFieldDescriptor newField = newType.getField(entry.getKey());
                    if (newField != null) {
                        FieldIdentifier newId = new FieldIdentifier();
                        newId.path = newField.getPath();

                        // deal with pseudo-types; find the jcr name
                        String fieldTypeName = newField.getType();
                        ITypeDescriptor fieldType;
                        if (types.containsKey(fieldTypeName)) {
                            fieldType = types.get(fieldTypeName).getNewType();
                        } else {
                            fieldType = locator.locate(fieldTypeName);
                        }
                        newId.type = fieldType.getType();

                        update.renames.put(oldId, newId);
                    }
                } else {
                    update.renames.put(oldId, oldId);
                }
            }
            return update;
        }
    }

    class SortContext {
        HashSet<String> visited;
        LinkedHashSet<ITypeDescriptor> result;

        SortContext() {
            visited = new HashSet<String>();
        }

        void visit(String typeName) {
            if (visited.contains(typeName) || !types.containsKey(typeName)) {
                return;
            }

            ITypeDescriptor descriptor = types.get(typeName).getNewType();

            visited.add(typeName);
            for (String superType : descriptor.getSuperTypes()) {
                visit(superType);
            }
            for (IFieldDescriptor field : descriptor.getFields().values()) {
                visit(field.getType());
            }
            result.add(types.get(typeName).getNewType());
        }

        LinkedHashSet<ITypeDescriptor> sort() {
            result = new LinkedHashSet<ITypeDescriptor>();
            for (String type : types.keySet()) {
                visit(type);
            }
            return result;
        }
    }

    private JcrSessionModel jcrSession;
    private Map<String, String> namespaces;
    private LinkedHashMap<String, TypeEntry> types;
    private TypeLocator locator;

    public CndSerializer(JcrSessionModel sessionModel, String namespace) throws RepositoryException, StoreException {
        this.jcrSession = sessionModel;

        IStore<ITypeDescriptor> jcrTypeStore = new JcrTypeStore();
        IStore<ITypeDescriptor> builtinTypeStore = new BuiltinTypeStore();
        locator = new TypeLocator(new IStore[] { jcrTypeStore, builtinTypeStore });

        this.namespaces = new HashMap<String, String>();
        addNamespace(namespace);

        initTypes(namespace);
        resolveNamespaces();

        versionNamespace(namespace);
    }

    public String getOutput() {
        StringBuffer output = new StringBuffer();
        for (Map.Entry<String, String> entry : namespaces.entrySet()) {
            output.append("<" + entry.getKey() + "='" + entry.getValue() + "'>\n");
        }
        output.append("\n");

        Set<ITypeDescriptor> sorted = sortTypes();
        try {
            for (ITypeDescriptor type : sorted) {
                renderType(output, type);
            }
        } catch (StoreException ex) {
            throw new RuntimeException("Type has disappeared!", ex);
        }
        return output.toString();
    }

    public Map<String, TypeUpdate> getUpdate() throws StoreException {
        Map<String, TypeUpdate> result = new HashMap<String, TypeUpdate>();
        for (Map.Entry<String, TypeEntry> entry : types.entrySet()) {
            TypeUpdate update = entry.getValue().getUpdate();
            if (update != null) {
                result.put(entry.getKey(), entry.getValue().getUpdate());
            }
        }
        return result;
    }

    private void initTypes(String namespace) throws RepositoryException {
        types = new LinkedHashMap<String, TypeEntry>();
        Session session = jcrSession.getSession();

        String uri = getCurrentUri(namespace);
        Node nsNode = session.getRootNode().getNode(HippoNodeType.NAMESPACES_PATH + "/" + namespace);
        NodeIterator typeIter = nsNode.getNodes();
        while (typeIter.hasNext()) {
            Node templateTypeNode = typeIter.nextNode();
            String pseudoName = namespace + ":" + templateTypeNode.getName();

            ITypeDescriptor oldType = null, newType = null;

            Node ntNode = templateTypeNode.getNode(HippoNodeType.HIPPOSYSEDIT_NODETYPE);
            NodeIterator versions = ntNode.getNodes(HippoNodeType.HIPPOSYSEDIT_NODETYPE);
            while (versions.hasNext()) {
                Node version = versions.nextNode();
                if (version.isNodeType(HippoNodeType.NT_REMODEL)) {
                    if (version.getProperty(HippoNodeType.HIPPO_URI).getString().equals(uri)) {
                        oldType = new JcrTypeDescriptor(new JcrNodeModel(version), locator);
                    }
                } else {
                    newType = new JcrTypeDescriptor(new JcrNodeModel(version), locator);
                }
            }

            // FIXME: it should be possible to delete types.
            if (newType == null) {
                newType = oldType;
            }
            types.put(pseudoName, new TypeEntry(oldType, newType));
        }
    }

    private void resolveNamespaces() throws StoreException {
        for (TypeEntry entry : types.values()) {
            ITypeDescriptor descriptor = entry.getNewType();
            if (descriptor.isNode()) {
                String type = descriptor.getType();
                addNamespace(type.substring(0, type.indexOf(':')));
                for (String superType : descriptor.getSuperTypes()) {
                    addNamespace(superType.substring(0, superType.indexOf(':')));
                }

                for (IFieldDescriptor field : descriptor.getFields().values()) {
                    String subType = field.getType();
                    ITypeDescriptor sub = getTypeDescriptor(subType);
                    if (sub.isNode()) {
                        addNamespace(subType.substring(0, subType.indexOf(':')));

                        List<String> superTypes = sub.getSuperTypes();
                        for (String superType : superTypes) {
                            addNamespace(superType.substring(0, superType.indexOf(':')));
                        }
                    } else if (field.getPath().indexOf(':') > 0) {
                        addNamespace(field.getPath().substring(0, field.getPath().indexOf(':')));
                    }
                }
            }
        }
    }

    private void addNamespace(String prefix) {
        if (!namespaces.containsKey(prefix)) {
            try {
                namespaces.put(prefix, jcrSession.getSession().getNamespaceURI(prefix));
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }
    }

    private void versionNamespace(String prefix) {
        if (namespaces.containsKey(prefix)) {
            String namespace = namespaces.get(prefix);
            String last = namespace;
            int pos = namespace.lastIndexOf('/');
            try {
                for (String registered : jcrSession.getSession().getNamespacePrefixes()) {
                    String uri = jcrSession.getSession().getNamespaceURI(registered);
                    if (uri.startsWith(namespace.substring(0, pos + 1))) {
                        if (isLater(uri, last)) {
                            last = uri;
                        }
                    }
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
                return;
            }

            int minorPos = last.lastIndexOf('.');
            if (minorPos > pos) {
                int minor = Integer.parseInt(last.substring(minorPos + 1));
                namespace = last.substring(0, minorPos + 1) + new Integer(minor + 1).toString();
                namespaces.put(prefix, namespace);
            } else {
                log.warn("namespace for " + prefix + " does not conform to versionable format");
            }
        } else {
            log.warn("namespace for " + prefix + " was not found");
        }
    }

    private static boolean isLater(String one, String two) {
        int pos = one.lastIndexOf('/');
        String[] oneVersions = one.substring(pos + 1).split("\\.");
        String[] twoVersions = two.substring(pos + 1).split("\\.");
        for (int i = 0; i < oneVersions.length; i++) {
            if (i < twoVersions.length) {
                int oneVersion = Integer.parseInt(oneVersions[i]);
                int twoVersion = Integer.parseInt(twoVersions[i]);
                if (oneVersion > twoVersion) {
                    return true;
                } else if (oneVersion < twoVersion) {
                    return false;
                }
            } else {
                return true;
            }
        }
        return false;
    }

    private String getCurrentUri(String prefix) {
        if ("system".equals(prefix)) {
            return "internal";
        }
        try {
            NamespaceRegistry nsReg = jcrSession.getSession().getWorkspace().getNamespaceRegistry();
            return nsReg.getURI(prefix);
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    private void renderField(StringBuffer output, IFieldDescriptor field) throws StoreException {
        String subType = field.getType();
        ITypeDescriptor sub = getTypeDescriptor(subType);
        if (sub.isNode()) {
            output.append("+");
        } else {
            output.append("-");
        }

        if (field.getPath() != null) {
            output.append(" " + encode(field.getPath()));
        } else {
            output.append(" *");
        }

        String type = sub.getType();
        if (type.indexOf(':') == -1) {
            type = type.toLowerCase();
        }
        output.append(" (" + type + ")");
        if (field.isMultiple()) {
            output.append(" multiple");
        }
        if (field.isMandatory()) {
            output.append(" mandatory");
        }
        if (field.isPrimary()) {
            output.append(" primary");
        }
        output.append("\n");
    }

    private void renderType(StringBuffer output, ITypeDescriptor typeDescriptor) throws StoreException {
        String type = typeDescriptor.getType();
        output.append("[" + encode(type) + "]");

        List<String> superFields = new LinkedList<String>();
        Iterator<String> superTypes = typeDescriptor.getSuperTypes().iterator();
        boolean first = true;
        while (superTypes.hasNext()) {
            String superType = superTypes.next();
            ITypeDescriptor superDescriptor = getTypeDescriptor(superType);
            if (superDescriptor != null) {
                Iterator<IFieldDescriptor> fields = superDescriptor.getFields().values().iterator();
                while (fields.hasNext()) {
                    IFieldDescriptor field = fields.next();
                    if (!superFields.contains(field.getPath())) {
                        superFields.add(field.getPath());
                    }
                }
            }
            if (first) {
                first = false;
                output.append(" > " + superType);
            } else {
                output.append(", " + superType);
            }
        }

        if (typeDescriptor.isMixin()) {
            output.append(" mixin");
        }

        for (IFieldDescriptor field : typeDescriptor.getFields().values()) {
            if (field.isOrdered()) {
                output.append(" orderable");
                break;
            }
        }

        output.append("\n");
        for (IFieldDescriptor field : typeDescriptor.getFields().values()) {
            if (!superFields.contains(field.getPath())) {
                renderField(output, field);
            }
        }
        output.append("\n");
    }

    private ITypeDescriptor getTypeDescriptor(String subType) throws StoreException {
        if (types.containsKey(subType)) {
            return types.get(subType).getNewType();
        } else {
            return locator.locate(subType);
        }
    }

    private Set<ITypeDescriptor> sortTypes() {
        return new SortContext().sort();
    }

    private static String encode(String name) {
        int colon = name.indexOf(':');
        if (colon > 0) {
            return name.substring(0, colon + 1) + ISO9075Helper.encodeLocalName(name.substring(colon + 1));
        }
        return name;
    }

}
