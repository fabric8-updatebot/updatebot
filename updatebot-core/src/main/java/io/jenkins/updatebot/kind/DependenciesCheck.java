/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.jenkins.updatebot.kind;

import io.jenkins.updatebot.model.DependencyVersionChange;

import java.util.List;
import java.util.Map;

/**
 * Returns the set of changes that are valid or invalid for all kinds
 */
public class DependenciesCheck {
    private final List<DependencyVersionChange> validChanges;
    private final List<DependencyVersionChange> invalidChanges;
    private final Map<Kind, KindDependenciesCheck> failures;

    public DependenciesCheck(List<DependencyVersionChange> validChanges, List<DependencyVersionChange> invalidChanges, Map<Kind, KindDependenciesCheck> failures) {
        this.validChanges = validChanges;
        this.invalidChanges = invalidChanges;
        this.failures = failures;
    }

    public List<DependencyVersionChange> getValidChanges() {
        return validChanges;
    }

    public List<DependencyVersionChange> getInvalidChanges() {
        return invalidChanges;
    }

    public Map<Kind, KindDependenciesCheck> getFailures() {
        return failures;
    }
}
