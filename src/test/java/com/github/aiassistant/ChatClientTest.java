package com.github.aiassistant;

import com.github.aiassistant.service.text.GenerateRequest;
import dev.langchain4j.model.openai.StreamingResponseHandlerAdapter;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.openai.OpenAiChatClient;
import dev.langchain4j.model.output.Response;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class ChatClientTest {
    private static final String apiKey = "sk-d23ced5bf9494edfbdxxxxxxxx";

    public static void main(String[] args) {
        OpenAiChatClient chatClient = AiBuilders.openAiChatClient(
                "https://dashscope.aliyuncs.com/compatible-mode/v1",
                apiKey,
                "qwen-plus");

        GenerateRequest request = new GenerateRequest(
                Arrays.asList(
                        new UserMessage("你好")
                )
        );
        CompletableFuture<Response<AiMessage>> future = chatClient.request(new StreamingResponseHandlerAdapter() {

            @Override
            public void onNext(String token) {
                System.out.println("token = " + token);
            }

            @Override
            public void onError(Throwable error) {

            }
        }, request);
        Response<AiMessage> join = future.join();
        System.out.println(join);
    }
}
