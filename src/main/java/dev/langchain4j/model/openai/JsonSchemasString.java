package dev.langchain4j.model.openai;

import com.github.aiassistant.platform.JsonUtil;
import com.github.aiassistant.util.ThrowableUtil;
import dev.ai4j.openai4j.chat.JsonSchemaElement;
import dev.langchain4j.model.chat.request.json.JsonSchema;

import java.io.IOException;

public class JsonSchemasString {

    public static String toJsonString(JsonSchema jsonSchema) {
        if (jsonSchema == null) {
            return null;
        }
        JsonSchemaElement element = InternalOpenAiHelper.toOpenAiJsonSchemaElement(jsonSchema.rootElement());
        JsonUtil.ObjectWriter objectWriter = JsonUtil.objectWriter();
        try {
            return objectWriter.writeValueAsString(element);
        } catch (IOException e) {
            ThrowableUtil.sneakyThrows(e);
            return null;
        }
    }
}
