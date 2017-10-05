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

import io.fabric8.updatebot.Configuration;
import io.fabric8.updatebot.support.Strings;
import org.kohsuke.github.GHRepository;

import java.net.URL;

/**
 */
public class GithubRepository extends GitRepository {
    private final GHRepository repository;

    public GithubRepository(GHRepository repository) {
        this.repository = repository;
        URL htmlUrl = repository.getHtmlUrl();
        if (htmlUrl != null) {
            setHtmlUrl(htmlUrl.toString());
        }
        setName(repository.getName());
        setCloneUrl(repository.getGitTransportUrl());
    }

    public GithubRepository(GHRepository ghRepository, GitRepositoryConfig details) {
        this(ghRepository);
        setRepositoryDetails(details);
    }

    @Override
    public String toString() {
        return "GithubRepository{" +
                "name='" + getName() + '\'' +
                ", cloneUrl='" + getCloneUrl() + '\'' +
                '}';
    }

    public GHRepository getRepository() {
        return repository;
    }

    @Override
    public String getFullName() {
        return repository.getFullName();

    }

    @Override
    public boolean hasCloneUrl(String url) {
        if (super.hasCloneUrl(url)) {
            return true;
        }
        return Strings.equalAnyValue(url,
                repository.getGitTransportUrl(),
                repository.gitHttpTransportUrl(),
                repository.getSshUrl(),
                repository.getUrl(),
                repository.getSvnUrl());
    }

    @Override
    public String secureCloneUrl(Configuration configuration) {
        if (configuration.isUseHttpsTransport()) {
            String username = configuration.getGithubUsername();
            String password = configuration.getGithubPassword();
            if (Strings.empty(username)) {
                throw new IllegalArgumentException("Missing githubUsername in configuration!");
            } else if (Strings.empty(password)) {
                throw new IllegalArgumentException("Missing githubPassword in configuration!");
            }

            URL url = repository.getHtmlUrl();
            String host = username + ":" + password + "@" + url.getHost();
            String file = url.getFile();
            if (!file.startsWith("/")) {
                file = "/" + file;
            }
            String answer = "https://" + host + file;
            if (!answer.endsWith(".git")) {
                answer += ".git";
            }
            return answer;
        } else {
            return repository.getSshUrl();
        }
    }
}
