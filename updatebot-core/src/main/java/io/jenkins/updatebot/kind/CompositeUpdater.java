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
package io.jenkins.updatebot.kind;

import io.jenkins.updatebot.commands.CommandContext;
import io.jenkins.updatebot.model.Dependencies;
import io.jenkins.updatebot.model.DependencyVersionChange;

import java.io.IOException;
import java.util.List;

/**
 */
public class CompositeUpdater implements Updater {

    public CompositeUpdater() {
    }

    @Override
    public boolean isApplicable(CommandContext context) {
        return true;
    }

    @Override
    public boolean pushVersions(CommandContext context, List<DependencyVersionChange> changes) throws IOException {
        Kind[] kinds = Kind.values();
        boolean answer = false;
        for (Kind kind : kinds) {
            Updater updater = kind.getUpdater();
            if (updater.isApplicable(context)) {
                if (updater.pushVersions(context, changes)) {
                    answer = true;
                }
            }
        }
        return answer;

    }

    @Override
    public boolean pullVersions(CommandContext context) throws IOException {
        Kind[] kinds = Kind.values();
        boolean answer = false;
        for (Kind kind : kinds) {
            Updater updater = kind.getUpdater();
            if (updater.isApplicable(context)) {
                if (updater.pullVersions(context)) {
                    answer = true;
                }
            }
        }
        return answer;
    }

    @Override
    public void addVersionChangesFromSource(CommandContext context, Dependencies dependencyConfig, List<DependencyVersionChange> list) throws IOException {
        Kind[] kinds = Kind.values();
        for (Kind kind : kinds) {
            Updater updater = kind.getUpdater();
            if (updater.isApplicable(context)) {
                updater.addVersionChangesFromSource(context, dependencyConfig, list);
            }
        }
    }

    @Override
    public KindDependenciesCheck checkDependencies(CommandContext context, List<DependencyVersionChange> changes) throws IOException {
        // TODO does not handle differnet kinds!!!
        KindDependenciesCheck answer = new KindDependenciesCheck();
        Kind[] kinds = Kind.values();
        for (Kind kind : kinds) {
            Updater updater = kind.getUpdater();
            if (updater.isApplicable(context)) {
                KindDependenciesCheck result = updater.checkDependencies(context, changes);
                answer.append(result);
            }
        }
        return answer;
    }
}
