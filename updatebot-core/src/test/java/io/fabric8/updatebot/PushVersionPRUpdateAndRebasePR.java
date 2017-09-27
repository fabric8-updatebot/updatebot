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
import io.fabric8.updatebot.commands.UpdatePullRequests;
import io.fabric8.updatebot.kind.Kind;
import io.fabric8.updatebot.repository.LocalRepository;
import io.fabric8.updatebot.support.GitHubHelpers;
import io.fabric8.updatebot.test.NpmTests;
import io.fabric8.updatebot.test.Tests;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 */
public class PushVersionPRUpdateAndRebasePR {
    private static final transient Logger LOG = LoggerFactory.getLogger(PushVersionPRUpdateAndRebasePR.class);

    protected String dependency = "@angular/core";
    protected String firstVersion = "4.3.7";
    protected String secondVersion = "4.3.8";
    protected String oldVersion = "4.0." + UUID.randomUUID().toString();

    protected PushVersionChanges updateBot = new PushVersionChanges();
    protected UpdatePullRequests updatePullRequests = new UpdatePullRequests();
    protected List<LocalRepository> localRepositories;
    protected Configuration configuration = new Configuration();

    @Before
    public void init() throws IOException {
        String configFile = new File(Tests.getBasedir(), "src/test/resources/npm/push/updatebot.yml").getPath();
        String workDirPath = Tests.getCleanWorkDir(getClass());

        configuration.setConfigFile(configFile);
        configuration.setWorkDir(workDirPath);

        // lets update a single version
        updateBot.setKind(Kind.NPM);
        updateBot.values(dependency, firstVersion);

        this.localRepositories = updateBot.cloneOrPullRepositories(configuration);

        // lets close all open PRs
        GitHubHelpers.closeOpenUpdateBotPullRequests(configuration.getGithubPullRequestLabel(), localRepositories);
        GitHubHelpers.deleteUpdateBotBranches(localRepositories);
    }

    @Test
    public void testUpdater() throws Exception {
        if (Tests.canTestWithGithubAPI(configuration)) {
            updateBot.run(configuration);

            // now lets try a second update to the same PR
            updateBot.values(dependency, secondVersion);
            updateBot.run(configuration);


            // lets do some dummy commits that force existing PRs to not be mergeable
            NpmTests.generateDummyPackageJsonCommit(localRepositories, oldVersion, "devDependencies", dependency);

            // now lets rebase
            updatePullRequests.run(configuration);
        }

    }

}
