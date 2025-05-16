package com.github.aiassistant;

import com.github.aiassistant.service.text.nlu.AliyunOpenNluModel;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class AliyunOpenNluModelTest {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        String apiKey = "sk-xxxx";
        AliyunOpenNluModel model = new AliyunOpenNluModel(apiKey);
        CompletableFuture<List<String>> future = model.classification("老师今天表扬我了", Arrays.asList("积极", "消极"));
        Collection<String> strings = future.get();

        CompletableFuture<List<String>> future1 = model.extraction("要求计算机专业", Arrays.asList("专业名称"));
        Collection<String> string1s = future1.get();
        System.out.println("strings = ");
    }
}
