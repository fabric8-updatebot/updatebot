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
package io.jenkins.updatebot.kind.maven;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import io.jenkins.updatebot.model.DependencyVersionChange;
import io.jenkins.updatebot.model.MavenArtifactKey;
import io.jenkins.updatebot.support.DecentXmlHelper;
import io.jenkins.updatebot.support.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static io.jenkins.updatebot.kind.maven.MavenDependencyVersionChange.elementProcessor;
import static io.jenkins.updatebot.model.MavenArtifactKey.fromString;

/**
 */
public class PomHelper {
    private static final transient Logger LOG = LoggerFactory.getLogger(PomHelper.class);


    public static boolean updatePomVersionsInPoms(File dir, List<DependencyVersionChange> changes) throws IOException {
        List<PomUpdateStatus> pomsToChange = new ArrayList<>();
        addPomFiles(dir, pomsToChange);
        return updatePomVersions(pomsToChange, changes);
    }

    public static boolean updatePomVersions(List<PomUpdateStatus> pomsToChange, List<DependencyVersionChange> changes) throws IOException {
        Map<String, String> propertyChanges = new TreeMap<>();
        for (PomUpdateStatus status : pomsToChange) {
            status.updateVersions(changes, propertyChanges);
        }

        if (!propertyChanges.isEmpty()) {
            for (PomUpdateStatus status : pomsToChange) {
                status.updateProperties(propertyChanges);
            }
        }
        boolean answer = false;
        for (PomUpdateStatus status : pomsToChange) {
            if (status.saveIfChanged()) {
                answer = true;
            }
        }
        return answer;
    }

    protected static void addPomFiles(File file, List<PomUpdateStatus> pomsToChange) {
        if (file.isFile()) {
            if (file.getName().equals("pom.xml")) {
                try {
                    PomUpdateStatus updateStatus = PomUpdateStatus.createPomUpdateStatus(file);
                    if (pomsToChange.isEmpty()) {
                        updateStatus.setRootPom(true);
                    }
                    pomsToChange.add(updateStatus);
                } catch (Exception e) {
                    LOG.warn("Failed to parse " + file + ". " + e, e);
                }
            }
        } else if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    addPomFiles(child, pomsToChange);
                }
            }
        }
    }


    public static boolean updatePluginVersion(Document doc, DependencyVersionChange change, Map<String, String> propertyChanges, boolean lazyAdd) {
        Element rootElement = doc.getRootElement();
        List<Element> plugins = DecentXmlHelper.findElementsWithName(rootElement, "plugin");

        boolean found = false;
        String newVersion = change.getVersion();
        boolean update = false;
        for (Element element : plugins) {
            String groupId = DecentXmlHelper.firstChildTextContent(element, "groupId");
            String artifactId = DecentXmlHelper.firstChildTextContent(element, "artifactId");
            if (change.matches(groupId, artifactId)) {
                found = true;
                String version = DecentXmlHelper.firstChildTextContent(element, "version");
                if (Strings.notEmpty(version)) {
                    if (version.startsWith("${") && version.endsWith("}")) {
                        String versionProperty = version.substring(2, version.length() - 1);
                        propertyChanges.put(versionProperty, newVersion);
                    } else {
                        if (DecentXmlHelper.updateFirstChild(element, "version", newVersion)) {
                            update = true;
                        }
                    }
                }
            }
        }
        if (lazyAdd && !found) {
            MavenArtifactKey key = fromString(change.getDependency());
            // lets add the plugin
            // lets add a new fmp plugin element
            String separator = "\n";
            Element build = DecentXmlHelper.getOrCreateChild(rootElement, "build", separator);
            separator += "  ";
            Element newPlugins = DecentXmlHelper.getOrCreateChild(build, "plugins", separator);
            separator += "  ";
            Element plugin = DecentXmlHelper.createChild(newPlugins, "plugin", separator);
            separator += "  ";
            DecentXmlHelper.addText(plugin, separator);
            DecentXmlHelper.addChildElement(plugin, "groupId", key.getGroupId());
            DecentXmlHelper.addText(plugin, separator);
            DecentXmlHelper.addChildElement(plugin, "artifactId", key.getArtifactId());
            DecentXmlHelper.addText(plugin, separator);
            DecentXmlHelper.addChildElement(plugin, "version", change.getVersion());

            // add any custom content
            ElementProcessor processor = elementProcessor(change);
            if (processor != null) {
                processor.process(plugin, separator);
            }
            update = true;
        }
        return update;
    }


    public static boolean updateDependencyVersion(Document doc, DependencyVersionChange change, Map<String, String> propertyChanges) {
        Element rootElement = doc.getRootElement();
        List<Element> dependencies = DecentXmlHelper.findElementsWithName(rootElement, "dependency");
        String newVersion = change.getVersion();
        boolean update = false;
        for (Element element : dependencies) {
            String groupId = DecentXmlHelper.firstChildTextContent(element, "groupId");
            String artifactId = DecentXmlHelper.firstChildTextContent(element, "artifactId");
            if (change.matches(groupId, artifactId)) {
                String version = DecentXmlHelper.firstChildTextContent(element, "version");
                if (Strings.notEmpty(version)) {
                    if (version.startsWith("${") && version.endsWith("}")) {
                        String versionProperty = version.substring(2, version.length() - 1);
                        propertyChanges.put(versionProperty, newVersion);
                    } else {
                        if (DecentXmlHelper.updateFirstChild(element, "version", newVersion)) {
                            update = true;
                        }
                    }
                }
            }
        }
        return update;
    }

    public static boolean updateProperties(Document doc, Map<String, String> propertyChanges) {
        Element rootElement = doc.getRootElement();
        boolean update = false;
        Element properties = DecentXmlHelper.firstChild(rootElement, "properties");
        if (properties != null) {
            for (Map.Entry<String, String> entry : propertyChanges.entrySet()) {
                String propertyName = entry.getKey();
                String propertyVersion = entry.getValue();
                if (DecentXmlHelper.updateFirstChild(properties, propertyName, propertyVersion)) {
                    update = true;
                }
            }
        }
        return update;
    }


}
