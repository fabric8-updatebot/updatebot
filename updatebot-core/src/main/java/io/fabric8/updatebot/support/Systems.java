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
package io.fabric8.updatebot.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class Systems {
    private static final transient Logger LOG = LoggerFactory.getLogger(Systems.class);

    public static String getConfigValue(String envVar) {
        return getConfigValue(envVar, null);
    }

    /**
     * Returns the value of the given environment variable as a system property or environment variable or returns the default value
     */
    public static String getConfigValue(String envVar, String defaultValue) {
        String systemProperty = envVar.toLowerCase().replace('_', '.');
        String answer = System.getProperty(systemProperty);
        if (Strings.notEmpty(answer)) {
            return answer;
        }
        try {
            answer = System.getenv(envVar);
        } catch (Exception e) {
            LOG.warn("Failed to look up environment variable $" + envVar + ". " + e, e);
        }
        if (Strings.notEmpty(answer)) {
            return answer;
        } else {
            return defaultValue;
        }
    }
}
