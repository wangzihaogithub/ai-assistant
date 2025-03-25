package com.github.aiassistant.entity.model.chat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.aiassistant.entity.AiQuestionClassifyAssistant;
import com.github.aiassistant.enums.AiQuestionClassifyActionEnum;
import com.github.aiassistant.service.jsonschema.QuestionClassifySchema;

import java.util.*;
import java.util.stream.Collectors;

// @Data
public class QuestionClassifyListVO {
    private QuestionClassifySchema.Result classifyResult;
    private List<ClassifyVO> classifyList;

    public QuestionClassifySchema.Result getClassifyResult() {
        return classifyResult;
    }

    public void setClassifyResult(QuestionClassifySchema.Result classifyResult) {
        this.classifyResult = classifyResult;
    }

    public List<ClassifyVO> getClassifyList() {
        return classifyList;
    }

    public void setClassifyList(List<ClassifyVO> classifyList) {
        this.classifyList = classifyList;
    }

    /**
     * 是否需要问答库
     *
     * @return 是否需要问答库
     */
    public boolean isQa() {
        return isEnable(AiQuestionClassifyActionEnum.qa);
    }

    /**
     * 是否需要简单联网
     *
     * @return 是否需要简单联网
     */
    public boolean isJdlw() {
        return isEnable(AiQuestionClassifyActionEnum.jdlw);
    }

    /**
     * 是否需要问题拆解
     *
     * @return 是否需要问题拆解
     */
    public boolean isWtcj() {
        return isEnable(AiQuestionClassifyActionEnum.wtcj);
    }

    /**
     * 是否需要多层联网
     *
     * @return 是否需要多层联网
     */
    public boolean isDclw() {
        return isEnable(AiQuestionClassifyActionEnum.dclw);
    }

    /**
     * 是否需要联网兜底
     *
     * @return 是否需要联网兜底
     */
    public boolean isLwdd() {
        return isEnable(AiQuestionClassifyActionEnum.lwdd);
    }

    /**
     * 是否无法回答
     *
     * @return 是否无法回答
     */
    public boolean isWfhd() {
        return isEnable(AiQuestionClassifyActionEnum.wfhd);
    }

    /**
     * 请求超时毫秒数
     *
     * @return 请求超时毫秒数
     */
    public Long getReadTimeoutMs() {
        if (classifyResult == null || classifyList == null) {
            return null;
        }
        Collection<ClassifyVO> classifyResultList = getClassifyResultList();
        Integer max = null;
        for (ClassifyVO classifyVO : classifyResultList) {
            Integer readTimeoutSecond = classifyVO.getReadTimeoutSecond();
            if (readTimeoutSecond != null && readTimeoutSecond > 0) {
                if (max == null) {
                    max = readTimeoutSecond;
                } else {
                    max = Math.max(max, readTimeoutSecond);
                }
            }
        }
        return max == null ? null : max * 1000L;
    }

    public AiQuestionClassifyAssistant getClassifyAssistant() {
        if (classifyResult == null || classifyList == null) {
            return null;
        }
        Collection<ClassifyVO> classifyResultList = getClassifyResultList();
        for (ClassifyVO classifyVO : classifyResultList) {
            // 马力会从提示词保证每个问题只分至一个类型，复合问题暂不解决。
            AiQuestionClassifyAssistant assistant = classifyVO.getAssistant();
            if (assistant != null) {
                return assistant;
            }
        }
        return null;
    }

    public List<ClassifyVO> getClassifyResultList() {
        if (classifyResult == null || classifyList == null) {
            return Collections.emptyList();
        }
        Collection<String> classifyResultList = classifyResult.getClassifyList();
        if (classifyResultList == null) {
            return Collections.emptyList();
        }
        List<ClassifyVO> list = new ArrayList<>();
        Map<String, ClassifyVO> classifyByNameMap = classifyList.stream().collect(Collectors.toMap(ClassifyVO::getClassifyName, e -> e));
        for (String classifyResult : classifyResultList) {
            if (classifyResult == null) {
                continue;
            }
            String trimClassifyResult = classifyResult.trim();
            ClassifyVO classifyVO = classifyByNameMap.get(trimClassifyResult);
            if (classifyVO != null) {
                list.add(classifyVO);
            }
        }
        return list;
    }

    public boolean isEnable(AiQuestionClassifyActionEnum actionEnum) {
        Collection<ClassifyVO> classifyResultList = getClassifyResultList();
        if (classifyResultList.isEmpty()) {
            return actionEnum.isDefaultEnable();
        }
        for (ClassifyVO classifyVO : classifyResultList) {
            if (classifyVO.actionEnums != null
                    && classifyVO.actionEnums.contains(actionEnum)) {
                return true;
            }
        }
        return false;
    }

    // @Data
    public static class ClassifyVO {
        private Integer id;
        private String classifyName;
        private String groupName;
        private String groupCode;
        /**
         * 读超时秒数
         */
        private Integer readTimeoutSecond;
        private Integer aiQuestionClassifyAssistantId;
        @JsonIgnore
        private AiQuestionClassifyAssistant assistant;
        private Collection<AiQuestionClassifyActionEnum> actionEnums;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getClassifyName() {
            return classifyName;
        }

        public void setClassifyName(String classifyName) {
            this.classifyName = classifyName;
        }

        public String getGroupName() {
            return groupName;
        }

        public void setGroupName(String groupName) {
            this.groupName = groupName;
        }

        public String getGroupCode() {
            return groupCode;
        }

        public void setGroupCode(String groupCode) {
            this.groupCode = groupCode;
        }

        public Integer getReadTimeoutSecond() {
            return readTimeoutSecond;
        }

        public void setReadTimeoutSecond(Integer readTimeoutSecond) {
            this.readTimeoutSecond = readTimeoutSecond;
        }

        public Integer getAiQuestionClassifyAssistantId() {
            return aiQuestionClassifyAssistantId;
        }

        public void setAiQuestionClassifyAssistantId(Integer aiQuestionClassifyAssistantId) {
            this.aiQuestionClassifyAssistantId = aiQuestionClassifyAssistantId;
        }

        public AiQuestionClassifyAssistant getAssistant() {
            return assistant;
        }

        public void setAssistant(AiQuestionClassifyAssistant assistant) {
            this.assistant = assistant;
        }

        public Collection<AiQuestionClassifyActionEnum> getActionEnums() {
            return actionEnums;
        }

        public void setActionEnums(Collection<AiQuestionClassifyActionEnum> actionEnums) {
            this.actionEnums = actionEnums;
        }
    }
}
