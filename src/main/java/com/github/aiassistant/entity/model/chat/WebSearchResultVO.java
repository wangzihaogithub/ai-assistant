package com.github.aiassistant.entity.model.chat;

import com.github.aiassistant.service.text.tools.functioncall.UrlReadTools;
import com.github.aiassistant.util.AiUtil;
import com.github.aiassistant.util.StringUtils;
import com.github.aiassistant.util.ThrowableUtil;

import java.util.*;
import java.util.stream.Collectors;

// @Data
public class WebSearchResultVO {
    public static final String DESC_URL = "URL";
    public static final String DESC_TITLE = "标题";
    public static final String DESC_CONTENT = "摘要";
    public static final String DESC_SOURCE = "来源";
    public static final String DESC_TIME = "发布时间";
    public static final String DESC_RESULT_SET = "ResultSet";
    private String pageNum;
    private List<Row> list;
    private List<String> error;
    private List<UrlReadTools.ProxyVO> proxyList;

    public static WebSearchResultVO empty() {
        WebSearchResultVO vo = new WebSearchResultVO();
        vo.setPageNum("");
        vo.setList(new ArrayList<>());
        return vo;
    }

    public static WebSearchResultVO error(Throwable throwable) {
        WebSearchResultVO vo = new WebSearchResultVO();
        vo.error = Collections.singletonList(ThrowableUtil.stackTraceToString(throwable));
        vo.setList(new ArrayList<>());
        return vo;
    }

    public static WebSearchResultVO mergeRow(List<List<Row>> voList) {
        List<WebSearchResultVO> collect = voList.stream()
                .map(e -> {
                    WebSearchResultVO vo = new WebSearchResultVO();
                    vo.setList(e);
                    return vo;
                })
                .collect(Collectors.toList());
        return merge(collect);
    }

    public static WebSearchResultVO merge(List<WebSearchResultVO> voList) {
        Set<String> urls = new HashSet<>();
        ArrayList<Row> list = new ArrayList<>();
        WebSearchResultVO vo = new WebSearchResultVO();
        vo.setList(list);
        Set<UrlReadTools.ProxyVO> proxyList = new LinkedHashSet<>();
        List<String> error = new ArrayList<>();
        for (WebSearchResultVO resultVO : voList) {
            if (resultVO.getList() != null) {
                for (Row row : resultVO.getList()) {
                    String url = row.getUrl();
                    if (url == null || url.isEmpty() || urls.add(url)) {
                        list.add(row);
                    }
                }
            }
            List<String> e = resultVO.getError();
            if (e != null) {
                error.addAll(e);
            }
            List<UrlReadTools.ProxyVO> p = resultVO.getProxyList();
            if (p != null) {
                proxyList.addAll(p);
            }
        }
        if (!proxyList.isEmpty()) {
            vo.setProxyList(new ArrayList<>(proxyList));
        }
        if (!error.isEmpty()) {
            vo.setError(error);
        }
        return vo;
    }

    public static String toSimpleAiString(WebSearchResultVO... voList) {
        return toAiString(true, voList);
    }

    public static String toAiString(WebSearchResultVO... voList) {
        return toAiString(false, voList);
    }

    private static String toAiString(boolean simple, WebSearchResultVO... voList) {
        if (voList.length == 0) {
            return "";
        }
        StringJoiner result = new StringJoiner("\n\n");
        StringJoiner rows = new StringJoiner("\n\n");
        for (WebSearchResultVO vo : voList) {
            addRowString(vo.list, rows, simple);
        }
        if (rows.length() == 0) {
            rows.add("无结果");
        }
        result.add(AiUtil.toAiXmlString(DESC_RESULT_SET, rows.toString()));
        if (voList.length == 1) {
            String pageNum = voList[0].getPageNum();
            if (StringUtils.hasText(pageNum)) {
                result.add(AiUtil.toAiXmlString("CurrentPageNumber", pageNum));
            }
        }
        return result.toString();
    }

    private static void addRowString(List<Row> list, StringJoiner rows, boolean simple) {
        for (Row rowVo : list) {
            StringJoiner row = new StringJoiner("");
            String title = rowVo.getTitle();
            String content = rowVo.getContent();
            String source = rowVo.getSource();
            String time = rowVo.getTime();
            if (!simple) {
                String url = rowVo.getUrl();
                if (StringUtils.hasText(url)) {
                    row.add(AiUtil.toAiXmlString(DESC_URL, url));
                }
            }
            if (StringUtils.hasText(title)) {
                row.add(AiUtil.toAiXmlString(DESC_TITLE, title));
            }
            if (StringUtils.hasText(content)) {
                row.add(AiUtil.toAiXmlString(DESC_CONTENT, content));
            }
            if (StringUtils.hasText(source)) {
                row.add(AiUtil.toAiXmlString(DESC_SOURCE, source));
            }
            if (StringUtils.hasText(time)) {
                row.add(AiUtil.toAiXmlString(DESC_TIME, time));
            }
            rows.add(AiUtil.toAiXmlString("row", row.toString()));
        }
    }

    @Override
    public String toString() {
        return "WebSearchResultVO{" +
                list +
                '}';
    }

    public String getPageNum() {
        return pageNum;
    }

    public void setPageNum(String pageNum) {
        this.pageNum = pageNum;
    }

    public List<Row> getList() {
        return list;
    }

    public void setList(List<Row> list) {
        this.list = list;
    }

    public List<String> getError() {
        return error;
    }

    public void setError(List<String> error) {
        this.error = error;
    }

    public List<UrlReadTools.ProxyVO> getProxyList() {
        return proxyList;
    }

    public void setProxyList(List<UrlReadTools.ProxyVO> proxyList) {
        this.proxyList = proxyList;
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    // @Data
    public static class Row {
        private String url;
        private String title;
        private String time;
        private String source;
        private String content;
        private Long urlReadTimeCost;
        private UrlReadTools.ProxyVO proxy;

        @Override
        public String toString() {
            String key;
            if (title != null && !title.isEmpty()) {
                key = title;
            } else {
                key = content;
            }
            return key;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getTime() {
            return time;
        }

        public void setTime(String time) {
            this.time = time;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public Long getUrlReadTimeCost() {
            return urlReadTimeCost;
        }

        public void setUrlReadTimeCost(Long urlReadTimeCost) {
            this.urlReadTimeCost = urlReadTimeCost;
        }

        public UrlReadTools.ProxyVO getProxy() {
            return proxy;
        }

        public void setProxy(UrlReadTools.ProxyVO proxy) {
            this.proxy = proxy;
        }

        public String reRankKey() {
            String key;
            if (title != null && !title.isEmpty()) {
                key = title;
            } else {
                key = content;
            }
            return StringUtils.left(key, 500, true);
        }
    }
}