package com.github.aiassistant.entity.model.chat;

import com.github.aiassistant.util.AiUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 问答知识库
 */
public class QaKnVO extends KnVO {
    private String question;
    private String answer;

    public static <T extends QaKnVO> String resultMapToString(Map<String, List<T>> resultMap) {
        StringJoiner joiner = new StringJoiner("\n");
        Map<String, List<T>> distinctMap = QaKnVO.distinct(resultMap);
        for (Map.Entry<String, List<T>> entry : distinctMap.entrySet()) {
            String q = AiUtil.limit(entry.getKey(), 64, true);
            String a = QaKnVO.answerToString(entry.getValue());
            joiner.add(AiUtil.toAiXmlString(q, a));
        }
        return joiner.toString();
    }

    public static <T extends QaKnVO> String resultMapToQAString(Map<String, List<T>> resultMap) {
        StringJoiner joiner = new StringJoiner("\n");
        Map<String, List<T>> distinctMap = QaKnVO.distinct(resultMap);
        for (Map.Entry<String, List<T>> entry : distinctMap.entrySet()) {
            String q = AiUtil.limit(entry.getKey(), 64, true);
            String a = QaKnVO.questionAnswerToString(entry.getValue());
            joiner.add(AiUtil.toAiXmlString(q, a));
        }
        return joiner.toString();
    }

    public static <T extends QaKnVO> Map<String, List<T>> distinct(Map<String, List<T>> sourceMap) {
        Map<String, Optional<T>> groupByMaxMap = sourceMap.values().stream().flatMap(Collection::stream).collect(Collectors.groupingBy(KnVO::getId, LinkedHashMap::new, Collectors.maxBy(Comparator.comparing(KnVO::getScore))));
        Map<String, List<T>> targetMap = new LinkedHashMap<>();
        sourceMap.forEach((k, v) -> {
            List<T> l = new ArrayList<>();
            for (T row : v) {
                T max = groupByMaxMap.get(row.getId()).orElse(null);
                if (max == null || Objects.equals(max.getScore(), row.getScore())) {
                    l.add(row);
                }
            }
            if (!l.isEmpty()) {
                targetMap.put(k, l);
            }
        });
        return targetMap;
    }

    public static String qaToString(List<List<QaKnVO>> qaKnVOList) {
        if (qaKnVOList == null) {
            return null;
        }
        StringJoiner joiner = new StringJoiner(",\n");
        for (List<QaKnVO> qaKnVOS : qaKnVOList) {
            for (QaKnVO qaKnVO : qaKnVOS) {
                String q = AiUtil.limit(qaKnVO.getQuestion(), 64, true);
                String a = qaKnVO.getAnswer();
                joiner.add(AiUtil.toAiXmlString(q, a));
            }
        }
        return joiner.toString();
    }

    public static <T extends QaKnVO> String questionAnswerToString(List<T> knLibList) {
        if (knLibList == null) {
            return null;
        }
        StringJoiner joiner = new StringJoiner(",\n");
        for (T qaKnVO : knLibList) {
            String q = AiUtil.limit(qaKnVO.getQuestion(), 64, true);
            String a = qaKnVO.getAnswer();
            joiner.add(AiUtil.toAiXmlString(q, a));
        }
        return joiner.toString();
    }

    public static <T extends QaKnVO> String answerToString(List<T> knLibList) {
        if (knLibList == null) {
            return null;
        }
        StringJoiner joiner = new StringJoiner(",\n");
        for (T qaKnVO : knLibList) {
            String a = qaKnVO.getAnswer();
            joiner.add(a);
        }
        return joiner.toString();
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    @Override
    public String toString() {
        return question;
    }
}
