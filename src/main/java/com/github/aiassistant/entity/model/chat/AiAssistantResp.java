package com.github.aiassistant.entity.model.chat;

import com.github.aiassistant.entity.AiAssistant;
import com.github.aiassistant.util.BeanUtil;

// @Data
public class AiAssistantResp {
    private String id;
    private String name;
    // @ApiModelProperty(value = "描述", required = true)
    private String description;
    // @ApiModelProperty(value = "Logo URL", required = true)
    private String logoUrl;
    // @ApiModelProperty(value = "打招呼", required = true)
    private String helloMessage;

    public static AiAssistantResp convert(AiAssistant aiChat) {
        AiAssistantResp target = new AiAssistantResp();
        BeanUtil.copyProperties(aiChat, target);
        return target;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getHelloMessage() {
        return helloMessage;
    }

    public void setHelloMessage(String helloMessage) {
        this.helloMessage = helloMessage;
    }
//    public static List<AiAssistantResp> convertList(List<AiAssistant> aiChat) {
//        return BeanUtil.copyToList(aiChat, AiAssistantResp.class);
//    }
}
