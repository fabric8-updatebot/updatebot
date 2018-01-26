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
package io.fabric8.updatebot;

import io.jenkins.updatebot.CommandNames;
import io.jenkins.updatebot.commands.PushVersionChanges;
import io.fabric8.updatebot.test.CommandAssertions;
import org.junit.Test;

import static io.jenkins.updatebot.kind.Kind.NPM;
import static io.fabric8.updatebot.test.CommandAssertions.assertParseCommand;

/**
 */
public class CommandParseTest {

    @Test
    public void testParseSimpleCommand() throws Exception {
        String dependency = "cheese";
        String version = "1.2.3";
        PushVersionChanges command = assertParseCommand(PushVersionChanges.class, CommandNames.PUSH_VERSION, "--kind", "npm", dependency, version);
        CommandAssertions.assertPushVersionContext(command, NPM, dependency, version);
    }

    @Test
    public void testParseDependencyWithAt() throws Exception {
        String dependency = "@angular/core";
        String version = "4.3.2";
        PushVersionChanges command = assertParseCommand(PushVersionChanges.class, CommandNames.PUSH_VERSION, "--kind", "npm", dependency, version);
        CommandAssertions.assertPushVersionContext(command, NPM, dependency, version);
    }

}
