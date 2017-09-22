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
package io.fabric8.updatebot;

import io.fabric8.updatebot.model.Projects;
import io.fabric8.updatebot.repository.LocalRepository;
import io.fabric8.updatebot.repository.Repositories;
import io.fabric8.updatebot.support.Strings;
import io.fabric8.updatebot.support.Systems;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import static io.fabric8.updatebot.support.YamlHelper.loadYaml;

/**
 */
public class Updater {
    private static final transient Logger LOG = LoggerFactory.getLogger(Updater.class);

    private GitHub github;
    private String configFile;
    private String workDir;
    private String githubUsername;
    private String githubPassword;
    private String githubToken;

    public static void main(String[] args) {
        Updater updater = new Updater();
        try {
            updater.run();
        } catch (IOException e) {
            System.err.println("Failed to update repositories: " + e);
            e.printStackTrace();
            Throwable cause = e.getCause();
            if (cause != e) {
                System.out.println("Caused by: " + cause);
                cause.printStackTrace();
            }
            System.exit(1);
        }
    }

    public void run() throws IOException {
        Projects projects = loadProjects();

        List<LocalRepository> repositories = Repositories.cloneOrPullRepositories(this, projects);
        for (LocalRepository repository : repositories) {
            updateRepository(repository);
        }
    }

    public void updateRepository(LocalRepository repository) {
        File dir = repository.getDir();
        dir.getParentFile().mkdirs();

        LOG.info("Cloning " + repository.getCloneUrl() + " to " + dir);
    }

    public GitHub getGithub() throws IOException {
        if (github == null) {
            GitHubBuilder ghb = new GitHubBuilder();
            String username = getGithubUsername();
            String password = getGithubPassword();
            String token = getGithubToken();
            if (Strings.notEmpty(username) && Strings.notEmpty(password)) {
                ghb.withPassword(username, password);
            } else if (Strings.notEmpty(token)) {
                if (Strings.notEmpty(username)) {
                    ghb.withOAuthToken(token, username);
                } else {
                    ghb.withOAuthToken(token);
                }
            }
            this.github = ghb.build();
        }
        return this.github;
    }

    // Properties
    //-------------------------------------------------------------------------

    public String getConfigFile() {
        if (configFile == null) {
            configFile = Systems.getConfigValue(EnvironmentVariables.CONFIG_FILE, "updatebot.yml");
        }
        return configFile;
    }

    public void setConfigFile(String configFile) {
        this.configFile = configFile;
    }

    public String getWorkDir() {
        if (workDir == null) {
            workDir = Systems.getConfigValue(EnvironmentVariables.WORK_DIR, "updatebot-repos");
        }
        return workDir;
    }

    public void setWorkDir(String workDir) {
        this.workDir = workDir;
    }

    public String getGithubUsername() {
        if (githubUsername == null) {
            githubUsername = Systems.getConfigValue(EnvironmentVariables.GITHUB_USER);
        }
        return githubUsername;
    }

    public void setGithubUsername(String githubUsername) {
        this.githubUsername = githubUsername;
    }

    public String getGithubPassword() {
        if (githubPassword == null) {
            githubPassword = Systems.getConfigValue(EnvironmentVariables.GITHUB_PASSWORD);
        }
        return githubPassword;
    }

    public void setGithubPassword(String githubPassword) {
        this.githubPassword = githubPassword;
    }

    public String getGithubToken() {
        if (githubToken == null) {
            githubToken = Systems.getConfigValue(EnvironmentVariables.GITHUB_TOKEN);
        }
        return githubToken;
    }

    public void setGithubToken(String githubToken) {
        this.githubToken = githubToken;
    }


    // Implementation
    //-------------------------------------------------------------------------

    private Projects loadProjects() throws IOException {
        String configFile = getConfigFile();
        File file = new File(configFile);
        if (!file.isFile() || !file.exists()) {
            throw new FileNotFoundException(configFile);
        }
        return loadYaml(file, Projects.class);
    }

}
