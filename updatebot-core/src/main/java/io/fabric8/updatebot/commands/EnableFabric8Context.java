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

import io.fabric8.updatebot.Configuration;
import io.fabric8.updatebot.repository.LocalRepository;

/**
 */
public class EnableFabric8Context extends CommandContext {
    private final LocalRepository jenkinsfileRepository;

    public EnableFabric8Context(LocalRepository repository, Configuration configuration, LocalRepository jenkinsfileRepository) {
        super(repository, configuration);
        this.jenkinsfileRepository = jenkinsfileRepository;
    }

    public LocalRepository getJenkinsfileRepository() {
        return jenkinsfileRepository;
    }

    @Override
    public String createCommit() {
        return "enable fabric8 CI / CD";
    }

    @Override
    public String createPullRequestTitle() {
        return "enable fabric8 CI / CD";
    }
}
