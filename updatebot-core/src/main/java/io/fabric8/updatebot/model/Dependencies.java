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

import io.fabric8.updatebot.support.Strings;

/**
 */
public class Dependencies {
    private NpmDependencies npm;
    private MavenDependencies maven;
    private FileDependencies file;
    private PluginsDependencies plugins;

    @Override
    public String toString() {
        String mavenText = "maven=" + maven;
        String npmText = "npm=" + npm;
        return "Dependencies{" +
                Strings.joinNotEmpty(", ", mavenText, npmText) + '}';
    }

    public NpmDependencies getNpm() {
        return npm;
    }

    public void setNpm(NpmDependencies npm) {
        this.npm = npm;
    }

    public MavenDependencies getMaven() {
        return maven;
    }

    public void setMaven(MavenDependencies maven) {
        this.maven = maven;
    }

    public FileDependencies getFile() {
        return file;
    }

    public void setFile(FileDependencies file) {
        this.file = file;
    }

    public PluginsDependencies getPlugins() {
        return plugins;
    }

    public void setPlugins(PluginsDependencies plugins) {
        this.plugins = plugins;
    }
}
