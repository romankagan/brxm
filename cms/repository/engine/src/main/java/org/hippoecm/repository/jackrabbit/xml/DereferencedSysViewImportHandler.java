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
package org.hippoecm.repository.jackrabbit.xml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;

import javax.jcr.InvalidSerializedDataException;
import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.xml.ImportHandler;
import org.apache.jackrabbit.core.xml.Importer;
import org.apache.jackrabbit.core.xml.NodeInfo;
import org.apache.jackrabbit.core.xml.TextValue;
import org.hippoecm.repository.jackrabbit.xml.NamespaceContext;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class DereferencedSysViewImportHandler extends DefaultHandler {

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    /**
     * stack of ImportState instances; an instance is pushed onto the stack
     * in the startElement method every time a sv:node element is encountered;
     * the same instance is popped from the stack in the endElement method
     * when the corresponding sv:node element is encountered.
     */
    private final Stack<ImportState> stack = new Stack<ImportState>();

    /**
     * fields used temporarily while processing sv:property and sv:value elements
     */
    private Name currentPropName;
    private int currentPropType = PropertyType.UNDEFINED;
    // list of AppendableValue objects
    private ArrayList<BufferedStringValue> currentPropValues = new ArrayList<BufferedStringValue>();
    private BufferedStringValue currentPropValue;

    protected final Importer importer;

    protected Name referencePropName;

    /**
     * The current namespace context. A new namespace context is created
     * for each XML element and the parent reference is used to link the
     * namespace contexts together in a tree hierarchy. This variable contains
     * a reference to the namespace context associated with the XML element
     * that is currently being processed.
     */
    protected NamespaceContext nsContext;

    protected NamePathResolver resolver;

    protected DereferencedSysViewImportHandler(Importer importer) {
        this.importer = importer;
    }

    /**
     * Initializes the underlying {@link Importer} instance. This method
     * is called by the XML parser when the XML document starts.
     *
     * @throws SAXException if the importer can not be initialized
     * @see DefaultHandler#startDocument()
     */
    public void startDocument() throws SAXException {
        try {
            importer.start();
            nsContext = null;
        } catch (RepositoryException re) {
            throw new SAXException(re);
        }
    }

    /**
     * Closes the underlying {@link Importer} instance. This method
     * is called by the XML parser when the XML document ends.
     *
     * @throws SAXException if the importer can not be closed
     * @see DefaultHandler#endDocument()
     */
    public void endDocument() throws SAXException {
        try {
            importer.end();
        } catch (RepositoryException re) {
            throw new SAXException(re);
        }
    }

    /**
     * Starts a local namespace context for the current XML element.
     * This method is called by {@link ImportHandler} when the processing of
     * an XML element starts. The given local namespace mappings have been
     * recorded by {@link ImportHandler#startPrefixMapping(String, String)}
     * for the current XML element.
     *
     * @param mappings local namespace mappings
     */
    public final void startNamespaceContext(Map mappings) {
        nsContext = new NamespaceContext(nsContext, mappings);
        resolver = new DefaultNamePathResolver(nsContext);
    }

    /**
     * Restores the parent namespace context. This method is called by
     * {@link ImportHandler} when the processing of an XML element ends.
     */
    public final void endNamespaceContext() {
        nsContext = nsContext.getParent();
    }

    private void processNode(ImportState state, boolean start, boolean end) throws SAXException {
        if (!start && !end) {
            return;
        }
        Name[] mixinNames = null;
        if (state.mixinNames != null) {
            mixinNames = (Name[]) state.mixinNames.toArray(new Name[state.mixinNames.size()]);
        }
        NodeId id = null;
        if (state.uuid != null) {
            id = NodeId.valueOf(state.uuid);
        }
        NodeInfo node = new NodeInfo(state.nodeName, state.nodeTypeName, mixinNames, id);
        // call Importer
        try {
            if (start) {
                importer.startNode(node, state.props);
                // dispose temporary property values
                for (Iterator<PropInfo> iter = state.props.iterator(); iter.hasNext();) {
                    PropInfo pi = iter.next();
                    pi.dispose();
                }

            }
            if (end) {
                importer.endNode(node);
            }
        } catch (RepositoryException re) {
            throw new SAXException(re);
        }
    }

    //-------------------------------------------------------< ContentHandler >

    /**
    * {@inheritDoc}
    */
    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        Name name = NameFactoryImpl.getInstance().create(namespaceURI, localName);
        // check element name
        if (name.equals(NameConstants.SV_NODE)) {
            // sv:node element

            // node name (value of sv:name attribute)
            String svName = getAttribute(atts, NameConstants.SV_NAME);
            if (svName == null) {
                throw new SAXException(new InvalidSerializedDataException(
                        "missing mandatory sv:name attribute of element sv:node"));
            }

            if (!stack.isEmpty()) {
                // process current node first
                ImportState current = (ImportState) stack.peek();
                // need to start current node
                if (!current.started) {
                    processNode(current, true, false);
                    current.started = true;
                }
            }

            // push new ImportState instance onto the stack
            ImportState state = new ImportState();
            try {
                state.nodeName = resolver.getQName(svName);
            } catch (NameException e) {
                throw new SAXException(new InvalidSerializedDataException("illegal node name: " + name, e));
            } catch (NamespaceException e) {
                throw new SAXException(new InvalidSerializedDataException("illegal node name: " + name, e));
            }
            stack.push(state);
        } else if (name.equals(NameConstants.SV_PROPERTY)) {
            // sv:property element

            // reset temp fields
            currentPropValues.clear();

            // property name (value of sv:name attribute)
            String svName = getAttribute(atts, NameConstants.SV_NAME);
            if (svName == null) {
                throw new SAXException(new InvalidSerializedDataException(
                        "missing mandatory sv:name attribute of element sv:property"));
            }
            try {
                currentPropName = resolver.getQName(svName);
            } catch (NameException e) {
                throw new SAXException(new InvalidSerializedDataException("illegal property name: " + name, e));
            } catch (NamespaceException e) {
                throw new SAXException(new InvalidSerializedDataException("illegal property name: " + name, e));
            }
            // property type (sv:type attribute)
            String type = getAttribute(atts, NameConstants.SV_TYPE);
            if (type == null) {
                throw new SAXException(new InvalidSerializedDataException(
                        "missing mandatory sv:type attribute of element sv:property"));
            }
            try {
                currentPropType = PropertyType.valueFromName(type);
            } catch (IllegalArgumentException e) {
                throw new SAXException(new InvalidSerializedDataException("Unknown property type: " + type, e));
            }
        } else if (name.equals(NameConstants.SV_VALUE)) {
            // sv:value element

            // reset temp fields
            currentPropValue = new BufferedStringValue(resolver);
        } else {
            throw new SAXException(new InvalidSerializedDataException(
                    "Unexpected element in system view xml document: " + name));
        }
    }

    /**
    * {@inheritDoc}
    */
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (currentPropValue != null) {
            // property value (character data of sv:value element)
            try {
                currentPropValue.append(ch, start, length);
            } catch (IOException ioe) {
                throw new SAXException("error while processing property value", ioe);
            }
        }
    }

    /**
    * {@inheritDoc}
    */
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        if (currentPropValue != null) {
            // property value

            // data reported by the ignorableWhitespace event within
            // sv:value tags is considered part of the value
            try {
                currentPropValue.append(ch, start, length);
            } catch (IOException ioe) {
                throw new SAXException("error while processing property value", ioe);
            }
        }
    }

    /**
    * {@inheritDoc}
    */
    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        Name name = NameFactoryImpl.getInstance().create(namespaceURI, localName);
        // check element name
        ImportState state = (ImportState) stack.peek();
        if (name.equals(NameConstants.SV_NODE)) {
            // sv:node element
            if (!state.started) {
                // need to start & end current node
                processNode(state, true, true);
                state.started = true;
            } else {
                // need to end current node
                processNode(state, false, true);
            }
            // pop current state from stack
            stack.pop();
        } else if (name.equals(NameConstants.SV_PROPERTY)) {
            // sv:property element

            // check if all system properties (jcr:primaryType, jcr:uuid etc.)
            // have been collected and create node as necessary
            if (currentPropName.equals(NameConstants.JCR_PRIMARYTYPE)) {
                BufferedStringValue val = (BufferedStringValue) currentPropValues.get(0);
                String s = null;
                try {
                    s = val.retrieve();
                    state.nodeTypeName = resolver.getQName(s);
                } catch (IOException ioe) {
                    throw new SAXException("error while retrieving value", ioe);
                } catch (NameException e) {
                    throw new SAXException(new InvalidSerializedDataException("illegal node type name: " + s, e));
                } catch (NamespaceException e) {
                    throw new SAXException(new InvalidSerializedDataException("illegal node type name: " + s, e));
                }
            } else if (currentPropName.equals(NameConstants.JCR_MIXINTYPES)) {
                if (state.mixinNames == null) {
                    state.mixinNames = new ArrayList<Name>(currentPropValues.size());
                }
                for (int i = 0; i < currentPropValues.size(); i++) {
                    BufferedStringValue val = (BufferedStringValue) currentPropValues.get(i);
                    String s = null;
                    try {
                        s = val.retrieve();
                        Name mixin = resolver.getQName(s);
                        state.mixinNames.add(mixin);
                    } catch (IOException ioe) {
                        throw new SAXException("error while retrieving value", ioe);
                    } catch (NameException e) {
                        throw new SAXException(new InvalidSerializedDataException("illegal mixin type name: " + s, e));
                    } catch (NamespaceException e) {
                        throw new SAXException(new InvalidSerializedDataException("illegal mixin type name: " + s, e));
                    }
                }
            } else if (currentPropName.equals(NameConstants.JCR_UUID)) {
                BufferedStringValue val = (BufferedStringValue) currentPropValues.get(0);
                try {
                    state.uuid = val.retrieve();
                } catch (IOException ioe) {
                    throw new SAXException("error while retrieving value", ioe);
                }
            } else {
                PropInfo prop = new PropInfo(resolver, currentPropName, currentPropType, (TextValue[]) currentPropValues
                        .toArray(new TextValue[currentPropValues.size()]));
                state.props.add(prop);
            }
            // reset temp fields
            currentPropValues.clear();
        } else if (name.equals(NameConstants.SV_VALUE)) {
            // sv:value element
            currentPropValues.add(currentPropValue);
            // reset temp fields
            currentPropValue = null;
        } else {
            throw new SAXException(new InvalidSerializedDataException("invalid element in system view xml document: "
                    + localName));
        }
    }

    //--------------------------------------------------------< inner classes >
    class ImportState {
        /**
         * name of current node
         */
        Name nodeName;
        /**
         * primary type of current node
         */
        Name nodeTypeName;
        /**
         * list of mixin types of current node
         */
        ArrayList<Name> mixinNames;
        /**
         * uuid of current node
         */
        String uuid;

        /**
         * list of PropInfo instances representing properties of current node
         */
        ArrayList<PropInfo> props = new ArrayList<PropInfo>();

        /**
         * flag indicating whether startNode() has been called for current node
         */
        boolean started = false;
    }

    //-------------------------------------------------------------< private >

    /**
    * Returns the value of the named XML attribute.
    *
    * @param attributes set of XML attributes
    * @param name attribute name
    * @return attribute value,
    *         or <code>null</code> if the named attribute is not found
    */
    private static String getAttribute(Attributes attributes, Name name) {
        return attributes.getValue(name.getNamespaceURI(), name.getLocalName());
    }

}
