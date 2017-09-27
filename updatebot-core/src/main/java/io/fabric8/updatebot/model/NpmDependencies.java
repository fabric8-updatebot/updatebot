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

/**
 * Represents the push or pull dependencies for NPM based projects
 */
public class NpmDependencies {
    private DependencySet dependencies = new DependencySet();
    private DependencySet devDependencies = new DependencySet();
    private DependencySet peerDependencies = new DependencySet();

    public DependencySet getDependencies() {
        return dependencies;
    }

    public void setDependencies(DependencySet dependencies) {
        this.dependencies = dependencies;
    }

    public DependencySet getDevDependencies() {
        return devDependencies;
    }

    public void setDevDependencies(DependencySet devDependencies) {
        this.devDependencies = devDependencies;
    }

    public DependencySet getPeerDependencies() {
        return peerDependencies;
    }

    public void setPeerDependencies(DependencySet peerDependencies) {
        this.peerDependencies = peerDependencies;
    }
}
