/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.onehippo.cms7.essentials.plugins.urlrewriter;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import javax.inject.Inject;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.instructions.Instruction;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus;
import org.onehippo.cms7.essentials.dashboard.model.TargetPom;
import org.onehippo.cms7.essentials.dashboard.packaging.MessageGroup;
import org.onehippo.cms7.essentials.dashboard.service.WebXmlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Modify the site's web.xml to install the rewrite filter.
 */
public class UrlRewriterInstruction implements Instruction {

    private static final Logger logger = LoggerFactory.getLogger(UrlRewriterInstruction.class);
    private static final String FILTER_CLASS = "org.onehippo.forge.rewriting.HippoRewriteFilter";
    private static final String FILTER_NAME = "RewriteFilter";
    private static final List<String> URL_PATTERNS = Collections.singletonList("/*");
    private static final List<WebXmlService.Dispatcher> DISPATCHERS = Arrays.asList(WebXmlService.Dispatcher.REQUEST, WebXmlService.Dispatcher.FORWARD);
    private static final TargetPom MODULE = TargetPom.SITE;
    private static final Map<String, String> initParams = new HashMap<>();

    static {
        // See HippoRewriteFilter for documentation of initParams
        initParams.put("rulesLocation", "/content/urlrewriter");
        initParams.put("logLevel", "slf4j");
        initParams.put("statusEnabled", "true");
        initParams.put("statusPath", "/rewrite-status");
        initParams.put("statusEnabledOnHosts", "localhost, 127.0.0.*, *.lan, *.local");
    }

    @Inject private WebXmlService webXmlService;

    @Override
    public InstructionStatus execute(final PluginContext context) {
        if (webXmlService.addFilter(context, MODULE, FILTER_NAME, FILTER_CLASS, initParams)
                && webXmlService.addFilterMapping(context, MODULE, FILTER_NAME, URL_PATTERNS)
                && webXmlService.addDispatchersToFilterMapping(context, MODULE, FILTER_NAME, DISPATCHERS)
                && webXmlService.addDispatchersToFilterMapping(context, MODULE, "HstFilter", DISPATCHERS)) {
            return InstructionStatus.SUCCESS;
        }
        return InstructionStatus.SKIPPED;
    }

    @Override
    public void populateChangeMessages(final BiConsumer<MessageGroup, String> changeMessageQueue) {
        changeMessageQueue.accept(MessageGroup.EXECUTE, "Install URL Rewriter filter into Site web.xml.");
    }
}
