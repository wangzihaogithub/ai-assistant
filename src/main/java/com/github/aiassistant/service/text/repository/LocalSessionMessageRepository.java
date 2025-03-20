package com.github.aiassistant.service.text.repository;


import com.github.aiassistant.entity.model.chat.MemoryIdVO;
import com.github.aiassistant.util.UniqueKeyGenerator;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 本地存储记忆
 */
public class LocalSessionMessageRepository<U> extends AbstractSessionMessageRepository<MemoryIdVO, U> {
    private final ChatMemoryStore chatMemoryStore;

    public LocalSessionMessageRepository(MemoryIdVO memoryId, U user, ChatMemoryStore chatMemoryStore) {
        super(memoryId, user, UniqueKeyGenerator.nextId(), System.currentTimeMillis());
        this.chatMemoryStore = chatMemoryStore;
    }

    @Override
    public List<ChatMessage> getHistoryList(MemoryIdVO memoryId, U user) {
        return chatMemoryStore.getMessages(memoryId);
    }

    @Override
    public CompletableFuture<Void> commit() {
        chatMemoryStore.updateMessages(requestTrace.getMemoryId(), new ArrayList<>(getMessageList()));
        return CompletableFuture.completedFuture(null);
    }

}