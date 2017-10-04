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

import com.beust.jcommander.Parameter;
import io.fabric8.updatebot.git.GitPlugin;
import io.fabric8.updatebot.kind.npm.DefaultNpmDependencyTreeGenerator;
import io.fabric8.updatebot.kind.npm.NpmDependencyTreeGenerator;
import io.fabric8.updatebot.support.Strings;
import io.fabric8.updatebot.support.Systems;
import org.kohsuke.github.AbuseLimitHandler;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.RateLimitHandler;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.TreeMap;

/**
 * Common configuration parameters
 */
public class Configuration {
    public GitPlugin git = new GitPlugin(this);
    @Parameter(names = {"--github-pr-label", "-ghl"}, description = "GitHub Pull Request Label")
    private String githubPullRequestLabel = Systems.getConfigValue(EnvironmentVariables.GITHUB_PR_LABEL, "updatebot");
    @Parameter(names = {"--dry"}, description = "Dry Run mode does not perform any git commits")
    private boolean dryRun = Systems.isConfigFlag(EnvironmentVariables.DRY_RUN);
    private GitHub github;
    @Parameter(names = {"--config", "-c"}, description = "Location of the UpdateBot YAML configuration file")
    private String configFile = Systems.getConfigValue(EnvironmentVariables.CONFIG_FILE, ".updatebot.yml");
    @Parameter(names = {"--work-dir", "-wd"}, description = "The work directory where other downstream projects are cloned")
    private String workDir = Systems.getConfigValue(EnvironmentVariables.WORK_DIR, "./.updatebot-repos");
    @Parameter(names = {"--github-username", "-ghu"}, description = "GitHub Username")
    private String githubUsername = Systems.getConfigValue(EnvironmentVariables.GITHUB_USER);
    @Parameter(names = {"--github-password", "-ghp"}, description = "GitHub Password")
    private String githubPassword = Systems.getConfigValue(EnvironmentVariables.GITHUB_PASSWORD);
    @Parameter(names = {"--github-token", "-ght"}, description = "GitHub Token")
    private String githubToken = Systems.getConfigValue(EnvironmentVariables.GITHUB_TOKEN);
    @Parameter(names = "--check", description = "Whether or not we should check dependencies are valid before submitting Pull Requests", arity = 1)
    private boolean checkDependencies = true;
    @Parameter(names = {"--dir", "-d"}, description = "The source directory containing the git clone of the source to process")
    private String sourcePath;
    private File sourceDir;
    private boolean rebaseMode = true;
    private NpmDependencyTreeGenerator npmDependencyTreeGenerator = new DefaultNpmDependencyTreeGenerator();
    private boolean pullDisabled;
    private Map<String, String> pollStatusCache = new TreeMap<>();
    private PrintStream printStream;

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
            ghb.withRateLimitHandler(RateLimitHandler.WAIT).
                    withAbuseLimitHandler(AbuseLimitHandler.WAIT);
            this.github = ghb.build();
        }
        return this.github;
    }

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

    public String getGithubPullRequestLabel() {
        return githubPullRequestLabel;
    }

    public void setGithubPullRequestLabel(String githubPullRequestLabel) {
        this.githubPullRequestLabel = githubPullRequestLabel;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public boolean isRebaseMode() {
        return rebaseMode;
    }

    public void setRebaseMode(boolean rebaseMode) {
        this.rebaseMode = rebaseMode;
    }

    public boolean isCheckDependencies() {
        return checkDependencies;
    }

    public void setCheckDependencies(boolean checkDependencies) {
        this.checkDependencies = checkDependencies;
    }

    public NpmDependencyTreeGenerator getNpmDependencyTreeGenerator() {
        return npmDependencyTreeGenerator;
    }

    public void setNpmDependencyTreeGenerator(NpmDependencyTreeGenerator npmDependencyTreeGenerator) {
        this.npmDependencyTreeGenerator = npmDependencyTreeGenerator;
    }

    public boolean isPullDisabled() {
        return pullDisabled;
    }

    /**
     * Allows pulling to be disabled which is handy for test cases where we commit to master without pushing
     *
     * @param pullDisabled
     */
    public void setPullDisabled(boolean pullDisabled) {
        this.pullDisabled = pullDisabled;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public File getSourceDir() {
        if (sourceDir == null) {
            if (sourcePath == null) {
                sourcePath = ".";
            }
            sourceDir = new File(sourcePath);
            if (!sourceDir.exists()) {
                // lets check for a URI instead from Jenkins
                URI uri = null;
                try {
                    uri = new URI(sourcePath);
                } catch (URISyntaxException e) {
                    // ignore
                }
                if (uri != null) {
                    sourceDir = new File(uri);
                }
            }
        }
        return sourceDir;
    }

    public void setSourceDir(File sourceDir) {
        this.sourceDir = sourceDir;
    }

    /**
     * Returns a cache indexed by the cloneUrl of the polling status for each repository
     */
    public Map<String, String> getPollStatusCache() {
        return pollStatusCache;
    }

    public PrintStream getPrintStream() {
        return printStream;
    }

    public void setPrintStream(PrintStream printStream) {
        this.printStream = printStream;
    }

    public GitPlugin getGit() {
        return git;
    }

    public void setGit(GitPlugin git) {
        this.git = git;
    }

    public void info(Logger log, String message) {
        if (printStream != null) {
            printStream.println(message);
        } else {
            log.info(message);
        }
    }

    public void warn(Logger log, String message) {
        if (printStream != null) {
            printStream.println("WARNING: " + message);
        } else {
            log.warn(message);
        }
    }

    public void warn(Logger log, String message, Throwable e) {
        if (printStream != null) {
            printStream.println("WARNING: " + message + " " + e);
            e.printStackTrace(printStream);
        } else {
            log.warn(message, e);
        }
    }
}
