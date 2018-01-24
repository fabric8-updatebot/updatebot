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
package io.jenkins.updatebot.support;

import io.jenkins.updatebot.Configuration;
import io.fabric8.utils.Files;
import io.fabric8.utils.IOHelpers;
import io.fabric8.utils.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 */
public class ProcessHelper {
    private static final transient Logger LOG = LoggerFactory.getLogger(ProcessHelper.class);

    public static int runCommand(File dir, String... commands) {
        return runCommand(dir, Collections.EMPTY_MAP, true, commands);
    }

    public static int runCommandIgnoreOutput(File dir, Map<String, String> environmentVariables, String... commands) {
        return runCommand(dir, environmentVariables, false, commands);
    }

    public static int runCommandIgnoreOutput(File dir, String... commands) {
        return runCommand(dir, Collections.EMPTY_MAP, false, commands);
    }

    public static int runCommand(File dir, Map<String, String> environmentVariables, boolean inheritIO, String... commands) {
        ProcessBuilder builder = new ProcessBuilder(commands);
        builder.directory(dir);
        applyEnvironmentVariables(builder, environmentVariables);
        if (inheritIO) {
            builder.inheritIO();
        }
        return doRunCommand(builder, commands);
    }

    public static String runCommandCaptureOutput(File dir, String... commands) throws IOException {
        return runCommandCaptureOutput(dir, Collections.EMPTY_MAP, commands);
    }

    public static String runCommandCaptureOutput(File dir, Map<String, String> environmentVariables, String... commands) throws IOException {
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
        return runCommand(dir, Collections.EMPTY_MAP, outputFile, errorFile, commands);
    }

    public static int runCommand(File dir, Map<String, String> environmentVariables, File outputFile, File errorFile, String... commands) {
        ProcessBuilder builder = new ProcessBuilder(commands);
        builder.directory(dir);
        applyEnvironmentVariables(builder, environmentVariables);
        builder.redirectOutput(outputFile);
        builder.redirectError(errorFile);
        return doRunCommand(builder, commands);
    }


    public static boolean runCommandAndLogOutput(File dir, String... commands) {
        File outputFile = new File(dir, "target/updatebot.log");
        File errorFile = new File(dir, "target/updatebot.err");
        try (FileDeleter ignored = new FileDeleter(outputFile, errorFile)) {
            outputFile.getParentFile().mkdirs();
            boolean answer = true;
            if (runCommand(dir, outputFile, errorFile, commands) != 0) {
                LOG.warn("Failed to run " + String.join(" ", commands));
                answer = false;
            }
            logOutput(outputFile, false);
            logOutput(errorFile, true);
            return answer;
        } catch (IOException e) {
            LOG.warn("Caught: " + e, e);
            return false;
        }
    }

    public static boolean runCommandAndLogOutput(Configuration configuration, Logger log, File dir, String... commands) {
        return runCommandAndLogOutput(configuration, log, dir, Collections.EMPTY_MAP, commands);
    }

    public static boolean runCommandAndLogOutput(Configuration configuration, Logger log, File dir, boolean useError, String... commands) {
        return runCommandAndLogOutput(configuration, log, dir, Collections.EMPTY_MAP, useError, commands);
    }

    public static boolean runCommandAndLogOutput(Configuration configuration, Logger log, File dir, Map<String, String> environmentVariables, String... commands) {
        return runCommandAndLogOutput(configuration, log, dir, environmentVariables, true, commands);
    }

    public static boolean runCommandAndLogOutput(Configuration configuration, Logger log, File dir, Map<String, String> environmentVariables, boolean useError, String... commands) {
        File outputFile = new File(dir, "target/updatebot.log");
        File errorFile = new File(dir, "target/updatebot.err");
        try (FileDeleter ignored = new FileDeleter(outputFile, errorFile)) {
            outputFile.getParentFile().mkdirs();
            boolean answer = true;
            if (runCommand(dir, environmentVariables, outputFile, errorFile, commands) != 0) {
                LOG.error("Failed to run " + String.join(" ", commands));
                answer = false;
            }
            logOutput(configuration, log, outputFile, false);
            logOutput(configuration, log, errorFile, useError);
            return answer;
        } catch (IOException e) {
            LOG.warn("Caught: " + e, e);
            return false;
        }
    }

    public static void logOutput(Configuration configuration, Logger log, File file, boolean error) {
        logOutput(configuration, log, loadFile(file), error);
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

    protected static void logOutput(Configuration configuration, Logger log, String output, boolean error) {
        if (Strings.notEmpty(output)) {
            String[] lines = output.split("\n");
            for (String line : lines) {
                if (error) {
                    configuration.error(log, line);
                } else {
                    configuration.info(log, line);
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


    protected static void applyEnvironmentVariables(ProcessBuilder builder, Map<String, String> environmentVariables) {
        if (environmentVariables != null) {
            for (Map.Entry<String, String> entry : environmentVariables.entrySet()) {
                builder.environment().put(entry.getKey(), entry.getValue());
            }
        }
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
}
