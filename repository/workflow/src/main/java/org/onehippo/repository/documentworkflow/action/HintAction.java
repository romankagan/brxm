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

package org.onehippo.repository.documentworkflow.action;

import java.io.Serializable;
import java.util.Map;

import org.onehippo.repository.documentworkflow.task.HintTask;
import org.onehippo.repository.scxml.AbstractTaskAction;

/**
 * HintAction delegates the execution to HintTask.
 */
public class HintAction extends AbstractTaskAction<HintTask> {

    private static final long serialVersionUID = 1L;

    public String getHint() {
        return getPropertiesMap().get("hint");
    }

    public void setHint(final String hint) {
        getPropertiesMap().put("hint", hint);
    }

    public String getValue() {
        return (String) getRuntimePropertiesMap().get("value");
    }

    public void setValue(final String value) {
        getRuntimePropertiesMap().put("value", value);
    }

    @Override
    protected HintTask createWorkflowTask() {
        return new HintTask();
    }

    @Override
    protected void initTaskBeforeEvaluation(HintTask task, Map<String, String> propertiesMap) {
        super.initTaskBeforeEvaluation(task, propertiesMap);
        task.setHint(propertiesMap.get("hint"));
    }

    @Override
    protected void initTaskAfterEvaluation(HintTask task, Map<String, Object> runtimePropertiesMap) {
        super.initTaskAfterEvaluation(task, runtimePropertiesMap);

        Serializable value = (Serializable) runtimePropertiesMap.get("value");
        task.setValue(value);
    }
}
