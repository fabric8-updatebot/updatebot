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
package io.jenkins.updatebot.commands;

import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static io.jenkins.updatebot.github.PullRequests.COMMAND_COMMENT_INDENT;
import static io.jenkins.updatebot.github.PullRequests.COMMAND_COMMENT_PREFIX;
import static io.jenkins.updatebot.github.PullRequests.COMMAND_COMMENT_PREFIX_SEPARATOR;

/**
 * Represents a list of commands to execute.
 * This class is usually created by parsing commands from comments on a PR
 */
public class CompositeCommand extends CommandSupport {
    private List<CommandSupport> commands = new ArrayList<>();

    public CompositeCommand() {
    }

    @Override
    protected void appendPullRequestComment(StringBuilder builder) {
        for (CommandSupport child : getCommands()) {
            child.appendPullRequestComment(builder);
        }
    }

    /**
     * Creates the updatebot command that we add as a comment so that we can re-run the commands later on for rebasing
     */
    public String createPullRequestComment() {
        StringBuilder builder = new StringBuilder(COMMAND_COMMENT_PREFIX);
        builder.append(COMMAND_COMMENT_PREFIX_SEPARATOR);
        boolean first = true;
        for (CommandSupport child : getCommands()) {
            builder.append(COMMAND_COMMENT_INDENT);
            builder.append(child.createPullRequestComment());
            builder.append("\n");
        }
        return builder.toString();
    }


    public void run(CommandContext originalContext) throws IOException {
        CommandContext parentContext = new CommandContext(originalContext.getRepository(), originalContext.getConfiguration());

        for (CommandSupport command : getCommands()) {
            File dir = parentContext.getRepository().getDir();
            dir.getParentFile().mkdirs();
            command.run(parentContext);
        }
    }


    /**
     * Invoked from a polling/update command
     */
    public void run(CommandContext context, GHRepository ghRepository, GHPullRequest pullRequest) throws IOException {
        for (CommandSupport command : getCommands()) {
            command.validateConfiguration(context.getConfiguration());
            if (command instanceof ModifyFilesCommandSupport) {
                ModifyFilesCommandSupport modifyCommand = (ModifyFilesCommandSupport) command;
                modifyCommand.run(context, ghRepository, pullRequest);
            }
        }
    }


    /**
     * Returns the commands in this set
     */
    public List<CommandSupport> getCommands() {
        return commands;
    }

    public void addCommand(CommandSupport command) {
        commands.add(command);

    }
}
