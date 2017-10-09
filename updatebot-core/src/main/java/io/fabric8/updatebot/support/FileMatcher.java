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

import io.fabric8.utils.Files;
import org.springframework.util.AntPathMatcher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class FileMatcher {
    private final List<String> includes;
    private final List<String> excludes;
    private AntPathMatcher pathMatcher = new AntPathMatcher(File.pathSeparator);

    public FileMatcher(List<String> includes, List<String> excludes) {
        this.includes = includes;
        this.excludes = excludes;
    }

    public static FileMatcher createFileMatcher(List<String> includes, List<String> excludes) {
        return new FileMatcher(includes, excludes);
    }

    public List<File> matchFiles(File dir) throws IOException {
        List<File> answer = new ArrayList<>();
        addMatchFiles(answer, dir, dir);
        return answer;
    }

    private void addMatchFiles(List<File> answer, File rootDir, File file) throws IOException {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    addMatchFiles(answer, rootDir, child);
                }
            }
        } else {
            String path = Files.getRelativePath(rootDir, file);
            path = Strings.trimAllPrefix(path, "/");
            if (matchesPatterns(path, includes) && !matchesPatterns(path, excludes)) {
                answer.add(file);
            }
        }
    }

    protected boolean matchesPatterns(String path, Iterable<String> patterns) {
        boolean matchesInclude = false;
        for (String include : patterns) {
            if (pathMatcher.match(include, path)) {
                matchesInclude = true;
            }
        }
        return matchesInclude;
    }
}
