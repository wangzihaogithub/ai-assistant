package com.github.aiassistant.service.text.tools.functioncall;

import com.github.aiassistant.entity.model.chat.MemoryIdVO;
import com.github.aiassistant.entity.model.chat.WebSearchResultVO;
import com.github.aiassistant.entity.model.chat.WebSearchToolExecutionResultMessage;
import com.github.aiassistant.enums.AiAssistantKnTypeEnum;
import com.github.aiassistant.enums.AiWebSearchSourceEnum;
import com.github.aiassistant.service.text.ChatStreamingResponseHandler;
import com.github.aiassistant.service.text.embedding.KnnApiService;
import com.github.aiassistant.service.text.embedding.ReRankModelClient;
import com.github.aiassistant.service.text.tools.Tools;
import com.github.aiassistant.service.text.tools.WebSearchService;
import com.github.aiassistant.util.FutureUtil;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolMemoryId;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

public class WebSearchTools extends Tools {
    private static final WebSearchService webSearchService =
            new WebSearchService(Arrays.asList(UrlReadTools.PROXY1, UrlReadTools.PROXY2));
    // @Autowired
    private final Function<MemoryIdVO, ReRankModelClient> reRankModelGetter;

    public WebSearchTools() {
        this.reRankModelGetter = null;
    }

    public WebSearchTools(Supplier<KnnApiService> knnApiServiceSupplier) {
        if (knnApiServiceSupplier == null) {
            this.reRankModelGetter = null;
        } else {
            this.reRankModelGetter = memoryIdVO -> Optional.of(memoryIdVO)
                    .map(e -> e.getAssistantKn(AiAssistantKnTypeEnum.rerank))
                    .map(e -> {
                        KnnApiService knnApiService = knnApiServiceSupplier.get();
                        return knnApiService == null ? null : knnApiService.getModel(e);
                    })
                    .map(ReRankModelClient::new)
                    .orElse(null);
        }
    }

    public WebSearchTools(Function<MemoryIdVO, ReRankModelClient> reRankModelGetter) {
        this.reRankModelGetter = reRankModelGetter;
    }

    @Override
    public void setBeanName(String beanName) {
        super.setBeanName(beanName);
        AiWebSearchSourceEnum.create(beanName);
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
        AiWebSearchSourceEnum sourceEnum = AiWebSearchSourceEnum.valueOf(getBeanName());

        ChatStreamingResponseHandler handler = getStreamingResponseHandler();
        CompletableFuture<WebSearchResultVO> read = webSearchService.webSearchRead(q, 1, 10000, false, handler.adapterWebSearch(sourceEnum));
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
