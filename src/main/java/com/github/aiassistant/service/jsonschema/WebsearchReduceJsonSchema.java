package com.github.aiassistant.service.jsonschema;

import com.github.aiassistant.entity.model.chat.WebSearchResultVO;
import com.github.aiassistant.util.AiUtil;
import com.github.aiassistant.util.Lists;
import com.github.aiassistant.util.StringUtils;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.openai.JsonSchemasString;
import dev.langchain4j.model.output.structured.Description;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.output.JsonSchemas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 联网搜索结果合并
 */
@FunctionalInterface
public interface WebsearchReduceJsonSchema extends JsonSchemaApi {
    /**
     * 因为有些供应商并没有实现完全最新的OpenAi接口，所以还是要靠提示词方式保持兼容性。例如（阿里千问）
     */
    JsonSchema jsonSchema = JsonSchemas.jsonSchemaFrom(Result.class).orElse(null);
    String jsonSchemaString = JsonSchemasString.toJsonString(jsonSchema);

    Logger log = LoggerFactory.getLogger(WebsearchReduceJsonSchema.class);

    @Override
    default JsonSchema getJsonSchema() {
        return jsonSchema;
    }

    @SystemMessage("# Role: 网络内容结果筛选专家\n" +
            "\n" +
            "## Profile\n" +
            "围绕聊天记录，和用户的提问筛选出符合最符合用户要求的前10条网络结果。\n" +
            "\n" +
            "## 聊天记录\n" +
            " <聊天记录>{{chat.historyMessage}}</聊天记录>\n" +
            "\n" +
            "## OutputFormat:\n" +
            "- 严格按照json格式返回，无需解释，字段要求如下:{{jsonSchema}}")
    @UserMessage("<提问>{{question}}</提问><网络结果>{{websearchResult}}</网络结果>")
    TokenStream parse(@V("websearchResult") String websearchResult,
                      @V("n") String n,
                      @V("question") String question,
                      @V("jsonSchema") String jsonSchema);

    default CompletableFuture<Result> future(String websearchResult, String question) {
        TokenStream stream = parse(websearchResult, "10", question, jsonSchemaString);
        return AiUtil.toFutureJson(stream, Result.class, getClass());
    }

    default CompletableFuture<WebSearchResultVO> reduce(List<WebSearchResultVO> flattedWebSearchResult, String question) {
        if (flattedWebSearchResult == null || flattedWebSearchResult.isEmpty()) {
            return CompletableFuture.completedFuture(WebSearchResultVO.empty());
        }
        Map<String, WebSearchResultVO.Row> rowMap = flattedWebSearchResult.stream().filter(Objects::nonNull).map(WebSearchResultVO::getList).flatMap(Collection::stream).filter(e -> StringUtils.hasText(e.getUrl())).collect(Collectors.toMap(WebSearchResultVO.Row::getUrl, e -> e, (o1, o2) -> o1));
        if (rowMap.size() <= 15) {
            return CompletableFuture.completedFuture(WebSearchResultVO.merge(flattedWebSearchResult));
        }
        int chunk = 10;
        int topN = 10;
        List<List<WebSearchResultVO.Row>> partition = Lists.partition(new ArrayList<>(rowMap.values()), chunk);
        int rowTopN = (int) Math.ceil((double) topN / (double) partition.size());
        CompletableFuture<WebSearchResultVO>[] futures = new CompletableFuture[partition.size()];
        for (int i = 0; i < partition.size(); i++) {
            List<WebSearchResultVO.Row> prows = partition.get(i);
            WebSearchResultVO req = new WebSearchResultVO();
            req.setList(prows);
            String websearchString = WebSearchResultVO.toAiString(req);
            TokenStream stream = parse(websearchString, String.valueOf(rowTopN), question, jsonSchemaString);
            CompletableFuture<WebSearchResultVO> f = AiUtil.toFutureJson(stream, Result.class, getClass())
                    .thenApply(results -> {
                        WebSearchResultVO vo = new WebSearchResultVO();
                        List<WebSearchResultVO.Row> rows;
                        if (results != null && results.resultSet != null) {
                            rows = results.resultSet.stream().filter(Objects::nonNull)
                                    .map(e -> rowMap.get(e.url))
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.toList());
                        } else {
                            rows = new ArrayList<>();
                        }
                        vo.setList(rows);
                        vo.setError(flattedWebSearchResult.stream().map(WebSearchResultVO::getError).filter(Objects::nonNull).flatMap(Collection::stream).collect(Collectors.toList()));
                        vo.setProxyList(flattedWebSearchResult.stream().map(WebSearchResultVO::getProxyList).filter(Objects::nonNull).flatMap(Collection::stream).distinct().collect(Collectors.toList()));
                        return vo;
                    });
            futures[i] = f;
        }
        CompletableFuture<WebSearchResultVO> future = new CompletableFuture<>();
        CompletableFuture.allOf(futures).whenComplete((unused, throwable) -> {
            List<WebSearchResultVO> voList = Stream.of(futures).map(e -> e.getNow(null)).filter(Objects::nonNull).collect(Collectors.toList());
            if (throwable != null) {
                log.warn("WebsearchReduceJsonSchema fail {} ", throwable.toString(), throwable);
                future.complete(WebSearchResultVO.empty());
            } else {
                future.complete(WebSearchResultVO.merge(voList));
            }
        });
        return future;
    }

    // @Data
    static class Result {
        @Description(value = WebSearchResultVO.DESC_RESULT_SET)
        private List<Row> resultSet;

        @Description(value = WebSearchResultVO.DESC_RESULT_SET)
        public static class Row {
            @Description(value = WebSearchResultVO.DESC_URL)
            public String url;
//            @Description(value = WebSearchResultVO.DESC_TITLE)
//            public String title;
//            @Description(value = WebSearchResultVO.DESC_TIME)
//            public String time;
//            @Description(value = WebSearchResultVO.DESC_SOURCE)
//            public String source;
//            @Description(value = WebSearchResultVO.DESC_CONTENT)
//            public String content;

            @Override
            public String toString() {
                return url;
            }
        }

    }

}
