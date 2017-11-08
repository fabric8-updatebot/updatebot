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
package io.fabric8.updatebot.kind.helm;

import io.fabric8.updatebot.commands.CommandContext;
import io.fabric8.updatebot.kind.Kind;
import io.fabric8.updatebot.kind.UpdaterSupport;
import io.fabric8.updatebot.kind.helm.model.Chart;
import io.fabric8.updatebot.kind.helm.model.Requirements;
import io.fabric8.updatebot.model.Dependencies;
import io.fabric8.updatebot.model.DependencyVersionChange;
import io.fabric8.updatebot.support.MarkupHelper;
import io.fabric8.updatebot.support.Strings;
import io.fabric8.utils.Files;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static io.fabric8.updatebot.kind.helm.HelmFiles.CHART_YAML;
import static io.fabric8.updatebot.kind.helm.HelmFiles.REQUIREMENTS_YAML;
import static io.fabric8.updatebot.support.FileHelper.isFile;

/**
 */
public class HelmUpdater extends UpdaterSupport {

    public static boolean applyRequirementsChanges(CommandContext context, List<DependencyVersionChange> changes, Requirements requirements, File requirementsFile) throws IOException {
        boolean answer = requirements.applyChanges(changes);
        if (answer) {
            // lets store the updated requirements
            try {
                MarkupHelper.saveYaml(requirements, requirementsFile);
            } catch (IOException e) {
                throw new IOException("Failed to save chart requirements " + requirementsFile + ". " + e, e);
            }
        }
        return answer;
    }

    @Override
    public boolean isApplicable(CommandContext context) {
        boolean answer = isFile(context.file(CHART_YAML));
        return answer;
    }

    /**
     * Lets find changes from a local release build of a chart and push them into dependent projects
     *
     * @param context
     * @param dependencyConfig
     * @param list
     * @throws IOException
     */
    @Override
    public void addVersionChangesFromSource(CommandContext context, Dependencies dependencyConfig, List<DependencyVersionChange> list) throws IOException {
        File file = context.file(CHART_YAML);
        if (isFile(file)) {
            Chart chart;
            try {
                chart = MarkupHelper.loadYaml(file, Chart.class);
            } catch (IOException e) {
                throw new IOException("Failed to load chart " + file + ". " + e, e);
            }
            if (chart != null) {
                String name = chart.getName();
                String version = chart.getVersion();
                if (Strings.notEmpty(name) && Strings.notEmpty(version)) {
                    list.add(new DependencyVersionChange(Kind.HELM, name, version));
                }
            }
        }
    }

    @Override
    public boolean pushVersions(CommandContext context, List<DependencyVersionChange> changes) throws IOException {
        return pushVersionsForDir(context, changes, context.getDir());
    }

    protected boolean pushVersionsForDir(CommandContext context, List<DependencyVersionChange> changes, File dir) throws IOException {
        File chartsFile = new File(dir, CHART_YAML);
        boolean answer = false;
        if (isFile(chartsFile)) {
            File requirementsFile = new File(dir, REQUIREMENTS_YAML);
            if (isFile(requirementsFile)) {
                Requirements requirements;
                try {
                    requirements = MarkupHelper.loadYaml(requirementsFile, Requirements.class);
                } catch (IOException e) {
                    throw new IOException("Failed to load chart requirements " + requirementsFile + ". " + e, e);
                }
                if (requirements != null) {
                    if (applyRequirementsChanges(context, changes, requirements, requirementsFile)) {
                        answer = true;
                    }
                }
            }
        } else {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (Files.isDirectory(file)) {
                        if (pushVersionsForDir(context, changes, file)) {
                            answer = true;
                        }
                    }
                }
            }
        }
        return answer;
    }
}
