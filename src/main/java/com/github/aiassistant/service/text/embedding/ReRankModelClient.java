package com.github.aiassistant.service.text.embedding;

// import lombok.AllArgsConstructor;
// import lombok.Data;
// import lombok.NoArgsConstructor;

import com.github.aiassistant.util.FutureUtil;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ReRankModelClient {
    private static final Map<float[], Float> MAGNITUDE_VECTOR_CACHE = Collections.synchronizedMap(new WeakHashMap<>());
    private final EmbeddingModelClient model;

    public ReRankModelClient(EmbeddingModelClient embeddingModelClient) {
        this.model = embeddingModelClient;
    }

    private static <E> CompletableFuture<List<E>> sort(EmbeddingModelClient model,
                                                       Map<float[], String> datasourceV,
                                                       float[] queryVector, Map<String, List<E>> groupByMap,
                                                       int topN,
                                                       BiFunction<EmbeddingModelClient, List<SortKey<E>>, CompletableFuture<List<SortKey<E>>>> filter) {
        LinkedList<SortKey<E>> sortList = new LinkedList<>();
        float magnitudeQueryVector = calculateMagnitude(queryVector);// 向量2的模
        datasourceV.forEach((k, v) -> sortList.add(new SortKey<>(v, k, similarity(k, queryVector, magnitudeQueryVector), groupByMap.get(v))));
        sortList.sort(Comparator.comparing(e -> e.similarity));

        CompletableFuture<List<E>> future = new CompletableFuture<>();
        Iterator<SortKey<E>> iterator = sortList.descendingIterator();
        class SortFilter implements Consumer<List<SortKey<E>>> {

            @Override
            public void accept(List<SortKey<E>> sortKeyList) {
                int sum = sortKeyList.stream().mapToInt(e -> e.valueList.size()).sum();
                if (!iterator.hasNext() || sum >= topN) {
                    List<E> result = sortKeyList.stream().flatMap(e -> e.valueList.stream()).limit(topN).collect(Collectors.toList());
                    future.complete(result);
                    return;
                }
                List<SortKey<E>> topNList = new ArrayList<>(sortKeyList);
                int count = sum;
                while (iterator.hasNext()) {
                    SortKey<E> next = iterator.next();
                    topNList.add(next);
                    count += next.valueList.size();
                    if (count >= topN) {
                        break;
                    }
                }
                filter.apply(model, topNList)
                        .thenAccept(new SortFilter())
                        .exceptionally(throwable -> {
                            future.completeExceptionally(throwable);
                            return null;
                        });
            }
        }
        new SortFilter().accept(Collections.emptyList());
        return future;
    }

    /**
     * 计算向量的模长
     */
    private static float calculateMagnitude(float[] vector) {
        return MAGNITUDE_VECTOR_CACHE.computeIfAbsent(vector, k -> {
            float sumOfSquares = 0;
            for (float component : vector) {
                sumOfSquares += component * component; // 计算每个分量的平方并累加
            }
            // 计算累加结果的平方根
            return (float) Math.sqrt(sumOfSquares);
        });
    }

    /**
     * 是否相似
     */
    private static float similarity(float[] vector, float[] queryVector, float magnitudeQueryVector) {
        if (vector == null || queryVector == null || vector.length != queryVector.length) {
            return 0; // 向量长度不同，直接认为不相似
        }
        float magnitudeVector = calculateMagnitude(vector); // 向量1的模
        float dotProduct = 0; // 点积
        for (int i = 0; i < vector.length; i++) {
            dotProduct += vector[i] * queryVector[i]; // 计算点积
        }
        // 计算余弦相似度
        return dotProduct / (magnitudeVector * magnitudeQueryVector);
    }

    public static <E> BiFunction<EmbeddingModelClient, List<SortKey<E>>, CompletableFuture<List<SortKey<E>>>>
    blackFilter(List<Function<E, String>> filterKeys, List<QuestionVO> blacklist) {
        return new BlackFilter<>(filterKeys, blacklist);
    }

    public <E> CompletableFuture<List<E>> topN(String query, Collection<E> datasourceList,
                                               Function<E, String> reRankKey, int topN) {
        return topN(query, datasourceList, reRankKey, topN, (o1, o2) -> CompletableFuture.completedFuture(o2));
    }

    public <E> CompletableFuture<List<E>> topN(String query, Collection<E> datasourceList,
                                               Function<E, String> reRankKey, int topN,
                                               BiFunction<EmbeddingModelClient, List<SortKey<E>>, CompletableFuture<List<SortKey<E>>>> filter) {
        if (datasourceList.size() <= topN) {
            return CompletableFuture.completedFuture(new ArrayList<>(datasourceList));
        }
        Map<String, List<E>> groupByMap = datasourceList.stream().collect(Collectors.groupingBy(reRankKey));
        ArrayList<String> keys = new ArrayList<>();
        keys.add(query);
        keys.addAll(groupByMap.keySet());
        CompletableFuture<List<float[]>> embedList = model.addEmbedList(keys);
        model.embedAllFuture();

        CompletableFuture<CompletableFuture<List<E>>> f = embedList.thenApply(list -> {
            Map<float[], String> datasourceV = new HashMap<>();
            for (int i = 1; i < list.size(); i++) {
                String key = keys.get(i);
                float[] floats = list.get(i);
                datasourceV.put(floats, key);
            }
            float[] queryVector = list.get(0);
            return sort(model, datasourceV, queryVector, groupByMap, topN, filter);
        });
        return FutureUtil.allOf(f);
    }

    public static class SortKey<E> {
        public final String key;
        public final float[] KeyVector;
        public final float similarity;
        public final List<E> valueList;

        public SortKey(String key, float[] keyVector, float similarity, List<E> valueList) {
            this.key = key;
            KeyVector = keyVector;
            this.similarity = similarity;
            this.valueList = valueList;
        }

        public SortKey<E> fork(List<E> valueList) {
            return new SortKey<>(key, KeyVector, similarity, valueList);
        }
    }

    // @Data
    public static class QuestionVO {
        private String question;
        private Double similarity;

        public QuestionVO() {
        }

        public QuestionVO(String question, Double similarity) {
            this.question = question;
            this.similarity = similarity;
        }

        @Override
        public String toString() {
            return question;
        }
    }

    private static class BlackFilter<E> implements BiFunction<EmbeddingModelClient, List<SortKey<E>>, CompletableFuture<List<SortKey<E>>>> {
        private final List<Function<E, String>> filterKeys;
        private final List<QuestionVO> blacklist;

        private BlackFilter(List<Function<E, String>> filterKeys, List<QuestionVO> blacklist) {
            this.filterKeys = filterKeys;
            this.blacklist = blacklist;
        }

        private static <E> List<E> filterTest(Predicate<float[]> rerankPredicate,
                                              List<E> valueList,
                                              Function<E, String> getter,
                                              Map<String, float[]> vectorMap) {
            List<E> list = new ArrayList<>();
            for (E row : valueList) {
                float[] floats = vectorMap.get(getter.apply(row));
                if (rerankPredicate.test(floats)) {
                    list.add(row);
                }
            }
            return list;
        }

        private static <E> CompletableFuture<List<SortKey<E>>> filterByColumnVector(
                EmbeddingModelClient model,
                List<SortKey<E>> sortKeys,
                Predicate<float[]> rerankPredicate,
                Function<E, String> getter) {
            List<String> stringMap = sortKeys.stream().map(e -> e.valueList).flatMap(Collection::stream).map(getter).collect(Collectors.toList());
            CompletableFuture<List<float[]>> contentFuture = model.addEmbedList(stringMap);
            CompletableFuture<List<SortKey<E>>> future = contentFuture.thenApply(list -> {
                Map<String, float[]> vectorMap = new HashMap<>();
                for (int i = 0; i < list.size(); i++) {
                    vectorMap.put(stringMap.get(i), list.get(i));
                }
                List<SortKey<E>> result = new ArrayList<>();
                for (SortKey<E> sortKey : sortKeys) {
                    List<E> filter = filterTest(rerankPredicate, sortKey.valueList, getter, vectorMap);
                    result.add(sortKey.fork(filter));
                }
                return result;
            });
            model.embedAllFuture();
            return future;
        }

        private static <E> List<SortKey<E>> filterByKeyVector(
                List<SortKey<E>> sortKeys,
                Predicate<float[]> rerankPredicate
        ) {
            List<SortKey<E>> list = new ArrayList<>();
            for (SortKey<E> sortKey : sortKeys) {
                if (rerankPredicate.test(sortKey.KeyVector)) {
                    list.add(sortKey);
                }
            }
            return list;
        }

        private static CompletableFuture<Predicate<float[]>> createPredicate(EmbeddingModelClient model, List<QuestionVO> blacklist) {
            CompletableFuture<Predicate<float[]>> future = model.addEmbedList(blacklist.stream().map(e -> e.question).collect(Collectors.toList())).thenApply(list -> {
                        List<BlackPredicate> result = new ArrayList<>();
                        for (int i = 0; i < list.size(); i++) {
                            result.add(new BlackPredicate(list.get(i), blacklist.get(i)));
                        }
                        return result;
                    })
                    .thenApply(blackPredicates -> {
                        Iterator<BlackPredicate> iterator = blackPredicates.iterator();
                        if (iterator.hasNext()) {
                            Predicate<float[]> root = iterator.next();
                            while (iterator.hasNext()) {
                                BlackPredicate next = iterator.next();
                                root = root.and(next);
                            }
                            return root;
                        }
                        return e -> true;
                    });
            model.embedAllFuture();
            return future;
        }

        @Override
        public CompletableFuture<List<SortKey<E>>> apply(EmbeddingModelClient model, List<SortKey<E>> sortKeys) {
            CompletableFuture<CompletableFuture<List<SortKey<E>>>> f = createPredicate(model, blacklist)
                    .thenApply(rerankPredicate -> {
                        List<SortKey<E>> resultList = filterByKeyVector(sortKeys, rerankPredicate);
                        if (filterKeys == null || filterKeys.isEmpty()) {
                            return CompletableFuture.completedFuture(resultList);
                        }
                        CompletableFuture<List<SortKey<E>>> future = new CompletableFuture<>();
                        Iterator<Function<E, String>> iterator = filterKeys.iterator();
                        class Filter implements Consumer<List<SortKey<E>>> {

                            @Override
                            public void accept(List<SortKey<E>> sortKeyList) {
                                if (iterator.hasNext()) {
                                    Function<E, String> filterKey = iterator.next();
                                    filterByColumnVector(model, sortKeyList, rerankPredicate, filterKey)
                                            .thenAccept(new Filter())
                                            .exceptionally(throwable -> {
                                                future.completeExceptionally(throwable);
                                                return null;
                                            });
                                } else {
                                    future.complete(sortKeyList);
                                }
                            }
                        }
                        new Filter().accept(resultList);
                        return future;
                    });
            return FutureUtil.allOf(f);
        }

        static class BlackPredicate implements Predicate<float[]> {
            final float questionMagnitude;
            final float[] questionVector;
            final QuestionVO vo;

            BlackPredicate(float[] questionVector, QuestionVO vo) {
                this.questionVector = questionVector;
                this.questionMagnitude = calculateMagnitude(questionVector);
                this.vo = vo;
            }

            @Override
            public boolean test(float[] s) {
                return ReRankModelClient.similarity(s, questionVector, questionMagnitude) < vo.similarity;
            }

            @Override
            public String toString() {
                return vo.question;
            }
        }
    }
}
