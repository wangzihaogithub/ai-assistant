package com.github.aiassistant.entity;

import java.util.*;
import java.util.stream.Collectors;

// @Data
// @ApiModel(value = "AiMemoryMessageTool", description = "通过AiMessageString放置的元数据，用于给业务做判断")
// @TableName("ai_memory_message_metadata")
public class AiMemoryMessageMetadata {

    // @TableId(value = "id", type = IdType.AUTO)
    // @ApiModelProperty(value = "ID", example = "1")
    private Integer id;

    // @ApiModelProperty(value = "记忆消息ID", example = "1")
    private Integer aiMemoryMessageId;
    // @ApiModelProperty(value = "第几个AiMessageString", example = "toolRequestId123")
    private Integer metaIndex;
    // @ApiModelProperty(value = "通过AiMessageString放置的数据key", example = "toolRequestId123")
    private String metaKey;

    // @ApiModelProperty(value = "通过AiMessageString放置的数据value", example = "搜索引擎")
    private String metaValue;

    // @ApiModelProperty(value = "用户问题聊天追踪号", example = "101")
    private String userQueryTraceNumber;
    // @ApiModelProperty(value = "重新回答用户问题聊天追踪号", example = "101")
    private String againUserQueryTraceNumber;
    // @ApiModelProperty(value = "重新回答用户问题聊天追踪号，根问题", example = "101")
    private String rootUserQueryTraceNumber;

    // @ApiModelProperty(value = "记忆ID", example = "1")
    private Integer aiMemoryId;

    public static List<Map<String, String>> convertMapList(List<AiMemoryMessageMetadata> list) {
        List<Map<String, String>> mapList = new ArrayList<>();
        if (list != null) {
            Map<Integer, List<AiMemoryMessageMetadata>> collect = list.stream()
                    .sorted(Comparator.comparingInt(AiMemoryMessageMetadata::getMetaIndex))
                    .collect(Collectors.groupingBy(AiMemoryMessageMetadata::getMetaIndex));
            for (List<AiMemoryMessageMetadata> rowList : collect.values()) {
                Map<String, String> map = new HashMap<>(rowList.size());
                for (AiMemoryMessageMetadata metadata : rowList) {
                    map.put(Objects.toString(metadata.getMetaKey(), ""), Objects.toString(metadata.getMetaValue(), ""));
                }
                mapList.add(map);
            }
        }
        return mapList;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getAiMemoryMessageId() {
        return aiMemoryMessageId;
    }

    public void setAiMemoryMessageId(Integer aiMemoryMessageId) {
        this.aiMemoryMessageId = aiMemoryMessageId;
    }

    public Integer getMetaIndex() {
        return metaIndex;
    }

    public void setMetaIndex(Integer metaIndex) {
        this.metaIndex = metaIndex;
    }

    public String getMetaKey() {
        return metaKey;
    }

    public void setMetaKey(String metaKey) {
        this.metaKey = metaKey;
    }

    public String getMetaValue() {
        return metaValue;
    }

    public void setMetaValue(String metaValue) {
        this.metaValue = metaValue;
    }

    public String getUserQueryTraceNumber() {
        return userQueryTraceNumber;
    }

    public void setUserQueryTraceNumber(String userQueryTraceNumber) {
        this.userQueryTraceNumber = userQueryTraceNumber;
    }

    public String getAgainUserQueryTraceNumber() {
        return againUserQueryTraceNumber;
    }

    public void setAgainUserQueryTraceNumber(String againUserQueryTraceNumber) {
        this.againUserQueryTraceNumber = againUserQueryTraceNumber;
    }

    public String getRootUserQueryTraceNumber() {
        return rootUserQueryTraceNumber;
    }

    public void setRootUserQueryTraceNumber(String rootUserQueryTraceNumber) {
        this.rootUserQueryTraceNumber = rootUserQueryTraceNumber;
    }

    public Integer getAiMemoryId() {
        return aiMemoryId;
    }

    public void setAiMemoryId(Integer aiMemoryId) {
        this.aiMemoryId = aiMemoryId;
    }

    @Override
    public String toString() {
        return id + "#" + metaValue;
    }
}