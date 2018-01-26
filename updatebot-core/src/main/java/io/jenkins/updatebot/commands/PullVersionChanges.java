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
package io.jenkins.updatebot.commands;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import io.jenkins.updatebot.CommandNames;
import io.jenkins.updatebot.kind.CompositeUpdater;
import io.jenkins.updatebot.kind.Kind;
import io.jenkins.updatebot.kind.Updater;
import io.jenkins.updatebot.repository.LocalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Pulls updates to your projects from upstream artifact repositories like npm or maven central
 */
@Parameters(commandNames = CommandNames.PULL, commandDescription = "Pulls version changes into your projects. " +
        "Lets you periodically query all the dependencies for all your projects and pull any upstream releases into your projects")
public class PullVersionChanges extends ModifyFilesCommandSupport {
    private static final transient Logger LOG = LoggerFactory.getLogger(PullVersionChanges.class);

    @Parameter(order = 0, names = {"--kind", "-k"}, description = "The kind of property to replace based on the kind of language or build tool. If not specified then all supported languages and build tools will be updated")
    private Kind kind;

    private Updater updater;

    @Override
    protected boolean doProcess(CommandContext context) throws IOException {
        LocalRepository repository = context.getRepository();
        LOG.debug("Pulling version changes into: " + repository.getDir() + " repo: " + repository.getCloneUrl());

        return getUpdater().pullVersions(context);
    }

    protected Updater getUpdater() {
        if (updater == null) {
            if (kind != null) {
                updater = kind.getUpdater();
            } else {
                updater = new CompositeUpdater();
            }
        }
        return updater;
    }
}
