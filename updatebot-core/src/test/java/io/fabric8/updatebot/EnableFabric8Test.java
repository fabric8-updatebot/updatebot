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


import io.fabric8.updatebot.commands.EnableFabric8;
import io.fabric8.updatebot.github.GitHubHelpers;
import io.fabric8.updatebot.repository.LocalRepository;
import io.fabric8.updatebot.test.Tests;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 */
public class EnableFabric8Test {
    private static final transient Logger LOG = LoggerFactory.getLogger(EnableFabric8Test.class);

    protected EnableFabric8 enableFabric8 = new EnableFabric8();
    protected List<LocalRepository> localRepositories;
    protected Configuration configuration = new Configuration();
    protected String organisationAndRepository = "jstrachan-testing/spring-boot-webmvc";

    @Before
    public void init() throws IOException {
        String workDirPath = Tests.getCleanWorkDir(getClass());

        configuration.setWorkDir(workDirPath);
        configuration.setSourceDir(new File(workDirPath));

        enableFabric8.setOrganisationAndRepository(organisationAndRepository);
    }

    @Test
    public void testUpdater() throws Exception {
        if (Tests.canTestWithGithubAPI(configuration)) {
            enableFabric8.run(configuration);


            // lets now close down the pending PRs
            localRepositories = enableFabric8.getLocalRepositories(configuration);
            GitHubHelpers.closeOpenUpdateBotIssuesAndPullRequests(configuration.getGithubPullRequestLabel(), localRepositories);
            GitHubHelpers.deleteUpdateBotBranches(localRepositories);
        }
    }

}
