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
package org.hippoecm.frontend.editor.tools;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.hippoecm.editor.tools.JcrTypeDescriptor;
import org.hippoecm.editor.tools.JcrTypeStore;
import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.junit.Test;

public class JcrTypeDescriptorTest extends PluginTest {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    @Test
    public void testAllFieldsReturned() throws Exception {
        JcrTypeStore typeStore = new JcrTypeStore();

        JcrTypeDescriptor descriptor = typeStore.getTypeDescriptor("test:inheriting");
        assertEquals(1, descriptor.getDeclaredFields().size());

        IFieldDescriptor field = descriptor.getDeclaredFields().values().iterator().next();
        assertEquals("extra", field.getName());
        assertEquals("String", field.getType());

        Map<String, IFieldDescriptor> fields = descriptor.getFields();
        assertEquals(3, fields.size());
    }

}
