package com.github.aiassistant.service.text.repository;

import com.github.aiassistant.dao.AiAssistantMstateMapper;
import com.github.aiassistant.entity.AiAssistantMstate;
import com.github.aiassistant.entity.model.chat.*;
import com.github.aiassistant.entity.model.user.AiAccessUserVO;
import com.github.aiassistant.enums.AiWebSearchSourceEnum;
import com.github.aiassistant.service.jsonschema.LlmJsonSchemaApiService;
import com.github.aiassistant.service.jsonschema.MStateKnownJsonSchema;
import com.github.aiassistant.service.jsonschema.MStateUnknownJsonSchema;
import com.github.aiassistant.service.jsonschema.ReasoningJsonSchema;
import com.github.aiassistant.service.text.acting.ActingService;
import com.github.aiassistant.service.text.chat.AiChatClassifyServiceImpl;
import com.github.aiassistant.service.text.chat.AiChatHistoryServiceImpl;
import com.github.aiassistant.service.text.chat.AiChatReasoningServiceImpl;
import com.github.aiassistant.service.text.chat.AiChatWebsearchServiceImpl;
import com.github.aiassistant.service.text.memory.AiMemoryErrorServiceImpl;
import com.github.aiassistant.service.text.memory.AiMemoryMessageServiceImpl;
import com.github.aiassistant.service.text.memory.AiMemoryMstateServiceImpl;
import com.github.aiassistant.util.AiUtil;
import com.github.aiassistant.util.FutureUtil;
import com.github.aiassistant.util.StringUtils;
import dev.langchain4j.data.message.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 数据库持久化
 */
//// @Component
//@Scope("prototype")
public class JdbcSessionMessageRepository extends AbstractSessionMessageRepository<MemoryIdVO, AiAccessUserVO> {
    private static final Logger log = LoggerFactory.getLogger(JdbcSessionMessageRepository.class);
    private final ChatQueryReq chatQueryRequest;
    private final CompletableFuture<AiChatHistoryServiceImpl.AiChatRequest> userChat = new CompletableFuture<>();
    // @Resource
    private final AiAssistantMstateMapper aiAssistantMstateMapper;
    //    // @Resource
    private final AiMemoryMessageServiceImpl aiMemoryMessageService;
    //    // @Resource
    private final AiChatHistoryServiceImpl aiChatHistoryService;
    //    // @Resource
    private final LlmJsonSchemaApiService llmJsonSchemaApiService;
    //    // @Autowired
    private final AiMemoryMstateServiceImpl aiMemoryMstateService;
    // @Autowired
    private final AiChatReasoningServiceImpl aiChatReasoningService;
    // @Autowired
    private final AiChatWebsearchServiceImpl aiChatWebsearchService;
    // @Resource
    private final AiMemoryErrorServiceImpl aiMemoryErrorService;
    // @Resource
    private final AiChatClassifyServiceImpl aiChatClassifyService;
    private MStateAiParseVO mStateAiParseVO;
    private boolean mock;
    private CompletableFuture<AiMemoryMessageServiceImpl.AiMemoryVO> userMemory;

    // @Autowired(required = false)
    public JdbcSessionMessageRepository(ChatQueryReq chatQueryRequest, MemoryIdVO memoryId, AiAccessUserVO user,
                                        AiAssistantMstateMapper aiAssistantMstateMapper,
                                        AiMemoryMessageServiceImpl aiMemoryMessageService,
                                        AiChatHistoryServiceImpl aiChatHistoryService,
                                        LlmJsonSchemaApiService llmJsonSchemaApiService,
                                        AiMemoryMstateServiceImpl aiMemoryMstateService,
                                        AiChatReasoningServiceImpl aiChatReasoningService,
                                        AiChatWebsearchServiceImpl aiChatWebsearchService,
                                        AiMemoryErrorServiceImpl aiMemoryErrorService,
                                        AiChatClassifyServiceImpl aiChatClassifyService) {
        super(memoryId, user, chatQueryRequest.userQueryTraceNumber(), chatQueryRequest.timestamp());
        this.chatQueryRequest = chatQueryRequest;
        this.aiAssistantMstateMapper = aiAssistantMstateMapper;
        this.aiMemoryMessageService = aiMemoryMessageService;
        this.aiChatHistoryService = aiChatHistoryService;
        this.llmJsonSchemaApiService = llmJsonSchemaApiService;
        this.aiMemoryMstateService = aiMemoryMstateService;
        this.aiChatReasoningService = aiChatReasoningService;
        this.aiChatWebsearchService = aiChatWebsearchService;
        this.aiMemoryErrorService = aiMemoryErrorService;
        this.aiChatClassifyService = aiChatClassifyService;
    }

    public static String getMstateJsonPrompt(Collection<AiAssistantMstate> mstateList) {
        if (mstateList == null || mstateList.isEmpty()) {
            return null;
        }
        Map<String, String> map = new LinkedHashMap<>();
        for (AiAssistantMstate mstate : mstateList) {
            map.put(mstate.getStateKey(), mstate.getPromptText());
        }
        return AiUtil.toJsonString(map);
    }

    /**
     * 当jsonSchema都生成好了，会触发这个方法
     */
    @Override
    public void afterJsonSchemaBuild() {
        String query = chatQueryRequest.getQuestion();
        if (!StringUtils.hasText(query)) {
            return;
        }
        MemoryIdVO memoryId = requestTrace.getMemoryId();
        MStateKnownJsonSchema knownJsonSchema = llmJsonSchemaApiService.getMStateknownJsonSchema(memoryId);
        MStateUnknownJsonSchema unknownJsonSchema = llmJsonSchemaApiService.getMStateUnknownJsonSchema(memoryId);
        if (knownJsonSchema == null && unknownJsonSchema == null) {
            return;
        }
        this.mStateAiParseVO = parseState(memoryId, knownJsonSchema, unknownJsonSchema);
    }

    /**
     * 插入用户的提问
     */
    @Override
    public CompletableFuture<?> addUserQuestion(List<ChatMessage> messageList) {
        if (mock) {
            return CompletableFuture.completedFuture(null);
        }
        Date now = new Date();
        String againUserQueryTraceNumber = chatQueryRequest.getAgainUserQueryTraceNumber();
        Boolean websearch = chatQueryRequest.getWebsearch();

        // 插入记忆
        CompletableFuture<AiMemoryMessageServiceImpl.AiMemoryVO> memory = userMemory = aiMemoryMessageService.insert(now, requestTrace, againUserQueryTraceNumber, websearch);
        // 插入聊天
        CompletableFuture<AiChatHistoryServiceImpl.AiChatRequest> chat = aiChatHistoryService.insert(now, requestTrace, againUserQueryTraceNumber, websearch, null);
        chat.thenAccept(userChat::complete)
                .exceptionally(throwable -> {
                    userChat.completeExceptionally(throwable);
                    return null;
                });
        // 切换至AI消息List
        requestTrace.changeToResponse();
        return FutureUtil.allOf(memory, chat);
    }

    /**
     * 查询记忆
     */
    @Override
    protected List<ChatMessage> getHistoryList(MemoryIdVO memoryId, AiAccessUserVO user) {
        return aiMemoryMessageService.selectHistoryList(memoryId.getMemoryId(), chatQueryRequest.getAgainUserQueryTraceNumber());
    }

    /**
     * 插入知识库
     */
    @Override
    public void addKnowledge(List<List<QaKnVO>> qaKnVOList) {
        requestTrace.setRequestKnowledgeList(qaKnVOList == null ? new ArrayList<>() : qaKnVOList);
    }

    /**
     * 插入问题分类
     */
    @Override
    public void addQuestionClassify(QuestionClassifyListVO questionClassify, String question) {
        if (!mock) {
            String userQueryTraceNumber = getUserQueryTraceNumber();
            Integer chatId = chatQueryRequest.getChatId();
            aiChatClassifyService.insert(questionClassify.getClassifyResultList(), chatId, question, userQueryTraceNumber);
        }
    }

    /**
     * 插入思考
     */
    @Override
    public void addReasoning(String question, ActingService.Plan plan, ReasoningJsonSchema.Result reason, boolean parallel) {
        if (!mock) {
            String userQueryTraceNumber = getUserQueryTraceNumber();
            aiChatReasoningService.insert(question, plan, reason, userQueryTraceNumber, userChat).exceptionally(throwable -> {
                log.error("addReasoning error {}, question = {}", throwable.toString(), question, throwable);
                return null;
            });
        }
    }

    /**
     * 插入联网搜索
     */
    @Override
    public void addWebSearchRead(AiWebSearchSourceEnum sourceEnum, String providerName, String question, WebSearchResultVO resultVO, long cost) {
        if (!mock) {
            String userQueryTraceNumber = getUserQueryTraceNumber();
            aiChatWebsearchService.insert(sourceEnum == null ? null : sourceEnum.getCode(), providerName, question, resultVO, cost, userQueryTraceNumber, userChat)
                    .exceptionally(throwable -> {
                        log.error("addWebSearchRead error {}, question = {}", throwable.toString(), question, throwable);
                        return null;
                    });
        }
    }

    public void setMock(boolean mock) {
        this.mock = mock;
    }

    /**
     * 提交记忆和聊天记录
     */
    @Override
    public CompletableFuture<?> commit() {
        if (mock) {
            return CompletableFuture.completedFuture(null);
        }
        Date now = new Date();
        String againUserQueryTraceNumber = chatQueryRequest.getAgainUserQueryTraceNumber();
        Boolean websearch = chatQueryRequest.getWebsearch();

        // 插入记忆
        CompletableFuture<?> memory = aiMemoryMessageService.insert(now, requestTrace, againUserQueryTraceNumber, websearch);
        // 插入聊天
        CompletableFuture<?> chat = aiChatHistoryService.insert(now, requestTrace, againUserQueryTraceNumber, websearch, userChat);
        // 如果该智能体开启了记忆状态，就插入状态
        if (mStateAiParseVO != null && userMemory != null) {
            userMemory.thenAccept(e -> aiMemoryMstateService.insert(e, mStateAiParseVO));
        }
        // 持久化完成后就可以恢复前端的提问按钮了
        return FutureUtil.allOf(memory, chat);
    }

    public MStateAiParseVO parseState(MemoryIdVO memoryId,
                                      MStateKnownJsonSchema knownJsonSchema,
                                      MStateUnknownJsonSchema unknownJsonSchema) {
        MStateVO mStateVO = null;
        Collection<AiAssistantMstate> mstateList;
        CompletableFuture<Map<String, Object>> known;
        if (knownJsonSchema == null) {
            known = CompletableFuture.completedFuture(null);
        } else if ((mstateList = aiAssistantMstateMapper.selectListByAssistantId(memoryId.getAiAssistantId())).isEmpty()) {
            known = CompletableFuture.completedFuture(null);
        } else {
            String mstateJsonPrompt = getMstateJsonPrompt(mstateList);
            mStateVO = aiMemoryMstateService.selectMstate(memoryId.getMemoryId());
            known = knownJsonSchema.future(mstateJsonPrompt, AiUtil.toJsonString(mStateVO.getKnownState()));
        }
        CompletableFuture<Map<String, Object>> unknown;
        if (unknownJsonSchema != null) {
            if (mStateVO == null) {
                mStateVO = aiMemoryMstateService.selectMstate(memoryId.getMemoryId());
            }
            if (mStateVO.isEmpty()) {
                unknown = CompletableFuture.completedFuture(null);
            } else {
                unknown = unknownJsonSchema.future(AiUtil.toJsonString(mStateVO.getUnknownState()));
            }
        } else {
            unknown = CompletableFuture.completedFuture(null);
        }
        return new MStateAiParseVO(known, unknown);
    }

    @Override
    public void addError(Throwable throwable, int baseMessageIndex, int addMessageCount, int generateCount) {
        if (mock) {
            return;
        }
        aiMemoryErrorService.insertByInner(throwable, baseMessageIndex, addMessageCount, generateCount, requestTrace);
    }

}