package com.github.aiassistant.service.jsonschema;

import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.output.structured.Description;
import dev.langchain4j.service.JsonSchemaTokenStream;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.output.JsonSchemas;

import java.util.List;

/**
 * 思考
 */
@FunctionalInterface
public interface ReasoningJsonSchema extends JsonSchemaApi {
    /**
     * 因为有些供应商并没有实现完全最新的OpenAi接口，所以还是要靠提示词方式保持兼容性。例如（阿里千问）
     */
    JsonSchema jsonSchema = JsonSchemas.jsonSchemaFrom(Result.class).orElse(null);

    @Override
    default JsonSchema getJsonSchema() {
        return jsonSchema;
    }

    @SystemMessage("# Role: 负责任务拆解成子任务的机器人\n" +
            "\n" +
            "## Profile\n" +
            "- description: 你负责将AI难以一次性执行的任务，拆解成方便AI执行的子任务。\n" +
            "并且你知道目前已拆解的任务树。\n" +
            "\n" +
            "## Tips\n" +
            "<目前已拆解的任务树>{{currentTask}}</目前已拆解的任务树>\n" +
            "\n" +
            "## OutputFormat:\n" +
            "- 严格按照json格式返回，无需解释，字段要求如下:{{jsonSchema}}\n")
    @UserMessage("{{question}}")
    JsonSchemaTokenStream parse(@V("question") String question);

    // @Data
    static class Result {
        @Description(value = "此字段用于标识用户的问题是否需要拆解，如果无需拆解这个字段为false")
        public boolean needSplitting;
        @Description(value = "此字段为被拆解成子问题，如果无需拆解可以为空")
        public List<String> tasks;

        public Result() {
        }

        public Result(boolean needSplitting, List<String> tasks) {
            this.needSplitting = needSplitting;
            this.tasks = tasks;
        }

        public boolean isNeedSplitting() {
            return needSplitting;
        }

        public void setNeedSplitting(boolean needSplitting) {
            this.needSplitting = needSplitting;
        }

        public List<String> getTasks() {
            return tasks;
        }

        public void setTasks(List<String> tasks) {
            this.tasks = tasks;
        }
    }
}
