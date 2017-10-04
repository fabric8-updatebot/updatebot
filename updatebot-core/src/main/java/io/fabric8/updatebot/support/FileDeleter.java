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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

/**
 * Allows a number of files to be deleted using a Java try-with-resources block (try / catch).
 * <br>
 * <code>
 *     try (new FileDeleter(file1, file2)) { ... }
 * </code>
 */
public class FileDeleter implements Closeable {
    private final File[] files;

    public FileDeleter(File... files) {
        this.files = files;
    }

    @Override
    public void close() throws IOException {
        for (File file : files) {
            if (file.exists()) {
                file.delete();
            }
        }
    }
}
