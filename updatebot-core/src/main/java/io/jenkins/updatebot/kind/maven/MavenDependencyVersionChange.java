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

import io.jenkins.updatebot.kind.Kind;
import io.jenkins.updatebot.model.DependencyVersionChange;

/**
 * Used to indicate a dependency change which is lazily created if not present but only applies
 * to the root <code>pom.xml</code>.
 * <p>
 * For example to add a particular maven plugin which updates the version if used in any pom but is added
 * only in the root pom.xml
 */
public class MavenDependencyVersionChange extends DependencyVersionChange {
    private final boolean addOnlyToRootPom;
    private final ElementProcessor elementProcessor;

    public MavenDependencyVersionChange(String dependency, String version, boolean addOnlyToRootPom, ElementProcessor elementProcessor) {
        super(Kind.MAVEN, dependency, version);
        this.addOnlyToRootPom = addOnlyToRootPom;
        this.elementProcessor = elementProcessor;
    }

    public MavenDependencyVersionChange(String dependency, String version, String scope, boolean addOnlyToRootPom, ElementProcessor elementProcessor) {
        super(Kind.MAVEN, dependency, version, scope);
        this.addOnlyToRootPom = addOnlyToRootPom;
        this.elementProcessor = elementProcessor;
    }

    public MavenDependencyVersionChange(String dependency, String version, String scope, boolean add, boolean addOnlyToRootPom, ElementProcessor elementProcessor) {
        super(Kind.MAVEN, dependency, version, scope, add);
        this.addOnlyToRootPom = addOnlyToRootPom;
        this.elementProcessor = elementProcessor;
    }

    public static ElementProcessor elementProcessor(DependencyVersionChange change) {
        if (change instanceof MavenDependencyVersionChange) {
            MavenDependencyVersionChange mavenDependencyVersionChange = (MavenDependencyVersionChange) change;
            return mavenDependencyVersionChange.getElementProcessor();
        }
        return null;
    }

    public boolean isAddOnlyToRootPom() {
        return addOnlyToRootPom;
    }

    public ElementProcessor getElementProcessor() {
        return elementProcessor;
    }
}
