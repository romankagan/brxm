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
package org.hippoecm.frontend.plugins.standardworkflow.reorder;

import javax.jcr.RepositoryException;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IconAttributeModifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListItem extends AbstractReadOnlyModel {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(ListItem.class);

    private String name;
    private AttributeModifier cellModifier;
    private AttributeModifier columnModifier;

    public ListItem(JcrNodeModel nodeModel) {
        try {
            name = nodeModel.getNode().getName();
            IconAttributeModifier attributeModifier = new IconAttributeModifier();
            cellModifier = attributeModifier.getCellAttributeModifier(nodeModel.getNode());
            columnModifier = attributeModifier.getColumnAttributeModifier(nodeModel.getNode());
        } catch (RepositoryException e) {
            log.error(e.getMessage(), e);
        }
    }

    public String getName() {
        return name;
    }

    public AttributeModifier getCellModifier() {
        return cellModifier;
    }

    public AttributeModifier getColumnModifier() {
        return columnModifier;
    }

    @Override
    public Object getObject() {
        return this;
    }

}
