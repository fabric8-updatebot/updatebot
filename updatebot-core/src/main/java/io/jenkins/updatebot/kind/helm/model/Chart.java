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
package io.jenkins.updatebot.kind.helm.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Represents the <a href="https://github.com/kubernetes/helm">Helm</a>
 * <a href="https://github.com/kubernetes/helm/blob/master/pkg/proto/hapi/chart/metadata.pb.go#L50">Chart.yaml file</a>
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Chart {
    @JsonProperty
    boolean deprecated;
    @JsonProperty
    private String name;
    @JsonProperty
    private String home;
    @JsonProperty
    private List<String> sources;
    @JsonProperty
    private String version;
    @JsonProperty
    private String description;
    @JsonProperty
    private List<String> keywords;
    @JsonProperty
    private List<Maintainer> maintainers;
    @JsonProperty
    private String engine;
    @JsonProperty
    private String icon;
    @JsonProperty
    private String appVersion;

    public Chart() {
    }

    public Chart(String name, String version) {
        this.name = name;
        this.version = version;
    }


    @Override
    public String toString() {
        return "Chart{" +
                "name='" + name + '\'' +
                ", home='" + home + '\'' +
                ", version='" + version + '\'' +
                '}';
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHome() {
        return home;
    }

    public void setHome(String home) {
        this.home = home;
    }

    public List<String> getSources() {
        return sources;
    }

    public void setSources(List<String> sources) {
        this.sources = sources;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public List<Maintainer> getMaintainers() {
        return maintainers;
    }

    public void setMaintainers(List<Maintainer> maintainers) {
        this.maintainers = maintainers;
    }

    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }
}
