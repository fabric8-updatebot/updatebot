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
package io.fabric8.updatebot.kind.npm.dependency;

/**
 */
public class DependencyCheck {
    private final boolean valid;
    private final String message;
    private final DependencyInfo dependencyInfo;

    public DependencyCheck(boolean valid, String message, DependencyInfo dependencyInfo) {
        this.valid = valid;
        this.message = message;
        this.dependencyInfo = dependencyInfo;
    }

    @Override
    public String toString() {
        return "DependencyCheck{" +
                "valid=" + valid +
                ", message='" + message + '\'' +
                '}';
    }

    public boolean isValid() {
        return valid;
    }

    public String getMessage() {
        return message;
    }

    public DependencyInfo getDependencyInfo() {
        return dependencyInfo;
    }

    public String getDependency() {
        return dependencyInfo.getDependency();
    }

    public String getVersion() {
        return dependencyInfo.getVersion();
    }
}
