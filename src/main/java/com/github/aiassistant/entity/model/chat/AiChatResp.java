package com.github.aiassistant.entity.model.chat;

import com.github.aiassistant.entity.AiChat;
import com.github.aiassistant.util.BeanUtil;

import java.util.Date;

// @Data
public class AiChatResp {
    private Integer id;
    private String name;
    private Date createTime;
    private Date updateTime;
    private Date lastChatTime;
    private Boolean lastWebsearchFlag;

    public static AiChatResp convert(AiChat aiChat) {
        return BeanUtil.toBean(aiChat, AiChatResp.class);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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
}
