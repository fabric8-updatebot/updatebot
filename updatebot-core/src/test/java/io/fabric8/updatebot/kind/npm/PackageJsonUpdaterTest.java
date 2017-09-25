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
package io.fabric8.updatebot.kind.npm;

import com.fasterxml.jackson.databind.JsonNode;
import io.fabric8.updatebot.commands.UpdateContext;
import io.fabric8.updatebot.commands.UpdateVersionContext;
import io.fabric8.updatebot.repository.LocalRepository;
import io.fabric8.updatebot.support.MarkupHelper;
import io.fabric8.updatebot.test.Tests;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static io.fabric8.updatebot.test.MarkupAssertions.assertTextValue;
import static org.assertj.core.api.Assertions.assertThat;

/**
 */
public class PackageJsonUpdaterTest {
    protected PackageJsonUpdater updater = new PackageJsonUpdater();
    protected UpdateContext parentContext;
    protected File testDir;
    protected File packageJson;

    @Before
    public void init() throws Exception {
        testDir = Tests.copyPackageSources(getClass());
        parentContext = new UpdateContext(LocalRepository.fromDirectory(testDir));
         packageJson = Tests.testFile(this.testDir, "package.json");
    }

    @Test
    public void testUpdateDependency() throws Exception {
        assertUpdatePackageJson(packageJson, "dependencies", "@angular/core", "4.3.7");
    }

    @Test
    public void testUpdateDevDependency() throws Exception {
        assertUpdatePackageJson(packageJson, "devDependencies", "@angular/compiler", "4.3.7");
    }

    public void assertUpdatePackageJson(File packageJson, String dependencyKey, String name, String version) throws IOException {
        UpdateVersionContext context = parentContext.updateVersion(name, version);
        assertThat(updater.isApplicable(context)).
                describedAs("File should be applicable " + packageJson).
                isTrue();
        updater.pushVersions(context);

        UpdateVersionContext.Change change = context.change(name);
        assertThat(change).describedAs("expected change for name " + name).isNotNull();
        assertThat(change.getNewValue()).isEqualTo(version);

        JsonNode tree = MarkupHelper.loadJson(packageJson);
        String updatedVersion = assertTextValue(tree, dependencyKey, name);
        assertThat(updatedVersion).
                describedAs("updated version of " + name).
                isEqualTo(version);
        System.out.println("Updated file " + packageJson + " " + dependencyKey + " " + name + " to version " + updatedVersion);
    }

}
