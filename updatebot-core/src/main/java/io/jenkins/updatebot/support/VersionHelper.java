/**
 * Copyright 2005-2015 Red Hat, Inc.
 * <p>
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.jenkins.updatebot.support;

import io.fabric8.utils.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public final class VersionHelper {
    private static final transient Logger LOG = LoggerFactory.getLogger(VersionHelper.class);

    private static Map<String, String> groupArtifactVersionMap;


    /**
     * Returns the version of updateBot to use
     */
    public static String updateBotVersion() {
        return getVersion("io.fabric8.updatebot", "updatebot-core");
    }

    /**
     * Retrieves the version of fabric8 maven plugin to use
     */
    public static String fabric8MavenPluginVersion() {
        return getVersion("io.fabric8", "fabric8-maven-plugin");
    }

    /**
     * Retrieves the version of fabric8 maven plugin to use
     */
    public static String fabric8Version() {
        return getVersion("io.fabric8", "kubernetes-api");
    }

    public static String getVersion(String groupId, String artifactId) {
        String key = "" + groupId + "/" + artifactId;
        Map map = getGroupArtifactVersionMap();
        String version = (String) map.get(key);
        if (version == null) {
            LOG.warn("Could not find the version for groupId: " + groupId + " artifactId: " + artifactId + " in: " + map);
        }
        return version;
    }

    public static String getVersion(String groupId, String artifactId, String defaultVersion) {
        String answer = getVersion(groupId, artifactId);
        if (Strings.isNullOrBlank(answer)) {
            answer = defaultVersion;
        }

        return answer;
    }

    protected static Map<String, String> getGroupArtifactVersionMap() {
        if (groupArtifactVersionMap == null) {
            groupArtifactVersionMap = new HashMap();
            InputStream in = VersionHelper.class.getResourceAsStream("versions.properties");
            if (in == null) {
                LOG.warn("Could not find versions.properties on the classpath!");
            } else {
                Properties properties = new Properties();

                try {
                    properties.load(in);
                } catch (IOException var7) {
                    throw new RuntimeException("Failed to load versions.properties: " + var7, var7);
                }

                Set entries = properties.entrySet();
                Iterator var3 = entries.iterator();

                while (var3.hasNext()) {
                    Map.Entry entry = (Map.Entry) var3.next();
                    Object key = entry.getKey();
                    Object value = entry.getValue();
                    if (key != null && value != null) {
                        groupArtifactVersionMap.put(key.toString(), value.toString());
                    }
                }
            }
        }

        return groupArtifactVersionMap;
    }

    public static String after(String text, String after) {
        if (!text.contains(after)) {
            return null;
        }
        return text.substring(text.indexOf(after) + after.length());
    }

    public static String before(String text, String before) {
        if (!text.contains(before)) {
            return null;
        }
        return text.substring(0, text.indexOf(before));
    }

    public static String between(String text, String after, String before) {
        text = after(text, after);
        if (text == null) {
            return null;
        }
        return before(text, before);
    }

}
