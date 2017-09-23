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
package io.fabric8.updatebot.kind.npm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.updatebot.UpdateVersionContext;
import io.fabric8.updatebot.kind.Updater;
import io.fabric8.updatebot.support.FileHelper;
import io.fabric8.updatebot.support.MarkupHelper;
import io.fabric8.utils.Objects;

import java.io.File;
import java.io.IOException;

/**
 */
public class PackageJsonUpdater implements Updater {
    private String[] dependencyKeys = {
            "dependencies", "devDependencies", "peerDependencies"
    };

    @Override
    public boolean isApplicable(UpdateVersionContext context) {
        return FileHelper.isFile(context.file("package.json"));
    }

    @Override
    public boolean updateVersion(UpdateVersionContext context) throws IOException {
        File file = context.file("package.json");
        JsonNode tree = MarkupHelper.loadJson(file);
        boolean answer = false;
        for (String dependencyKey : dependencyKeys) {
            JsonNode dependencies = tree.get(dependencyKey);
            if (dependencies instanceof ObjectNode) {
                ObjectNode objectNode = (ObjectNode) dependencies;
                if (updateDependencyVersion(dependencyKey, objectNode, context)) {
                    answer = true;
                }
            }
        }
        if (answer) {
            MarkupHelper.savePrettyJson(file, tree);
            context.updatedFile(file);
        }
        return answer;
    }

    protected boolean updateDependencyVersion(String dependencyKey, ObjectNode dependencies, UpdateVersionContext context) {
        String name = context.getName();
        String value = context.getValue();
        JsonNode dependency = dependencies.get(name);
        if (dependency != null && dependency.isTextual()) {
            String old = dependency.textValue();
            if (!Objects.equal(old, value)) {
                dependencies.put(name, value);
                context.updatedVersion(dependencyKey, name, value, old);
                return true;
            }
        }
        return false;
    }
}
