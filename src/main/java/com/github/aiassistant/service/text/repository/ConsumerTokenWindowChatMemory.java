package com.github.aiassistant.service.text.repository;

import com.github.aiassistant.entity.model.chat.MemoryIdVO;
import com.github.aiassistant.util.AiUtil;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.Tokenizer;

import java.util.*;
import java.util.function.Consumer;

/**
 * 聊天记忆：
 * 带监听功能的聊天记忆，根据Token窗口限制字数
 */
public class ConsumerTokenWindowChatMemory implements ChatMemory {
    protected final MemoryIdVO id;
    protected final Integer maxTokens;
    protected final Integer maxMemoryRounds;
    protected final Tokenizer tokenizer;
    protected final LinkedList<ChatMessage> messages = new LinkedList<>();
    protected final Consumer<ChatMessage> addMessages;

    public ConsumerTokenWindowChatMemory(MemoryIdVO id,
                                         Integer maxTokens,
                                         Integer maxMemoryRounds,
                                         Tokenizer tokenizer, List<ChatMessage> historyList,
                                         Consumer<ChatMessage> addMessages) {
        this.id = id;
        this.maxTokens = maxTokens;
        this.tokenizer = tokenizer;
        this.addMessages = addMessages;
        if (historyList != null) {
            messages.addAll(historyList);
        }
        this.maxMemoryRounds = maxMemoryRounds;
    }

    /**
     * 确保对话轮数不超限，超限就删除消息
     */
    private static void ensureCapacityRounds(LinkedList<ChatMessage> messages, Integer maxMemoryRounds) {
        if (maxMemoryRounds == null || maxMemoryRounds < 0 || messages.size() <= maxMemoryRounds) {
            return;
        }
        Map<ChatMessage, List<ChatMessage>> removeMap = getRemoveMapByEnsureCapacityRounds(messages, maxMemoryRounds);
        if (removeMap.isEmpty()) {
            return;
        }
        Set<ChatMessage> needRemoveSet = Collections.newSetFromMap(new IdentityHashMap<>());
        removeMap.forEach((k, v) -> {
            needRemoveSet.add(k);
            needRemoveSet.addAll(v);
        });
        messages.removeIf(needRemoveSet::contains);
    }

    private static Map<ChatMessage, List<ChatMessage>> getRemoveMapByEnsureCapacityRounds(LinkedList<ChatMessage> messages, Integer maxMemoryRounds) {
        Iterator<ChatMessage> iterator = messages.descendingIterator();
        int rounds = -1;
        Map<ChatMessage, List<ChatMessage>> removeMap = new LinkedHashMap<>();
        LinkedList<ChatMessage> roundList = new LinkedList<>();
        while (iterator.hasNext()) {
            ChatMessage next = iterator.next();
            if (AiUtil.isFewshot(next)) {
                continue;
            }
            if (AiUtil.isUser(next)) {
                rounds++;
                if (rounds > maxMemoryRounds) {
                    removeMap.put(next, new ArrayList<>(roundList));
                }
                roundList.clear();
            } else {
                roundList.add(next);
            }
        }
        return removeMap;
    }

    /**
     * 确保容量不超限，超限就删除消息
     */
    private static void ensureCapacity(List<ChatMessage> messages, int maxTokens, Tokenizer tokenizer) {
        int currentTokenCount = tokenizer.estimateTokenCountInMessages(messages);
        while (currentTokenCount > maxTokens) {
            int messageToEvictIndex = 0;
            if (messages.get(0) instanceof SystemMessage) {
                messageToEvictIndex = 1;
            }

            ChatMessage evictedMessage = messages.remove(messageToEvictIndex);
            int tokenCountOfEvictedMessage = tokenizer.estimateTokenCountInMessage(evictedMessage);
            currentTokenCount -= tokenCountOfEvictedMessage;

            if (evictedMessage instanceof AiMessage && ((AiMessage) evictedMessage).hasToolExecutionRequests()) {
                while (messages.size() > messageToEvictIndex
                        && messages.get(messageToEvictIndex) instanceof ToolExecutionResultMessage) {
                    // Some LLMs (e.g. OpenAI) prohibit ToolExecutionResultMessage(s) without corresponding AiMessage,
                    // so we have to automatically evict orphan ToolExecutionResultMessage(s) if AiMessage was evicted
                    ChatMessage orphanToolExecutionResultMessage = messages.remove(messageToEvictIndex);
                    currentTokenCount -= tokenizer.estimateTokenCountInMessage(orphanToolExecutionResultMessage);
                }
            }
        }
    }

    /**
     * 记忆ID
     */
    @Override
    public MemoryIdVO id() {
        return id;
    }

    /**
     * 追加记忆
     */
    @Override
    public void add(ChatMessage message) {
        if (addMessages != null) {
            addMessages.accept(message);
        }
        messages.add(message);
    }


    /**
     * 查询记忆
     */
    @Override
    public List<ChatMessage> messages() {
        LinkedList<ChatMessage> messages = new LinkedList<>(this.messages);
        ensureCapacityRounds(messages, maxMemoryRounds);
        ensureCapacity(messages, maxTokens, tokenizer);
        return messages;
    }

    /**
     * 清空记忆
     */
    @Override
    public void clear() {
        messages.clear();
    }
}
