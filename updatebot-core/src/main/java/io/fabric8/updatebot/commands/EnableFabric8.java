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
import com.beust.jcommander.Parameters;
import io.fabric8.updatebot.CommandNames;
import io.fabric8.updatebot.Configuration;
import io.fabric8.updatebot.github.GitHubHelpers;
import io.fabric8.updatebot.kind.Kind;
import io.fabric8.updatebot.kind.maven.ElementProcessors;
import io.fabric8.updatebot.kind.maven.MavenDependencyVersionChange;
import io.fabric8.updatebot.kind.maven.MavenScopes;
import io.fabric8.updatebot.kind.maven.MavenUpdater;
import io.fabric8.updatebot.model.DependencyVersionChange;
import io.fabric8.updatebot.model.GitHubProjects;
import io.fabric8.updatebot.model.GitRepository;
import io.fabric8.updatebot.model.RepositoryConfig;
import io.fabric8.updatebot.repository.LocalRepository;
import io.fabric8.updatebot.repository.Repositories;
import io.fabric8.updatebot.support.FileExtensionFilter;
import io.fabric8.updatebot.support.FileHelper;
import io.fabric8.updatebot.support.Strings;
import io.fabric8.updatebot.support.VersionHelper;
import io.fabric8.utils.Files;
import org.kohsuke.github.GHPerson;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Enables fabric8 CI / CD by adding a Jenkinsfile to a project via a Pull Request
 * and optionally adding the fabric8-maven-plugin to any maven project not using it already
 */
@Parameters(commandNames = CommandNames.ENABLE_F8, commandDescription = "Enables fabric8 CI/CD by submitting a PR to add a Jenkinsfile.")
public class EnableFabric8 extends ModifyFilesCommandSupport {
    private static final transient Logger LOG = LoggerFactory.getLogger(EnableFabric8.class);

    @Parameter(names = "--pipeline", description = "The pipeline name to use from the pipeline library")
    private String pipeline;

    @Parameter(names = "--overwrite", description = "Should we overwrite any Jenkinsfile if it already exists?", arity = 1)
    private boolean overwriteJenkinsfile;

    @Parameter(description = "The github organisation/repository", required = true)
    private String organisationAndRepository;

    private String organisation;
    private String name;

    private String[] defaultJenkinsfileNames = {"ReleaseStageApproveAndPromote", "ReleaseAndStage", "Release"};

    public String getPipeline() {
        return pipeline;
    }

    public void setPipeline(String pipeline) {
        this.pipeline = pipeline;
    }

    public String getOrganisationAndRepository() {
        return organisationAndRepository;
    }

    public void setOrganisationAndRepository(String organisationAndRepository) {
        this.organisationAndRepository = organisationAndRepository;
        int idx = organisationAndRepository.indexOf('/');
        if (idx <= 0) {
            throw new IllegalArgumentException("Invalid format. Expected a string of the form: organisationName/repositoryName");
        }
        this.organisation = organisationAndRepository.substring(0, idx);
        this.name = organisationAndRepository.substring(idx + 1);
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

        RepositoryConfig config = new RepositoryConfig();
        GitHubProjects gitHubProjects = config.github();
        gitHubProjects.organisation(organisation).repository(name);
        String jenkinsfileGitCloneURL = configuration.getJenksinsfileGitRepo();
        GitRepository jenkinsfileGitRepo = new GitRepository("jenkinsfile library", jenkinsfileGitCloneURL);
        config.add(jenkinsfileGitRepo);

        setRepositoryConfig(config);

        List<LocalRepository> localRepositories = Repositories.cloneOrPullRepositories(configuration, config);
        setLocalRepositories(localRepositories);
        LocalRepository localRepository = LocalRepository.findRepository(localRepositories, name);
        if (localRepository == null) {
            throw new IOException("Could not find repository called " + name + " in " + localRepositories);
        }
        LocalRepository jenkinsfileRepository = LocalRepository.findRepository(localRepositories, jenkinsfileGitRepo);
        if (jenkinsfileRepository == null) {
            throw new IOException("Could not find repository for " + jenkinsfileGitRepo + " in " + localRepositories);
        }

        EnableFabric8Context context = new EnableFabric8Context(localRepository, configuration, jenkinsfileRepository);
        parentContext.addChild(context);

        run(context);

        return parentContext;
    }


    @Override
    protected boolean doProcess(CommandContext context) throws IOException {
        return enableFabric8((EnableFabric8Context) context);
    }

    protected boolean enableFabric8(EnableFabric8Context context) throws IOException {
        boolean updated = false;
        MavenUpdater updater = new MavenUpdater();
        if (updater.isApplicable(context)) {
            List<DependencyVersionChange> changes = createEnableFabric8VersionChanges();
            if (updater.pushVersions(context, changes)) {
                updated = true;
            }
        }
        if (addPipeline(context)) {
            updated = true;
        }
        return updated;
    }

    protected boolean addPipeline(EnableFabric8Context context) throws IOException {
        Configuration configuration = context.getConfiguration();
        if (Files.isFile(context.file("Jenkinsfile"))) {
            if (!overwriteJenkinsfile) {
                return false;
            }
        }
        LocalRepository jenkinsfileRepository = context.getJenkinsfileRepository();
        if (jenkinsfileRepository == null) {
            configuration.warn(LOG, "No Jenkinsfile library!");
            return false;
        }

        String path = getPipelinePath(context);
        if (path == null) {
            return false;
        }
        File jenkinsLibraryDir = jenkinsfileRepository.getDir();
        File jenkinsfileFolder = new File(jenkinsLibraryDir, path);

        File jenkinsfile = null;
        boolean found = false;
        if (Strings.notEmpty(pipeline)) {
            jenkinsfile = new File(jenkinsfileFolder, pipeline + "/Jenkinsfile");
            found = Files.isFile(jenkinsfile);
        }
        if (!found) {
            if (Strings.notEmpty(pipeline)) {
                configuration.warn(LOG, "Could not find Jenkinsfile " + jenkinsfile + " in the library!");
            } else {
                configuration.warn(LOG, "No pipeline configured so using a default");
            }
            for (String name : defaultJenkinsfileNames) {
                jenkinsfile = new File(jenkinsfileFolder, name + "/Jenkinsfile");
                if (Files.isFile(jenkinsfile)) {
                    found = true;
                    break;
                }

            }
        }
        if (!found) {
            return false;
        }

        String relativePath = Files.getRelativePath(jenkinsLibraryDir, jenkinsfile);
        configuration.info(LOG, "Adding Jenkinsfile " + Strings.trimAllPrefix(relativePath, "/"));
        File dir = context.getDir();
        Files.copy(jenkinsfile, new File(dir, "Jenkinsfile"));
        return true;
    }

    protected String getPipelinePath(EnableFabric8Context context) {
        if (hasFile(context, "pom.xml")) {
            return "maven";
        }
        if (hasFile(context, "package.json") || hasExtension(context, "js")) {
            return "node";
        }
        if (hasExtension(context, "go")) {
            return "golang";
        }
        if (hasFile(context, "Rakefile") || hasExtension(context, "rb")) {
            return "ruby";
        }
        if (hasExtension(context, "swift")) {
            return "swift";
        }
        if (hasFile(context, "urls.py") || hasFile(context, "wsgi.py")) {
            return "django";
        }
        if (hasExtension(context, "swift")) {
            return "swift";
        }

        if (hasExtension(context, "php")) {
            return "php";
        }
        if (hasExtension(context, "cs")) {
            return "dotnet";
        }
        if (hasExtension(context, "sbt")) {
            return "sbt";
        }
        return null;
    }

    protected boolean hasExtension(EnableFabric8Context context, final String extension) {
        FileFilter filter = new FileExtensionFilter(extension);
        return FileHelper.hasFile(context.getDir(), filter);
    }

    protected boolean hasFile(EnableFabric8Context context, String name) {
        return Files.isFile(new File(context.getDir(), name));
    }


    protected List<DependencyVersionChange> createEnableFabric8VersionChanges() {
        List<DependencyVersionChange> answer = new ArrayList<>();
        String fmpVersion = VersionHelper.fabric8MavenPluginVersion();
        answer.add(new MavenDependencyVersionChange("io.fabric8:fabric8-maven-plugin", fmpVersion, MavenScopes.PLUGIN, true, ElementProcessors.createFabric8MavenPluginElementProcessor()));

        addVersionChanges(answer, VersionHelper.fabric8Version(), "io.fabric8",
                "fabric8-utils", "kubernetes-api", "fabric8-parent", "fabric8-project-bom",
                "fabric8-project-bom-camel-spring-boot", "fabric8-project-bom-cxf-spring-boot", "fabric8-project-bom-fuse-karaf", "fabric8-project-bom-with-platform-deps");
        return answer;
    }

    private void addVersionChanges(List<DependencyVersionChange> answer, String version, String groupId, String... artifacts) {
        for (String artifact : artifacts) {
            answer.add(new DependencyVersionChange(Kind.MAVEN, groupId + ":" + artifact, version));
        }
    }

}
