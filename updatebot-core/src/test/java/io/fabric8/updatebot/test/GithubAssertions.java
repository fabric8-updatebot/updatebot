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
package io.fabric8.updatebot.test;

import io.jenkins.updatebot.github.GitHubHelpers;
import io.jenkins.updatebot.github.Issues;
import io.jenkins.updatebot.github.PullRequests;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 */
public class GithubAssertions {
    public static List<GHPullRequest> assertOpenPullRequestCount(GHRepository repository, String label, int exepectedPullRequestCount) throws IOException {
        List<GHPullRequest> openPullRequests = PullRequests.getOpenPullRequests(repository, label);
        assertThat(openPullRequests).describedAs("open github PR with label " + label + ": " + openPullRequests).hasSize(exepectedPullRequestCount);
        return openPullRequests;
    }

    public static List<GHIssue> assertOpenIssueCount(GHRepository repository, String label, int expectedIssueCount) throws IOException {
        List<GHIssue> openIssues = Issues.getOpenIssues(repository, label);
        assertThat(openIssues).describedAs("open github issues with label " + label + ": " + openIssues).hasSize(expectedIssueCount);
        return openIssues;
    }

    /**
     * Waits for the mergable state to be available on the given pull request (which can take some time due to caching)
     */
    public static void assertWaitForPullRequestMergable(GHPullRequest pullRequest, boolean expectedMergable) throws IOException {
        Boolean mergable = GitHubHelpers.waitForPullRequestToHaveMergable(pullRequest, 1000L, 30000L);
        assertThat(mergable).describedAs("Should have found a mergable for PullRequest " + pullRequest.getHtmlUrl()).isNotNull().isEqualTo(expectedMergable);

        assertThat(GitHubHelpers.isMergeable(pullRequest)).describedAs("should not be mergable!").isEqualTo(expectedMergable);
    }
}
