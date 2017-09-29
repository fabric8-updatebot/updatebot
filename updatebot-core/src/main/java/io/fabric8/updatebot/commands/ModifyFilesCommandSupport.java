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

import io.fabric8.updatebot.Configuration;
import io.fabric8.updatebot.github.GitHubHelpers;
import io.fabric8.updatebot.github.Issues;
import io.fabric8.updatebot.github.PullRequests;
import io.fabric8.updatebot.kind.DependenciesCheck;
import io.fabric8.updatebot.kind.Kind;
import io.fabric8.updatebot.kind.KindDependenciesCheck;
import io.fabric8.updatebot.kind.Updater;
import io.fabric8.updatebot.model.DependencyVersionChange;
import io.fabric8.updatebot.support.Commands;
import io.fabric8.updatebot.support.GitHelper;
import io.fabric8.utils.Objects;
import org.kohsuke.github.GHCommitPointer;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Base class for all UpdateBot commands
 */
public abstract class ModifyFilesCommandSupport extends CommandSupport {
    private static final transient Logger LOG = LoggerFactory.getLogger(ModifyFilesCommandSupport.class);
    private GHIssue issue;

    @Override
    public void run(CommandContext context) throws IOException {
        prepareDirectory(context);
        if (doProcess(context) && !context.getConfiguration().isDryRun()) {
            gitCommitAndPullRequest(context);
        }
    }

    public void run(CommandContext context, GHRepository ghRepository, GHPullRequest pullRequest) throws IOException {
        prepareDirectory(context);
        if (doProcess(context)) {
            processPullRequest(context, ghRepository, pullRequest);
        }
    }

    // Implementation methods
    //-------------------------------------------------------------------------
    protected void prepareDirectory(CommandContext context) {
        File dir = context.getRepository().getDir();
        dir.getParentFile().mkdirs();
        GitHelper.gitStashAndCheckoutMaster(dir);
    }

    protected boolean doProcess(CommandContext context) throws IOException {
        return false;
    }

    protected void gitCommitAndPullRequest(CommandContext context) throws IOException {
        GHRepository ghRepository = context.gitHubRepository();
        if (ghRepository != null) {
            List<GHPullRequest> pullRequests = PullRequests.getOpenPullRequests(ghRepository, context.getConfiguration());
            GHPullRequest pullRequest = findPullRequest(context, pullRequests);
            processPullRequest(context, ghRepository, pullRequest);
        } else {
            // TODO what to do with vanilla git repos?
        }

    }

    protected void processPullRequest(CommandContext context, GHRepository ghRepository, GHPullRequest pullRequest) throws IOException {
        Configuration configuration = context.getConfiguration();
        String title = context.createPullRequestTitle();
        String remoteURL = "git@github.com:" + ghRepository.getOwnerName() + "/" + ghRepository.getName();
        File dir = context.getDir();
        if (Commands.runCommandIgnoreOutput(dir, "git", "remote", "set-url", "origin", remoteURL) != 0) {
            LOG.warn("Could not set the remote URL of " + remoteURL);
        }

        String commandComment = createPullRequestComment();

        if (pullRequest == null) {
            String localBranch = "updatebot-" + UUID.randomUUID().toString();
            doCommit(context, dir, localBranch);

            String body = context.createPullRequestBody();
            //String head = getGithubUsername() + ":" + localBranch;
            String head = localBranch;

            if (Commands.runCommand(dir, "git", "push", "-f", "origin", localBranch) != 0) {
                LOG.warn("Failed to push branch " + localBranch + " for " + context.getCloneUrl());
                return;
            }
            pullRequest = ghRepository.createPullRequest(title, head, "master", body);
            context.setPullRequest(pullRequest);
            LOG.info("Created pull request " + pullRequest.getHtmlUrl());

            pullRequest.comment(commandComment);
            addIssueClosedCommentIfRequired(context, pullRequest, true);
            pullRequest.setLabels(configuration.getGithubPullRequestLabel());
        } else {
            context.setPullRequest(pullRequest);

            addIssueClosedCommentIfRequired(context, pullRequest, false);
            String oldTitle = pullRequest.getTitle();
            if (Objects.equal(oldTitle, title)) {
                // lets check if we need to rebase
                if (configuration.isRebaseMode()) {
                    if (GitHubHelpers.isMergeable(pullRequest)) {
                        return;
                    }
                    pullRequest.comment("[UpdateBot](https://github.com/fabric8io/updatebot) rebasing due to merge conflicts");
                }
            } else {
                //pullRequest.comment("Replacing previous commit");
                pullRequest.setTitle(title);

                pullRequest.comment(commandComment);
            }

            GHCommitPointer head = pullRequest.getHead();
            String remoteRef = head.getRef();

            String localBranch = remoteRef;

            // lets remove any local branches of this name
            Commands.runCommandIgnoreOutput(dir, "git", "branch", "-D", localBranch);

            doCommit(context, dir, localBranch);

            if (Commands.runCommand(dir, "git", "push", "-f", "origin", localBranch + ":" + remoteRef) != 0) {
                LOG.warn("Failed to push branch " + localBranch + " to existing github branch " + remoteRef + " for " + pullRequest.getHtmlUrl());
            }
            LOG.info("Updated PR " + pullRequest.getHtmlUrl());
        }
    }

    private void addIssueClosedCommentIfRequired(CommandContext context, GHPullRequest pullRequest, boolean create) {
        GHIssue issue = context.getIssue();
        if (issue == null) {
            return;
        }
        if (!create) {
            // avoid duplicate comment
            try {
                List<GHIssueComment> comments = pullRequest.getComments();
                for (GHIssueComment comment : comments) {
                    String body = comment.getBody();
                    if (body != null && body.startsWith(PullRequests.ISSUE_LINK_COMMENT)) {
                        return;
                    }
                }
            } catch (IOException e) {
                // ignore
            }
        }
        try {
            pullRequest.comment(PullRequests.ISSUE_LINK_COMMENT + " " + issue.getHtmlUrl() + PullRequests.ISSUE_LINK_COMMENT_SUFFIX);
        } catch (IOException e) {
            // ignore
        }
    }

    private boolean doCommit(CommandContext context, File dir, String branch) {
        String commitComment = context.createCommit();
        if (Commands.runCommandIgnoreOutput(dir, "git", "checkout", "-b", branch) == 0) {
            return GitHelper.gitAddAndCommit(dir, commitComment);
        }
        return false;
    }

    /**
     * Lets try find a pull request for previous PRs
     */
    protected GHPullRequest findPullRequest(CommandContext context, List<GHPullRequest> pullRequests) {
        String prefix = context.createPullRequestTitlePrefix();
        if (pullRequests != null) {
            for (GHPullRequest pullRequest : pullRequests) {
                String title = pullRequest.getTitle();
                if (title != null && title.startsWith(prefix)) {
                    return pullRequest;
                }
            }
        }
        return null;
    }

    protected boolean pushVersionChangesWithoutChecks(CommandContext parentContext, List<DependencyVersionChange> steps) throws IOException {
        boolean answer = false;
        for (DependencyVersionChange step : steps) {
            if (pushSingleVersionChangeWithoutChecks(parentContext, step)) {
                answer = true;
            }
        }
        return answer;
    }

    protected boolean pushSingleVersionChangeWithoutChecks(CommandContext parentContext, DependencyVersionChange step) throws IOException {
        Kind kind = step.getKind();
        Updater updater = kind.getUpdater();
        PushVersionChangesContext context = new PushVersionChangesContext(parentContext, step);
        if (updater.isApplicable(context)) {
            boolean updated = updater.pushVersions(context);
            if (!updated) {
                parentContext.removeChild(context);
            }
            return updated;
        }
        return false;
    }

    protected boolean pushVersionsWithChecks(CommandContext context, List<DependencyVersionChange> originalSteps) throws IOException {
        List<DependencyVersionChange> pendingChanges = loadPendingChanges(context);
        List<DependencyVersionChange> steps = combinePendingChanges(originalSteps, pendingChanges);
        boolean answer = pushVersionChangesWithoutChecks(context, steps);
        if (answer) {
            if (context.getConfiguration().isCheckDependencies()) {
                DependenciesCheck check = checkDependencyChanges(context, steps);
                List<DependencyVersionChange> invalidChanges = check.getInvalidChanges();
                List<DependencyVersionChange> validChanges = check.getValidChanges();
                if (invalidChanges.size() > 0) {
                    // lets revert the current changes
                    GitHelper.revertChanges(context.getDir());
                    if (validChanges.size() > 0) {
                        // lets perform just the valid changes
                        if (!pushVersionChangesWithoutChecks(context, validChanges)) {
                            LOG.warn("Attempted to apply the subset of valid changes " + DependencyVersionChange.describe(validChanges) + " but no files were modified!");
                            return false;
                        }
                    }
                }
                updatePendingChanges(context, check, pendingChanges);
                return validChanges.size() > 0;
            }
        }
        return answer;
    }

    public void updatePendingChanges(CommandContext context, DependenciesCheck check, List<DependencyVersionChange> pendingChanges) throws IOException {
        List<DependencyVersionChange> currentPendingChanges = check.getInvalidChanges();
        GHRepository ghRepository = context.gitHubRepository();
        if (ghRepository != null) {
            GHIssue issue = getOrFindIssue(context, ghRepository);
            if (currentPendingChanges.equals(pendingChanges)) {
                if (issue != null) {
                    LOG.debug("Pending changes unchanged so not modifying the issue");
                }
                return;
            }
            String operationDescrption = getOperationDescription(context);
            if (currentPendingChanges.isEmpty()) {
                if (issue != null) {
                    LOG.info("Closing issue as we have no further pending issues " + issue.getHtmlUrl());
                    issue.comment(Issues.CLOSE_MESSAGE + operationDescrption);
                    issue.close();
                }
                return;
            }
            if (issue == null) {
                issue = Issues.createIssue(context, ghRepository);
                context.setIssue(issue);
                LOG.info("Created issue " + issue.getHtmlUrl());
            } else {
                LOG.info("Modifying issue " + issue.getHtmlUrl());
            }
            Issues.addConflictsComment(issue, currentPendingChanges, operationDescrption, check);
        } else {
            // TODO what to do with vanilla git repos?
        }
    }

    protected String getOperationDescription(CommandContext context) {
        return "pushing versions";
    }

    private List<DependencyVersionChange> combinePendingChanges(List<DependencyVersionChange> changes, List<DependencyVersionChange> pendingChanges) {
        if (pendingChanges.isEmpty()) {
            return changes;
        }
        List<DependencyVersionChange> answer = new ArrayList<>(changes);
        for (DependencyVersionChange pendingStep : pendingChanges) {
            if (!DependencyVersionChange.hasDependency(changes, pendingStep)) {
                answer.add(pendingStep);
            }
        }
        return answer;
    }

    protected List<DependencyVersionChange> loadPendingChanges(CommandContext context) throws IOException {
        GHRepository ghRepository = context.gitHubRepository();
        if (ghRepository != null) {
            List<GHIssue> issues = Issues.getOpenIssues(ghRepository, context.getConfiguration());
            GHIssue issue = Issues.findIssue(context, issues);
            if (issue != null) {
                context.setIssue(issue);
                return Issues.loadPendingChangesFromIssue(context, issue);
            }
        } else {
            // TODO what to do with vanilla git repos?
        }
        return new ArrayList<>();
    }


    protected DependenciesCheck checkDependencyChanges(CommandContext context, List<DependencyVersionChange> steps) {
        Map<Kind, List<DependencyVersionChange>> map = new LinkedHashMap<>();
        for (DependencyVersionChange change : steps) {
            Kind kind = change.getKind();
            List<DependencyVersionChange> list = map.get(kind);
            if (list == null) {
                list = new ArrayList<>();
                map.put(kind, list);
            }
            list.add(change);
        }


        List<DependencyVersionChange> validChanges = new ArrayList<>();
        List<DependencyVersionChange> invalidChanges = new ArrayList<>();
        Map<Kind, KindDependenciesCheck> allResults = new LinkedHashMap<>();

        for (Map.Entry<Kind, List<DependencyVersionChange>> entry : map.entrySet()) {
            Kind kind = entry.getKey();
            Updater updater = kind.getUpdater();
            KindDependenciesCheck results = updater.checkDependencies(context, entry.getValue());
            validChanges.addAll(results.getValidChanges());
            invalidChanges.addAll(results.getInvalidChanges());
            allResults.put(kind, results);
        }
        return new DependenciesCheck(validChanges, invalidChanges, allResults);
    }
}
