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

public interface Pipeline
{
    
    void initialize() throws ContainerException;
    
    void beforeInvoke(ServletConfig servletConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException, ContainerNoMatchException;

    void invoke(ServletConfig servletConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException, ContainerNoMatchException;
    
    void afterInvoke(ServletConfig servletConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException, ContainerNoMatchException;

    void destroy() throws ContainerException;
    
}
