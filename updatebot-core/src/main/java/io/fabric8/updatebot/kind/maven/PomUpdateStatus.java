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
import io.fabric8.utils.DomHelper;
import io.fabric8.utils.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 */
public class PomUpdateStatus {
    private static final transient Logger LOG = LoggerFactory.getLogger(PomUpdateStatus.class);

    private final File pom;
    private final Document doc;
    private boolean updated;

    public PomUpdateStatus(File pom, Document doc) {
        this.pom = pom;
        this.doc = doc;
    }

    public static PomUpdateStatus createPomUpdateStatus(File file) throws IOException, SAXException, ParserConfigurationException {
        Document doc = PomHelper.parseXmlFile(file);
        return new PomUpdateStatus(file, doc);
    }


    /**
     * Saves the pom.xml if its been changed
     *
     * @return true if the pom was modified
     * @throws IOException
     */
    public boolean saveIfChanged() throws IOException {
        if (updated) {
            LOG.info("Updating " + pom);
            try {
                DomHelper.save(doc, pom);
            } catch (Exception e) {
                throw new IOException("failed to save " + pom + ". " + e, e);
            }
        }
        return updated;
    }

    public void updateVersions(List<DependencyVersionChange> changes, Map<String, String> propertyChanges) {
        for (DependencyVersionChange change : changes) {
            String scope = change.getScope();
            if (Objects.equal(MavenScopes.PLUGIN, scope)) {
                if (PomHelper.updatePluginVersion(doc, change, propertyChanges)) {
                    updated = true;
                }
            } else {
                if (PomHelper.updateDependencyVersion(doc, change, propertyChanges)) {
                    updated = true;
                }
                // TODO check for BOM / Parent change too!
            }
        }
    }

    public void updateProperties(Map<String, String> propertyChanges) {
        if (PomHelper.updateProperties(doc, propertyChanges)) {
            updated = true;
        }

    }
}
