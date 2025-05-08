package com.github.aiassistant.service.text.tools.functioncall;

import com.github.aiassistant.entity.model.chat.AiVariablesVO;
import com.github.aiassistant.service.text.tools.Tools;
import com.github.aiassistant.util.BeanUtil;
import com.github.aiassistant.util.Name;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolMemoryId;
import dev.langchain4j.data.message.ToolExecutionResultMessage;

import java.util.Map;
import java.util.Objects;

public class VariablesTools extends Tools {

    @Tool(name = "获取变量", value = {"# 插件功能\n此工具可获取人类用户的聊天记录"})
    public Object getValue(
            @Name("varKey") String varKey,
            @ToolMemoryId ToolExecutionRequest request) {
        AiVariablesVO variables = getVariables();
        Map<String, Object> variablesMap = BeanUtil.toMap(variables);
        Object varValue = variablesMap.get(varKey);
        String text = Objects.toString(varValue, "");
        return new ToolExecutionResultMessage(request.id(), request.name(), text);
    }
}
