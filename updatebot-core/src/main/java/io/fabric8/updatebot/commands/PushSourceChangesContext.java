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

import io.fabric8.updatebot.repository.LocalRepository;
import io.fabric8.updatebot.support.Markdown;
import io.fabric8.updatebot.support.Strings;

/**
 */
public class PushSourceChangesContext extends CommandContext {
    private final PushSourceChanges command;
    private final LocalRepository sourceRepository;

    public PushSourceChangesContext(CommandContext parentContext, PushSourceChanges command, LocalRepository sourceRepository) {
        super(parentContext);
        this.command = command;
        this.sourceRepository = sourceRepository;
    }

    public PushSourceChanges getCommand() {
        return command;
    }

    @Override
    public String createPullRequestBody() {
        String gitUrl = command.getCloneUrl();
        String ref = command.getRef();
        String gitUrlText = "`" + gitUrl + "`";

        if (sourceRepository != null) {
            String htmlUrl = sourceRepository.getRepo().getHtmlUrl();
            if (Strings.notEmpty(htmlUrl)) {
                gitUrlText = "[" + gitUrl + "](" + htmlUrl + ")";
            }
        }
        return Markdown.UPDATEBOT_ICON + " pushed version changes from the source code in repository: " +
                gitUrlText +
                " ref: `" + ref + "`\n";
    }

    @Override
    public String createCommit() {
        String gitUrl = command.getCloneUrl();
        String ref = command.getRef();
        return "fix(versions): " + gitUrl + "\n\n" +
                "Push version changes from the source code in repository: " + gitUrl + " ref: " + ref + "\n";
    }

    @Override
    public String createPullRequestTitle() {
        return createPullRequestTitlePrefix() + command.getRef();
    }

    @Override
    public String createPullRequestTitlePrefix() {
        return "push " + command.getCloneUrl() + " ";
    }

}
