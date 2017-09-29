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
package io.fabric8.updatebot.comments;

import io.fabric8.updatebot.github.Issues;
import io.fabric8.updatebot.kind.DependenciesCheck;
import io.fabric8.updatebot.kind.Kind;
import io.fabric8.updatebot.kind.KindDependenciesCheck;
import io.fabric8.updatebot.kind.npm.dependency.DependencyCheck;
import io.fabric8.updatebot.kind.npm.dependency.DependencyInfo;
import io.fabric8.updatebot.model.DependencyVersionChange;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 */
public class IssuePendingChangeReadWriteTest {
    @Test
    public void testGenerateAndParsePendingChanges() throws Exception {
        String dependency1 = "ngx-base";
        String dependency2 = "ngx-fabric8-wit";
        String dependency3 = "something-random";

        String version1 = "1.3.0";
        String version2 = "2.3.4";
        String version3 = "4.0.0";

        DependencyVersionChange change1 = new DependencyVersionChange(Kind.NPM, dependency1, version1);
        DependencyVersionChange change2 = new DependencyVersionChange(Kind.NPM, dependency2, version2, "devDependencies");
        List<DependencyVersionChange> expectedChanges = Arrays.asList(change1, change2);

        ArrayList<DependencyVersionChange> validChanges = new ArrayList<>();
        Map<Kind, KindDependenciesCheck> failureMap = new HashMap<>();
        Map<String, DependencyCheck> failedChecks = new HashMap<>();
        addFailedCheck(failedChecks, dependency1, version1);
        addFailedCheck(failedChecks, dependency2, version2);
        addFailedCheck(failedChecks, dependency3, version3);
        KindDependenciesCheck npmFailures = new KindDependenciesCheck(validChanges, expectedChanges, failedChecks);
        failureMap.put(Kind.NPM, npmFailures);
        DependenciesCheck check = new DependenciesCheck(validChanges, expectedChanges, failureMap);

        String command = Issues.conflictChangesComment(expectedChanges, check);

        System.out.println("Generated pending comment command: " + command);

        List<DependencyVersionChange> changes = Issues.parseUpdateBotIssuePendingChangesComment(command);
        assertThat(changes).describedAs("Parsed changes " + changes).hasSize(2).isEqualTo(expectedChanges);
    }

    private void addFailedCheck(Map<String, DependencyCheck> failedChecks, String dependency, String version) {
        String message = "multiple versions found " + version + " and 3.0.0";
        DependencyInfo dependencyInfo = new DependencyInfo(dependency);
        dependencyInfo.setVersion(version);
        DependencyCheck check = new DependencyCheck(false, message, dependencyInfo);
        failedChecks.put(dependency, check);
    }


}
