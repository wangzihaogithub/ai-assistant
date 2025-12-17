package com.github.aiassistant.service.jsonschema;

import com.github.aiassistant.dao.AiJsonschemaMapper;
import com.github.aiassistant.entity.AiJsonschema;
import com.github.aiassistant.entity.model.chat.AiModelVO;
import com.github.aiassistant.entity.model.chat.AiVariablesVO;
import com.github.aiassistant.entity.model.chat.MemoryIdVO;
import com.github.aiassistant.entity.model.chat.QuestionClassifyListVO;
import com.github.aiassistant.entity.model.user.AiAccessUserVO;
import com.github.aiassistant.exception.JsonSchemaCreateException;
import com.github.aiassistant.service.text.ChatStreamingResponseHandler;
import com.github.aiassistant.service.text.repository.JsonSchemaTokenWindowChatMemory;
import com.github.aiassistant.service.text.tools.AiToolServiceImpl;
import com.github.aiassistant.service.text.tools.Tools;
import com.github.aiassistant.util.AiUtil;
import com.github.aiassistant.util.BeanUtil;
import com.github.aiassistant.util.StringUtils;
import dev.langchain4j.model.openai.OpenAiChatClient;
import dev.langchain4j.service.AiServiceContext;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.FunctionalInterfaceAiServices;

import java.io.Serializable;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * JsonSchema类型的模型
 */
// @Component
public class LlmJsonSchemaApiService {
    /**
     * JsonSchema的实例
     */
    private final Map<String, Map<Class, AiModelVO[]>> jsonSchemaInstanceMap = new ConcurrentHashMap<>();
    /**
     * JsonSchema的本地记忆
     */
    private final Map<Object, Session> memoryMap = Collections.synchronizedMap(new IdentityHashMap<>());
    private final int schemaInstanceCount;
    private final Map<Class, AtomicInteger> schemasIndexMap = Collections.synchronizedMap(new WeakHashMap<>());
    // @Autowired
    private final AiToolServiceImpl aiToolService;
    private final AiJsonschemaMapper aiJsonschemaMapper;

    /**
     * 大模型建立socket链接超时时间
     */
    private Duration connectTimeout = Duration.ofSeconds(3);

    public LlmJsonSchemaApiService(AiJsonschemaMapper aiJsonschemaMapper, AiToolServiceImpl aiToolService) {
        this(aiJsonschemaMapper, aiToolService, 5);
    }

    public LlmJsonSchemaApiService(AiJsonschemaMapper aiJsonschemaMapper, AiToolServiceImpl aiToolService, int schemaInstanceCount) {
        this.aiJsonschemaMapper = aiJsonschemaMapper;
        this.aiToolService = aiToolService;
        this.schemaInstanceCount = schemaInstanceCount;
    }

    private static String uniqueKey(Object... keys) {
        return Arrays.toString(keys);
    }

    public static String toJsonSchemaEnum(Class<?> jsonSchemaType) {
        return jsonSchemaType.getSimpleName();
    }

    /**
     * 删除JsonSchema的本地记忆
     *
     * @param memoryIdVO memoryIdVO
     */
    public void removeSession(MemoryIdVO memoryIdVO) {
        memoryMap.remove(memoryIdVO);
    }

    /**
     * 保存JsonSchema的本地记忆
     *
     * @param memoryIdVO memoryIdVO
     * @param chatMemory chatMemory
     */
    public void putSessionMemory(MemoryIdVO memoryIdVO, JsonSchemaTokenWindowChatMemory chatMemory) {
        getSession(memoryIdVO, true).chatMemory = chatMemory;
    }

    public void putSessionHandler(MemoryIdVO memoryIdVO,
                                  Boolean websearch,
                                  Boolean reasoning,
                                  ChatStreamingResponseHandler responseHandler,
                                  AiAccessUserVO userVO) {
        Session session = getSession(memoryIdVO, true);
        session.responseHandler = responseHandler;
        session.websearch = websearch;
        session.reasoning = reasoning;
        session.user = userVO;
    }

    public void putSessionQuestionClassify(MemoryIdVO memoryIdVO, QuestionClassifyListVO classifyListVO) {
        Session session = getSession(memoryIdVO, true);
        session.classifyListVO = classifyListVO;
    }

    public void putSessionVariables(MemoryIdVO memoryIdVO,
                                    AiVariablesVO variables) {
        Session session = getSession(memoryIdVO, true);
        session.variables = variables;
    }

    public void addSessionJsonSchema(MemoryIdVO memoryIdVO,
                                     String aiJsonschemaIds,
                                     AiJsonschemaMapper aiJsonschemaMapper,
                                     Executor threadPoolTaskExecutor) {
        Session session = getSession(memoryIdVO, true);
        session.threadPoolTaskExecutor = threadPoolTaskExecutor;
        Collection<AiJsonschema> jsonschemaList = Optional.ofNullable(aiJsonschemaIds)
                .filter(StringUtils::hasText)
                .map(e -> Arrays.asList(e.split(",")))
                .map(e -> e.stream().filter(o -> !session.jsonschemaMap.containsKey(o)).collect(Collectors.toList()))
                .filter(e -> !e.isEmpty())
                .map(aiJsonschemaMapper::selectBatchIds)
                .orElseGet(Collections::emptyList);
        for (AiJsonschema jsonschema : jsonschemaList) {
            session.jsonschemaMap.put(Objects.toString(jsonschema.getId(), ""), jsonschema);
        }
    }

    public boolean isEnableJsonschema(Object memoryIdVO, String jsonSchemaEnum) {
        Session session = getSession(memoryIdVO, false);
        if (session == null) {
            return false;
        }
        Collection<AiJsonschema> jsonschemaList = session.jsonschemaMap.values();
        return jsonschemaList.stream()
                .anyMatch(e -> Objects.equals(jsonSchemaEnum, e.getJsonSchemaEnum()) && Boolean.TRUE.equals(e.getEnableFlag()));
    }

    public AiJsonschema getSessionJsonschema(Object memoryIdVO, String jsonSchemaEnum) {
        Session session = getSession(memoryIdVO, false);
        if (session == null) {
            return null;
        }
        Collection<AiJsonschema> jsonschemaList = session.jsonschemaMap.values();
        if (jsonschemaList.isEmpty()) {
            return null;
        }
        return jsonschemaList.stream()
                .filter(e -> Objects.equals(jsonSchemaEnum, e.getJsonSchemaEnum()) && Boolean.TRUE.equals(e.getEnableFlag()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 提示词中是否使用了变量
     *
     * @param memoryIdVO     memoryIdVO
     * @param jsonSchemaEnum jsonSchemaEnum
     * @param varKeys        varKeys
     * @return 使用了变量
     */
    public boolean existPromptVariableKey(Object memoryIdVO, String jsonSchemaEnum, String... varKeys) {
        AiJsonschema jsonschema = getSessionJsonschema(memoryIdVO, jsonSchemaEnum);
        if (jsonschema == null) {
            return false;
        }
        String[] ps = {jsonschema.getSystemPromptText(), jsonschema.getUserPromptText()};
        for (String p : ps) {
            if (AiUtil.existPromptVariable(p, Arrays.asList(varKeys))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取JsonSchema类型的模型（不要记忆）
     *
     * @param jsonschema jsonschema配置
     * @param type       type
     * @param <T>        类型
     * @return JsonSchema类型的模型
     * @throws JsonSchemaCreateException JsonSchema创建失败
     */
    public <T> T getSchemaNoMemory(AiJsonschema jsonschema, Class<T> type) throws JsonSchemaCreateException {
        return getSchema(null, jsonschema, type, false);
    }

    /**
     * 获取JsonSchema类型的模型（不要记忆）
     *
     * @param aiJsonschemaId jsonschema配置ID
     * @param type           type
     * @param <T>            类型
     * @return JsonSchema类型的模型
     * @throws JsonSchemaCreateException JsonSchema创建失败
     */
    public <T> T getSchemaByIdNoMemory(Integer aiJsonschemaId, Class<T> type) throws JsonSchemaCreateException {
        AiJsonschema jsonschema = selectJsonschemaById(aiJsonschemaId);
        return getSchema(null, jsonschema, type, false);
    }

    /**
     * 获取JsonSchema类型的模型
     *
     * @param memoryIdVO            memoryIdVO
     * @param type                  type
     * @param useChatMemoryProvider 是否给AI提供多伦对话历史记录
     * @param <T>                   类型
     * @return JsonSchema类型的模型
     * @throws JsonSchemaCreateException JsonSchema创建失败
     */
    public <T> T getSchema(Object memoryIdVO, Class<T> type, boolean useChatMemoryProvider) throws JsonSchemaCreateException {
        return getSchema(memoryIdVO, toJsonSchemaEnum(type), type, useChatMemoryProvider);
    }

    /**
     * 获取JsonSchema类型的模型
     *
     * @param memoryIdVO memoryIdVO
     * @param type       type
     * @param <T>        类型
     * @return JsonSchema类型的模型
     * @throws JsonSchemaCreateException JsonSchema创建失败
     */
    public <T> T getSchema(Object memoryIdVO, Class<T> type) throws JsonSchemaCreateException {
        return getSchema(memoryIdVO, toJsonSchemaEnum(type), type, false);
    }

    /**
     * 获取JsonSchema类型的模型
     *
     * @param memoryIdVO            memoryIdVO
     * @param type                  type
     * @param useChatMemoryProvider 是否给AI提供多伦对话历史记录
     * @param jsonSchemaEnum jsonSchemaEnum
     * @param <T>                   类型
     * @return JsonSchema类型的模型
     * @throws JsonSchemaCreateException JsonSchema创建失败
     */
    public <T> T getSchema(Object memoryIdVO, String jsonSchemaEnum, Class<T> type, boolean useChatMemoryProvider) throws JsonSchemaCreateException {
        AiJsonschema jsonschema = getSessionJsonschema(memoryIdVO, jsonSchemaEnum);
        if (jsonschema == null) {
            return null;
        }
        return getSchema(memoryIdVO, jsonschema, type, useChatMemoryProvider);
    }

    /**
     * 获取JsonSchema类型的模型
     *
     * @param memoryIdVO            memoryIdVO
     * @param jsonschema            jsonschema配置
     * @param type                  type
     * @param useChatMemoryProvider 是否给AI提供多伦对话历史记录
     * @param <T>                   类型
     * @return JsonSchema类型的模型
     * @throws JsonSchemaCreateException JsonSchema创建失败
     */
    public <T> T getSchema(Object memoryIdVO, AiJsonschema jsonschema, Class<T> type, boolean useChatMemoryProvider) throws JsonSchemaCreateException {
        String apiKey = jsonschema.getApiKey();
        String baseUrl = jsonschema.getBaseUrl();
        String modelName = jsonschema.getModelName();
        String responseFormat = jsonschema.getResponseFormat();
        Double topP = jsonschema.getTopP();
        Double temperature = jsonschema.getTemperature();
        Integer maxCompletionTokens = jsonschema.getMaxCompletionTokens();
        Integer timeoutMs = jsonschema.getTimeoutMs();
        AiModelVO[] aiModels = jsonSchemaInstanceMap
                .computeIfAbsent(uniqueKey(apiKey, baseUrl, modelName, responseFormat, maxCompletionTokens, temperature, topP, type, timeoutMs), k -> Collections.synchronizedMap(new WeakHashMap<>()))
                .computeIfAbsent(type, k -> {
                    AiModelVO[] arrays = new AiModelVO[schemaInstanceCount];
                    for (int i = 0; i < arrays.length; i++) {
                        arrays[i] = createJsonSchemaModel(apiKey, baseUrl, modelName, maxCompletionTokens, temperature, topP, responseFormat, timeoutMs);
                    }
                    return arrays;
                });
        AtomicInteger index = schemasIndexMap.computeIfAbsent(type, e -> new AtomicInteger());
        AiModelVO aiModel = aiModels[index.getAndIncrement() % aiModels.length];
        return newInstance(aiModel, type, useChatMemoryProvider, memoryIdVO, jsonschema);
    }

    private <T> T newInstance(AiModelVO aiModel, Class<T> type, boolean useChatMemoryProvider,
                              Object memoryIdVO, AiJsonschema jsonschema) throws JsonSchemaCreateException {
        if (memoryIdVO == null) {
            memoryIdVO = MemoryIdVO.NULL;
        }
        try {
            Session session = getSession(memoryIdVO, false);
            ChatStreamingResponseHandler responseHandler;
            AiAccessUserVO user;
            AiVariablesVO variables;
            Map<String, Object> variablesMap;
            Boolean websearch;
            Boolean reasoning;
            QuestionClassifyListVO classifyListVO;
            Executor executor;
            if (session != null) {
                user = session.user;
                variables = session.variables;
                responseHandler = session.responseHandler;
                websearch = session.websearch;
                reasoning = session.reasoning;
                classifyListVO = session.classifyListVO;
                executor = session.threadPoolTaskExecutor;
            } else {
                user = new AiAccessUserVO();
                variables = new AiVariablesVO();
                responseHandler = ChatStreamingResponseHandler.EMPTY;
                websearch = null;
                reasoning = null;
                executor = Runnable::run;
                classifyListVO = new QuestionClassifyListVO();
            }
            variablesMap = BeanUtil.toMap(variables);

            String systemPromptText = jsonschema.getSystemPromptText();
            String userPromptText = jsonschema.getUserPromptText();
            List<Tools.ToolMethod> toolMethodList = AiUtil.initTool(aiToolService.selectToolMethodList(StringUtils.split(jsonschema.getAiToolIds(), ","), variablesMap), variables, user);
            AiServices<T> aiServices = new FunctionalInterfaceAiServices<>(new AiServiceContext(type), jsonschema.getId(), jsonschema.getJsonSchemaEnum(), systemPromptText, userPromptText,
                    variablesMap, responseHandler, toolMethodList, aiModel.isSupportChineseToolName(),
                    classifyListVO, websearch, reasoning, aiModel.modelName, memoryIdVO, executor);
            aiServices.streamingChatLanguageModel(FunctionalInterfaceAiServices.adapter(aiModel.streaming));
            if (useChatMemoryProvider && memoryIdVO != MemoryIdVO.NULL) {
                aiServices.chatMemoryProvider(this::getChatMemory);
            }
            return aiServices.build();
        } catch (Exception e) {
            throw new JsonSchemaCreateException(String.format("JsonSchema create error! id = %s,name =%s, cause = %s",
                    jsonschema.getId(), jsonschema.getJsonSchemaEnum(), e),
                    e, jsonschema);
        }
    }

    public MStateUnknownJsonSchema getMStateUnknownJsonSchema(MemoryIdVO memoryIdVO) throws JsonSchemaCreateException {
        return getSchema(memoryIdVO, MStateUnknownJsonSchema.class, false);
    }

    public ReasoningJsonSchema getReasoningJsonSchema(MemoryIdVO memoryIdVO) throws JsonSchemaCreateException {
        return getSchema(memoryIdVO, ReasoningJsonSchema.class, false);
    }

    public ActingJsonSchema getActingJsonSchema(MemoryIdVO memoryIdVO) throws JsonSchemaCreateException {
        return getSchema(memoryIdVO, ActingJsonSchema.class, false);
    }

    public WebsearchReduceJsonSchema getWebsearchReduceJsonSchema(MemoryIdVO memoryIdVO) throws JsonSchemaCreateException {
        return getSchema(memoryIdVO, WebsearchReduceJsonSchema.class, false);
    }

    public MStateKnownJsonSchema getMStateknownJsonSchema(MemoryIdVO memoryIdVO) throws JsonSchemaCreateException {
        return getSchema(memoryIdVO, MStateKnownJsonSchema.class, false);
    }

    public WhetherWaitingForAiJsonSchema getWhetherWaitingForAiJsonSchema(MemoryIdVO memoryIdVO) throws JsonSchemaCreateException {
        return getSchema(memoryIdVO, WhetherWaitingForAiJsonSchema.class, false);
    }

    public QuestionClassifySchema getQuestionClassifySchema(MemoryIdVO memoryIdVO) throws JsonSchemaCreateException {
        return getSchema(memoryIdVO, QuestionClassifySchema.class, false);
    }

    /**
     * 获取记忆
     *
     * @param memoryIdVO memoryIdVO
     * @return 记忆
     */
    public JsonSchemaTokenWindowChatMemory getChatMemory(Object memoryIdVO) {
        Session session = memoryMap.get(memoryIdVO);
        return session == null ? null : session.chatMemory;
    }

    private Session getSession(Object memoryIdVO, boolean create) {
        if (create) {
            return memoryMap.computeIfAbsent(memoryIdVO, k -> new Session());
        } else {
            return memoryMap.get(memoryIdVO);
        }
    }

    /**
     * 创建JsonSchema类型的模型
     *
     * @param apiKey              apiKey
     * @param baseUrl             baseUrl
     * @param modelName           modelName
     * @param maxCompletionTokens maxCompletionTokens
     * @param temperature         temperature
     * @param topP                topP
     * @param responseFormat      responseFormat
     * @param timeoutMs           timeoutMs
     * @return JsonSchema类型的模型
     */
    private AiModelVO createJsonSchemaModel(String apiKey,
                                            String baseUrl,
                                            String modelName,
                                            Integer maxCompletionTokens,
                                            Double temperature,
                                            Double topP,
                                            String responseFormat,
                                            Integer timeoutMs) {
        if (topP == null || topP <= 0) {
            topP = 0.1;
        }
        if (temperature == null || temperature <= 0) {
            temperature = 0.1;
        }
        if (maxCompletionTokens != null && maxCompletionTokens <= 0) {
            maxCompletionTokens = null;
        }
        // https://docs.langchain4j.dev/integrations/language-models/open-ai#structured-outputs-for-json-mode
        // 大模型stream需要4.12.0, 大模型非stream需要okhttp3，这里舍弃非stream，保持整个项目全用stream
//        OpenAiChatModel model = OpenAiChatModel.builder()
//                .topP(topP)
//                .temperature(temperature)
//                .maxCompletionTokens(maxCompletionTokens)
//                .apiKey(apiKey)
//                .baseUrl(baseUrl)
//                .modelName(modelName)
//                .responseFormat(responseFormatType.name())
//                .strictJsonSchema(true)
//                .timeout(ofSeconds(60))
//                .tokenizer(new OpenAiTokenizer())
//                .build();
        if (timeoutMs == null || timeoutMs <= 0) {
            timeoutMs = 60_000;
        }
        // 大模型stream需要4.12.0, 大模型非stream需要okhttp3，这里舍弃非stream，保持整个项目全用stream
        OpenAiChatClient streaming = OpenAiChatClient.builder()
                .topP(topP)
                .temperature(temperature)
                .maxCompletionTokens(maxCompletionTokens)
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .responseFormat(responseFormat)
                .timeout(Duration.ofMillis(timeoutMs))
                .connectTimeout(connectTimeout)
                .strictJsonSchema(true)
                .build();
        return new AiModelVO(baseUrl, modelName, streaming);
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public AiJsonschema selectJsonschemaById(Serializable id) {
        return aiJsonschemaMapper.selectById(id);
    }

    public List<AiJsonschema> selectJsonschemaByIds(Collection<? extends Serializable> idList) {
        return aiJsonschemaMapper.selectBatchIds(idList);
    }

    public static class Session {
        final Map<String, AiJsonschema> jsonschemaMap = new ConcurrentHashMap<>();
        Executor threadPoolTaskExecutor;
        AiAccessUserVO user;
        AiVariablesVO variables;
        Boolean websearch;
        Boolean reasoning;
        QuestionClassifyListVO classifyListVO;
        ChatStreamingResponseHandler responseHandler;
        JsonSchemaTokenWindowChatMemory chatMemory;
    }
}
