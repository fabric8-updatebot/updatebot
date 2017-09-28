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

/**
 * A dependency link from a parent dependency to a child dependency
 */
public class DependencyLink {
    private final DependencyInfo parent;
    private final DependencyInfo child;
    private final String version;
    private final String dependencyKind;

    public DependencyLink(DependencyInfo parent, DependencyInfo child, String version, String dependencyKind) {
        this.parent = parent;
        this.child = child;
        this.version = version;
        this.dependencyKind = dependencyKind;
    }

    @Override
    public String toString() {
        return "DependencyLink{" +
                "parent=" + parent +
                ", child=" + child +
                ", version='" + version + '\'' +
                ", dependencyKind='" + dependencyKind + '\'' +
                '}';
    }

    public DependencyInfo getParent() {
        return parent;
    }

    public DependencyInfo getChild() {
        return child;
    }

    public String getVersion() {
        return version;
    }

    public String getDependencyKind() {
        return dependencyKind;
    }
}
