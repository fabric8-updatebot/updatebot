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
package io.jenkins.updatebot.kind.helm.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.jenkins.updatebot.model.DependencyVersionChange;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents the <a href="https://github.com/kubernetes/helm">Helm</a> requirements.yaml file
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Requirements {
    @JsonProperty
    private List<ChartDependency> dependencies = new ArrayList<>();

    public Requirements() {
    }


    /**
     * Returns the dependency for the given name or null if it cannot be found
     */
    public ChartDependency dependency(String name) {
        if (dependencies != null) {
            for (ChartDependency dependency : dependencies) {
                if (Objects.equals(name, dependency.getName())) {
                    return dependency;
                }
            }
        }
        return null;
    }

    public List<ChartDependency> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<ChartDependency> dependencies) {
        this.dependencies = dependencies;
    }

    public boolean applyChanges(Iterable<DependencyVersionChange> changes) {
        boolean answer = false;
        for (DependencyVersionChange change : changes) {
            if (applyChange(change)) {
                answer = true;
            }
        }
        return answer;
    }

    public boolean applyChange(DependencyVersionChange change) {
        String name = change.getDependency();
        String version = change.getVersion();
        ChartDependency dependency = dependency(name);
        if (dependency != null) {
            String oldVersion = dependency.getVersion();
            if (oldVersion == null || !Objects.equals(oldVersion, version)) {
                dependency.setVersion(version);
                return true;
            }
        }
        return false;

    }
}
