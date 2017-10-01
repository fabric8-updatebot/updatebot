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

import com.beust.jcommander.JCommander;
import io.fabric8.updatebot.commands.CommandContext;
import io.fabric8.updatebot.commands.CommandSupport;
import io.fabric8.updatebot.commands.Help;
import io.fabric8.updatebot.commands.ParentContext;
import io.fabric8.updatebot.commands.PullVersionChanges;
import io.fabric8.updatebot.commands.PushSourceChanges;
import io.fabric8.updatebot.commands.PushVersionChanges;
import io.fabric8.updatebot.commands.UpdatePullRequests;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.UpdateBotLogConfiguration;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.fabric8.updatebot.CommandNames.HELP;
import static io.fabric8.updatebot.CommandNames.PULL;
import static io.fabric8.updatebot.CommandNames.PUSH_SOURCE;
import static io.fabric8.updatebot.CommandNames.PUSH_VERSION;
import static io.fabric8.updatebot.CommandNames.UPDATE;

/**
 */
public class UpdateBot {
    private Configuration config = new Configuration();
    private CommandSupport lastCommend;
    private UpdatePullRequests updatePullRequests = new UpdatePullRequests();

    public static void main(String[] args) {
        try {
            new UpdateBot().run(args);
        } catch (IOException e) {
            System.err.println("Failed: " + e);
            e.printStackTrace();
            Throwable cause = e.getCause();
            if (cause != e) {
                System.out.println("Caused by: " + cause);
                cause.printStackTrace();
            }
            System.exit(1);
        }
    }

    /**
     * Parses the command from the given command line arguments or returns null if there is no command found
     */
    public static CommandSupport parseCommand(String[] args, Configuration config, boolean defaultToHelp) {
        PushSourceChanges pushSourceChanges = new PushSourceChanges();
        PushVersionChanges pushVersionChanges = new PushVersionChanges();
        PullVersionChanges pullVersionChanges = new PullVersionChanges();
        UpdatePullRequests updatePullRequests = new UpdatePullRequests();
        Help help = new Help();

        JCommander commander = JCommander.newBuilder()
                .addObject(config)
                .addCommand(HELP, help)
                .addCommand(PULL, pullVersionChanges)
                .addCommand(PUSH_SOURCE, pushSourceChanges)
                .addCommand(PUSH_VERSION, pushVersionChanges)
                .addCommand(UPDATE, updatePullRequests)
                .build();
        commander.setExpandAtSign(false);
        commander.setProgramName("updatebot");
        commander.parse(args);

        help.setCommander(commander);

        String parsedCommand = commander.getParsedCommand();
        if (parsedCommand != null) {
            switch (parsedCommand) {
                case HELP:
                    return help;

                case PULL:
                    return pullVersionChanges;

                case PUSH_SOURCE:
                    return pushSourceChanges;

                case PUSH_VERSION:
                    return pushVersionChanges;

                case UPDATE:
                    return updatePullRequests;
            }
        }
        if (defaultToHelp) {
            return help;
        }
        return null;
    }

    public Configuration getConfig() {
        return config;
    }

    public void setConfig(Configuration config) {
        this.config = config;
    }

    public void setLoggerOutput(PrintStream out) {
        UpdateBotLogConfiguration config = new UpdateBotLogConfiguration(out);
        config.init();

        Logger LOG = LoggerFactory.getLogger(UpdateBot.class);
        LOG.debug("Configured custom logger");
    }

    /**
     * Runs a command
     */
    public CommandSupport run(String[] args) throws IOException {
        CommandSupport command = parseCommand(args, config, true);
        this.lastCommend = command;
        command.run(config);
        return command;
    }

    /**
     * Returns the list of PullRequests / Issues and their status from the previous command
     */
    public List<Map<String, String>> poll() throws IOException {
        List<Map<String, String>> answer = new ArrayList<>();

        ParentContext context = updatePullRequests.run(getConfig());
        List<CommandContext> children = context.getChildren();
        for (CommandContext child : children) {
            Map<String, String> map = child.createStatusMap();
            answer.add(map);
        }
        return answer;
    }

    public CommandSupport getLastCommend() {
        return lastCommend;
    }
}
