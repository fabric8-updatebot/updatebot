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
package io.fabric8.updatebot;


import io.fabric8.updatebot.commands.PushVersionChanges;
import io.fabric8.updatebot.github.GitHubHelpers;
import io.fabric8.updatebot.kind.Kind;
import io.fabric8.updatebot.model.GitRepositoryConfig;
import io.fabric8.updatebot.model.RepositoryConfigs;
import io.fabric8.updatebot.repository.LocalRepository;
import io.fabric8.updatebot.test.Tests;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 */
public class BrewPushSourceDependenciesTest {
    private static final transient Logger LOG = LoggerFactory.getLogger(BrewPushSourceDependenciesTest.class);

    protected PushVersionChanges command;
    protected List<LocalRepository> localRepositories;
    protected Configuration configuration = new Configuration();

    @Before
    public void init() throws IOException {
        command = new PushVersionChanges(Kind.BREW, "jx", "1.0.13");

        String configFile = new File(Tests.getBasedir(), "src/test/resources/brew/source/updatebot.yml").getPath();
        String workDirPath = Tests.getCleanWorkDir(getClass());

        configuration.setConfigFile(configFile);
        configuration.setWorkDir(workDirPath);

        if (System.getProperty("updatebot.disable.pull", "false").equals("true")) {
            LOG.info("Disabling pull to speed up tests");
            configuration.setPullDisabled(true);
        }
        if (Tests.canTestWithGithubAPI(configuration)) {

            localRepositories = command.cloneOrPullRepositories(configuration);

            // lets close all open PRs
            GitHubHelpers.closeOpenUpdateBotIssuesAndPullRequests(configuration.getGithubPullRequestLabel(), localRepositories);
            GitHubHelpers.deleteUpdateBotBranches(localRepositories);
        }
    }

    @Test
    public void testUpdater() throws Exception {
        if (Tests.canTestWithGithubAPI(configuration)) {
            command.run(configuration);
        }
    }

}
