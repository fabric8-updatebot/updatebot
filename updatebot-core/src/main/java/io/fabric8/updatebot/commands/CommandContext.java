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
import io.fabric8.updatebot.kind.Kind;
import io.fabric8.updatebot.model.DependencyVersionChange;
import io.fabric8.updatebot.repository.LocalRepository;
import io.fabric8.updatebot.support.GitHubHelpers;
import io.fabric8.updatebot.support.Markdown;
import org.kohsuke.github.GHRepository;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 */
public class CommandContext {
    private final LocalRepository repository;
    private final Configuration configuration;
    private final Set<File> updatedFiles = new TreeSet<>();
    private final CommandContext parentContext;
    private List<CommandContext> children = new ArrayList<>();

    public CommandContext(LocalRepository repository, Configuration configuration) {
        this.repository = repository;
        this.configuration = configuration;
        this.parentContext = null;
    }

    public CommandContext(CommandContext parentContext) {
        this.repository = parentContext.getRepository();
        this.configuration = parentContext.getConfiguration();
        this.parentContext = parentContext;
        this.parentContext.addChild(this);
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public CommandContext getParentContext() {
        return parentContext;
    }

    public List<CommandContext> getChildren() {
        return children;
    }

    public LocalRepository getRepository() {
        return repository;
    }

    /**
     * Returns true if one or more files have been updated
     */
    public boolean isUpdated() {
        return updatedFiles.size() > 0;
    }

    public Set<File> getUpdatedFiles() {
        return updatedFiles;
    }


    public String getCloneUrl() {
        return repository.getCloneUrl();
    }

    public File getDir() {
        return repository.getDir();
    }

    /**
     * Returns the underlying github repository or null if its not a github repo
     */
    public GHRepository gitHubRepository() {
        return GitHubHelpers.getGitHubRepository(repository);
    }

    /**
     * Returns the relative file path within the local repo
     */
    public File file(String relativePath) {
        return new File(repository.getDir(), relativePath);
    }

    public void updatedFile(File file) {
        updatedFiles.add(file);
    }


    // TODO inline or remove?? should use the Command rather than context?
    public PushVersionChangesContext updateVersion(Kind kind, String name, String version) {
        return new PushVersionChangesContext(this, new DependencyVersionChange(kind, name, version));
    }

    public String createPullRequestTitle() {
        CommandContext child = firstChild();
        if (child != null) {
            return child.createPullRequestTitle();
        }
        return "Pulling new versions";
    }

    public String createIssueTitlePrefix() {
        return "UpdateBot has pending version changes";
    }

    public String createPullRequestTitlePrefix() {
        CommandContext child = firstChild();
        if (child != null) {
            return child.createPullRequestTitlePrefix();
        }
        return createPullRequestTitle();
    }

    public String createCommit() {
        CommandContext child = firstChild();
        if (child != null) {
            return child.createCommit();
        }
        return createPullRequestTitle();
    }


    public String createPullRequestBody() {
        CommandContext child = firstChild();
        if (child != null) {
            return child.createPullRequestBody();
        }
        return Markdown.GENERATED_BY;
    }

    protected void addChild(CommandContext child) {
        children.add(child);
    }

    /**
     * Lets remove a child context if it wasn't applicable (to avoid generating unnecessary change comments etc
     */
    public void removeChild(CommandContext child) {
        children.remove(child);
    }

    protected CommandContext firstChild() {
        if (!children.isEmpty()) {
            return children.get(0);
        }
        return null;
    }
}
