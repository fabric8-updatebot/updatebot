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
package io.fabric8.updatebot.kind.plugins;

import io.fabric8.updatebot.commands.CommandContext;
import io.fabric8.updatebot.kind.Updater;
import io.fabric8.updatebot.kind.UpdaterSupport;
import io.fabric8.updatebot.model.Dependencies;
import io.fabric8.updatebot.model.DependencyVersionChange;
import io.fabric8.updatebot.model.GitRepositoryConfig;
import io.fabric8.updatebot.model.PluginsDependencies;
import io.fabric8.updatebot.support.FileMatcher;
import io.fabric8.updatebot.support.MarkupHelper;
import io.fabric8.utils.IOHelpers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Updates any <code>plugins.txt</code>` files with new jenkins plugin versions
 */
public class PluginsUpdater extends UpdaterSupport implements Updater {
    public static final String PLUGIN_DEPENDENCY_PREFIX = "org.jenkins-ci.plugins:";
    public static final String PLUGINS_SEPARATOR = ":";
    private static final transient Logger LOG = LoggerFactory.getLogger(PluginsUpdater.class);

    private PluginVersions pluginVersions;

    @Override
    public boolean isApplicable(CommandContext context) {
/*
        PluginsDependencies plugins = getPlugins(context);
        return plugins != null && !plugins.isEmpty();
*/
        return true;
    }


    @Override
    public void addVersionChangesFromSource(CommandContext context, Dependencies dependencyConfig, List<DependencyVersionChange> list) throws IOException {
    }

    @Override
    public boolean pullVersions(CommandContext context) throws IOException {
        boolean updated = false;
        PluginVersions newVersions = null;
        PluginsDependencies plugins = getPlugins(context);
        boolean hasMatcher = false;
        if (plugins != null) {
            FileMatcher fileMatcher = plugins.createFileMatcher();
            if (!plugins.isEmpty()) {
                hasMatcher = true;
            }
            List<File> files = fileMatcher.matchFiles(context.getDir());
            for (File file : files) {
                if (pullVersionsInFile(context, file, getPluginVersions(context))) {
                    updated = true;
                }
            }
        }
        if (!hasMatcher) {
            plugins = createDefaultPluginsDependencies();
            FileMatcher fileMatcher = plugins.createFileMatcher();
            List<File> files = fileMatcher.matchFiles(context.getDir());
            for (File file : files) {
                if (pullVersionsInFile(context, file, getPluginVersions(context))) {
                    updated = true;
                }
            }
        }
        return updated;

    }

    private PluginsDependencies createDefaultPluginsDependencies() {
        PluginsDependencies answer = new PluginsDependencies();
        answer.getIncludes().add("plugins.txt");
        return answer;
    }

    public PluginVersions getPluginVersions(CommandContext context) throws IOException {
        if (pluginVersions == null) {
            pluginVersions = loadNewPluginVersions(context);
            Set<Map.Entry<String, PluginVersion>> entries = pluginVersions.getPlugins().entrySet();
            for (Map.Entry<String, PluginVersion> entry : entries) {
                LOG.info("Plugin " + entry.getKey() + " version: " + entry.getValue().getVersion());
            }
        }
        return pluginVersions;
    }

    protected PluginVersions loadNewPluginVersions(CommandContext context) throws IOException {
        URL url = new URL("http://ftp-chi.osuosl.org/pub/jenkins/updates/current/update-center.actual.json");
        return MarkupHelper.loadJson(url, PluginVersions.class);
    }

    @Override
    public boolean pushVersions(CommandContext context, List<DependencyVersionChange> changes) throws IOException {
        boolean updated = false;
        PluginsDependencies plugins = getPlugins(context);
        if (plugins != null) {
            FileMatcher fileMatcher = plugins.createFileMatcher();
            List<File> files = fileMatcher.matchFiles(context.getDir());
            for (File file : files) {
                if (updateVersionsInFile(context, file, plugins, changes)) {
                    updated = true;
                }
            }
        }
        return updated;

    }

    private boolean pullVersionsInFile(CommandContext context, File file, PluginVersions pluginVersions) throws IOException {
        LOG.info("Processing file " + file);
        List<String> lines = IOHelpers.readLines(file);
        List<String> answer = new ArrayList<>(lines.size());

        boolean changed = false;
        for (String line : lines) {
            int idx = line.indexOf(PLUGINS_SEPARATOR);
            if (idx < 0) {
                answer.add(line);
                continue;
            }
            String artifactId = line.substring(0, idx);
            String currentVersion = line.substring(idx + 1).trim();
            if (!currentVersion.isEmpty()) {
                String newVersion = pluginVersions.getVersion(artifactId);
                if (newVersion == null) {
                    LOG.info("No new version for plugin " + artifactId);
                } else if (!newVersion.equals(currentVersion)) {
                    answer.add(artifactId + PLUGINS_SEPARATOR + newVersion);
                    changed = true;
                    continue;
                }
            }
            answer.add(line);
        }
        if (changed) {
            IOHelpers.writeLines(file, answer);
        }
        return changed;
    }


    private boolean updateVersionsInFile(CommandContext context, File file, PluginsDependencies plugins, List<DependencyVersionChange> changes) throws IOException {
        LOG.info("Processing file " + file);
        List<String> lines = IOHelpers.readLines(file);
        List<String> answer = new ArrayList<>(lines.size());

        Map<String, String> versionMap = new HashMap<>();
        for (DependencyVersionChange change : changes) {
            versionMap.put(change.getDependency(), change.getVersion());
        }

        boolean changed = false;
        for (String line : lines) {
            int idx = line.indexOf(PLUGINS_SEPARATOR);
            if (idx < 0) {
                answer.add(line);
                continue;
            }
            String artifactId = line.substring(0, idx);
            String newVersion = versionMap.get(PLUGIN_DEPENDENCY_PREFIX + artifactId);
            if (newVersion == null) {
                answer.add(line);
                continue;
            } else {
                answer.add(artifactId + PLUGINS_SEPARATOR + newVersion);
                changed = true;
            }
        }
        if (changed) {
            IOHelpers.writeLines(file, answer);
        }
        return changed;
    }


    protected PluginsDependencies getPlugins(CommandContext context) {
        PluginsDependencies plugins = null;
        GitRepositoryConfig details = context.getRepository().getRepo().getRepositoryDetails();
        if (details != null) {
            Dependencies push = details.getPush();
            if (push != null) {
                plugins = push.getPlugins();
            }
        }
        return plugins;
    }


}
