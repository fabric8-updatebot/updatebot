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
package io.fabric8.updatebot.repository;

import io.fabric8.updatebot.model.GitRepository;

import java.io.File;

/**
 */
public class LocalRepository {
    private GitRepository repo;
    private File dir;

    public LocalRepository(GitRepository repo, File dir) {
        this.repo = repo;
        this.dir = dir;
    }

    /**
     * Returns a local repository from a directory.
     */
    public static LocalRepository fromDirectory(File dir) {
        return new LocalRepository(new GitRepository(dir.getName()), dir);
    }

    @Override
    public String toString() {
        return "LocalRepository{" +
                "repo=" + repo +
                ", dir=" + dir +
                '}';
    }

    public GitRepository getRepo() {
        return repo;
    }

    public File getDir() {
        return dir;
    }

    public String getCloneUrl() {
        return repo.getCloneUrl();
    }

    /**
     * Returns true if this repository can be cloned using the given URL
     */
    public boolean hasCloneUrl(String cloneUrl) {
        return repo.hasCloneUrl(cloneUrl);
    }
}
