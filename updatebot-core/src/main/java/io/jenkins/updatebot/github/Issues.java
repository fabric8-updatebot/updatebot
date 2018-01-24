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
package io.jenkins.updatebot.github;

import io.jenkins.updatebot.Configuration;
import io.jenkins.updatebot.commands.CommandContext;
import io.jenkins.updatebot.kind.DependenciesCheck;
import io.jenkins.updatebot.kind.Kind;
import io.jenkins.updatebot.kind.KindDependenciesCheck;
import io.jenkins.updatebot.kind.npm.dependency.DependencyCheck;
import io.jenkins.updatebot.model.DependencyVersionChange;
import io.jenkins.updatebot.support.Markdown;
import io.jenkins.updatebot.support.Strings;
import io.fabric8.utils.Objects;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static io.jenkins.updatebot.github.GitHubHelpers.retryGithub;

/**
 */
public class Issues {
    public static final String BODY = Markdown.UPDATEBOT_ICON + " cannot update some dependency versions until other projects are released to fix dependency conflicts.\n\n" +
            "This issue is used to coordinate version changes on this repository coming from other repositories and will be closed once all the version conflicts are resolved.";
    public static final String CLOSE_MESSAGE = Markdown.UPDATEBOT_ICON + " closing as no more dependency conflicts while ";
    public static final String PENDING_CHANGE_COMMENT_PREFIX = Markdown.UPDATEBOT_ICON + " detected conflicts while ";
    public static final String HEADER_KIND = "## ";
    public static final String CONFLICTS_HEADER = "### Conflicts";
    public static final String CONFLICT_PREFIX = "* ";
    public static final String PENDING_COMMAND_PREFIX = "    ";
    private static final transient Logger LOG = LoggerFactory.getLogger(Issues.class);

    public static List<GHIssue> getOpenIssues(GHRepository ghRepository, Configuration configuration) throws IOException {
        String label = configuration.getGithubPullRequestLabel();
        return getOpenIssues(ghRepository, label);
    }

    public static List<GHIssue> getOpenIssues(GHRepository ghRepository, String label) throws IOException {
        List<GHIssue> issues = retryGithub(() -> ghRepository.getIssues(GHIssueState.OPEN));
        List<GHIssue> answer = new ArrayList<>();
        for (GHIssue issue : issues) {
            if (GitHubHelpers.hasLabel(getLabels(issue), label) && !issue.isPullRequest()) {
                answer.add(issue);
            }
        }
        return answer;
    }


    public static List<DependencyVersionChange> loadPendingChangesFromIssue(CommandContext context, GHIssue issue) throws IOException {
        List<GHIssueComment> comments = issue.getComments();
        String lastCommand = null;
        for (GHIssueComment comment : comments) {
            String command = updateBotIssuePendingChangesComment(context, comment);
            if (command != null) {
                lastCommand = command;
            }
        }
        if (lastCommand == null) {
            LOG.warn("No UpdateBot comment found on issue " + issue.getHtmlUrl());
            return new ArrayList<>();
        }
        return parseUpdateBotIssuePendingChangesComment(lastCommand);
    }

    public static List<DependencyVersionChange> parseUpdateBotIssuePendingChangesComment(String command) {
        String[] lines = command.split("\n");
        List<DependencyVersionChange> answer = new ArrayList<>();
        Kind kind = null;
        for (String line : lines) {
            boolean commandLine = line.startsWith(PENDING_COMMAND_PREFIX);
            String text = line.trim();
            if (Strings.notEmpty(text)) {
                if (text.startsWith("#")) {
                    String header = Strings.trimAllPrefix(text, "#").trim();
                    Kind k = Kind.fromName(header);
                    if (k != null) {
                        kind = k;
                    }
                } else if (commandLine) {
                    if (kind != null) {
                        addChangeFromCommentLine(answer, kind, text);
                    }
                }
            }
        }
        return answer;
    }

    protected static void addChangeFromCommentLine(List<DependencyVersionChange> answer, Kind kind, String text) {
        String[] words = text.split("\\s+");
        if (words.length < 2) {
            LOG.warn("Ignoring command: Not enough arguments: " + text);
            return;
        }
        DependencyVersionChange change;
        String dependency = words[0];
        String version = words[1];
        String scope = null;
        if (words.length > 2) {
            scope = words[2];
        }
        if (scope == null) {
            change = new DependencyVersionChange(kind, dependency, version);
        } else {
            change = new DependencyVersionChange(kind, dependency, version, scope);
        }
        answer.add(change);
    }

    protected static String createPendingVersionChangeCommands(List<DependencyVersionChange> changes) {
        StringBuilder builder = new StringBuilder();
        for (DependencyVersionChange change : changes) {
            builder.append("\n    ");
            String scope = change.getScope();
            if (scope == null) {
                scope = "";
            }
            builder.append(String.join(" ", change.getDependency(), change.getVersion(), scope));
        }
        return builder.toString();
    }


    public static void addConflictsComment(GHIssue issue, List<DependencyVersionChange> pendingChanges, String operationDescription, DependenciesCheck check) throws IOException {
        String prefix = PENDING_CHANGE_COMMENT_PREFIX + operationDescription + "\n";
        String issueComment = prefix + conflictChangesComment(pendingChanges, check);
        issue.comment(issueComment);
    }

    public static String conflictChangesComment(List<DependencyVersionChange> pendingChanges, DependenciesCheck check) {
        StringBuilder builder = new StringBuilder();
        Map<Kind, KindDependenciesCheck> failures = check.getFailures();
        for (Map.Entry<Kind, KindDependenciesCheck> entry : failures.entrySet()) {
            Kind kind = entry.getKey();
            KindDependenciesCheck kindCheck = entry.getValue();
            List<DependencyVersionChange> kindChanges = DependencyVersionChange.forKind(kind, pendingChanges);
            List<DependencyCheck> kindConflicts = kindCheck.getFailedChecksFor(kindChanges);

            boolean hasConflicts = kindConflicts.size() > 0;
            boolean hasChanges = kindChanges.size() > 0;
            if (hasConflicts && hasChanges) {
                builder.append("\n\n");
                builder.append(HEADER_KIND);
                builder.append(kind.getName());
                if (hasChanges) {
                    builder.append("\n");
                    builder.append(createPendingVersionChangeCommands(kindChanges));
                }
                if (hasConflicts) {
                    builder.append("\n\n");
                    builder.append(CONFLICTS_HEADER);
                    builder.append("\n\n");
                    builder.append(createConflictComments(kindConflicts));
                }
            }
        }
        return builder.toString();
    }

    private static String createConflictComments(List<DependencyCheck> conflicts) {
        StringBuilder builder = new StringBuilder();
        for (DependencyCheck conflict : conflicts) {
            if (conflict.isValid()) {
                continue;
            }
            String message = conflict.getMessage();
            String dependency = conflict.getDependency();
            builder.append(CONFLICT_PREFIX + "`" + dependency + "` " + message + "\n");
        }
        return builder.toString();
    }

    protected static String createConfictIssueComment(DependenciesCheck check) {
        return null;
    }


    public static String updateBotIssuePendingChangesComment(CommandContext context, GHIssueComment comment) throws IOException {
        GHUser user = comment.getUser();
        if (user != null) {
            if (Objects.equal(context.getConfiguration().getGithubUsername(), user.getLogin())) {
                String body = comment.getBody();
                if (body != null) {
                    body = body.trim();
                    if (body.startsWith(PENDING_CHANGE_COMMENT_PREFIX)) {
                        return body;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Lets try find the issue
     */
    public static GHIssue findIssue(CommandContext context, List<GHIssue> issues) {
        String prefix = context.createIssueTitlePrefix();
        if (issues != null) {
            for (GHIssue issue : issues) {
                String title = issue.getTitle();
                if (title != null && title.startsWith(prefix)) {
                    return issue;
                }
            }
        }
        return null;
    }

    public static GHIssue createIssue(CommandContext context, GHRepository repository) throws IOException {
        return repository.createIssue(context.createIssueTitlePrefix()).
                body(BODY).
                label(context.getConfiguration().getGithubPullRequestLabel()).
                create();
    }

    public static void logOpen(List<GHIssue> issues) {
        for (GHIssue issue : issues) {
            LOG.info("Open issue " + issue.getHtmlUrl());
        }
    }

    /**
     * Lets return the labels on an issue with retries
     */
    public static Collection<GHLabel> getLabels(GHIssue issue) throws IOException {
        return retryGithub(() -> issue.getLabels());
    }

    public static boolean isOpen(GHIssue issue) {
        GHIssueState state = issue.getState();
        if (state == null) {
            return true;
        }
        return state == GHIssueState.OPEN;
    }
}