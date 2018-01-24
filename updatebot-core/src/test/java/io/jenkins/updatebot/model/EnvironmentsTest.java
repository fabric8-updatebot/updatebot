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

import io.jenkins.updatebot.Configuration;
import io.fabric8.updatebot.test.Tests;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 */
public class EnvironmentsTest {
    protected Configuration configuration = new Configuration();
    protected String sourceRepoName = "kubernetes-client";

    @Test
    public void testParseEnvironments() throws Exception {
        String configFile = new File(Tests.getBasedir(), "src/test/resources/helm/updatebot.yml").getPath();
        String workDirPath = Tests.getCleanWorkDir(getClass());

        configuration.setConfigFile(configFile);
        configuration.setWorkDir(workDirPath);


        RepositoryConfig repositoryConfig = configuration.loadRepositoryConfig();
        assertThat(repositoryConfig).describedAs("repositoryConfig").isNotNull();
        List<Environment> environments = repositoryConfig.getEnvironments();
        assertThat(environments).describedAs("repositoryConfig.environments").hasSize(2);
        Environment env1 = environments.get(0);
        Environment env2 = environments.get(1);
        assertThat(env1).describedAs("repositoryConfig.environments[0]").isNotNull();
        assertThat(env1.getId()).describedAs("repositoryConfig.environments[0].id").isEqualTo("stage");
        assertThat(env1.getName()).describedAs("repositoryConfig.environments[0].name").isEqualTo("Staging");
        assertThat(env1.getGithub()).describedAs("repositoryConfig.environments[0].github").isEqualTo("jstrachan/test-env-stage");
        assertThat(env2.getId()).describedAs("repositoryConfig.environments[1].id").isEqualTo("prod");
        assertThat(env2.getName()).describedAs("repositoryConfig.environments[1].name").isEqualTo("Production");
        assertThat(env2.getGithub()).describedAs("repositoryConfig.environments[1].github").isEqualTo("jstrachan/test-env-prod");
    }
}
