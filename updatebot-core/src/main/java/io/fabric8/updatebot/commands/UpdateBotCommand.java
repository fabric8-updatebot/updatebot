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
package io.fabric8.updatebot.commands;

import com.beust.jcommander.Parameter;
import io.fabric8.updatebot.EnvironmentVariables;
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

import static io.fabric8.updatebot.support.MarkupHelper.loadYaml;

/**
 */
public abstract class UpdateBotCommand {
    private static final transient Logger LOG = LoggerFactory.getLogger(UpdateBotCommand.class);

    private GitHub github;

    @Parameter(names = {"--config", "-c"}, description = "Location of the UpdateBot YAML configuration file")
    private String configFile = Systems.getConfigValue(EnvironmentVariables.CONFIG_FILE, "updatebot.yml");

    @Parameter(names = {"--dir", "-d"}, description = "Directory where the git repositories are cloned")
    private String workDir = Systems.getConfigValue(EnvironmentVariables.WORK_DIR, "updatebot-repos");

    @Parameter(names = {"--github-username", "-ghu"}, description = "GitHub Username")
    private String githubUsername = Systems.getConfigValue(EnvironmentVariables.GITHUB_USER);

    @Parameter(names = {"--github-password", "-ghp"}, description = "GitHub Password")
    private String githubPassword = Systems.getConfigValue(EnvironmentVariables.GITHUB_PASSWORD);

    @Parameter(names = {"--github-token", "-ght"}, description = "GitHub Token")
    private String githubToken = Systems.getConfigValue(EnvironmentVariables.GITHUB_TOKEN);


    public void run() throws IOException {
        Projects projects = loadProjects();

        List<LocalRepository> repositories = Repositories.cloneOrPullRepositories(this, projects);
        for (LocalRepository repository : repositories) {
            processRepository(repository);
        }
    }

    public boolean processRepository(LocalRepository repository) throws IOException {
        File dir = repository.getDir();
        dir.getParentFile().mkdirs();
        return doProcess(repository);
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
        return configFile;
    }

    public void setConfigFile(String configFile) {
        this.configFile = configFile;
    }

    public String getWorkDir() {
        return workDir;
    }

    public void setWorkDir(String workDir) {
        this.workDir = workDir;
    }

    public String getGithubUsername() {
        return githubUsername;
    }

    public void setGithubUsername(String githubUsername) {
        this.githubUsername = githubUsername;
    }

    public String getGithubPassword() {
        return githubPassword;
    }

    public void setGithubPassword(String githubPassword) {
        this.githubPassword = githubPassword;
    }

    public String getGithubToken() {
        return githubToken;
    }

    public void setGithubToken(String githubToken) {
        this.githubToken = githubToken;
    }


    // Implementation
    //-------------------------------------------------------------------------
    protected abstract boolean doProcess(LocalRepository repository) throws IOException;

    protected Projects loadProjects() throws IOException {
        String configFile = getConfigFile();
        File file = new File(configFile);
        if (!file.isFile() || !file.exists()) {
            throw new FileNotFoundException(configFile);
        }
        return loadYaml(file, Projects.class);
    }
}
