package com.github.aiassistant.serviceintercept;

import com.github.aiassistant.entity.model.chat.AiVariablesVO;
import com.github.aiassistant.entity.model.chat.MemoryIdVO;
import com.github.aiassistant.entity.model.user.AiAccessUserVO;
import dev.langchain4j.data.message.ChatMessage;

import java.util.List;

public interface AiVariablesServiceIntercept extends ServiceIntercept {

    default AiVariablesVO afterAiVariables(AiVariablesVO variables,
                                           AiAccessUserVO currentUser, List<ChatMessage> historyList,
                                           String lastQuestion, MemoryIdVO memoryId, Boolean websearch) {
//        // 员工
//        setterEmployees(variables.getEmployees()); // 学生
//        setterStudent(variables.getStudent(), currentUser);      // 竞争对手
//        setterRival(variables.getRival());
//        return variables;
        return variables;
    }

}
