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
package io.fabric8.updatebot.model;

import io.fabric8.utils.Objects;

/**
 */
public class GitRepository extends DtoSupport {
    private String name;
    private String cloneUrl;
    private String htmlUrl;
    private GitHubRepositoryDetails repositoryDetails;

    public GitRepository() {
    }

    public GitRepository(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "GitRepository{" +
                "name='" + name + '\'' +
                ", cloneUrl='" + cloneUrl + '\'' +
                '}';
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCloneUrl() {
        return cloneUrl;
    }

    public void setCloneUrl(String cloneUrl) {
        this.cloneUrl = cloneUrl;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

    public GitHubRepositoryDetails getRepositoryDetails() {
        return repositoryDetails;
    }

    public void setRepositoryDetails(GitHubRepositoryDetails repositoryDetails) {
        this.repositoryDetails = repositoryDetails;
    }

    public boolean hasCloneUrl(String url) {
        return Objects.equal(this.cloneUrl, url);
    }


}
