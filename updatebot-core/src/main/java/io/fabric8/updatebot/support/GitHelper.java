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
package io.fabric8.updatebot.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 */
public class GitHelper {
    private static final transient Logger LOG = LoggerFactory.getLogger(GitHelper.class);

    public static boolean gitAddAndCommit(File dir, String commitComment) {
        if (Commands.runCommandIgnoreOutput(dir, "git", "add", "*") == 0) {
            if (Commands.runCommand(dir, "git", "commit", "-m", commitComment) == 0) {
                return true;
            }
        }
        return false;
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

    public static void revertChanges(File dir) throws IOException {
        if (Commands.runCommandIgnoreOutput(dir, "git", "stash") != 0) {
            throw new IOException("Failed to stash old changes!");
        }
    }
}
