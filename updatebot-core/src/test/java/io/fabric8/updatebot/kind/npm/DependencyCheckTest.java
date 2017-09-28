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
import io.fabric8.updatebot.kind.npm.dependency.DependencyCheck;
import io.fabric8.updatebot.kind.npm.dependency.DependencyInfo;
import io.fabric8.updatebot.kind.npm.dependency.DependencyTree;
import io.fabric8.updatebot.support.MarkupHelper;
import io.fabric8.updatebot.test.Tests;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 */
public class DependencyCheckTest {
    private static final transient Logger LOG = LoggerFactory.getLogger(DependencyCheckTest.class);

    protected File testDir = new File(Tests.getBasedir(), "src/test/resources/npm/dependencies");
    protected String fileName;

    public static DependencyTree assertLoadDependencyTree(File testFile) throws IOException {
        assertThat(testFile).isFile().exists();

        JsonNode node = MarkupHelper.loadJson(testFile);
        DependencyTree tree = DependencyTree.parseTree(node);
        assertThat(tree).describedAs("Failed to load file " + testFile).isNotNull();
        return tree;
    }

    @Test
    public void testValidDependencies() throws Exception {
        DependencyTree tree = assertLoadDependencyTree("fabric8-planner.json");

        assertDependencyCheck(tree, "ngx-base", true);
        assertDependencyCheck(tree, "ngx-fabric8-wit", true);
    }

    @Test
    public void testChangeBase() throws Exception {
        DependencyTree tree = assertLoadDependencyTree("fabric8-planner-change-ngx-base.json");

        assertDependencyCheck(tree, "ngx-base", false);
        assertDependencyCheck(tree, "ngx-fabric8-wit", true);
    }

    @Test
    public void testChangeFabric8Wit() throws Exception {
        DependencyTree tree = assertLoadDependencyTree("fabric8-planner-change-ngx-fabric8-wit.json");

        assertDependencyCheck(tree, "ngx-base", true);
        assertDependencyCheck(tree, "ngx-fabric8-wit", true);
    }

    public DependencyTree assertLoadDependencyTree(String fileName) throws IOException {
        this.fileName = fileName;
        return assertLoadDependencyTree(new File(testDir, fileName));
    }

    public void assertDependencyCheck(DependencyTree tree, String dependency, boolean valid) {
        DependencyCheck check = tree.dependencyCheck(dependency);
        DependencyInfo info = check.getDependencyInfo();
        assertThat(info).describedAs("Should find a DependencyInfo for " + dependency).isNotNull();

        String validText = check.isValid() ? "valid" : "invalid";
        LOG.info(fileName + " " + info + " " + validText + ": " + check.getMessage());
        assertThat(check.isValid()).describedAs("Dependency check: " + dependency + " " + check.getMessage()).isEqualTo(valid);
    }

}
