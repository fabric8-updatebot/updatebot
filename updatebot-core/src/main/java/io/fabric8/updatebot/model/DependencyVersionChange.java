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
package io.fabric8.updatebot.model;

import io.fabric8.updatebot.kind.Kind;
import io.fabric8.utils.Objects;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents the update of a dependency version with an optional scope
 */
public class DependencyVersionChange {
    private final Kind kind;
    private final String dependency;
    private final String version;
    private final String scope;

    public DependencyVersionChange(Kind kind, String dependency, String version) {
        this(kind, dependency, version, null);
    }


    public DependencyVersionChange(Kind kind, String dependency, String version, String scope) {
        this.kind = kind;
        this.dependency = dependency;
        this.version = version;
        this.scope = scope;
    }

    public static String describe(List<DependencyVersionChange> changes) {
        if (changes.isEmpty()) {
            return "no changes";
        }
        return changes.stream().map(c -> c.getDependency() + " => " + c.getVersion()).collect(Collectors.joining(", "));
    }

    public static boolean hasDependency(List<DependencyVersionChange> changes, DependencyVersionChange change) {
        return changes.stream().anyMatch(c -> c.matches(change));
    }

    /**
     * Returns the dependency version changes for the given kind
     */
    public static List<DependencyVersionChange> forKind(Kind kind, List<DependencyVersionChange> list) {
        return list.stream().filter(d -> kind.equals(d.getKind())).collect(Collectors.toList());
    }

    /**
     * Returns the dependency version changes by kind
     */
    public static Map<Kind, List<DependencyVersionChange>> byKind(List<DependencyVersionChange> list) {
        Map<Kind, List<DependencyVersionChange>> answer = new LinkedHashMap<>();
        for (DependencyVersionChange change : list) {
            Kind key = change.getKind();
            List<DependencyVersionChange> changes = answer.get(key);
            if (changes == null) {
                changes = new ArrayList<>();
                answer.put(key, changes);
            }
            changes.add(change);
        }
        return answer;
    }

    @Override
    public String toString() {
        return "DependencyVersionChange{" +
                "kind=" + kind +
                ", dependency='" + dependency + '\'' +
                ", version='" + version + '\'' +
                ", scope='" + scope + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DependencyVersionChange that = (DependencyVersionChange) o;

        if (kind != that.kind) return false;
        if (!dependency.equals(that.dependency)) return false;
        if (!version.equals(that.version)) return false;
        return scope != null ? scope.equals(that.scope) : that.scope == null;
    }

    @Override
    public int hashCode() {
        int result = kind.hashCode();
        result = 31 * result + dependency.hashCode();
        result = 31 * result + version.hashCode();
        result = 31 * result + (scope != null ? scope.hashCode() : 0);
        return result;
    }

    /**
     * Returns true if the given change is for the same kind and dependency
     */
    public boolean matches(DependencyVersionChange that) {
        return Objects.equal(this.kind, that.kind) && Objects.equal(this.dependency, that.dependency);
    }

    /**
     * Returns true if this change matches the given artifact key
     */
    public boolean matches(String groupId, String artifactId) {
        return matches(new MavenArtifactKey(groupId, artifactId));
    }

    /**
     * Returns true if this change matches the given artifact key
     */
    public boolean matches(MavenArtifactKey artifactKey) {
        return Objects.equal(this.dependency, artifactKey.toString());
    }

    public Kind getKind() {
        return kind;
    }

    public String getDependency() {
        return dependency;
    }

    public String getVersion() {
        return version;
    }

    public String getScope() {
        return scope;
    }

}
