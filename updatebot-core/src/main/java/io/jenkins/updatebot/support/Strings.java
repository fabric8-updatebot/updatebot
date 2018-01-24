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

import io.fabric8.utils.Objects;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class Strings {
    /**
     * Returns true if the string is null or empty
     */
    public static boolean empty(String text) {
        return text == null || text.isEmpty();
    }

    /**
     * Returns true if the string is not null and not empty
     */
    public static boolean notEmpty(String text) {
        return text != null && !text.isEmpty();
    }

    /**
     * Returns true if the actual value matches any of the String representations of the given values.
     * <p>
     * So can match against String or URL objects etc
     */
    public static boolean equalAnyValue(String actual, Object... values) {
        for (Object value : values) {
            if (value != null) {
                String text = value.toString();
                if (Objects.equal(text, actual)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns true if the actual value matches any of the String representations of the given values.
     * <p>
     * So can match against String or URL objects etc
     */
    public static boolean equalAnyValue(String actual, Iterable<?> values) {
        for (Object value : values) {
            if (value != null) {
                String text = value.toString();
                if (Objects.equal(text, actual)) {
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * Trim all prefixes on the given text of the given prefix
     */
    public static String trimAllPrefix(String text, String prefix) {
        String answer = text;
        while (answer.startsWith(prefix)) {
            answer = answer.substring(prefix.length());
        }
        return answer;
    }

    public static String toString(Object value) {
        if (value != null) {
            return value.toString();
        }
        return null;
    }

    /**
     * Returns a joined string with the separator for all the non-empty strings
     */
    public static String joinNotEmpty(String sep, String... values) {
        List<String> list = new ArrayList<>();
        for (String value : values) {
            if (Strings.notEmpty(value)) {
                list.add(value);
            }
        }
        return String.join(sep, list);
    }
}