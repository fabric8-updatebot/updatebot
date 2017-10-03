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

import io.fabric8.utils.Files;
import io.fabric8.utils.IOHelpers;
import io.fabric8.utils.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 */
public class ProcessHelper {
    private static final transient Logger LOG = LoggerFactory.getLogger(ProcessHelper.class);

    public static int runCommand(File dir, String... commands) {
        return runCommand(dir, true, commands);
    }

    public static int runCommandIgnoreOutput(File dir, String... commands) {
        return runCommand(dir, false, commands);
    }

    public static int runCommand(File dir, boolean inheritIO, String... commands) {
        ProcessBuilder builder = new ProcessBuilder(commands);
        builder.directory(dir);
        if (inheritIO) {
            builder.inheritIO();
        }
        return doRunCommand(builder, commands);
    }

    public static String runCommandCaptureOutput(File dir, String... commands) throws IOException {
        File outputFile;
        File errorFile;
        try {
            outputFile = File.createTempFile("updatebot-", ".log");
            errorFile = File.createTempFile("updatebot-", ".err");
        } catch (IOException e) {
            throw new IOException("Failed to create temporary files " + e, e);
        }

        int result = runCommand(dir, outputFile, errorFile, commands);
        String output = loadFile(outputFile);
        String err = loadFile(errorFile);
        logOutput(err, true);
        if (result != 0) {
            LOG.warn("Failed to run commands " + String.join(" ", commands) + " result: " + result);
            logOutput(output, false);
            throw new IOException("Failed to run commands " + String.join(" ", commands) + " result: " + result);
        }
        return output;
    }

    public static int runCommand(File dir, File outputFile, File errorFile, String... commands) {
        ProcessBuilder builder = new ProcessBuilder(commands);
        builder.directory(dir);
        builder.redirectOutput(outputFile);
        builder.redirectError(errorFile);
        return doRunCommand(builder, commands);
    }

    protected static int doRunCommand(ProcessBuilder builder, String[] commands) {
        String line = String.join(" ", commands);
        try {
            Process process = builder.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                LOG.warn("Failed to run command " + line + " in " + builder.directory() + " : exit " + exitCode);
            }
            return exitCode;
        } catch (IOException e) {
            LOG.warn("Failed to run command " + line + " in " + builder.directory() + " : error " + e);
        } catch (InterruptedException e) {
            // ignore
        }
        return 1;
    }

    public static boolean runCommandAndLogOutput(File dir, String[] commands) {
        File outputFile = new File(dir, "target/updatebot.log");
        File errorFile = new File(dir, "target/updatebot.err");
        outputFile.getParentFile().mkdirs();
        boolean answer = true;
        if (runCommand(dir, outputFile, errorFile, commands) != 0) {
            LOG.warn("Failed to run " + String.join(" ", commands));
            answer = false;
        }
        logOutput(outputFile, false);
        logOutput(errorFile, true);
        return answer;
    }

    public static void logOutput(File file, boolean error) {
        logOutput(loadFile(file), error);
    }

    protected static void logOutput(String output, boolean error) {
        if (Strings.notEmpty(output)) {
            String[] lines = output.split("\n");
            for (String line : lines) {
                if (error) {
                    LOG.error(line);
                } else {
                    LOG.info(line);
                }
            }
        }
    }

    protected static String loadFile(File file) {
        String output = null;
        if (Files.isFile(file)) {
            try {
                output = IOHelpers.readFully(file);
            } catch (IOException e) {
                LOG.error("Failed to load " + file + ". " + e, e);
            }
        }
        return output;
    }
}
