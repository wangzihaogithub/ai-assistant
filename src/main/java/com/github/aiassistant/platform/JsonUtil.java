package com.github.aiassistant.platform;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.CollectionType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class JsonUtil {
    private static final ObjectMapper objectMapper;

    static {
        ObjectMapper o = new ObjectMapper();
        o.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
        o.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        o.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        o.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        objectMapper = o;
    }

    public static ObjectWriter objectWriter() {
        return new ObjectWriter() {
            @Override
            public void writeValue(OutputStream outputStream, Object value) throws IOException {
                objectMapper.writeValue(outputStream, value);
            }

            @Override
            public byte[] writeValueAsBytes(Object value) throws IOException {
                return objectMapper.writeValueAsBytes(value);
            }

            @Override
            public String writeValueAsString(Object value) throws IOException {
                return objectMapper.writeValueAsString(value);
            }
        };
    }

    public static ObjectReader objectReader() {
        return new ObjectReader() {
            @Override
            public <T> T readValue(InputStream src, Class<T> valueType) throws IOException {
                return objectMapper.readValue(src, valueType);
            }

            @Override
            public <T> T readValue(String src, Class<T> valueType) throws IOException {
                return objectMapper.readValue(src, valueType);
            }

            @Override
            public <T> List<T> readValueList(String src, Class<T> valueType) throws IOException {
                CollectionType collectionType = objectMapper.getTypeFactory().constructCollectionType(List.class, valueType);
                return objectMapper.readValue(src, collectionType);
            }
        };
    }

    public interface ObjectWriter {
        void writeValue(OutputStream outputStream, Object value) throws IOException;

        byte[] writeValueAsBytes(Object value) throws IOException;

        String writeValueAsString(Object value) throws IOException;
    }

    public interface ObjectReader {
        <T> T readValue(InputStream src, Class<T> valueType) throws IOException;

        <T> T readValue(String src, Class<T> valueType) throws IOException;

        <T> List<T> readValueList(String src, Class<T> valueType) throws IOException;
    }
}