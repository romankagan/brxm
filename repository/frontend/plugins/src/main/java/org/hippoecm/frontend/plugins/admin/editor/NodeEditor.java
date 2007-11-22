/*
 * Copyright 2007 Hippo
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
package org.hippoecm.frontend.plugins.admin.editor;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.nodetypes.JcrNodeTypesProvider;
import org.hippoecm.frontend.model.properties.JcrPropertiesProvider;
import org.hippoecm.frontend.plugin.JcrEvent;

public class NodeEditor extends Form {
    private static final long serialVersionUID = 1L;
    
    private PropertiesEditor properties;
    private NodeTypesEditor types;

    public NodeEditor(String id, JcrNodeModel model) {
        super(id, model);
        setOutputMarkupId(true);
        
        properties = new PropertiesEditor("properties", new JcrPropertiesProvider(model));
        add(properties);

        types = new NodeTypesEditor("types", new JcrNodeTypesProvider(model)) {
            private static final long serialVersionUID = 1L;

            protected void onAddNodeType(String type) {
                try {
                    JcrNodeModel model = (JcrNodeModel) NodeEditor.this.getModel();
                    Node node = model.getNode();
                    node.addMixin(type);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
            protected void onRemoveNodeType(String type) {
                try {
                    JcrNodeModel model = (JcrNodeModel) NodeEditor.this.getModel();
                    Node node = model.getNode();
                    node.removeMixin(type);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        add(types);
    }

    public void update(AjaxRequestTarget target, JcrEvent jcrEvent) {
        if (jcrEvent.getModel() != null) {
            properties.setProvider(new JcrPropertiesProvider(jcrEvent.getModel()));
            types.setProvider(new JcrNodeTypesProvider(jcrEvent.getModel()));
            setModel(jcrEvent.getModel());
        }
        if (target != null && findPage() != null) {
            target.addComponent(this);
        }
    }

}
