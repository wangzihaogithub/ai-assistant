package dev.langchain4j.agent.tool;

import dev.langchain4j.model.output.structured.Description;

import java.lang.reflect.*;
import java.util.*;

import static dev.langchain4j.agent.tool.JsonSchemaProperty.*;
import static dev.langchain4j.internal.TypeUtils.*;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

public class ToolSpecificationsUtil {

    public static ToolParameters toolParameters(Method method,
                                                Collection<String> requiredSet,
                                                Set<String> disabledSet,
                                                String[] methodParameterNames,
                                                Map<String, String> parameterDescriptions,
                                                Collection<String> addParamNames) {
        ToolSpecification.Builder builder = ToolSpecification.builder()
                .name("name")
                .description("description");
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            if (parameter.isAnnotationPresent(ToolMemoryId.class)) {
                continue;
            }
            P p = parameter.getAnnotation(P.class);
            if (p == null) {
                continue;
            }
            String parameterName = methodParameterNames[i];
            if (disabledSet.contains(parameterName)) {
                continue;
            }
            if (!parameterDescriptions.containsKey(parameterName) && !p.required()) {
                continue;
            }
            String parameterDescription = parameterDescriptions.get(parameterName);
            if (parameterDescription == null || parameterDescription.isEmpty()) {
                parameterDescription = parameterName;
            }
            Iterable<JsonSchemaProperty> propertyList = toJsonSchemaPropertiesV2(parameter, parameterDescription);
            if (requiredSet.contains(parameterName)) {
                builder.addParameter(parameterName, propertyList);
            } else {
                builder.addOptionalParameter(parameterName, propertyList);
            }
        }
        if (addParamNames != null) {
            for (String parameterName : addParamNames) {
                String parameterDescription = parameterDescriptions.get(parameterName);
                if (parameterDescription == null || parameterDescription.isEmpty()) {
                    parameterDescription = parameterName;
                }
                Collection<JsonSchemaProperty> propertyList = Arrays.asList(STRING, description(parameterDescription));
                if (requiredSet.contains(parameterName)) {
                    builder.addParameter(parameterName, propertyList);
                } else {
                    builder.addOptionalParameter(parameterName, propertyList);
                }
            }
        }
        return builder.build().parameters();
    }

    /**
     * Convert a {@link Parameter} to a {@link JsonSchemaProperty}.
     *
     * @param parameter the parameter.
     * @return the {@link JsonSchemaProperty}.
     */
    static Iterable<JsonSchemaProperty> toJsonSchemaPropertiesV2(Parameter parameter, String parameterDescription) {
        Class<?> type = parameter.getType();
        JsonSchemaProperty description;
        if (parameterDescription == null || parameterDescription.isEmpty()) {
            P annotation = parameter.getAnnotation(P.class);
            if (annotation != null) {
                description = description(annotation.value());
            } else {
                description = null;
            }
        } else {
            description = description(parameterDescription);
        }

        Iterable<JsonSchemaProperty> simpleType = toJsonSchemaProperties(type, description);

        if (simpleType != null) {
            return simpleType;
        }

        if (Collection.class.isAssignableFrom(type)) {
            return removeNulls(ARRAY, arrayTypeFrom(parameter.getParameterizedType()), description);
        }


        return removeNulls(OBJECT, schema(type), description);
    }

    static JsonSchemaProperty schema(Class<?> structured) {
        return schema(structured, new HashMap<>());
    }

    private static JsonSchemaProperty schema(Class<?> structured, HashMap<Class<?>, JsonSchemaProperty> visited) {
        if (visited.containsKey(structured)) {
            return visited.get(structured);
        }

        // Mark the class as visited by inserting it in the visited map with a null value initially.
        visited.put(structured, null);
        Map<String, Object> properties = new HashMap<>();
        for (Field field : structured.getDeclaredFields()) {
            String name = field.getName();
            if (name.equals("this$0") || Modifier.isStatic(field.getModifiers())) {
                // Skip inner class reference.
                continue;
            }
            Iterable<JsonSchemaProperty> schemaProperties = toJsonSchemaProperties(field, visited);
            Map<Object, Object> objectMap = new HashMap<>();
            for (JsonSchemaProperty jsonSchemaProperty : schemaProperties) {
                objectMap.put(jsonSchemaProperty.key(), jsonSchemaProperty.value());
            }
            properties.put(name, objectMap);
        }
        JsonSchemaProperty jsonSchemaProperty = from("properties", properties);
        // Update the visited map with the final JsonSchemaProperty for the current class
        visited.put(structured, jsonSchemaProperty);
        return jsonSchemaProperty;
    }

    private static Iterable<JsonSchemaProperty> toJsonSchemaProperties(Field field, HashMap<Class<?>, JsonSchemaProperty> visited) {

        Class<?> type = field.getType();

        Description annotation = field.getAnnotation(Description.class);
        JsonSchemaProperty description = annotation == null ? null : description(String.join(" ", annotation.value()));

        Iterable<JsonSchemaProperty> simpleType = toJsonSchemaProperties(type, description);

        if (simpleType != null) {
            return simpleType;
        }

        if (Collection.class.isAssignableFrom(type)) {
            return removeNulls(ARRAY, arrayTypeFrom((Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0]), description);
        }

        return removeNulls(OBJECT, schema(type, visited), description);
    }

    private static Iterable<JsonSchemaProperty> toJsonSchemaProperties(Class<?> type, JsonSchemaProperty description) {

        if (type == String.class) {
            return removeNulls(STRING, description);
        }

        if (isJsonBoolean(type)) {
            return removeNulls(BOOLEAN, description);
        }

        if (isJsonInteger(type)) {
            return removeNulls(INTEGER, description);
        }

        if (isJsonNumber(type)) {
            return removeNulls(NUMBER, description);
        }

        if (type.isArray()) {
            return removeNulls(ARRAY, arrayTypeFrom(type.getComponentType()), description);
        }

        if (type.isEnum()) {
            return removeNulls(STRING, enums((Class<?>) type), description);
        }

        return null;
    }


    private static JsonSchemaProperty arrayTypeFrom(Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            if (actualTypeArguments.length == 1) {
                return arrayTypeFrom((Class<?>) actualTypeArguments[0]);
            }
        }
        return items(JsonSchemaProperty.OBJECT);
    }

    private static JsonSchemaProperty arrayTypeFrom(Class<?> clazz) {
        if (clazz == String.class) {
            return items(JsonSchemaProperty.STRING);
        }
        if (isJsonBoolean(clazz)) {
            return items(JsonSchemaProperty.BOOLEAN);
        }
        if (isJsonInteger(clazz)) {
            return items(JsonSchemaProperty.INTEGER);
        }
        if (isJsonNumber(clazz)) {
            return items(JsonSchemaProperty.NUMBER);
        }
        return objectItems(schema(clazz));
    }

    /**
     * Remove nulls from the given array.
     *
     * @param items the array
     * @return an iterable of the non-null items.
     */
    static Iterable<JsonSchemaProperty> removeNulls(JsonSchemaProperty... items) {
        return stream(items)
                .filter(Objects::nonNull)
                .collect(toList());
    }
}
