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

import de.pdark.decentxml.Element;

import static io.fabric8.updatebot.support.DecentXmlHelper.addChildElement;
import static io.fabric8.updatebot.support.DecentXmlHelper.addText;
import static io.fabric8.updatebot.support.DecentXmlHelper.createChild;

/**
 */
public class ElementProcessors {
    public static ElementProcessor createFabric8MavenPluginElementProcessor() {
        return new ElementProcessor() {
            @Override
            public String toString() {
                return "Fabric8MavenPluginElementProcessor";
            }

            @Override
            public void process(Element element, String separator) {
                addFabric8MavenPluginConfig(element, separator);
            }
        };
    }

    protected static void addFabric8MavenPluginConfig(Element plugin, String separator) {
        Element executions = createChild(plugin, "executions", separator);
        separator += "  ";
        Element execution = createChild(executions, "execution", separator);
        separator += "  ";
        Element goals = createChild(execution, "goals", separator);
        String closeSep = separator;
        separator += "  ";
        addText(goals, separator);
        addChildElement(goals, "goal", "resource");
        addText(goals, separator);
        addChildElement(goals, "goal", "build");
        addText(goals, closeSep);
    }
}
