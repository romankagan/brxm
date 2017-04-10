/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.processor.html;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HtmlProcessorFactoryTest {

    @Test
    public void testNOOPFactory() throws Exception {
        assertEquals("noop-read", HtmlProcessorFactory.NOOP.read("noop-read", null));
        assertEquals("noop-write", HtmlProcessorFactory.NOOP.write("noop-write", null));
    }

    @Test
    public void convertsDeprecatedProcessorIds() throws Exception {
        assertEquals("richtext", HtmlProcessorFactory.parseProcessorId("org.hippoecm.frontend.plugins.richtext.IHtmlCleanerService"));
        assertEquals("formatted", HtmlProcessorFactory.parseProcessorId("org.hippoecm.frontend.plugins.richtext.DefaultHtmlCleanerService"));
    }

}
