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

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstComponentException;

/**
 * HST Component Window.
 * This interface represents a fragment window of a page which is generated by a HstComponent.
 * 
 * @version $Id$
 */
public interface HstComponentWindow {

    /**
     * The reference name of the component window.
     * 
     * @return the reference name of the component window
     */
    String getReferenceName();
    
    /**
     * The reference namespace of the component window.
     * 
     * @return the reference namespace of the component window
     */
    String getReferenceNamespace();

    /**
     * The actual HstComponent instance.
     * 
     * @return the actual HstComponent instance
     */
    HstComponent getComponent();
    
    /**
     * Whether it has component exceptions or not
     * 
     * @return
     */
    boolean hasComponentExceptions();
    
    /**
     * The component exceptions during initialization or runtime.
     * 
     * @return the possible component exception list
     */
    List<HstComponentException> getComponentExceptions();
    
    /**
     * Adds a component exceptions during initialization or runtime.
     */
    void addComponentExcpetion(HstComponentException e);
    
    /**
     * Adds a component exceptions during initialization or runtime.
     */
    void clearComponentExceptions();
    
    /**
     * The dispatching path path to render this component window.
     * 
     * @return the dispatching path to render this component window
     */
    String getRenderPath(); 
    
    /**
     * The dispatching path path to serve resource in this component window.
     * 
     * @return the dispatching path to serve resource in this component window
     */
    String getServeResourcePath();
    
    /**
     * The parent component window containing this component window.
     * 
     * @return the component window containing this component window
     */
    HstComponentWindow getParentWindow();
    
    /**
     * The child component windows contained in this component window.
     * 
     * @return the component windows contained in this component window
     */
    Map<String, HstComponentWindow> getChildWindowMap();
    
    /**
     * The child component window which can be accessed by the path.
     * 
     * @param referenceName the referenceName of the child component window
     * @return the child component window which has the referenceName
     */
    HstComponentWindow getChildWindow(String referenceName);
    
    /**
     * Flushes the output content of this component window
     * 
     * @throws IOException
     */
    void flushContent() throws IOException;
    
}
