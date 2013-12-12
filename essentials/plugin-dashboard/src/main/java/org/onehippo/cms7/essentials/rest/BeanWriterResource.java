/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.rest;

import java.io.File;
import java.util.Collection;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.onehippo.cms7.essentials.dashboard.ctx.DashboardPluginContext;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.model.BeanWriterLogEntry;
import org.onehippo.cms7.essentials.dashboard.utils.BeanWriterUtils;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.dashboard.utils.ProjectUtils;
import org.onehippo.cms7.essentials.dashboard.utils.beansmodel.MemoryBean;
import org.onehippo.cms7.essentials.rest.model.MessageRestful;
import org.onehippo.cms7.essentials.rest.model.RestfulList;

import com.google.common.collect.Multimap;

/**
 * @version "$Id$"
 */
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("/beanwriter/")
public class BeanWriterResource extends BaseResource {


    @POST
    public RestfulList<MessageRestful> runBeanWriter() {
        final String basePath = ProjectUtils.getBaseProjectDirectory();
        final RestfulList<MessageRestful> messages = new RestfulList<>();
        messages.add(new MessageRestful("Not Enabled @see org.onehippo.cms7.essentials.rest.BeanWriterResource"));
        messages.add(new MessageRestful("Not implemented yet"));
      /*  final java.nio.file.Path namespacePath = new File(basePath + File.separator + "bootstrap").toPath();
        final PluginContext context = new DashboardPluginContext(GlobalUtils.createSession(), null);
        final List<MemoryBean> memoryBeans = BeanWriterUtils.buildBeansGraph(namespacePath, context, EssentialConst.SOURCE_PATTERN_JAVA);
        BeanWriterUtils.addMissingMethods(context, memoryBeans, EssentialConst.FILE_EXTENSION_JAVA);
        final Multimap<String,Object> pluginContextData = context.getPluginContextData();
        final Collection<Object> objects = pluginContextData.get(BeanWriterUtils.CONTEXT_DATA_KEY);
        for (Object object : objects) {
            final BeanWriterLogEntry entry = (BeanWriterLogEntry) object;
            messages.add(new MessageRestful(entry.getMessage()));
        }*/
        return messages;
    }
}
