package com.github.aiassistant.service.jsonschema;

import com.github.aiassistant.util.AiUtil;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.request.json.JsonSchemaElement;
import dev.langchain4j.model.output.structured.Description;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.output.JsonSchemas;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 问题分类
 */
@FunctionalInterface
public interface QuestionClassifySchema {
    /**
     * 因为有些供应商并没有实现完全最新的OpenAi接口，所以还是要靠提示词方式保持兼容性。例如（阿里千问）
     */
    Map<String, JsonSchemaElement> jsonSchema = JsonSchemas.jsonSchemaFrom(Result.class)
            .map(JsonSchema::rootElement)
            .map(e -> (JsonObjectSchema) e)
            .map(JsonObjectSchema::properties)
            .orElse(null);

    @SystemMessage("## 角色定义\n" +
            "你是一位经验丰富的分类专家\"AI助手\"，你擅长根据聊天记录准确理解并对用户的问题进行分类。\n" +
            "\n" +
            "## 分类范围要求\n" +
            " <分类范围要求>{{defineClassify}}</分类范围要求>\n" +
            " \n" +
            "## 聊天记录\n" +
            " <聊天记录>{{chat.historyMessage}}</聊天记录>\n" +
            " \n" +
            "## OutputFormat:\n" +
            "严格按照json格式返回，无需解释，字段要求如下:{{jsonSchema}}\n")
    @UserMessage("{{question}}")
    TokenStream parse(@V("question") String question,
                      @V("defineClassifyNames") String defineClassifyNames,
                      @V("defineClassify") String defineClassify,
                      @V("jsonSchema") String jsonSchema);

    default CompletableFuture<Result> future(String q, String defineClassifyNames, String defineClassify) {
        return AiUtil.toFutureJson(parse(q, defineClassifyNames, defineClassify, jsonSchema.toString()), Result.class);
    }

    static class Result {
        @Description(value = "分类结果集")
        public List<String> classifyList;

        public List<String> getClassifyList() {
            return classifyList;
        }

        public void setClassifyList(List<String> classifyList) {
            this.classifyList = classifyList;
        }
    }

}
