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
package io.jenkins.updatebot.model;

/**
 */
public class MavenArtifactKey implements Comparable<MavenArtifactKey> {
    private final String groupId;
    private final String artifactId;

    public MavenArtifactKey(String groupId, String artifactId) {
        this.groupId = groupId;
        this.artifactId = artifactId;
    }

    /**
     * Returns a maven dependency from the given string using <code>:</code> to separate the group id and artifact
     */
    public static MavenArtifactKey fromString(String value) {
        int idx = value.indexOf(':');
        if (idx < 0) {
            throw new IllegalArgumentException("No `:` character in the maven dependency: " + value);
        }
        return new MavenArtifactKey(value.substring(0, idx), value.substring(idx + 1));
    }

    @Override
    public String toString() {
        return groupId + ":" + artifactId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MavenArtifactKey that = (MavenArtifactKey) o;

        if (!groupId.equals(that.groupId)) return false;
        return artifactId.equals(that.artifactId);
    }

    @Override
    public int hashCode() {
        int result = groupId.hashCode();
        result = 31 * result + artifactId.hashCode();
        return result;
    }

    @Override
    public int compareTo(MavenArtifactKey that) {
        int answer = this.groupId.compareTo(that.groupId);
        if (answer == 0) {
            answer = this.artifactId.compareTo(that.artifactId);
        }
        return answer;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }
}
