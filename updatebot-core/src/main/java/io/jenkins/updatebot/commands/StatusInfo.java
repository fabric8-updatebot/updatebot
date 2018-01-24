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

import io.jenkins.updatebot.Configuration;
import io.jenkins.updatebot.repository.LocalRepository;
import io.jenkins.updatebot.support.Strings;
import io.fabric8.utils.Objects;
import org.fusesource.jansi.Ansi;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents the status of a repository
 */
public class StatusInfo {
    private static final transient Logger LOG = LoggerFactory.getLogger(StatusInfo.class);

    private final LocalRepository repository;
    private final Status status;
    private final GHIssue issue;
    private final GHPullRequest pullRequest;
    private final String cloneUrl;
    private final GHIssueState issueState;
    private final GHIssueState pullRequestState;
    private String issueUrl;
    private String pullRequestUrl;

    public StatusInfo(LocalRepository repository, Status status, GHIssue issue, GHPullRequest pullRequest) {
        this.repository = repository;
        this.issue = issue;
        this.pullRequest = pullRequest;
        this.issueUrl = (issue != null) ? Strings.toString(issue.getHtmlUrl()) : null;
        this.pullRequestUrl = (pullRequest != null) ? Strings.toString(pullRequest.getHtmlUrl()) : null;
        this.cloneUrl = repository.getCloneUrl();
        this.issueState = state(issue);
        this.pullRequestState = state(pullRequest);
        if (nullOrClosed(issueState) && nullOrClosed(pullRequestState) && status.equals(Status.PENDING)) {
            status = Status.COMPLETE;
        }
        this.status = status;
    }

    /**
     * Returns true if there are any pending statuses in the map
     */
    public static boolean isPending(Map<String, StatusInfo> statusMap) {
        return isPending(statusMap.values());
    }

    /**
     * Returns true if there are any pending statuses in the map
     */
    public static boolean isPending(Iterable<StatusInfo> statuses) {
        for (StatusInfo status : statuses) {
            if (status.isPending()) {
                return true;
            }
        }
        return false;
    }

    protected static boolean nullOrClosed(GHIssueState state) {
        return state == null || state.equals(GHIssueState.CLOSED);
    }

    protected static GHIssueState state(GHIssue issue) {
        if (issue != null) {
            return issue.getState();
        }
        return null;
    }

    /**
     * Lets return the current status by combining the old and new status maps to handle new maps
     * not including links to old pending issues or pull requests that are now closed
     */
    public static Map<String, StatusInfo> changedStatuses(Configuration configuration, Map<String, StatusInfo> oldMap, Map<String, StatusInfo> newMap) {
        Set<String> allKeys = new LinkedHashSet(oldMap.keySet());
        allKeys.addAll(newMap.keySet());
        Map<String, StatusInfo> answer = new LinkedHashMap<>();
        for (String key : allKeys) {
            StatusInfo status = changedStatus(configuration, oldMap.get(key), newMap.get(key));
            if (status != null) {
                answer.put(key, status);
            }
        }
        return answer;
    }

    private static StatusInfo changedStatus(Configuration configuration, StatusInfo oldStatus, StatusInfo newStatus) {
        if (oldStatus == null) {
            return newStatus;
        } else {
            if (newStatus == null) {
                return null;
            }
            if (oldStatus.equalStatus(newStatus)) {
                return null;
            }
            LOG.info("Status changed for " + oldStatus + " " + newStatus);
            // lets use the status which has the issue/pull request which is usually the old one
            // as when things close they don't appear in searches for open issues/PRs
            if (newStatus.getIssueUrl() != null || newStatus.getPullRequestUrl() != null) {
                LOG.info("new status has PR " + newStatus);
                return newStatus;
            } else {
                return StatusInfo.createStatus(configuration, oldStatus);
            }
        }
    }

    private static StatusInfo createStatus(Configuration configuration, StatusInfo oldStatus) {
        GHRepository repository = null;
        GHIssue issue = oldStatus.getIssue();
        if (issue != null) {
            repository = issue.getRepository();
            try {
                issue = repository.getIssue(issue.getNumber());
            } catch (IOException e) {
                configuration.warn(LOG, "Failed to lookup issue " + oldStatus.getIssueUrl() + ". " + e, e);
            }
        }
        GHPullRequest pullRequest = oldStatus.getPullRequest();
        if (pullRequest != null) {
            if (repository == null) {
                repository = pullRequest.getRepository();
            }
            try {
                pullRequest = repository.getPullRequest(pullRequest.getNumber());
            } catch (IOException e) {
                configuration.warn(LOG, "Failed to lookup pull request " + oldStatus.getPullRequestUrl() + ". " + e, e);
            }
        }
        return new StatusInfo(oldStatus.getRepository(), oldStatus.getStatus(), issue, pullRequest);
    }

    @Override
    public String toString() {
        return "StatusInfo{" +
                "name='" + getFullName() + '\'' +
                ", status=" + status +
                ", issueState=" + issueState +
                ", pullRequestState=" + pullRequestState +
                ", pullRequestUrl='" + pullRequestUrl + '\'' +
                '}';
    }

    /**
     * Returns true if this and that object have the same underlying status
     */
    public boolean equalStatus(StatusInfo that) {
        return Objects.equal(this.status, that.status) &&
                Objects.equal(this.issueState, that.issueState) &&
                Objects.equal(this.pullRequestState, that.pullRequestState);
    }


    /**
     * Returns true if this status is pending
     */
    public boolean isPending() {
        return status.equals(Status.PENDING);
    }

    public LocalRepository getRepository() {
        return repository;
    }

    public Status getStatus() {
        return status;
    }

    public GHIssue getIssue() {
        return issue;
    }

    public GHPullRequest getPullRequest() {
        return pullRequest;
    }

    public String getFullName() {
        return getRepository().getFullName();
    }

    public String getCloneUrl() {
        return cloneUrl;
    }

    public String getIssueUrl() {
        return issueUrl;
    }

    public String getPullRequestUrl() {
        return pullRequestUrl;
    }

    public GHIssueState getIssueState() {
        return issueState;
    }

    public GHIssueState getPullRequestState() {
        return pullRequestState;
    }

    /**
     * Returns a description of the status
     *
     * @param configuration
     */
    public String description(Configuration configuration) {
        StringBuilder builder = new StringBuilder();
        Ansi.Color color = Configuration.COLOR_PENDING;
        if (status != null) {
            switch (status) {
                case COMPLETE:
                    color = Configuration.COLOR_COMPLETE;
                    break;
                case FAILED:
                    color = Configuration.COLOR_WARNING;
                    break;
            }
            builder.append(status.getName());
            builder.append(":");
        }
        if (issueUrl != null) {
            builder.append(" issue ");
            if (issueState != null) {
                builder.append(format(issueState));
                builder.append(" ");
            }
            builder.append(issueUrl);
        }
        if (pullRequestUrl != null) {
            builder.append(" pull request ");
            if (pullRequestState != null) {
                builder.append(format(pullRequestState));
                builder.append(" ");
            }
            builder.append(pullRequestUrl);
        }
        // lets add a trailing space just in case ansi color codes break URLs in logs ;)
        builder.append(" ");
        return configuration.colored(color, builder.toString());
    }

    protected String format(GHIssueState state) {
        if (state != null) {
            return state.toString().toLowerCase();
        }
        return null;
    }
}
