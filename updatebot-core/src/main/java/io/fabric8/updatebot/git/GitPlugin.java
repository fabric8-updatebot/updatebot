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
import io.fabric8.utils.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 */
public class GitPlugin {
    private static final transient Logger LOG = LoggerFactory.getLogger(GitPlugin.class);
    private final Configuration configuration;

    public GitPlugin(Configuration configuration) {
        this.configuration = configuration;
    }


    public void setRemoteURL(File dir, String remoteURL) {
        if (ProcessHelper.runCommandIgnoreOutput(dir, "git", "remote", "set-url", "origin", remoteURL) != 0) {
            configuration.warn(LOG, "Could not set the remote URL of " + remoteURL);
        }
    }

    public boolean push(File dir, String localBranch) {
        return ProcessHelper.runCommandAndLogOutput(configuration, LOG, dir, "git", "push", "-f", "origin", localBranch);
    }

    public void pull(File dir, String cloneUrl) {
        LOG.debug("Pulling: " + dir + " repo: " + cloneUrl);
        ProcessHelper.runCommandAndLogOutput(configuration, LOG, dir, "git", "pull");
    }

    public void clone(File dir, String cloneUrl, String name) {
        ProcessHelper.runCommandAndLogOutput(configuration, LOG, dir, "git", "clone", cloneUrl, name);
    }

    public boolean commitToBranch(File dir, String branch, String commitComment) {
        if (ProcessHelper.runCommandIgnoreOutput(dir, "git", "checkout", "-b", branch) == 0) {
            return addAndCommit(dir, commitComment);
        }
        return false;
    }

    public void deleteBranch(File dir, String localBranch) {
        ProcessHelper.runCommandIgnoreOutput(dir, "git", "branch", "-D", localBranch);
    }

    public boolean addAndCommit(File dir, String commitComment) {
        if (ProcessHelper.runCommandIgnoreOutput(dir, "git", "add", "*") == 0) {
            if (ProcessHelper.runCommand(dir, "git", "commit", "-m", commitComment) == 0) {
                return true;
            }
        }
        return false;
    }

    public boolean stashAndCheckoutMaster(File dir) {
        if (ProcessHelper.runCommandIgnoreOutput(dir, "git", "stash") == 0) {
            if (ProcessHelper.runCommandIgnoreOutput(dir, "git", "checkout", "master") == 0) {
                return true;
            }
        }
        LOG.warn("Failed to checkout master in " + dir);
        return false;
    }

    public void revertChanges(File dir) throws IOException {
        if (ProcessHelper.runCommandIgnoreOutput(dir, "git", "stash") != 0) {
            throw new IOException("Failed to stash old changes!");
        }
    }

    /**
     * Returns true if the given directory has modified files
     */
    public static boolean hasChangedFiles(File dir) {
        try {
            String output = ProcessHelper.runCommandCaptureOutput(dir, "git", "status", "-s");
            if (output != null) {
                output = output.trim();
            }
            return Strings.notEmpty(output);
        } catch (IOException e) {
            return false;
        }
    }
}
