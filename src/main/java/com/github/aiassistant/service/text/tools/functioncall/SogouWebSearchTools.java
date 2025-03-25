package com.github.aiassistant.service.text.tools.functioncall;

import com.github.aiassistant.entity.model.chat.MemoryIdVO;
import com.github.aiassistant.entity.model.chat.WebSearchResultVO;
import com.github.aiassistant.entity.model.chat.WebSearchToolExecutionResultMessage;
import com.github.aiassistant.enums.AiWebSearchSourceEnum;
import com.github.aiassistant.platform.HtmlQuery;
import com.github.aiassistant.service.text.ChatStreamingResponseHandler;
import com.github.aiassistant.service.text.tools.Tools;
import com.github.aiassistant.service.text.tools.WebSearch;
import com.github.aiassistant.util.FutureUtil;
import com.github.aiassistant.util.StringUtils;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolMemoryId;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 搜狗联网搜索
 */
public class SogouWebSearchTools extends Tools implements WebSearch {
    private static final UrlReadTools urlReadTools =
            new UrlReadTools("web-sogou", 500, 1500, 1, UrlReadTools.PROXY1);

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
        CompletableFuture<WebSearchResultVO> vo = new SogouWebSearchTools().webSearch("菜鸟无忧", 10);
        WebSearchResultVO vo1 = vo.get();
        System.out.println("vo = " + vo1);
    }

    private static Long parseDate(String date) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < date.length(); i++) {
            char c = date.charAt(i);
            boolean parseIng = b.length() > 0;
            if (c >= '0' && c <= '9') {
                b.append(c);
            } else if (parseIng) {
                break;
            }
        }
        Long ts = null;
        if (b.length() == 10) {
            try {
                ts = Long.parseLong(b.toString()) * 1000L;
            } catch (Exception e) {
            }
        } else if (b.length() == 13) {
            try {
                ts = Long.valueOf(b.toString());
            } catch (Exception e) {
            }
        }
        if (ts != null) {
            return ts;
        }
        return null;
    }

    @Override
    public CompletableFuture<WebSearchResultVO> webSearch(String q, int limit) {
        if (!StringUtils.hasText(q)) {
            return CompletableFuture.completedFuture(WebSearchResultVO.empty());
        }
        String domain = "https://weixin.sogou.com";
        String qLimit = StringUtils.substring(q, 20, true);
        String urlString = domain + "/weixin?oq=&query={q}&_sug_type_=1&sut=0&s_from=input&ri=4&_sug_=n&type=2&page={pn}&ie=utf8";
        int pageSize = 10;

        int count = (int) Math.ceil((double) limit / (double) pageSize);
        List<CompletableFuture<WebSearchResultVO>> futures = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            Map<String, Object> query = new HashMap<>();
            query.put("q", qLimit);
            query.put("pn", i);
            futures.add(webSearch(urlString, query, domain));
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

    private CompletableFuture<WebSearchResultVO> webSearch(String urlString, Map<String, Object> query, String domain) {
        CompletableFuture<Object> read = urlReadTools.read(urlString, query, defaultHeaders, 1);
        UrlReadTools.ProxyVO proxy = urlReadTools.getProxyVO();
        return read.thenApply(o -> {
            if (!(o instanceof HtmlQuery)) {
                return WebSearchResultVO.empty();
            }
            HtmlQuery htmlQuery = (HtmlQuery) o;
            List<HtmlQuery<?>> list = htmlQuery.selectList(".news-list li");
            class Row {
                final WebSearchResultVO.Row row;
                final Long ts;

                Row(WebSearchResultVO.Row row, Long ts) {
                    this.row = row;
                    this.ts = ts;
                }
            }
            List<WebSearchResultVO.Row> rows = list.stream().map(e -> {
                        WebSearchResultVO.Row vo = new WebSearchResultVO.Row();
                        HtmlQuery<?> a = e.selectElement(".txt-box a", 0);
                        vo.setContent(String.join(".\n", e.selectTexts(".txt-info")));
                        vo.setSource(e.selectText(".s-p", 0));
                        vo.setTitle(a.text());
                        Long ts = parseDate(e.selectHtml(".s-p script", 0));
                        if (ts != null) {
                            vo.setTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Timestamp(ts)));
                        }
                        vo.setUrl(domain + a.attr("href"));
                        return new Row(vo, ts == null ? Long.MIN_VALUE : ts);
                    })
                    .sorted(Comparator.comparing((Function<Row, Long>) row -> row.ts).reversed())
                    .map(e -> e.row)
                    .collect(Collectors.toList());
            WebSearchResultVO resultVO = new WebSearchResultVO();
            resultVO.setList(rows);
            resultVO.setProxyList(Collections.singletonList(proxy));
            resultVO.setPageNum(htmlQuery.selectHtml("#pagebar_container span", 0));
            return resultVO;
        });
    }

    @Override
    public String getProviderName() {
        return "搜狗";
    }

    @Override
    public void setBeanName(String beanName) {
        super.setBeanName(beanName);
        AiWebSearchSourceEnum.create(beanName);
    }

    @Tool(name = "菜鸟无忧搜狗搜索", value = {"# 插件功能\n" +
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
