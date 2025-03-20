package com.github.aiassistant.service.text.tools;


import com.github.aiassistant.entity.model.chat.WebSearchResultVO;
import com.github.aiassistant.service.text.tools.functioncall.BaiduWebSearchTools;
import com.github.aiassistant.service.text.tools.functioncall.BingWebSearchTools;
import com.github.aiassistant.service.text.tools.functioncall.SogouWebSearchTools;
import com.github.aiassistant.service.text.tools.functioncall.UrlReadTools;
import com.github.aiassistant.util.AiUtil;
import com.github.aiassistant.util.FutureUtil;

import java.net.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

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
            urlReadTools.add(new UrlReadTools("link-read", 200, 500, 1));
        } else {
            for (Proxy proxy : proxyList) {
                urlReadTools.add(new UrlReadTools("link-read", 200, 500, 1, proxy));
            }
        }
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        WebSearchService webSearchService = new WebSearchService();
        CompletableFuture<WebSearchResultVO> read = webSearchService.webSearchRead("菜鸟无忧", 5, 10000, false, EventListener.EMPTY);
        read.thenAccept(new Consumer<WebSearchResultVO>() {
            @Override
            public void accept(WebSearchResultVO resultVO) {
                System.out.println("resultVO = " + resultVO);
            }
        });
        WebSearchResultVO vo = read.get();
        System.out.println("vo = " + vo);
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
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        String providerName = webSearch.getProviderName();
        long start = System.currentTimeMillis();
        eventListener.beforeWebSearch(providerName, q);
        CompletableFuture<CompletableFuture<WebSearchResultVO>> f = webSearch.webSearch(q, topN)
                .thenApply(resultVO -> {
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
                                futures.add(u.readString(url).thenAccept(text -> {
                                    row.setProxy(proxy);
                                    String merge = AiUtil.limit(mergeContent(row.getContent(), text), itemLimit, true);
                                    long urlReadCost = System.currentTimeMillis() - startUrl;
                                    row.setUrlReadTimeCost(urlReadCost);
                                    eventListener.afterUrlRead(providerName, q, u, row, text, merge, urlReadCost);
                                    row.setContent(merge);
                                }).exceptionally(throwable -> null));
                            }
                        }
                        return FutureUtil.allOf(futures).thenApply(unused -> resultVO);
                    } else {
                        return CompletableFuture.completedFuture(resultVO);
                    }
                });
        return FutureUtil.allOf(f);
    }

    public static interface EventListener {
        EventListener EMPTY = new EventListener() {
        };

        default void beforeWebSearch(String providerName, String question) {

        }

        default void afterWebSearch(String providerName, String question, WebSearchResultVO resultVO, long cost) {

        }

        default void beforeUrlRead(String providerName, String question, UrlReadTools urlReadTools, WebSearchResultVO.Row row) {

        }

        default void afterUrlRead(String providerName, String question, UrlReadTools urlReadTools, WebSearchResultVO.Row row, String content, String merge, long cost) {

        }
    }
}
