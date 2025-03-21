package com.github.aiassistant.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ContentType {
    private final String type;
    private final String subtype;
    private final Map<String, String> parameters;

    // Constructor for ContentType
    private ContentType(String type, String subtype, Map<String, String> parameters) {
        this.type = type;
        this.subtype = subtype;
        this.parameters = parameters; // Use a copy to avoid modification issues
    }

    public static ContentType create(String type, String subtype) {
        return new ContentType(type, subtype, Collections.emptyMap());
    }

    // Static method to parse content type string
    public static ContentType parse(String contentTypeString) {
        if (contentTypeString == null || contentTypeString.isEmpty()) {
            throw new IllegalArgumentException("Content-Type string cannot be null or empty");
        }

        String[] parts = contentTypeString.split(";");
        String mainType = parts[0].trim();

        // Split main type into type and subtype
        String[] mainTypeParts = mainType.split("/");
        if (mainTypeParts.length != 2) {
            throw new IllegalArgumentException("Invalid Content-Type format: " + contentTypeString);
        }

        String type = mainTypeParts[0].trim();
        String subtype = mainTypeParts[1].trim();

        Map<String, String> params = new HashMap<>();
        for (int i = 1; i < parts.length; i++) {
            String param = parts[i].trim();
            if (!param.isEmpty()) {
                int equalIndex = param.indexOf('=');
                if (equalIndex != -1) {
                    String paramName = param.substring(0, equalIndex).trim();
                    String paramValue = param.substring(equalIndex + 1).trim();

                    // Remove any quotes around the value
                    if (paramValue.startsWith("\"") && paramValue.endsWith("\"")) {
                        paramValue = paramValue.substring(1, paramValue.length() - 1);
                    }

                    params.put(paramName, paramValue);
                } else {
                    // If there's no '=', we can ignore this part or log a warning
                    // Here, we'll just ignore it for simplicity
                }
            }
        }

        return new ContentType(type, subtype, params);
    }

    // Main method for testing
    public static void main(String[] args) {
        String contentTypeString1 = "application/json; charset=utf-8; boundary=something";
        String contentTypeString2 = "application/xml; boundary=abc123";

        ContentType contentType1 = parse(contentTypeString1);
        ContentType contentType2 = parse(contentTypeString2);

        System.out.println("ContentType 1:");
        System.out.println("Type: " + contentType1.getType());
        System.out.println("Subtype: " + contentType1.getSubtype());
        System.out.println("Charset: " + contentType1.getCharset());
        System.out.println("Parameters: " + contentType1.getParameters());

        System.out.println("\nContentType 2:");
        System.out.println("Type: " + contentType2.getType());
        System.out.println("Subtype: " + contentType2.getSubtype());
        System.out.println("Charset: " + contentType2.getCharset());
        System.out.println("Parameters: " + contentType2.getParameters());
    }

    // Getters for ContentType fields
    public String getType() {
        return type;
    }

    public String getSubtype() {
        return subtype;
    }

    public Map<String, String> getParameters() {
        return parameters; // Return a copy to avoid modification issues
    }

    // Helper method to get charset parameter, returning null if not present
    public String getCharset() {
        return parameters.get("charset");
    }
}