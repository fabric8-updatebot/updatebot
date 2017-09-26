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
package io.fabric8.updatebot.test;

import io.fabric8.updatebot.Configuration;
import io.fabric8.updatebot.UpdateBot;
import io.fabric8.updatebot.commands.CommandSupport;
import io.fabric8.updatebot.commands.CompositeCommand;
import io.fabric8.updatebot.commands.PushVersionChanges;
import io.fabric8.updatebot.kind.Kind;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 */
public class CommandAssertions {

    /**
     * Asserts we can parse the given comand arguments to a command of the given class
     *
     * @return the parsed command
     */
    public static <T extends CommandSupport> T assertParseCommand(Class<T> clazz, String... args) {
        CommandSupport command = UpdateBot.parseCommand(args, new Configuration(), false);
        assertThat(command).isInstanceOf(clazz);
        return clazz.cast(command);
    }

    /**
     * Asserts that the child at the given index is a {@link PushVersionChanges} command
     *
     * @return the command
     */
    public static PushVersionChanges assertChildIsPushVersionChanges(CompositeCommand commands, int index) {
        return assertCommandIsA(commands, index, PushVersionChanges.class);
    }

    public static <T extends CommandSupport> T assertCommandIsA(CompositeCommand commands, int index, Class<T> clazz) {
        List<CommandSupport> children = commands.getCommands();
        assertThat(children.size()).describedAs("command count").isGreaterThan(index);
        CommandSupport command = children.get(index);
        assertThat(command).isInstanceOf(clazz);
        return clazz.cast(command);
    }

    public static void assertPushVersionContext(PushVersionChanges context, Kind kind, String... arguments) {
        assertThat(context.getKind()).describedAs("kind").isEqualTo(kind);
        List<String> argumentList = Arrays.asList(arguments);
        assertThat(context.getValues()).describedAs("values").isEqualTo(argumentList);
    }
}
