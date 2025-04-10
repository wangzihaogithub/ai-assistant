package com.github.aiassistant.util;

import com.github.aiassistant.entity.model.chat.WebSearchResultVO;
import com.github.aiassistant.enums.AiWebSearchSourceEnum;
import com.github.aiassistant.service.text.ChatStreamingResponseHandler;
import com.github.aiassistant.service.text.tools.WebSearchService;
import com.github.aiassistant.service.text.tools.functioncall.UrlReadTools;

public class ResponseHandlerWebSearchEventListener implements WebSearchService.EventListener {
    private final ChatStreamingResponseHandler h;
    private final AiWebSearchSourceEnum sourceEnum;

    public ResponseHandlerWebSearchEventListener(ChatStreamingResponseHandler h, AiWebSearchSourceEnum sourceEnum) {
        this.h = h;
        this.sourceEnum = sourceEnum;
    }

    @Override
    public void beforeWebSearch(String providerName, String question) {
        h.beforeWebSearch(sourceEnum, providerName, question);
    }

    @Override
    public void afterWebSearch(String providerName, String question, WebSearchResultVO resultVO, long cost) {
        h.afterWebSearch(sourceEnum, providerName, question, resultVO, cost);
    }

    @Override
    public void beforeUrlRead(String providerName, String question, UrlReadTools urlReadTools, WebSearchResultVO.Row row) {
        h.beforeUrlRead(sourceEnum, providerName, question, urlReadTools, row);
    }

    @Override
    public void afterUrlRead(String providerName, String question, UrlReadTools urlReadTools, WebSearchResultVO.Row row, String content, String merge, long cost) {
        h.afterUrlRead(sourceEnum, providerName, question, urlReadTools, row, content, merge, cost);
    }
}
