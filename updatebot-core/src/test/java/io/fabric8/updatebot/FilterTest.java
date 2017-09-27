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

import io.fabric8.updatebot.model.GithubOrganisation;
import io.fabric8.utils.Filter;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 */
public class FilterTest {
    GithubOrganisation organisation = new GithubOrganisation();

    @Test
    public void testIncludesAndExcludes() throws Exception {
        organisation.include("foo-*", "bar");
        organisation.exclude("cheese", "foo-x*");

        assertFilter(true, organisation, "foo-", "foo-bar");
        assertFilter(false, organisation, "cheese", "foo-x");
    }

    @Test
    public void testIncludeOnly() throws Exception {
        organisation.include("spring-boot-camel*");

        assertFilter(true, organisation, "spring-boot-camel", "spring-boot-camel-xml");
        assertFilter(false, organisation, "spring-boot-amq", "vertx");
    }

    private void assertFilter(boolean expected, GithubOrganisation organisation, String... values) {
        Filter<String> filter = organisation.createFilter();
        for (String value : values) {
            boolean actual = filter.matches(value);
            assertThat(actual).
                    describedAs("Filter " + filter + " from includes " + organisation.getIncludes() + " excludes: " + organisation.getExcludes()).
                    isEqualTo(expected);
        }
    }
}
