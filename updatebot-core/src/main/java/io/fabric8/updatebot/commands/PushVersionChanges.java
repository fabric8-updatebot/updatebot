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
package io.fabric8.updatebot.commands;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import io.fabric8.updatebot.kind.Kind;
import io.fabric8.updatebot.kind.Updater;
import io.fabric8.updatebot.repository.LocalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Push changes from a specific release pipeline into downstream projects
 */
@Parameters(commandDescription = "Pushes version changes into your projects. " +
        "You usually invoke this command after a release has been performed")
public class PushVersionChanges extends UpdateBotCommand {
    private static final transient Logger LOG = LoggerFactory.getLogger(PushVersionChanges.class);

    @Parameter(order = 0, names = {"--kind", "-k"}, description = "The kind of property to replace based on the kind of language or build tool.", required = true)
    private Kind kind;

    @Parameter(description = "The property name and values to be replaced", required = true)
    private List<String> values;

    public Kind getKind() {
        return kind;
    }

    public void setKind(Kind kind) {
        this.kind = kind;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
        if (values.isEmpty()) {
            throw new ParameterException("At least 2 values need to be specified for the property to be updated and its value!");
        }
        if (values.size() %2 == 1) {
            throw new ParameterException("You must specify an even number of arguments for pairs of the property and version!");
        }
    }

    public void values(String... values) {
        setValues(Arrays.asList(values));
    }

    @Override
    protected boolean doProcess(UpdateContext context) throws IOException {
        LocalRepository repository = context.getRepository();
        LOG.debug("Updating version in: " + repository.getDir() + " repo: " + repository.getCloneUrl());

        boolean answer = false;
        for (int i = 0; i + 1 < values.size(); i += 2) {
            String propertyName = values.get(i);
            String version = values.get(i+1);

            if (updatePropertyVersion(context, propertyName, version)) {
                answer = true;
            }
        }
        return answer;
    }

    protected boolean updatePropertyVersion(UpdateContext parentContext, String propertyName, String version) throws IOException {
        UpdateVersionContext context = new UpdateVersionContext(parentContext, kind, propertyName, version);
        Updater updater = kind.getUpdater();
        boolean updated = false;
        if (updater.isApplicable(context)) {
            updated = updater.pushVersions(context);
        }
        return updated;
    }


}
