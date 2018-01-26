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

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import io.jenkins.updatebot.CommandNames;
import io.jenkins.updatebot.Configuration;
import io.jenkins.updatebot.git.GitHelper;
import io.jenkins.updatebot.git.GitRepositoryInfo;
import io.jenkins.updatebot.github.GitHubHelpers;
import io.jenkins.updatebot.kind.Kind;
import io.jenkins.updatebot.kind.helm.HelmUpdater;
import io.jenkins.updatebot.model.DependencyVersionChange;
import io.jenkins.updatebot.model.Environment;
import io.jenkins.updatebot.model.GitHubProjects;
import io.jenkins.updatebot.model.RepositoryConfig;
import io.jenkins.updatebot.repository.LocalRepository;
import io.jenkins.updatebot.repository.Repositories;
import io.jenkins.updatebot.support.Strings;
import org.kohsuke.github.GHPerson;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Promotes a new release across the environments configured for this app
 */
@Parameters(commandNames = CommandNames.PROMOTE, commandDescription = "Promotes a release through its environments.")
public class Promote extends ModifyFilesCommandSupport {
    private static final transient Logger LOG = LoggerFactory.getLogger(Promote.class);

    @Parameter(names = "--environment", description = "Only promote to the specific named environment")
    private String environment;

    @Parameter(names = "--chart", description = "The name of the chart to promote", required = true)
    private String chart;

    @Parameter(names = "--version", description = "The new version to promote", required = true)
    private String version;

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getChart() {
        return chart;
    }

    public void setChart(String chart) {
        this.chart = chart;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
// Implementation
    //-------------------------------------------------------------------------

    @Override
    protected CommandContext createCommandContext(LocalRepository repository, Configuration configuration) {
        return new EnableFabric8Context(repository, configuration, null);
    }

    @Override
    public ParentContext run(Configuration configuration) throws IOException {
        validateConfiguration(configuration);

        ParentContext parentContext = new ParentContext();


        RepositoryConfig repositoryConfig = configuration.loadRepositoryConfig();
        List<Environment> environments = repositoryConfig.getEnvironments();
        for (Environment env : environments) {
            if (Strings.notEmpty(environment)) {
                if (!environment.equals(env.getId()) && !environment.equals(env.getName())) {
                    continue;
                }
            }
            promoteEnvironment(parentContext, configuration, env);
        }
        return parentContext;
    }

    protected void promoteEnvironment(ParentContext parentContext, Configuration configuration, Environment env) throws IOException {
        String repo = env.getGithub();
        if (Strings.empty(repo)) {
            configuration.warn(LOG, "Only github repositories are supported so far so cannot promote to " + env.getName());
            return;
        }
        GitRepositoryInfo info = GitHelper.parseRepository(repo);
        String organisation = info.getOrganisation();
        String name = info.getName();
        GitHub github = configuration.getGithub();
        GHPerson user;
        try {
            user = GitHubHelpers.getOrganisationOrUser(github, organisation);
        } catch (Exception e) {
            throw new IOException("Failed to find organisation or user: " + organisation + ". " + e, e);
        }
        GHRepository repository;
        try {
            repository = user.getRepository(name);
        } catch (IOException e) {
            throw new IOException("Failed to find repository: " + name + " for user " + organisation + ". " + e, e);
        }

        RepositoryConfig singleProjectConfig = new RepositoryConfig();
        GitHubProjects gitHubProjects = singleProjectConfig.github();
        gitHubProjects.organisation(organisation).repository(name);

        setRepositoryConfig(singleProjectConfig);

        List<LocalRepository> localRepositories = Repositories.cloneOrPullRepositories(configuration, singleProjectConfig);
        setLocalRepositories(localRepositories);
        LocalRepository localRepository = LocalRepository.findRepository(localRepositories, name);
        if (localRepository == null) {
            throw new IOException("Could not find repository called " + name + " in " + localRepositories);
        }

        // lets record the local repos for the pull request polling
        setLocalRepositories(Arrays.asList(localRepository));

        PromoteContext context = new PromoteContext(localRepository, configuration, chart, version);
        parentContext.addChild(context);

        run(context);

    }


    @Override
    protected boolean doProcess(CommandContext context) throws IOException {
        return promote((PromoteContext) context);
    }

    protected boolean promote(PromoteContext context) throws IOException {
        boolean updated = false;
        HelmUpdater updater = new HelmUpdater();
        if (updater.isApplicable(context)) {
            List<DependencyVersionChange> changes = new ArrayList<>();
            changes.add(new DependencyVersionChange(Kind.HELM, chart, version));
            if (updater.pushVersions(context, changes)) {
                updated = true;
            }
        }
        return updated;
    }
}
