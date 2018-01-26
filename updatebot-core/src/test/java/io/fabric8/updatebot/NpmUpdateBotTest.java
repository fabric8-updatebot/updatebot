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
package io.fabric8.updatebot;


import io.jenkins.updatebot.Configuration;
import io.jenkins.updatebot.commands.PushVersionChanges;
import io.jenkins.updatebot.kind.Kind;
import io.fabric8.updatebot.test.Tests;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

/**
 */
public class NpmUpdateBotTest {
    protected PushVersionChanges updateBot = new PushVersionChanges();
    private Configuration configuration = new Configuration();

    @Before
    public void init() {
        File testClasses = new File(Tests.getBasedir(), "src/test/resources/npm/updatebot.yml");
        configuration.setConfigFile(testClasses.getPath());
        configuration.setDryRun(true);

        // lets update a single version
        updateBot.setKind(Kind.NPM);
        updateBot.values("@angular/core", "4.3.7");
    }

    @Test
    public void testUpdater() throws Exception {
        updateBot.run(configuration);
    }

}
