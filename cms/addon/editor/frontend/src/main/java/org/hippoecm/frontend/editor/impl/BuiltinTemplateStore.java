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
package org.hippoecm.frontend.editor.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hippoecm.frontend.model.ocm.IStore;
import org.hippoecm.frontend.model.ocm.StoreException;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.types.ITypeLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BuiltinTemplateStore implements IStore<IClusterConfig> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(BuiltinTemplateStore.class);

    private ITypeLocator typeLocator;

    public BuiltinTemplateStore(ITypeLocator typeStore) {
        this.typeLocator = typeStore;
    }

    public Iterator<IClusterConfig> find(Map<String, Object> criteria) {
        if (criteria.containsKey("type")) {
            List<IClusterConfig> list = new ArrayList<IClusterConfig>(1);
            list.add(new BuiltinTemplateConfig((ITypeDescriptor) criteria.get("type")));
            return list.iterator();
        }
        return new ArrayList<IClusterConfig>(0).iterator();
    }

    public IClusterConfig load(String id) throws StoreException {
        try {
            return new BuiltinTemplateConfig(typeLocator.locate(id));
        } catch (StoreException ex) {
            throw new StoreException("No type found for " + id, ex);
        }
    }

    public String save(IClusterConfig cluster) throws StoreException {
        throw new UnsupportedOperationException("Builtin template store is read only");
    }

    public void delete(IClusterConfig object) {
        throw new UnsupportedOperationException("Builtin template store is read only");
    }

}
