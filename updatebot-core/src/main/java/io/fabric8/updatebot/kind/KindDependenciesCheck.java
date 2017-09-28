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
package io.fabric8.updatebot.kind;

import io.fabric8.updatebot.kind.npm.dependency.DependencyCheck;
import io.fabric8.updatebot.model.DependencyVersionChange;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Returns the set of changes that are valid or invalid
 */
public class KindDependenciesCheck {
    private final List<DependencyVersionChange> validChanges;
    private final List<DependencyVersionChange> invalidChanges;
    private final Map<String, DependencyCheck> failedChecks;

    public KindDependenciesCheck() {
        this.validChanges = new ArrayList<>();
        this.invalidChanges = new ArrayList<>();
        this.failedChecks = new LinkedHashMap<>();
    }

    public KindDependenciesCheck(List<DependencyVersionChange> validChanges, List<DependencyVersionChange> invalidChanges, Map<String, DependencyCheck> failedChecks) {
        this.validChanges = validChanges;
        this.invalidChanges = invalidChanges;
        this.failedChecks = failedChecks;
    }

    public List<DependencyVersionChange> getValidChanges() {
        return validChanges;
    }

    public List<DependencyVersionChange> getInvalidChanges() {
        return invalidChanges;
    }

    public Map<String, DependencyCheck> getFailedChecks() {
        return failedChecks;
    }

    public void append(KindDependenciesCheck that) {
        this.validChanges.addAll(that.validChanges);
        this.invalidChanges.addAll(that.invalidChanges);
        this.failedChecks.putAll(that.failedChecks);
    }
}
