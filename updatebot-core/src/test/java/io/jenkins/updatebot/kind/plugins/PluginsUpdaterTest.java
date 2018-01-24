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
package io.jenkins.updatebot.kind.plugins;

import io.jenkins.updatebot.Configuration;
import io.jenkins.updatebot.commands.CommandContext;
import io.jenkins.updatebot.kind.Kind;
import io.jenkins.updatebot.model.DependencyVersionChange;
import io.jenkins.updatebot.repository.LocalRepository;
import io.fabric8.updatebot.test.Tests;
import io.fabric8.utils.IOHelpers;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 */
public class PluginsUpdaterTest {
    protected PluginsUpdater updater = new PluginsUpdater();
    protected CommandContext parentContext;
    protected File testDir;
    protected File pluginsFile;
    protected Configuration configuration = new Configuration();

    @Before
    public void init() throws Exception {
        testDir = Tests.copyPackageSources(getClass());
        LocalRepository repository = LocalRepository.fromDirectory(configuration, testDir);
        parentContext = new CommandContext(repository, configuration);
        pluginsFile = Tests.testFile(this.testDir, "plugins.txt");
    }

    @Test
    public void testUpdateDependency() throws Exception {
        assertUpdatePackageJson(pluginsFile, "dependencies", "branch-api", "3.0.0");
    }

    public void assertUpdatePackageJson(File packageJson, String dependencyKey, String artifactId, String version) throws IOException {
        List<DependencyVersionChange> changes = new ArrayList<>();
        changes.add(new DependencyVersionChange(Kind.MAVEN, PluginsUpdater.PLUGIN_DEPENDENCY_PREFIX + artifactId, version));
        updater.pushVersions(parentContext, changes);


        // lets assert that the file contains the correct line
        String expectedLine = artifactId + PluginsUpdater.PLUGINS_SEPARATOR + version;


        List<String> lines = IOHelpers.readLines(pluginsFile);
        assertThat(lines).describedAs("Should have updated the plugin " + artifactId + " to " + version).contains(expectedLine);
    }
}
