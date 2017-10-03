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
package io.fabric8.updatebot.model;

import io.fabric8.utils.Files;
import io.fabric8.utils.GitHelpers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import static io.fabric8.updatebot.support.MarkupHelper.loadYaml;

/**
 */
public class RepositoryConfigs {
    /**
     * Returns the UpdateBot project configurations from the given configFile (File or URL) and source directory
     */
    public static RepositoryConfig loadRepositoryConfig(String configFile, File sourceDir) throws IOException {
        File file = new File(configFile);
        if (Files.isDirectory(sourceDir) && !file.isAbsolute()) {
            file = new File(sourceDir, configFile);
        }
        if (!Files.isFile(file)) {
            URL url = null;
            try {
                url = new URL(configFile);
                InputStream in = null;
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
            throw new FileNotFoundException(file.getCanonicalPath());
        }
        return loadYaml(file, RepositoryConfig.class);
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
            return repositoryConfig.getRepositoryDetails(cloneUrl);
        }
        return null;
    }
}
