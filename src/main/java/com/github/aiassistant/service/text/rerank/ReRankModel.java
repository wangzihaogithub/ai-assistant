package com.github.aiassistant.service.text.rerank;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ReRank模型‌是一种用于优化信息检索结果排序的机器学习模型，通过精细化评估文档与查询的相关性，提升最终结果的准确性和语义匹配度。
 * ReRank模型在初步检索（如关键词匹配或向量相似度检索）之后，对候选文档进行二次筛选和排序，从而提升语义排序的精度‌
 *
 * @see AliyunReRankModel 使用阿里云Rerank模型进行rerank
 * @see EmbeddingReRankModel 使用Embedding模型进行rerank
 */
public interface ReRankModel {

    /**
     * 相似度为NULL时，返回-1
     * 例如：执行topNKey方法时，取出排名考前的N条, 如果documents小于topN，则不计算相似度，所以相似度都为NULL_SIMILARITY
     *
     * @see #topNKey(String, Collection, Function, int, BiFunction)
     * @see #topNKey(String, Collection, Function, int)
     */
    float NULL_SIMILARITY = -1;

    /**
     * 根据字段进行元素分组
     *
     * @param documents 元素集合
     * @param groupKey  分组字段
     * @param <E>       元素类型
     * @return 元素分组
     */
    default <E> LinkedHashMap<String, List<E>> groupByKey(Collection<E> documents, Function<E, String> groupKey) {
        return documents.stream()
                .collect(Collectors.groupingBy(groupKey, LinkedHashMap::new, Collectors.toList()));
    }

    /**
     * 取出排名考前的N条元素
     *
     * @param query     问题
     * @param documents 待排序的候选document文档列表
     * @param reRankKey 排序字段
     * @param topN      排序返回的top文档数量
     * @param <E>       文档元素
     * @return 排序后的元素集合
     */
    default <E> CompletableFuture<List<E>> topN(
            String query, Collection<E> documents,
            Function<E, String> reRankKey, int topN
    ) {
        return topN(query, documents, reRankKey, topN, null);
    }

    /**
     * 取出排名考前的N条元素（支持同时过滤元素）
     *
     * @param query     问题
     * @param documents 待排序的候选document文档列表
     * @param reRankKey 排序字段
     * @param topN      排序返回的top文档数量
     * @param filter    元素过滤
     * @param <E>       文档元素
     * @param <M>       rerank模型
     * @param <K>       排序字段
     * @return 排序后的元素集合
     */
    default <E, M extends ReRankModel, K extends SortKey<E>> CompletableFuture<List<E>> topN(
            String query, Collection<E> documents,
            Function<E, String> reRankKey, int topN,
            BiFunction<M, List<K>, CompletableFuture<List<K>>> filter
    ) {
        return topNKey(query, documents, reRankKey, topN, filter)
                .thenApply(sortKeys -> sortKeys.stream()
                        .map(SortKey::getValueList)
                        .filter(Objects::nonNull)
                        .flatMap(Collection::stream)
                        .limit(topN)
                        .collect(Collectors.toList()));
    }

    /**
     * 取出排名考前的N条
     *
     * @param query     问题
     * @param documents 待排序的候选document文档列表
     * @param reRankKey 排序字段
     * @param topN      排序返回的top文档数量
     * @param <E>       文档元素
     * @param <K>       排序字段
     * @return 排序键
     */
    default <E, K extends SortKey<E>> CompletableFuture<List<K>> topNKey(
            String query, Collection<E> documents,
            Function<E, String> reRankKey, int topN) {
        return topNKey(query, documents, reRankKey, topN, null);
    }

    /**
     * 取出排名考前的N条（支持同时过滤元素）
     *
     * @param query     问题
     * @param documents 待排序的候选document文档列表
     * @param reRankKey 排序字段
     * @param topN      排序返回的top文档数量
     * @param filter    元素过滤
     * @param <E>       文档元素
     * @param <M>       rerank模型
     * @param <K>       排序字段
     * @return 排序键
     */
    default <E, M extends ReRankModel, K extends SortKey<E>> CompletableFuture<List<K>> topNKey(String query, Collection<E> documents, Function<E, String> reRankKey, int topN, BiFunction<M, List<K>, CompletableFuture<List<K>>> filter) {
        if (documents.size() <= topN) {
            return CompletableFuture.completedFuture(groupByKey(documents, reRankKey).entrySet().stream()
                    .map(e -> (K) new SortKeyImpl<>(e.getValue(), e.getKey(), null))
                    .collect(Collectors.toList()));
        }
        return sortKey(query, documents, reRankKey, topN, filter);
    }

    /**
     * 根据排序字段排序（支持同时过滤元素）
     *
     * @param query     问题
     * @param documents 待排序的候选document文档列表
     * @param reRankKey 排序字段
     * @param topN      排序返回的top文档数量
     * @param filter    元素过滤
     * @param <E>       文档元素
     * @param <M>       rerank模型
     * @param <K>       排序字段
     * @return 排序键
     */
    <E, M extends ReRankModel, K extends SortKey<E>> CompletableFuture<List<K>> sortKey(
            String query, Collection<E> documents,
            Function<E, String> reRankKey, int topN,
            BiFunction<M, List<K>, CompletableFuture<List<K>>> filter
    );

    interface SortKey<E> {
        String getKey();

        float getSimilarity();

        List<E> getValueList();
    }

    public static class SortKeyImpl<E> implements SortKey<E> {
        protected final List<E> valueList;
        protected final String key;
        protected final Number similarity;

        public SortKeyImpl(List<E> valueList, String key, Number similarity) {
            this.valueList = valueList;
            this.key = key;
            this.similarity = similarity;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public float getSimilarity() {
            return similarity == null ? NULL_SIMILARITY : similarity.floatValue();
        }

        @Override
        public List<E> getValueList() {
            return valueList;
        }

        @Override
        public String toString() {
            return "(" + Math.round(getSimilarity() * 100) * 0.01 + ")" + key;
        }
    }

}
