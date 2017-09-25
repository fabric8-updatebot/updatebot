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
import io.fabric8.updatebot.kind.Kind;
import io.fabric8.updatebot.repository.LocalRepository;
import io.fabric8.updatebot.support.GitHubHelpers;
import io.fabric8.updatebot.support.Strings;
import io.fabric8.updatebot.test.NpmTests;
import io.fabric8.updatebot.test.Tests;
import io.fabric8.utils.Files;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 */
public class PushPRTest {
    private static final transient Logger LOG = LoggerFactory.getLogger(PushPRTest.class);

    protected String dependency = "@angular/core";
    protected String firstVersion = "4.3.7";
    protected String secondVersion = "4.3.8";
    protected String oldVersion = "4.0.0";

    protected PushVersionChanges updateBot = new PushVersionChanges();
    protected List<LocalRepository> localRepositories;

    @Before
    public void init() throws IOException {
        File testClasses = new File(Tests.getBasedir(), "src/test/resources/npm/push/updatebot.yml");

        updateBot.setConfigFile(testClasses.getPath());

        // lets update a single version
        updateBot.setKind(Kind.NPM);
        updateBot.values(dependency, firstVersion);
        File testDataDir = Tests.getTestDataDir(getClass());
        System.out.println("Using workDir: " + testDataDir);
        Files.recursiveDelete(testDataDir);
        updateBot.setWorkDir(testDataDir.getPath());

        // TODO lets close all open PRs

        this.localRepositories = updateBot.closeOrPullRepositories();
        GitHubHelpers.closeOpenUpdateBotPullRequests(updateBot.getGithubPullRequestLabel(), localRepositories);
    }

    @Test
    public void testUpdater() throws Exception {
        if (Strings.notEmpty(updateBot.getGithubUsername()) &&
                (Strings.notEmpty(updateBot.getGithubPassword()) || Strings.notEmpty(updateBot.getGithubToken()))) {
            updateBot.run();

            // now lets try a second update to the same PR
            updateBot.values(dependency, secondVersion);
            updateBot.run();


            // lets do some dummy commits that force existing PRs to not be mergeable
            NpmTests.generateDummyPackageJsonCommit(localRepositories, oldVersion, "devDependencies", dependency);

            // now lets repeat the same command to see if we do anything (rebase mode)
            updateBot.values(dependency, secondVersion);
            updateBot.run();

        } else {
            LOG.info("Disabling this test case as we do not have a github username and password/token defined via environment variables");
        }
    }

}
