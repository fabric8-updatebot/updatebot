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
 */
public class MavenDependency {
    private final String groupId;
    private final String artifactId;

    public MavenDependency(String groupId, String artifactId) {
        this.groupId = groupId;
        this.artifactId = artifactId;
    }

    /**
     * Returns a maven dependency from the given string using <code>:</code> to separate the group id and artifact
     */
    public static MavenDependency fromString(String value) {
        int idx = value.indexOf(':');
        if (idx < 0) {
            throw new IllegalArgumentException("No `:` character in the maven dependency: " + value);
        }
        return new MavenDependency(value.substring(0, idx), value.substring(idx + 1));
    }

    @Override
    public String toString() {
        return groupId + ":" + artifactId;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }
}
