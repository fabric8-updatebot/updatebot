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
package io.fabric8.updatebot.kind.helm;

import io.fabric8.updatebot.Configuration;
import io.fabric8.updatebot.commands.CommandContext;
import io.fabric8.updatebot.kind.Kind;
import io.fabric8.updatebot.kind.helm.model.ChartDependency;
import io.fabric8.updatebot.kind.helm.model.Requirements;
import io.fabric8.updatebot.model.DependencyVersionChange;
import io.fabric8.updatebot.repository.LocalRepository;
import io.fabric8.updatebot.support.MarkupHelper;
import io.fabric8.updatebot.test.Tests;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 */
public class HelmUpdaterTest {
    protected HelmUpdater updater = new HelmUpdater();
    protected CommandContext parentContext;
    protected File testDir;
    protected File requirementsYaml;
    protected Configuration configuration = new Configuration();

    @Before
    public void init() throws Exception {
        testDir = Tests.copyPackageSources(getClass());
        parentContext = new CommandContext(LocalRepository.fromDirectory(configuration, testDir), configuration);
        requirementsYaml = Tests.testFile(this.testDir, HelmFiles.REQUIREMENTS_YAML);
    }

    @Test
    public void testUpdateDependency() throws Exception {
        assertUpdateHelm(requirementsYaml, "subchart2", "0.2.1");
    }


    public void assertUpdateHelm(File requirementsYaml, String name, String version) throws IOException {
        assertThat(this.requirementsYaml).exists().isFile();

        List<DependencyVersionChange> changes = new ArrayList<>();
        changes.add(new DependencyVersionChange(Kind.HELM, name, version));
        updater.pushVersions(parentContext, changes);

        Requirements requirements = MarkupHelper.loadYaml(requirementsYaml, Requirements.class);
        ChartDependency dependency = requirements.dependency(name);
        assertThat(dependency).describedAs("Should find a dependency for name " + name).isNotNull();
        String updatedVersion = dependency.getVersion();
        assertThat(updatedVersion).describedAs("Dependency " + name + " version").isEqualTo(version);

        System.out.println("Updated file " + requirementsYaml + " " + name + " to version " + updatedVersion);
    }

}
