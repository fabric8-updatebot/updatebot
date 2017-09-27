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
import io.fabric8.updatebot.commands.CommandSupport;
import io.fabric8.updatebot.commands.Help;
import io.fabric8.updatebot.commands.PullVersionChanges;
import io.fabric8.updatebot.commands.PushVersionChanges;
import io.fabric8.updatebot.commands.UpdatePullRequests;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static io.fabric8.updatebot.CommandNames.HELP;
import static io.fabric8.updatebot.CommandNames.PULL;
import static io.fabric8.updatebot.CommandNames.PUSH_VERSION;
import static io.fabric8.updatebot.CommandNames.UPDATE;

/**
 */
public class UpdateBot {
    private static final transient Logger LOG = LoggerFactory.getLogger(UpdateBot.class);

    public static void main(String[] args) {
        try {
            Configuration config = new Configuration();
            CommandSupport command = parseCommand(args, config, true);
            command.run(config);
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
        PushVersionChanges pushVersionChanges = new PushVersionChanges();
        PullVersionChanges pullVersionChanges = new PullVersionChanges();
        UpdatePullRequests updatePullRequests = new UpdatePullRequests();
        Help help = new Help();

        JCommander commander = JCommander.newBuilder()
                .addObject(config)
                .addCommand(HELP, help)
                .addCommand(PULL, pullVersionChanges)
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
}
