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
import io.fabric8.updatebot.kind.Kind;
import io.fabric8.updatebot.model.DependencyVersionChange;
import io.fabric8.updatebot.test.Tests;
import io.fabric8.utils.Objects;
import io.fabric8.utils.Strings;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.fabric8.updatebot.support.DecentXmlHelper.findElementsWithName;
import static io.fabric8.updatebot.support.DecentXmlHelper.firstChild;
import static io.fabric8.updatebot.support.DecentXmlHelper.firstChildTextContent;
import static io.fabric8.updatebot.support.DecentXmlHelper.parseXmlFile;
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
        boolean found = false;
        List<Element> elements = findElementsWithName(doc.getRootElement(), "plugin");
        for (Element element : elements) {
            String groupId = firstChildTextContent(element, "groupId");
            String artifactId = firstChildTextContent(element, "artifactId");
            String version = firstChildTextContent(element, "version");
            if (Strings.notEmpty(groupId) && Strings.notEmpty(artifactId) && Strings.notEmpty(version)) {
                if (change.matches(groupId, artifactId)) {
                    found = true;
                    if (!version.startsWith("$")) {
                        LOG.info("File " + file + " has plugin " + change.getDependency() + " version: " + version);
                        assertThat(version).describedAs("File " + file + " plugin version for " + change.getDependency()).isEqualTo(change.getVersion());
                    }
                }
            }
        }
        assertThat(found).describedAs("File " + file + " does not have plugin " +
                change.getDependency() + " version: " + change.getVersion()).isTrue();
    }

    protected static void assertDependencyVersionChanged(File file, Document doc, DependencyVersionChange change) {
        boolean found = false;
        List<Element> elements = findElementsWithName(doc.getRootElement(), "dependency");
        for (Element element : elements) {
            String groupId = firstChildTextContent(element, "groupId");
            String artifactId = firstChildTextContent(element, "artifactId");
            String version = firstChildTextContent(element, "version");
            if (Strings.notEmpty(groupId) && Strings.notEmpty(artifactId) && Strings.notEmpty(version)) {
                if (change.matches(groupId, artifactId)) {
                    found = true;
                    if (!version.startsWith("$")) {
                        LOG.info("File " + file + " has dependency " + change.getDependency() + " version: " + version);
                        assertThat(version).describedAs("File " + file + " dependency version for " + change.getDependency()).isEqualTo(change.getVersion());
                    }
                }
            }
        }
/*
        assertThat(found).describedAs("File " + file + " does not have dependency " +
                change.getDependency() + " version: " + change.getVersion()).isTrue();
*/
    }

    protected static void assertPropertiesValid(File file, Document doc, Map<String, String> propertyVersions) {
        Element properties = firstChild(doc.getRootElement(), "properties");
        if (properties != null) {
            for (Map.Entry<String, String> entry : propertyVersions.entrySet()) {
                String propertyName = entry.getKey();
                String propertyValue = entry.getValue();
                assertPropertyEqualsIfExists(file, properties, propertyName, propertyValue);
            }
        }
    }

    protected static void assertPropertyEqualsIfExists(File file, Element properties, String propertyName, String expectedValue) {
        String value = firstChildTextContent(properties, propertyName);
        if (value != null) {
            LOG.info("File " + file + " has property " + propertyName + " = " + value);
            assertEquals("File " + file + " property " + propertyName + " element", expectedValue, value);
        }
    }

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
                PomUpdateStatus pomUpdateStatus = PomUpdateStatus.createPomUpdateStatus(file);
                pomUpdateStatus.setRootPom(true);
                pomsToChange.add(pomUpdateStatus);
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
        changes.add(new MavenDependencyVersionChange("io.fabric8:fabric8-maven-plugin", fmpVersion, MavenScopes.PLUGIN, true, ElementProcessors.createFabric8MavenPluginElementProcessor()));

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
    }
}