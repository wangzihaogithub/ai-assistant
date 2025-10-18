package com.github.aiassistant;

import com.github.aiassistant.service.text.embedding.EmbeddingModelClient;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class EmbeddingModelClientTest {
    private static final String apiKey = "sk-d23ced5bf9494edfbdxxxxxxxx";

    public static void main(String[] args) {
        EmbeddingModelClient.Factory factory = AiBuilders.aliyunEmbeddingV4(
                apiKey, 1024);
        EmbeddingModelClient client = factory.get();
        float[] floats = client.embed("九棵树");
        CompletableFuture<List<float[]>> future = client.addEmbedList(Arrays.asList("client", "九棵树"));
        client.embedAllFuture();
        List<float[]> floats1 = client.embedList(Arrays.asList("clienst", "九d棵树"));

        System.out.println();

    }
}
