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
package io.jenkins.updatebot.kind.plugins;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.jenkins.updatebot.model.DtoSupport;

/**
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PluginVersion extends DtoSupport {
    private String url;
    private String version;
    private String wiki;
    private String title;
    private String scm;

    @Override
    public String toString() {
        return "PluginVersion{" +
                "url='" + url + '\'' +
                ", version='" + version + '\'' +
                ", wiki='" + wiki + '\'' +
                ", title='" + title + '\'' +
                ", scm='" + scm + '\'' +
                '}';
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getWiki() {
        return wiki;
    }

    public void setWiki(String wiki) {
        this.wiki = wiki;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getScm() {
        return scm;
    }

    public void setScm(String scm) {
        this.scm = scm;
    }
}
