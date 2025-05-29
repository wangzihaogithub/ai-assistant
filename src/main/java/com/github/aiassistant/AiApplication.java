package com.github.aiassistant;

import com.github.aiassistant.dao.*;
import com.github.aiassistant.entity.model.chat.ChatQueryReq;
import com.github.aiassistant.entity.model.chat.MemoryIdVO;
import com.github.aiassistant.entity.model.user.AiAccessUserVO;
import com.github.aiassistant.platform.Mybatis3DAOProvider;
import com.github.aiassistant.service.accessuser.AccessUserService;
import com.github.aiassistant.service.jsonschema.LlmJsonSchemaApiService;
import com.github.aiassistant.service.text.AiAssistantServiceImpl;
import com.github.aiassistant.service.text.AiQuestionClassifyService;
import com.github.aiassistant.service.text.LlmTextApiService;
import com.github.aiassistant.service.text.acting.ActingService;
import com.github.aiassistant.service.text.chat.*;
import com.github.aiassistant.service.text.embedding.KnSettingWebsearchBlacklistServiceImpl;
import com.github.aiassistant.service.text.embedding.KnnApiService;
import com.github.aiassistant.service.text.memory.*;
import com.github.aiassistant.service.text.reasoning.ReasoningService;
import com.github.aiassistant.service.text.repository.JdbcSessionMessageRepository;
import com.github.aiassistant.service.text.tools.AiToolServiceImpl;
import com.github.aiassistant.service.text.tools.Tools;
import com.github.aiassistant.service.text.variables.AiVariablesService;
import com.github.aiassistant.serviceintercept.*;
import org.elasticsearch.client.RestClient;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;

public class AiApplication {
    private final RestClient embeddingStore;
    private final Function<String, Tools> toolsMap;
    private final Function<Class<? extends ServiceIntercept>, Collection<ServiceIntercept>> interceptMap;
    private final DAOProvider daoProvider;

    private final AiToolMapper aiToolMapper;
    private final AiToolParameterMapper aiToolParameterMapper;
    private final AiQuestionClassifyMapper aiQuestionClassifyMapper;
    private final AiQuestionClassifyAssistantMapper aiQuestionClassifyAssistantMapper;
    private final AiVariablesMapper aiVariablesMapper;
    private final AiEmbeddingMapper aiEmbeddingMapper;
    private final KnSettingWebsearchBlacklistMapper knSettingWebsearchBlacklistMapper;
    private final AiMemoryMapper aiMemoryMapper;
    private final AiMemoryMessageKnMapper aiMemoryMessageKnMapper;
    private final AiMemoryMessageMapper aiMemoryMessageMapper;
    private final AiMemoryMessageToolMapper aiMemoryMessageToolMapper;
    private final AiMemoryMessageMetadataMapper aiMemoryMessageMetadataMapper;
    private final AiMemoryRagMapper aiMemoryRagMapper;
    private final AiMemoryRagDocMapper aiMemoryRagDocMapper;
    private final AiChatAbortMapper aiChatAbortMapper;
    private final AiAssistantFewshotMapper aiAssistantFewshotMapper;
    private final AiAssistantJsonschemaMapper aiAssistantJsonschemaMapper;
    private final AiAssistantKnMapper aiAssistantKnMapper;
    private final AiAssistantMapper aiAssistantMapper;
    private final AiAssistantMstateMapper aiAssistantMstateMapper;
    private final AiChatClassifyMapper aiChatClassifyMapper;
    private final AiChatHistoryMapper aiChatHistoryMapper;
    private final AiChatMapper aiChatMapper;
    private final AiChatReasoningMapper aiChatReasoningMapper;
    private final AiChatReasoningPlanMapper aiChatReasoningPlanMapper;
    private final AiChatWebsearchMapper aiChatWebsearchMapper;
    private final AiMemoryErrorMapper aiMemoryErrorMapper;
    private final AiMemoryMstateMapper aiMemoryMstateMapper;

    private final AccessUserService accessUserService;
    private final LlmJsonSchemaApiService llmJsonSchemaApiService;
    private final KnnApiService knnApiService;
    private final LlmTextApiService llmTextApiService;

    private final AiChatServiceImpl aiChatService;
    private final AiToolServiceImpl aiToolService;
    private final ActingService actingService;
    private final ReasoningService reasoningService;
    private final AiQuestionClassifyService aiQuestionClassifyService;
    private final KnSettingWebsearchBlacklistServiceImpl knSettingWebsearchBlacklistServiceImpl;
    private final AiMemoryMessageServiceImpl aiMemoryMessageService;
    private final AiAssistantServiceImpl aiAssistantService;
    private final AiChatClassifyServiceImpl aiChatClassifyService;
    private final AiMemoryServiceImpl aiMemoryService;
    private final AiChatReasoningServiceImpl aiChatReasoningService;
    private final AiChatWebsearchResultMapper aiChatWebsearchResultMapper;
    private final AiChatWebsearchServiceImpl aiChatWebsearchService;
    private final AiChatHistoryServiceImpl aiChatHistoryService;
    private final AiMemoryErrorServiceImpl aiMemoryErrorService;
    private final AiChatAbortServiceImpl aiChatAbortService;
    private final AiMemoryMstateServiceImpl aiMemoryMstateService;
    private final AiMemoryRagServiceImpl aiMemoryRagService;
    private final AiVariablesService aiVariablesService;

    public AiApplication(DataSource dataSource,
                         RestClient embeddingStore,
                         Function<String, Tools> toolsMap,
                         Function<Class<? extends ServiceIntercept>, Collection<ServiceIntercept>> interceptMap) {
        this(null, new Mybatis3DAOProvider(dataSource), embeddingStore, toolsMap, interceptMap);
    }

    public AiApplication(Executor threadPoolTaskExecutor,
                         DataSource dataSource,
                         RestClient embeddingStore,
                         Function<String, Tools> toolsMap,
                         Function<Class<? extends ServiceIntercept>, Collection<ServiceIntercept>> interceptMap) {
        this(threadPoolTaskExecutor, new Mybatis3DAOProvider(dataSource), embeddingStore, toolsMap, interceptMap);
    }

    public AiApplication(Executor threadPoolTaskExecutor,
                         DAOProvider daoProvider,
                         RestClient embeddingStore,
                         Function<String, Tools> toolsMap,
                         Function<Class<? extends ServiceIntercept>, Collection<ServiceIntercept>> interceptMap) {
        this.embeddingStore = embeddingStore;
        this.toolsMap = toolsMap;
        this.interceptMap = interceptMap;
        this.daoProvider = daoProvider;
        this.aiToolMapper = daoProvider.getMapper(AiToolMapper.class);
        this.aiToolParameterMapper = daoProvider.getMapper(AiToolParameterMapper.class);
        this.aiQuestionClassifyMapper = daoProvider.getMapper(AiQuestionClassifyMapper.class);
        this.aiQuestionClassifyAssistantMapper = daoProvider.getMapper(AiQuestionClassifyAssistantMapper.class);
        this.aiVariablesMapper = daoProvider.getMapper(AiVariablesMapper.class);
        this.aiEmbeddingMapper = daoProvider.getMapper(AiEmbeddingMapper.class);
        this.knSettingWebsearchBlacklistMapper = daoProvider.getMapper(KnSettingWebsearchBlacklistMapper.class);
        this.aiMemoryMapper = daoProvider.getMapper(AiMemoryMapper.class);
        this.aiMemoryMessageKnMapper = daoProvider.getMapper(AiMemoryMessageKnMapper.class);
        this.aiMemoryMessageMapper = daoProvider.getMapper(AiMemoryMessageMapper.class);
        this.aiMemoryMessageToolMapper = daoProvider.getMapper(AiMemoryMessageToolMapper.class);
        this.aiChatAbortMapper = daoProvider.getMapper(AiChatAbortMapper.class);
        this.aiAssistantFewshotMapper = daoProvider.getMapper(AiAssistantFewshotMapper.class);
        this.aiAssistantJsonschemaMapper = daoProvider.getMapper(AiAssistantJsonschemaMapper.class);
        this.aiAssistantKnMapper = daoProvider.getMapper(AiAssistantKnMapper.class);
        this.aiAssistantMapper = daoProvider.getMapper(AiAssistantMapper.class);
        this.aiAssistantMstateMapper = daoProvider.getMapper(AiAssistantMstateMapper.class);
        this.aiChatClassifyMapper = daoProvider.getMapper(AiChatClassifyMapper.class);
        this.aiChatHistoryMapper = daoProvider.getMapper(AiChatHistoryMapper.class);
        this.aiChatMapper = daoProvider.getMapper(AiChatMapper.class);
        this.aiMemoryMstateMapper = daoProvider.getMapper(AiMemoryMstateMapper.class);
        this.aiMemoryErrorMapper = daoProvider.getMapper(AiMemoryErrorMapper.class);
        this.aiChatReasoningMapper = daoProvider.getMapper(AiChatReasoningMapper.class);
        this.aiChatReasoningPlanMapper = daoProvider.getMapper(AiChatReasoningPlanMapper.class);
        this.aiChatWebsearchMapper = daoProvider.getMapper(AiChatWebsearchMapper.class);
        this.aiChatWebsearchResultMapper = daoProvider.getMapper(AiChatWebsearchResultMapper.class);
        this.aiMemoryMessageMetadataMapper = daoProvider.getMapper(AiMemoryMessageMetadataMapper.class);
        this.aiMemoryRagDocMapper = daoProvider.getMapper(AiMemoryRagDocMapper.class);
        this.aiMemoryRagMapper = daoProvider.getMapper(AiMemoryRagMapper.class);

        this.aiToolService = new AiToolServiceImpl(aiToolMapper, aiToolParameterMapper, toolsMap);
        this.llmJsonSchemaApiService = new LlmJsonSchemaApiService(aiToolService);
        this.actingService = new ActingService(llmJsonSchemaApiService);
        this.reasoningService = new ReasoningService(llmJsonSchemaApiService);
        this.knnApiService = new KnnApiService(embeddingStore, aiEmbeddingMapper);
        this.aiQuestionClassifyService = new AiQuestionClassifyService(aiQuestionClassifyMapper, aiQuestionClassifyAssistantMapper, llmJsonSchemaApiService);
        this.knSettingWebsearchBlacklistServiceImpl = new KnSettingWebsearchBlacklistServiceImpl(knSettingWebsearchBlacklistMapper);
        this.aiMemoryService = new AiMemoryServiceImpl(aiMemoryMapper);
        this.aiMemoryMessageService = new AiMemoryMessageServiceImpl(aiMemoryMapper, aiMemoryMessageKnMapper, aiMemoryMessageMapper, aiMemoryMessageToolMapper, aiMemoryMessageMetadataMapper, getServiceInterceptSupplier(AiMemoryMessageServiceIntercept.class, interceptMap));
        this.aiAssistantService = new AiAssistantServiceImpl(aiAssistantMapper);
        this.aiChatClassifyService = new AiChatClassifyServiceImpl(aiChatClassifyMapper);
        this.aiChatService = new AiChatServiceImpl(aiChatMapper, aiChatHistoryMapper, aiMemoryService);
        this.aiChatHistoryService = new AiChatHistoryServiceImpl(aiMemoryMessageMapper, aiChatHistoryMapper, aiMemoryErrorMapper, aiChatWebsearchMapper, aiChatMapper, aiChatAbortMapper, aiMemoryMessageMetadataMapper, getServiceInterceptSupplier(AiChatHistoryServiceIntercept.class, interceptMap));

        this.aiChatReasoningService = new AiChatReasoningServiceImpl(aiChatReasoningMapper, aiChatReasoningPlanMapper, aiChatHistoryMapper);
        this.aiChatWebsearchService = new AiChatWebsearchServiceImpl(aiChatWebsearchMapper, aiChatWebsearchResultMapper, aiChatHistoryMapper);
        this.aiMemoryErrorService = new AiMemoryErrorServiceImpl(aiMemoryErrorMapper, aiChatHistoryService);
        this.aiChatAbortService = new AiChatAbortServiceImpl(aiChatAbortMapper, aiChatHistoryService);
        this.aiMemoryMstateService = new AiMemoryMstateServiceImpl(aiMemoryMstateMapper);
        this.accessUserService = new AccessUserService(aiChatMapper, aiAssistantMapper, aiAssistantKnMapper, aiChatHistoryService, getServiceInterceptSupplier(AccessUserServiceIntercept.class, interceptMap));
        this.aiMemoryRagService = new AiMemoryRagServiceImpl(aiMemoryRagMapper, aiMemoryRagDocMapper);

        this.aiVariablesService = new AiVariablesService(aiMemoryMstateService, aiVariablesMapper, getServiceInterceptSupplier(AiVariablesServiceIntercept.class, interceptMap));
        this.llmTextApiService = new LlmTextApiService(llmJsonSchemaApiService, aiQuestionClassifyService, aiAssistantJsonschemaMapper, aiAssistantFewshotMapper, aiToolService, aiVariablesService, knnApiService, actingService, reasoningService, knSettingWebsearchBlacklistServiceImpl,
                threadPoolTaskExecutor, getServiceInterceptSupplier(LlmTextApiServiceIntercept.class, interceptMap));
    }

    private static <T extends ServiceIntercept> Supplier<Collection<T>> getServiceInterceptSupplier(Class<T> clazz, Function<Class<? extends ServiceIntercept>, Collection<ServiceIntercept>> interceptMap) {
        if (interceptMap == null) {
            return Collections::emptyList;
        }
        return () -> {
            Collection collection = interceptMap.apply(clazz);
            return collection == null ? Collections.emptyList() : collection;
        };
    }

    public RestClient getEmbeddingStore() {
        return embeddingStore;
    }

    public Function<String, Tools> getToolsMap() {
        return toolsMap;
    }

    public Function<Class<? extends ServiceIntercept>, Collection<ServiceIntercept>> getInterceptMap() {
        return interceptMap;
    }

    public DAOProvider getDaoProvider() {
        return daoProvider;
    }

    public AiMemoryRagDocMapper getAiMemoryRagDocMapper() {
        return aiMemoryRagDocMapper;
    }

    public AiMemoryRagMapper getAiMemoryRagMapper() {
        return aiMemoryRagMapper;
    }

    public AiMemoryMessageMetadataMapper getAiMemoryMessageMetadataMapper() {
        return aiMemoryMessageMetadataMapper;
    }

    public AiToolMapper getAiToolMapper() {
        return aiToolMapper;
    }

    public AiToolParameterMapper getAiToolParameterMapper() {
        return aiToolParameterMapper;
    }

    public AiQuestionClassifyMapper getAiQuestionClassifyMapper() {
        return aiQuestionClassifyMapper;
    }

    public AiQuestionClassifyAssistantMapper getAiQuestionClassifyAssistantMapper() {
        return aiQuestionClassifyAssistantMapper;
    }

    public AiVariablesMapper getAiVariablesMapper() {
        return aiVariablesMapper;
    }

    public AiEmbeddingMapper getAiEmbeddingMapper() {
        return aiEmbeddingMapper;
    }

    public KnSettingWebsearchBlacklistMapper getKnSettingWebsearchBlacklistMapper() {
        return knSettingWebsearchBlacklistMapper;
    }

    public AiMemoryMapper getAiMemoryMapper() {
        return aiMemoryMapper;
    }

    public AiMemoryMessageKnMapper getAiMemoryMessageKnMapper() {
        return aiMemoryMessageKnMapper;
    }

    public AiMemoryMessageMapper getAiMemoryMessageMapper() {
        return aiMemoryMessageMapper;
    }

    public AiMemoryMessageToolMapper getAiMemoryMessageToolMapper() {
        return aiMemoryMessageToolMapper;
    }

    public AiChatAbortMapper getAiChatAbortMapper() {
        return aiChatAbortMapper;
    }

    public AiAssistantFewshotMapper getAiAssistantFewshotMapper() {
        return aiAssistantFewshotMapper;
    }

    public AiAssistantJsonschemaMapper getAiAssistantJsonschemaMapper() {
        return aiAssistantJsonschemaMapper;
    }

    public AiAssistantKnMapper getAiAssistantKnMapper() {
        return aiAssistantKnMapper;
    }

    public AiAssistantMapper getAiAssistantMapper() {
        return aiAssistantMapper;
    }

    public AiAssistantMstateMapper getAiAssistantMstateMapper() {
        return aiAssistantMstateMapper;
    }

    public AiChatClassifyMapper getAiChatClassifyMapper() {
        return aiChatClassifyMapper;
    }

    public AiChatHistoryMapper getAiChatHistoryMapper() {
        return aiChatHistoryMapper;
    }

    public AiChatMapper getAiChatMapper() {
        return aiChatMapper;
    }

    public AiChatReasoningMapper getAiChatReasoningMapper() {
        return aiChatReasoningMapper;
    }

    public AiChatReasoningPlanMapper getAiChatReasoningPlanMapper() {
        return aiChatReasoningPlanMapper;
    }

    public AiChatWebsearchMapper getAiChatWebsearchMapper() {
        return aiChatWebsearchMapper;
    }

    public AiMemoryErrorMapper getAiMemoryErrorMapper() {
        return aiMemoryErrorMapper;
    }

    public AiMemoryMstateMapper getAiMemoryMstateMapper() {
        return aiMemoryMstateMapper;
    }

    public AccessUserService getAccessUserService() {
        return accessUserService;
    }

    public LlmJsonSchemaApiService getLlmJsonSchemaApiService() {
        return llmJsonSchemaApiService;
    }

    public KnnApiService getKnnApiService() {
        return knnApiService;
    }

    public LlmTextApiService getLlmTextApiService() {
        return llmTextApiService;
    }

    public AiChatServiceImpl getAiChatService() {
        return aiChatService;
    }

    public AiToolServiceImpl getAiToolService() {
        return aiToolService;
    }

    public ActingService getActingService() {
        return actingService;
    }

    public ReasoningService getReasoningService() {
        return reasoningService;
    }

    public AiQuestionClassifyService getAiQuestionClassifyService() {
        return aiQuestionClassifyService;
    }

    public KnSettingWebsearchBlacklistServiceImpl getKnSettingWebsearchBlacklistServiceImpl() {
        return knSettingWebsearchBlacklistServiceImpl;
    }

    public AiMemoryMessageServiceImpl getAiMemoryMessageService() {
        return aiMemoryMessageService;
    }

    public AiAssistantServiceImpl getAiAssistantService() {
        return aiAssistantService;
    }

    public AiChatClassifyServiceImpl getAiChatClassifyService() {
        return aiChatClassifyService;
    }

    public AiMemoryServiceImpl getAiMemoryService() {
        return aiMemoryService;
    }

    public AiChatReasoningServiceImpl getAiChatReasoningService() {
        return aiChatReasoningService;
    }

    public AiChatWebsearchResultMapper getAiChatWebsearchResultMapper() {
        return aiChatWebsearchResultMapper;
    }

    public AiChatWebsearchServiceImpl getAiChatWebsearchService() {
        return aiChatWebsearchService;
    }

    public AiChatHistoryServiceImpl getAiChatHistoryService() {
        return aiChatHistoryService;
    }

    public AiMemoryErrorServiceImpl getAiMemoryErrorService() {
        return aiMemoryErrorService;
    }

    public AiChatAbortServiceImpl getAiChatAbortService() {
        return aiChatAbortService;
    }

    public AiMemoryMstateServiceImpl getAiMemoryMstateService() {
        return aiMemoryMstateService;
    }

    public AiVariablesService getAiVariablesService() {
        return aiVariablesService;
    }

    public AiMemoryRagServiceImpl getAiMemoryRagService() {
        return aiMemoryRagService;
    }

    public JdbcSessionMessageRepository newJdbcSessionMessageRepository(ChatQueryReq chatQueryRequest,
                                                                        MemoryIdVO memoryId, AiAccessUserVO user) {
        return new JdbcSessionMessageRepository(chatQueryRequest, memoryId, user,
                aiAssistantMstateMapper,
                aiMemoryMessageService, aiChatHistoryService,
                llmJsonSchemaApiService, aiMemoryMstateService,
                aiChatReasoningService, aiChatWebsearchService,
                aiMemoryErrorService, aiChatClassifyService, aiMemoryRagService);
    }
}
