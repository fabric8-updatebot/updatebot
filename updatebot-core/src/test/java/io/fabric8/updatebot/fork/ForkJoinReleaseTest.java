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
package io.fabric8.updatebot.fork;


import io.jenkins.updatebot.Configuration;
import io.jenkins.updatebot.commands.PushSourceChanges;
import io.jenkins.updatebot.github.GitHubHelpers;
import io.jenkins.updatebot.github.Issues;
import io.jenkins.updatebot.github.PullRequests;
import io.jenkins.updatebot.repository.LocalRepository;
import io.jenkins.updatebot.support.FileHelper;
import io.fabric8.updatebot.test.GithubAssertions;
import io.fabric8.updatebot.test.NpmTests;
import io.fabric8.updatebot.test.Tests;
import org.junit.Before;
import org.junit.Test;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 */
public class ForkJoinReleaseTest {
    private static final transient Logger LOG = LoggerFactory.getLogger(ForkJoinReleaseTest.class);

    protected Configuration configuration = new Configuration();
    protected List<LocalRepository> localRepositories;
    protected String destinationRepoName = "fabric8-planner";
    protected TestNpmDependencyTreeGenerator testNpmDependencyTreeGenerator = new TestNpmDependencyTreeGenerator(destinationRepoName);
    protected boolean fullTest;

    @Before
    public void init() throws IOException {
        String configFile = new File(Tests.getBasedir(), "src/test/resources/npm/join/updatebot.yml").getPath();
        String workDirPath = Tests.getCleanWorkDir(getClass());

        configuration.setConfigFile(configFile);
        configuration.setWorkDir(workDirPath);
        configuration.setPullDisabled(true);
        configuration.setNpmDependencyTreeGenerator(testNpmDependencyTreeGenerator);

        if (Tests.canTestWithGithubAPI(configuration)) {
            PushSourceChanges dummy = new PushSourceChanges();
            localRepositories = dummy.cloneOrPullRepositories(configuration);

            // lets close all open PRs
            GitHubHelpers.closeOpenUpdateBotIssuesAndPullRequests(configuration.getGithubPullRequestLabel(), localRepositories);
            GitHubHelpers.deleteUpdateBotBranches(localRepositories);
        }
    }

    @Test
    public void testUpdater() throws Exception {
        if (Tests.canTestWithGithubAPI(configuration)) {
            assertOpenIssueAndPullRequestCount(destinationRepoName, 0, 0, false);

            simulateRelease("ngx-base", "2.0.0");
            assertOpenIssueAndPullRequestCount(destinationRepoName, 1, 0, true);

            int expectedPullRequests = 0;
            if (fullTest) {
                expectedPullRequests = 1;
                simulateRelease("ngx-widgets", "3.1.1");
                // its safe to PR on fabric8-planner with ngx-widgets as there's no direct ngx-base dependency!
                assertOpenIssueAndPullRequestCount(destinationRepoName, 1, expectedPullRequests, false);
            }

            simulateRelease("ngx-login-client", "2.2.2");
            assertOpenIssueAndPullRequestCount(destinationRepoName, 1, expectedPullRequests, false);

            simulateRelease("ngx-fabric8-wit", "7.3.3");
            assertOpenIssueAndPullRequestCount(destinationRepoName, 0, 1, false);
        }
    }

    /**
     * Simulates making a real release but without really making a release.
     * <p>
     * For now we just update the package.json and then use this new version in the test dependency resolver
     */
    protected void simulateRelease(String repoName, String version) throws IOException {
        LOG.info("Simulating " + repoName + " release of " + version);
        testNpmDependencyTreeGenerator.addRelease(repoName, version);

        LocalRepository sourceRepo = assertLocalRepository(repoName);
        File dir = sourceRepo.getDir();

        NpmTests.updatePackageJsonVersion(configuration, new File(dir, "package.json"), version);

        PushSourceChanges pushSourceChanges = new PushSourceChanges();

        // lets find the cloned repo...
        configuration.setSourceDir(dir);

        LOG.info("Pushing source changes from " + FileHelper.getRelativePathToCurrentDir(dir));
        pushSourceChanges.run(configuration);
    }

    public void assertOpenIssueAndPullRequestCount(String repoName, int expectedIssueCount, int exepectedPullRequestCount, boolean verbose) throws IOException {
        LocalRepository repository = assertLocalRepository(repoName);

        GHRepository gitHubRepository = GitHubHelpers.getGitHubRepository(repository);
        assertThat(gitHubRepository).describedAs("Not a GitHub repository " + repository).isNotNull();

        String label = configuration.getGithubPullRequestLabel();

        List<GHIssue> issues;
        List<GHPullRequest> prs;

        boolean doAssert = true;
        if (doAssert) {
            issues = GithubAssertions.assertOpenIssueCount(gitHubRepository, label, expectedIssueCount);
            prs = GithubAssertions.assertOpenPullRequestCount(gitHubRepository, label, exepectedPullRequestCount);
        } else {
            issues = Issues.getOpenIssues(gitHubRepository, configuration);
            prs = PullRequests.getOpenPullRequests(gitHubRepository, configuration);
        }

        if (verbose) {
            LOG.warn("===> " + gitHubRepository.getName() + " Issue count: " + issues.size() + " PR count: " + prs.size());

            Issues.logOpen(issues);
            PullRequests.logOpen(prs);
        }
    }

    protected LocalRepository assertLocalRepository(String repoName) {
        LocalRepository repository = LocalRepository.findRepository(localRepositories, repoName);
        assertThat(repository).describedAs("Could not find LocalRepository for name: " + repoName).isNotNull();
        return repository;
    }

}
