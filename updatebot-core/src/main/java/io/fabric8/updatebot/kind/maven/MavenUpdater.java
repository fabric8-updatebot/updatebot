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
package io.fabric8.updatebot.kind.maven;

import io.fabric8.updatebot.Configuration;
import io.fabric8.updatebot.commands.CommandContext;
import io.fabric8.updatebot.kind.Updater;
import io.fabric8.updatebot.kind.UpdaterSupport;
import io.fabric8.updatebot.model.Dependencies;
import io.fabric8.updatebot.model.DependencyVersionChange;
import io.fabric8.updatebot.model.MavenArtifactVersionChange;
import io.fabric8.updatebot.model.MavenArtifactVersionChanges;
import io.fabric8.updatebot.support.FileHelper;
import io.fabric8.updatebot.support.MarkupHelper;
import io.fabric8.updatebot.support.ProcessHelper;
import io.fabric8.updatebot.support.VersionHelper;
import io.fabric8.utils.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

/**
 */
public class MavenUpdater extends UpdaterSupport implements Updater {
    private static final transient Logger LOG = LoggerFactory.getLogger(MavenUpdater.class);

    @Override
    public boolean isApplicable(CommandContext context) {
        boolean answer = FileHelper.isFile(context.file("pom.xml"));
        if (answer) {
            // lets verify we have a maven install
            Configuration configuration = context.getConfiguration();
            String mvnCommand = configuration.getMvnCommand();
            int returnCode = ProcessHelper.runCommandIgnoreOutput(context.getDir(), configuration.getMvnEnvironmentVariables(), mvnCommand, "-v");
            if (returnCode != 0) {
                context.warn(LOG, "Could not invoke Maven!. Command failed: " + mvnCommand + " -v => " + returnCode);
                context.warn(LOG, "Please verify you have `mvn` on your PATH or you have configured Maven property");
                return false;
            }
        }
        return answer;
    }

    @Override
    public void addVersionChangesFromSource(CommandContext context, Dependencies dependencyConfig, List<DependencyVersionChange> list) throws IOException {
        File file = context.file("pom.xml");
        if (Files.isFile(file)) {
            // lets run the maven plugin to generate the export versions file
            Configuration configuration = context.getConfiguration();
            String configFile = configuration.getConfigFile();
            File versionsFile = createVersionsYamlFile(context);
            Map<String, String> env = configuration.getMvnEnvironmentVariables();
            String mvnCommand = configuration.getMvnCommand();
            String updateBotPluginVersion = VersionHelper.updateBotVersion();
            if (ProcessHelper.runCommandAndLogOutput(context.getConfiguration(), LOG, context.getDir(), env, mvnCommand,
                    "-B",
                    "io.fabric8.updatebot:updatebot-maven-plugin:" + updateBotPluginVersion + ":export",
                    "-DdestFile=" + versionsFile, "-DupdateBotYaml=" + configFile)) {
                if (!Files.isFile(versionsFile)) {
                    LOG.warn("Should have generated the export versions file " + versionsFile);
                    return;
                }

                MavenArtifactVersionChanges changes;
                try {
                    changes = MarkupHelper.loadYaml(versionsFile, MavenArtifactVersionChanges.class);
                } catch (IOException e) {
                    throw new IOException("Failed to load " + versionsFile + ". " + e, e);
                }
                List<MavenArtifactVersionChange> changeList = changes.getChanges();
                if (list != null) {
                    for (MavenArtifactVersionChange change : changeList) {
                        list.add(change.createDependencyVersionChange());
                    }

                    PrintStream printStream = configuration.getPrintStream();
                    if (!changeList.isEmpty() && printStream != null) {
                        printStream.println("\n");
                    }
                }
            }
        }
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

    protected File createVersionsYamlFile(CommandContext context) {
        return new File(context.getDir(), "target/updatebot-versions.yaml");
    }

}
