package com.github.aiassistant.entity.model.chat;

import com.github.aiassistant.entity.AiChatAbort;
import com.github.aiassistant.entity.AiMemoryError;
import com.github.aiassistant.enums.AiErrorTypeEnum;
import com.github.aiassistant.platform.JsonUtil;
import com.github.aiassistant.util.BeanUtil;
import com.github.aiassistant.util.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

// @Data
public class AiUserChatHistoryResp {
    // @ApiModelProperty(value = "ID", example = "1")
    private Integer id;
    // @ApiModelProperty(value = "创建时间", example = "2023-04-01T12:00:00")
    private Date createTime;
    /**
     * 开始时间
     */
    private Date startTime;
    // @ApiModelProperty(value = "消息类型枚举", example = "User", notes = "消息类型 User(\"User\"),\n" +
//            "    System(\"System\"),\n" +
//            "    ToolResult(\"ToolResult\"),\n" +
//            "    Ai(\"Ai\");")
    private String messageTypeEnum;
    // @ApiModelProperty(value = "文本消息", example = "Hello, how are you?")
    private String messageText;
    // @ApiModelProperty(value = "用户问题聊天追踪号", example = "101")
    private String userQueryTraceNumber;
    private Boolean websearchFlag;
    /**
     * 是否思考
     */
    private Boolean reasoningFlag;
    private List<AiChatHistoryResp> aiHistoryList;
    private Abort abort;
    private Error error;
    private List<AiChatWebsearchResp> websearch;

    public static List<AiUserChatHistoryResp> groupByUser(List<AiChatHistoryResp> list,
                                                          List<AiChatAbort> abortList,
                                                          List<AiMemoryError> memoryErrorList,
                                                          List<AiChatWebsearchResp> websearchList) {
        Map<String, List<AiChatAbort>> abortMap = abortList.stream()
                .collect(Collectors.groupingBy(AiChatAbort::getRootAgainUserQueryTraceNumber));
        Map<String, List<AiMemoryError>> errorMap = memoryErrorList.stream()
                .collect(Collectors.groupingBy(e -> Objects.toString(e.getRootAgainUserQueryTraceNumber(), e.getUserQueryTraceNumber())));
        Map<String, List<AiChatWebsearchResp>> websearchMap = websearchList.stream()
                .collect(Collectors.groupingBy(AiChatWebsearchResp::getUserQueryTraceNumber));
        Map<Integer, List<AiChatHistoryResp>> groupByUser = list.stream()
                .collect(Collectors.groupingBy(AiChatHistoryResp::getUserChatHistoryId,
                        LinkedHashMap::new, Collectors.toList()));
        List<AiUserChatHistoryResp> result = new ArrayList<>();
        List<AiChatHistoryResp> userList = list.stream().filter(e -> groupByUser.containsKey(e.getId())).collect(Collectors.toList());
        for (AiChatHistoryResp user : userList) {
            String userQueryTraceNumber = user.getUserQueryTraceNumber();
            List<AiChatAbort> abort = abortMap.getOrDefault(userQueryTraceNumber, Collections.emptyList());
            List<AiMemoryError> error = errorMap.getOrDefault(userQueryTraceNumber, Collections.emptyList());
            List<AiChatHistoryResp> historyList = groupByUser.getOrDefault(user.getId(), Collections.emptyList());

            Object lastEvent = getLastEvent(abort, error, historyList);
            List<AiChatHistoryResp> historyRespList = null;
            AiUserChatHistoryResp resp = BeanUtil.toBean(user, AiUserChatHistoryResp.class);
            String respQueryTraceNumber;
            if (lastEvent instanceof AiChatAbort) {
                AiChatAbort cast = (AiChatAbort) lastEvent;
                resp.setAbort(BeanUtil.toBean(lastEvent, Abort.class));
                resp.setAiHistoryList(new ArrayList<>());
                respQueryTraceNumber = cast.getUserQueryTraceNumber();
            } else if (lastEvent instanceof AiMemoryError) {
                AiMemoryError cast = (AiMemoryError) lastEvent;
                String errorType = cast.getErrorType();
                String attachmentJson = cast.getAttachmentJson();
                Object attachment = null;
                if (StringUtils.hasText(attachmentJson)) {
                    JsonUtil.ObjectReader objectReader = JsonUtil.objectReader();
                    try {
                        attachment = objectReader.readValue(attachmentJson, Map.class);
                    } catch (Exception ignored) {
                        attachment = attachmentJson;
                    }
                }
                Error errorVO = new Error();
                errorVO.setCreateTime(cast.getCreateTime());
                errorVO.setAttachmentJson(attachment);
                errorVO.setErrorType(errorType);
                errorVO.setMessageText(cast.getMessageText());
                resp.setAiHistoryList(new ArrayList<>());
                resp.setError(errorVO);
                respQueryTraceNumber = cast.getUserQueryTraceNumber();
            } else {
                historyRespList = historyList.stream()
                        .filter(e -> e != user)
                        .collect(Collectors.toList());
                resp.setAiHistoryList(historyRespList);
                respQueryTraceNumber = historyList.isEmpty() ? userQueryTraceNumber : historyList.get(historyList.size() - 1).getUserQueryTraceNumber();
            }

            resp.setWebsearch(websearchMap.getOrDefault(respQueryTraceNumber, Collections.emptyList()));
            if (historyRespList == null || !historyRespList.isEmpty()) {
                // historyRespList == null的情况是终止或出错。
                // !historyRespList.isEmpty()的情况是已回复完成
                result.add(resp);
            } else {
                // historyRespList.isEmpty()是AI正在回复中，目前策略是不给前端返回，避免多余的麻烦。
            }
        }
        return result;
    }

    private static Object getLastEvent(List<AiChatAbort> abort, List<AiMemoryError> error, List<AiChatHistoryResp> historyList) {
        Function<AiChatAbort, Date> abortTimeGetter = AiChatAbort::getCreateTime;
        Function<AiMemoryError, Date> errorTimeGetter = AiMemoryError::getSessionTime;
        Function<AiChatHistoryResp, Date> chatTimeGetter = AiChatHistoryResp::getStartTime;

        Optional<AiChatAbort> lastAbort = abort.stream().max(Comparator.comparing(abortTimeGetter));
        Optional<AiMemoryError> lastError = error.stream().max(Comparator.comparing(errorTimeGetter));
        Optional<AiChatHistoryResp> lastAgain = historyList.stream()
                .filter(e -> StringUtils.hasText(e.getAgainUserQueryTraceNumber())).filter(e -> chatTimeGetter.apply(e) != null)
                .collect(Collectors.groupingBy(AiChatHistoryResp::getAgainUserQueryTraceNumber))
                .values().stream()
                .map(e -> e.stream().min(Comparator.comparing(chatTimeGetter)).orElse(null))
                .filter(Objects::nonNull)
                .max(Comparator.comparing(chatTimeGetter));
        Optional<AiChatHistoryResp> user = historyList.stream()
                .filter(e -> Boolean.TRUE.equals(e.getUserQueryFlag()))
                .filter(e -> chatTimeGetter.apply(e) != null)
                .max(Comparator.comparing(chatTimeGetter));

        Map<Object, Date> map = new IdentityHashMap<>();
        lastAbort.ifPresent(e -> map.put(e, abortTimeGetter.apply(e)));
        lastError.ifPresent(e -> map.put(e, errorTimeGetter.apply(e)));
        lastAgain.ifPresent(e -> map.put(e, chatTimeGetter.apply(e)));
        user.ifPresent(e -> map.put(e, chatTimeGetter.apply(e)));
        return map.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public String getMessageTypeEnum() {
        return messageTypeEnum;
    }

    public void setMessageTypeEnum(String messageTypeEnum) {
        this.messageTypeEnum = messageTypeEnum;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public String getUserQueryTraceNumber() {
        return userQueryTraceNumber;
    }

    public void setUserQueryTraceNumber(String userQueryTraceNumber) {
        this.userQueryTraceNumber = userQueryTraceNumber;
    }

    public Boolean getWebsearchFlag() {
        return websearchFlag;
    }

    public void setWebsearchFlag(Boolean websearchFlag) {
        this.websearchFlag = websearchFlag;
    }

    public Boolean getReasoningFlag() {
        return reasoningFlag;
    }

    public void setReasoningFlag(Boolean reasoningFlag) {
        this.reasoningFlag = reasoningFlag;
    }

    public List<AiChatHistoryResp> getAiHistoryList() {
        return aiHistoryList;
    }

    public void setAiHistoryList(List<AiChatHistoryResp> aiHistoryList) {
        this.aiHistoryList = aiHistoryList;
    }

    public Abort getAbort() {
        return abort;
    }

    public void setAbort(Abort abort) {
        this.abort = abort;
    }

    public Error getError() {
        return error;
    }

    public void setError(Error error) {
        this.error = error;
    }

    public List<AiChatWebsearchResp> getWebsearch() {
        return websearch;
    }

    public void setWebsearch(List<AiChatWebsearchResp> websearch) {
        this.websearch = websearch;
    }

    public Integer getWebsearchResultCount() {
        return websearch == null ? 0 : websearch.stream()
                .map(AiChatWebsearchResp::getResultCount)
                .filter(Objects::nonNull)
                .mapToInt(e -> e)
                .sum();
    }

    // @Data
    public static class Abort {

        // @ApiModelProperty(value = "创建时间", example = "2023-01-01T00:00:00")
        private Date createTime;

        // @ApiModelProperty(value = "终止前文本", example = "之前的文本内容")
        private String beforeText;

        public Date getCreateTime() {
            return createTime;
        }

        public void setCreateTime(Date createTime) {
            this.createTime = createTime;
        }

        public String getBeforeText() {
            return beforeText;
        }

        public void setBeforeText(String beforeText) {
            this.beforeText = beforeText;
        }
    }

    // @Data
    public static class Error {
        // @ApiModelProperty(value = "创建时间", example = "2023-01-01T00:00:00")
        private Date createTime;
        // @ApiModelProperty(value = "错误类型", example = "错误类型")
        private String errorType;
        // @ApiModelProperty(value = "错误文案", example = "错误文案")
        private String messageText;
        // @ApiModelProperty(value = "附加信息", example = "附加信息")
        private Object attachmentJson;

        public Date getCreateTime() {
            return createTime;
        }

        public void setCreateTime(Date createTime) {
            this.createTime = createTime;
        }

        public String getErrorType() {
            return errorType;
        }

        public void setErrorType(String errorType) {
            this.errorType = errorType;
        }

        public String getMessageText() {
            return messageText;
        }

        public void setMessageText(String messageText) {
            this.messageText = messageText;
        }

        public Object getAttachmentJson() {
            return attachmentJson;
        }

        public void setAttachmentJson(Object attachmentJson) {
            this.attachmentJson = attachmentJson;
        }
    }
}
