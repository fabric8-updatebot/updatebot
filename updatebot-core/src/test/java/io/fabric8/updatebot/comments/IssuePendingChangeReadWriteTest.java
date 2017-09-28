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
import io.fabric8.updatebot.kind.Kind;
import io.fabric8.updatebot.model.DependencyVersionChange;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 */
public class IssuePendingChangeReadWriteTest {
    @Test
    public void testGenerateAndParsePendingChanges() throws Exception {
        DependencyVersionChange dependency1 = new DependencyVersionChange(Kind.NPM, "ngx-base", "1.3.0");
        DependencyVersionChange dependency2 = new DependencyVersionChange(Kind.NPM, "ngx-fabric8-wit", "2.3.4", "devDependencies");
        List<DependencyVersionChange> expectedChanges = Arrays.asList(dependency1, dependency2);

        String command = Issues.createPendingChangesCommentCommand(expectedChanges);

        System.out.println("Generated pending comment command: " + command);

        List<DependencyVersionChange> changes = Issues.parseUpdateBotIssuePendingChangesComment(command);
        assertThat(changes).describedAs("Parsed changes " + changes).hasSize(2).isEqualTo(expectedChanges);
    }


}
