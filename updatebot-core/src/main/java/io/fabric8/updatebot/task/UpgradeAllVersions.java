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

import io.fabric8.updatebot.repository.LocalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class UpgradeAllVersions implements Operation {
    private static final transient Logger LOG = LoggerFactory.getLogger(UpgradeAllVersions.class);

    @Override
    public boolean apply(LocalRepository repository) {
        LOG.info("Upgrading all versions in " + repository.getDir());

        return false;
    }
}
