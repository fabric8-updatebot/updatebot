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
package io.jenkins.updatebot.repository;

import io.jenkins.updatebot.Configuration;
import io.jenkins.updatebot.model.GitRepository;
import io.jenkins.updatebot.model.GitRepositoryConfig;
import io.jenkins.updatebot.model.RepositoryConfig;
import io.jenkins.updatebot.model.RepositoryConfigs;
import io.jenkins.updatebot.support.Strings;
import io.fabric8.utils.Files;
import io.fabric8.utils.Objects;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static io.jenkins.updatebot.Configuration.DEFAULT_CONFIG_FILE;

/**
 */
public class LocalRepository {
    private GitRepository repo;
    private File dir;

    public LocalRepository(GitRepository repo, File dir) {
        this.repo = repo;
        this.dir = dir;
    }

    /**
     * Returns a local repository from a directory.
     */
    public static LocalRepository fromDirectory(Configuration configuration, File dir) throws IOException {
        LocalRepository localRepository = new LocalRepository(new GitRepository(dir.getName()), dir);
        File configFile = new File(dir, DEFAULT_CONFIG_FILE);
        if (Files.isFile(configFile)) {
            RepositoryConfig config = RepositoryConfigs.loadRepositoryConfig(configuration, DEFAULT_CONFIG_FILE, dir);
            if (config != null) {
                GitRepositoryConfig local = config.getLocal();
                if (local != null) {
                    localRepository.getRepo().setRepositoryDetails(local);
                }
            }
        }
        return localRepository;
    }

    /**
     * Returns the repository for the given name or null if it could not be found
     */
    public static LocalRepository findRepository(List<LocalRepository> localRepositories, String name) {
        if (localRepositories != null) {
            for (LocalRepository repository : localRepositories) {
                GitRepository repo = repository.getRepo();
                if (repo != null) {
                    if (Objects.equal(name, repo.getName())) {
                        return repository;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns the repository for the given repo
     */
    public static LocalRepository findRepository(List<LocalRepository> localRepositories, GitRepository gitRepository) {
        if (localRepositories != null) {
            for (LocalRepository repository : localRepositories) {
                GitRepository repo = repository.getRepo();
                if (repo != null) {
                    if (Objects.equal(repo.getCloneUrl(), gitRepository.getCloneUrl())) {
                        return repository;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns the link to the repository
     */
    public static String getRepositoryLink(LocalRepository repository) {
        return getRepositoryLink(repository, repository.getFullName());
    }

    /**
     * Returns the link to the repository
     */
    public static String getRepositoryLink(LocalRepository repository, String label) {
        return getRepositoryLink(repository, label, "`" + label + "`");
    }

    /**
     * Returns the link to the repository
     */
    public static String getRepositoryLink(LocalRepository repository, String label, String defaultValue) {
        if (repository != null) {
            String htmlUrl = repository.getRepo().getHtmlUrl();
            if (Strings.notEmpty(htmlUrl)) {
                return "[" + label + "](" + htmlUrl + ")";
            }
        }
        if (Strings.notEmpty(defaultValue)) {
            return defaultValue;
        }
        return label;
    }

    @Override
    public String toString() {
        return "LocalRepository{" +
                "repo=" + repo +
                ", dir=" + dir +
                '}';
    }

    public String getFullName() {
        return repo.getFullName();
    }

    public GitRepository getRepo() {
        return repo;
    }

    public File getDir() {
        return dir;
    }

    public String getCloneUrl() {
        return repo.getCloneUrl();
    }

    /**
     * Returns true if this repository can be cloned using the given URL
     */
    public boolean hasCloneUrl(String cloneUrl) {
        // sometimes folks miss off the ".git" from URLs so lets check for that too
        return repo.hasCloneUrl(cloneUrl) || repo.hasCloneUrl(cloneUrl + ".git");
    }
}
