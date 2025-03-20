package com.github.aiassistant.service.text.tools;

import com.github.aiassistant.entity.model.chat.WebSearchResultVO;

import java.util.concurrent.CompletableFuture;

public interface WebSearch {
    CompletableFuture<WebSearchResultVO> webSearch(String q, int limit);

    String getProviderName();
}
