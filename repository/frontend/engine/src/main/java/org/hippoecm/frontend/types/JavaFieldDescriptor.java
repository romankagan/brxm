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
package org.hippoecm.frontend.types;

import java.util.Set;

public class JavaFieldDescriptor implements IFieldDescriptor {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private String type;
    private String path;

    private Set<String> excluded;

    private boolean multiple;
    private boolean binary;
    private boolean protect;
    private boolean mandatory;
    private boolean ordered;
    private boolean primary;

    public JavaFieldDescriptor(String path) {
        this.type = null;
        this.path = path;
        this.excluded = null;

        multiple = protect = binary = mandatory = ordered = primary = false;
    }

    public String getName() {
        return path;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setMultiple(boolean multiple) {
        this.multiple = multiple;
    }

    public boolean isMultiple() {
        return multiple;
    }

    public boolean isBinary() {
        return binary;
    }

    public boolean isProtected() {
        return protect;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    public boolean isOrdered() {
        return ordered;
    }

    public void setOrdered(boolean isOrdered) {
        this.ordered = isOrdered;
    }

    public boolean isPrimary() {
        return primary;
    }

    public Set<String> getExcluded() {
        return excluded;
    }

    public void setExcluded(Set<String> set) {
        excluded = set;
    }

    void setPrimary(boolean isPrimary) {
        this.primary = isPrimary;
    }

    public void detach() {
    }
}
