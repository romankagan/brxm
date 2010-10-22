/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.hst.jaxrs.model.content;

import javax.jcr.RepositoryException;

import org.hippoecm.hst.content.beans.standard.HippoBean;

/**
 * NodeRepresentation
 * @version $Id$
 */
public abstract class NodeRepresentation {
    
    private String name;
    private String localizedName;
    private String path;
    private String primaryNodeTypeName;
    private boolean leaf;
    private String pageLink;
    
    public NodeRepresentation() {    	
    }
    
    public NodeRepresentation represent(HippoBean hippoBean) throws RepositoryException {
		this.name = hippoBean.getName();
		this.localizedName = hippoBean.getLocalizedName();
		
		this.path = hippoBean.getPath();
        
		// TODO: shouldn't primaryNodeType be added to hippoBean interface?
        primaryNodeTypeName = hippoBean.getNode().getPrimaryNodeType().getName(); 
        leaf = hippoBean.isLeaf();
        
        return this;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getLocalizedName() {
    	return localizedName;
    }
    
    public void setLocalizedName(String localizedName) {
    	this.localizedName = localizedName;
    }
    
    public String getPath() {
        return path;
    }
    
    public String getPrimaryNodeTypeName() {
        return primaryNodeTypeName;
    }
    
    public void setPrimaryNodeTypeName(String primaryNodeTypeName) {
        this.primaryNodeTypeName = primaryNodeTypeName;
    }
    
    public boolean isLeaf() {
    	return leaf;
    }
    
    public String getPageLink() {
        return pageLink;
    }
    
    public void setPageLink(String pageLink) {
        this.pageLink = pageLink;
    }
}
