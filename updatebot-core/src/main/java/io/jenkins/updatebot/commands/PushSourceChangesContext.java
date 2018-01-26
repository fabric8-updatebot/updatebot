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
import io.jenkins.updatebot.support.Markdown;

/**
 */
public class PushSourceChangesContext extends CommandContext {
    private final PushSourceChanges command;
    private final LocalRepository sourceRepository;

    public PushSourceChangesContext(LocalRepository repository, Configuration configuration, PushSourceChanges command, LocalRepository sourceRepository) {
        super(repository, configuration);
        this.command = command;
        this.sourceRepository = sourceRepository;
    }

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
        String gitUrl = command.getRepositoryFullName();
        String ref = command.getRef();
        String linkText = LocalRepository.getRepositoryLink(this.sourceRepository, gitUrl);

        return Markdown.UPDATEBOT_ICON + " pushed version changes from the source code in repository: " +
                linkText +
                " ref: `" + ref + "`\n";
    }

    @Override
    public String createCommit() {
        String gitUrl = command.getRepositoryFullName();
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
        return "push " + command.getRepositoryFullName() + " ";
    }

}
