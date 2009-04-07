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
package org.hippoecm.frontend.model.properties;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFactory;

import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StringConverter implements IModel {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;
    static final Logger log = LoggerFactory.getLogger(StringConverter.class);
    private JcrPropertyValueModel decorated;

    public StringConverter(JcrPropertyValueModel valueModel) {
        decorated = valueModel;
    }

    public String getObject() {
        try {
            if (decorated != null && decorated.getValue() != null) {
                return decorated.getValue().getString();
            } else {
                log.warn("StringConverter: JcrPropertyValueModel decorated equals null");
            }
        } catch (RepositoryException ex) {
            log.info(ex.getMessage());
        }
        return null;
    }

    public void setObject(Object object) {
        try {
            Property property = decorated.getJcrPropertymodel().getProperty();
            ValueFactory factory = property.getSession().getValueFactory();
            String string = object == null ? "" : object.toString();
            decorated.setValue(factory.createValue(string, property.getType()));
        } catch (RepositoryException ex) {
            log.info(ex.getMessage());
            return;
        }
    }

    public void detach() {
        decorated.detach();
    }

}