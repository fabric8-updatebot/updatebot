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
package io.fabric8.updatebot.github;

import io.fabric8.updatebot.model.GitRepository;
import io.fabric8.updatebot.model.GithubRepository;
import io.fabric8.updatebot.repository.LocalRepository;
import io.fabric8.utils.Objects;
import org.kohsuke.github.GHCommitStatus;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHPerson;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

/**
 */
public class GitHubHelpers {
    private static final transient Logger LOG = LoggerFactory.getLogger(GitHubHelpers.class);

    public static void closeOpenUpdateBotIssuesAndPullRequests(String prLabel, List<LocalRepository> repositories) {
        for (LocalRepository repository : repositories) {
            GHRepository ghRepo = GitHubHelpers.getGitHubRepository(repository);
            if (ghRepo != null) {
                try {
                    closePullRequests(PullRequests.getOpenPullRequests(ghRepo, prLabel));
                    closeIssues(Issues.getOpenIssues(ghRepo, prLabel));
                } catch (IOException e) {
                    LOG.warn("Failed to close pending open Pull Requests on " + repository.getCloneUrl());
                }
            }
        }
    }

    public static void closeIssues(List<GHIssue> issues) throws IOException {
        for (GHIssue issue : issues) {
            issue.close();
        }
    }

    public static void closePullRequests(List<GHPullRequest> pullRequests) throws IOException {
        for (GHPullRequest pullRequest : pullRequests) {
            pullRequest.close();
        }
    }

    /**
     * Returns the underlying GitHub repository if this repository is on github
     */
    public static GHRepository getGitHubRepository(LocalRepository repository) {
        GitRepository repo = repository.getRepo();
        if (repo instanceof GithubRepository) {
            GithubRepository githubRepository = (GithubRepository) repo;
            return githubRepository.getRepository();
        }
        return null;
    }

    public static boolean hasLabel(Collection<GHLabel> labels, String label) {
        if (labels != null) {
            for (GHLabel ghLabel : labels) {
                if (Objects.equal(label, ghLabel.getName())) {
                    return true;
                }
            }
        }
        return false;
    }


    public static boolean isMergeable(GHPullRequest pullRequest) throws IOException {
        boolean canMerge = false;
        Boolean mergeable = pullRequest.getMergeable();
        GHPullRequest single = null;
        if (mergeable == null) {
            single = pullRequest.getRepository().getPullRequest(pullRequest.getNumber());
            mergeable = single.getMergeable();
        }
        if (mergeable == null) {
            LOG.warn("Mergeable flag is still null on pull request " + pullRequest.getHtmlUrl() + " assuming its still mergable. Probably a caching issue and this flag may appear again later");
            return true;
        }
        if (mergeable != null && mergeable.booleanValue()) {
            canMerge = true;
        }
        return canMerge;
    }

    public static void deleteUpdateBotBranches(List<LocalRepository> localRepositories) throws IOException {
/*
        for (LocalRepository localRepository : localRepositories) {
            GHRepository ghRepository = getGitHubRepository(localRepository);
            if (ghRepository != null) {
                Map<String, GHBranch> branches = ghRepository.getBranches();
                for (GHBranch ghBranch : branches.values()) {
                    String name = ghBranch.getName();
                    if (name.startsWith("updatebot-")) {
                        // TODO no API to delete branches yet
                    }
                }
            }
        }
*/
    }

    public static GHPerson getOrganisationOrUser(GitHub github, String orgName) {
        GHPerson person = null;
        try {
            person = github.getOrganization(orgName);
        } catch (IOException e) {
        }
        if (person == null) {
            try {
                person = github.getUser(orgName);
            } catch (IOException e) {
                LOG.warn("Could not find organisation or user for " + orgName + ". " + e, e);
            }
        }
        return person;
    }

    public static GHCommitStatus getLastCommitStatus(GHRepository repository, GHPullRequest pullRequest) throws IOException {
        String commitSha = pullRequest.getHead().getRef();
        return repository.getLastCommitStatus(commitSha);
    }

    public static <T> T retryGithub(Callable<T> callable) throws IOException {
        return retryGithub(callable, 5, 1000);
    }

    /**
     * Allow automatic retries when timeout exceptions happen
     */
    public static <T> T retryGithub(Callable<T> callable, int retries, long timeout) throws IOException {
        for (int i = 0; i < retries; i++) {
            if (i > 0) {
                try {
                    Thread.sleep(timeout);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            try {
                return callable.call();
            } catch (HttpException e) {
                int code = e.getResponseCode();
                LOG.warn("GitHub Operation returned response " + code + " so retrying. Exception " + e, e);
                if (code >= 100) {
                    throw e;
                }
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                throw new IOException(e);
            }
        }
        return null;
    }

    public static Boolean waitForPullRequestToHaveMergable(GHPullRequest pullRequest, long sleepMS, long maximumTimeMS) throws IOException {
        long end = System.currentTimeMillis() + maximumTimeMS;
        while (true) {
            Boolean mergeable = pullRequest.getMergeable();
            if (mergeable == null) {
                GHRepository repository = pullRequest.getRepository();
                int number = pullRequest.getNumber();
                pullRequest = repository.getPullRequest(number);
                mergeable = pullRequest.getMergeable();
            }
            if (mergeable != null) {
                return mergeable;
            }
            if (System.currentTimeMillis() > end) {
                return null;
            }
            try {
                Thread.sleep(sleepMS);
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }
}
