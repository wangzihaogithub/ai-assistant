package com.github.aiassistant.exception;

import com.github.aiassistant.entity.model.chat.QuestionClassifyListVO;

/**
 * 根据相关规定暂无法回答，我们可以聊聊其他话题吗？
 */
public class DataInspectionFailedException extends AiAssistantException {
    private final String question;
    private final QuestionClassifyListVO classifyListVO;

    public DataInspectionFailedException(String message, String question, QuestionClassifyListVO classifyListVO) {
        super(message);
        this.question = question;
        this.classifyListVO = classifyListVO;
    }

    public String getQuestion() {
        return question;
    }

    public QuestionClassifyListVO getClassifyListVO() {
        return classifyListVO;
    }
}
