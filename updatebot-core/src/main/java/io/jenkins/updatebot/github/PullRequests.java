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
package io.jenkins.updatebot.github;

import io.jenkins.updatebot.Configuration;
import io.jenkins.updatebot.support.Markdown;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static io.jenkins.updatebot.github.GitHubHelpers.retryGithub;

/**
 */
public class PullRequests {
    public static final String COMMAND_COMMENT_PREFIX = Markdown.UPDATEBOT_ICON + " commands:";
    public static final String COMMAND_COMMENT_PREFIX_SEPARATOR = "\n\n";
    public static final String COMMAND_COMMENT_INDENT = "    " + Markdown.UPDATEBOT + " ";
    private static final transient Logger LOG = LoggerFactory.getLogger(PullRequests.class);
    public static String ISSUE_LINK_COMMENT = Markdown.UPDATEBOT_ICON + " raised issue ";
    public static String ISSUE_LINK_COMMENT_SUFFIX = " to manage version conflicts";

    public static List<GHPullRequest> getOpenPullRequests(GHRepository ghRepository, Configuration configuration) throws IOException {
        return getOpenPullRequests(ghRepository, configuration.getGithubPullRequestLabel());
    }

    public static List<GHPullRequest> getOpenPullRequests(GHRepository ghRepository, String label) throws IOException {
        List<GHPullRequest> pullRequests = retryGithub(() -> ghRepository.getPullRequests(GHIssueState.OPEN));
        List<GHPullRequest> answer = new ArrayList<>();
        if (pullRequests != null) {
            for (GHPullRequest pullRequest : pullRequests) {
                if (GitHubHelpers.hasLabel(Issues.getLabels(pullRequest), label)) {
                    answer.add(pullRequest);
                }
            }
        }
        return answer;
    }


    public static void logOpen(List<GHPullRequest> prs) {
        for (GHPullRequest pr : prs) {
            LOG.info("Open Pull Request " + pr.getHtmlUrl());
        }
    }
}