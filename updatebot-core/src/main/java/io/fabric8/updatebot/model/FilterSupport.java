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
package io.fabric8.updatebot.model;

import io.fabric8.updatebot.support.FilterHelpers;
import io.fabric8.utils.Filter;
import io.fabric8.utils.Filters;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class FilterSupport extends DtoSupport {
    private List<String> includes = new ArrayList<>();
    private List<String> excludes = new ArrayList<>();

    public void include(String... values) {
        addValues(this.includes, values);
    }

    public void exclude(String... values) {
        addValues(this.excludes, values);
    }

    public List<String> getIncludes() {
        return includes;
    }

    public void setIncludes(List<String> includes) {
        this.includes = includes;
    }

    public List<String> getExcludes() {
        return excludes;
    }

    public void setExcludes(List<String> excludes) {
        this.excludes = excludes;
    }

    /**
     * Returns a filter for the names
     */
    public Filter<String> createFilter() {
        Filter<String> includeFilter = Filters.createStringFilters(includes);
        if (excludes.isEmpty()) {
            if (includes.isEmpty()) {
                return Filters.falseFilter();
            }
            return includeFilter;
        }
        Filter<String> excludeFilter = Filters.createStringFilters(excludes);
        if (includes.isEmpty()) {
            return excludeFilter;
        }
        return FilterHelpers.and(includeFilter, Filters.not(excludeFilter));
    }

    protected void addValues(List<String> list, String[] values) {
        for (String value : values) {
            if (!list.contains(value)) {
                list.add(value);
            }
        }
    }
}
