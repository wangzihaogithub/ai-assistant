package com.github.aiassistant.service.jsonschema;

import com.github.aiassistant.entity.AiJsonschema;
import com.github.aiassistant.entity.model.chat.AiModel;
import com.github.aiassistant.entity.model.chat.AiVariables;
import com.github.aiassistant.entity.model.chat.MemoryIdVO;
import com.github.aiassistant.entity.model.user.AiAccessUserVO;
import com.github.aiassistant.service.text.ChatStreamingResponseHandler;
import com.github.aiassistant.service.text.repository.JsonSchemaTokenWindowChatMemory;
import com.github.aiassistant.service.text.tools.AiToolServiceImpl;
import com.github.aiassistant.service.text.tools.Tools;
import com.github.aiassistant.util.AiUtil;
import com.github.aiassistant.util.BeanUtil;
import com.github.aiassistant.util.StringUtils;
import dev.ai4j.openai4j.chat.ResponseFormatType;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import dev.langchain4j.service.AiServiceContext;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.FunctionalInterfaceAiServices;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static java.time.Duration.ofSeconds;

/**
 * JsonSchema类型的模型
 */
// @Component
public class LlmJsonSchemaApiService {
    /**
     * JsonSchema的实例
     */
    private final Map<String, Map<Class, AiModel[]>> jsonSchemaInstanceMap = new ConcurrentHashMap<>();
    /**
     * JsonSchema的本地记忆
     */
    private final Map<Object, Session> memoryMap = Collections.synchronizedMap(new IdentityHashMap<>());
    private final int schemaInstanceCount;
    private final Map<Class, AtomicInteger> schemasIndexMap = new ConcurrentHashMap<>();
    // @Autowired
    private final AiToolServiceImpl aiToolService;

    public LlmJsonSchemaApiService(AiToolServiceImpl aiToolService) {
        this(aiToolService, 3);
    }

    public LlmJsonSchemaApiService(AiToolServiceImpl aiToolService, int schemaInstanceCount) {
        this.aiToolService = aiToolService;
        this.schemaInstanceCount = schemaInstanceCount;
    }

    private static String uniqueKey(String apiKey,
                                    String baseUrl,
                                    String modelName,
                                    String responseFormat,
                                    Integer maxCompletionTokens,
                                    Double temperature,
                                    Double topP,
                                    Class<?> type) {
        return apiKey
                + ":" + baseUrl
                + ":" + modelName
                + ":" + responseFormat
                + ":" + maxCompletionTokens
                + ":" + temperature
                + ":" + topP
                + ":" + type.getName();
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
                                  ChatStreamingResponseHandler responseHandler,
                                  AiVariables variables,
                                  AiAccessUserVO userVO) {
        Session session = getSession(memoryIdVO, true);
        session.responseHandler = responseHandler;
        session.variables = variables;
        session.user = userVO;
    }

    public void putSessionJsonSchema(MemoryIdVO memoryIdVO,
                                     Collection<AiJsonschema> jsonschemaList) {
        Session session = getSession(memoryIdVO, true);
        session.jsonschemaList = jsonschemaList;
    }

    public boolean isEnableJsonschema(MemoryIdVO memoryIdVO, String jsonSchemaEnum) {
        Session session = getSession(memoryIdVO, false);
        if (session == null) {
            return false;
        }
        Collection<AiJsonschema> jsonschemaList = session.jsonschemaList;
        return jsonschemaList != null && jsonschemaList.stream()
                .anyMatch(e -> Objects.equals(jsonSchemaEnum, e.getJsonSchemaEnum()) && Boolean.TRUE.equals(e.getEnableFlag()));
    }

    public AiJsonschema getJsonschema(MemoryIdVO memoryIdVO, String jsonSchemaEnum) {
        Session session = getSession(memoryIdVO, false);
        if (session == null) {
            return null;
        }
        Collection<AiJsonschema> jsonschemaList = session.jsonschemaList;
        if (jsonschemaList == null || jsonschemaList.isEmpty()) {
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
    public boolean existPromptVariableKey(MemoryIdVO memoryIdVO, String jsonSchemaEnum, String... varKeys) {
        AiJsonschema jsonschema = getJsonschema(memoryIdVO, jsonSchemaEnum);
        if (jsonschema == null) {
            return false;
        }
        String[] ps = {jsonschema.getSystemPromptText(), jsonschema.getUserPromptText(), jsonschema.getKnPromptText()};
        for (String p : ps) {
            if (AiUtil.existPromptVariable(p, Arrays.asList(varKeys))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取JsonSchema类型的模型
     *
     * @param memoryIdVO memoryIdVO
     * @param type       type
     * @param memory     memory
     * @param <T>        类型
     * @return JsonSchema类型的模型
     */
    public <T> T getSchema(MemoryIdVO memoryIdVO, Class<T> type, boolean memory) {
        return getSchema(memoryIdVO, toJsonSchemaEnum(type), type, memory);
    }

    public <T> T getSchema(MemoryIdVO memoryIdVO, Class<T> type) {
        return getSchema(memoryIdVO, type, false);
    }

    public <T> T getSchema(MemoryIdVO memoryIdVO, String jsonSchemaEnum, Class<T> type, boolean memory) {
        AiJsonschema jsonschema = getJsonschema(memoryIdVO, jsonSchemaEnum);
        if (jsonschema == null) {
            return null;
        }
        String apiKey = jsonschema.getApiKey();
        String baseUrl = jsonschema.getBaseUrl();
        String modelName = jsonschema.getModelName();
        String responseFormat = jsonschema.getResponseFormat();
        Double topP = jsonschema.getTopP();
        Double temperature = jsonschema.getTemperature();
        Integer maxCompletionTokens = jsonschema.getMaxCompletionTokens();
        AiModel[] aiModels = jsonSchemaInstanceMap
                .computeIfAbsent(uniqueKey(apiKey, baseUrl, modelName, responseFormat, maxCompletionTokens, temperature, topP, type), k -> new ConcurrentHashMap<>())
                .computeIfAbsent(type, k -> {
                    AiModel[] arrays = new AiModel[schemaInstanceCount];
                    for (int i = 0; i < arrays.length; i++) {
                        AiModel aiModel = createJsonSchemaModel(apiKey, baseUrl, modelName, maxCompletionTokens, temperature, topP, responseFormat);
                        arrays[i] = aiModel;
                    }
                    return arrays;
                });
        AtomicInteger index = schemasIndexMap.computeIfAbsent(type, e -> new AtomicInteger());
        AiModel aiModel = aiModels[index.getAndIncrement() % aiModels.length];
        return newInstance(aiModel, type, memory, memoryIdVO, jsonschema, jsonSchemaEnum);
    }

    private <T> T newInstance(AiModel aiModel, Class<T> type, boolean memory,
                              MemoryIdVO memoryIdVO, AiJsonschema jsonschema,
                              String jsonSchemaEnum) {
        Session session = getSession(memoryIdVO, false);
        ChatStreamingResponseHandler responseHandler = null;
        AiAccessUserVO user = null;
        AiVariables variables = null;
        Map<String, Object> variablesMap = new HashMap<>();
        if (session != null) {
            user = session.user;
            variables = session.variables;
            responseHandler = session.responseHandler;
            variablesMap = BeanUtil.toMap(variables);
        }

        List<Tools.ToolMethod> toolMethodList = new ArrayList<>();
        String systemPromptText = null;
        String userPromptText = null;
        String knPromptText = null;
        if (jsonschema != null) {
            systemPromptText = jsonschema.getSystemPromptText();
            userPromptText = jsonschema.getUserPromptText();
            knPromptText = jsonschema.getKnPromptText();
            toolMethodList = AiUtil.initTool(aiToolService.selectToolMethodList(StringUtils.split(jsonschema.getAiToolIds(), ",")), variables, user);
        }
        AiServices<T> aiServices = new FunctionalInterfaceAiServices<>(new AiServiceContext(type), systemPromptText, userPromptText,
                knPromptText, variablesMap, responseHandler, toolMethodList, aiModel.isSupportChineseToolName(), aiModel.modelName, memoryIdVO);
        aiServices.chatLanguageModel(aiModel.model);
        aiServices.streamingChatLanguageModel(aiModel.streaming);
        if (memory) {
            aiServices.chatMemoryProvider(this::getChatMemory);
        }
        return aiServices.build();
    }

    public MStateUnknownJsonSchema getMStateUnknownJsonSchema(MemoryIdVO memoryIdVO) {
        return getSchema(memoryIdVO, MStateUnknownJsonSchema.class, false);
    }

    public ReasoningJsonSchema getReasoningJsonSchema(MemoryIdVO memoryIdVO) {
        return getSchema(memoryIdVO, ReasoningJsonSchema.class, false);
    }

    public ActingJsonSchema getActingJsonSchema(MemoryIdVO memoryIdVO) {
        return getSchema(memoryIdVO, ActingJsonSchema.class, false);
    }

    public WebsearchReduceJsonSchema getWebsearchReduceJsonSchema(MemoryIdVO memoryIdVO) {
        return getSchema(memoryIdVO, WebsearchReduceJsonSchema.class, false);
    }

    public MStateKnownJsonSchema getMStateknownJsonSchema(MemoryIdVO memoryIdVO) {
        return getSchema(memoryIdVO, MStateKnownJsonSchema.class, false);
    }

    public WhetherWaitingForAiJsonSchema getWhetherWaitingForAiJsonSchema(MemoryIdVO memoryIdVO) {
        return getSchema(memoryIdVO, WhetherWaitingForAiJsonSchema.class, false);
    }

    public QuestionClassifySchema getQuestionClassifySchema(MemoryIdVO memoryIdVO) {
        return getSchema(memoryIdVO, QuestionClassifySchema.class, false);
    }

    /**
     * 获取记忆
     *
     * @param memoryIdVO memoryIdVO
     * @return 记忆
     */
    public JsonSchemaTokenWindowChatMemory getChatMemory(Object memoryIdVO) {
        MemoryIdVO key = (MemoryIdVO) memoryIdVO;
        Session session = memoryMap.get(key);
        return session == null ? null : session.chatMemory;
    }

    private Session getSession(Object memoryIdVO, boolean create) {
        MemoryIdVO key = (MemoryIdVO) memoryIdVO;
        if (create) {
            return memoryMap.computeIfAbsent(key, k -> new Session());
        } else {
            return memoryMap.get(key);
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
     * @return JsonSchema类型的模型
     */
    private AiModel createJsonSchemaModel(String apiKey,
                                          String baseUrl,
                                          String modelName,
                                          Integer maxCompletionTokens,
                                          Double temperature,
                                          Double topP,
                                          String responseFormat) {
        ResponseFormatType responseFormatType = ResponseFormatType.valueOf(responseFormat);
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
        // 大模型stream需要4.12.0, 大模型非stream需要okhttp3，这里舍弃非stream，保持整个项目全用stream
        OpenAiStreamingChatModel streaming = OpenAiStreamingChatModel.builder()
                .topP(topP)
                .temperature(temperature)
                .maxCompletionTokens(maxCompletionTokens)
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .responseFormat(responseFormatType == ResponseFormatType.JSON_SCHEMA ? ResponseFormatType.JSON_OBJECT.name() : responseFormatType.name())
                .timeout(ofSeconds(60))
                .tokenizer(new OpenAiTokenizer())
                .build();
        return new AiModel(baseUrl, modelName, null, streaming);
    }

    public static class Session {
        AiAccessUserVO user;
        AiVariables variables;
        ChatStreamingResponseHandler responseHandler;
        JsonSchemaTokenWindowChatMemory chatMemory;
        Collection<AiJsonschema> jsonschemaList;
        Collection<Tools.ToolMethod> toolMethods;
    }
}
