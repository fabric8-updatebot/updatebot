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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;

import java.io.IOException;

/**
 * A pretty printer for npm layout syntax
 */
public final class NpmJsonPrettyPrinter extends DefaultPrettyPrinter {
    public NpmJsonPrettyPrinter() {
        this._objectFieldValueSeparatorWithSpaces = ": ";

        Indenter indenter = new DefaultIndenter("  ", System.lineSeparator());
        this.indentObjectsWith(indenter);
        this.indentArraysWith(indenter);
    }

    @Override
    public DefaultPrettyPrinter createInstance() {
        return new NpmJsonPrettyPrinter();
    }

    @Override
    public void writeEndObject(JsonGenerator g, int nrOfEntries) throws IOException {
        if (!this._objectIndenter.isInline()) {
            --this._nesting;
        }

        if (nrOfEntries > 0) {
            this._objectIndenter.writeIndentation(g, this._nesting);
        } else {
            // lets disable the space in empty objects
            //g.writeRaw(' ');
        }

        g.writeRaw('}');
    }
}
