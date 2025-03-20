package com.github.aiassistant.service.text.repository;

import com.github.aiassistant.entity.model.chat.QaKnVO;
import com.github.aiassistant.entity.model.chat.QuestionClassifyListVO;
import com.github.aiassistant.entity.model.chat.WebSearchResultVO;
import com.github.aiassistant.service.text.acting.ActingService;
import com.github.aiassistant.service.jsonschema.ReasoningJsonSchema;
import dev.langchain4j.data.message.ChatMessage;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 会话级别的记忆存储
 */
public interface SessionMessageRepository {
    /**
     * 当jsonSchema都生成好了，会触发这个方法
     */
    default void afterJsonSchemaBuild() {

    }

    /**
     * 当吐token，会触发这个方法
     */
    default void afterToken(String token) {
    }

    /**
     * 插入用户的提问
     */
    default CompletableFuture<?> addUserQuestion(List<ChatMessage> questionList) {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 插入消息
     */
    void add(ChatMessage message);

    /**
     * 插入问题分类
     */
    default void addQuestionClassify(QuestionClassifyListVO questionClassify, String question) {
    }

    /**
     * 插入知识库
     */
    default void addKnowledge(List<List<QaKnVO>> qaKnVOList) {

    }

    /**
     * 插入思考
     */
    default void addReasoning(String question, ActingService.Plan plan, ReasoningJsonSchema.Result reason, boolean parallel) {

    }

    /**
     * 插入联网搜索
     */
    default void addWebSearchRead(String sourceEnum, String providerName, String question, WebSearchResultVO resultVO, long cost) {

    }

    /**
     * 插入错误
     */
    default void addError(Throwable error, int baseMessageIndex, int addMessageCount, int generateCount) {

    }

    List<ChatMessage> getHistoryList();

    CompletableFuture<?> commit();

}