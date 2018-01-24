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
package io.jenkins.updatebot.kind.file;

import io.jenkins.updatebot.commands.CommandContext;
import io.jenkins.updatebot.kind.Updater;
import io.jenkins.updatebot.kind.UpdaterSupport;
import io.jenkins.updatebot.kind.maven.PomHelper;
import io.jenkins.updatebot.model.Dependencies;
import io.jenkins.updatebot.model.DependencyVersionChange;
import io.jenkins.updatebot.model.FileDependencies;
import io.jenkins.updatebot.model.GitRepositoryConfig;
import io.fabric8.utils.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Performs updates on files using regular expressions
 */
public class FileUpdater extends UpdaterSupport implements Updater {
    private static final transient Logger LOG = LoggerFactory.getLogger(FileUpdater.class);

    @Override
    public boolean isApplicable(CommandContext context) {
        GitRepositoryConfig details = context.getRepository().getRepo().getRepositoryDetails();
        if (details != null) {
            Dependencies push = details.getPush();
            if (push != null) {
                FileDependencies file = push.getFile();
                if (file != null && !file.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void addVersionChangesFromSource(CommandContext context, Dependencies dependencyConfig, List<DependencyVersionChange> list) throws IOException {
    }


    @Override
    public boolean pushVersions(CommandContext context, List<DependencyVersionChange> changes) throws IOException {
        File file = context.file("pom.xml");
        boolean answer = false;
        if (Files.isFile(file)) {
            if (PomHelper.updatePomVersionsInPoms(context.getDir(), changes)) {
                return true;
            }
        }
        return answer;

    }


}
