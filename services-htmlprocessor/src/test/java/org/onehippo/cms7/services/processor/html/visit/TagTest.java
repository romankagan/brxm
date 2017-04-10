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
package org.onehippo.cms7.services.processor.html.visit;

import org.htmlcleaner.HtmlNode;
import org.htmlcleaner.TagNode;
import org.junit.Test;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

public class TagTest {

    @Test
    public void testCreateTagFromTagNode() throws Exception {
        final TagNode tagNode = mockTagNode();

        final Tag tag = Tag.from(tagNode);
        tag.getName();
        tag.addAttribute("attr", "attrValue");
        tag.getAttribute("attr");
        tag.removeAttribute("attr");

        verify(tagNode);
    }

    @Test
    public void testCreateTagFromHtmlNode() throws Exception {
        final HtmlNode htmlNode =  mockTagNode();

        final Tag tag = Tag.from(htmlNode);
        tag.getName();
        tag.addAttribute("attr", "attrValue");
        tag.getAttribute("attr");
        tag.removeAttribute("attr");

        verify(htmlNode);
    }

    private static TagNode mockTagNode() {
        final TagNode tagNode = createMock(TagNode.class);
        expect(tagNode.getName()).andReturn("a");
        expect(tagNode.getAttributeByName(eq("attr"))).andReturn("attrValue");
        tagNode.removeAttribute("attr");
        expectLastCall();
        tagNode.addAttribute(eq("attr"), eq("attrValue"));
        expectLastCall();
        replay(tagNode);
        return tagNode;
    }
}
