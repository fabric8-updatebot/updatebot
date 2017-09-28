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
package io.fabric8.updatebot.fork;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.updatebot.commands.CommandContext;
import io.fabric8.updatebot.github.GitHubHelpers;
import io.fabric8.updatebot.kind.npm.NpmDependencyKinds;
import io.fabric8.updatebot.kind.npm.NpmDependencyTreeGenerator;
import io.fabric8.updatebot.repository.LocalRepository;
import io.fabric8.updatebot.support.MarkupHelper;
import io.fabric8.updatebot.test.Tests;
import io.fabric8.utils.IOHelpers;
import io.fabric8.utils.Objects;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Stubs out the NPM dependency checking so we can simulate releases
 */
public class TestNpmDependencyTreeGenerator implements NpmDependencyTreeGenerator {
    private static final transient Logger LOG = LoggerFactory.getLogger(TestNpmDependencyTreeGenerator.class);

    private final String destinationRepoName;
    private Map<String, String> simuatedReleases = new HashMap<>();

    public TestNpmDependencyTreeGenerator(String destinationRepoName) {
        this.destinationRepoName = destinationRepoName;
    }

    @Override
    public void generateDependencyTree(CommandContext context, String dependencyFileName) {
        File outFile = new File(context.getDir(), dependencyFileName);
        LocalRepository repository = context.getRepository();
        GHRepository gitHubRepository = GitHubHelpers.getGitHubRepository(repository);
        if (gitHubRepository != null) {
            String name = gitHubRepository.getName();
            if (Objects.equal(destinationRepoName, name)) {
                try {
                    generateSimulatedDependencyJson(context, outFile);
                } catch (IOException e) {
                    fail("Failed to generate dependency JSON", e);
                }
                return;
            }
        }
        // lets generate an empty dependency file for now
        try {
            IOHelpers.writeFully(outFile, "{}");
        } catch (IOException e) {
            LOG.error("Failed to generate " + outFile + ". " + e, e);
        }
    }

    private void generateSimulatedDependencyJson(CommandContext context, File outFile) throws IOException {
        File sourceDependencyDir = new File(Tests.getBasedir(), "src/test/resources/npm/dependencies");
        assertThat(sourceDependencyDir).describedAs("npm dependency samples dir").isDirectory();

        File sourceDependencyFile = new File(sourceDependencyDir, destinationRepoName + ".json");
        assertThat(sourceDependencyFile).describedAs("npm dependency JSON").isFile();


        JsonNode tree = MarkupHelper.loadJson(sourceDependencyFile);

        modifyReleasedVersions(tree, false);

        MarkupHelper.savePrettyJson(outFile, tree);
    }

    private void modifyReleasedVersions(JsonNode tree, boolean recurse) {
        for (String dependencyKey : NpmDependencyKinds.DEPENDENCY_KEYS) {
            JsonNode dependencies = tree.get(dependencyKey);
            if (dependencies instanceof ObjectNode) {
                ObjectNode objectNode = (ObjectNode) dependencies;
                modifyReleasedVersionsChild(objectNode);


                // we are in a child dependency of a released version so lets check for
                // any transitive versions to update too
                if (recurse) {
                    Iterator<String> iter = objectNode.fieldNames();
                    while (iter.hasNext()) {
                        String field = iter.next();
                        JsonNode dependentPackage = objectNode.get(field);
                        if (dependentPackage != null) {
                            modifyReleasedVersions(dependentPackage, true);
                        }
                    }
                }
            }
        }
    }

    private void modifyReleasedVersionsChild(ObjectNode dependencyNode) {
        for (Map.Entry<String, String> entry : simuatedReleases.entrySet()) {
            String dependency = entry.getKey();
            String version = entry.getValue();

            JsonNode node = dependencyNode.get(dependency);
            if (node instanceof ObjectNode) {
                ObjectNode objectNode = (ObjectNode) node;
                objectNode.put("version", version);

                // recurse through children if we find anything
                modifyReleasedVersions(objectNode, true);
            }
        }
    }

    public void addRelease(String dependency, String version) {
        simuatedReleases.put(dependency, version);
    }
}
