package com.github.aiassistant.service.text.repository;

import com.github.aiassistant.entity.model.chat.QaKnVO;
import com.github.aiassistant.entity.model.chat.QuestionClassifyListVO;
import com.github.aiassistant.entity.model.chat.WebSearchResultVO;
import com.github.aiassistant.enums.AiWebSearchSourceEnum;
import com.github.aiassistant.exception.JsonSchemaCreateException;
import com.github.aiassistant.service.jsonschema.ReasoningJsonSchema;
import com.github.aiassistant.service.text.acting.ActingService;
import com.github.aiassistant.service.text.sseemitter.AiMessageString;
import dev.langchain4j.data.message.ChatMessage;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 会话级别的记忆存储
 */
public interface SessionMessageRepository {
    /**
     * 当jsonSchema都生成好了，会触发这个方法
     *
     * @throws JsonSchemaCreateException 创建Jsonschema出现错误
     */
    default void afterJsonSchemaBuild() throws JsonSchemaCreateException {

    }

    /**
     * 当吐token，会触发这个方法
     *
     * @param token token
     */
    default void afterToken(AiMessageString token) {
    }

    /**
     * 插入用户的提问
     *
     * @param questionList questionList
     * @return 插入成功后
     */
    default CompletableFuture<?> addUserQuestion(List<ChatMessage> questionList) {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 插入消息
     *
     * @param message message
     */
    void add(ChatMessage message);

    /**
     * 插入问题分类
     *
     * @param questionClassify questionClassify
     * @param question         question
     */
    default void addQuestionClassify(QuestionClassifyListVO questionClassify, String question) {
    }

    /**
     * 插入知识库
     *
     * @param qaKnVOList qaKnVOList
     */
    default void addKnowledge(List<List<QaKnVO>> qaKnVOList) {

    }

    /**
     * 插入思考
     *
     * @param question question
     * @param plan     plan
     * @param reason   reason
     * @param parallel parallel
     */
    default void addReasoning(String question, ActingService.Plan plan, ReasoningJsonSchema.Result reason, boolean parallel) {

    }

    /**
     * 插入联网搜索
     *
     * @param sourceEnum   sourceEnum
     * @param providerName providerName
     * @param question     question
     * @param resultVO     resultVO
     * @param cost         cost
     */
    default void addWebSearchRead(AiWebSearchSourceEnum sourceEnum, String providerName, String question, WebSearchResultVO resultVO, long cost) {

    }

    /**
     * 插入错误
     *
     * @param error            error
     * @param baseMessageIndex baseMessageIndex
     * @param addMessageCount  addMessageCount
     * @param generateCount    generateCount
     */
    default void addError(Throwable error, int baseMessageIndex, int addMessageCount, int generateCount) {

    }

    /**
     * 记忆历史记录
     *
     * @return 不为NULL的记忆历史记录
     */
    List<ChatMessage> getHistoryList();

    CompletableFuture<?> commit();

}