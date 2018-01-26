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

import io.jenkins.updatebot.git.GitHelper;
import io.jenkins.updatebot.support.Strings;
import io.fabric8.utils.Objects;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class GithubOrganisation extends FilterSupport {
    private String name;
    private List<GitRepositoryConfig> repositories = new ArrayList<>();

    public GithubOrganisation() {
    }

    public GithubOrganisation(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "GithubOrganisation{" +
                "name='" + name + '\'' +
                ", repositories=" + repositories +
                '}';
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<GitRepositoryConfig> getRepositories() {
        return repositories;
    }

    public void setRepositories(List<GitRepositoryConfig> repositories) {
        this.repositories = repositories;
    }

    public GitRepositoryConfig getRepositoryDetails(String cloneUrl) {
        for (GitRepositoryConfig repository : repositories) {
            if (hasCloneUrl(repository, cloneUrl)) {
                return repository;
            }
        }
        return null;
    }

    protected boolean hasCloneUrl(GitRepositoryConfig repository, String cloneUrl) {
        List<String> gitUrls = GitHelper.getGitHubCloneUrls("github.com", getName(), repository.getName());
        return Strings.equalAnyValue(cloneUrl, gitUrls);
    }

    /**
     * Returns the github repository for the given name, lazily creating or updating if required
     */
    public GitRepositoryConfig repository(String name) {
        GitRepositoryConfig config = findRepository(name);
        if (config == null) {
            config = new GitRepositoryConfig(name);
            repositories.add(config);
        }
        return config;
    }

    public GitRepositoryConfig findRepository(String name) {
        for (GitRepositoryConfig repository : repositories) {
            if (Objects.equal(name, repository.getName())) {
                return repository;
            }
        }
        return null;
    }
}
