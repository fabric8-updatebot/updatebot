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
package io.fabric8.updatebot.support;

import com.fasterxml.jackson.databind.JsonNode;
import io.fabric8.updatebot.model.GitRepository;
import io.fabric8.updatebot.model.GithubRepository;
import io.fabric8.updatebot.repository.LocalRepository;
import io.fabric8.utils.Files;
import io.fabric8.utils.Objects;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 */
public class GitHubHelpers {
    private static final transient Logger LOG = LoggerFactory.getLogger(GitHubHelpers.class);

    public static void closeOpenUpdateBotPullRequests(String prLabel, List<LocalRepository> repositories) {
        for (LocalRepository repository : repositories) {
            GitRepository repo = repository.getRepo();
            if (repo instanceof GithubRepository) {
                GithubRepository githubRepository = (GithubRepository) repo;
                GHRepository ghRepo = githubRepository.getRepository();
                try {
                    List<GHPullRequest> pullRequests = ghRepo.getPullRequests(GHIssueState.OPEN);
                    for (GHPullRequest pullRequest : pullRequests) {
                        if (hasLabel(pullRequest.getLabels(), prLabel)) {
                            pullRequest.close();
                        }
                    }
                } catch (IOException e) {
                    LOG.warn("Failed to close pending open Pull Requests on " + repository.getCloneUrl());
                }


            }
        }

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
        if (mergeable != null && !mergeable.booleanValue()) {
            canMerge = true;
        }
        return canMerge;
    }
}
