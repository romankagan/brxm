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
package org.hippoecm.hst.core.container;

import javax.servlet.ServletConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class HstRequestProcessorImpl implements HstRequestProcessor {
    
    protected Pipelines pipelines;
    
    public HstRequestProcessorImpl(Pipelines pipelines) {
        this.pipelines = pipelines;
    }

    public void processRequest(ServletConfig servletConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        Pipeline pipeline = this.pipelines.getDefaultPipeline();

        try {
            pipeline.beforeInvoke(servletConfig, servletRequest, servletResponse);
            pipeline.invoke(servletConfig, servletRequest, servletResponse);
            
        }
        catch(ContainerNoMatchException e){
          throw e;  	
        } catch (Throwable th) {
            throw new ContainerException(th);
        } finally {
            pipeline.afterInvoke(servletConfig, servletRequest, servletResponse);
        }
    }

}
