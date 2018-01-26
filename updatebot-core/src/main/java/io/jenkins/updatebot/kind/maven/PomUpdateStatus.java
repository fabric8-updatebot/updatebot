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
import io.jenkins.updatebot.model.DependencyVersionChange;
import io.jenkins.updatebot.support.DecentXmlHelper;
import io.fabric8.utils.IOHelpers;
import io.fabric8.utils.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private boolean rootPom;

    public PomUpdateStatus(File pom, Document doc) {
        this.pom = pom;
        this.doc = doc;
    }

    public static PomUpdateStatus createPomUpdateStatus(File file) throws IOException {
        Document doc = DecentXmlHelper.parseXmlFile(file);
        return new PomUpdateStatus(file, doc);
    }

    public boolean isRootPom() {
        return rootPom;
    }

    public void setRootPom(boolean rootPom) {
        this.rootPom = rootPom;
    }

    /**
     * Returns true if we should add the given dependency version change if the dependency is missing.
     * <p>
     * For dependencies like plugins we often only add it to the parent <code>pom.xml</code> only
     */
    public boolean shouldLazyAdd(DependencyVersionChange change) {
        if (change instanceof MavenDependencyVersionChange) {
            MavenDependencyVersionChange mavenDependencyVersionChange = (MavenDependencyVersionChange) change;
            if (mavenDependencyVersionChange.isAddOnlyToRootPom()) {
                return isRootPom();
            }
        }
        return change.isAdd();
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
                IOHelpers.writeFully(pom, doc.toXML());
            } catch (Exception e) {
                throw new IOException("failed to save " + pom + ". " + e, e);
            }
        }
        return updated;
    }

    public void updateVersions(List<DependencyVersionChange> changes, Map<String, String> propertyChanges) {
        for (DependencyVersionChange change : changes) {
            String scope = change.getScope();
            boolean lazyAdd = shouldLazyAdd(change);
            if (Objects.equal(MavenScopes.PLUGIN, scope)) {
                if (PomHelper.updatePluginVersion(doc, change, propertyChanges, lazyAdd)) {
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
