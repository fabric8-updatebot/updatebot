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

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

/**
 * Helper assertions for dealing with JSON or YAML markup
 */
public class MarkupAssertions {
    public static String assertTextValue(JsonNode tree, String... paths) {
        JsonNode node = assertPath(tree, paths);
        String pathText = Arrays.asList(paths).toString();

        assertThat(node).describedAs("Could not find value for path " + pathText).isNotNull();
        assertThat(node.isTextual()).describedAs("Value of path " + pathText + " is not textual but is " + node).isTrue();

        return node.asText();
    }

    public static JsonNode assertPath(JsonNode tree, String... paths) {
        JsonNode node = tree;
        for (String path : paths) {
            assertThat(node.isObject()).
                    describedAs("cannot access property " + path + " as the node is not an object " + node).
                    isTrue();

            JsonNode value = node.get(path);
            assertThat(value).
                    describedAs("property " + path + " should exist on " + node).
                    isNotNull();
            node = value;
        }
        return node;
    }
}
