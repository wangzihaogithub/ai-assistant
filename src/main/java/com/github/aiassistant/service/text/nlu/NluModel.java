package com.github.aiassistant.service.text.nlu;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 自然语言理解(Natural Language Understanding, NLU)是所有支持机器理解文本内容的方法模型或任务的总称。
 * NLU在文本信息处理处理系统中扮演着非常重要的角色，是推荐、问答、搜索等系统的必备模块。
 * 3种典型任务：文本分类、文本聚类、命名实体识别
 */
public interface NluModel {

    /**
     * 对象实例销毁
     */
    default void destroy() {

    }

    /**
     * 文本分类
     * 主题分类：体育新闻，娱乐新闻
     * 情感分类：积极，消极
     *
     * @param document 文本
     * @param labels   可选分类
     * @return 选择分类
     */
    CompletableFuture<List<String>> classification(
            String document, Collection<String> labels);

    /**
     * 抽取任务
     * 实体识别：人名，地名
     * 阅读理解：小明的年龄是多少？，小明的身高是多少？
     *
     * @param document 文本
     * @param labels   可选分类
     * @return 选择分类
     */
    CompletableFuture<List<String>> extraction(
            String document, Collection<String> labels);

}
