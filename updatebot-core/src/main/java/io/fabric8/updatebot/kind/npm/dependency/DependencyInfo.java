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
package io.fabric8.updatebot.kind.npm.dependency;

import io.fabric8.updatebot.kind.npm.NpmDependencyKinds;
import io.fabric8.updatebot.support.Strings;
import io.fabric8.utils.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 */
public class DependencyInfo {
    private static final transient Logger LOG = LoggerFactory.getLogger(DependencyInfo.class);

    private final String dependency;
    private String version;
    private Map<String, List<DependencyLink>> versions = new TreeMap<>();

    public DependencyInfo(String dependency) {
        this.dependency = dependency;
    }

    @Override
    public String toString() {
        return Strings.notEmpty(version) ? dependency + "@" + version : dependency;
    }

    public DependencyCheck dependencyCheck() {
        if (versions.isEmpty()) {
            return new DependencyCheck(true, "No transient dependencies of " + dependency, this);
        }
        if (versions.keySet().size() == 1 && versions.containsKey(version)) {
            return new DependencyCheck(true, "All transitive dependencies using " + version, this);
        }
        return new DependencyCheck(false, "Direct dependency is " + version + " but has conflicts: " + conflictedDependencyText(), this);
    }

    private String conflictedDependencyText() {
        List<String> messages = new ArrayList<>();
        for (Map.Entry<String, List<DependencyLink>> entry : versions.entrySet()) {
            String key = entry.getKey();
            if (!Objects.equal(version, key)) {
                List<DependencyLink> dependencies = entry.getValue();
                String dependencyNames = dependencies.stream().map(link -> link.getParent().toString()).collect(Collectors.joining(", "));
                messages.add(dependencyNames + " => " + key);
            }
        }
        return String.join(", ", messages);
    }

    public String getDependency() {
        return dependency;
    }

    public Map<String, List<DependencyLink>> getVersions() {
        return versions;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Associates this dependency as a transient dependency of the parent
     *
     * @param parent        the parent who is dependent on this package
     * @param version       the version of this package the parent depends on
     * @param dependencyKey the kind of dependency key as per {@link NpmDependencyKinds}
     */
    public void addDependency(DependencyInfo parent, String version, String dependencyKey) {
        if (version == null) {
            LOG.debug("Dependency " + parent + " does not specify a version for " + this.dependency);
        } else {
            List<DependencyLink> dependencyLinks = versions.get(version);
            if (dependencyLinks == null) {
                dependencyLinks = new ArrayList<>();
                versions.put(version, dependencyLinks);
            }
            dependencyLinks.add(new DependencyLink(parent, this, version, dependencyKey));
        }
    }
}
