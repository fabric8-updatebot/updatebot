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
package io.jenkins.updatebot;

import com.beust.jcommander.JCommander;
import io.jenkins.updatebot.commands.CommandContext;
import io.jenkins.updatebot.commands.CommandSupport;
import io.jenkins.updatebot.commands.Help;
import io.jenkins.updatebot.commands.ParentContext;
import io.jenkins.updatebot.commands.PullVersionChanges;
import io.jenkins.updatebot.commands.PushSourceChanges;
import io.jenkins.updatebot.commands.PushVersionChanges;
import io.jenkins.updatebot.commands.StatusInfo;
import io.jenkins.updatebot.commands.UpdatePullRequests;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.jenkins.updatebot.CommandNames.HELP;
import static io.jenkins.updatebot.CommandNames.PULL;
import static io.jenkins.updatebot.CommandNames.PUSH_SOURCE;
import static io.jenkins.updatebot.CommandNames.PUSH_VERSION;
import static io.jenkins.updatebot.CommandNames.UPDATE;

/**
 */
public class UpdateBot {
    private Configuration configuration = new Configuration();
    private CommandSupport command;
    private UpdatePullRequests updatePullRequests = new UpdatePullRequests();
    private Logger LOG;
    private Map<String, StatusInfo> lastStatusMap = new HashMap();

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

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public void setLoggerOutput(PrintStream out) {
        getConfiguration().setPrintStream(out);
    }

    /**
     * Runs a command
     */
    public CommandSupport run(String[] args) throws IOException {
        CommandSupport command = parseCommand(args, configuration, true);
        this.command = command;
        command.run(configuration);
        return command;
    }

    /**
     * Returns the list of PullRequests / Issues and their status from the previous command
     */
    public Map<String, StatusInfo> poll() throws IOException {
        Map<String, StatusInfo> answer = new LinkedHashMap();

        Configuration configuration = getConfiguration();
        ParentContext context = updatePullRequests.run(configuration);
        List<CommandContext> children = context.getChildren();
        for (CommandContext child : children) {
            StatusInfo status = child.createStatusInfo();
            answer.put(status.getCloneUrl(), status);
        }

        // lets get the previous state and compare them then log the differences
        Collection<StatusInfo> changes;
        boolean logBlankLineAfter = false;
        if (lastStatusMap.isEmpty() && !answer.isEmpty()) {
            changes = answer.values();
            configuration.info(LOG, "");
            configuration.info(LOG, "");
            logBlankLineAfter = true;
        } else {
            changes = StatusInfo.changedStatuses(configuration, lastStatusMap, answer).values();
        }
        for (StatusInfo change : changes) {
            configuration.info(LOG, change.description(configuration));
        }
        if (logBlankLineAfter) {
            configuration.info(LOG, "");
        }
        this.lastStatusMap = answer;
        return answer;
    }

    public Logger getLOG() {
        if (LOG == null) {
            LOG = LoggerFactory.getLogger(UpdateBot.class);
        }
        return LOG;
    }

    public CommandSupport getCommand() {
        return command;
    }

    public void setCommand(CommandSupport command) throws IOException {
        this.command = command;
        if (command != null) {
            updatePullRequests.setLastCommand(command, configuration);
        }
    }
}
