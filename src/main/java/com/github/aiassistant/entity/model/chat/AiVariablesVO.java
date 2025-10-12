package com.github.aiassistant.entity.model.chat;

import com.github.aiassistant.util.BeanMap;
import com.github.aiassistant.util.BeanUtil;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;

public class AiVariablesVO implements Serializable {
    /**
     * 知识库变量
     */
    public static final String VAR_KN_DOCUMENTS = "kn.documents";
    /**
     * 联网搜索结果变量
     */
    public static final String VAR_WEB_SEARCH_RESULT = "kn.webSearchResult";
    /**
     * 思考结果变量
     */
    public static final String VAR_REASONING_RESULT = "kn.reasoningResult";

    //    private final Student student = new Student();
    private final Assistant assistant = new Assistant();
    //    private final Job job = new Job();
//    private final Rival rival = new Rival();
    private final Chat chat = new Chat();
    private final Mstate mstate = new Mstate();
    private final Map<String, String> var = new LinkedHashMap<>();
    private final Sys sys = new Sys();
    private final Kn kn = new Kn();
    private final QuestionClassify questionClassify = new QuestionClassify();
    private final Request request = new Request();
//    private final Employees employees = new Employees();

    public <T extends AiVariablesVO> void copyTo(T target) {
        BeanMap sourceMap = new BeanMap(this);
        BeanMap targetMap = new BeanMap(target);
        for (Map.Entry<String, Object> entry : sourceMap.entrySet()) {
            String key = entry.getKey();
            Field field = targetMap.getField(key);
            if (field == null) {
                continue;
            }
            Object targetValue = targetMap.get(key);
            Object sourceValue = entry.getValue();
            if (Modifier.isFinal(field.getModifiers())) {
                if (sourceValue instanceof Map && targetValue instanceof Map) {
                    ((Map<?, ?>) targetValue).clear();
                    ((Map<?, ?>) targetValue).putAll((Map) sourceValue);
                } else {
                    BeanUtil.copyProperties(sourceValue, targetValue);
                }
            } else {
                targetMap.set(key, sourceValue);
            }
        }
    }

    public Assistant getAssistant() {
        return assistant;
    }

    public Chat getChat() {
        return chat;
    }

    public Mstate getMstate() {
        return mstate;
    }

    public Map<String, String> getVar() {
        return var;
    }

    public Sys getSys() {
        return sys;
    }

    public Kn getKn() {
        return kn;
    }

    public QuestionClassify getQuestionClassify() {
        return questionClassify;
    }

    public Request getRequest() {
        return request;
    }

    /**
     * 用户请求
     */
    public static class Request {
        /**
         * 是否联网
         */
        private Boolean websearch;

        public Boolean getWebsearch() {
            return websearch;
        }

        public void setWebsearch(Boolean websearch) {
            this.websearch = websearch;
        }
    }

    /**
     * 知识库
     */
    public static class Kn {
        private String documents;
        /**
         * 思考结果
         */
        private String reasoningResult;
        /**
         * 思考题目
         */
        private String reasoningTitle;
        /**
         * 思考联网结果
         */
        private String webSearchActingResult;
        /**
         * 联网搜索结果
         */
        private String webSearchResult;

        public String getDocuments() {
            return documents;
        }

        public void setDocuments(String documents) {
            this.documents = documents;
        }

        public String getReasoningResult() {
            return reasoningResult;
        }

        public void setReasoningResult(String reasoningResult) {
            this.reasoningResult = reasoningResult;
        }

        public String getReasoningTitle() {
            return reasoningTitle;
        }

        public void setReasoningTitle(String reasoningTitle) {
            this.reasoningTitle = reasoningTitle;
        }

        public String getWebSearchActingResult() {
            return webSearchActingResult;
        }

        public void setWebSearchActingResult(String webSearchActingResult) {
            this.webSearchActingResult = webSearchActingResult;
        }

        public String getWebSearchResult() {
            return webSearchResult;
        }

        public void setWebSearchResult(String webSearchResult) {
            this.webSearchResult = webSearchResult;
        }
    }

    /**
     * 系统时间
     */
    public static class Sys {
        private String datetime;
        private String year;

        public String getDatetime() {
            return datetime;
        }

        public void setDatetime(String datetime) {
            this.datetime = datetime;
        }

        public String getYear() {
            return year;
        }

        public void setYear(String year) {
            this.year = year;
        }
    }

    /**
     * 聊天
     */
    public static class Chat {
        private String historyUserMessage;
        private String historyMessage;
        private String systemMessage;
        private String question;

        public String getHistoryUserMessage() {
            return historyUserMessage;
        }

        public void setHistoryUserMessage(String historyUserMessage) {
            this.historyUserMessage = historyUserMessage;
        }

        public String getHistoryMessage() {
            return historyMessage;
        }

        public void setHistoryMessage(String historyMessage) {
            this.historyMessage = historyMessage;
        }

        public String getSystemMessage() {
            return systemMessage;
        }

        public void setSystemMessage(String systemMessage) {
            this.systemMessage = systemMessage;
        }

        public String getQuery() {
            return question;
        }

        public String getQuestion() {
            return question;
        }

        public void setQuestion(String question) {
            this.question = question;
        }
    }

    public static class Mstate {
        private String knownState;
        private String unknownState;

        public String getKnownState() {
            return knownState;
        }

        public void setKnownState(String knownState) {
            this.knownState = knownState;
        }

        public String getUnknownState() {
            return unknownState;
        }

        public void setUnknownState(String unknownState) {
            this.unknownState = unknownState;
        }
    }

    public static class QuestionClassify {
        //        // @ApiModelProperty(value = "定义的分类名称")
//        private String defineClassifyNames;
//        // @ApiModelProperty(value = "定义的分类代码")
//        private String defineClassifyCodes;

        /**
         * 分类结果
         */
        private String resultClassify;

        public String getResultClassify() {
            return resultClassify;
        }

        public void setResultClassify(String resultClassify) {
            this.resultClassify = resultClassify;
        }
    }


    public static class Assistant {
        private String id;

        private String name;

        private String description;

        private String helloMessage;

        public String getHelloMessage() {
            return helloMessage;
        }

        public void setHelloMessage(String helloMessage) {
            this.helloMessage = helloMessage;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }


}
