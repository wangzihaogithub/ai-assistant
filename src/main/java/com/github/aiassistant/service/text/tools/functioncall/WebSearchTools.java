package com.github.aiassistant.service.text.tools.functioncall;

import com.github.aiassistant.entity.model.chat.MemoryIdVO;
import com.github.aiassistant.entity.model.chat.WebSearchResultVO;
import com.github.aiassistant.entity.model.chat.WebSearchToolExecutionResultMessage;
import com.github.aiassistant.service.text.ChatStreamingResponseHandler;
import com.github.aiassistant.service.text.tools.Tools;
import com.github.aiassistant.service.text.tools.WebSearchService;
import com.github.aiassistant.util.FutureUtil;
import com.github.aiassistant.service.text.embedding.ReRankModelClient;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolMemoryId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class WebSearchTools extends Tools {
    private static final WebSearchService webSearchService =
            new WebSearchService(Arrays.asList(UrlReadTools.PROXY1, UrlReadTools.PROXY2));
    // @Autowired
    private final Function<MemoryIdVO, ReRankModelClient> reRankModelGetter;

    public WebSearchTools(Function<MemoryIdVO, ReRankModelClient> reRankModelGetter) {
        this.reRankModelGetter = reRankModelGetter;
    }

    @Tool(name = "联网搜索", value = {"# 插件功能\n" +
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
        ChatStreamingResponseHandler handler = getStreamingResponseHandler();
        CompletableFuture<WebSearchResultVO> read = webSearchService.webSearchRead(q, 1, 10000, false, handler.adapterWebSearch(getBeanName()));
        CompletableFuture<CompletableFuture<WebSearchToolExecutionResultMessage>> f = read.thenApply(ws -> {
            ReRankModelClient reRankModelClient = newReRankModel(memoryIdVO);
            CompletableFuture<WebSearchResultVO> wsf;
            if (reRankModelClient != null) {
                List<CompletableFuture<List<WebSearchResultVO.Row>>> topNList = new ArrayList<>();
                for (String s : q) {
                    CompletableFuture<List<WebSearchResultVO.Row>> topN = reRankModelClient.topN(s, ws.getList(), WebSearchResultVO.Row::reRankKey, 5);
                    topNList.add(topN);
                }
                wsf = FutureUtil.allOf(topNList).thenApply(WebSearchResultVO::mergeRow);
            } else {
                wsf = CompletableFuture.completedFuture(ws);
            }
            return wsf.thenApply(resultVO -> {
                String text = WebSearchResultVO.toAiString(resultVO);
                return new WebSearchToolExecutionResultMessage(request, text, Collections.singletonList(ws));
            });
        });
        return FutureUtil.allOf(f);
    }

    private ReRankModelClient newReRankModel(MemoryIdVO memoryIdVO) {
        return reRankModelGetter != null ? reRankModelGetter.apply(memoryIdVO) : null;
    }
}
