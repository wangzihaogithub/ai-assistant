package com.github.aiassistant.service.text.tools;

import com.github.aiassistant.service.text.embedding.EmbeddingModelClient;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class QueryBuilderUtil {

    private static final Map<Class, Set<String>> FIELD_NAME_CACHE = Collections.synchronizedMap(new WeakHashMap<>());
    public static String FIELD_VECTOR_END_WITH = "Vector";
    public static List<String> SOURCE_EXCLUDES_ALL_VECTOR = Collections.singletonList("*" + FIELD_VECTOR_END_WITH);
    public static int KNN_K_MAX = 10000;

    /**
     * 问答
     */
    public static CompletableFuture<Map<String, Object>> buildQaEsQuery(String vectorFieldName, List<String> majorName, EmbeddingModelClient model,
                                                                        Integer size, Double minScore,
                                                                        Collection<String> sourceInclude) {
        return buildKnEsQuery(vectorFieldName, majorName, model, size, minScore, sourceInclude);
    }

    /**
     * 知识库查询
     */
    private static CompletableFuture<Map<String, Object>> buildKnEsQuery(String vectorFieldName, List<String> queryStringList,
                                                                         EmbeddingModelClient model, Integer size, Double minScore,
                                                                         Collection<String> sourceInclude) {
        int sizeNormal = size == null ? 1 : Math.max(1, size);
        Map<String, Object> body = new HashMap<>(3);
        body.put("size", sizeNormal);
        body.put("_source", _source(sourceInclude, sourceInclude != null ? null : SOURCE_EXCLUDES_ALL_VECTOR));
        Collection<String> qList = queryStringList == null ? null : queryStringList.stream().filter(e -> e != null && !e.isEmpty()).collect(Collectors.toList());
        if (qList != null && !qList.isEmpty()) {
            return model.addEmbedList(queryStringList)
                    .thenApply(vectorList -> {
                        Collection<Map<String, Object>> knnList = vectorList.stream()
                                .map(e -> knn(vectorFieldName, e, null, sizeNormal, sizeNormal * 10, minScore, null).toMap())
                                .collect(Collectors.toList());
                        body.put("knn", knnList);
                        return body;
                    });
        } else {
            body.put("query", matchAll());
            return CompletableFuture.completedFuture(body);
        }
    }

    private static Map<String, Object> _source(Collection<String> includes, List<String> excludes) {
        Map<String, Object> source = new HashMap<>(3);
        if (includes != null) {
            source.put("includes", includes);
        }
        if (excludes != null) {
            source.put("excludes", excludes);
        }
        return source;
    }

    public static <T> List<String> getFieldNameList(Class<T> type) {
        return new ArrayList<>(FIELD_NAME_CACHE.computeIfAbsent(type, k -> {
            Set<String> result = new LinkedHashSet<>();
            for (Class<?> clazz = k; clazz != null; clazz = clazz.getSuperclass()) {
                Field[] fields = clazz.getDeclaredFields();
                for (Field field : fields) {
                    result.add(field.getName());
                }
            }
            return Collections.unmodifiableSet(result);
        }));
    }

    private static Map<String, Object> matchAll() {
        return Collections.singletonMap("match_all", Collections.emptyMap());
    }

    private static Map<String, Object> functionScore(Map<String, Object> query, Map<String, Object> scriptScore, Map<String, Object> fieldValueFactor, String boostMode) {
        Map<String, Object> map = new HashMap<>(3);
        map.put("query", query);
        if (scriptScore != null) {
            map.put("script_score", scriptScore);
        }
        if (fieldValueFactor != null) {
            map.put("field_value_factor", fieldValueFactor);
        }
        if (boostMode != null && !boostMode.isEmpty()) {
            map.put("boost_mode", boostMode);
        }
        return Collections.singletonMap("function_score", map);
    }

    private static Map<String, Object> scriptScore(Map<String, Object> query, String source, Map<String, Object> params) {
        Map<String, Object> map = new LinkedHashMap<>(2);
        if (query != null) {
            map.put("query", query);
        }
        Map<String, Object> script = new HashMap<>(2);
        script.put("source", source);
        if (params != null) {
            script.put("params", params);
        }
        map.put("script", script);
        return Collections.singletonMap("script_score", map);
    }

    private static Map<String, Object> constantScore(Object filter, Double boost) {
        Map<String, Object> constantScore = new LinkedHashMap<>(2);
        constantScore.put("filter", filter);
        if (boost != null) {
            constantScore.put("boost", boost);
        }
        return Collections.singletonMap("constant_score", constantScore);
    }

    private static Map<String, Object> intervalsAnyOf(List<Map<String, Object>> intervals) {
        Map<String, Object> map = new LinkedHashMap<>(2);
        map.put("intervals", intervals);
        return Collections.singletonMap("any_of", map);
    }

    private static Map<String, Object> intervalsAllOf(List<Map<String, Object>> intervals, Boolean ordered) {
        Map<String, Object> map = new LinkedHashMap<>(2);
        map.put("intervals", intervals);
        if (ordered != null) {
            map.put("ordered", ordered);
        }
        return Collections.singletonMap("all_of", map);
    }

    private static Map<String, Object> intervalsField(String fieldName, Map<String, Object> intervalsRule) {
        return Collections.singletonMap("intervals", Collections.singletonMap(fieldName, intervalsRule));
    }

    private static Map<String, Object> intervalsMatch(String query, Integer maxGaps, boolean ordered) {
        Map<String, Object> map = new LinkedHashMap<>(2);
        map.put("query", query);
        if (maxGaps != null) {
            map.put("max_gaps", maxGaps);
        }
        map.put("ordered", ordered);
        return Collections.singletonMap("match", map);
    }

    private static Map<String, Object> fieldValueFactor(String field, Double factor, String modifier, Number missing) {
        Map<String, Object> constantScore = new LinkedHashMap<>(2);
        constantScore.put("field", field);
        constantScore.put("factor", factor);
        constantScore.put("modifier", modifier);
        constantScore.put("missing", missing);
        return constantScore;
    }

    private static Knn knn(String fieldName, float[] queryVector, Map<String, Object> filter, int k, int numCandidates, Double similarity, Double boost) {
        Knn knn = new Knn();
        numCandidates = Math.max(10, Math.min(numCandidates, KNN_K_MAX));

        knn.setFilter(filter);
        knn.setSimilarity(similarity);
        knn.setBoost(boost);
        knn.setField(fieldName);
        knn.setQueryVector(queryVector);
        knn.setNumCandidates(numCandidates);
        knn.setK(Math.min(k, numCandidates));
        return knn;
    }

    private static Map<String, Object> mergeQuery(List<Map<String, Object>> and, List<Map<String, Object>> andNot) {
        if (andNot.isEmpty() && and.isEmpty()) {
            return null;
        } else if (andNot.isEmpty()) {
            return and.size() == 1 ? and.get(0) : and(and);
        } else if (and.isEmpty()) {
            return andNot.size() == 1 ? andNot.get(0) : andNot(andNot);
        } else {
            return andAndNot(and, andNot);
        }
    }

    private static Knn knnAnd(List<Knn> knnList, Map<String, Object> filter) {
        Iterator<Knn> knnIterator = knnList.iterator();
        Knn prev = null;
        while (knnIterator.hasNext()) {
            Knn knn = knnIterator.next();
            Map<String, Object> knnFilter;
            if (prev == null) {
                knnFilter = filter;
            } else {
                knnFilter = prev.toKnnMap();
            }
            knn.addFilterAnd(knnFilter);
            prev = knn;
        }
        return prev;
    }

    private static List<String> listToString(List<Integer> idList) {
        return idList.stream().distinct().map(Object::toString).collect(Collectors.toList());
    }

    private static Map<String, Object> or(List<Map<String, Object>> or) {
        return Collections.singletonMap("bool", Collections.singletonMap("should", or));
    }

    private static Map<String, Object> orFilter(List<Map<String, Object>> or, Map<String, Object> filter) {
        Map<String, Object> bool = new HashMap<>(2);
        bool.put("should", or);
        if (filter != null) {
            bool.put("filter", filter);
        }
        return Collections.singletonMap("bool", bool);
    }

    private static Map<String, Object> andFilter(List<Map<String, Object>> or, Map<String, Object> filter) {
        Map<String, Object> bool = new HashMap<>(2);
        bool.put("must", or);
        if (filter != null) {
            bool.put("filter", filter);
        }
        return Collections.singletonMap("bool", bool);
    }

    private static Map<String, Object> and(List<Map<String, Object>> and) {
        return Collections.singletonMap("bool", Collections.singletonMap("must", and));
    }

    private static Map<String, Object> andAndNot(List<Map<String, Object>> and, List<Map<String, Object>> andNot) {
        Map<String, Object> result = new HashMap<>(2);
        result.put("must", and);
        result.put("must_not", andNot);
        return Collections.singletonMap("bool", result);
    }

    private static Map<String, Object> andNot(List<Map<String, Object>> and) {
        return Collections.singletonMap("bool", Collections.singletonMap("must_not", and));
    }

    private static Map<String, Object> match(Object value, String field) {
        Map<String, Object> match = new HashMap<>(2);
        match.put(field, value);
        return Collections.singletonMap("match", match);
    }

    private static Map<String, Object> nested(String path, Map<String, Object> query) {
        Map<String, Object> nested = new HashMap<>(2);
        nested.put("path", path);
        nested.put("query", query);
        return Collections.singletonMap("nested", nested);
    }

    private static Map<String, Object> term(Object value, String field) {
        return Collections.singletonMap("term", Collections.singletonMap(field, Collections.singletonMap("value", value)));
    }

    private static Map<String, Object> range(Object gte, Object lte, String field) {
        Map<String, Object> range = new HashMap<>(2);
        if (gte != null) {
            range.put("gte", gte);
        }
        if (lte != null) {
            range.put("lte", lte);
        }
        return Collections.singletonMap("range", Collections.singletonMap(field, range));
    }

    private static Map<String, Object> wildcard(Object value, String field) {
        return Collections.singletonMap("wildcard", Collections.singletonMap(field, Collections.singletonMap("wildcard", "*" + value + "*")));
    }

    private static Map<String, Object> matchPhrase(Object value, String field, Integer slop, String analyzer) {
        Map<String, Object> map = new HashMap<>(3);
        map.put("query", value);
        if (slop != null) {
            map.put("slop", slop);
        }
        if (analyzer != null && !analyzer.isEmpty()) {
            map.put("analyzer", analyzer);
        }
        return Collections.singletonMap("match_phrase", Collections.singletonMap(field, map));
    }

    private static Map<String, Object> matchPhrasePrefix(Object value, String field) {
        return Collections.singletonMap("match_phrase_prefix", Collections.singletonMap(field, Collections.singletonMap("query", value)));
    }

    private static Map<String, Object> termsSet(List<String> terms, String field, int minimum_should_match) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("terms", terms);
        map.put("minimum_should_match", minimum_should_match);
        return Collections.singletonMap("terms_set", Collections.singletonMap(field, map));
    }

    private static Map<String, Object> terms(Object value, String field) {
        return Collections.singletonMap("terms", Collections.singletonMap(field, value));
    }

    private static List<Map<String, Object>> sort(String... kv) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < kv.length; i += 2) {
            Map<String, Object> sort = new HashMap<>(1);
            sort.put(kv[i], Collections.singletonMap("order", kv[i + 1]));
            list.add(sort);
        }
        return list;
    }

    /**
     * 用于多个相同字段同时KNN时，将单字段权重保持在0～1之间。防止权重过大导致排名靠前。
     */
    private static Double avgBoost(double boost, int count) {
        return Math.round(boost * 100D / (double) count) / 100D;
    }

    // @Data
    public static class Knn {
        private final List<Map<String, Object>> filterAnd = new ArrayList<>();
        private Map<String, Object> filter;
        private Double similarity;
        private Double boost;
        private String field;
        private float[] queryVector;
        private Integer k;
        private Integer numCandidates;

        public List<Map<String, Object>> getFilterAnd() {
            return filterAnd;
        }

        public Map<String, Object> getFilter() {
            return filter;
        }

        public void setFilter(Map<String, Object> filter) {
            this.filter = filter;
        }

        public Double getSimilarity() {
            return similarity;
        }

        public void setSimilarity(Double similarity) {
            this.similarity = similarity;
        }

        public Double getBoost() {
            return boost;
        }

        public void setBoost(Double boost) {
            this.boost = boost;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public float[] getQueryVector() {
            return queryVector;
        }

        public void setQueryVector(float[] queryVector) {
            this.queryVector = queryVector;
        }

        public Integer getK() {
            return k;
        }

        public void setK(Integer k) {
            this.k = k;
        }

        public Integer getNumCandidates() {
            return numCandidates;
        }

        public void setNumCandidates(Integer numCandidates) {
            this.numCandidates = numCandidates;
        }

        @Override
        public String toString() {
            return field;
        }

        public void addFilterAnd(Map<String, Object> filter) {
            if (filter != null) {
                filterAnd.add(filter);
            }
        }

        public Map<String, Object> toKnnMap() {
            return Collections.singletonMap("knn", toMap());
        }

        public Map<String, Object> toMap() {
            Map<String, Object> knn = new HashMap<>(8);
            if (!filterAnd.isEmpty()) {
                List<Map<String, Object>> and = new ArrayList<>(filterAnd);
                if (filter != null) {
                    and.add(filter);
                }
                if (and.size() == 1) {
                    knn.put("filter", and.get(0));
                } else {
                    knn.put("filter", QueryBuilderUtil.and(and));
                }
            } else if (filter != null) {
                knn.put("filter", filter);
            }
            if (similarity != null) {
                knn.put("similarity", similarity);
            }
            if (boost != null) {
                knn.put("boost", boost);
            }
            knn.put("field", field);
            knn.put("query_vector", queryVector);
            knn.put("k", k);
            knn.put("num_candidates", numCandidates);
            return knn;
        }

        public void limit(Integer limit) {
            this.k = limit;
            this.numCandidates = limit;
        }
    }
}
