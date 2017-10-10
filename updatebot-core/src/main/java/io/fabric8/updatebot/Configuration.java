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
import io.fabric8.updatebot.git.GitPluginCLI;
import io.fabric8.updatebot.kind.npm.DefaultNpmDependencyTreeGenerator;
import io.fabric8.updatebot.kind.npm.NpmDependencyTreeGenerator;
import io.fabric8.updatebot.support.Strings;
import io.fabric8.updatebot.support.Systems;
import org.fusesource.jansi.Ansi;
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

import static org.fusesource.jansi.Ansi.Color.BLUE;
import static org.fusesource.jansi.Ansi.Color.GREEN;
import static org.fusesource.jansi.Ansi.Color.MAGENTA;
import static org.fusesource.jansi.Ansi.Color.RED;
import static org.fusesource.jansi.Ansi.Color.YELLOW;
import static org.fusesource.jansi.Ansi.ansi;

/**
 * Common configuration parameters
 */
public class Configuration {
    public static final String DEFAULT_CONFIG_FILE = ".updatebot.yml";
    // ANSI escapes for various colors (or empty strings if no coloring is used)
    public static Ansi.Color
            COLOR_ERROR = RED,
            COLOR_COMPLETE = MAGENTA,
            COLOR_COMMAND = BLUE,
            COLOR_PENDING = GREEN,
            COLOR_WARNING = YELLOW;


    @Parameter(names = {"--github-pr-label", "-ghl"}, description = "GitHub Pull Request Label")
    private String githubPullRequestLabel = Systems.getConfigValue(EnvironmentVariables.GITHUB_PR_LABEL, "updatebot");
    @Parameter(names = {"--dry"}, description = "Dry Run mode does not perform any git commits")
    private boolean dryRun = Systems.isConfigFlag(EnvironmentVariables.DRY_RUN);
    private GitHub github;
    @Parameter(names = {"--config", "-c"}, description = "Location of the UpdateBot YAML configuration file")
    private String configFile = Systems.getConfigValue(EnvironmentVariables.CONFIG_FILE, DEFAULT_CONFIG_FILE);
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
    @Parameter(names = {"--https"}, description = "Whether to use HTTPS transport instead of git and SSH")
    private boolean useHttpsTransport;
    @Parameter(names = {"--disable-ansi"}, description = "Whether to disable the use of ANSI colours in the output")
    private boolean disableAnsi;

    @Parameter(names = {"--mvn"}, description = "The location of the `mvn` executable for invoking maven")
    private String mvnCommand = Systems.getConfigValue(EnvironmentVariables.MVN_COMMAND, "mvn");
    @Parameter(names = {"--npm"}, description = "The location of the `npm` executable for invoking nodejs tooling")
    private String npmCommand = Systems.getConfigValue(EnvironmentVariables.NPM_COMMAND, "npm");

    private File sourceDir;
    private boolean rebaseMode = true;
    private NpmDependencyTreeGenerator npmDependencyTreeGenerator = new DefaultNpmDependencyTreeGenerator();
    private boolean pullDisabled;
    private Map<String, String> pollStatusCache = new TreeMap<>();
    private PrintStream printStream;
    private GitPlugin git = new GitPluginCLI(this);
    private Map<String, String> mvnEnvironmentVariables;
    private Map<String, String> npmEnvironmentVariables;
    private boolean ansiInitialised;
    private boolean useAnsi;

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

    public boolean isUseHttpsTransport() {
        return useHttpsTransport;
    }

    public void setUseHttpsTransport(boolean useHttpsTransport) {
        this.useHttpsTransport = useHttpsTransport;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public boolean isDisableAnsi() {
        return disableAnsi;
    }

    public void setDisableAnsi(boolean disableAnsi) {
        this.disableAnsi = disableAnsi;
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

    public String getMvnCommand() {
        return mvnCommand;
    }

    public void setMvnCommand(String mvnCommand) {
        this.mvnCommand = mvnCommand;
    }

    public String getNpmCommand() {
        return npmCommand;
    }

    public void setNpmCommand(String npmCommand) {
        this.npmCommand = npmCommand;
    }


    public Map<String, String> getMvnEnvironmentVariables() {
        return mvnEnvironmentVariables;
    }

    public void setMvnEnvironmentVariables(Map<String, String> mvnEnvironmentVariables) {
        this.mvnEnvironmentVariables = mvnEnvironmentVariables;
    }

    public Map<String, String> getNpmEnvironmentVariables() {
        return npmEnvironmentVariables;
    }

    public void setNpmEnvironmentVariables(Map<String, String> npmEnvironmentVariables) {
        this.npmEnvironmentVariables = npmEnvironmentVariables;
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
            printStream.println(colored(COLOR_WARNING, "WARNING: " + message));
        } else {
            log.warn(message);
        }
    }

    public void warn(Logger log, String message, Throwable e) {
        if (printStream != null) {
            printStream.println(colored(COLOR_WARNING, "WARNING: " + message + " " + e));
            e.printStackTrace(printStream);
        } else {
            log.warn(message, e);
        }
    }

    public void error(Logger log, String message) {
        if (printStream != null) {
            printStream.println(colored(COLOR_ERROR, "ERROR: " + message));
        } else {
            log.warn(message);
        }
    }


    public void error(Logger log, String message, Throwable e) {
        if (printStream != null) {
            printStream.println(colored(COLOR_ERROR, "ERROR: " + message + " " + e));
            e.printStackTrace(printStream);
        } else {
            log.warn(message, e);
        }
    }

    private boolean useAnsiColor() {
        if (!ansiInitialised) {
            this.useAnsi = !isDisableAnsi() && System.console() != null && !isWindows();
            if (useAnsi) {
                //AnsiConsole.systemInstall();
                Ansi.setEnabled(true);
            } else {
                Ansi.setEnabled(false);
            }
            ansiInitialised = true;
        }
        return useAnsi;
    }

    private boolean isWindows() {
        String os = System.getProperty("os.name");
        return os != null && os.toLowerCase().startsWith("windows");
    }


    public String colored(Ansi.Color color, String message) {
        if (!useAnsiColor()) {
            return message;
        }
        Ansi ansi = ansi().fg(color);
        return ansi.a(message).reset().toString();
    }

}
