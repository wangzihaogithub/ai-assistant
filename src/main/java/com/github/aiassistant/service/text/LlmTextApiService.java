package com.github.aiassistant.service.text;

import com.github.aiassistant.dao.AiAssistantFewshotMapper;
import com.github.aiassistant.dao.AiJsonschemaMapper;
import com.github.aiassistant.entity.AiAssistantKn;
import com.github.aiassistant.entity.model.chat.*;
import com.github.aiassistant.entity.model.langchain4j.KnowledgeAiMessage;
import com.github.aiassistant.entity.model.langchain4j.MetadataAiMessage;
import com.github.aiassistant.entity.model.langchain4j.MstateAiMessage;
import com.github.aiassistant.entity.model.user.AiAccessUserVO;
import com.github.aiassistant.enums.AiAssistantKnTypeEnum;
import com.github.aiassistant.enums.AiWebSearchSourceEnum;
import com.github.aiassistant.exception.*;
import com.github.aiassistant.service.jsonschema.LlmJsonSchemaApiService;
import com.github.aiassistant.service.jsonschema.ReasoningJsonSchema;
import com.github.aiassistant.service.jsonschema.WebsearchReduceJsonSchema;
import com.github.aiassistant.service.text.acting.ActingService;
import com.github.aiassistant.service.text.embedding.*;
import com.github.aiassistant.service.text.reasoning.ReasoningService;
import com.github.aiassistant.service.text.repository.ConsumerTokenWindowChatMemory;
import com.github.aiassistant.service.text.repository.JsonSchemaTokenWindowChatMemory;
import com.github.aiassistant.service.text.repository.SessionMessageRepository;
import com.github.aiassistant.service.text.rerank.EmbeddingReRankModel;
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
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.Tokenizer;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.openai.OpenAiChatClient;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 文本类型聊天模型服务
 */
public class LlmTextApiService {
    private static final Logger log = LoggerFactory.getLogger(LlmTextApiService.class);
    /**
     * 预测各种文本类型（如文本、提示、文本段等）中的标记计数的接口
     */
    private final Tokenizer tokenizer = new OpenAiTokenizer();
    /**
     * Text类型的聊天模型
     */
    private final Map<String, AiModelVO[]> modelMap = new ConcurrentHashMap<>();
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
    private final AiJsonschemaMapper aiJsonschemaMapper;
    private final AiAssistantFewshotMapper aiAssistantFewshotMapper;
    /**
     * 向量模型服务
     */
    private final KnnApiService knnApiService;
    /**
     * 线程池：如果java21可以传过来虚拟线程
     */
    private final Executor functionCallingThreadPool;
    private final EmbeddingModelPool embeddingModelPool;
    private final ActingService actingService;
    private final ReasoningService reasoningService;
    private final KnSettingWebsearchBlacklistServiceImpl knSettingWebsearchBlacklistService;
    private final Supplier<Collection<LlmTextApiServiceIntercept>> interceptList;
    /**
     * 每个智能体的聊天模型并发数量
     */
    private final int clientModelInstanceCount = 5;
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
    /**
     * 联网搜索服务
     */
    private WebSearchService webSearchService = new WebSearchService();
    /**
     * 用户提的问题大于等于多少才开启联网，默认3
     */
    private int minEnableWebSearchStringLength = 3;
    /**
     * 大模型请求超时时间
     */
    private Duration timeout = Duration.ofSeconds(120);
    /**
     * 大模型建立socket链接超时时间
     */
    private Duration connectTimeout = Duration.ofSeconds(3);

    public LlmTextApiService(EmbeddingModelPool embeddingModelPool, LlmJsonSchemaApiService llmJsonSchemaApiService,
                             AiQuestionClassifyService aiQuestionClassifyService,
                             AiJsonschemaMapper aiJsonschemaMapper,
                             AiAssistantFewshotMapper aiAssistantFewshotMapper,
                             AiToolServiceImpl aiToolService,
                             AiVariablesService aiVariablesService,
                             KnnApiService knnApiService,
                             ActingService actingService, ReasoningService reasoningService,
                             KnSettingWebsearchBlacklistServiceImpl knSettingWebsearchBlacklistService, Executor functionCallingThreadPool,
                             Supplier<Collection<LlmTextApiServiceIntercept>> interceptList) {
        if (functionCallingThreadPool == null) {
            int threads = Math.max(Runtime.getRuntime().availableProcessors() * 2, 6);
            functionCallingThreadPool = new ThreadPoolExecutor(
                    1, threads,
                    60, TimeUnit.SECONDS,
                    new SynchronousQueue<>(), target -> {
                Thread thread = new Thread(target);
                thread.setName("ai-functioncall-" + thread.getId());
                thread.setDaemon(true);
                return thread;
            }, new ThreadPoolExecutor.CallerRunsPolicy());
        }
        this.embeddingModelPool = embeddingModelPool;
        this.aiAssistantFewshotMapper = aiAssistantFewshotMapper;
        this.aiToolService = aiToolService;
        this.aiJsonschemaMapper = aiJsonschemaMapper;
        this.llmJsonSchemaApiService = llmJsonSchemaApiService;
        this.aiQuestionClassifyService = aiQuestionClassifyService;
        this.aiVariablesService = aiVariablesService;
        this.knnApiService = knnApiService;
        this.functionCallingThreadPool = functionCallingThreadPool;
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

    private static String uniqueKey(Object... keys) {
        return Arrays.toString(keys);
    }

    /**
     * 结合整体判断是否开启思考
     *
     * @param reasoning        是否需要思考
     * @param assistantConfig  智能体配置
     * @param knPromptText     in context learning的知识库提示词
     * @param mstatePromptText in context learning的记忆提示词
     * @param lastQuery        用户最后一次的提问内容
     * @return true=结合整体判断是否开启思考
     */
    private static boolean isEnableReasoning(Boolean reasoning, AssistantConfig assistantConfig, String knPromptText, String mstatePromptText, String lastQuery) {
        return Boolean.TRUE.equals(reasoning)
                && AiVariablesService.isEnableReasoning(assistantConfig, knPromptText, mstatePromptText)
                && Objects.toString(lastQuery, "").length() > 2;
    }

    /**
     * 提问报错了
     *
     * @param error           异常
     * @param memoryId        记忆ID
     * @param responseHandler 事件回调函数
     * @return 持久化异常
     */
    private static CompletableFuture<FunctionCallStreamingResponseHandler> questionError(Throwable error, MemoryIdVO memoryId, ChatStreamingResponseHandler responseHandler) {
        CompletableFuture<FunctionCallStreamingResponseHandler> handlerFuture = new CompletableFuture<>();
        handlerFuture.completeExceptionally(error);
        responseHandler.onError(error, 0, 0, 0);
        log.error("llm questionError chatId {}, error {}", memoryId.getChatId(), error.toString(), error);
        return handlerFuture;
    }

    public WebSearchService getWebSearchService() {
        return webSearchService;
    }

    public void setWebSearchService(WebSearchService webSearchService) {
        this.webSearchService = webSearchService;
    }

    /**
     * 结合整体判断是否开启联网
     *
     * @param websearch        是否需要联网
     * @param assistantConfig  智能体配置
     * @param knPromptText     in context learning的知识库提示词
     * @param mstatePromptText in context learning的记忆提示词
     * @param lastQuery        用户最后一次的提问内容
     * @return true=结合整体判断是否开启联网
     */
    private boolean isEnableWebSearch(Boolean websearch, AssistantConfig assistantConfig, String knPromptText, String mstatePromptText, String lastQuery) {
        return Boolean.TRUE.equals(websearch)
                && AiVariablesService.isEnableWebSearch(assistantConfig, knPromptText, mstatePromptText)
                && Objects.toString(lastQuery, "").length() >= minEnableWebSearchStringLength;
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
     * 向AI发起提问
     *
     * @param user            当前用户
     * @param repository      持久化生成过程中的数据
     * @param question        用户本次的提问内容
     * @param websearch       是否需要联网（应用层面的实现，非供应商提供的）
     * @param reasoning       是否需要思考（应用层面的实现，非供应商提供的）
     * @param memoryId        记忆ID
     * @param responseHandler 事件回调函数
     * @return 构造出支持工具调用的回调函数(对接底层模型)
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
            llmJsonSchemaApiService.addSessionJsonSchema(memoryId, memoryId.getAiAssistant().getAiJsonschemaIds(), aiJsonschemaMapper, functionCallingThreadPool);
            // 持久化
            ChatStreamingResponseHandler mergeResponseHandler = new MergeChatStreamingResponseHandler(
                    Arrays.asList(responseHandler, new RepositoryChatStreamingResponseHandler(repository)),
                    responseHandler);
            llmJsonSchemaApiService.putSessionHandler(memoryId, websearch, reasoning, mergeResponseHandler, user);

            Collection<LlmTextApiServiceIntercept> intercepts = interceptList.get();

            // 历史记录
            List<ChatMessage> historyList = interceptHistoryList(repository.getHistoryList(),
                    user, memoryId, websearch, reasoning, responseHandler, question, intercepts);

            int baseMessageIndex = historyList.size();//起始消息下标
            int addMessageCount = 1;// 为什么是1？因为第0个是内置的SystemMessage，所以至少要有一个。
            // 当前问题，如果是重新回答需要获取最后一次问题getLastUserQuestion
            String lastQuestion = StringUtils.hasText(question) ? question : AiUtil.getLastUserQuestion(historyList);
            if (!StringUtils.hasText(lastQuestion)) {
                throw new QuestionEmptyException("user question is empty!", historyList);
            }
            // 初始化
            mergeResponseHandler.onTokenBegin(baseMessageIndex, addMessageCount, 0);
            // 查询变量
            AiVariablesVO variables = aiVariablesService.selectVariables(user, historyList, lastQuestion, memoryId, websearch, mergeResponseHandler);
            // 绑定会话钩子
            llmJsonSchemaApiService.putSessionVariables(memoryId, variables);
            // 进行问题分类
            CompletableFuture<QuestionClassifyListVO> classifyFuture = aiQuestionClassifyService.classify(lastQuestion, memoryId);
            // 问题分类报错
            classifyFuture.exceptionally(throwable -> {
                mergeResponseHandler.onError(throwable, baseMessageIndex, addMessageCount, 0);
                return null;
            });
            // buildHandler 构造出支持工具调用的回调函数(对接底层模型)
            CompletableFuture<CompletableFuture<FunctionCallStreamingResponseHandler>> f = classifyFuture
                    .thenApply(c -> buildHandler(c, user, repository, question, websearch,
                            reasoning, memoryId, mergeResponseHandler, historyList, baseMessageIndex, addMessageCount, lastQuestion, variables, intercepts));
            // 等待完毕
            return FutureUtil.allOf(f)
                    // 完毕后删除JsonSchema的本地记忆
                    .whenComplete((handler, throwable) -> removeJsonSchemaSession(handler, throwable, memoryId));
        } catch (Throwable e) {
            // 提问报错了，删除JsonSchema的本地记忆
            removeJsonSchemaSession(null, e, memoryId);
            return questionError(e, memoryId, responseHandler);
        }
    }

    /**
     * 完毕后删除JsonSchema的本地记忆
     *
     * @param handler   构造出支持工具调用的回调函数(对接底层模型)
     * @param throwable 异常
     * @param memoryId  记忆ID
     */
    private void removeJsonSchemaSession(FunctionCallStreamingResponseHandler handler, Throwable throwable, MemoryIdVO memoryId) {
        if (handler == null || throwable != null) {
            llmJsonSchemaApiService.removeSession(memoryId);
        } else {
            handler.whenComplete((unused, throwable1) -> llmJsonSchemaApiService.removeSession(memoryId));
        }
    }

    /**
     * 构造出支持工具调用的回调函数(对接底层模型)
     *
     * @param classifyListVO   问题分类
     * @param user             当前用户
     * @param repository       持久化生成过程中的数据
     * @param question         用户本次的提问内容
     * @param websearch        是否需要联网
     * @param reasoning        是否需要思考
     * @param memoryId         记忆ID
     * @param responseHandler  事件回调函数
     * @param historyList      记忆中的聊天记录
     * @param baseMessageIndex 历史中聊天根下标
     * @param addMessageCount  本次添加了几条消息
     * @param lastQuestion     用户最后一次的提问内容
     * @param variables        提示词内可引用的变量
     * @param intercepts       拦截
     * @return 回调函数(对接底层模型)
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
            AiVariablesVO variables,
            Collection<LlmTextApiServiceIntercept> intercepts) {
        CompletableFuture<FunctionCallStreamingResponseHandler> handlerFuture = new CompletableFuture<>();
        try {
            // 将问题分类绑定至会话
            llmJsonSchemaApiService.putSessionQuestionClassify(memoryId, classifyListVO);
            // 选择智能体（2选1）
            AssistantConfig assistantConfig = AssistantConfig.select(memoryId.getAiAssistant(), classifyListVO.getClassifyAssistant());
            // in context learning的知识库提示词
            String knPromptText = assistantConfig.getKnPromptText();
            // in context learning的记忆提示词
            String mstatePromptText = assistantConfig.getMstatePromptText();

            // 问题分类完成通知
            responseHandler.onQuestionClassify(classifyListVO, lastQuestion, variables);
            // 分类结果放到全局提示词变量中
            aiVariablesService.setterQuestionClassifyResult(variables.getQuestionClassify(), classifyListVO.getClassifyResult());

            // 准备就绪后，是否需要中断后续流程
            // 用户代码是否需要拦截持久化
            Function<FunctionCallStreamingResponseHandler, CompletableFuture<Void>> interceptRepository =
                    interceptRepository(user, memoryId, classifyListVO, variables, websearch, reasoning, responseHandler, historyList, question, lastQuestion, intercepts);
            // 用户代码是否需要拦截提问
            Function<FunctionCallStreamingResponseHandler, CompletableFuture<Void>> interceptQuestion =
                    interceptRepository == null ?
                            interceptQuestion(user, memoryId, classifyListVO, variables, websearch, reasoning, responseHandler, historyList, question, lastQuestion, intercepts) : null;
            // 用户代码是否提出了需要拦截
            boolean interrupt = interceptRepository != null || interceptQuestion != null;

            // 1.联网搜索
            CompletableFuture<String> webSearchResult;
            if (!interrupt && classifyListVO.isJdlw() && isEnableWebSearch(websearch, assistantConfig, knPromptText, mstatePromptText, lastQuestion)) {
                // 剩余可用字数
                int maxCharLength = assistantConfig.getMaxMemoryTokens() - AiUtil.sumLength(historyList);
                // 联网
                CompletableFuture<CompletableFuture<WebSearchResultVO>> webSearchFuture = webSearchService.webSearchRead(lastQuestion, 1, maxCharLength, false, responseHandler.adapterWebSearch(AiWebSearchSourceEnum.LlmTextApiService)).thenApply(s -> {
                    return s.isEmpty() ? CompletableFuture.completedFuture(s) : reduceWebSearch(Collections.singletonList(s), lastQuestion, memoryId, maxSimpleWebSearch);
                });
                // 联网后放入全局提示词变量
                webSearchResult = FutureUtil.allOf(webSearchFuture).thenApply(resultVO -> {
                    String xmlString = AiUtil.toAiXmlString(lastQuestion, WebSearchResultVO.toSimpleAiString(resultVO), 64);
                    variables.getKn().setWebSearchResult(xmlString);
                    return xmlString;
                });
            } else {
                webSearchResult = CompletableFuture.completedFuture(null);
            }
            // 2.查询知识库
            CompletableFuture<List<List<QaKnVO>>> knnFuture = classifyListVO.isQa() && knnApiService != null ? selectKnList(assistantConfig, knPromptText, memoryId.getAssistantKnList(AiAssistantKnTypeEnum.qa), lastQuestion, responseHandler) : CompletableFuture.completedFuture(Collections.emptyList());
            // 3.思考并行动
            CompletableFuture<ActingService.Plan> reasoningResultFuture;
            boolean reasoningAndActing = !interrupt && classifyListVO.isWtcj() && isEnableReasoning(reasoning, assistantConfig, knPromptText, mstatePromptText, lastQuestion);
            if (reasoningAndActing) {
                reasoningResultFuture = reasoningAndActing(knnFuture, webSearchResult, lastQuestion, memoryId, reasoningAndActingParallel, responseHandler, websearch, classifyListVO);
            } else {
                // 是否需要思考并行动的事件通知
                responseHandler.onBeforeReasoningAndActing(false);
                reasoningResultFuture = CompletableFuture.completedFuture(null);
            }

            // 等待结果
            FutureUtil.allOfs(webSearchResult, knnFuture, reasoningResultFuture)
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
                            // 构造出支持工具调用的回调函数(对接底层模型)
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
                                    baseMessageIndex,
                                    websearch,
                                    reasoning
                            );
                        } catch (AssistantConfigException | FewshotConfigException | ToolCreateException |
                                 JsonSchemaCreateException e) {
                            // 智能体配置异常 ｜ 少样本提示异常 | AI工具创建异常 | JsonSchema创建异常
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
                                DataInspectionFailedException inspectionFailedException = new DataInspectionFailedException(
                                        String.format("无法回答问题'%s', '%s'", lastQuestion, classifyListVO), lastQuestion, classifyListVO);
                                handlerFuture.completeExceptionally(inspectionFailedException);
                                responseHandler.onError(inspectionFailedException, baseMessageIndex, addMessageCount, 0);
                                return;
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
            AiVariablesVO variables,
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
            AiVariablesVO variables,
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

    /**
     * 拦截记忆历史记录（可以改写）
     *
     * @param historyList     记忆历史记录
     * @param user            当前用户
     * @param memoryId        记忆ID
     * @param websearch       联网
     * @param reasoning       思考
     * @param responseHandler 事件回调函数
     * @param question        用户本次的提问内容
     * @param intercepts      拦截
     * @return 拦截改写后的记忆历史记录
     */
    private List<ChatMessage> interceptHistoryList(
            List<ChatMessage> historyList,
            AiAccessUserVO user,
            MemoryIdVO memoryId,
            Boolean websearch,
            Boolean reasoning,
            ChatStreamingResponseHandler responseHandler,
            String question,
            Collection<LlmTextApiServiceIntercept> intercepts) {
        for (LlmTextApiServiceIntercept intercept : intercepts) {
            historyList = intercept.interceptHistoryList(historyList, user, memoryId, websearch, reasoning, responseHandler, question);
        }
        return historyList;
    }

    /**
     * 构造出支持工具调用的回调函数(对接底层模型)
     *
     * @param classifyListVO      问题分类
     * @param assistantConfig     智能体配置
     * @param mstatePromptText    in context learning的记忆提示词
     * @param knPromptText        in context learning的知识库提示词
     * @param user                当前用户
     * @param variables           提示词内可引用的变量
     * @param qaKnVOList          问答RAG出的结果
     * @param repository          持久化生成过程中的数据
     * @param question            用户本次的提问内容
     * @param memoryId            记忆ID
     * @param responseHandler     事件回调函数
     * @param lastQuestion        用户最后一次的提问内容
     * @param historyList         记忆中的聊天记录
     * @param rootAddMessageCount 历史中聊天总条数
     * @param baseMessageIndex    历史中聊天根下标
     * @param websearch           是否需要联网
     * @param reasoning           是否需要思考
     * @return 回调函数(对接底层模型)
     * @throws AssistantConfigException  智能体配置出现错误
     * @throws FewshotConfigException    少样本提示异常
     * @throws ToolCreateException       工具创建异常
     * @throws JsonSchemaCreateException JsonSchema创建出现错误
     */
    private FunctionCallStreamingResponseHandler newFunctionCallStreamingResponseHandler(
            QuestionClassifyListVO classifyListVO,
            AssistantConfig assistantConfig,
            String mstatePromptText,
            String knPromptText,
            AiAccessUserVO user,
            AiVariablesVO variables,
            List<List<QaKnVO>> qaKnVOList,
            SessionMessageRepository repository,
            String question,
            MemoryIdVO memoryId,
            ChatStreamingResponseHandler responseHandler,
            String lastQuestion,
            List<ChatMessage> historyList,
            int rootAddMessageCount,
            int baseMessageIndex,
            Boolean websearch,
            Boolean reasoning
    ) throws AssistantConfigException, FewshotConfigException, ToolCreateException, JsonSchemaCreateException {
        // jsonschema模型
        llmJsonSchemaApiService.addSessionJsonSchema(memoryId, assistantConfig.getAiJsonschemaIds(), aiJsonschemaMapper, functionCallingThreadPool);

        // 系统消息
        SystemMessage systemMessage = buildSystemMessage(assistantConfig.getSystemPromptText(), responseHandler, variables, assistantConfig);
        // 少样本学习
        List<ChatMessage> fewshotMessageList = AiUtil.deserializeFewshot(aiAssistantFewshotMapper.selectListByAssistantId(memoryId.getAiAssistantId()), variables);
        AiUtil.addToHistoryList(historyList, systemMessage, fewshotMessageList);

        // 记忆
        ConsumerTokenWindowChatMemory chatMemory = new ConsumerTokenWindowChatMemory(memoryId, assistantConfig.getMaxMemoryTokens(), assistantConfig.getMaxMemoryRounds(),
                tokenizer, historyList, e -> RepositoryChatStreamingResponseHandler.add(e, repository));
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
        List<ChatMessage> useQuestionList = responseHandler.onBeforeUsedMessageList(questionList, user, variables, memoryId, websearch, reasoning, question);
        int addMessageCount = useQuestionList.size() + rootAddMessageCount;
        useQuestionList.forEach(chatMemory::add);

        // 插入用户的提问
        if (repository != null) {
            repository.addUserQuestion(useQuestionList).exceptionally(throwable -> {
                // 插入失败，返回错误消息
                responseHandler.onError(throwable, baseMessageIndex, addMessageCount, 0);
                return null;
            });
        }

        // 获取模型
        AiModelVO aiModel = memoryId.indexAt(getModels(assistantConfig));
        // 可用工具集
        List<Tools.ToolMethod> toolMethodList = AiUtil.initTool(aiToolService.selectToolMethodList(StringUtils.split(assistantConfig.getAiToolIds(), ",")), variables, user);
        // 构造出支持工具调用的回调函数(对接底层模型) 处理异步回调
        return new FunctionCallStreamingResponseHandler(
                // 模型名称，流模型，聊天记忆，业务回调钩子
                aiModel.modelName, aiModel.streaming, chatMemory, responseHandler,
                llmJsonSchemaApiService,
                // 工具集合，
                toolMethodList,
                // 是否支持中文工具名称（因为deepseek仅支持英文名称）
                aiModel.isSupportChineseToolName(),
                // 消息下标
                baseMessageIndex, addMessageCount,
                // 读取超时时间
                classifyListVO.getReadTimeoutMs(),
                // 问题分类
                classifyListVO,
                // 是否需要联网
                websearch,
                // 是否需要思考
                reasoning,
                // 切换新线程，用于退出Okhttp的事件循环线程，
                // 防止在Okhttp的AI回复后，仍需要再次请求。这样会导致事件循环线程中又触发调用，导致阻塞父事件循环。
                functionCallingThreadPool);
    }

    /**
     * 思考并行动
     *
     * @param knnFuture       问答RAG出的结果
     * @param webSearchResult 联网搜索结果
     * @param lastQuestion    用户最后一次的提问内容
     * @param memoryIdVO      记忆ID
     * @param parallel        是否并行执行，思考的子问题（注：并行不能携带上一个子问题的执行结果,如果子问题需要依赖上一个子问题的执行结果，需要改为false）
     * @param responseHandler 事件回调函数
     * @param websearch       是否需要联网
     * @param classifyListVO  问题分类
     * @return 思考并行动的结果
     */
    private CompletableFuture<ActingService.Plan> reasoningAndActing(CompletableFuture<List<List<QaKnVO>>> knnFuture,
                                                                     CompletableFuture<String> webSearchResult,
                                                                     String lastQuestion, MemoryIdVO memoryIdVO,
                                                                     boolean parallel,
                                                                     ChatStreamingResponseHandler responseHandler,
                                                                     Boolean websearch,
                                                                     QuestionClassifyListVO classifyListVO) {
        // 思考并行动（等问答RAG出结果后再开始）
        CompletableFuture<CompletableFuture<ActingService.Plan>> future = knnFuture
                .handle((qaList, throwable) -> {
                    if (throwable != null) {
                        responseHandler.onBeforeReasoningAndActing(false);
                        return CompletableFuture.completedFuture(null);
                    } else if (qaList != null && !qaList.isEmpty()) {
                        responseHandler.onBeforeReasoningAndActing(false);
                        return CompletableFuture.completedFuture(null);
                    } else {
                        // 事件通知
                        responseHandler.onBeforeReasoning(lastQuestion, parallel);
                        ReasoningJsonSchema.Result[] result0 = new ReasoningJsonSchema.Result[1];
                        CompletableFuture<CompletableFuture<CompletableFuture<ActingService.Plan>>> f =
                                // 制作一份计划(联网搜索后再开始)
                                webSearchResult.thenApply(s -> reasoningService.makePlan(lastQuestion, memoryIdVO)
                                        .thenApply(result -> {
                                            // 是否需要思考并行动的事件通知
                                            responseHandler.onBeforeReasoningAndActing(result != null);
                                            // 事件通知
                                            if (result != null) {
                                                responseHandler.onReasoning(lastQuestion, result);
                                            }
                                            result0[0] = result;
                                            return result;
                                        })
                                        .thenApply(result -> {
                                            ActingService.Plan plan = actingService.toPlan(result);
                                            // 开联网 并且【多层联网】判断原问题是否需要联网
                                            if (Boolean.TRUE.equals(websearch) && classifyListVO.isDclw()) {
                                                // 根据计划，执行子问题
                                                return actingService.executeTask(plan, lastQuestion, memoryIdVO, parallel, responseHandler, classifyListVO.isLwdd());
                                            } else {
                                                return CompletableFuture.completedFuture(plan);
                                            }
                                        }));
                        CompletableFuture<CompletableFuture<ActingService.Plan>> f1 = FutureUtil.allOf(FutureUtil.allOf(f))
                                .thenApply(plan -> {
                                    if (plan != null) {
                                        try {
                                            // 联网合并
                                            List<WebSearchResultVO> flattedWebSearchResult = plan.flatWebSearchResult();
                                            // 事件通知
                                            responseHandler.onBeforeWebSearchReduce(flattedWebSearchResult);
                                            CompletableFuture<WebSearchResultVO> reduce = reduceWebSearch(flattedWebSearchResult, lastQuestion, memoryIdVO, maxActingWebSearch);
                                            CompletableFuture<ActingService.Plan> planFuture = new CompletableFuture<>();
                                            reduce.whenComplete((resultVO, throwable1) -> {
                                                if (throwable1 == null && resultVO != null) {
                                                    plan.setReduceWebSearchResult(resultVO);
                                                }
                                                // 事件通知
                                                responseHandler.onAfterWebSearchReduce(flattedWebSearchResult, resultVO);
                                                responseHandler.onAfterReasoning(lastQuestion, plan, result0[0], parallel);
                                                // 完成
                                                planFuture.complete(plan);
                                            });
                                            return planFuture;
                                        } catch (Exception e) {
                                            log.error("WebsearchReduceJsonSchema fail. question={}, chatId={}, error={}", lastQuestion, memoryIdVO.getChatId(), e.toString(), e);
                                        }
                                    }
                                    // 事件通知
                                    responseHandler.onAfterReasoning(lastQuestion, plan, result0[0], parallel);
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
    private BiFunction<EmbeddingReRankModel, List<EmbeddingReRankModel.EmbeddingSortKey<WebSearchResultVO.Row>>, CompletableFuture<List<EmbeddingReRankModel.EmbeddingSortKey<WebSearchResultVO.Row>>>>
    createWebSearchFilter() {
        List<EmbeddingReRankModel.QuestionVO> questionList = knSettingWebsearchBlacklistService.selectBlackList();
        return EmbeddingReRankModel.blackFilter(
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
        WebsearchReduceJsonSchema websearchReduceJsonSchema;
        try {
            websearchReduceJsonSchema = llmJsonSchemaApiService.getWebsearchReduceJsonSchema(memoryIdVO);
        } catch (JsonSchemaCreateException e) {
            return FutureUtil.completeExceptionally(e);
        }
        if (websearchReduceJsonSchema != null) {
            return websearchReduceJsonSchema.reduce(flattedWebSearchResult, question);
        } else {
            WebSearchResultVO merge = WebSearchResultVO.merge(flattedWebSearchResult);
            AiAssistantKn assistantKn = memoryIdVO.getAssistantKn(AiAssistantKnTypeEnum.embedding);
            if (assistantKn == null) {
                // 全部都要
                return CompletableFuture.completedFuture(merge);
            } else {
                // rerank TopN
                EmbeddingModelClient embeddingModelClient = embeddingModelPool.getModel(assistantKn);
                EmbeddingReRankModel reRankModelClient = new EmbeddingReRankModel(embeddingModelClient);
                CompletableFuture<List<WebSearchResultVO.Row>> future = reRankModelClient.topN(question, merge.getList(), WebSearchResultVO.Row::reRankKey, topN, createWebSearchFilter());
                return future.thenApply(rows -> {
                    merge.setList(rows);
                    return merge;
                });
            }
        }
    }

    private ChatMessage buildMstate(AiVariablesVO variables,
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

    private ChatMessage buildKnowledge(AiVariablesVO variables,
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
            return new KnowledgeAiMessage(text, question, qaKnVOList);
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
    private AiModelVO[] getModels(AssistantConfig assistant) {
        String apiKey = assistant.getChatApiKey();
        String baseUrl = assistant.getChatBaseUrl();
        String modelName = assistant.getChatModelName();
        Double temperature = assistant.getTemperature();
        Integer maxCompletionTokens = assistant.getMaxCompletionTokens();
        return modelMap.computeIfAbsent(uniqueKey(apiKey, baseUrl, modelName, temperature, maxCompletionTokens), k -> {
            AiModelVO[] arrays = new AiModelVO[clientModelInstanceCount];
            for (int i = 0; i < arrays.length; i++) {
                OpenAiChatClient streamingChatModel = createTextModel(apiKey, baseUrl, modelName, temperature, maxCompletionTokens);
                arrays[i] = new AiModelVO(baseUrl, modelName, streamingChatModel);
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
    private OpenAiChatClient createTextModel(String apiKey,
                                             String baseUrl,
                                             String modelName,
                                             Double temperature,
                                             Integer maxCompletionTokens) {
        OpenAiChatClient.Builder builder = OpenAiChatClient.builder()
                .timeout(timeout)
                .connectTimeout(connectTimeout)
                .temperature(temperature)
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .responseFormat(ResponseFormatType.TEXT.name());
        if (maxCompletionTokens != null && maxCompletionTokens > 0) {
            builder = builder.maxCompletionTokens(maxCompletionTokens);
        }
        return builder.build();
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    /**
     * 系统提示词，定义智能体
     *
     * @param promptText      promptText
     * @param responseHandler responseHandler
     * @param variables       variables
     * @param assistantConfig assistantConfig
     * @return 系统提示词
     * @throws AssistantConfigException system_prompt 异常
     */
    private SystemMessage buildSystemMessage(String promptText,
                                             ChatStreamingResponseHandler responseHandler,
                                             AiVariablesVO variables,
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
     * @param responseHandler onKnnSearch
     * @return 知识库
     */
    private CompletableFuture<List<List<QaKnVO>>> selectKnList(AssistantConfig assistant, String knPromptText, List<AiAssistantKn> assistantKnList, String question, ChatStreamingResponseHandler responseHandler) {
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
                EmbeddingModelClient model = embeddingModelPool.getModel(assistantKn);
                CompletableFuture<Map<String, Object>> body = QueryBuilderUtil.buildQaEsQuery(
                        assistantKn.getVectorFieldName(),
                        Collections.singletonList(question),
                        model, assistantKn.getKnLimit(),
                        AiUtil.scoreToDouble(assistantKn.getMinScore()),
                        QueryBuilderUtil.getFieldNameList(QaKnVO.class));
                model.embedAllFuture();
                KnnResponseListenerFuture<QaKnVO> future = knnApiService.knnSearchLib(assistantKn, QaKnVO.class, body);
                responseHandler.onKnnSearch(future);
                f = future;
            }
            futures.add(f);
        }
        return FutureUtil.allOf(futures)
                .thenApply(lists -> {
                    if (lists.stream().allMatch(List::isEmpty)) {
                        return new ArrayList<>();
                    }
                    return lists;
                });
    }

    public int getMinEnableWebSearchStringLength() {
        return minEnableWebSearchStringLength;
    }

    public void setMinEnableWebSearchStringLength(int minEnableWebSearchStringLength) {
        this.minEnableWebSearchStringLength = minEnableWebSearchStringLength;
    }

    private static class RepositoryChatStreamingResponseHandler implements ChatStreamingResponseHandler {
        final SessionMessageRepository repository;

        RepositoryChatStreamingResponseHandler(SessionMessageRepository repository) {
            this.repository = repository;
        }

        static void add(ChatMessage chatMessage, SessionMessageRepository repository) {
            /**
             * 思考的已经持久化过了，不用再持久化了
             * @see #onAfterModelThinking(Response)
             */
            if (chatMessage instanceof MetadataAiMessage && ((MetadataAiMessage) chatMessage).isTypeThinkingAiMessage()) {
                return;
            }
            repository.add(chatMessage);
        }

        @Override
        public void onKnnSearch(KnnResponseListenerFuture<? extends KnVO> future) {
            repository.addKnnSearch(future);
        }

        @Override
        public void onQuestionClassify(QuestionClassifyListVO questionClassify, String question, AiVariablesVO variables) {
            repository.addQuestionClassify(questionClassify, question);
        }

        @Override
        public void onAfterModelThinking(Response<AiMessage> thinkingResponse) {
            repository.add(MetadataAiMessage.convert(thinkingResponse));
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
