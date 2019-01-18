/*
 *  Copyright 2009-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.validation;

import java.util.LinkedHashSet;
import java.util.Set;

public class ValidationResult implements IValidationResult {

    private Set<Violation> violations;

    public ValidationResult() {
        this(new LinkedHashSet<>());
    }

    public ValidationResult(final Set<Violation> violations) {
        this.violations = violations;
    }

    public Set<Violation> getViolations() {
        return violations;
    }

    public void setViolations(final Set<Violation> violations) {
        this.violations = violations;
    }

    public boolean isValid() {
        return violations.isEmpty();
    }

    public void detach() {
        violations.forEach(Violation::detach);
    }

}
