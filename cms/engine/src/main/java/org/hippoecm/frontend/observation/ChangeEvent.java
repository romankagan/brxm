/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.observation;

import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

class ChangeEvent implements Event {

    private final String userID;
    private final String nodePath;

    ChangeEvent(final String nodePath, final String userID) {
        this.nodePath = nodePath;
        this.userID = userID;
    }

    public String getPath() throws RepositoryException {
        return nodePath;
    }

    public int getType() {
        return 0;
    }

    public String getUserID() {
        return userID;
    }

    public String getIdentifier() throws RepositoryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Map getInfo() throws RepositoryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getUserData() throws RepositoryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public long getDate() throws RepositoryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}