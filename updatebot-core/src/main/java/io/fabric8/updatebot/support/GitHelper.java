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
package io.fabric8.updatebot.support;

import io.fabric8.utils.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class GitHelper {
    private static final transient Logger LOG = LoggerFactory.getLogger(GitHelper.class);

    public static List<String> getGitHubCloneUrls(String gitHubHost, String orgName, String repository) {
        List<String> answer = new ArrayList<>();
        answer.add("https://" + gitHubHost + "/" + orgName + "/" + repository);
        answer.add("git@" + gitHubHost + ":" + orgName + "/" + repository);
        answer.add("git://" + gitHubHost + "/" + orgName + "/" + repository);

        // now lets add the .git versions
        List<String> copy = new ArrayList<>(answer);
        for (String url : copy) {
            if (!url.endsWith(".git")) {
                answer.add(url + ".git");
            }
        }
        return answer;
    }

    /**
     * Parses the git URL string and determines the host and organisation string
     */
    public static GitRepositoryInfo parseGitRepositoryInfo(String gitUrl) {
        if (Strings.isNullOrBlank(gitUrl)) {
            return null;
        }
        try {
            URI url = new URI(gitUrl);
            String host = url.getHost();
            String userInfo = url.getUserInfo();
            String path = url.getPath();
            path = stripSlashesAndGit(path);
            if (Strings.notEmpty(userInfo)) {
                return new GitRepositoryInfo(host, userInfo, path);
            } else {
                if (Strings.notEmpty(path)) {
                    String[] paths = path.split("/", 2);
                    if (paths.length > 1) {
                        return new GitRepositoryInfo(host, paths[0], paths[1]);
                    }
                }
                return null;
            }
        } catch (URISyntaxException e) {
            // ignore
        }
        String prefix = "git@";
        if (gitUrl.startsWith(prefix)) {
            String path = Strings.stripPrefix(gitUrl, prefix);
            path = stripSlashesAndGit(path);
            String[] paths = path.split(":|/", 3);
            if (paths.length == 3) {
                return new GitRepositoryInfo(paths[0], paths[1], paths[2]);
            }
        }
        return null;
    }

    protected static String stripSlashesAndGit(String path) {
        path = Strings.stripPrefix(path, "/");
        path = Strings.stripPrefix(path, "/");
        path = Strings.stripSuffix(path, "/");
        path = Strings.stripSuffix(path, ".git");
        return path;
    }

}
