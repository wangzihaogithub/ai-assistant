package com.github.aiassistant;

import com.github.aiassistant.service.text.rerank.AliyunReRankModel;
import com.github.aiassistant.service.text.rerank.ReRankModel;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class AliyunReRankModelTest {

    public static void main(String[] args) throws Exception {
        Collection<String> list = Arrays.asList(
                "文本排序模型广泛用于搜索引擎和推荐系统中，它们根据文本相关性对候选文本进行排序",
                "量子计算是计算科学的一个前沿领域",
                "预训练语言模型的发展给文本排序模型带来了新的进展"
        );
        String q = "什么是文本排序模型";
        String apiKey = "sk-xxxxxx";
        AliyunReRankModel model = new AliyunReRankModel(apiKey);
        CompletableFuture<List<String>> future = model.topN(q, list, e -> e, 2, new BiFunction<ReRankModel, List<ReRankModel.SortKey<String>>, CompletableFuture<List<ReRankModel.SortKey<String>>>>() {

            @Override
            public CompletableFuture<List<ReRankModel.SortKey<String>>> apply(ReRankModel reRankModel, List<ReRankModel.SortKey<String>> sortKeys) {
                List<ReRankModel.SortKey<String>> collect = sortKeys.stream().filter(e -> e.getSimilarity() > 0.5D).collect(Collectors.toList());
                return CompletableFuture.completedFuture(collect);
            }
        });
        Collection<String> strings = future.get();
        System.out.println("strings = " + strings);
    }

}
