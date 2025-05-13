package com.github.aiassistant.service.jsonschema;

import com.github.aiassistant.service.text.FunctionCallStreamingResponseHandler;
import com.github.aiassistant.service.text.GenerateRequest;
import dev.langchain4j.model.chat.request.json.JsonSchema;

/**
 * Structured Outputs vs JSON mode 结构化输出与JSON模式
 * response_format 参数的新选项
 * 开发者现在可以通过 response_format 参数的新选项 json_schema 提供 JSON 架构。
 * 当模型不调用工具时，但以结构化方式响应用户时，这非常有用。
 * 此功能适用于我们最新的 GPT-4o 模型：今天发布的 gpt-4o-2024-08-06 和 gpt-4o-mini-2024-07-18。
 * 当提供了 response_format 并设置 strict: true 时，模型输出将匹配提供的架构。
 * https://platform.openai.com/docs/guides/structured-outputs
 */
public interface JsonSchemaApi {
    default JsonSchema getJsonSchema() {
        return null;
    }

    default void config(FunctionCallStreamingResponseHandler h, GenerateRequest request) {
    }
}
