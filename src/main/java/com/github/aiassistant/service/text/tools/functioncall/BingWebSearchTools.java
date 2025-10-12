package com.github.aiassistant.service.text.tools.functioncall;

import com.github.aiassistant.entity.model.chat.WebSearchResultVO;
import com.github.aiassistant.entity.model.langchain4j.WebSearchToolExecutionResultMessage;
import com.github.aiassistant.enums.AiWebSearchSourceEnum;
import com.github.aiassistant.platform.HtmlQuery;
import com.github.aiassistant.service.text.ChatStreamingResponseHandler;
import com.github.aiassistant.service.text.tools.Tools;
import com.github.aiassistant.service.text.tools.WebSearch;
import com.github.aiassistant.util.FutureUtil;
import com.github.aiassistant.util.Name;
import com.github.aiassistant.util.StringUtils;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolMemoryId;

import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 必应联网搜索
 */
public class BingWebSearchTools extends Tools implements WebSearch {
    private static final UrlReadTools urlReadTools =
            new UrlReadTools(
                    System.getProperty("aiassistant.BingWebSearchTools.threadNamePrefix", "web-bing"),
                    Integer.getInteger("aiassistant.BingWebSearchTools.clients", 6),
                    Integer.getInteger("aiassistant.BingWebSearchTools.connectTimeout", 500),
                    Integer.getInteger("aiassistant.BingWebSearchTools.readTimeout", 1500),
                    Integer.getInteger("aiassistant.BingWebSearchTools.max302", 1),
                    UrlReadTools.PROXY1);
    private final String[] defaultHeaders = {
            "accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
            "accept-language", "zh-CN,zh;q=0.9",
            "cache-control", "no-cache",
            "sec-ch-ua", "\"Google Chrome\";v=\"131\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"",
            "sec-ch-ua-mobile", "?0",
            "sec-ch-ua-platform", "\"macOS\"",
            "sec-fetch-dest", "document",
            "sec-fetch-mode", "navigate",
            "sec-fetch-site", "none",
            "sec-fetch-user", "?1",
            "upgrade-insecure-requests", "1",
            "pragma", "no-cache",
            "user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    };

    private static String getTime(String content) {
        if (content != null) {
            for (String s : new String[]{"·", "："}) {
                String[] split = content.split(s, 2);
                String time = split[0].trim();
                if (!time.isEmpty() && time.length() <= 12) {
                    return time;
                }
            }
        }
        return null;
    }

    private static String getSourceByUrl(String url) {
        if (url != null && url.startsWith("http")) {
            try {
                URI uri = new URI(url);
                String host = uri.getHost();
                return host.startsWith("www.") ? host.substring("www.".length()) : host;
            } catch (Exception ignored) {

            }
        }
        return null;
    }

    @Override
    public CompletableFuture<WebSearchResultVO> webSearch(String q, int limit) {
        if (!StringUtils.hasText(q)) {
            return CompletableFuture.completedFuture(WebSearchResultVO.empty());
        }
        String qLimit = StringUtils.left(q, 20, true);
        String urlString = "https://cn.bing.com/search?q={q}&first={pn}&FORM=PERE1";
        int pageSize = 10;
        int count = (int) Math.ceil((double) limit / (double) pageSize);
        List<CompletableFuture<WebSearchResultVO>> futures = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Map<String, Object> query = new HashMap<>();
            query.put("q", qLimit);
            query.put("pn", i * pageSize);
            futures.add(webSearch(urlString, query));
        }
        return FutureUtil.allOf(futures)
                .thenApply(WebSearchResultVO::merge)
                .thenApply(resultVO -> {
                    if (count > 1) {
                        resultVO.setList(resultVO.getList().stream().limit(limit).collect(Collectors.toList()));
                    }
                    return resultVO;
                });
    }

    private CompletableFuture<WebSearchResultVO> webSearch(String urlString, Map<String, Object> query) {
        CompletableFuture<Object> read = urlReadTools.read(urlString, query, defaultHeaders, 1);
        UrlReadTools.ProxyVO proxy = urlReadTools.getProxyVO();
        return read.thenApply(o -> {
                    if (!(o instanceof HtmlQuery)) {
                        return WebSearchResultVO.empty();
                    }
                    HtmlQuery htmlQuery = (HtmlQuery) o;
                    List<HtmlQuery<?>> htmlList = htmlQuery.selectList("#b_results .b_algo");
                    List<HtmlQuery<?>> topList = htmlQuery.selectList("#b_results .b_ans");
                    List<WebSearchResultVO.Row> topRows = topList.stream().map(e -> {
                        WebSearchResultVO.Row vo = new WebSearchResultVO.Row();
                        HtmlQuery<?> a = e.selectElement("h2 a", 0);
                        vo.setContent(String.join(".\n", e.selectTexts("li")));
                        vo.setTitle(e.selectText(".b_algo", 0));
                        vo.setUrl(a.attr("href"));
                        vo.setSource(getSourceByUrl(vo.getUrl()));
                        return vo;
                    }).collect(Collectors.toList());
                    List<WebSearchResultVO.Row> rows = htmlList.stream()
                            .map(e -> {
                                WebSearchResultVO.Row vo = new WebSearchResultVO.Row();
                                HtmlQuery<?> a = e.selectElement("h2 a", 0);
                                if (a.isEmpty()) {
                                    a = e.selectElement("a", 0);
                                }
                                vo.setContent(String.join(".\n", e.selectTexts(".b_caption")));
                                String title = String.join(".\n", e.selectTexts(".b_algoheader"));
                                if (title.isEmpty()) {
                                    title = a.text();
                                }
                                vo.setTitle(title);
                                vo.setUrl(a.attr("href"));
                                vo.setTime(getTime(vo.getContent()));
                                String source = e.selectText(".b_tpcn .tptxt .tptt", 0);
                                if (source == null || source.isEmpty()) {
                                    source = getSourceByUrl(vo.getUrl());
                                }
                                vo.setSource(source);
                                return vo;
                            })
                            .filter(e -> StringUtils.hasText(e.getContent()))
                            .collect(Collectors.toList());
                    WebSearchResultVO resultVO = new WebSearchResultVO();
                    topRows.addAll(rows);
                    resultVO.setList(topRows);
                    resultVO.setProxyList(Collections.singletonList(proxy));
                    resultVO.setPageNum(htmlQuery.selectHtml(".sb_pagF li a", 0));
                    return resultVO;
                })
                .exceptionally(WebSearchResultVO::error);

    }

    @Override
    public String getProviderName() {
        return "必应";
    }

    @Override
    public void setBeanName(String beanName) {
        super.setBeanName(beanName);
        AiWebSearchSourceEnum.create(beanName);
    }

    @Tool(name = "必应搜索", value = {"# 插件功能\n" +
            "此工具可用于使用谷歌等搜索引擎进行全网搜索。特别是新闻相关\n" +
            "# 返回字段名单\n" +
            "URL\n" +
            "标题\n" +
            "摘要\n" +
            "来源"})
    public Object search(
            @P(value = "搜索内容", required = true) @Name("q") List<String> q,
            @ToolMemoryId ToolExecutionRequest request) {
        List<CompletableFuture<WebSearchResultVO>> voList = new ArrayList<>();
        ChatStreamingResponseHandler handler = getStreamingResponseHandler();
        String beanName = getBeanName();
        String providerName = getProviderName();
        AiWebSearchSourceEnum sourceEnum = AiWebSearchSourceEnum.valueOf(beanName);
        for (String s : q) {
            handler.beforeWebSearch(sourceEnum, providerName, s);
            long start = System.currentTimeMillis();
            CompletableFuture<WebSearchResultVO> vo = webSearch(s, 10);
            vo.thenAccept(resultVO -> handler.afterWebSearch(sourceEnum, providerName, s, resultVO, System.currentTimeMillis() - start));
            voList.add(vo);
        }
        return FutureUtil.allOf(voList).thenApply(list -> {
            String text = WebSearchResultVO.toAiString(list.toArray(new WebSearchResultVO[0]));
            return new WebSearchToolExecutionResultMessage(request, text, list);
        });
    }

}
