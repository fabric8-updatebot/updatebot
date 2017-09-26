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

import io.fabric8.updatebot.Configuration;
import io.fabric8.updatebot.commands.CommandContext;
import io.fabric8.updatebot.commands.CommandSupport;
import io.fabric8.updatebot.commands.CompositeCommand;
import io.fabric8.updatebot.commands.PushVersionChanges;
import io.fabric8.updatebot.commands.UpdatePullRequests;
import io.fabric8.updatebot.repository.LocalRepository;
import io.fabric8.updatebot.test.CommandAssertions;
import io.fabric8.updatebot.test.Tests;
import io.fabric8.utils.Strings;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static io.fabric8.updatebot.CommandNames.PUSH;
import static io.fabric8.updatebot.kind.Kind.NPM;
import static io.fabric8.updatebot.support.PullRequests.COMMAND_COMMENT_INDENT;
import static io.fabric8.updatebot.support.PullRequests.COMMAND_COMMENT_PREFIX;
import static io.fabric8.updatebot.support.PullRequests.COMMAND_COMMENT_PREFIX_SEPARATOR;
import static org.assertj.core.api.Assertions.assertThat;

/**
 */
public class CommentReadWriteTest {
    protected File testDir = Tests.getTestDataDir(getClass());
    protected CommandContext parentContext;
    protected UpdatePullRequests updatePullRequests = new UpdatePullRequests();
    protected Configuration configuration = new Configuration();

    public static void assertContextCommandComment(CommandSupport command, String expectedComment) {
        String comment = command.createPullRequestComment();

        String fullExpectedComment = COMMAND_COMMENT_PREFIX + COMMAND_COMMENT_PREFIX_SEPARATOR + expectedComment + "\n";
        assertThat(comment).describedAs("context command comment").isEqualTo(fullExpectedComment);

    }

    @Before
    public void init() {
        parentContext = new CommandContext(LocalRepository.fromDirectory(testDir), configuration);
    }

    @Test
    public void testSingleUpdate() throws Exception {
        String dependency = "cheese";
        String version = "1.2.3";

        PushVersionChanges command = new PushVersionChanges(NPM, dependency, version);
        parentContext.updateVersion(NPM, dependency, version);
        assertContextCommandComment(command,
                COMMAND_COMMENT_INDENT + Strings.join(" ", PUSH, "--kind", NPM, dependency, version));


        String comment = command.createPullRequestComment();
        CompositeCommand parsedCommands = parseCommandComment(comment, 1);
        PushVersionChanges parsedUpdateVersion = CommandAssertions.assertChildIsPushVersionChanges(parsedCommands, 0);
        CommandAssertions.assertPushVersionContext(parsedUpdateVersion, NPM, dependency, version);
    }

    @Test
    public void testTwoUpdates() throws Exception {
        String dependency1 = "beer";
        String version1 = "2.3.4";

        String dependency2 = "wine";
        String version2 = "4.5.6";

        PushVersionChanges command = new PushVersionChanges(NPM, dependency1, version1, dependency2, version2);

        assertContextCommandComment(command,
                COMMAND_COMMENT_INDENT + Strings.join(" ", PUSH, "--kind", NPM, dependency1, version1, dependency2, version2));

        String comment = command.createPullRequestComment();
        CompositeCommand parsedCommands = parseCommandComment(comment, 1);
        PushVersionChanges parsedUpdateVersion1 = CommandAssertions.assertChildIsPushVersionChanges(parsedCommands, 0);
        CommandAssertions.assertPushVersionContext(parsedUpdateVersion1, NPM, dependency1, version1, dependency2, version2);
    }


    protected CompositeCommand parseCommandComment(String comment, int expectedChildren) {
        CompositeCommand answer = updatePullRequests.parseUpdateBotCommandComment(parentContext, comment);
        assertThat(answer).describedAs("parsed context").isNotNull();
        assertThat(answer.getCommands()).describedAs("commands").isNotEmpty().hasSize(expectedChildren);
        return answer;
    }


}
