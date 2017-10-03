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
package io.fabric8.updatebot.test;

import io.fabric8.updatebot.Configuration;
import io.fabric8.updatebot.model.GitHubProjects;
import io.fabric8.updatebot.model.GitRepositoryConfig;
import io.fabric8.updatebot.model.GithubOrganisation;
import io.fabric8.updatebot.model.RepositoryConfig;
import io.fabric8.updatebot.support.MarkupHelper;
import io.fabric8.updatebot.support.Strings;
import io.fabric8.utils.Files;
import org.assertj.core.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

/**
 */
public class Tests {
    private static final transient Logger LOG = LoggerFactory.getLogger(Tests.class);

    public static File getBasedir() {
        String basedirName = System.getProperty("basedir", ".");
        return new File(basedirName);
    }

    /**
     * Copies the source test data files into a test data area so we can test on the files and modify them
     *
     * @return the directory of test files
     */
    public static File copyPackageSources(Class<?> clazz) {
        String packagePath = getPackagePath(clazz);
        File basedir = Tests.getBasedir();
        File testDir = getTestDataDir(clazz);
        File srcDir = new File(basedir, "src/test/resources/" + packagePath);

        assertThat(srcDir).describedAs("Source test files for test " + clazz.getName()).isDirectory();

        if (testDir.exists()) {
            Files.recursiveDelete(testDir);
        }

        assertThat(testDir).describedAs("Test data dir").doesNotExist();

        testDir.mkdirs();

        try {
            Files.copy(srcDir, testDir);
        } catch (IOException e) {
            fail("Failed to copy " + srcDir + " to " + testDir + ". " + e, e);
        }

        assertThat(testDir).describedAs("Test data dir").isDirectory();
        File[] childFiles = testDir.listFiles();
        assertThat(childFiles).describedAs("Test data dir " + testDir).isNotEmpty();
        return testDir;
    }

    public static String getPackagePath(Class<?> clazz) {
        return clazz.getPackage().getName().replace('.', '/');
    }

    public static File getTestDataDir(Class<?> clazz) {
        String packagePath = getPackagePath(clazz);
        File basedir = Tests.getBasedir();
        return new File(basedir, "target/test-data/" + packagePath + "/" + clazz.getSimpleName());
    }

    /**
     * Returns a local test file, asserting that the file exists
     */
    public static File testFile(File dir, String localPath) {
        File answer = new File(dir, localPath);
        assertThat(answer).describedAs("test file").isFile();
        return answer;
    }

    public static String getCleanWorkDir(Class<?> clazz) {
        File testDataDir = getTestDataDir(clazz);
        System.out.println("Using workDir: " + testDataDir);
        String property = System.getProperty("updatebot.preserve.testdir", "false");
        if (property.equals("true")) {
            LOG.info("Preserving contents of " + testDataDir + " to speed up test");
        } else {
            Files.recursiveDelete(testDataDir);
        }
        return testDataDir.getPath();
    }

    /**
     * Returns true if we have configured environment variables or system properties so that we can use the github API to query repos
     */
    public static boolean canTestWithGithubAPI(Configuration configuration) {
        if (Strings.notEmpty(configuration.getGithubUsername()) &&
                (Strings.notEmpty(configuration.getGithubPassword()) || Strings.notEmpty(configuration.getGithubToken()))) {
            return true;
        }
        LOG.info("Disabling this test case as we do not have a github username and password/token defined via environment variables");
        return false;
    }

    public static RepositoryConfig assertLoadProjects(File config) throws IOException {
        RepositoryConfig repositoryConfig = MarkupHelper.loadYaml(config, RepositoryConfig.class);
        assertThat(repositoryConfig).describedAs("projects").isNotNull();
        return repositoryConfig;
    }

    public static GitRepositoryConfig assertGithubRepositoryFindByName(RepositoryConfig repositoryConfig, String repoName) {
        GitHubProjects github = repositoryConfig.getGithub();
        assertThat(github).describedAs("github").isNotNull();

        List<GithubOrganisation> organisations = github.getOrganisations();
        assertThat(organisations).describedAs("github organisations").isNotNull();
        for (GithubOrganisation organisation : organisations) {
            List<GitRepositoryConfig> repositories = organisation.getRepositories();
            for (GitRepositoryConfig repository : repositories) {
                if (repoName.equals(repository.getName())) {
                    return repository;
                }
            }
        }
        Assertions.fail("Could not find github repository called " + repoName + " in project: " + github);
        return null;
    }
}
