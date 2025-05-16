package com.github.aiassistant.service.text.rerank;

import com.github.aiassistant.service.text.embedding.EmbeddingModelClient;
import com.github.aiassistant.util.FutureUtil;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class EmbeddingReRankModel implements ReRankModel {
    private static final Map<float[], Float> MAGNITUDE_VECTOR_CACHE = Collections.synchronizedMap(new WeakHashMap<>());
    private final EmbeddingModelClient model;
    private final boolean autoEmbedAllFuture;

    public EmbeddingReRankModel(EmbeddingModelClient embeddingModelClient) {
        this.model = embeddingModelClient;
        this.autoEmbedAllFuture = true;
    }

    public EmbeddingReRankModel(EmbeddingModelClient embeddingModelClient, boolean autoEmbedAllFuture) {
        this.model = embeddingModelClient;
        this.autoEmbedAllFuture = autoEmbedAllFuture;
    }

    private static <E> CompletableFuture<List<SortKeyGroup<E>>> sort(EmbeddingReRankModel model,
                                                                     Map<float[], String> vectorMap,
                                                                     float[] queryVector, Map<String, List<E>> groupByMap,
                                                                     int topN,
                                                                     BiFunction<EmbeddingReRankModel, List<SortKeyGroup<E>>, CompletableFuture<List<SortKeyGroup<E>>>> filter) {
        LinkedList<SortKeyGroup<E>> sortList = new LinkedList<>();
        float magnitudeQueryVector = calculateMagnitude(queryVector);// 向量2的模
        vectorMap.forEach((k, v) -> sortList.add(new SortKeyGroup<>(v, k, similarity(k, queryVector, magnitudeQueryVector), groupByMap.get(v))));
        sortList.sort(Comparator.comparing(e -> e.similarity));

        CompletableFuture<List<SortKeyGroup<E>>> future = new CompletableFuture<>();
        Iterator<SortKeyGroup<E>> iterator = sortList.descendingIterator();
        class SortFunction implements Consumer<List<SortKeyGroup<E>>> {

            @Override
            public void accept(List<SortKeyGroup<E>> sortKeyList) {
                int sum = sortKeyList.stream().mapToInt(e -> e.valueList.size()).sum();
                if (!iterator.hasNext() || sum >= topN) {
                    future.complete(sortKeyList);
                    return;
                }
                List<SortKeyGroup<E>> topNList = new ArrayList<>(sortKeyList);
                int count = sum;
                while (iterator.hasNext()) {
                    SortKeyGroup<E> next = iterator.next();
                    topNList.add(next);
                    count += next.valueList.size();
                    if (count >= topN) {
                        break;
                    }
                }
                filter.apply(model, topNList)
                        .thenAccept(new SortFunction())
                        .exceptionally(throwable -> {
                            future.completeExceptionally(throwable);
                            return null;
                        });
            }
        }
        new SortFunction().accept(Collections.emptyList());
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

    public static <E> BiFunction<EmbeddingReRankModel, List<SortKeyGroup<E>>, CompletableFuture<List<SortKeyGroup<E>>>>
    blackFilter(List<Function<E, String>> filterKeys, List<QuestionVO> blacklist) {
        return new BlackFilter<>(filterKeys, blacklist);
    }

    public EmbeddingModelClient getModel() {
        return model;
    }

    public boolean isAutoEmbedAllFuture() {
        return autoEmbedAllFuture;
    }

    @Override
    public <E, M extends ReRankModel, K extends SortKey<E>> CompletableFuture<List<E>> topN(String query, Collection<E> documents, Function<E, String> reRankKey, int topN, BiFunction<M, List<K>, CompletableFuture<List<K>>> filter) {
        if (documents.size() <= topN) {
            return CompletableFuture.completedFuture(new ArrayList<>(documents));
        }
        BiFunction<EmbeddingReRankModel, List<SortKeyGroup<E>>, CompletableFuture<List<SortKeyGroup<E>>>> filterCast
                = (m, k) -> filter != null ? (CompletableFuture) filter.apply((M) m, (List<K>) k) : CompletableFuture.completedFuture(k);
        return sort(query, documents, reRankKey, topN, filterCast)
                .thenApply(sortKeyList -> sortKeyList.stream().flatMap(e -> e.valueList.stream()).limit(topN).collect(Collectors.toList()));
    }


    public <E> CompletableFuture<List<SortKeyGroup<E>>> sort(String query, Collection<E> datasourceList,
                                                             Function<E, String> reRankKey, int topN,
                                                             BiFunction<EmbeddingReRankModel, List<SortKeyGroup<E>>, CompletableFuture<List<SortKeyGroup<E>>>> filter) {
        Map<String, List<E>> groupByMap = datasourceList.stream().collect(Collectors.groupingBy(reRankKey));
        ArrayList<String> keys = new ArrayList<>();
        keys.add(query);
        keys.addAll(groupByMap.keySet());
        CompletableFuture<List<float[]>> embedList = model.addEmbedList(keys);
        if (isAutoEmbedAllFuture()) {
            embedAllFuture();
        }
        CompletableFuture<CompletableFuture<List<SortKeyGroup<E>>>> f = embedList.thenApply(list -> {
            Map<float[], String> vectorMap = new HashMap<>();
            for (int i = 1; i < list.size(); i++) {
                vectorMap.put(list.get(i), keys.get(i));
            }
            float[] queryVector = list.get(0);
            return sort(this, vectorMap, queryVector, groupByMap, topN, filter);
        });
        return FutureUtil.allOf(f);
    }

    public List<? extends CompletableFuture<List<float[]>>> embedAllFuture() {
        return model.embedAllFuture();
    }

    public static class SortKeyGroup<E> implements SortKey<E> {
        private final String key;
        private final float[] KeyVector;
        private final float similarity;
        private final List<E> valueList;

        private SortKeyGroup(String key, float[] keyVector, float similarity, List<E> valueList) {
            this.key = key;
            KeyVector = keyVector;
            this.similarity = similarity;
            this.valueList = valueList;
        }

        @Override
        public String getKey() {
            return key;
        }

        public float[] getKeyVector() {
            return KeyVector;
        }

        @Override
        public float getSimilarity() {
            return similarity;
        }

        @Override
        public List<E> getValueList() {
            return valueList;
        }

        public SortKeyGroup<E> fork(List<E> valueList) {
            return new SortKeyGroup<>(key, KeyVector, similarity, valueList);
        }

        @Override
        public String toString() {
            return "(" + Math.round(similarity * 100) * 0.01 + ")" + getKey();
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

    private static class BlackFilter<E> implements BiFunction<EmbeddingReRankModel, List<SortKeyGroup<E>>, CompletableFuture<List<SortKeyGroup<E>>>> {
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
            List<E> list = new ArrayList<>(valueList.size());
            for (E row : valueList) {
                float[] floats = vectorMap.get(getter.apply(row));
                if (rerankPredicate.test(floats)) {
                    list.add(row);
                }
            }
            return list;
        }

        private static <E> CompletableFuture<List<SortKeyGroup<E>>> filterByColumnVector(
                EmbeddingModelClient model,
                List<SortKeyGroup<E>> sortKeys,
                Predicate<float[]> rerankPredicate,
                Function<E, String> getter) {
            List<String> stringMap = sortKeys.stream().map(e -> e.valueList).flatMap(Collection::stream).map(getter).collect(Collectors.toList());
            CompletableFuture<List<SortKeyGroup<E>>> future = model.addEmbedList(stringMap).thenApply(list -> {
                Map<String, float[]> vectorMap = new HashMap<>();
                for (int i = 0; i < list.size(); i++) {
                    vectorMap.put(stringMap.get(i), list.get(i));
                }
                return sortKeys.stream()
                        .map(sortKey -> {
                            List<E> filter = filterTest(rerankPredicate, sortKey.valueList, getter, vectorMap);
                            return sortKey.fork(filter);
                        })
                        .collect(Collectors.toList());
            });
            model.embedAllFuture();
            return future;
        }

        private static CompletableFuture<Predicate<float[]>> createPredicate(EmbeddingReRankModel model, List<QuestionVO> blacklist) {
            CompletableFuture<Predicate<float[]>> future = model.model.addEmbedList(blacklist.stream().map(e -> e.question).collect(Collectors.toList())).thenApply(list -> {
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
        public CompletableFuture<List<SortKeyGroup<E>>> apply(EmbeddingReRankModel model, List<SortKeyGroup<E>> sortKeys) {
            CompletableFuture<CompletableFuture<List<SortKeyGroup<E>>>> f = createPredicate(model, blacklist)
                    .thenApply(rerankPredicate -> {
                        List<SortKeyGroup<E>> resultList = sortKeys.stream().filter(e -> rerankPredicate.test(e.KeyVector)).collect(Collectors.toList());
                        if (filterKeys == null || filterKeys.isEmpty()) {
                            return CompletableFuture.completedFuture(resultList);
                        }
                        CompletableFuture<List<SortKeyGroup<E>>> future = new CompletableFuture<>();
                        Iterator<Function<E, String>> iterator = filterKeys.iterator();
                        class IteratorFutureFunction implements Consumer<List<SortKeyGroup<E>>> {

                            @Override
                            public void accept(List<SortKeyGroup<E>> sortKeyList) {
                                if (iterator.hasNext()) {
                                    Function<E, String> filterKey = iterator.next();
                                    filterByColumnVector(model.model, sortKeyList, rerankPredicate, filterKey)
                                            .thenAccept(new IteratorFutureFunction())
                                            .exceptionally(throwable -> {
                                                future.completeExceptionally(throwable);
                                                return null;
                                            });
                                } else {
                                    future.complete(sortKeyList);
                                }
                            }
                        }
                        new IteratorFutureFunction().accept(resultList);
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
                return EmbeddingReRankModel.similarity(s, questionVector, questionMagnitude) < vo.similarity;
            }

            @Override
            public String toString() {
                return vo.question;
            }
        }
    }
}
