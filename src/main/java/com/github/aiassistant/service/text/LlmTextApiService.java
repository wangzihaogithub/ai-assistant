package com.github.aiassistant.service.text;

import com.github.aiassistant.dao.AiAssistantFewshotMapper;
import com.github.aiassistant.dao.AiAssistantJsonschemaMapper;
import com.github.aiassistant.entity.AiAssistantKn;
import com.github.aiassistant.entity.model.chat.*;
import com.github.aiassistant.entity.model.user.AiAccessUserVO;
import com.github.aiassistant.enums.AiAssistantKnTypeEnum;
import com.github.aiassistant.enums.AiWebSearchSourceEnum;
import com.github.aiassistant.exception.AssistantConfigException;
import com.github.aiassistant.exception.FewshotConfigException;
import com.github.aiassistant.exception.QuestionEmptyException;
import com.github.aiassistant.service.jsonschema.LlmJsonSchemaApiService;
import com.github.aiassistant.service.jsonschema.ReasoningJsonSchema;
import com.github.aiassistant.service.jsonschema.WebsearchReduceJsonSchema;
import com.github.aiassistant.service.text.acting.ActingService;
import com.github.aiassistant.service.text.embedding.EmbeddingModelClient;
import com.github.aiassistant.service.text.embedding.KnSettingWebsearchBlacklistServiceImpl;
import com.github.aiassistant.service.text.embedding.KnnApiService;
import com.github.aiassistant.service.text.embedding.ReRankModelClient;
import com.github.aiassistant.service.text.reasoning.ReasoningService;
import com.github.aiassistant.service.text.repository.ConsumerTokenWindowChatMemory;
import com.github.aiassistant.service.text.repository.JsonSchemaTokenWindowChatMemory;
import com.github.aiassistant.service.text.repository.SessionMessageRepository;
import com.github.aiassistant.service.text.sseemitter.AiMessageString;
import com.github.aiassistant.service.text.sseemitter.SseHttpResponse;
import com.github.aiassistant.service.text.tools.AiToolServiceImpl;
import com.github.aiassistant.service.text.tools.QueryBuilderUtil;
import com.github.aiassistant.service.text.tools.Tools;
import com.github.aiassistant.service.text.tools.WebSearchService;
import com.github.aiassistant.service.text.variables.AiVariablesService;
import com.github.aiassistant.serviceintercept.LlmTextApiServiceIntercept;
import com.github.aiassistant.util.*;
import dev.ai4j.openai4j.chat.ResponseFormatType;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.Tokenizer;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Text类型的聊天模型服务
 */
public class LlmTextApiService {
    public static final String MESSAGE_TEXT_BLACK_LIST_QUESTION = "根据相关规定，我无法回答这个问题，换个话题吧。";
    private static final Logger log = LoggerFactory.getLogger(LlmTextApiService.class);
    /**
     * 估计各种文本类型（如文本、提示、文本段等）中的标记计数的接口
     */
    private final Tokenizer tokenizer = new OpenAiTokenizer();
    /**
     * Text类型的聊天模型
     */
    private final Map<String, AiModel[]> modelMap = new ConcurrentHashMap<>();
    /**
     * 每个智能体的聊天模型并发数量
     */
    private final int concurrentChatModelCount = 10;
    private final WebSearchService webSearchService = new WebSearchService();
    /**
     * JsonSchema类型的模型
     */
    private final LlmJsonSchemaApiService llmJsonSchemaApiService;
    private final AiQuestionClassifyService aiQuestionClassifyService;
    /**
     * AI变量服务
     */
    private final AiVariablesService aiVariablesService;
    private final AiToolServiceImpl aiToolService;
    private final AiAssistantJsonschemaMapper aiAssistantJsonschemaMapper;
    // @Resource
    private final AiAssistantFewshotMapper aiAssistantFewshotMapper;
    /**
     * 向量模型服务
     */
    private final KnnApiService knnApiService;
    private final Executor threadPoolTaskExecutor;
    private final ActingService actingService;
    private final ReasoningService reasoningService;
    private final KnSettingWebsearchBlacklistServiceImpl knSettingWebsearchBlacklistService;
    private final Supplier<Collection<LlmTextApiServiceIntercept>> interceptList;
    /**
     * 最大联网兜底条数
     */
    public int maxActingWebSearch = 20;
    /**
     * 最大简单联网条数
     */
    public int maxSimpleWebSearch = 10;
    /**
     * 是否并行执行，思考的子问题（注：并行不能携带上一个子问题的执行结果,如果子问题需要依赖上一个子问题的执行结果，需要改为false）
     */
    public boolean reasoningAndActingParallel = true;

    public LlmTextApiService(LlmJsonSchemaApiService llmJsonSchemaApiService,
                             AiQuestionClassifyService aiQuestionClassifyService,
                             AiAssistantJsonschemaMapper aiAssistantJsonschemaMapper,
                             AiAssistantFewshotMapper aiAssistantFewshotMapper,
                             AiToolServiceImpl aiToolService,
                             AiVariablesService aiVariablesService,
                             KnnApiService knnApiService,
                             ActingService actingService, ReasoningService reasoningService,
                             KnSettingWebsearchBlacklistServiceImpl knSettingWebsearchBlacklistService, int threads,
                             Supplier<Collection<LlmTextApiServiceIntercept>> interceptList) {
        ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(
                threads, threads,
                60, TimeUnit.SECONDS,
                new SynchronousQueue<>(), target -> {
            Thread thread = new Thread(target);
            thread.setName("Ai-Question-" + thread.getId());
            thread.setDaemon(true);
            return thread;
        }, new ThreadPoolExecutor.CallerRunsPolicy());
        this.aiAssistantFewshotMapper = aiAssistantFewshotMapper;
        this.aiToolService = aiToolService;
        this.aiAssistantJsonschemaMapper = aiAssistantJsonschemaMapper;
        this.llmJsonSchemaApiService = llmJsonSchemaApiService;
        this.aiQuestionClassifyService = aiQuestionClassifyService;
        this.aiVariablesService = aiVariablesService;
        this.knnApiService = knnApiService;
        this.threadPoolTaskExecutor = poolExecutor;
        this.actingService = actingService;
        this.reasoningService = reasoningService;
        this.knSettingWebsearchBlacklistService = knSettingWebsearchBlacklistService;
        this.interceptList = interceptList;
    }

    private static List<ChatMessage> mergeMessageList(ChatMessage userMessage, ChatMessage knowledge, ChatMessage mstate) {
        List<ChatMessage> messageList = new ArrayList<>();
        messageList.add(userMessage);
        messageList.add(mstate);
        messageList.add(knowledge);
        return messageList.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    private static String uniqueKey(String apiKey,
                                    String baseUrl,
                                    String modelName,
                                    Double temperature,
                                    Integer maxCompletionTokens) {
        return apiKey + ":" + baseUrl + ":" + modelName + ":" + temperature + ":" + maxCompletionTokens;
    }

    private static String getLastUserQuestion(List<ChatMessage> historyList) {
        for (int i = historyList.size() - 1; i >= 0; i--) {
            ChatMessage message = historyList.get(i);
            if (message instanceof UserMessage) {
                return AiUtil.userMessageToString((UserMessage) message);
            }
        }
        return null;
    }

    /**
     * 是否开启思考
     *
     * @param reasoning        reasoning
     * @param assistantConfig  assistantConfig
     * @param knPromptText     knPromptText
     * @param mstatePromptText mstatePromptText
     * @param lastQuery        lastQuery
     * @return 开启思考
     */
    private static boolean isEnableReasoning(Boolean reasoning, AssistantConfig assistantConfig, String knPromptText, String mstatePromptText, String lastQuery) {
        return Boolean.TRUE.equals(reasoning)
                && AiVariablesService.isEnableReasoning(assistantConfig, knPromptText, mstatePromptText)
                && Objects.toString(lastQuery, "").length() > 2;
    }

    /**
     * 是否开启联网
     *
     * @param websearch        websearch
     * @param assistantConfig  assistantConfig
     * @param knPromptText     knPromptText
     * @param mstatePromptText mstatePromptText
     * @param lastQuery        lastQuery
     * @return 是否开启联网
     */
    private static boolean isEnableWebSearch(Boolean websearch, AssistantConfig assistantConfig, String knPromptText, String mstatePromptText, String lastQuery) {
        return Boolean.TRUE.equals(websearch)
                && AiVariablesService.isEnableWebSearch(assistantConfig, knPromptText, mstatePromptText)
                && Objects.toString(lastQuery, "").length() > 2;
    }

    /**
     * 提问报错了
     *
     * @param e               异常
     * @param memoryId        memoryId
     * @param responseHandler responseHandler
     * @return 持久化异常
     */
    private static CompletableFuture<FunctionCallStreamingResponseHandler> questionError(Throwable e, MemoryIdVO memoryId, ChatStreamingResponseHandler responseHandler) {
        CompletableFuture<FunctionCallStreamingResponseHandler> handlerFuture = new CompletableFuture<>();
        handlerFuture.completeExceptionally(e);
        responseHandler.onError(e, 0, 0, 0);
        log.error("llm questionError chatId {}, error {}", memoryId.getChatId(), e.toString(), e);
        return handlerFuture;
    }

    public int getMaxActingWebSearch() {
        return maxActingWebSearch;
    }

    public void setMaxActingWebSearch(int maxActingWebSearch) {
        this.maxActingWebSearch = maxActingWebSearch;
    }

    public int getMaxSimpleWebSearch() {
        return maxSimpleWebSearch;
    }

    public void setMaxSimpleWebSearch(int maxSimpleWebSearch) {
        this.maxSimpleWebSearch = maxSimpleWebSearch;
    }

    public boolean isReasoningAndActingParallel() {
        return reasoningAndActingParallel;
    }

    public void setReasoningAndActingParallel(boolean reasoningAndActingParallel) {
        this.reasoningAndActingParallel = reasoningAndActingParallel;
    }

    /**
     * 提问
     *
     * @param user            user
     * @param repository      repository
     * @param question        question
     * @param websearch       websearch
     * @param reasoning       reasoning
     * @param memoryId        memoryId
     * @param responseHandler userResponseHandler
     * @return 提问结果
     */
    public CompletableFuture<FunctionCallStreamingResponseHandler> question(AiAccessUserVO user,
                                                                            SessionMessageRepository repository,
                                                                            String question,
                                                                            Boolean websearch,
                                                                            Boolean reasoning,
                                                                            MemoryIdVO memoryId,
                                                                            ChatStreamingResponseHandler responseHandler) {
        try {
            // jsonschema模型
            llmJsonSchemaApiService.addSessionJsonSchema(memoryId, memoryId.getAiAssistant().getAiJsonschemaIds(), aiAssistantJsonschemaMapper);
            // 持久化
            ChatStreamingResponseHandler mergeResponseHandler = new MergeChatStreamingResponseHandler(
                    Arrays.asList(responseHandler, new RepositoryChatStreamingResponseHandler(repository)),
                    responseHandler);
            // 历史记录
            Collection<ChatMessage> hl = repository.getHistoryList();
            List<ChatMessage> historyList = AiUtil.removeSystemMessage(hl);
            int baseMessageIndex = historyList.size();//起始消息下标
            int addMessageCount = 1;// 为什么是1？因为第0个是内置的SystemMessage，所以至少要有一个。
            // 当前问题，如果是重新回答需要获取最后一次问题getLastUserQuestion
            String lastQuestion = StringUtils.hasText(question) ? question : getLastUserQuestion(historyList);
            if (!StringUtils.hasText(lastQuestion)) {
                throw new QuestionEmptyException("user question is empty!");
            }
            // 初始化
            mergeResponseHandler.onTokenBegin(baseMessageIndex, addMessageCount, 0);
            // 查询变量
            AiVariables variables = aiVariablesService.selectVariables(user, historyList, lastQuestion, memoryId, websearch);
            // 绑定会话钩子
            llmJsonSchemaApiService.putSessionHandler(memoryId, mergeResponseHandler, variables, user);
            // 进行问题分类
            CompletableFuture<QuestionClassifyListVO> classifyFuture = aiQuestionClassifyService.classify(lastQuestion, memoryId);
            // 构建
            CompletableFuture<CompletableFuture<FunctionCallStreamingResponseHandler>> f = classifyFuture
                    .thenApply(c -> buildHandler(c, user, repository, question, websearch,
                            reasoning, memoryId, mergeResponseHandler, historyList, baseMessageIndex, addMessageCount, lastQuestion, variables));
            // 等待完毕
            return FutureUtil.allOf(f)
                    // 完毕后删除JsonSchema的本地记忆
                    .whenComplete((handler, throwable) -> removeJsonSchemaSession(handler, throwable, memoryId));
        } catch (Throwable e) {
            // 提问报错了
            removeJsonSchemaSession(null, e, memoryId);
            return questionError(e, memoryId, responseHandler);
        }
    }

    /**
     * 完毕后删除JsonSchema的本地记忆
     *
     * @param handler   handler
     * @param throwable throwable
     * @param memoryId  memoryId
     */
    private void removeJsonSchemaSession(FunctionCallStreamingResponseHandler handler, Throwable throwable, MemoryIdVO memoryId) {
        if (handler == null || throwable != null) {
            llmJsonSchemaApiService.removeSession(memoryId);
        } else {
            handler.whenComplete((unused, throwable1) -> llmJsonSchemaApiService.removeSession(memoryId));
        }
    }

    /**
     * 构建
     *
     * @param classifyListVO   classifyListVO
     * @param user             user
     * @param repository       repository
     * @param question         question
     * @param websearch        websearch
     * @param reasoning        reasoning
     * @param memoryId         memoryId
     * @param responseHandler  responseHandler
     * @param historyList      historyList
     * @param baseMessageIndex baseMessageIndex
     * @param addMessageCount  addMessageCount
     * @param lastQuestion     lastQuestion
     * @param variables        variables
     * @return 构建结果
     */
    private CompletableFuture<FunctionCallStreamingResponseHandler> buildHandler(
            QuestionClassifyListVO classifyListVO,
            AiAccessUserVO user,
            SessionMessageRepository repository,
            String question,
            Boolean websearch,
            Boolean reasoning,
            MemoryIdVO memoryId,
            ChatStreamingResponseHandler responseHandler,
            List<ChatMessage> historyList,
            int baseMessageIndex,
            int addMessageCount,
            String lastQuestion,
            AiVariables variables) {
        CompletableFuture<FunctionCallStreamingResponseHandler> handlerFuture = new CompletableFuture<>();
        try {
            AssistantConfig assistantConfig = AssistantConfig.select(memoryId.getAiAssistant(), classifyListVO.getClassifyAssistant());
            String knPromptText = assistantConfig.getKnPromptText();
            String mstatePromptText = assistantConfig.getMstatePromptText();

            // 问题分类完成通知
            responseHandler.onQuestionClassify(classifyListVO, lastQuestion, variables);
            aiVariablesService.setterQuestionClassifyResult(variables.getQuestionClassify(), classifyListVO.getClassifyResult());
            int historySumLength = AiUtil.sumLength(historyList);

            // 准备就绪后，是否需要中断后续流程
            Collection<LlmTextApiServiceIntercept> intercepts = interceptList.get();
            Function<FunctionCallStreamingResponseHandler, CompletableFuture<Void>> interceptRepository =
                    interceptRepository(user, memoryId, classifyListVO, variables, websearch, reasoning, responseHandler, historyList, question, lastQuestion, intercepts);
            Function<FunctionCallStreamingResponseHandler, CompletableFuture<Void>> interceptQuestion =
                    interceptRepository == null ?
                            interceptQuestion(user, memoryId, classifyListVO, variables, websearch, reasoning, responseHandler, historyList, question, lastQuestion, intercepts) : null;
            boolean interrupt = interceptRepository != null || interceptQuestion != null;

            // 1.联网搜索
            CompletableFuture<String> webSearchResult;
            if (!interrupt && classifyListVO.isJdlw() && isEnableWebSearch(websearch, assistantConfig, knPromptText, mstatePromptText, lastQuestion)) {
                int maxCharLength = assistantConfig.getMaxMemoryTokens() - historySumLength;
                CompletableFuture<CompletableFuture<WebSearchResultVO>> wf =
                        webSearchService.webSearchRead(lastQuestion, 1, maxCharLength, false, responseHandler.adapterWebSearch(AiWebSearchSourceEnum.LlmTextApiService)).thenApply(s -> {
                            return s.isEmpty() ? CompletableFuture.completedFuture(s) : reduceWebSearch(Collections.singletonList(s), lastQuestion, memoryId, maxSimpleWebSearch);
                        });
                webSearchResult = FutureUtil.allOf(wf).thenApply(resultVO -> {
                    String xmlString = AiUtil.toAiXmlString(lastQuestion, WebSearchResultVO.toSimpleAiString(resultVO));
                    variables.getKn().setWebSearchResult(xmlString);
                    return xmlString;
                });
            } else {
                webSearchResult = CompletableFuture.completedFuture(null);
            }
            // 2.查询知识库
            CompletableFuture<List<List<QaKnVO>>> knnFuture = classifyListVO.isQa() ? selectKnList(assistantConfig, knPromptText, memoryId.getAssistantKnList(AiAssistantKnTypeEnum.qa), lastQuestion) : CompletableFuture.completedFuture(Collections.emptyList());
            // 3.思考
            CompletableFuture<ActingService.Plan> reasoningResultFuture;
            boolean reasoningAndActing = !interrupt && classifyListVO.isWtcj() && isEnableReasoning(reasoning, assistantConfig, knPromptText, mstatePromptText, lastQuestion);
            if (reasoningAndActing) {
                reasoningResultFuture = reasoningAndActing(knnFuture, webSearchResult, lastQuestion, memoryId, reasoningAndActingParallel, responseHandler, websearch, classifyListVO);
            } else {
                reasoningResultFuture = CompletableFuture.completedFuture(null);
            }
            responseHandler.onBeforeReasoningAndActing(reasoningAndActing);

            // 等待结果
            FutureUtil.allOf(webSearchResult, knnFuture, reasoningResultFuture)
                    .thenAccept(unused -> {
                        ActingService.Plan reasoningResult = reasoningResultFuture.getNow(null);
                        List<List<QaKnVO>> qaKnVOList = knnFuture.getNow(null);
                        aiVariablesService.setterKn(variables.getKn(), qaKnVOList, webSearchResult.getNow(null), reasoningResult);
                        // 事件通知
                        responseHandler.onVariables(variables);
                        responseHandler.onKnowledge(qaKnVOList, lastQuestion);

                        // ToolCallStreamingResponseHandler
                        FunctionCallStreamingResponseHandler handler;
                        try {
                            handler = newFunctionCallStreamingResponseHandler(
                                    classifyListVO,
                                    assistantConfig,
                                    mstatePromptText,
                                    knPromptText,
                                    user,
                                    variables,
                                    qaKnVOList,
                                    interceptRepository != null ? null : repository,
                                    question,
                                    memoryId,
                                    responseHandler,
                                    lastQuestion,
                                    historyList,
                                    addMessageCount,
                                    baseMessageIndex
                            );
                        } catch (AssistantConfigException | FewshotConfigException e) {
                            handlerFuture.completeExceptionally(e);
                            responseHandler.onError(e, baseMessageIndex, addMessageCount, 0);
                            return;
                        }
                        // 完毕前
                        responseHandler.onBeforeQuestionLlm(lastQuestion);
                        // 无法回答
                        if (classifyListVO.isWfhd()) {
                            SseHttpResponse response = handler.toResponse();
                            responseHandler.onBlacklistQuestion(response, lastQuestion, classifyListVO);
                            if (response.isEmpty()) {
                                response.close(MESSAGE_TEXT_BLACK_LIST_QUESTION);
                            }
                        }
                        CompletableFuture<Void> interruptAfter = null;
                        if (interceptRepository != null) {
                            interruptAfter = interceptRepository.apply(handler);
                        } else if (interceptQuestion != null) {
                            interruptAfter = interceptQuestion.apply(handler);
                        }
                        // 构建完毕
                        if (interruptAfter != null) {
                            interruptAfter.thenAccept(unused1 -> handlerFuture.complete(handler));
                        } else {
                            handlerFuture.complete(handler);
                        }
                    })
                    .exceptionally(throwable -> {
                        handlerFuture.completeExceptionally(throwable);
                        responseHandler.onError(throwable, baseMessageIndex, addMessageCount, 0);
                        return null;
                    });
        } catch (Exception e) {
            handlerFuture.completeExceptionally(e);
            responseHandler.onError(e, baseMessageIndex, addMessageCount, 0);
            log.error("llm question chatId {}, error {}", memoryId.getChatId(), e.toString(), e);
        }
        return handlerFuture;
    }

    private Function<FunctionCallStreamingResponseHandler, CompletableFuture<Void>> interceptRepository(
            AiAccessUserVO user,
            MemoryIdVO memoryId,
            QuestionClassifyListVO classifyListVO,
            AiVariables variables,
            Boolean websearch,
            Boolean reasoning,
            ChatStreamingResponseHandler responseHandler,
            List<ChatMessage> historyList,
            String question,
            String lastQuestion,
            Collection<LlmTextApiServiceIntercept> intercepts) {
        for (LlmTextApiServiceIntercept intercept : intercepts) {
            Function<FunctionCallStreamingResponseHandler, CompletableFuture<Void>> function = intercept.interceptRepository(user, memoryId, classifyListVO, variables, websearch, reasoning, responseHandler, historyList, question, lastQuestion);
            if (function != null) {
                return function;
            }
        }
        return null;
    }

    private Function<FunctionCallStreamingResponseHandler, CompletableFuture<Void>> interceptQuestion(
            AiAccessUserVO user,
            MemoryIdVO memoryId,
            QuestionClassifyListVO classifyListVO,
            AiVariables variables,
            Boolean websearch,
            Boolean reasoning,
            ChatStreamingResponseHandler responseHandler,
            List<ChatMessage> historyList,
            String question,
            String lastQuestion,
            Collection<LlmTextApiServiceIntercept> intercepts) {
        for (LlmTextApiServiceIntercept intercept : intercepts) {
            Function<FunctionCallStreamingResponseHandler, CompletableFuture<Void>> function = intercept.interceptQuestion(user, memoryId, classifyListVO, variables, websearch, reasoning, responseHandler, historyList, question, lastQuestion);
            if (function != null) {
                return function;
            }
        }
        return null;
    }

    private FunctionCallStreamingResponseHandler newFunctionCallStreamingResponseHandler(
            QuestionClassifyListVO classifyListVO,
            AssistantConfig assistantConfig,
            String mstatePromptText,
            String knPromptText,
            AiAccessUserVO user,
            AiVariables variables,
            List<List<QaKnVO>> qaKnVOList,
            SessionMessageRepository repository,
            String question,
            MemoryIdVO memoryId,
            ChatStreamingResponseHandler responseHandler,
            String lastQuestion,
            List<ChatMessage> historyList,
            int rootAddMessageCount,
            int baseMessageIndex
    ) throws AssistantConfigException, FewshotConfigException {
        // jsonschema模型
        llmJsonSchemaApiService.addSessionJsonSchema(memoryId, assistantConfig.getAiJsonschemaIds(), aiAssistantJsonschemaMapper);

        // 系统消息
        SystemMessage systemMessage = buildSystemMessage(assistantConfig.getSystemPromptText(), responseHandler, variables, assistantConfig);
        // 少样本学习
        List<ChatMessage> fewshotMessageList = AiUtil.deserializeFewshot(aiAssistantFewshotMapper.selectListByAssistantId(memoryId.getAiAssistantId()), variables);
        AiUtil.addToHistoryList(historyList, systemMessage, fewshotMessageList);

        // 记忆
        ConsumerTokenWindowChatMemory chatMemory = new ConsumerTokenWindowChatMemory(memoryId, assistantConfig.getMaxMemoryTokens(), assistantConfig.getMaxMemoryRounds(),
                tokenizer, historyList, repository == null ? null : repository::add);
        // JsonSchema
        llmJsonSchemaApiService.putSessionMemory(memoryId, new JsonSchemaTokenWindowChatMemory(chatMemory, systemMessage, fewshotMessageList));
        if (repository != null) {
            repository.afterJsonSchemaBuild();
        }

        // 用户消息
        ChatMessage userMessage = StringUtils.hasText(question) ? new UserMessage(question) : null;
        // 知识库消息
        ChatMessage knowledge = buildKnowledge(variables, lastQuestion, qaKnVOList, knPromptText, assistantConfig);
        // 记忆消息
        ChatMessage mstate = knowledge != null ? null : buildMstate(variables, mstatePromptText, assistantConfig);

        // 合并消息请求大模型
        List<ChatMessage> questionList = mergeMessageList(userMessage, knowledge, mstate);
        int addMessageCount = questionList.size() + rootAddMessageCount;
        questionList.forEach(chatMemory::add);

        // 插入用户的提问
        if (repository != null) {
            repository.addUserQuestion(questionList).exceptionally(throwable -> {
                // 插入失败，返回错误消息
                responseHandler.onError(throwable, baseMessageIndex, addMessageCount, 0);
                return null;
            });
        }

        // 获取模型
        AiModel aiModel = memoryId.indexAt(getModels(assistantConfig));
        // 可用工具集
        List<Tools.ToolMethod> toolMethodList = AiUtil.initTool(aiToolService.selectToolMethodList(StringUtils.split(assistantConfig.getAiToolIds(), ",")), variables, user);
        // 处理异步回调
        return new FunctionCallStreamingResponseHandler(aiModel.modelName, aiModel.streaming, chatMemory, responseHandler,
                llmJsonSchemaApiService, toolMethodList, aiModel.isSupportChineseToolName(),
                baseMessageIndex, addMessageCount, classifyListVO.getReadTimeoutMs(), threadPoolTaskExecutor);
    }

    private CompletableFuture<ActingService.Plan> reasoningAndActing(CompletableFuture<List<List<QaKnVO>>> knnFuture,
                                                                     CompletableFuture<String> webSearchResult,
                                                                     String question, MemoryIdVO memoryIdVO,
                                                                     boolean parallel,
                                                                     ChatStreamingResponseHandler responseHandler,
                                                                     Boolean websearch,
                                                                     QuestionClassifyListVO classifyListVO) {
        CompletableFuture<CompletableFuture<ActingService.Plan>> future = knnFuture
                .handle((qaList, throwable) -> {
                    if (throwable != null) {
                        return CompletableFuture.completedFuture(null);
                    } else if (qaList != null && !qaList.isEmpty()) {
                        return CompletableFuture.completedFuture(null);
                    } else {
                        responseHandler.onBeforeReasoning(question, parallel);
                        ReasoningJsonSchema.Result[] result0 = new ReasoningJsonSchema.Result[1];
                        CompletableFuture<CompletableFuture<CompletableFuture<ActingService.Plan>>> f =
                                webSearchResult.thenApply(s -> reasoningService.makePlan(question, memoryIdVO)
                                        .thenApply(result -> {
                                            if (result != null) {
                                                responseHandler.onReasoning(question, result);
                                            }
                                            result0[0] = result;
                                            return result;
                                        })
                                        .thenApply(result -> {
                                            ActingService.Plan plan = actingService.toPlan(result);
                                            // 开联网 并且【多层联网】判断原问题是否需要联网
                                            if (Boolean.TRUE.equals(websearch) && classifyListVO.isDclw()) {
                                                // 执行子问题
                                                return actingService.executeTask(plan, question, memoryIdVO, parallel, responseHandler, classifyListVO.isLwdd());
                                            } else {
                                                return CompletableFuture.completedFuture(plan);
                                            }
                                        }));
                        CompletableFuture<CompletableFuture<ActingService.Plan>> f1 = FutureUtil.allOf(FutureUtil.allOf(f))
                                .thenApply(plan -> {
                                    if (plan != null) {
                                        try {
                                            List<WebSearchResultVO> flattedWebSearchResult = plan.flatWebSearchResult();
                                            responseHandler.onBeforeWebSearchReduce(flattedWebSearchResult);
                                            CompletableFuture<WebSearchResultVO> reduce = reduceWebSearch(flattedWebSearchResult, question, memoryIdVO, maxActingWebSearch);
                                            CompletableFuture<ActingService.Plan> planFuture = new CompletableFuture<>();
                                            reduce.whenComplete((resultVO, throwable1) -> {
                                                if (throwable1 == null && resultVO != null) {
                                                    plan.setReduceWebSearchResult(resultVO);
                                                }
                                                responseHandler.onAfterWebSearchReduce(flattedWebSearchResult, resultVO);
                                                responseHandler.onAfterReasoning(question, plan, result0[0], parallel);
                                                planFuture.complete(plan);
                                            });
                                            return planFuture;
                                        } catch (Exception e) {
                                            log.error("WebsearchReduceJsonSchema fail. question={}, chatId={}, error={}", question, memoryIdVO.getChatId(), e.toString(), e);
                                        }
                                    }
                                    responseHandler.onAfterReasoning(question, plan, result0[0], parallel);
                                    return CompletableFuture.completedFuture(plan);
                                });
                        return FutureUtil.allOf(f1);
                    }
                });
        return FutureUtil.allOf(future);
    }

    /**
     * 黑名单关键词过滤
     */
    private BiFunction<EmbeddingModelClient, List<ReRankModelClient.SortKey<WebSearchResultVO.Row>>, CompletableFuture<List<ReRankModelClient.SortKey<WebSearchResultVO.Row>>>>
    createWebSearchFilter() {
        List<ReRankModelClient.QuestionVO> questionList = knSettingWebsearchBlacklistService.selectBlackList();
        return ReRankModelClient.blackFilter(
                Collections.singletonList(WebSearchResultVO.Row::getContent),
                questionList);
    }

    /**
     * 合并联网结果
     */
    private CompletableFuture<WebSearchResultVO> reduceWebSearch(List<WebSearchResultVO> flattedWebSearchResult, String question, MemoryIdVO memoryIdVO, int topN) {
        if (flattedWebSearchResult == null || flattedWebSearchResult.isEmpty()) {
            return CompletableFuture.completedFuture(WebSearchResultVO.empty());
        }
        WebsearchReduceJsonSchema websearchReduceJsonSchema = llmJsonSchemaApiService.getWebsearchReduceJsonSchema(memoryIdVO);
        if (websearchReduceJsonSchema != null) {
            return websearchReduceJsonSchema.reduce(flattedWebSearchResult, question);
        } else {
            WebSearchResultVO merge = WebSearchResultVO.merge(flattedWebSearchResult);
            AiAssistantKn assistantKn = memoryIdVO.getAssistantKn(AiAssistantKnTypeEnum.rerank);
            if (assistantKn == null) {
                // 全部都要
                return CompletableFuture.completedFuture(merge);
            } else {
                // rerank TopN
                EmbeddingModelClient embeddingModelClient = knnApiService.getModel(assistantKn);
                ReRankModelClient reRankModelClient = new ReRankModelClient(embeddingModelClient);
                CompletableFuture<List<WebSearchResultVO.Row>> future = reRankModelClient.topN(question, merge.getList(), WebSearchResultVO.Row::reRankKey, topN, createWebSearchFilter());
                return future.thenApply(rows -> {
                    merge.setList(rows);
                    return merge;
                });
            }
        }
    }

    private ChatMessage buildMstate(AiVariables variables,
                                    String mstatePromptText,
                                    AssistantConfig assistantConfig) throws AssistantConfigException {
        // 记忆状态
        if (!StringUtils.hasText(mstatePromptText)) {
            return null;
        }

        try {
            Prompt prompt = AiUtil.toPrompt(mstatePromptText, variables);
            return new MstateAiMessage(prompt.text());
        } catch (IllegalArgumentException e) {
            throw new AssistantConfigException(String.format("%s %s[mstate_prompt_text] config error! detail:%s", assistantConfig.getName(), assistantConfig.getTableName(), e.toString()),
                    e, assistantConfig);
        }
    }

    private ChatMessage buildKnowledge(AiVariables variables,
                                       String question,
                                       List<List<QaKnVO>> qaKnVOList,
                                       String knPromptText,
                                       AssistantConfig assistantConfig) throws AssistantConfigException {
        // 知识库消息
        if (qaKnVOList == null || qaKnVOList.isEmpty() || !StringUtils.hasText(knPromptText)) {
            return null;
        }
        // 知识库提问
        Map<String, Object> var = BeanUtil.toMap(variables);
        var.put("documents", QaKnVO.qaToString(qaKnVOList));
        try {
            String text = AiUtil.toPrompt(knPromptText, var).text();
            return new KnowledgeAiMessage(new KnowledgeTextContent(text, question, question, qaKnVOList));
        } catch (IllegalArgumentException e) {
            throw new AssistantConfigException(String.format("%s %s[kn_prompt_text] config error! detail:%s", assistantConfig.getName(), assistantConfig.getTableName(), e.toString()),
                    e, assistantConfig);
        }
    }

    /**
     * 获取聊天模型
     *
     * @param assistant assistant
     * @return 聊天模型
     */
    private AiModel[] getModels(AssistantConfig assistant) {
        String apiKey = assistant.getChatApiKey();
        String baseUrl = assistant.getChatBaseUrl();
        String modelName = assistant.getChatModelName();
        Double temperature = assistant.getTemperature();
        Integer maxCompletionTokens = assistant.getMaxCompletionTokens();
        return modelMap.computeIfAbsent(uniqueKey(apiKey, baseUrl, modelName, temperature, maxCompletionTokens), k -> {
            AiModel[] arrays = new AiModel[concurrentChatModelCount];
            for (int i = 0; i < arrays.length; i++) {
                OpenAiStreamingChatModel streamingChatModel = createTextModel(apiKey, baseUrl, modelName, temperature, maxCompletionTokens);
                arrays[i] = new AiModel(baseUrl, modelName, null, streamingChatModel);
            }
            return arrays;
        });
    }

    /**
     * 创建聊天模型
     *
     * @param apiKey              apiKey
     * @param baseUrl             baseUrl
     * @param modelName           modelName
     * @param temperature         temperature
     * @param maxCompletionTokens maxCompletionTokens
     * @return 聊天模型
     */
    private OpenAiStreamingChatModel createTextModel(String apiKey,
                                                     String baseUrl,
                                                     String modelName,
                                                     Double temperature,
                                                     Integer maxCompletionTokens) {
        OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder = OpenAiStreamingChatModel.builder()
                .temperature(temperature)
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .tokenizer(tokenizer)
                .responseFormat(ResponseFormatType.TEXT.name());
        if (maxCompletionTokens != null && maxCompletionTokens > 0) {
            builder = builder.maxCompletionTokens(maxCompletionTokens);
        }
        return builder.build();
    }

    /**
     * 系统提示词，定义智能体
     *
     * @param promptText      promptText
     * @param responseHandler responseHandler
     * @param variables       variables
     * @param assistantConfig assistantConfig
     * @return 系统提示词
     */
    private SystemMessage buildSystemMessage(String promptText,
                                             ChatStreamingResponseHandler responseHandler,
                                             AiVariables variables,
                                             AssistantConfig assistantConfig) throws AssistantConfigException {
        if (!StringUtils.hasText(promptText)) {
            return null;
        }
        try {
            Prompt prompt = AiUtil.toPrompt(promptText, variables);
            responseHandler.onSystemMessage(prompt.text(), variables, promptText);
            return prompt.toSystemMessage();
        } catch (IllegalArgumentException e) {
            throw new AssistantConfigException(String.format("%s %s[system_prompt_text] config error! detail:%s", assistantConfig.getName(), assistantConfig.getTableName(), e.toString()),
                    e, assistantConfig);
        }
    }

    /**
     * 查询知识库
     *
     * @param assistant       assistant
     * @param knPromptText    knPromptText
     * @param assistantKnList assistantKnList
     * @param question        question
     * @return 知识库
     */
    private CompletableFuture<List<List<QaKnVO>>> selectKnList(AssistantConfig assistant, String knPromptText, List<AiAssistantKn> assistantKnList, String question) {
        if (!StringUtils.hasText(question) || assistantKnList.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        List<CompletableFuture<List<QaKnVO>>> futures = new ArrayList<>();
        for (AiAssistantKn assistantKn : assistantKnList) {
            CompletableFuture<List<QaKnVO>> f;
            // 知识库提问开启最小长度
            if (question.length() < assistantKn.getKnQueryMinCharLength()) {
                f = CompletableFuture.completedFuture(Collections.emptyList());
            } else if (!AiVariablesService.isEnableKnQuery(assistant, knPromptText)) {
                // 是否开启知识库查询
                f = CompletableFuture.completedFuture(Collections.emptyList());
            } else {
                // 知识库提问
                EmbeddingModelClient model = knnApiService.getModel(assistantKn);
                CompletableFuture<Map<String, Object>> body = QueryBuilderUtil.buildQaEsQuery(
                        assistantKn.getVectorFieldName(),
                        Collections.singletonList(question),
                        model, assistantKn.getKnLimit(),
                        AiUtil.scoreToDouble(assistantKn.getMinScore()),
                        QueryBuilderUtil.getFieldNameList(QaKnVO.class));
                model.embedAllFuture();
                f = knnApiService.knnSearchLib(assistantKn, QaKnVO.class, body);
            }
            futures.add(f);
        }
        return FutureUtil.allOf(futures)
                .thenApply(lists -> {
                    if (lists.size() == 1 && lists.get(0).isEmpty()) {
                        return new ArrayList<>();
                    }
                    return lists;
                });
    }

    private static class RepositoryChatStreamingResponseHandler implements ChatStreamingResponseHandler {
        final SessionMessageRepository repository;

        RepositoryChatStreamingResponseHandler(SessionMessageRepository repository) {
            this.repository = repository;
        }

        @Override
        public void onQuestionClassify(QuestionClassifyListVO questionClassify, String question, AiVariables variables) {
            repository.addQuestionClassify(questionClassify, question);
        }

        @Override
        public void onToken(AiMessageString token, int baseMessageIndex, int addMessageCount) {
            repository.afterToken(token);
        }

        @Override
        public void onKnowledge(List<List<QaKnVO>> knLibList, String question) {
            repository.addKnowledge(knLibList);
        }

        @Override
        public void onAfterReasoning(String question, ActingService.Plan plan, ReasoningJsonSchema.Result reason, boolean parallel) {
            repository.addReasoning(question, plan, reason, parallel);
        }

        @Override
        public void afterWebSearch(AiWebSearchSourceEnum sourceEnum, String providerName, String question, WebSearchResultVO resultVO, long cost) {
            repository.addWebSearchRead(sourceEnum, providerName, question, resultVO, cost);
        }

        @Override
        public void onError(Throwable error, int baseMessageIndex, int addMessageCount, int generateCount) {
            repository.addError(error, baseMessageIndex, addMessageCount, generateCount);
        }
    }
}
