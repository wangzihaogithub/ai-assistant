package com.github.aiassistant.service.text.rerank;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * ReRank模型‌是一种用于优化信息检索结果排序的机器学习模型，通过精细化评估文档与查询的相关性，提升最终结果的准确性和语义匹配度。
 * ReRank模型在初步检索（如关键词匹配或向量相似度检索）之后，对候选文档进行二次筛选和排序，从而提升语义排序的精度‌
 */
public interface ReRankModel {

    default <E> CompletableFuture<List<E>> topN(
            String query, Collection<E> documents,
            Function<E, String> reRankKey, int topN
    ) {
        return topN(query, documents, reRankKey, topN, null);
    }

    <E, M extends ReRankModel, K extends SortKey<E>> CompletableFuture<List<E>> topN(
            String query, Collection<E> documents,
            Function<E, String> reRankKey, int topN,
            BiFunction<M, List<K>, CompletableFuture<List<K>>> filter
    );

    interface SortKey<E> {
        String getKey();

        float getSimilarity();

        List<E> getValueList();
    }
}
