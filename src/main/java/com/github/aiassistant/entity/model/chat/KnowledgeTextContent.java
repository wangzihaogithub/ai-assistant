package com.github.aiassistant.entity.model.chat;

import dev.langchain4j.data.message.TextContent;

import java.util.List;

public class KnowledgeTextContent extends TextContent {
    private final List<List<QaKnVO>> knLibList;
    private final String query;
    private final String queryRewrite;

    public KnowledgeTextContent(String text, String query, String queryRewrite, List<List<QaKnVO>> knLibList) {
        super(text);
        this.knLibList = knLibList;
        this.query = query;
        this.queryRewrite = queryRewrite;
    }

    public List<List<QaKnVO>> getKnLibList() {
        return knLibList;
    }

    public String getQuery() {
        return query;
    }

    public String getQueryRewrite() {
        return queryRewrite;
    }
}