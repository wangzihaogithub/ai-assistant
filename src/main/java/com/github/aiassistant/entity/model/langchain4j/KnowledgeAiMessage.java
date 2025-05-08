package com.github.aiassistant.entity.model.langchain4j;

import com.github.aiassistant.entity.model.chat.QaKnVO;
import dev.langchain4j.data.message.AiMessage;

import java.util.List;

public class KnowledgeAiMessage extends AiMessage {
    private final List<List<QaKnVO>> knLibList;
    private final String question;

    public KnowledgeAiMessage(String text, String question, List<List<QaKnVO>> knLibList) {
        super(text);
        this.question = question;
        this.knLibList = knLibList;
    }

    public String getQuestion() {
        return question;
    }

    public List<List<QaKnVO>> getKnLibList() {
        return knLibList;
    }
}
