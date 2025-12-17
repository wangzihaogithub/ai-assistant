package com.github.aiassistant.service.text.repository;

import com.github.aiassistant.entity.model.langchain4j.Feature;
import com.github.aiassistant.entity.model.langchain4j.KnowledgeAiMessage;
import com.github.aiassistant.entity.model.langchain4j.ThinkingAiMessage;
import com.github.aiassistant.util.AiUtil;
import com.github.aiassistant.util.StringUtils;
import dev.langchain4j.data.message.*;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * 转成JsonSchema类型的记忆
 */
public class JsonSchemaTokenWindowChatMemory extends ConsumerTokenWindowChatMemory {

    public JsonSchemaTokenWindowChatMemory(ConsumerTokenWindowChatMemory parent, SystemMessage systemMessage, List<ChatMessage> fewshotMessageList) {
        super(parent.id, parent.maxTokens, null, merge(parent.messages, systemMessage, fewshotMessageList), null);
    }

    private static List<ChatMessage> merge(List<ChatMessage> messages, SystemMessage systemMessage, List<ChatMessage> fewshotMessageList) {
        List<ChatMessage> list = new ArrayList<>(messages.size() + 2);
        AiUtil.addToHistoryList(list, systemMessage, fewshotMessageList);
        list.addAll(messages);
        return messages;
    }

    public static String getMessageString(List<ChatMessage> messages) {
        StringJoiner joiner = new StringJoiner("\n");
        for (ChatMessage message : messages) {
            if (isIgnore(message)) {
                continue;
            }
            if (message instanceof UserMessage) {
                String text = ((UserMessage) message).contents().stream().filter(e -> e instanceof TextContent).map(e -> ((TextContent) e)).map(e -> e.text()).collect(Collectors.joining("\n"));
                if (!StringUtils.hasText(text)) {
                    continue;
                }
                joiner.add("<用户提问>" + text + "</用户提问>");
            } else if (message instanceof AiMessage) {
                AiMessage aiMessage = (AiMessage) message;
                String text = aiMessage.text();
                if (!StringUtils.hasText(text)) {
                    continue;
                }
                joiner.add("<AI回答>" + text + "</AI回答>");
            }
        }
        return joiner.toString();
    }

    private static boolean isIgnore(ChatMessage message) {
        if (AiUtil.isNull(message)) {
            return true;
        }
        if (message instanceof Feature.Ignore && ((Feature.Ignore) message).isIgnoreAddRepository()) {
            return true;
        }
        return AiUtil.isTypeLangChain(message)
                || message instanceof KnowledgeAiMessage
                || message instanceof ThinkingAiMessage;
    }

    public static String getUserMessageString(List<ChatMessage> messages) {
        StringJoiner joiner = new StringJoiner("\n");
        for (ChatMessage message : messages) {
            if (isIgnore(message)) {
                continue;
            }
            if (message instanceof UserMessage) {
                String text = ((UserMessage) message).contents().stream().filter(e -> e instanceof TextContent).map(e -> ((TextContent) e)).map(e -> e.text()).collect(Collectors.joining("\n"));
                if (!StringUtils.hasText(text)) {
                    continue;
                }
                joiner.add("<用户提问>" + text + "</用户提问>");
            }
        }
        return joiner.toString();
    }

    public static String getSystemString(List<ChatMessage> messages) {
        for (ChatMessage message : messages) {
            if (message instanceof SystemMessage) {
                return ((SystemMessage) message).text();
            }
        }
        return "";
    }

    @Override
    public String toString() {
        return getMessageString(messages());
    }
}
