package com.github.aiassistant.service.text.variables;

import com.github.aiassistant.dao.AiVariablesMapper;
import com.github.aiassistant.entity.AiAssistant;
import com.github.aiassistant.entity.model.chat.AiVariablesVO;
import com.github.aiassistant.entity.model.chat.MStateVO;
import com.github.aiassistant.entity.model.chat.MemoryIdVO;
import com.github.aiassistant.entity.model.chat.QaKnVO;
import com.github.aiassistant.entity.model.user.AiAccessUserVO;
import com.github.aiassistant.service.jsonschema.QuestionClassifySchema;
import com.github.aiassistant.service.text.AssistantConfig;
import com.github.aiassistant.service.text.ChatStreamingResponseHandler;
import com.github.aiassistant.service.text.acting.ActingService;
import com.github.aiassistant.service.text.memory.AiMemoryMstateServiceImpl;
import com.github.aiassistant.service.text.repository.JsonSchemaTokenWindowChatMemory;
import com.github.aiassistant.serviceintercept.AiVariablesServiceIntercept;
import com.github.aiassistant.util.AiUtil;
import com.github.aiassistant.util.BeanUtil;
import com.github.aiassistant.util.StringUtils;
import dev.langchain4j.data.message.ChatMessage;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * AI变量初始化
 */
// @Component
public class AiVariablesService {
    // @Autowired
    private final AiMemoryMstateServiceImpl aiMemoryMstateService;
    // @Resource
    private final AiVariablesMapper aiVariablesMapper;
    private final Supplier<Collection<AiVariablesServiceIntercept>> interceptList;

    public AiVariablesService(AiMemoryMstateServiceImpl aiMemoryMstateService,
                              AiVariablesMapper aiVariablesMapper,
                              Supplier<Collection<AiVariablesServiceIntercept>> interceptList) {
        this.aiMemoryMstateService = aiMemoryMstateService;
        this.aiVariablesMapper = aiVariablesMapper;
        this.interceptList = interceptList;
    }

    /**
     * 是否开启联网查询
     * 提示词里有用到就开启，提示词里没用到就不开启。
     *
     * @param assistantConfig  智能体配置
     * @param knPromptText     knPromptText
     * @param mstatePromptText mstatePromptText
     * @return true=需要联网查询
     */
    public static boolean isEnableWebSearch(AssistantConfig assistantConfig, String knPromptText, String mstatePromptText) {
        String[] strings = new String[]{assistantConfig.getSystemPromptText(), knPromptText, mstatePromptText};
        for (String string : strings) {
            if (AiUtil.existPromptVariable(string, AiVariablesVO.VAR_WEB_SEARCH_RESULT)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否开启思考
     * 提示词里有用到就开启，提示词里没用到就不开启。
     *
     * @param assistantConfig  智能体配置
     * @param knPromptText     knPromptText
     * @param mstatePromptText mstatePromptText
     * @return true=需要开启思考
     */
    public static boolean isEnableReasoning(AssistantConfig assistantConfig, String knPromptText, String mstatePromptText) {
        String[] strings = new String[]{assistantConfig.getSystemPromptText(), knPromptText, mstatePromptText};
        for (String string : strings) {
            if (AiUtil.existPromptVariable(string, AiVariablesVO.VAR_REASONING_RESULT)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否开启知识库查询
     * 提示词里有用到就开启，提示词里没用到就不开启。
     *
     * @param assistant    智能体配置
     * @param knPromptText knPromptText
     * @return true=需要查询知识库
     */
    public static boolean isEnableKnQuery(AssistantConfig assistant, String knPromptText) {
        // 知识库作为消息提示词
        if (StringUtils.hasText(knPromptText)) {
            return true;
        }
        // 知识库作为系统提示词
        return AiUtil.existPromptVariable(assistant.getSystemPromptText(), AiVariablesVO.VAR_KN_DOCUMENTS);
    }

    /**
     * 提示词
     *
     * @param promptMessage promptMessage
     * @param currentUser   currentUser
     * @param memoryId      memoryId
     * @return 提示词
     */
    public String prompt(String promptMessage, AiAccessUserVO currentUser, MemoryIdVO memoryId) {
        AiVariablesVO aiVariables = selectVariables(currentUser, new ArrayList<>(), null, memoryId, false, null);
        return AiUtil.toPrompt(promptMessage, aiVariables).text();
    }

    /**
     * 查询变量
     *
     * @param currentUser     currentUser
     * @param historyList     historyList
     * @param lastQuestion    lastQuestion
     * @param memoryId        memoryId
     * @param websearch       websearch
     * @param responseHandler 响应处理
     * @return 变量
     */
    public AiVariablesVO selectVariables(AiAccessUserVO currentUser, List<ChatMessage> historyList,
                                         String lastQuestion, MemoryIdVO memoryId, Boolean websearch,
                                         ChatStreamingResponseHandler responseHandler) {
        AiVariablesVO variables = new AiVariablesVO();

        // 智能体
        setterAssistant(variables.getAssistant(), memoryId.getAiAssistant());

        // 状态
        setterMstate(variables.getMstate(), memoryId);
        // 聊天
        setterChat(variables.getChat(), historyList, lastQuestion);

        // 系统变量
        setterSys(variables.getSys());
        // 用户请求
        setterRequest(variables.getRequest(), websearch);
        // 变量
        setterVar(variables.getVar());

        for (AiVariablesServiceIntercept intercept : interceptList.get()) {
            variables = intercept.afterAiVariables(variables, currentUser, historyList, lastQuestion, memoryId, websearch);
        }
        if (responseHandler != null) {
            variables = responseHandler.onAfterSelectVariables(variables, currentUser, historyList, lastQuestion, memoryId, websearch);
        }
        return variables;
    }

    /**
     * 用户请求
     *
     * @param target    target
     * @param websearch websearch
     */
    private void setterRequest(AiVariablesVO.Request target, Boolean websearch) {
        target.setWebsearch(Boolean.TRUE.equals(websearch));
    }

    /**
     * 聊天
     *
     * @param target      target
     * @param historyList historyList
     * @param query       query
     */
    private void setterChat(AiVariablesVO.Chat target, List<ChatMessage> historyList, String query) {
        target.setHistoryUserMessage(JsonSchemaTokenWindowChatMemory.getUserMessageString(historyList));
        target.setHistoryMessage(JsonSchemaTokenWindowChatMemory.getMessageString(historyList));
        target.setSystemMessage(JsonSchemaTokenWindowChatMemory.getSystemString(historyList));
        target.setQuestion(query);
    }

    /**
     * 知识库
     *
     * @param target          target
     * @param knn             knn
     * @param webSearchResult webSearchResult
     * @param reasoningResult reasoningResult
     */
    public void setterKn(AiVariablesVO.Kn target, List<List<QaKnVO>> knn, String webSearchResult, ActingService.Plan reasoningResult) {
        target.setDocuments(QaKnVO.qaToString(knn));
        if (reasoningResult != null) {
            target.setReasoningResult(reasoningResult.toActingResult());
            target.setWebSearchActingResult(reasoningResult.toWebSearchActingResult());
            target.setReasoningTitle(reasoningResult.toAiTitleString());
        }
        target.setWebSearchResult(webSearchResult);
    }

    /**
     * 问题分类结果
     *
     * @param classify classify
     * @param result   result
     */
    public void setterQuestionClassifyResult(AiVariablesVO.QuestionClassify classify, QuestionClassifySchema.Result result) {
        if (result == null) {
            return;
        }
        Collection<String> classifyList = result.getClassifyList();
        if (classifyList != null) {
            classify.setResultClassify(AiUtil.toArrayJson(classifyList, Function.identity()));
        }
    }

    /**
     * 系统
     *
     * @param sys sys
     */
    private void setterSys(AiVariablesVO.Sys sys) {
        Date now = new Date();
        sys.setDatetime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(now));
        sys.setYear(new SimpleDateFormat("yyyy").format(now));
    }

    /**
     * 变量
     *
     * @param var var
     */
    private void setterVar(Map<String, String> var) {
        List<com.github.aiassistant.entity.AiVariables> aiVariables = aiVariablesMapper.selectEnableList();
        for (com.github.aiassistant.entity.AiVariables row : aiVariables) {
            var.put(row.getVarKey(), row.getVarValue());
        }
    }

    /**
     * 状态
     *
     * @param target   target
     * @param memoryId memoryId
     */
    private void setterMstate(AiVariablesVO.Mstate target, MemoryIdVO memoryId) {
        MStateVO mStateVO = aiMemoryMstateService.selectMstate(memoryId.getMemoryId());
        if (mStateVO == null) {
            return;
        }
        target.setKnownState(AiUtil.toAiXmlString(mStateVO.getKnownState()));
        target.setUnknownState(AiUtil.toAiXmlString(mStateVO.getUnknownState()));
    }

//    /**
//     * 学生
//     */
//    private void setterStudent(AiVariables.Student target, AiAccessUserVO currentUser) {
//        Integer studentId = currentUser.getId();
//        BeanUtil.copyProperties(studentWholeProcessMapper.selectByStudentId(studentId), target);
//        target.setDelivery(StudentJobDeliveryDescVO.toAiString(studentJobDeliveryMapper.selectDescList(studentId)));
//    }

    /**
     * 智能体
     *
     * @param target target
     * @param source source
     */
    private void setterAssistant(AiVariablesVO.Assistant target, AiAssistant source) {
        BeanUtil.copyProperties(source, target);
    }


//    /**
//     * 竞争对手
//     */
//    private void setterRival(AiVariables.Rival target) {
//        List<AiRivalCompany> list = aiRivalCompanyMapper.selectListByEnable();
//        target.setCompanyNames(list.stream().map(AiRivalCompany::getCompanyName).filter(StringUtils::hasText).filter(e -> e.length() > 1).collect(Collectors.joining(",\r\n")));
//        target.setBrandNames(list.stream().map(AiRivalCompany::getBrandName).filter(StringUtils::hasText).filter(e -> e.length() > 1).collect(Collectors.joining(",\r\n")));
//    }

}
