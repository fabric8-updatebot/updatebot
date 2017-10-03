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
import io.fabric8.updatebot.support.Strings;
import io.fabric8.utils.Filter;
import io.fabric8.utils.Filters;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class MavenDependencyFilter {
    private String groupInclude;
    private String groupExclude;
    private String artifactInclude;
    private String artifactExclude;

    protected static Filter<String> createStringFilter(String include, String exclude) {
        Filter<String> includeFilter = null;
        Filter<String> excludeFilter = null;
        if (Strings.notEmpty(include)) {
            includeFilter = Filters.createStringFilter(include);
        }
        if (Strings.notEmpty(exclude)) {
            excludeFilter = Filters.not(Filters.createStringFilter(exclude));
        }
        if (includeFilter == null && excludeFilter == null) {
            return Filters.trueFilter();
        }
        if (includeFilter == null) {
            return excludeFilter;
        }
        if (excludeFilter == null) {
            return includeFilter;
        }
        return FilterHelpers.and(includeFilter, excludeFilter);
    }

    public static Filter<MavenArtifactKey> createFilter(List<MavenDependencyFilter> dependencies) {
        List<Filter<MavenArtifactKey>> filters = new ArrayList<>();
        for (MavenDependencyFilter dependency : dependencies) {
            Filter<MavenArtifactKey> filter = dependency.createFilter();
            filters.add(filter);
        }
        if (filters.isEmpty()) {
            // for empty filters lets not match anything!
            return Filters.falseFilter();
        }
        return Filters.or(filters);
    }

    public String getGroupInclude() {
        return groupInclude;
    }

    public void setGroupInclude(String groupInclude) {
        this.groupInclude = groupInclude;
    }

    public String getGroupExclude() {
        return groupExclude;
    }

    public void setGroupExclude(String groupExclude) {
        this.groupExclude = groupExclude;
    }

    public String getArtifactInclude() {
        return artifactInclude;
    }

    public void setArtifactInclude(String artifactInclude) {
        this.artifactInclude = artifactInclude;
    }

    public String getArtifactExclude() {
        return artifactExclude;
    }

    public void setArtifactExclude(String artifactExclude) {
        this.artifactExclude = artifactExclude;
    }

    public Filter<MavenArtifactKey> createFilter() {
        final Filter<String> groupFilter = createStringFilter(groupInclude, groupExclude);
        final Filter<String> artifactFilter = createStringFilter(artifactInclude, artifactExclude);

        final MavenDependencyFilter that = this;
        return new Filter<MavenArtifactKey>() {

            @Override
            public String toString() {
                return "Filter(" + that + ")";
            }

            @Override
            public boolean matches(MavenArtifactKey mavenArtifactKey) {
                return groupFilter.matches(mavenArtifactKey.getGroupId()) && artifactFilter.matches(mavenArtifactKey.getArtifactId());
            }
        };
    }
}
