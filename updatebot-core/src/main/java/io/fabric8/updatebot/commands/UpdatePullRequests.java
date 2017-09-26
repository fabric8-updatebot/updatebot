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

import com.beust.jcommander.Parameters;
import io.fabric8.updatebot.CommandNames;
import io.fabric8.updatebot.Configuration;
import io.fabric8.updatebot.UpdateBot;
import io.fabric8.updatebot.support.GitHubHelpers;
import io.fabric8.updatebot.support.PullRequests;
import io.fabric8.updatebot.support.Strings;
import io.fabric8.utils.Objects;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static io.fabric8.updatebot.support.PullRequests.UPDATEBOT;

/**
 * Updates any open pull requests, rebasing any that require rebasing, merging any that are ready or responding to comments
 */
@Parameters(commandNames = CommandNames.UPDATE, commandDescription = "Updates open Pull Requests. Rebases any unmergable PRs or merge any PRs that are ready.")
public class UpdatePullRequests extends CommandSupport {
    private static final transient Logger LOG = LoggerFactory.getLogger(UpdatePullRequests.class);

    @Override
    public void run(CommandContext context) throws IOException {
        GHRepository ghRepository = context.gitHubRepository();
        if (ghRepository != null) {
            List<GHPullRequest> pullRequests = ghRepository.getPullRequests(GHIssueState.OPEN);
            for (GHPullRequest pullRequest : pullRequests) {
                if (GitHubHelpers.hasLabel(pullRequest.getLabels(), context.getConfiguration().getGithubPullRequestLabel())) {
                    if (!GitHubHelpers.isMergeable(pullRequest)) {
                        // lets re-run the update commands we can find on the PR
                        CompositeCommand commands = loadCommandsFromPullRequest(context, ghRepository, pullRequest);
                        if (commands != null) {
                            commands.run(context, ghRepository, pullRequest);
                        }
                    }
                }
            }
        }
    }

    /**
     * Lets load the old command context from comments on the PullRequest so that we can re-run a command to rebase things.
     */
    protected CompositeCommand loadCommandsFromPullRequest(CommandContext context, GHRepository ghRepository, GHPullRequest pullRequest) throws IOException {
        List<GHIssueComment> comments = pullRequest.getComments();
        String lastCommand = null;
        for (GHIssueComment comment : comments) {
            String command = updateBotCommentCommand(context, comment);
            if (command != null) {
                lastCommand = command;
            }
        }
        if (lastCommand == null) {
            LOG.warn("No UpdateBot comment found on pull request " + pullRequest.getHtmlUrl() + " so cannot rebase!");
            return null;
        }
        return parseUpdateBotCommandComment(context, lastCommand);
    }

    public CompositeCommand parseUpdateBotCommandComment(CommandContext context, String fullCommand) {
        CompositeCommand commands = new CompositeCommand();
        CommandContext answer = new CommandContext(context.getRepository(), context.getConfiguration());

        String command = fullCommand.substring(PullRequests.COMMAND_COMMENT_PREFIX.length()).trim();
        String[] lines = command.split("\n");
        for (String line : lines) {
            String text = line.trim();
            if (Strings.notEmpty(text)) {
                addBotCommand(commands, answer, text);
            }
        }
        return commands;
    }

    private void addBotCommand(CompositeCommand commands, CommandContext parentContext, String commandLine) {
        String subCommand = commandLine;
        if (subCommand.startsWith(UPDATEBOT)) {
            subCommand = subCommand.substring(UPDATEBOT.length()).trim();
        }
        String[] args = subCommand.split(" ");
        Configuration dummyConfig = new Configuration();
        CommandSupport command = UpdateBot.parseCommand(args, dummyConfig, false);
        if (command == null) {
            LOG.warn("Could not parse command line: " + commandLine);
        } else {
            commands.addCommand(command);
        }
    }

    private String updateBotCommentCommand(CommandContext context, GHIssueComment comment) throws IOException {
        GHUser user = comment.getUser();
        if (user != null) {
            if (Objects.equal(context.getConfiguration().getGithubUsername(), user.getLogin())) {
                String body = comment.getBody();
                if (body != null) {
                    body = body.trim();
                    if (body.startsWith(PullRequests.COMMAND_COMMENT_PREFIX)) {
                        return body;
                    }
                }
            }
        }
        return null;
    }
}
