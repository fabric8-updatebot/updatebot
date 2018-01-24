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
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 */
public class LoadGithubOrgConfigTest {
    private static final transient Logger LOG = LoggerFactory.getLogger(LoadGithubOrgConfigTest.class);

    Configuration configuration = new Configuration();

    @Test
    public void testLoadConfigFromGithubOrganisation() throws Exception {
        String gitUrl = "git@github.com:jstrachan-testing/doesNotExist.git";
        RepositoryConfig config = loadConfigFromGithubOrg(gitUrl);
        assertThat(config).
                describedAs("Should have found an UpdateBot YAML at the jstrachan-testing-updatebot-config repo from git url " + gitUrl).isNotNull();
    }

    protected RepositoryConfig loadConfigFromGithubOrg(String gitUrl) throws IOException {
        RepositoryConfig config = RepositoryConfigs.loadGithubOrganisationConfig(configuration, gitUrl);
        LOG.info("git url " + gitUrl + " found " + config);
        return config;
    }


    @Test
    public void testCannotLoadConfigurationFromOrgansisationWithoutUpdateBotRepo() throws Exception {
        String gitUrl = "git@github.com:jstrachan/cheese.git";
        RepositoryConfig config = loadConfigFromGithubOrg(gitUrl);
        assertThat(config).
                describedAs("Should not have found an UpdateBot YAML at the jstrachan-updatebot-config repo from git url " + gitUrl).isNull();
    }

}
