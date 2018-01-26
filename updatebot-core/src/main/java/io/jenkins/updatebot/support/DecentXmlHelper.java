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
package io.jenkins.updatebot.support;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.Text;
import de.pdark.decentxml.XMLParser;
import io.fabric8.utils.Objects;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class DecentXmlHelper {
    /**
     * Returns the first child with the given name, lazily creating one if required with the
     * given text prepended before the element if the text is not null
     */
    public static Element getOrCreateChild(Element element, String name, String text) {
        Element child = firstChild(element, name);
        if (child == null) {
            if (text != null) {
                element.addNode(new Text(text + "  "));
            }
            child = new Element(name);
            element.addNode(child);
            if (text != null) {
                element.addNode(new Text(text + "  "));
            }
        }
        return child;
    }

    /**
     * Creates a new child of the given element adding the text after the new node
     */
    public static Element createChild(Element element, String name, String text) {
        if (text != null) {
            element.addNode(new Text(text + "  "));
        }
        Element child = new Element(name);
        element.addNode(child);
        if (text != null) {
            element.addNode(new Text(text));
        }
        return child;
    }

    /**
     * Adds the given text to the given node
     */
    public static Text addText(Element element, String text) {
        Text textNode = new Text(text + "  ");
        element.addNode(textNode);
        return textNode;
    }

    public static Element addChildElement(Element parent, String elementName) {
        Element element = new Element(elementName);
        parent.addNode(element);
        return element;
    }

    public static Element addChildElement(Element parent, String elementName, String textContent) {
        Element element = addChildElement(parent, elementName);
        element.setText(textContent);
        return element;
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

    public static Document parseXmlFile(File pomFile) throws IOException {
        XMLParser parser = new XMLParser();
        return parser.parse(pomFile);
    }

    public static boolean updateFirstChild(Element parentElement, String elementName, String value) {
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
