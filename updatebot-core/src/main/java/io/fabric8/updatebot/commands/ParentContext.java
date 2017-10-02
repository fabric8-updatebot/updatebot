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

import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHPullRequest;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class ParentContext {
    private List<CommandContext> children = new ArrayList<>();

    public void addChild(CommandContext context) {
        children.add(context);
    }

    public List<CommandContext> getChildren() {
        return children;
    }


    public List<GHPullRequest> getPullRequests() {
        List<GHPullRequest> answer = new ArrayList<>();
        for (CommandContext child : children) {
            GHPullRequest pullRequest = child.getPullRequest();
            if (pullRequest != null) {
                answer.add(pullRequest);
            }
        }
        return answer;
    }

    public List<GHIssue> getIssues() {
        List<GHIssue> answer = new ArrayList<>();
        for (CommandContext child : children) {
            GHIssue issue = child.getIssue();
            if (issue != null) {
                answer.add(issue);
            }
        }
        return answer;
    }
}
