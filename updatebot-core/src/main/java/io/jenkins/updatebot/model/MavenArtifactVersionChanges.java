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

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 */
public class MavenArtifactVersionChanges {
    private List<MavenArtifactVersionChange> changes = new ArrayList<>();

    public MavenArtifactVersionChanges() {
    }

    public MavenArtifactVersionChanges(Collection<MavenArtifactVersionChange> changes) {
        this.changes = new ArrayList<>(changes);
    }

    public List<MavenArtifactVersionChange> getChanges() {
        return changes;
    }

    public void setChanges(List<MavenArtifactVersionChange> changes) {
        this.changes = changes;
    }


    public void addChange(DependencyVersionChange change) {
        addChange(new MavenArtifactVersionChange(change));
    }

    public void addChange(MavenArtifactVersionChange change) {
        changes.add(change);
    }

    @JsonIgnore
    public boolean isEmpty() {
        return changes.isEmpty();
    }
}
