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
import io.fabric8.utils.Objects;

import java.io.File;
import java.io.FileFilter;

/**
 */
public class FileExtensionFilter implements FileFilter {

    private final String extension;

    public FileExtensionFilter(String extension) {
        this.extension = extension;
    }

    @Override
    public String toString() {
        return "FileExtensionFilter{" +
                "extension='" + extension + '\'' +
                '}';
    }

    @Override
    public boolean accept(File file) {
        return Objects.equal(extension, Files.getExtension(file.getName()));
    }
}
