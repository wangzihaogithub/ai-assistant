package dev.langchain4j.service;

import com.github.aiassistant.service.text.ChatStreamingResponseHandler;
import com.github.aiassistant.util.AiUtil;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.rag.content.Content;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface JsonSchemaTokenStream extends TokenStream {

    String VAR_JSON_SCHEMA = "jsonSchema";

    JsonSchemaTokenStream responseHandler(ChatStreamingResponseHandler handler);

    JsonSchemaTokenStream jsonSchema(JsonSchema jsonSchema);

    @Override
    JsonSchemaTokenStream onComplete(Consumer<Response<AiMessage>> consumer);

    @Override
    JsonSchemaTokenStream onError(Consumer<Throwable> consumer);

    @Override
    JsonSchemaTokenStream onNext(Consumer<String> consumer);

    @Override
    JsonSchemaTokenStream onRetrieved(Consumer<List<Content>> consumer);

    @Override
    JsonSchemaTokenStream ignoreErrors();

    default CompletableFuture<Map<String, Object>> toMapFuture() {
        CompletableFuture future = AiUtil.toFutureJson(this, LinkedHashMap.class, getInterfaceClass());
        return future;
    }

    default <T> CompletableFuture<T> toJsonFuture(Class<T> responseType) {
        return AiUtil.toFutureJson(this, responseType, getInterfaceClass());
    }

    default <T> CompletableFuture<List<T>> toJsonListFuture(Class<T> responseType) {
        return AiUtil.toFutureJsonList(this, responseType, getInterfaceClass());
    }

    default CompletableFuture<Boolean> toBooleanFuture() {
        return AiUtil.toFutureBoolean(this);
    }

    default CompletableFuture<String> toStringFuture() {
        return AiUtil.toFutureString(this);
    }

    Class<?> getInterfaceClass();
}
