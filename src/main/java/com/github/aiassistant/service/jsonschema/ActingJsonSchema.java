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
 * 执行
 */
@FunctionalInterface
public interface ActingJsonSchema {
    /**
     * 因为有些供应商并没有实现完全最新的OpenAi接口，所以还是要靠提示词方式保持兼容性。例如（阿里千问）
     */
    Map<String, JsonSchemaElement> jsonSchema = JsonSchemas.jsonSchemaFrom(Result.class)
            .map(JsonSchema::rootElement)
            .map(e -> (JsonObjectSchema) e)
            .map(JsonObjectSchema::properties)
            .orElse(null);

    @SystemMessage("# Role: 负责执行任务的机器人\n" +
            "\n" +
            "## Profile\n" +
            "- description: 你负责执行Tips中的任务清单里的其中一项任务。\n" +
            "并且你知道这些任务都是为了解决Tips中的人类用户提出的原始问题，不要跑偏方向。\n" +
            "并且你知道Tips中之前已经执行过的任务结果。\n" +
            "\n" +
            "## Tips\n" +
            "<任务清单>{{taskList}}</任务清单>\n" +
            "<之前已经执行过的任务结果>{{taskAnswerList}}</之前已经执行过的任务结果>\n" +
            "<人类用户提出的原始问题>{{question}}</人类用户提出的原始问题>\n" +
            "\n" +
            "## OutputFormat:\n" +
            "- 严格按照json格式返回，无需解释，字段要求如下:{{jsonSchema}}")
    @UserMessage("{{task}}")
    TokenStream parse(@V("task") String task,
                      @V("question") String question,
                      @V("taskList") String taskList,
                      @V("taskAnswerList") String taskAnswerList,
                      @V("jsonSchema") String jsonSchema);

    default CompletableFuture<Result> future(String task, String question, String taskList, String taskAnswerList) {
        TokenStream stream = parse(task, question, taskList, taskAnswerList, jsonSchema.toString());
        return AiUtil.toFutureJson(stream, Result.class, getClass());
    }

    // @Data
    static class Result {
        @Description(value = "此字段用于标识这个任务是否被解决")
        public boolean resolved;
        @Description(value = "此字段用于你向用户解释，没有解决的原因")
        public String failMessage;
        @Description(value = "如果已被解决，这就是解决的最终答案")
        public String answer;
        @Description(value = "如果你有不明白或需要向用户确认的问题，可以通过此字段向用户提问")
        public String aiQuestion;
        @Description(value = "如果任务未被解决，可以在此字段上返回一些搜索关键词，以助于使用搜索引擎")
        public List<String> websearchKeyword;
    }

}
