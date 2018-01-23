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
package io.fabric8.updatebot.kind.brew;

import io.fabric8.updatebot.commands.CommandContext;
import io.fabric8.updatebot.commands.PushVersionChangesContext;
import io.fabric8.updatebot.kind.KindDependenciesCheck;
import io.fabric8.updatebot.kind.Updater;
import io.fabric8.updatebot.kind.UpdaterSupport;
import io.fabric8.updatebot.model.Dependencies;
import io.fabric8.updatebot.model.DependencyVersionChange;
import io.fabric8.updatebot.support.FileHelper;
import io.fabric8.utils.Files;
import io.fabric8.utils.IOHelpers;
import io.fabric8.utils.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

/**
 */
public class BrewUpdater extends UpdaterSupport {
    private static final transient Logger LOG = LoggerFactory.getLogger(BrewUpdater.class);

    @Override
    public boolean isApplicable(CommandContext context) {
        return FileHelper.isDirectory(context.file("Formula"));
    }

    @Override
    public void addVersionChangesFromSource(CommandContext context, Dependencies dependencyConfig, List<DependencyVersionChange> list) throws IOException {
    }

    @Override
    public boolean pushVersions(CommandContext parentContext, List<DependencyVersionChange> changes) throws IOException {
        boolean answer = false;
        if (isApplicable(parentContext)) {
            for (DependencyVersionChange step : changes) {
                PushVersionChangesContext context = new PushVersionChangesContext(parentContext, step);
                boolean updated = pushVersions(context);
                if (updated) {
                    answer = true;
                } else {
                    parentContext.removeChild(context);
                }
            }
        }
        return answer;
    }

    protected boolean pushVersions(PushVersionChangesContext context) throws IOException {
        boolean answer = false;
        List<PushVersionChangesContext.Change> changes = context.getChanges();
        for (PushVersionChangesContext.Change change : changes) {
            if (doPushVersionChange(context, context.getName(), context.getValue())) {
                answer = true;
            }
        }
        DependencyVersionChange step = context.getStep();
        if (step != null) {
            if (doPushVersionChange(context, step.getDependency(), context.getValue())) {
                answer = true;
            }
        }
        return answer;
    }


    private boolean doPushVersionChange(PushVersionChangesContext context, String name, String value) throws IOException {
        Pattern re = Pattern.compile("\\s*version\\s+\"([^\"]+)\"");
        boolean answer = false;

        File dir = context.file("Formula");
        File rb = new File(dir, name + ".rb");

        if (Files.isFile(rb)) {
            String text = IOHelpers.readFully(rb);
            String updatedText = text.replaceAll("version\\s+\"([^\"]+)\"", "version \"" + value + "\"");
            if (!Objects.equal(text, updatedText)) {
                context.updatedVersion(name, name, value, null);
                answer = true;
            }
            if (answer) {
                IOHelpers.writeFully(rb, updatedText);
            }
/*
            List<String> lines = IOHelpers.readLines(rb);
            for (int i = 0, size = lines.size(); i < size; i++) {
                String line = lines.get(i);
                Matcher m = re.matcher(line);
                if (m.matches()) {
                    String old = m.group(1);
                    StringBuffer buffer = new StringBuffer();
                    m.appendReplacement(buffer, value);
                    String updatedText = buffer.toString();
                    if (!Objects.equal(line, updatedText)) {
                        lines.set(i, updatedText);
                        context.updatedVersion(name, name, value, old);
                        answer = true;
                    }
                    break;
                }
            }
            if (answer) {
                IOHelpers.writeLines(rb, lines);
            }
*/
        }
        return answer;
    }
}