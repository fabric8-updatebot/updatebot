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
import com.beust.jcommander.Parameters;
import io.fabric8.updatebot.Configuration;
import io.fabric8.updatebot.github.Issues;
import io.fabric8.updatebot.model.Projects;
import io.fabric8.updatebot.repository.LocalRepository;
import io.fabric8.updatebot.repository.Repositories;
import io.fabric8.utils.Files;
import io.fabric8.utils.Strings;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHRepository;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;

import static io.fabric8.updatebot.github.PullRequests.COMMAND_COMMENT_INDENT;
import static io.fabric8.updatebot.github.PullRequests.COMMAND_COMMENT_PREFIX;
import static io.fabric8.updatebot.github.PullRequests.COMMAND_COMMENT_PREFIX_SEPARATOR;
import static io.fabric8.updatebot.support.MarkupHelper.loadYaml;
import static io.fabric8.updatebot.support.ReflectionHelper.findFieldsAnnotatedWith;
import static io.fabric8.updatebot.support.ReflectionHelper.getFieldValue;

/**
 */
public abstract class CommandSupport {
    private List<LocalRepository> localRepositories;

    public String createPullRequestComment() {
        StringBuilder builder = new StringBuilder(COMMAND_COMMENT_PREFIX);
        builder.append(COMMAND_COMMENT_PREFIX_SEPARATOR);
        appendPullRequestComment(builder);
        return builder.toString();
    }

    protected void appendPullRequestComment(StringBuilder builder) {
        builder.append(COMMAND_COMMENT_INDENT);

        Parameters annotation = getClass().getAnnotation(Parameters.class);
        if (annotation != null) {
            String[] commandNames = annotation.commandNames();
            if (commandNames != null && commandNames.length > 0) {
                builder.append(commandNames[0]);
            }
        }
        appendPullRequestCommentArguments(builder);
        builder.append("\n");
    }

    /**
     * Appends any command specific parameters
     */
    protected void appendPullRequestCommentArguments(StringBuilder builder) {
        List<Field> fields = findFieldsAnnotatedWith(getClass(), Parameter.class);
        appendPullRequestsCommentArguments(builder, fields, true);
        appendPullRequestsCommentArguments(builder, fields, false);
    }

    private void appendPullRequestsCommentArguments(StringBuilder builder, List<Field> fields, boolean namedArguments) {
        for (Field field : fields) {
            Parameter parameter = field.getAnnotation(Parameter.class);
            String[] names = parameter.names();
            if (names.length > 0) {
                String name = names[0];
                if (!namedArguments) {
                    continue;
                }
                builder.append(" ");
                builder.append(name);
            } else {
                if (namedArguments) {
                    continue;
                }
            }
            Object value = getFieldValue(field, this);
            if (value != null) {
                builder.append(" ");
                if (value instanceof Collection) {
                    builder.append(Strings.join((Collection) value, " "));
                } else if (value instanceof Object[]) {
                    builder.append(Strings.join(" ", (Object[]) value));
                } else {
                    builder.append(value);
                }
            }
        }
    }

    public ParentContext run(Configuration configuration) throws IOException {
        validateConfiguration(configuration);

        ParentContext parentContext = new ParentContext();
        List<LocalRepository> repositories = cloneOrPullRepositories(configuration);
        for (LocalRepository repository : repositories) {
            CommandContext context = createCommandContext(repository, configuration);
            parentContext.addChild(context);
            run(context);
        }
        return parentContext;
    }

    protected void validateConfiguration(Configuration configuration) {
    }

    protected CommandContext createCommandContext(LocalRepository repository, Configuration configuration) {
        return new CommandContext(repository, configuration);
    }

    public abstract void run(CommandContext context) throws IOException;

    public List<LocalRepository> cloneOrPullRepositories(Configuration configuration) throws IOException {
        Projects projects = loadProjects(configuration);
        this.localRepositories = Repositories.cloneOrPullRepositories(this, configuration, projects);
        return localRepositories;
    }

    public List<LocalRepository> getLocalRepositories() {
        return localRepositories;
    }

    // Properties
    //-------------------------------------------------------------------------
    protected Projects loadProjects(Configuration configuration) throws IOException {
        String configFile = configuration.getConfigFile();
        File file = new File(configFile);
        if (!Files.isFile(file)) {
            File sourceDir = configuration.getSourceDir();
            if (sourceDir != null) {
                file = new File(sourceDir, configFile);
            }
        }
        if (!Files.isFile(file)) {
            URL url = null;
            try {
                url = new URL(configFile);
                InputStream in = null;
                try {
                    in = url.openStream();
                } catch (IOException e) {
                    throw new IOException("Failed to open URL " + configFile + ". " + e, e);
                }
                if (in != null) {
                    return loadYaml(in, Projects.class);
                }
            } catch (MalformedURLException e) {
                // ignore
            }
            throw new FileNotFoundException(file.getCanonicalPath());
        }
        return loadYaml(file, Projects.class);
    }

    protected GHIssue getOrFindIssue(CommandContext context, GHRepository ghRepository) throws IOException {
        GHIssue issue = context.getIssue();
        if (issue == null) {
            List<GHIssue> issues = Issues.getOpenIssues(ghRepository, context.getConfiguration());
            issue = Issues.findIssue(context, issues);
            context.setIssue(issue);
        }
        return issue;
    }
}
