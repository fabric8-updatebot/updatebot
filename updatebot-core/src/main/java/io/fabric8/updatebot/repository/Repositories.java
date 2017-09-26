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
package io.fabric8.updatebot.repository;

import io.fabric8.updatebot.Configuration;
import io.fabric8.updatebot.commands.CommandSupport;
import io.fabric8.updatebot.model.GitHubProjects;
import io.fabric8.updatebot.model.GitRepository;
import io.fabric8.updatebot.model.GithubOrganisation;
import io.fabric8.updatebot.model.GithubRepository;
import io.fabric8.updatebot.model.Projects;
import io.fabric8.updatebot.support.Commands;
import io.fabric8.updatebot.support.GitHubHelpers;
import io.fabric8.utils.Filter;
import org.kohsuke.github.GHPerson;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 */
public class Repositories {
    private static final transient Logger LOG = LoggerFactory.getLogger(Repositories.class);


    public static List<LocalRepository> cloneOrPullRepositories(CommandSupport command, Configuration configuration, Projects projects) throws IOException {
        List<LocalRepository> repositories = findRepositories(command, configuration, projects);
        for (LocalRepository repository : repositories) {
            cloneOrPull(repository);
        }
        return repositories;
    }

    public static boolean gitStashAndCheckoutMaster(File dir) {
        if (Commands.runCommandIgnoreOutput(dir, "git", "stash") == 0) {
            if (Commands.runCommandIgnoreOutput(dir, "git", "checkout", "master") == 0) {
                return true;
            }
        }
        LOG.warn("Failed to checkout master in " + dir);
        return false;
    }

    private static void cloneOrPull(LocalRepository repository) {
        File dir = repository.getDir();
        File gitDir = new File(dir, ".git");
        if (gitDir.exists()) {
            if (Commands.runCommandIgnoreOutput(dir, "git", "stash") == 0) {
                if (Commands.runCommandIgnoreOutput(dir, "git", "checkout", "master") == 0) {
                    LOG.info("Pulling: " + dir + " repo: " + repository.getCloneUrl());
                    Commands.runCommandIgnoreOutput(dir, "git", "pull");
                }
            }
        } else {
            File parentDir = dir.getParentFile();
            parentDir.mkdirs();

            LOG.info("Cloning: " + dir + " repo: " + repository.getCloneUrl());
            Commands.runCommand(parentDir, "git", "clone", repository.getCloneUrl(), dir.getName());
        }
    }

    protected static List<LocalRepository> findRepositories(CommandSupport updateBot, Configuration configuration, Projects projects) throws IOException {
        File workDir = new File(configuration.getWorkDir());
        workDir.mkdirs();

        Map<String, LocalRepository> map = new LinkedHashMap<>();
        File gitHubDir = new File(workDir, "github");
        File gitDir = new File(workDir, "git");

        GitHubProjects githubProjects = projects.getGithub();
        if (githubProjects != null) {
            List<GithubOrganisation> organisations = githubProjects.getOrganisations();
            if (organisations != null && !organisations.isEmpty()) {
                GitHub github = configuration.getGithub();
                for (GithubOrganisation organisation : organisations) {
                    addGitHubRepositories(map, github, organisation, new File(gitHubDir, organisation.getName()));
                }
            }
        }
        List<GitRepository> gitRepositories = projects.getGit();
        if (gitRepositories != null) {
            for (GitRepository gitRepository : gitRepositories) {
                addRepository(map, gitDir, gitRepository);
            }
        }
        return new ArrayList<>(map.values());
    }

    protected static void addRepository(Map<String, LocalRepository> map, File gitDir, GitRepository gitRepository) {
        LocalRepository localRepository = new LocalRepository(gitRepository, new File(gitDir, gitRepository.getName()));
        map.putIfAbsent(localRepository.getCloneUrl(), localRepository);
    }

    protected static void addGitHubRepositories(Map<String, LocalRepository> map, GitHub github, GithubOrganisation organisation, File file) {
        String orgName = organisation.getName();
        Filter<String> filter = organisation.createFilter();

        GHPerson person = GitHubHelpers.getOrganisationOrUser(github, orgName);
        if (person != null) {
            try {
                Map<String, GHRepository> repositories = person.getRepositories();
                for (Map.Entry<String, GHRepository> entry : repositories.entrySet()) {
                    String repoName = entry.getKey();
                    if (filter.matches(repoName)) {
                        GitRepository gitRepository = new GithubRepository(entry.getValue());
                        addRepository(map, file, gitRepository);

                    }
                }
            } catch (IOException e) {
                LOG.warn("Failed to load organisation: " + orgName + ". " + e, e);
            }
        }
    }

}
