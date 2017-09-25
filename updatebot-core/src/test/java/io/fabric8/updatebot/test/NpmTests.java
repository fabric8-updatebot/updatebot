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
package io.fabric8.updatebot.test;

import com.fasterxml.jackson.databind.JsonNode;
import io.fabric8.updatebot.model.GitRepository;
import io.fabric8.updatebot.model.GithubRepository;
import io.fabric8.updatebot.repository.LocalRepository;
import io.fabric8.updatebot.support.Commands;
import io.fabric8.updatebot.support.MarkupHelper;
import io.fabric8.utils.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 */
public class NpmTests {
    private static final transient Logger LOG = LoggerFactory.getLogger(NpmTests.class);


    public static void generateDummyPackageJsonCommit(List<LocalRepository> localRepositories, String value, String... paths) {
        for (LocalRepository repository : localRepositories) {
            GitRepository repo = repository.getRepo();
            if (repo instanceof GithubRepository) {
                File dir = repository.getDir();

                Commands.runCommand(dir, "git", "checkout", "master");

                boolean changed = false;
                if (generateDummyChangePackageJson(dir, value, paths)) {
                    changed = true;
                }
                if (changed) {
                    Commands.runCommand(dir, "git", "add", "*");
                    if (Commands.runCommand(dir, "git", "commit", "-m", "Dummy commit to test rebase at " + new Date()) == 0) {
                        if (Commands.runCommand(dir, "git", "push", "origin", "master") == 0) {
                            break;
                        }
                    }
                    LOG.warn("Failed to commit and push a dummy change in " + dir);
                }
            }
        }
    }

    private static boolean generateDummyChangePackageJson(File dir, String value, String... paths) {
        File file = new File(dir, "package.json");
        if (Files.isFile(file)) {
            try {
                JsonNode jsonNode = MarkupHelper.loadJson(file);
                MarkupAssertions.assertSetValue(jsonNode, value, paths);
                MarkupHelper.savePrettyJson(file, jsonNode);
                return true;
            } catch (IOException e) {
                LOG.warn("Failed to modify " + file + " due to: " + e, e);
            }
        }
        return false;
    }
}
