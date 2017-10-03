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

import io.fabric8.updatebot.kind.Kind;
import io.fabric8.updatebot.model.DependencyVersionChange;
import io.fabric8.updatebot.test.Tests;
import io.fabric8.utils.DomHelper;
import io.fabric8.utils.Objects;
import io.fabric8.utils.Strings;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.fabric8.updatebot.kind.maven.PomHelper.parseXmlFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 */
public class PomHelperTest {
    private static final transient Logger LOG = LoggerFactory.getLogger(PomHelperTest.class);

    String fmpVersion = "9.0.0";
    String assertJVersion = "9.1.0";
    String fabric8Version = "9.2.0";
    String springBootVersion = "9.3.0";

    protected static void assertChangesValid(File file, Document doc, List<DependencyVersionChange> changes) {
        for (DependencyVersionChange change : changes) {
            if (Objects.equal(MavenScopes.PLUGIN, change.getScope())) {
                assertPluginVersionChanged(file, doc, change);
            } else {
                assertDependencyVersionChanged(file, doc, change);
            }
        }
    }

    protected static void assertPluginVersionChanged(File file, Document doc, DependencyVersionChange change) {
        NodeList plugins = doc.getElementsByTagName("plugin");
        for (int i = 0, size = plugins.getLength(); i < size; i++) {
            Node item = plugins.item(i);
            if (item instanceof Element) {
                Element element = (Element) item;
                String groupId = DomHelper.firstChildTextContent(element, "groupId");
                String artifactId = DomHelper.firstChildTextContent(element, "artifactId");
                String version = DomHelper.firstChildTextContent(element, "version");
                if (Strings.notEmpty(groupId) && Strings.notEmpty(artifactId) && Strings.notEmpty(version)) {
                    if (change.matches(groupId, artifactId) && !version.startsWith("$")) {
                        LOG.info("File " + file + " has plugin " + change.getDependency() + " version: " + version);
                        assertThat(version).describedAs("File " + file + " plugin version for " + change.getDependency()).isEqualTo(change.getVersion());
                    }
                }
            }
        }
    }

    protected static void assertDependencyVersionChanged(File file, Document doc, DependencyVersionChange change) {
        NodeList plugins = doc.getElementsByTagName("dependency");
        for (int i = 0, size = plugins.getLength(); i < size; i++) {
            Node item = plugins.item(i);
            if (item instanceof Element) {
                Element element = (Element) item;
                String groupId = DomHelper.firstChildTextContent(element, "groupId");
                String artifactId = DomHelper.firstChildTextContent(element, "artifactId");
                String version = DomHelper.firstChildTextContent(element, "version");
                if (Strings.notEmpty(groupId) && Strings.notEmpty(artifactId) && Strings.notEmpty(version)) {
                    if (change.matches(groupId, artifactId) && !version.startsWith("$")) {
                        LOG.info("File " + file + " has dependency " + change.getDependency() + " version: " + version);
                        assertThat(version).describedAs("File " + file + " dependency version for " + change.getDependency()).isEqualTo(change.getVersion());
                    }
                }
            }
        }
    }

    protected static void assertPropertiesValid(File file, Document doc, Map<String, String> propertyVersions) {
        Element properties = DomHelper.firstChild(doc.getDocumentElement(), "properties");
        if (properties != null) {
            for (Map.Entry<String, String> entry : propertyVersions.entrySet()) {
                String propertyName = entry.getKey();
                String propertyValue = entry.getValue();
                assertPropertyEqualsIfExists(file, properties, propertyName, propertyValue);
            }
        }
    }

    protected static void assertPropertyEqualsIfExists(File file, Element properties, String propertyName, String expectedValue) {
        String value = DomHelper.firstChildTextContent(properties, propertyName);
        if (value != null) {
            LOG.info("File " + file + " has property " + propertyName + " = " + value);
            assertEquals("File " + file + " property " + propertyName + " element", expectedValue, value);
        }
    }
/*

     protected static void assertPluginVersionsMatchIndices(File file, Document doc, List<String> expectPluginVersionList) {
         String fmpVersion = VersionHelper.fabric8MavenPluginVersion();
         NodeList plugins = doc.getElementsByTagName("plugin");
         int index = 0;
         for (int i = 0, size = plugins.getLength(); i < size; i++) {
             Node item = plugins.item(i);
             if (item instanceof Element) {
                 Element element = (Element) item;
                 if ("fabric8-maven-plugin".equals(DomHelper.firstChildTextContent(element, "artifactId"))) {
                     String version = DomHelper.firstChildTextContent(element, "version");

                     if (expectPluginVersionList.size() <= index) {
                         fail("file " + file + " does not have enough version expressions in <expectPluginVersions> element has we have at least " + (index + 1) + " fabric8-maven-plugin elements!");
                     }
                     String expected = expectPluginVersionList.get(index);
                     if ("version".equals(expected)) {
                         expected = fmpVersion;
                     }

                     if ("none".equals(expected)) {
                         if (version != null) {
                             fail("file " + file + " has a <version> element in the fabric8-maven-plugin element index " + index + " when it is not expected");
                         }
                     } else {
                         if (version == null) {
                             fail("file " + file + " expected a <version> element in the fabric8-maven-plugin element index " + index + " for " + element);
                         }
                         assertEquals("file " + file + " fabric8-maven-plugin element index " + index + " <version> element", expected, version);
                     }
                     System.out.println("file " + file + " has fabric8-maven-plugin " + index + " version " + expected);
                     index++;
                 }
             }
         }
         if (expectPluginVersionList.size() > index) {
             fail("file " + file + " does not contain " + expectPluginVersionList.size() + " fabric8-maven-plugin elements so <expectPluginVersions> element contains too many version expressions!");
         }

         // now lets assert the properties
         Element properties = DomHelper.firstChild(doc.getDocumentElement(), "properties");
         if (properties != null) {
             assertPropertyEqualsIfExists(file, properties, "fabric8.version", VersionHelper.fabric8Version());
             assertPropertyEqualsIfExists(file, properties, "fabric8.maven.plugin.version", fmpVersion);
         }
     }


*/

    @Test
    public void testVersionReplacement() throws Exception {

        File outDir = Tests.copyPackageSources(getClass());

        LOG.info("Updating poms in " + outDir);

        File[] files = outDir.listFiles();
        assertNotNull("No output files!", files);
        assertTrue("No output files!", files.length > 0);

        List<PomUpdateStatus> pomsToChange = new ArrayList<>();

        for (File file : files) {
            try {
                pomsToChange.add(PomUpdateStatus.createPomUpdateStatus(file));
            } catch (Exception e) {
                fail("Failed to parse " + file, e);
            }
        }


        Map<String, String> propertyVersions = new HashMap<>();
        propertyVersions.put("assertj.version", assertJVersion);
        propertyVersions.put("fabric8.maven.plugin.version", fmpVersion);
        propertyVersions.put("fabric8.version", fabric8Version);
        propertyVersions.put("spring-boot.version", springBootVersion);

        // lets add some changes
        List<DependencyVersionChange> changes = new ArrayList<>();
        changes.add(new DependencyVersionChange(Kind.MAVEN, "io.fabric8:fabric8-maven-plugin", fmpVersion, MavenScopes.PLUGIN));


        changes.add(new DependencyVersionChange(Kind.MAVEN, "org.assertj:assertj-core", assertJVersion, MavenScopes.ARTIFACT));

        // BOM dependencies
        changes.add(new DependencyVersionChange(Kind.MAVEN, "io.fabric8:fabric8-project-bom-with-platform-deps", fabric8Version, MavenScopes.ARTIFACT));
        changes.add(new DependencyVersionChange(Kind.MAVEN, "org.springframework.boot:spring-boot-dependencies", springBootVersion, MavenScopes.ARTIFACT));

        PomHelper.updatePomVersions(pomsToChange, changes);

        for (File file : files) {
            Document doc;
            try {
                doc = parseXmlFile(file);
            } catch (Exception e) {
                fail("Failed to parse " + file + " due to " + e, e);
                continue;
            }

            assertPropertiesValid(file, doc, propertyVersions);
            assertChangesValid(file, doc, changes);
        }


        // TODO find the
/*
             StatusDTO status = new StatusDTO();
             ChoosePipelineStep.updatePomVersions(file, status, "myspace");

             List<String> warnings = status.getWarnings();
             for (String warning : warnings) {
                 System.out.println("Warning: " + warning);
             }

             // lets assert that the right elements got a version added...
             Document doc;
             try {
                 doc = parseXmlFile(file);
             } catch (Exception e) {
                 LOG.error("Failed to parse " + file + " " + e, e);
                 fail("Failed to parse " + file + " due to " + e);
                 continue;
             }

             String expectPluginVersionsText = "";
             NodeList pluginVersionElements = doc.getElementsByTagName("expectPluginVersions");
             if (pluginVersionElements.getLength() > 0) {
                 Node item = pluginVersionElements.item(0);
                 if (item instanceof Element) {
                     Element element = (Element) item;
                     expectPluginVersionsText = element.getTextContent();
                 }
             }

             List<String> expectPluginVersionList = new ArrayList<>();
             if (Strings.isNotBlank(expectPluginVersionsText)) {
                 StringTokenizer iter = new StringTokenizer(expectPluginVersionsText);
                 while (iter.hasMoreTokens()) {
                     expectPluginVersionList.add(iter.nextToken());
                 }
             } else {
                 fail("File " + file + " does not contain a <expectPluginVersions> in the <properties> section!");
             }

             assertPluginVersionsMatchIndices(file, doc, expectPluginVersionList);
         }*/
    }
}