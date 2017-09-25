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
import io.fabric8.updatebot.model.GitRepository;
import io.fabric8.updatebot.model.GithubRepository;
import io.fabric8.updatebot.model.Projects;
import io.fabric8.updatebot.repository.LocalRepository;
import io.fabric8.updatebot.repository.Repositories;
import io.fabric8.updatebot.support.Commands;
import io.fabric8.updatebot.support.Strings;
import io.fabric8.updatebot.support.Systems;
import org.kohsuke.github.GHCommitPointer;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static io.fabric8.updatebot.support.MarkupHelper.loadYaml;
import static org.kohsuke.github.GHIssueState.OPEN;

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

    @Parameter(names = {"--dry", "-d"}, description = "GitHub Token")
    private boolean dryRun = Systems.isConfigFlag(EnvironmentVariables.DRY_RUN);


    public void run() throws IOException {
        Projects projects = loadProjects();

        List<LocalRepository> repositories = Repositories.cloneOrPullRepositories(this, projects);
        for (LocalRepository repository : repositories) {
            UpdateContext context = new UpdateContext(repository);
            if (processRepository(context) && !dryRun) {
                gitCommitAndPullRequest(context);
            }
        }
    }


    public boolean processRepository(UpdateContext context) throws IOException {
        File dir = context.getRepository().getDir();
        dir.getParentFile().mkdirs();
        return doProcess(context);
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

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    // Implementation
    //-------------------------------------------------------------------------
    protected abstract boolean doProcess(UpdateContext context) throws IOException;

    protected Projects loadProjects() throws IOException {
        String configFile = getConfigFile();
        File file = new File(configFile);
        if (!file.isFile() || !file.exists()) {
            throw new FileNotFoundException(configFile);
        }
        return loadYaml(file, Projects.class);
    }

    protected void gitCommitAndPullRequest(UpdateContext context) throws IOException {
        LocalRepository repository = context.getRepository();
        GitRepository repo = repository.getRepo();
        File dir = repository.getDir();
        if (repo instanceof GithubRepository) {
            GithubRepository githubRepository = (GithubRepository) repo;
            GHRepository ghRepository = githubRepository.getRepository();

            List<GHPullRequest> pullRequests = ghRepository.getPullRequests(OPEN);

            String remoteURL = "git@github.com:" + ghRepository.getOwnerName() + "/" + ghRepository.getName();
            if (Commands.runCommandIgnoreOutput(dir, "git", "remote", "set-url", "origin", remoteURL) != 0) {
                LOG.warn("Could not set the remote URL of " + remoteURL);
            }

            GHPullRequest pullRequest = findPullRequest(context, pullRequests);
            String title = context.createTitle();
            if (pullRequest == null) {
                String localBranch = "updatebot-" + UUID.randomUUID().toString();
                doCommit(context, dir, localBranch);

                String body = title + "\n\nThis PR was generated by [UpdateBot](https://github.com/fabric8io/updatebot)";
                //String head = getGithubUsername() + ":" + localBranch;
                String head = localBranch;

                if (Commands.runCommand(dir, "git", "push", "-f", "origin", localBranch) != 0) {
                    LOG.warn("Failed to push branch " + localBranch + " for " + repository.getCloneUrl());
                    return;
                }
                pullRequest = ghRepository.createPullRequest(title, head, "master", body);
                LOG.info("Created pull request " + pullRequest.getHtmlUrl());
            } else {
                pullRequest.comment("Replacing previous commit");
                pullRequest.setTitle(title);

                GHCommitPointer head = pullRequest.getHead();
                String remoteRef = head.getRef();

                String localBranch = remoteRef;

                // lets remove any local branches of this name
                Commands.runCommandIgnoreOutput(dir, "git", "branch", "-D", localBranch);

                doCommit(context, dir, localBranch);

                if (Commands.runCommand(dir, "git", "push", "-f", "origin", localBranch + ":" + remoteRef) != 0) {
                    LOG.warn("Failed to push branch " + localBranch + " to existing github branch " + remoteRef + " for " + pullRequest.getHtmlUrl());
                }
                LOG.info("Updated PR " + pullRequest.getHtmlUrl());
            }

            // lets add the merge comment
            pullRequest.comment("[merge]");
        } else {
            // TODO what to do with vanilla git repos?
        }

    }

    private boolean doCommit(UpdateContext context, File dir, String branch) {
        String commitComment = context.createTitle();
        if (Commands.runCommandIgnoreOutput(dir, "git", "checkout", "-b", branch) == 0) {
            if (Commands.runCommandIgnoreOutput(dir, "git", "add", "*") == 0) {
                if (Commands.runCommand(dir, "git", "commit", "-m", commitComment) == 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Lets try find a pull request for previous PRs
     */
    protected GHPullRequest findPullRequest(UpdateContext context, List<GHPullRequest> pullRequests) {
        String prefix = context.createTitlePrefix();
        if (pullRequests != null) {
            for (GHPullRequest pullRequest : pullRequests) {
                String title = pullRequest.getTitle();
                if (title != null && title.startsWith(prefix)) {
                    return pullRequest;
                }
            }
        }
        return null;
    }
}
