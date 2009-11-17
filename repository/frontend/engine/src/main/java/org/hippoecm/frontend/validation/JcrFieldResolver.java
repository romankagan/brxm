/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.frontend.validation;

import java.util.Iterator;

import javax.jcr.Node;

import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ChildNodeProvider;
import org.hippoecm.frontend.model.PropertyValueProvider;
import org.hippoecm.frontend.model.ocm.IStore;
import org.hippoecm.frontend.model.ocm.StoreException;
import org.hippoecm.frontend.service.EditorException;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates a model from a container model, using a {@link FieldPath}.
 */
public class JcrFieldResolver implements IFieldResolver {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(JcrFieldResolver.class);

    private IStore<ITypeDescriptor> typeStore;

    public JcrFieldResolver(IStore<ITypeDescriptor> typeStore) {
        this.typeStore = typeStore;
    }

    public IModel resolve(IModel model, FieldPath path) throws EditorException {
        if (model instanceof JcrNodeModel) {
            JcrNodeModel nodeModel = (JcrNodeModel) model;
            Node node = nodeModel.getNode();
            try {
                for (FieldElement element : path.getElements()) {
                    IFieldDescriptor field = element.getField();
                    ITypeDescriptor fieldType = typeStore.load(field.getType());
                    IDataProvider provider;
                    if (fieldType.isNode()) {
                        provider = new ChildNodeProvider(field, null, nodeModel.getItemModel());
                    } else {
                        provider = new PropertyValueProvider(field, null, nodeModel.getItemModel());
                    }
                    Iterator iter = provider.iterator(element.getIndex(), 1);
                    if (iter.hasNext()) {
                        if (fieldType.isNode()) {
                            JcrNodeModel childModel = (JcrNodeModel) provider.model(iter.next());
                            node = childModel.getNode();
                        } else {
                            return provider.model(iter.next());
                        }
                    } else {
                        throw new EditorException("Field is not available in model");
                    }
                }
            } catch (StoreException ex) {
                throw new EditorException("Type not found", ex);
            }
            return new JcrNodeModel(node);
        } else {
            throw new EditorException("Unknown model type");
        }
    }

}
