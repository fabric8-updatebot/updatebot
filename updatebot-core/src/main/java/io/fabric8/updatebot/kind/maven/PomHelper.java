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
package io.fabric8.updatebot.kind.maven;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.XMLParser;
import io.fabric8.updatebot.model.DependencyVersionChange;
import io.fabric8.updatebot.support.Strings;
import io.fabric8.utils.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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


    public static boolean updatePluginVersion(Document doc, DependencyVersionChange change, Map<String, String> propertyChanges) {
        Element rootElement = doc.getRootElement();
        List<Element> plugins = findElementsWithName(rootElement, "plugin");

        String newVersion = change.getVersion();
        boolean update = false;
        for (Element element : plugins) {
            String groupId = firstChildTextContent(element, "groupId");
            String artifactId = firstChildTextContent(element, "artifactId");
            if (change.matches(groupId, artifactId)) {
                String version = firstChildTextContent(element, "version");
                if (Strings.notEmpty(version)) {
                    if (version.startsWith("${") && version.endsWith("}")) {
                        String versionProperty = version.substring(2, version.length() - 1);
                        propertyChanges.put(versionProperty, newVersion);
                    } else {
                        if (updateFirstChild(element, "version", newVersion)) {
                            update = true;
                        }
                    }
                }
            }
        }
        return update;
    }

    public static boolean updateDependencyVersion(Document doc, DependencyVersionChange change, Map<String, String> propertyChanges) {
        Element rootElement = doc.getRootElement();
        List<Element> dependencies = findElementsWithName(rootElement, "dependency");
        String newVersion = change.getVersion();
        boolean update = false;
        for (Element element : dependencies) {
            String groupId = firstChildTextContent(element, "groupId");
            String artifactId = firstChildTextContent(element, "artifactId");
            if (change.matches(groupId, artifactId)) {
                String version = firstChildTextContent(element, "version");
                if (Strings.notEmpty(version)) {
                    if (version.startsWith("${") && version.endsWith("}")) {
                        String versionProperty = version.substring(2, version.length() - 1);
                        propertyChanges.put(versionProperty, newVersion);
                    } else {
                        if (updateFirstChild(element, "version", newVersion)) {
                            update = true;
                        }
                    }
                }
            }
        }
        return update;
    }

    public static List<Element> findElementsWithName(Element rootElement, String elementName) {
        List<Element> answer = new ArrayList<>();
        List<Element> children = rootElement.getChildren();
        for (Element child : children) {
            if (Objects.equal(elementName, child.getName())) {
                answer.add(child);
            } else {
                answer.addAll(findElementsWithName(child, elementName));
            }
        }
        return answer;
    }

    public static String firstChildTextContent(Element element, String elementName) {
        Element child = firstChild(element, elementName);
        if (child != null) {
            return child.getText();
        }
        return null;
    }

    public static Element firstChild(Element element, String elementName) {
        return element.getChild(elementName);
    }

    public static boolean updateProperties(Document doc, Map<String, String> propertyChanges) {
        Element rootElement = doc.getRootElement();
        boolean update = false;
        Element properties = firstChild(rootElement, "properties");
        if (properties != null) {
            for (Map.Entry<String, String> entry : propertyChanges.entrySet()) {
                String propertyName = entry.getKey();
                String propertyVersion = entry.getValue();
                if (updateFirstChild(properties, propertyName, propertyVersion)) {
                    update = true;
                }
            }
        }
        return update;
    }


    public static Document parseXmlFile(File pomFile) throws IOException {
        XMLParser parser = new XMLParser();
        return parser.parse(pomFile);
    }

    private static boolean updateFirstChild(Element parentElement, String elementName, String value) {
        if (parentElement != null) {
            Element element = firstChild(parentElement, elementName);
            if (element != null) {
                String textContent = element.getText();
                if (textContent == null || !value.equals(textContent)) {
                    element.setText(value);
                    return true;
                }
            }
        }
        return false;
    }
}
