/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.repository.documentworkflow.task;

import java.io.Serializable;

import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.api.WorkflowException;
import org.onehippo.repository.documentworkflow.DocumentHandle;

/**
 * HintTask sets or removes a DocumentHandle (dm context variable) hints key
 */
public class HintTask extends AbstractDocumentTask {

    private static final long serialVersionUID = 1L;

    private String hint;
    private Serializable value;

    public String getHint() {
        return hint;
    }

    public void setHint(final String hint) {
        this.hint = hint;
    }

    public Serializable getValue() {
        return value;
    }

    public void setValue(Serializable value) {
        this.value = value;
    }

    @Override
    public Object doExecute() throws WorkflowException, RepositoryException {
        if (StringUtils.isBlank(getHint())) {
            throw new WorkflowException("No hint specified");
        }

        DocumentHandle dm = getDataModel();

        if (getValue() == null) {
            dm.getHints().remove(getHint());
        } else {
            dm.getHints().put(getHint(), getValue());
        }

        return null;
    }

}
