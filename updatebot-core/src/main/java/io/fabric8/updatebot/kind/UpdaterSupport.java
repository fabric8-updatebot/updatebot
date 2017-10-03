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
package io.fabric8.updatebot.kind;

import io.fabric8.updatebot.commands.CommandContext;
import io.fabric8.updatebot.commands.PushVersionChangesContext;
import io.fabric8.updatebot.kind.Updater;
import io.fabric8.updatebot.model.DependencyVersionChange;

import java.io.IOException;
import java.util.List;

/**
 * A useful base class for implementing {@link Updater}
 */
public abstract class UpdaterSupport implements Updater {

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
}
