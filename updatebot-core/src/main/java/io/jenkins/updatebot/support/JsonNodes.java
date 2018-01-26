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
package io.jenkins.updatebot.support;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Helper assertions for dealing with JSON or YAML markup
 */
public class JsonNodes {

    public static JsonNode path(JsonNode tree, String... paths) {
        JsonNode node = tree;
        for (String path : paths) {
            if (node == null || !node.isObject()) {
                return null;
            }
            node = node.get(path);
        }
        return node;
    }

    /**
     * Returns the text value of the given field on an object or null if its not a value or the value is not a string
     */
    public static String textValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value != null && value.isTextual()) {
            return value.textValue();
        }
        return null;
    }
}
