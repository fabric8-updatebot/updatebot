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

import io.fabric8.updatebot.support.ProcessHelper;
import io.fabric8.utils.Strings;

import java.io.File;
import java.io.IOException;

/**
 */
public interface GitPlugin {
    /**
     * Returns true if the given directory has modified files
     */
    static boolean hasChangedFiles(File dir) {
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

    void setRemoteURL(File dir, String remoteURL);

    boolean push(File dir, String localBranch);

    void pull(File dir, String cloneUrl);

    void clone(File dir, String cloneUrl, String name);

    void configUserNameAndEmail(File dir);

    boolean commitToBranch(File dir, String branch, String commitComment);

    void deleteBranch(File dir, String localBranch);

    boolean addAndCommit(File dir, String commitComment);

    boolean stashAndCheckoutMaster(File dir);

    void revertChanges(File dir) throws IOException;
}
