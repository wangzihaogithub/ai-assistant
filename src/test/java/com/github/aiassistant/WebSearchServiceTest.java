package com.github.aiassistant;

import com.github.aiassistant.entity.model.chat.WebSearchResultVO;
import com.github.aiassistant.service.text.tools.WebSearchService;
import com.github.aiassistant.service.text.tools.functioncall.UrlReadTools;

import java.util.concurrent.CompletableFuture;

public class WebSearchServiceTest {
    public static void main(String[] args) throws Throwable {
        WebSearchService webSearchService = new WebSearchService();
        CompletableFuture<WebSearchResultVO> future = webSearchService.webSearchRead("give me usa job ", 1, 1000, false, new WebSearchService.EventListener() {
            @Override
            public void beforeWebSearch(String providerName, String question) {

            }

            @Override
            public void afterWebSearch(String providerName, String question, WebSearchResultVO resultVO, long cost) {
            }

            @Override
            public void beforeUrlRead(String providerName, String question, UrlReadTools urlReadTools, WebSearchResultVO.Row row) {
            }

            @Override
            public void afterUrlRead(String providerName, String question, UrlReadTools urlReadTools, WebSearchResultVO.Row row, String content, String merge, long cost) {
            }
        });

        WebSearchResultVO webSearchResultVO = future.get();
        System.out.println("webSearchResultVO = " + webSearchResultVO);
    }
}
