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
package io.fabric8.updatebot.commands;

import io.fabric8.updatebot.repository.LocalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 */
public class UpdateAllVersions extends UpdateBotCommand {
    private static final transient Logger LOG = LoggerFactory.getLogger(UpdateAllVersions.class);

    @Override
    protected boolean doProcess(LocalRepository repository) throws IOException {
        LOG.debug("Updating al version in: " + repository.getDir() + " repo: " + repository.getCloneUrl());
        return false;
    }
}
