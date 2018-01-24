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
package io.jenkins.updatebot.model;

import io.fabric8.utils.Objects;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class RepositoryConfig {
    private GitHubProjects github;
    private List<GitRepository> git = new ArrayList<>();
    private GitRepositoryConfig local;
    private List<Environment> environments = new ArrayList();

    @Override
    public String toString() {
        return "RepositoryConfig{" +
                "github=" + github +
                ", git=" + git +
                '}';
    }

    /**
     * Returns the github configuration, lazily created if required
     */
    public GitHubProjects github() {
        if (github == null) {
            github = new GitHubProjects();
        }
        return github;
    }

    public GitHubProjects getGithub() {
        return github;
    }

    public void setGithub(GitHubProjects github) {
        this.github = github;
    }

    public List<GitRepository> getGit() {
        return git;
    }

    public void setGit(List<GitRepository> git) {
        this.git = git;
    }

    public GitRepositoryConfig getLocal() {
        return local;
    }

    public void setLocal(GitRepositoryConfig local) {
        this.local = local;
    }

    public List<Environment> getEnvironments() {
        return environments;
    }

    public void setEnvironments(List<Environment> environments) {
        this.environments = environments;
    }

    public GitRepositoryConfig getRepositoryDetails(String cloneUrl) {
        GitRepositoryConfig answer = github.getRepositoryDetails(cloneUrl);
        if (answer == null) {
            for (GitRepository gitRepository : git) {
                if (Objects.equal(cloneUrl, gitRepository.getCloneUrl())) {
                    answer = gitRepository.getRepositoryDetails();
                    if (answer != null) {
                        return answer;
                    }
                }
            }
        }
        return answer;
    }

    public void add(GitRepository gitRepository) {
        if (git == null) {
            git = new ArrayList<>();
        }
        git.add(gitRepository);
    }
}
