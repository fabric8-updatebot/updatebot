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
package org.slf4j.impl;

import java.io.PrintStream;

/**
 * Replaces the simple logging configuration with a custom stream
 */
public class UpdateBotLogConfiguration extends SimpleLoggerConfiguration {
    private final PrintStream out;

    public UpdateBotLogConfiguration(PrintStream out) {
        this.out = out;
    }

    public void init() {
        // lets set the initialised flag so we don't get replaced later on
        SimpleLogger.lazyInit();
        SimpleLogger.CONFIG_PARAMS = this;
        super.init();
        this.outputChoice = new OutputChoice(out);
        this.showThreadName = false;
    }
}
