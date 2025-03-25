package com.github.aiassistant.service.text;

import com.github.aiassistant.dao.AiQuestionClassifyAssistantMapper;
import com.github.aiassistant.dao.AiQuestionClassifyMapper;
import com.github.aiassistant.entity.AiQuestionClassify;
import com.github.aiassistant.entity.AiQuestionClassifyAssistant;
import com.github.aiassistant.entity.model.chat.MemoryIdVO;
import com.github.aiassistant.entity.model.chat.QuestionClassifyListVO;
import com.github.aiassistant.enums.AiQuestionClassifyActionEnum;
import com.github.aiassistant.service.jsonschema.LlmJsonSchemaApiService;
import com.github.aiassistant.service.jsonschema.QuestionClassifySchema;
import com.github.aiassistant.util.AiUtil;
import com.github.aiassistant.util.BeanUtil;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 问题分类
 */
public class AiQuestionClassifyService {
    // @Resource
    private final AiQuestionClassifyMapper aiQuestionClassifyMapper;
    // @Resource
    private final AiQuestionClassifyAssistantMapper aiQuestionClassifyAssistantMapper;
    /**
     * JsonSchema类型的模型
     */
    // @Autowired
    private final LlmJsonSchemaApiService llmJsonSchemaApiService;

    public AiQuestionClassifyService(AiQuestionClassifyMapper aiQuestionClassifyMapper, AiQuestionClassifyAssistantMapper aiQuestionClassifyAssistantMapper, LlmJsonSchemaApiService llmJsonSchemaApiService) {
        this.aiQuestionClassifyMapper = aiQuestionClassifyMapper;
        this.aiQuestionClassifyAssistantMapper = aiQuestionClassifyAssistantMapper;
        this.llmJsonSchemaApiService = llmJsonSchemaApiService;
    }

    private static QuestionClassifyListVO convert(List<AiQuestionClassify> classifyList) {
        QuestionClassifyListVO result = new QuestionClassifyListVO();
        result.setClassifyList(classifyList.stream()
                .map(e -> {
                    QuestionClassifyListVO.ClassifyVO vo = new QuestionClassifyListVO.ClassifyVO();
                    vo.setId(e.getId());
                    vo.setClassifyName(e.getClassifyName());
                    vo.setGroupCode(e.getGroupCode());
                    vo.setGroupName(e.getGroupName());
                    vo.setReadTimeoutSecond(e.getReadTimeoutSecond());
                    vo.setAiQuestionClassifyAssistantId(e.getAiQuestionClassifyAssistantId());
                    vo.setActionEnums(AiQuestionClassifyActionEnum.parse(e.getActionEnums()));
                    return vo;
                })
                .collect(Collectors.toList()));
        return result;
    }

    private static String toXml(List<AiQuestionClassify> classifyList) {
        StringJoiner joiner = new StringJoiner("\n\n");
        for (AiQuestionClassify classify : classifyList) {
            Map<String, Object> map = new LinkedHashMap<>(BeanUtil.toMap(classify));
            map.remove("id");
            map.remove("readTimeoutSecond");
            map.remove("groupCode");
            map.remove("enableFlag");
            map.remove("actionEnums");
            String xmlString = AiUtil.toAiXmlString(classify.getClassifyName(), AiUtil.toAiXmlString(map));
            joiner.add(xmlString);
        }
        return joiner.toString();
    }

    /**
     * 进行问题分类
     * @param question question
     * @param memoryIdVO memoryIdVO
     * @return 问题分类
     */
    public CompletableFuture<QuestionClassifyListVO> classify(String question, MemoryIdVO memoryIdVO) {
        // 查询定义的问题分类
        List<AiQuestionClassify> classifyList = selectListVO(memoryIdVO.getAiAssistant().getId());

        QuestionClassifyListVO result = convert(classifyList);
        QuestionClassifySchema schema = llmJsonSchemaApiService.getQuestionClassifySchema(memoryIdVO);
        if (schema == null) {
            return CompletableFuture.completedFuture(result);
        } else {
            String defineClassifyNames = AiUtil.toArrayJson(classifyList, AiQuestionClassify::getClassifyName);
            String xml = toXml(classifyList);
            return schema.future(question, defineClassifyNames, xml).thenApply(e -> {
                result.setClassifyResult(e);
                // 这个集合的长度一般为1，基本都是只归到一个类型上
                Collection<QuestionClassifyListVO.ClassifyVO> classifyResultList = result.getClassifyResultList();
                if (!classifyResultList.isEmpty()) {
                    Set<Integer> aiQuestionClassifyAssistantId = classifyResultList.stream().map(QuestionClassifyListVO.ClassifyVO::getAiQuestionClassifyAssistantId).collect(Collectors.toSet());
                    Map<Integer, AiQuestionClassifyAssistant> classifyAssistantMap = aiQuestionClassifyAssistantMapper.selectBatchIds(aiQuestionClassifyAssistantId).stream()
                            .collect(Collectors.toMap(AiQuestionClassifyAssistant::getId, o -> o));
                    for (QuestionClassifyListVO.ClassifyVO classifyVO : classifyResultList) {
                        classifyVO.setAssistant(classifyAssistantMap.get(classifyVO.getAiQuestionClassifyAssistantId()));
                    }
                }
                return result;
            });
        }
    }

    private List<AiQuestionClassify> selectListVO(String aiAssistantId) {
        return aiQuestionClassifyMapper.selectEnableList(aiAssistantId);
    }

}
