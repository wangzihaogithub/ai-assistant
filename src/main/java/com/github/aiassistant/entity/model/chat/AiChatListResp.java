package com.github.aiassistant.entity.model.chat;

import com.github.aiassistant.entity.AiChatHistory;
import com.github.aiassistant.util.BeanUtil;
import com.github.aiassistant.util.StringUtils;

import java.util.Date;

// @Data
public class AiChatListResp {
    private Integer id;
    private String name;
    private Date createTime;
    private Date updateTime;
    private Date lastChatTime;
    private Boolean lastWebsearchFlag;
    private Integer aiChatHistoryId;
    private String assistantId;
    private String logoUrl;
    private String chatSourceEnum;//聊天来源枚举（pc=pc端创建的，wxmini=微信小程序）

    private AiChatHistoryResp history;

    public static AiChatHistoryResp convertHistory(String keyword, AiChatHistory history) {
        if (history == null) {
            return null;
        }
        AiChatHistoryResp bean = BeanUtil.toBean(history, AiChatHistoryResp.class);
        if (StringUtils.hasText(keyword)) {
            bean.setMessageText(messageText(keyword, bean.getMessageText()));
        }
        return bean;
    }

    private static int findIndex(int j, String messageText, int maxChars) {
        int charCount = 0;
        for (int i = j - 1; i > 0; i--) {
            char c = messageText.charAt(i);
            if (isSplitter(c)) {
                return i + 1;
            }
            if (charCount >= maxChars) {
                return j;
            }
            charCount++;
        }
        return j;
    }

    private static boolean isSplitter(char c) {
        return Character.isWhitespace(c)
                || c == ',' || c == '，'
                || c == '.' || c == '。'
                || c == ':' || c == '：'
                || c == '？' || c == '?'
                || c == '!' || c == '！';
    }

    private static String messageText(String keyword, String messageText) {
        int maxChars = 20;
        if (messageText.length() <= maxChars) {
            return messageText;
        }
        int i = messageText.indexOf(keyword);
        if (i != -1) {
            if (messageText.length() - i <= maxChars) {
                return messageText.substring(messageText.length() - maxChars);
            } else {
                int index = findIndex(i, messageText, maxChars);
                return messageText.substring(index);
            }
        } else {
            return messageText;
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getChatSourceEnum() {
        return chatSourceEnum;
    }

    public void setChatSourceEnum(String chatSourceEnum) {
        this.chatSourceEnum = chatSourceEnum;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public Date getLastChatTime() {
        return lastChatTime;
    }

    public void setLastChatTime(Date lastChatTime) {
        this.lastChatTime = lastChatTime;
    }

    public Boolean getLastWebsearchFlag() {
        return lastWebsearchFlag;
    }

    public void setLastWebsearchFlag(Boolean lastWebsearchFlag) {
        this.lastWebsearchFlag = lastWebsearchFlag;
    }

    public Integer getAiChatHistoryId() {
        return aiChatHistoryId;
    }

    public void setAiChatHistoryId(Integer aiChatHistoryId) {
        this.aiChatHistoryId = aiChatHistoryId;
    }

    public String getAssistantId() {
        return assistantId;
    }

    public void setAssistantId(String assistantId) {
        this.assistantId = assistantId;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public AiChatHistoryResp getHistory() {
        return history;
    }

    public void setHistory(AiChatHistoryResp history) {
        this.history = history;
    }

    // @Data
    public static class AiChatHistoryResp {
        // @ApiModelProperty(value = "创建时间", example = "2023-04-01T12:00:00")
        private Date createTime;
        // @ApiModelProperty(value = "消息类型枚举", example = "User", notes = "消息类型 User(\"User\"),\n" +
//                "    System(\"System\"),\n" +
//                "    ToolResult(\"ToolResult\"),\n" +
//                "    Ai(\"Ai\");")
        private String messageTypeEnum;
        // @ApiModelProperty(value = "文本消息", example = "Hello, how are you?")
        private String messageText;
        private Boolean userQueryFlag; // bit(1) 类型在 Java 中通常映射为 boolean

        public Date getCreateTime() {
            return createTime;
        }

        public void setCreateTime(Date createTime) {
            this.createTime = createTime;
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

        public Boolean getUserQueryFlag() {
            return userQueryFlag;
        }

        public void setUserQueryFlag(Boolean userQueryFlag) {
            this.userQueryFlag = userQueryFlag;
        }
    }

    @Override
    public String toString() {
        return id + "#" + name;
    }
}
