package com.github.aiassistant.service.text;

import com.github.aiassistant.service.text.tools.Tools;
import dev.langchain4j.data.message.ChatMessage;

import java.util.List;

/**
 * 请求构建器
 * 完成后必须调用 {@link #complete()} 方法
 */
public abstract class RequestBuilder {
    /**
     * 当前消息列表(可变集合，支持修改)
     */
    private final List<ChatMessage> messageList;
    /**
     * 工具方法列表(可变集合，支持修改)
     */
    private final List<Tools.ToolMethod> toolMethodList;
    /**
     * 请求选项
     */
    private final GenerateRequest.Options options;

    public RequestBuilder(List<ChatMessage> messageList, List<Tools.ToolMethod> toolMethodList,
                          GenerateRequest.Options options) {
        this.messageList = messageList;
        this.toolMethodList = toolMethodList;
        this.options = options;
    }

    public List<ChatMessage> getMessageList() {
        return messageList;
    }

    public List<Tools.ToolMethod> getToolMethodList() {
        return toolMethodList;
    }

    public GenerateRequest.Options getOptions() {
        return options;
    }

    public abstract void complete();
}

