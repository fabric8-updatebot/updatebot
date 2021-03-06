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
package io.fabric8.updatebot.support;

import io.fabric8.utils.Filter;

import java.util.Arrays;

/**
 */
public class FilterHelpers {
    public static <T> Filter<T> and(final Filter<T>... filters) {
        return new Filter<T>() {
            @Override
            public boolean matches(T t) {
                for (Filter<T> filter : filters) {
                    if (!filter.matches(t)) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public String toString() {
                return "AndFilter" + Arrays.asList(filters);
            }
        };
    }
}
