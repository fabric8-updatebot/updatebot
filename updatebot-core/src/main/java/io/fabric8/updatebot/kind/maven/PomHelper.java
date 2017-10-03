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

import io.fabric8.updatebot.model.DependencyVersionChange;
import io.fabric8.updatebot.support.Strings;
import io.fabric8.utils.DomHelper;
import io.fabric8.utils.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
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


    public static Document parsePom(File pom) throws IOException {
        if (Files.isFile(pom)) {
            Document doc;
            try {
                doc = parseXmlFile(pom);
            } catch (Exception e) {
                throw new IOException("Cannot parse pom.xml: " + e, e);
            }
        }
        return null;
    }

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
        Element rootElement = doc.getDocumentElement();
        NodeList plugins = rootElement.getElementsByTagName("plugin");

        String newVersion = change.getVersion();
        boolean update = false;
        for (int i = 0, size = plugins.getLength(); i < size; i++) {
            Node item = plugins.item(i);
            if (item instanceof Element) {
                Element element = (Element) item;
                String groupId = DomHelper.firstChildTextContent(element, "groupId");
                String artifactId = DomHelper.firstChildTextContent(element, "artifactId");
                if (change.matches(groupId, artifactId)) {
                    String version = DomHelper.firstChildTextContent(element, "version");
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
        }
        return update;
    }

    public static boolean updateDependencyVersion(Document doc, DependencyVersionChange change, Map<String, String> propertyChanges) {
        Element rootElement = doc.getDocumentElement();
        NodeList plugins = rootElement.getElementsByTagName("dependency");

        String newVersion = change.getVersion();
        boolean update = false;
        for (int i = 0, size = plugins.getLength(); i < size; i++) {
            Node item = plugins.item(i);
            if (item instanceof Element) {
                Element element = (Element) item;
                String groupId = DomHelper.firstChildTextContent(element, "groupId");
                String artifactId = DomHelper.firstChildTextContent(element, "artifactId");
                if (change.matches(groupId, artifactId)) {
                    String version = DomHelper.firstChildTextContent(element, "version");
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
        }
        return update;
    }

    public static boolean updateProperties(Document doc, Map<String, String> propertyChanges) {
        Element rootElement = doc.getDocumentElement();
        boolean update = false;
        Element properties = DomHelper.firstChild(rootElement, "properties");
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

    /**
     * Returns the combined text of previous nodes of the given element
     */

    private static String getPreviousText(Node node) {
        StringBuilder builder = new StringBuilder();
        while (node != null) {
            node = node.getPreviousSibling();
            if (node instanceof Text) {
                Text textNode = (Text) node;
                builder.append(textNode.getWholeText());
            } else {
                break;
            }
        }
        return builder.toString();
    }

    public static Document parseXmlFile(File pomFile) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        return documentBuilder.parse(pomFile);
    }

    private static boolean updateFirstChild(Element parentElement, String elementName, String value) {
        if (parentElement != null) {
            Element element = DomHelper.firstChild(parentElement, elementName);
            if (element != null) {
                String textContent = element.getTextContent();
                if (textContent == null || !value.equals(textContent)) {
                    element.setTextContent(value);
                    return true;
                }
            }
        }
        return false;
    }

    private static Element getGrandParentElement(Element node) {
        Node parentNode = node.getParentNode();
        if (parentNode != null) {
            Node grandParent = parentNode.getParentNode();
            if (grandParent instanceof Element) {
                Element element = (Element) grandParent;
                return element;
            }
        }
        return null;
    }
}
