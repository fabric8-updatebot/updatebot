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

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import io.fabric8.updatebot.CommandNames;
import io.fabric8.updatebot.Configuration;
import io.fabric8.updatebot.kind.CompositeUpdater;
import io.fabric8.updatebot.model.Dependencies;
import io.fabric8.updatebot.model.DependencyVersionChange;
import io.fabric8.updatebot.model.GitRepository;
import io.fabric8.updatebot.model.GitRepositoryConfig;
import io.fabric8.updatebot.repository.LocalRepository;
import io.fabric8.updatebot.support.Strings;
import io.fabric8.utils.Files;
import io.fabric8.utils.GitHelpers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Push changes from local source code into downstream projects
 */
@Parameters(commandNames = CommandNames.PUSH_SOURCE, commandDescription = "Pushes version and dependency changes from your local source code into downstream projects. " +
        "You usually invoke this command after a release has been performed using the tagged versioned code as the input")
public class PushSourceChanges extends ModifyFilesCommandSupport {
    private static final transient Logger LOG = LoggerFactory.getLogger(PushSourceChanges.class);


    @Parameter(names = {"--ref", "-r"}, description = "The git repository ref (sha, branch or tag) to clone the source repository from for the source code")
    private String ref = "master";

    @Parameter(description = "The git repository to clone from for the source code")
    private String cloneUrl;

    private LocalRepository sourceRepository;

    public PushSourceChanges() {
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public String getCloneUrl() {
        return cloneUrl;
    }

    public void setCloneUrl(String cloneUrl) {
        this.cloneUrl = cloneUrl;
    }

    protected String getOperationDescription(CommandContext context) {
        LocalRepository sourceRepository = getSourceRepository();
        if (sourceRepository != null) {
            return "pushing versions from " + LocalRepository.getRepositoryLink(sourceRepository);
        }
        return super.getOperationDescription(context);
    }

    @Override
    protected boolean doProcess(CommandContext context) throws IOException {
        LocalRepository repository = context.getRepository();
        if (repository.hasCloneUrl(cloneUrl)) {
            LOG.debug("Ignoring repository " + repository.getCloneUrl() + " as this is the source repository!");
            return false;
        }
        List<DependencyVersionChange> steps = loadPushVersionSteps(context);
        String sourceFullName = getCloneUrl();
        LocalRepository sourceRepository = getSourceRepository();
        if (sourceRepository != null) {
            sourceFullName = sourceRepository.getFullName();
        }
        String message = "push source changes from " + sourceFullName + " to " + context.getRepositoryFullName();
        String stepDescription = " changes found: " + DependencyVersionChange.describe(steps);
        if (steps.isEmpty()) {
            stepDescription = " no changes found";
            LOG.debug(message + stepDescription);
        } else {
            LOG.info(message + stepDescription);
        }
        return pushVersionsWithChecks(context, steps);
    }

    @Override
    protected void validateConfiguration(Configuration configuration) throws IOException {
        File dir = configuration.getSourceDir();
        if (this.cloneUrl == null) {
            if (dir != null) {
                if (!Files.isDirectory(dir)) {
                    throw new ParameterException("Directory does not exist " + dir);
                }
                String url;
                try {
                    url = GitHelpers.extractGitUrl(dir);
                } catch (IOException e) {
                    throw new ParameterException("Could not find the git clone URL in " + dir + ". " + e, e);
                }
                if (url != null) {
                    setCloneUrl(url);
                }
            }
        }
        validateCloneUrl();

        if (sourceRepository == null) {
            sourceRepository = findLocalRepository(configuration);
        }
        if (sourceRepository == null) {
            File sourceDir = configuration.getSourceDir();
            if (sourceDir != null) {
                GitRepository repo = new GitRepository(dir.getName());
                // TODO create a GitHubRepository if we can figure that out
                repo.setCloneUrl(getCloneUrl());
                this.sourceRepository = new LocalRepository(repo, dir);
            }
        }

        if (dir != null) {
            configuration.setSourceDir(dir);
        }
    }

    @Override
    protected CommandContext createCommandContext(LocalRepository repository, Configuration configuration) {
        return new PushSourceChangesContext(repository, configuration, this, getSourceRepository());

    }

    public LocalRepository getSourceRepository() {
        return sourceRepository;
    }

    protected List<DependencyVersionChange> loadPushVersionSteps(CommandContext context) throws IOException {
        Configuration configuration = context.getConfiguration();
        List<DependencyVersionChange> list = new ArrayList<>();
        if (sourceRepository == null) {
            LOG.warn("No source repository for " + context.getDir());
            return list;
        }
        GitRepository repo = sourceRepository.getRepo();
        if (repo == null) {
            LOG.warn("No git repo for " + sourceRepository + " " + context.getDir());
            return list;
        }
        GitRepositoryConfig repositoryDetails = repo.getRepositoryDetails();
        Dependencies push = null;
        if (repositoryDetails == null) {
            LOG.debug("No push repository details found for repository " + sourceRepository);
        } else {
            push = repositoryDetails.getPush();
        }
        if (push == null) {
            LOG.debug("No push version details found for repository " + sourceRepository + " for configuration " + repositoryDetails);
            push = new Dependencies();
        }
        CommandContext sourceContext = new CommandContext(sourceRepository, configuration);
        CompositeUpdater updater = new CompositeUpdater();
        updater.addPushVersionsSteps(sourceContext, push, list);
        return list;
    }

    protected void validateCloneUrl() {
        String cloneUrl = getCloneUrl();
        if (Strings.empty(cloneUrl)) {
            throw new IllegalArgumentException("No cloneUrl argument specified!");
        }
    }

    /**
     * Lets find the repository for the given directory so that we can extract any extra configuration like the
     * {@link GitRepositoryConfig} for a local repository
     *
     * @param configuration
     */
    protected LocalRepository findLocalRepository(Configuration configuration) throws IOException {
        String cloneUrl = getCloneUrl();
        List<LocalRepository> localRepositories = getLocalRepositories(configuration);
        for (LocalRepository localRepository : localRepositories) {
            if (localRepository.hasCloneUrl(cloneUrl)) {
                return localRepository;
            }
        }
        return null;
    }
}
