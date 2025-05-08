package com.github.aiassistant.entity.model.langchain4j;

/**
 * 小样本提示不参与聊天记录，记忆
 */
public interface Fewshot {

    public static class AiMessage extends dev.langchain4j.data.message.AiMessage implements Fewshot {
        public AiMessage(String text) {
            super(text);
        }
    }

    public static class UserMessage extends dev.langchain4j.data.message.UserMessage implements Fewshot {
        public UserMessage(String text) {
            super(text);
        }
    }

}
