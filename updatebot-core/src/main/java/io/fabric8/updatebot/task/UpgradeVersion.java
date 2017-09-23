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
package io.fabric8.updatebot.task;

import io.fabric8.updatebot.UpdateBot;
import io.fabric8.updatebot.UpdateVersionContext;
import io.fabric8.updatebot.kind.Updater;
import io.fabric8.updatebot.repository.LocalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Upgrades a single version of a single dependency in downstream projects
 */
public class UpgradeVersion implements Operation {
    private static final transient Logger LOG = LoggerFactory.getLogger(UpgradeVersion.class);

    private final UpdateBot updateBot;
    private final Updater updater;
    private final String propertyName;
    private final String version;

    public UpgradeVersion(UpdateBot updateBot, Updater updater, String propertyName, String version) {
        this.updateBot = updateBot;
        this.updater = updater;
        this.propertyName = propertyName;
        this.version = version;
    }

    @Override
    public boolean apply(LocalRepository repository) throws IOException {
        LOG.info("Updating version of " + propertyName + " to " + version + " in " + repository.getDir());

        UpdateVersionContext context = new UpdateVersionContext(repository, propertyName, version);

        if (updater.isApplicable(context)) {
            return updater.updateVersion(context);
        }
        return false;
    }
}
