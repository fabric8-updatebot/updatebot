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
package io.fabric8.updatebot.maven;

import io.fabric8.updatebot.model.Dependencies;
import io.fabric8.updatebot.model.GitRepositoryConfig;
import io.fabric8.updatebot.model.MavenArtifactKey;
import io.fabric8.updatebot.model.MavenArtifactVersionChange;
import io.fabric8.updatebot.model.MavenArtifactVersionChanges;
import io.fabric8.updatebot.model.MavenDependencies;
import io.fabric8.updatebot.model.MavenDependencyFilter;
import io.fabric8.updatebot.model.RepositoryConfig;
import io.fabric8.updatebot.model.RepositoryConfigs;
import io.fabric8.updatebot.support.MarkupHelper;
import io.fabric8.utils.Files;
import io.fabric8.utils.Filter;
import io.fabric8.utils.Filters;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Updates versions in this project from a file of version changes
 */
@Mojo(name = "update", aggregator = true, requiresProject = true,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class UpdateVersionsMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true)
    protected MavenSession session;

    @Parameter(property = "updateBotYaml")
    protected String configYaml;

    @Parameter(property = "file", defaultValue = "${basedir}/target/updatebot-versions.yml")
    protected File file;

    private RepositoryConfig repositoryConfig;

    protected static void addArtifact(Map<MavenArtifactKey, MavenArtifactVersionChange> exportVersions, MavenArtifactKey artifactKey, String version, String scope) {
        exportVersions.put(artifactKey, new MavenArtifactVersionChange(artifactKey, version, scope));
    }

    protected static MavenArtifactKey toMavenDependency(Dependency dependency) {
        return new MavenArtifactKey(dependency.getGroupId(), dependency.getArtifactId());
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Log log = getLog();
        File sourceDir = project.getBasedir();
        try {
            repositoryConfig = RepositoryConfigs.loadRepositoryConfig(configYaml, sourceDir);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to load updatebot.yml file from " + configYaml + ". " + e, e);
        }
        GitRepositoryConfig config;
        try {
            config = RepositoryConfigs.getGitHubRepositoryDetails(repositoryConfig, sourceDir);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to find GitRepositoryConfig from updatebot.yml file " + configYaml + ". " + e, e);
        }
        if (!Files.isFile(file)) {
            throw new MojoExecutionException("File does not exist " + file);
        }
        log.debug("Loaded git config " + config + " from " + sourceDir);

        MavenDependencies mavenDependencies = null;
        if (config != null) {
            Dependencies pull = config.getPull();
            if (pull != null) {
                mavenDependencies = pull.getMaven();
            }
        }
        Filter<MavenArtifactKey> dependencyFilter = Filters.falseFilter();
        if (mavenDependencies != null) {
            List<MavenDependencyFilter> dependencies = mavenDependencies.getDependencies();
            if (dependencies != null) {
                dependencyFilter = MavenDependencyFilter.createFilter(dependencies);
            }
        }

        List<MavenProject> projects = project.getCollectedProjects();

        MavenArtifactVersionChanges changes;
        try {
            changes = MarkupHelper.loadYaml(file, MavenArtifactVersionChanges.class);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to load changes YAML " + file + ". " + e, e);
        }

        List<MavenArtifactVersionChange> changeList = changes.getChanges();
        if (changeList == null || changeList.isEmpty()) {
            log.info("No changes to apply!");
            return;
        }

        for (MavenArtifactVersionChange change : changeList) {
            log.info("Applying change " + change);
        }

        for (MavenProject project : projects) {
            applyChanges(project, changeList, mavenDependencies, dependencyFilter);
        }

    }

    protected void applyChanges(MavenProject project, List<MavenArtifactVersionChange> changeList, MavenDependencies mavenDependencies, Filter<MavenArtifactKey> dependencyFilter) {
        // TODO
    }


}
