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
package io.fabric8.updatebot.commands;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import io.fabric8.updatebot.CommandNames;
import io.fabric8.updatebot.Configuration;
import io.fabric8.updatebot.support.Strings;

import java.io.IOException;

/**
 * Displays help
 */
@Parameters(commandNames = CommandNames.HELP, commandDescription = "Displays help on using the available commands")
public class Help extends CommandSupport {
    @Parameter()
    private String command;

    private JCommander commander;


    @Override
    public void run(Configuration configuration) throws IOException {
        showUsage();
    }

    @Override
    public void run(CommandContext context) throws IOException {
        showUsage();
    }

    public void showUsage() {
        if (Strings.notEmpty(command)) {
            commander.usage(command);
        } else {
            commander.usage();
        }
    }

    public JCommander getCommander() {
        return commander;
    }

    public void setCommander(JCommander commander) {
        this.commander = commander;
    }
}
