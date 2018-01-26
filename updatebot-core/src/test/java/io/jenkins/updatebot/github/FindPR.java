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
import org.kohsuke.github.GHCommitStatus;
import org.kohsuke.github.GHPerson;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestReviewComment;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 */
public class FindPR {
    private static final transient Logger LOG = LoggerFactory.getLogger(FindPR.class);

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("FindPR organisation repo prNumber");
            return;
        }
        String name = args[0];
        String repo = args[1];
        String pr = args[2];

        Configuration configuration = new Configuration();
        GHPerson organization = getGhOrganizationOrUser(configuration, name);
        if (organization == null) {
            System.out.println("No organisation " + name);
            return;
        }
        GHRepository repository = getGhRepository(organization, repo);
        if (repository == null) {
            System.out.println("No repository: " + repo);
            return;
        }
        int prNumber = 0;
        try {
            prNumber = Integer.parseInt(pr);
        } catch (NumberFormatException e) {
            System.err.println("Failed to parse PR number `" + pr + "` due to: " + e);
            return;
        }
        try {
            GHPullRequest pullRequest = repository.getPullRequest(prNumber);
            System.out.println("Pull Request at " + pullRequest.getHtmlUrl());

            List<GHPullRequestReviewComment> reviewComments = pullRequest.listReviewComments().asList();
            for (GHPullRequestReviewComment reviewComment : reviewComments) {
                System.out.println("Review comment: " + reviewComment);
            }

            GHCommitStatus status = GitHubHelpers.getLastCommitStatus(repository, pullRequest);
            System.out.println("Status: " + status);
        } catch (IOException e) {
            System.err.println("Failed to load PR " + prNumber);
            return;
        }
    }


    protected static GHRepository getGhRepository(GHPerson organization, String repo) {
        try {
            return organization.getRepository(repo);
        } catch (IOException e) {
            LOG.warn("Failed to load repository on " + organization.getLogin() + "/" + repo + ". " + e, e);
            return null;
        }
    }

    protected static GHPerson getGhOrganizationOrUser(Configuration configuration, String name) {
        try {
            GitHub github = configuration.getGithub();
            return GitHubHelpers.getOrganisationOrUser(github, name);
        } catch (IOException e) {
            LOG.warn("Failed to load organisation " + name + ". " + e, e);
            return null;
        }
    }
}
