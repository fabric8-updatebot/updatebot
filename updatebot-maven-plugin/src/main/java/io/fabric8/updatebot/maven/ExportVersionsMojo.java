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

import io.jenkins.updatebot.Configuration;
import io.jenkins.updatebot.kind.maven.MavenScopes;
import io.fabric8.updatebot.maven.support.MavenHelper;
import io.jenkins.updatebot.model.Dependencies;
import io.jenkins.updatebot.model.GitRepositoryConfig;
import io.jenkins.updatebot.model.MavenArtifactKey;
import io.jenkins.updatebot.model.MavenArtifactVersionChange;
import io.jenkins.updatebot.model.MavenArtifactVersionChanges;
import io.jenkins.updatebot.model.MavenDependencies;
import io.jenkins.updatebot.model.MavenDependencyFilter;
import io.jenkins.updatebot.model.RepositoryConfig;
import io.jenkins.updatebot.model.RepositoryConfigs;
import io.jenkins.updatebot.support.MarkupHelper;
import io.fabric8.utils.Filter;
import io.fabric8.utils.Filters;
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
import java.util.TreeMap;

/**
 * Exports the versions from the source code of the current project so that we can apply the versions
 * to other projects
 */
@Mojo(name = "export", aggregator = true, requiresProject = true,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class ExportVersionsMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Parameter(property = "updateBotYaml")
    protected String configYaml;

    @Parameter(property = "destFile", defaultValue = "${basedir}/target/updatebot-versions.yml")
    protected File destFile;

    private RepositoryConfig repositoryConfig;

    protected static void addArtifact(Map<MavenArtifactKey, MavenArtifactVersionChange> exportVersions, MavenArtifactKey artifactKey, String version, String scope) {
        exportVersions.put(artifactKey, new MavenArtifactVersionChange(artifactKey, version, scope));
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Log log = getLog();
        File sourceDir = project.getBasedir();
        Configuration configuration = new Configuration();
        configuration.setSourceDir(sourceDir);
        try {
            repositoryConfig = RepositoryConfigs.loadRepositoryConfig(configuration, configYaml, sourceDir);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to load updatebot.yml file from " + configYaml + ". " + e, e);
        }
        GitRepositoryConfig config;
        try {
            config = RepositoryConfigs.getGitHubRepositoryDetails(repositoryConfig, sourceDir);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to find GitRepositoryConfig from updatebot.yml file " + configYaml + ". " + e, e);
        }
        log.debug("Loaded git config " + config + " from " + sourceDir);

        MavenDependencies mavenDependencies = null;
        if (config != null) {
            Dependencies push = config.getPush();
            if (push != null) {
                mavenDependencies = push.getMaven();
            }
        }
        Filter<MavenArtifactKey> dependencyFilter = Filters.falseFilter();
        if (mavenDependencies != null) {
            List<MavenDependencyFilter> dependencies = mavenDependencies.getDependencies();
            if (dependencies != null) {
                dependencyFilter = MavenDependencyFilter.createFilter(dependencies);
            }
        }
        Map<MavenArtifactKey, MavenArtifactVersionChange> exportVersions = new TreeMap<>();

        List<MavenProject> projects = project.getCollectedProjects();
        for (MavenProject project : projects) {
            MavenArtifactKey artifactKey = new MavenArtifactKey(project.getGroupId(), project.getArtifactId());
            addArtifact(exportVersions, artifactKey, project.getVersion(), MavenScopes.ARTIFACT);

            log.debug("Collected project : " + project);
            List<Dependency> dependencies = project.getDependencies();
            for (Dependency dependency : dependencies) {
                MavenArtifactKey dependencyKey = MavenHelper.toMavenDependency(dependency);
                if (dependencyFilter.matches(dependencyKey)) {
                    log.debug("    dependency: " + dependency);
                    addArtifact(exportVersions, dependencyKey, dependency.getVersion(), MavenScopes.DEPENDENCY);
                }
            }

            // TODO we don't add the plugins!
        }

        destFile.getParentFile().mkdirs();
        try {
            MavenArtifactVersionChanges changes = new MavenArtifactVersionChanges(exportVersions.values());
            MarkupHelper.saveYaml(changes, destFile);

            log.info("Generated updatebot version file " + destFile);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to write to " + destFile + ". " + e, e);
        }
    }


}
