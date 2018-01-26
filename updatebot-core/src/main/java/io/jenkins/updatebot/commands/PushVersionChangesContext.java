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
package io.jenkins.updatebot.commands;

import io.jenkins.updatebot.kind.Kind;
import io.jenkins.updatebot.model.DependencyVersionChange;
import io.jenkins.updatebot.support.Markdown;
import io.fabric8.utils.Objects;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class PushVersionChangesContext extends CommandContext {
    private final DependencyVersionChange step;
    private List<Change> changes = new ArrayList<>();

    public PushVersionChangesContext(CommandContext parentContext, DependencyVersionChange step) {
        super(parentContext);
        this.step = step;
    }

    @Override
    public String toString() {
        return "PushVersionContext{" +
                "kind='" + getKind() + '\'' +
                ", name='" + getName() + '\'' +
                ", value='" + getValue() + '\'' +
                '}';
    }

    @Override
    public String createPullRequestBody() {
        return Markdown.UPDATEBOT_ICON + " pushed " + step.getKind() + " dependency: `" + step.getDependency() + "` to: `" + step.getVersion() + "`";
    }

    @Override
    public String createCommit() {
        return "fix(version): update " + step.getDependency() + " to " + step.getVersion();
    }

    @Override
    public String createPullRequestTitle() {
        return createPullRequestTitlePrefix() + step.getVersion();
    }

    @Override
    public String createPullRequestTitlePrefix() {
        return "update " + step.getDependency() + " to ";
    }

    public DependencyVersionChange getStep() {
        return step;
    }

    public Kind getKind() {
        return step.getKind();
    }

    public String getName() {
        return step.getDependency();
    }

    public String getValue() {
        return step.getVersion();
    }

    public List<Change> getChanges() {
        return changes;
    }

    public void updatedVersion(String dependencyKey, String name, String newValue, String oldValue) {
        changes.add(new Change(dependencyKey, name, newValue, oldValue));
    }

    /**
     * Returns the change for the given name or null if there is none
     */
    public Change change(String name) {
        for (Change change : changes) {
            if (Objects.equal(name, change.getName())) {
                return change;
            }
        }
        return null;
    }

    public static class Change {
        private final String scope;
        private final String name;
        private final String newValue;
        private final String oldValue;

        public Change(String scope, String name, String newValue, String oldValue) {
            this.scope = scope;
            this.name = name;
            this.newValue = newValue;
            this.oldValue = oldValue;
        }

        @Override
        public String toString() {
            return "Change{" +
                    "scope='" + scope + '\'' +
                    ", name='" + name + '\'' +
                    ", newValue='" + newValue + '\'' +
                    ", oldValue='" + oldValue + '\'' +
                    '}';
        }

        public String getScope() {
            return scope;
        }

        public String getName() {
            return name;
        }

        public String getNewValue() {
            return newValue;
        }

        public String getOldValue() {
            return oldValue;
        }
    }
}
