package com.github.aiassistant.service.text.tools;


import com.github.aiassistant.entity.model.chat.WebSearchResultVO;
import com.github.aiassistant.exception.AiAssistantException;
import com.github.aiassistant.service.text.tools.functioncall.BaiduWebSearchTools;
import com.github.aiassistant.service.text.tools.functioncall.BingWebSearchTools;
import com.github.aiassistant.service.text.tools.functioncall.SogouWebSearchTools;
import com.github.aiassistant.service.text.tools.functioncall.UrlReadTools;
import com.github.aiassistant.util.FutureUtil;
import com.github.aiassistant.util.HttpClient;
import com.github.aiassistant.util.StringUtils;

import java.net.Proxy;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class WebSearchService {
    private final BaiduWebSearchTools baiduWebSearchTools = new BaiduWebSearchTools();
    private final SogouWebSearchTools sogouWebSearchTools = new SogouWebSearchTools();
    private final BingWebSearchTools bingWebSearchTools = new BingWebSearchTools();
    private final List<UrlReadTools> urlReadTools = new ArrayList<>();
    private int urlReadToolsIndex = 0;

    public WebSearchService() {
        this(Collections.emptyList());
    }

    public WebSearchService(List<Proxy> proxyList) {
        if (proxyList == null || proxyList.isEmpty()) {
            urlReadTools.add(new UrlReadTools(
                    System.getProperty("aiassistant.WebSearchService.threadNamePrefix", "link-read"),
                    Integer.getInteger("aiassistant.WebSearchService.clients", 6),
                    Integer.getInteger("aiassistant.WebSearchService.connectTimeout", 200),
                    Integer.getInteger("aiassistant.WebSearchService.readTimeout", 500),
                    Integer.getInteger("aiassistant.WebSearchService.max302", 1),
                    null
            ));
        } else {
            for (Proxy proxy : proxyList) {
                String name = Optional.ofNullable(proxy)
                        .map(HttpClient::parseAddress)
                        .map(e -> e.getHostString() + "_" + e.getPort())
                        .map(e -> "link-read-" + e)
                        .orElse("link-read");
                urlReadTools.add(new UrlReadTools(
                        System.getProperty("aiassistant.WebSearchService.threadNamePrefix", name),
                        Integer.getInteger("aiassistant.WebSearchService.clients", 6),
                        Integer.getInteger("aiassistant.WebSearchService.connectTimeout", 200),
                        Integer.getInteger("aiassistant.WebSearchService.readTimeout", 500),
                        Integer.getInteger("aiassistant.WebSearchService.max302", 1),
                        proxy));
            }
        }
    }

    private static String mergeContent(String before, String after) {
        if (after == null || after.isEmpty()) {
            return before;
        }
        if (before == null || before.isEmpty()) {
            return after;
        }
        if (isErrorPage(after)) {
            return before;
        }
        if (before.length() > after.length()) {
            return before;
        } else {
            return after;
        }
    }

    private static boolean isErrorPage(String after) {
        if (after.startsWith("搜狗搜索") && after.contains("此验证码用于确认这些请求是您的正常行为而不是自动程序发出的")) {
            return true;
        }
        return false;
    }

    public CompletableFuture<WebSearchResultVO> webSearchRead(List<String> q,
                                                              int topN, int maxCharLength,
                                                              boolean readContent,
                                                              EventListener eventListener) {
        if (q == null || q.isEmpty()) {
            return CompletableFuture.completedFuture(WebSearchResultVO.empty());
        }
        List<CompletableFuture<WebSearchResultVO>> list = new ArrayList<>();
        int rowTopN = (int) Math.ceil((double) topN / (double) q.size());
        int rowMaxCharLength = (int) Math.ceil((double) maxCharLength / (double) q.size());
        for (String s : q) {
            list.add(webSearchRead(s, rowTopN, rowMaxCharLength, readContent, eventListener));
        }
        return FutureUtil.allOf(list)
                .thenApply(WebSearchResultVO::merge);
    }

    public CompletableFuture<WebSearchResultVO> webSearchRead(String q,
                                                              int topN, int maxCharLength,
                                                              boolean readContent,
                                                              EventListener eventListener) {
        int itemLimit = (int) Math.ceil((double) topN / 3D);

        CompletableFuture<WebSearchResultVO> sougou = readWebSearch(sogouWebSearchTools, q, itemLimit, maxCharLength, readContent, eventListener);
        CompletableFuture<WebSearchResultVO> baidu = readWebSearch(baiduWebSearchTools, q, itemLimit, maxCharLength, readContent, eventListener);
        CompletableFuture<WebSearchResultVO> bing = readWebSearch(bingWebSearchTools, q, itemLimit, maxCharLength, readContent, eventListener);
        return FutureUtil.allOf(Arrays.asList(sougou, baidu, bing))
                .thenApply(WebSearchResultVO::merge)
                .exceptionally(WebSearchResultVO::error);
    }

    private CompletableFuture<WebSearchResultVO> readWebSearch(WebSearch webSearch,
                                                               String q, int topN, int maxCharLength,
                                                               boolean readContent,
                                                               EventListener eventListener) {
        if (maxCharLength <= 0) {
            return CompletableFuture.completedFuture(WebSearchResultVO.empty());
        }
        List<CompletableFuture<String>> futures = new ArrayList<>();
        String providerName = webSearch.getProviderName();
        long start = System.currentTimeMillis();
        try {
            eventListener.beforeWebSearch(providerName, q);
        } catch (AiAssistantException e) {
            return FutureUtil.completeExceptionally(e);
        }
        CompletableFuture<CompletableFuture<WebSearchResultVO>> f = webSearch.webSearch(q, topN)
                .thenApply(resultVO -> {
                    try {
                        eventListener.afterWebSearch(providerName, q, resultVO, System.currentTimeMillis() - start);
                        if (readContent) {
                            int itemLimit = Math.max(maxCharLength / topN, 100);
                            for (WebSearchResultVO.Row row : resultVO.getList()) {
                                String url = row.getUrl();
                                if (url != null && !url.isEmpty()) {
                                    UrlReadTools u = urlReadTools.get(urlReadToolsIndex++ % urlReadTools.size());
                                    UrlReadTools.ProxyVO proxy = u.getProxyVO();
                                    long startUrl = System.currentTimeMillis();
                                    eventListener.beforeUrlRead(providerName, q, u, row);

                                    futures.add(FutureUtil.accept(u.readString(url), text -> {
                                        row.setProxy(proxy);
                                        String merge = StringUtils.left(mergeContent(row.getContent(), text), itemLimit, true);
                                        long urlReadCost = System.currentTimeMillis() - startUrl;
                                        row.setUrlReadTimeCost(urlReadCost);
                                        eventListener.afterUrlRead(providerName, q, u, row, text, merge, urlReadCost);
                                        row.setContent(merge);
                                    }));
                                }
                            }
                            return FutureUtil.allOf(futures).thenApply(unused -> resultVO);
                        } else {
                            return CompletableFuture.completedFuture(resultVO);
                        }
                    } catch (AiAssistantException e) {
                        return FutureUtil.completeExceptionally(e);
                    }
                });
        return FutureUtil.allOf(f);
    }

    public static interface EventListener {
        EventListener EMPTY = new EventListener() {
        };

        default void beforeWebSearch(String providerName, String question) throws AiAssistantException {

        }

        default void afterWebSearch(String providerName, String question, WebSearchResultVO resultVO, long cost) throws AiAssistantException {

        }

        default void beforeUrlRead(String providerName, String question, UrlReadTools urlReadTools, WebSearchResultVO.Row row) throws AiAssistantException {

        }

        default void afterUrlRead(String providerName, String question, UrlReadTools urlReadTools, WebSearchResultVO.Row row, String content, String merge, long cost) throws AiAssistantException {

        }
    }
}
