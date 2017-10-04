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
package io.fabric8.updatebot.kind.npm;

import io.fabric8.updatebot.commands.CommandContext;
import io.fabric8.updatebot.support.FileDeleter;
import io.fabric8.updatebot.support.ProcessHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 */
public class DefaultNpmDependencyTreeGenerator implements NpmDependencyTreeGenerator {
    private static final transient Logger LOG = LoggerFactory.getLogger(DefaultNpmDependencyTreeGenerator.class);

    @Override
    public void generateDependencyTree(CommandContext context, String dependencyFileName) throws IOException {
        File dir = context.getDir();
        context.info(LOG, "Generating dependency tree file " + dependencyFileName + " in " + dir);

        ProcessHelper.runCommandIgnoreOutput(dir, "npm", "install");

        File outputFile = new File(dir, dependencyFileName);
        File errorFile = new File(dir, "npm-list-errors.log");
        try (FileDeleter ignored = new FileDeleter(errorFile)) {
            if (ProcessHelper.runCommand(dir, outputFile, errorFile, "npm", "list", "-json") != 0) {
                context.warn(LOG, "Failed to generate dependencies file " + outputFile);
            } else {
                LOG.debug("Generate dependencies file " + outputFile);
            }
        }

    }
}
