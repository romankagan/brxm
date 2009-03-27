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
package org.hippoecm.hst.mock;

import java.util.HashMap;
import java.util.Map;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.springframework.mock.web.MockHttpServletRequest;

public class MockHstRequest extends MockHttpServletRequest implements HstRequest {

    protected String referencePath;
    protected Map<String, Map<String, Object>> attributeMap = new HashMap<String, Map<String, Object>>();
    protected Map<String, Map<String, Object>> parameterMap = new HashMap<String, Map<String, Object>>();
    protected HstRequestContext requestContext;
    protected String resourceId;
    protected String referenceNamespace;
    
    public void setReferencePath(String referencePath) {
        this.referencePath = referencePath;
    }

    public Map<String, Object> getAttributeMap() {
        return getAttributeMap(this.referencePath);
    }
    
    public void setAttributeMap(String referencePath, Map<String, Object> attrMap) {
        this.attributeMap.put(referencePath, attrMap);
    }
    
    public Map<String, Object> getAttributeMap(String referencePath) {
        return this.attributeMap.get(referencePath);
    }

    public void setParameterMap(String referencePath, Map<String, Object> paramMap) {
        this.parameterMap.put(referencePath, paramMap);
    }
    
    public Map<String, Object> getParameterMap(String referencePath) {
        return this.parameterMap.get(referencePath);
    }

    public void setRequestContext(HstRequestContext requestContext) {
        this.requestContext = requestContext;
    }
    
    public HstRequestContext getRequestContext() {
        return this.requestContext;
    }
    
    public void setResourceID(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceID() {
        return this.resourceId;
    }

    public String getReferenceNamespace() {
        return this.referenceNamespace;
    }
    
    public void setReferenceNamespace(String referenceNamespace) {
        this.referenceNamespace =referenceNamespace;
    }
    
}