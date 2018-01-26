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
package io.fabric8.updatebot.filter;

import io.jenkins.updatebot.model.Dependencies;
import io.jenkins.updatebot.model.GitRepositoryConfig;
import io.jenkins.updatebot.model.MavenArtifactKey;
import io.jenkins.updatebot.model.MavenDependencies;
import io.jenkins.updatebot.model.MavenDependencyFilter;
import io.jenkins.updatebot.model.RepositoryConfig;
import io.fabric8.updatebot.test.Tests;
import io.fabric8.utils.Filter;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 */
public class FilterMavenTest {

    @Test
    public void testIncludes() throws Exception {
        File config = Tests.testFile(Tests.getBasedir(), "src/test/resources/maven/filter/updatebot.yml");


        RepositoryConfig repositoryConfig = Tests.assertLoadProjects(config);

        String cloneUrl = "https://github.com/jstrachan-testing/updatebot.git";
        GitRepositoryConfig updateBotDetails = repositoryConfig.getRepositoryDetails(cloneUrl);
        assertThat(updateBotDetails).describedAs("Should have found project details from cloneUrl " + cloneUrl).isNotNull();

        GitRepositoryConfig repository = Tests.assertGithubRepositoryFindByName(repositoryConfig, "updatebot");

        Dependencies push = repository.getPush();
        assertThat(push).describedAs("push configuration for " + repository).isNotNull();

        MavenDependencies maven = push.getMaven();
        assertThat(maven).describedAs("maven push configuration for " + repository).isNotNull();

        List<MavenDependencyFilter> mavenDependencies = maven.getDependencies();
        assertThat(mavenDependencies).describedAs("maven dependencies push configuration for " + repository).isNotNull();


        assertFilterDependency(true, mavenDependencies, "org.springframework:something", "org.apache.maven:whatnot", "org.apache:cheese");
        assertFilterDependency(false, mavenDependencies, "cheese:edam", "org.springframework.orm:cheese");
    }

    private void assertFilterDependency(boolean expected, List<MavenDependencyFilter> dependencies, String... values) {
        Filter<MavenArtifactKey> filter = MavenDependencyFilter.createFilter(dependencies);
        for (String value : values) {
            MavenArtifactKey dependency = MavenArtifactKey.fromString(value);
            boolean actual = filter.matches(dependency);
            assertThat(actual).
                    describedAs("Dependency " + dependency + " with Filter " + filter + " from  " + dependencies).
                    isEqualTo(expected);
        }
    }
}
