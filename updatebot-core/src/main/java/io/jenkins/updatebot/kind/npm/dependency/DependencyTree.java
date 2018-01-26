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
package io.jenkins.updatebot.kind.npm.dependency;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.jenkins.updatebot.support.JsonNodes;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.jenkins.updatebot.kind.npm.NpmDependencyKinds.DEPENDENCY_KEYS;

/**
 */
public class DependencyTree {
    private Map<String, DependencyInfo> dependencies = new LinkedHashMap<>();

    public static DependencyTree parseTree(JsonNode tree) {
        DependencyTree dependencyTree = new DependencyTree();
        dependencyTree.parse(tree, null);
        return dependencyTree;
    }

    protected void parse(JsonNode tree, DependencyInfo parent) {
        for (String dependencyKey : DEPENDENCY_KEYS) {
            JsonNode deps = tree.get(dependencyKey);
            if (deps instanceof ObjectNode) {
                ObjectNode objectNode = (ObjectNode) deps;
                Iterator<String> iter = objectNode.fieldNames();
                while (iter.hasNext()) {
                    String field = iter.next();
                    JsonNode properties = objectNode.get(field);
                    String version = JsonNodes.textValue(properties, "version");
                    DependencyInfo dependencyInfo = getOrCreateDependencyInfo(field);
                    if (parent == null) {
                        dependencyInfo.setVersion(version);
                    } else {
                        dependencyInfo.addDependency(parent, version, dependencyKey);
                    }
                    parse(properties, dependencyInfo);
                }
            }
        }
    }

    public DependencyCheck dependencyCheck(String dependency) {
        DependencyInfo info = getDependencyInfo(dependency);
        if (info == null) {
            return new DependencyCheck(true, "Not found!", null);
        }
        return info.dependencyCheck();
    }

    protected DependencyInfo getOrCreateDependencyInfo(String dependency) {
        DependencyInfo answer = getDependencyInfo(dependency);
        if (answer == null) {
            answer = new DependencyInfo(dependency);
            dependencies.put(dependency, answer);
        }
        return answer;

    }

    public DependencyInfo getDependencyInfo(String dependency) {
        return dependencies.get(dependency);
    }
}
