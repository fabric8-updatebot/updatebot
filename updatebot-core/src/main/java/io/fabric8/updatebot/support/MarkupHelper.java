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
package io.fabric8.updatebot.support;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.fabric8.utils.Files;
import io.fabric8.utils.IOHelpers;

import javax.tools.FileObject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URL;

/**
 */
public class MarkupHelper {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();


    public static ObjectMapper createYamlObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        return objectMapper;
    }


    public static String toJson(Object dto) throws JsonProcessingException {
        if (dto == null) {
            return "null";
        }
        Class<?> clazz = dto.getClass();
        return OBJECT_MAPPER.writerFor(clazz).writeValueAsString(dto);
    }

    public static String toPrettyJson(Object dto) throws JsonProcessingException {
        if (dto == null) {
            return "null";
        }
        Class<?> clazz = dto.getClass();
        ObjectMapper objectMapper = createPrettyJsonObjectMapper();
        return objectMapper.writerFor(clazz).writeValueAsString(dto);
    }

    protected static ObjectMapper createPrettyJsonObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        return objectMapper;
    }

    /**
     * Loads the YAML for the given DTO class
     */
    public static <T> T loadYaml(File file, Class<T> clazz) throws IOException {
        ObjectMapper mapper = createYamlObjectMapper();
        return mapper.readValue(file, clazz);
    }

    /**
     * Loads the YAML for the given DTO class
     */
    public static <T> T loadYaml(URL src, Class<T> clazz) throws IOException {
        ObjectMapper mapper = createYamlObjectMapper();
        return mapper.readValue(src, clazz);
    }

    /**
     * Loads the YAML file for the given DTO class
     */
    public static <T> T loadYaml(InputStream in, Class<T> clazz) throws IOException {
        byte[] data = Files.readBytes(in);
        return loadYaml(data, clazz);
    }


    /**
     * Loads the YAML text for the given DTO class
     */
    public static <T> T loadYaml(String text, Class<T> clazz) throws IOException {
        byte[] data = text.getBytes();
        return loadYaml(data, clazz);
    }


    /**
     * Loads the YAML file for the given DTO class
     */
    public static <T> T loadYaml(byte[] data, Class<T> clazz) throws IOException {
        ObjectMapper mapper = createYamlObjectMapper();
        return mapper.readValue(data, clazz);
    }

    public static void saveYaml(Object data, File file) throws IOException {
        ObjectMapper mapper = createYamlObjectMapper();
        mapper.writeValue(file, data);
    }

    public static void saveYaml(Object data, FileObject fileObject) throws IOException {
        ObjectMapper mapper = createYamlObjectMapper();
        try (Writer writer = fileObject.openWriter()) {
            mapper.writeValue(writer, data);
        }
    }

    public static String toYaml(Object data) throws IOException {
        ObjectMapper mapper = createYamlObjectMapper();
        return mapper.writeValueAsString(data);
    }

    public static JsonNode loadJson(File file) throws IOException {
        return OBJECT_MAPPER.readTree(file);
    }

    /**
     * Loads the JSON
     */
    public static <T> T loadJson(URL src, Class<T> clazz) throws IOException {
        return OBJECT_MAPPER.readValue(src, clazz);
    }

    public static void savePrettyJson(File file, Object value) throws IOException {
        // lets use the node layout
        NpmJsonPrettyPrinter printer = new NpmJsonPrettyPrinter();

        ObjectMapper objectMapper = createPrettyJsonObjectMapper();
        objectMapper.setDefaultPrettyPrinter(printer);
        String json = objectMapper.writer().writeValueAsString(value);

        IOHelpers.writeFully(file, json + System.lineSeparator());
    }

}
