package com.github.aiassistant.service.text.tools;

import com.github.aiassistant.entity.model.chat.AiVariablesVO;
import com.github.aiassistant.entity.model.user.AiAccessUserVO;
import com.github.aiassistant.platform.JsonUtil;
import com.github.aiassistant.service.text.ChatStreamingResponseHandler;
import com.github.aiassistant.util.ThrowableUtil;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * 工具库接口
 */
public abstract class Tools {

    public static final String MOCK_FUNCTION_CALL_ID_PREFIX = "mock_";
    private AiAccessUserVO aiAccessUserVO;
    private AiVariablesVO variables;
    private ChatStreamingResponseHandler streamingResponseHandler;
    private String beanName;

    public static String newMockFunctionCallId() {
        return MOCK_FUNCTION_CALL_ID_PREFIX + UUID.randomUUID().toString().replaceAll("-", "");
    }

    public static List<ChatMessage> buildFunctionCallMessage(String functionName, Map<String, Object> arguments, String result) {
        return buildFunctionCallMessage(newMockFunctionCallId(), functionName, arguments, result);
    }

    public static List<ChatMessage> buildFunctionCallMessage(String id, String functionName, Map<String, Object> arguments, String result) {
        try {
            ToolExecutionRequest toolExecutionRequest = ToolExecutionRequest.builder()
                    .id(id)
                    .name(functionName)
                    .arguments(JsonUtil.objectWriter().writeValueAsString(arguments))
                    .build();
            AiMessage aiMessage = new AiMessage(Collections.singletonList(toolExecutionRequest));
            ToolExecutionResultMessage toolExecutionResultMessage = ToolExecutionResultMessage.from(toolExecutionRequest, result);
            return Arrays.asList(aiMessage, toolExecutionResultMessage);
        } catch (IOException e) {
            ThrowableUtil.sneakyThrows(e);
            return Collections.emptyList();
        }
    }

    @Override
    public String toString() {
        if (beanName != null) {
            return beanName;
        }
        return super.toString();
    }

    public AiAccessUserVO getAiAccessUserVO() {
        return aiAccessUserVO;
    }

    public void setAiAccessUserVO(AiAccessUserVO aiAccessUserVO) {
        this.aiAccessUserVO = aiAccessUserVO;
    }

    public AiVariablesVO getVariables() {
        return variables;
    }

    public void setVariables(AiVariablesVO variables) {
        this.variables = variables;
    }

    public ChatStreamingResponseHandler getStreamingResponseHandler() {
        return streamingResponseHandler;
    }

    public void setStreamingResponseHandler(ChatStreamingResponseHandler streamingResponseHandler) {
        this.streamingResponseHandler = streamingResponseHandler;
    }

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public static class ParamValidationResult {
        private final AiMessage aiMessage;
        private final boolean stopExecute;

        public ParamValidationResult(AiMessage aiMessage, boolean stopExecute) {
            this.aiMessage = aiMessage;
            this.stopExecute = stopExecute;
        }

        public static ParamValidationResult build(String aiMessage, boolean stopExecute) {
            return new ParamValidationResult(new AiMessage(aiMessage), stopExecute);
        }


        public boolean isStopExecute() {
            return stopExecute;
        }

        public AiMessage getAiMessage() {
            return aiMessage;
        }
    }

    public static class ToolMethod {
        private final ToolSpecification toolSpecification;
        private final Method method;
        private final Tools tool;
        private final String[] parameterNames;
        private final Map<String, String> parameterDefaultValueMap;
        private final String englishName;

        public ToolMethod(Tools tool,
                          ToolSpecification toolSpecification,
                          String englishName,
                          String[] parameterNames,
                          Method method,
                          Map<String, String> parameterDefaultValueMap) {
            this.englishName = englishName;
            this.toolSpecification = toolSpecification;
            this.method = method;
            this.tool = tool;
            this.parameterNames = parameterNames;
            this.parameterDefaultValueMap = parameterDefaultValueMap;
        }

        public static ToolMethod select(List<ToolMethod> toolMethodList, String name, boolean isSupportChineseToolName) {
            for (ToolMethod toolMethod : toolMethodList) {
                String methodName;
                if (isSupportChineseToolName) {
                    methodName = toolMethod.name();
                } else {
                    methodName = toolMethod.englishName();
                }
                if (Objects.equals(methodName, name)) {
                    return toolMethod;
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return method.toString();
        }

        public Tools tool() {
            return tool;
        }

        public String[] parameterNames() {
            return parameterNames;
        }

        public String name() {
            return toolSpecification.name();
        }

        private String englishName() {
            return englishName;
        }

        public Map<String, String> parameterDefaultValueMap() {
            return parameterDefaultValueMap;
        }

        public String description() {
            return toolSpecification.description();
        }

        public ToolSpecification toRequest(boolean isSupportChineseToolName) {
            if (isSupportChineseToolName) {
                return toolSpecification;
            } else {
                return ToolSpecification.builder()
                        .name(englishName())
                        .description(toolSpecification.description())
                        .parameters(toolSpecification.parameters())
                        .build();
            }
        }

        public Method method() {
            return method;
        }
    }
}
