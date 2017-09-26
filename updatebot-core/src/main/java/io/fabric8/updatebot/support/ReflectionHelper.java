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

import io.fabric8.utils.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class ReflectionHelper {
    private static final transient Logger LOG = LoggerFactory.getLogger(ReflectionHelper.class);

    /**
     * Returns all the fields annotated with the given annotation in the given class or any super classes
     */
    public static List<Field> findFieldsAnnotatedWith(Class<?> type, final Class<? extends Annotation> annotationClass) {
        return findFieldsMatching(type, new Filter<Field>() {
            @Override
            public boolean matches(Field field) {
                Annotation annotation = field.getAnnotation(annotationClass);
                return annotation != null;
            }

            @Override
            public String toString() {
                return "HasAnnotation(" + annotationClass.getName() + ")";
            }
        });
    }

    /**
     * Returns all the fields matching the given filter in the given class or any super classes
     */
    public static List<Field> findFieldsMatching(Class<?> type, Filter<Field> filter) {
        List<Field> answer = new ArrayList<>();
        appendFieldsAnnotatatedWith(answer, type, filter);
        return answer;
    }

    protected static void appendFieldsAnnotatatedWith(List<Field> list, Class<?> type, Filter<Field> filter) {
        Field[] fields = type.getDeclaredFields();
        if (fields != null) {
            for (Field field : fields) {
                if (filter.matches(field)) {
                    list.add(field);
                }
            }
        }

        if (type.getSuperclass() != null) {
            appendFieldsAnnotatatedWith(list, type.getSuperclass(), filter);
        }
    }

    public static Object getFieldValue(Field field, Object instance) {
        try {
            field.setAccessible(true);
            return field.get(instance);
        } catch (IllegalAccessException e) {
            LOG.warn("Could not access " + field + " on " + instance + ". " + e, e);
            return null;
        }
    }
}
