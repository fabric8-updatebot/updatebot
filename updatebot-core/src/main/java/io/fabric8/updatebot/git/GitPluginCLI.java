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
package io.fabric8.updatebot.git;

import io.fabric8.updatebot.Configuration;
import io.fabric8.updatebot.support.ProcessHelper;
import io.fabric8.updatebot.support.Strings;
import org.kohsuke.github.GHMyself;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 */
public class GitPluginCLI implements GitPlugin {
    private static final transient Logger LOG = LoggerFactory.getLogger(GitPluginCLI.class);
    private final Configuration configuration;

    public GitPluginCLI(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void setRemoteURL(File dir, String remoteURL) {
        if (ProcessHelper.runCommandIgnoreOutput(dir, "git", "remote", "set-url", "origin", remoteURL) != 0) {
            configuration.warn(LOG, "Could not set the remote URL of " + remoteURL);
        }
    }

    @Override
    public boolean push(File dir, String localBranch) {
        // this option leaks the secure git URL... - we could try filter it out?
        //return ProcessHelper.runCommandAndLogOutput(configuration, LOG, dir, "git", "push", "-f", "origin", localBranch);
        return ProcessHelper.runCommandIgnoreOutput(dir, "git", "push", "-f", "origin", localBranch) == 0;
    }

    @Override
    public void pull(File dir, String cloneUrl) {
        LOG.debug("Pulling: " + dir + " repo: " + cloneUrl);
        ProcessHelper.runCommandAndLogOutput(configuration, LOG, dir, "git", "pull");
    }

    @Override
    public void clone(File dir, String cloneUrl, String repoName) {
        ProcessHelper.runCommandAndLogOutput(configuration, LOG, dir, "git", "clone", cloneUrl, repoName);
    }

    @Override
    public void configUserNameAndEmail(File dir) {
        String email = null;
        String personName = null;
        try {
            GitHub github = configuration.getGithub();
            if (github != null) {
                GHMyself myself = github.getMyself();
                if (myself != null) {
                    email = myself.getEmail();
                    personName = myself.getName();

                }
            }
        } catch (IOException e) {
            configuration.warn(LOG, "Failed to load github username and email: " + e, e);
        }
        if (Strings.notEmpty(email)) {
            ProcessHelper.runCommandAndLogOutput(configuration, LOG, dir, "git", "config", "user.email", email);
        }
        if (Strings.notEmpty(personName)) {
            ProcessHelper.runCommandAndLogOutput(configuration, LOG, dir, "git", "config", "user.name", personName);
        }
    }

    @Override
    public boolean commitToBranch(File dir, String branch, String commitComment) {
        if (ProcessHelper.runCommandIgnoreOutput(dir, "git", "checkout", "-b", branch) == 0) {
            return addAndCommit(dir, commitComment);
        }
        return false;
    }

    @Override
    public void deleteBranch(File dir, String localBranch) {
        ProcessHelper.runCommandIgnoreOutput(dir, "git", "branch", "-D", localBranch);
    }

    @Override
    public boolean addAndCommit(File dir, String commitComment) {
        if (ProcessHelper.runCommandIgnoreOutput(dir, "git", "add", "*") == 0) {
            if (ProcessHelper.runCommand(dir, "git", "commit", "-m", commitComment) == 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean stashAndCheckoutMaster(File dir) {
        if (ProcessHelper.runCommandIgnoreOutput(dir, "git", "stash") == 0) {
            if (ProcessHelper.runCommandIgnoreOutput(dir, "git", "checkout", "master") == 0) {
                return true;
            }
        }
        LOG.warn("Failed to checkout master in " + dir);
        return false;
    }

    @Override
    public void revertChanges(File dir) throws IOException {
        if (ProcessHelper.runCommandIgnoreOutput(dir, "git", "stash") != 0) {
            throw new IOException("Failed to stash old changes!");
        }
    }
}
