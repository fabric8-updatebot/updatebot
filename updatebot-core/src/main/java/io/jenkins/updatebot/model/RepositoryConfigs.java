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
package io.jenkins.updatebot.model;

import io.jenkins.updatebot.Configuration;
import io.jenkins.updatebot.git.GitHelper;
import io.jenkins.updatebot.git.GitRepositoryInfo;
import io.jenkins.updatebot.support.Strings;
import io.fabric8.utils.Files;
import io.fabric8.utils.GitHelpers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import static io.jenkins.updatebot.support.MarkupHelper.loadYaml;

/**
 */
public class RepositoryConfigs {
    private static final transient Logger LOG = LoggerFactory.getLogger(RepositoryConfigs.class);

    /**
     * Returns the UpdateBot project configurations from the given configFile (File or URL) and source directory
     */
    public static RepositoryConfig loadRepositoryConfig(Configuration configuration, String configFile, File sourceDir) throws IOException {
        File file = new File(configFile);
        if (Files.isDirectory(sourceDir) && !file.isAbsolute()) {
            file = new File(sourceDir, configFile);
        }
        if (!Files.isFile(file)) {
            URL url = null;
            try {
                url = new URL(configFile);
                InputStream in;
                try {
                    in = url.openStream();
                } catch (IOException e) {
                    throw new IOException("Failed to open URL " + configFile + ". " + e, e);
                }
                if (in != null) {
                    return loadYaml(in, RepositoryConfig.class);
                }
            } catch (MalformedURLException e) {
                // ignore
            }
            RepositoryConfig answer = findGithubOrganisationConfig(configuration, sourceDir);
            if (answer != null) {
                return answer;
            }
            throw new FileNotFoundException(file.getCanonicalPath());
        }
        return loadYaml(file, RepositoryConfig.class);
    }

    /**
     * Lets try detect the github organisation level configuration for a project.
     * <p>
     * This lets us have a shared updatebot configuration across repositories within a github organisation
     *
     * @return null if it cannot be found
     */
    protected static RepositoryConfig findGithubOrganisationConfig(Configuration configuration, File sourceDir) throws IOException {
        String gitCloneURL = null;
        try {
            gitCloneURL = GitHelpers.extractGitUrl(sourceDir);
        } catch (IOException e) {
            // ignore
        }
        return loadGithubOrganisationConfig(configuration, gitCloneURL);
    }

    public static RepositoryConfig loadGithubOrganisationConfig(Configuration configuration, String gitCloneURL) throws IOException {
        if (Strings.notEmpty(gitCloneURL)) {
            GitRepositoryInfo info = GitHelper.parseGitRepositoryInfo(gitCloneURL);
            if (info != null) {
                String host = info.getHost();
                String organisation = info.getOrganisation();

                if (Strings.notEmpty(host) && Strings.notEmpty(organisation) && host.equals("github.com")) {
                    String sourceUrl = "https://github.com/" + organisation + "/" + organisation + "-updatebot-config/blob/master/.updatebot.yml";
                    URL url = null;
                    try {
                        url = new URL("https://raw.githubusercontent.com/" + organisation + "/" + organisation + "-updatebot-config/master/.updatebot.yml");
                    } catch (MalformedURLException e) {
                        // ignore - should never happen ;)
                    }
                    if (url != null) {
                        try {
                            InputStream in = url.openStream();
                            if (in != null) {
                                configuration.info(LOG, "Loading UpdateBot configuration at: " + sourceUrl + " from: " + url);
                                return loadYaml(in, RepositoryConfig.class);
                            }
                        } catch (FileNotFoundException e) {
                            // ignore
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns the git repository configuration details
     *
     * @param repositoryConfig
     * @param sourceDir
     * @return
     * @throws IOException
     */
    public static GitRepositoryConfig getGitHubRepositoryDetails(RepositoryConfig repositoryConfig, File sourceDir) throws IOException {
        String cloneUrl = GitHelpers.extractGitUrl(sourceDir);
        if (cloneUrl != null) {
            // lets remove any user/password if using HTTPS
            cloneUrl = GitHelper.removeUsernamePassword(cloneUrl);
            return repositoryConfig.getRepositoryDetails(cloneUrl);
        }
        return null;
    }
}
