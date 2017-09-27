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
package io.fabric8.updatebot.model;

import io.fabric8.updatebot.kind.Kind;

/**
 * Represents the update of a dependency with an optional scope
 */
public class PushVersionDetails {
    private final Kind kind;
    private final String property;
    private final String value;
    private final String scope;

    public PushVersionDetails(Kind kind, String property, String value) {
        this(kind, property, value, null);
    }

    public PushVersionDetails(Kind kind, String property, String value, String scope) {
        this.kind = kind;
        this.property = property;
        this.value = value;
        this.scope = scope;
    }

    @Override
    public String toString() {
        return "UpdateDependencyStep{" +
                "kind=" + kind +
                ", property='" + property + '\'' +
                ", value='" + value + '\'' +
                ", scope='" + scope + '\'' +
                '}';
    }

    public Kind getKind() {
        return kind;
    }

    public String getProperty() {
        return property;
    }

    public String getValue() {
        return value;
    }

    public String getScope() {
        return scope;
    }
}
