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
public class Commands {
    private static final transient Logger LOG = LoggerFactory.getLogger(Commands.class);

    public static int runCommand(File dir, String... commands) {
        return runCommand(dir, true, commands);
    }

    public static int runCommandIgnoreOutput(File dir, String... commands) {
        return runCommand(dir, false, commands);
    }

    public static int runCommand(File dir, boolean inheritIO, String... commands) {
        String line = String.join(" ", commands);
        ProcessBuilder builder = new ProcessBuilder(commands);
        builder.directory(dir);
        if (inheritIO) {
            builder.inheritIO();
        }
        try {
            Process process = builder.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                LOG.warn("Failed to run command " + line + " : exit " + exitCode);
            }
            return exitCode;
        } catch (IOException e) {
            LOG.warn("Failed to run command " + line + " : error " + e);
        } catch (InterruptedException e) {
            // ignore
        }
        return 1;
    }
}
