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

import io.fabric8.updatebot.commands.PushVersionChangesContext;
import io.fabric8.updatebot.kind.Kind;

/**
 */
public class MavenArtifactVersionChange {
    private String groupId;
    private String artifactId;
    private String version;
    private String scope;

    public MavenArtifactVersionChange() {
    }

    public MavenArtifactVersionChange(String groupId, String artifactId, String version, String scope) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.scope = scope;
    }

    public MavenArtifactVersionChange(MavenArtifactKey artifactKey, String version, String scope) {
        this(artifactKey.getGroupId(), artifactKey.getArtifactId(), version, scope);
    }

    public MavenArtifactVersionChange(DependencyVersionChange change) {
        this(MavenArtifactKey.fromString(change.getDependency()), change.getVersion(), change.getScope());
    }

    @Override
    public String toString() {
        return "MavenArtifactVersionChange{" +
                "groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", version='" + version + '\'' +
                ", scope='" + scope + '\'' +
                '}';
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public DependencyVersionChange createDependencyVersionChange() {
        return new DependencyVersionChange(Kind.MAVEN, new MavenArtifactKey(groupId, artifactId).toString(), version, scope);
    }
}
