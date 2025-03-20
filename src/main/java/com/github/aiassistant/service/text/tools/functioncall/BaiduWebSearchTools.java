package com.github.aiassistant.service.text.tools.functioncall;

import com.github.aiassistant.entity.model.chat.MemoryIdVO;
import com.github.aiassistant.entity.model.chat.WebSearchResultVO;
import com.github.aiassistant.entity.model.chat.WebSearchToolExecutionResultMessage;
import com.github.aiassistant.service.text.tools.Tools;
import com.github.aiassistant.service.text.tools.WebSearch;
import com.github.aiassistant.util.AiUtil;
import com.github.aiassistant.service.text.ChatStreamingResponseHandler;
import com.github.aiassistant.util.FutureUtil;
import com.github.aiassistant.platform.HtmlQuery;
import com.github.aiassistant.util.StringUtils;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolMemoryId;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class BaiduWebSearchTools extends Tools implements WebSearch {
    private static final UrlReadTools urlReadTools =
            new UrlReadTools("web-baidu", 500, 1500, 1, UrlReadTools.PROXY1);
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
//            "user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    };

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        BaiduWebSearchTools tools = new BaiduWebSearchTools();
        CompletableFuture<WebSearchResultVO> f = tools.webSearch("介绍一下 当前最具有发展 前景的岗位类型", 35);
        WebSearchResultVO vo = f.get();
        System.out.println("vo = " + vo);
    }

    @Override
    public CompletableFuture<WebSearchResultVO> webSearch(String q, int limit) {
        if (!StringUtils.hasText(q)) {
            return CompletableFuture.completedFuture(WebSearchResultVO.empty());
        }
        String qLimit = AiUtil.limit(q, 20, true);
        int pageSize = 10;

        int count = (int) Math.ceil((double) limit / (double) pageSize);
        String urlString = "http://www.baidu.com/s?wd={q}&pn={pn}&ie=utf-8";
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
                    // 查询class="c-gap-top-small"的元素
                    List<HtmlQuery<?>> list = htmlQuery.selectList(".result");
                    List<WebSearchResultVO.Row> rows = list.stream().map(e -> {
                                WebSearchResultVO.Row vo = new WebSearchResultVO.Row();
                                vo.setContent(e.selectElement("[class^='content-right']", 0).text());
                                vo.setSource(e.selectElement(".c-color-gray", 0).text());
                                vo.setTitle(e.selectElement(".c-title", 0).text());
                                vo.setTime(e.selectElement(".c-color-gray2", 0).text());
                                String url;

                                String mu = e.attr("mu");
                                if (StringUtils.hasText(mu)) {
                                    url = mu;
                                } else {
                                    url = e.selectAttr("a", "href", 0);
                                }
                                vo.setUrl(url);
                                return vo;
                            })
                            .filter(e -> StringUtils.hasText(e.getContent()))
                            .collect(Collectors.toList());
                    WebSearchResultVO resultVO = new WebSearchResultVO();
                    resultVO.setList(rows);
                    resultVO.setPageNum(htmlQuery.selectElement("#page [class^='page-inner'] strong", 0).text());
                    resultVO.setProxyList(Collections.singletonList(proxy));
                    return resultVO;
                })
                .exceptionally(WebSearchResultVO::error);
    }

    @Override
    public String getProviderName() {
        return "百度";
    }

    @Tool(name = "菜鸟无忧百度搜索", value = {"# 插件功能\n" +
            "此工具可用于使用谷歌等搜索引擎进行全网搜索。特别是新闻相关\n" +
            "# 返回字段名单\n" +
            "URL\n" +
            "标题\n" +
            "摘要\n" +
            "来源"})
    public Object search(
            @P(value = "搜索内容", required = true) List<String> q,
            @ToolMemoryId ToolExecutionRequest request,
            @ToolMemoryId MemoryIdVO memoryIdVO) {
        List<CompletableFuture<WebSearchResultVO>> voList = new ArrayList<>();
        ChatStreamingResponseHandler handler = getStreamingResponseHandler();
        String beanName = getBeanName();
        String providerName = getProviderName();
        for (String s : q) {
            handler.beforeWebSearch(beanName, providerName, s);
            long start = System.currentTimeMillis();
            CompletableFuture<WebSearchResultVO> vo = webSearch(s, 10);
            vo.thenAccept(resultVO -> handler.afterWebSearch(beanName, providerName, s, resultVO, System.currentTimeMillis() - start));
            voList.add(vo);
        }
        return FutureUtil.allOf(voList).thenApply(list -> {
            String text = WebSearchResultVO.toAiString(list.toArray(new WebSearchResultVO[0]));
            return new WebSearchToolExecutionResultMessage(request, text, list);
        });
    }
}
