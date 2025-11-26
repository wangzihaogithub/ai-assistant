package com.github.aiassistant.util;

import com.github.aiassistant.entity.AiAssistantFewshot;
import com.github.aiassistant.entity.AiMemoryMessage;
import com.github.aiassistant.entity.AiMemoryMessageTool;
import com.github.aiassistant.entity.model.chat.AiVariablesVO;
import com.github.aiassistant.entity.model.langchain4j.Fewshot;
import com.github.aiassistant.entity.model.langchain4j.KnowledgeAiMessage;
import com.github.aiassistant.entity.model.langchain4j.ThinkingAiMessage;
import com.github.aiassistant.entity.model.user.AiAccessUserVO;
import com.github.aiassistant.enums.MessageTypeEnum;
import com.github.aiassistant.exception.FewshotConfigException;
import com.github.aiassistant.exception.JsonschemaResultParseException;
import com.github.aiassistant.platform.JsonUtil;
import com.github.aiassistant.service.text.tools.Tools;
import com.github.aiassistant.service.text.tools.WebSearch;
import com.github.aiassistant.service.text.tools.functioncall.WebSearchTools;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.service.TokenStream;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AiUtil {
    public static final AiMessage NULL = new AiMessage("null");
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{(.+?)\\}\\}");
    private static final Pattern TRAILING_COMMA_PATTERN = Pattern.compile(",(\\s*[}\\]])");
    public static int MAX_KEY_LENGTH = 18;

    /**
     * 是否是联网工具
     *
     * @param tool tool
     * @return 是否是联网工具
     */
    private static boolean isWebsearchTool(Tools tool) {
        return tool instanceof WebSearch || tool instanceof WebSearchTools;
    }

    public static List<Tools.ToolMethod> initTool(List<Tools.ToolMethod> toolMethodList, AiVariablesVO variables, AiAccessUserVO user) {
        boolean websearch = Optional.ofNullable(variables).map(AiVariablesVO::getRequest).map(AiVariablesVO.Request::getWebsearch).orElse(true);
        List<Tools.ToolMethod> resultList = new ArrayList<>(toolMethodList.size());
        for (Tools.ToolMethod toolMethod : toolMethodList) {
            Tools tool = toolMethod.tool();
            // 过滤联网
            if (!websearch && isWebsearchTool(tool)) {
                continue;
            }
            // 初始化变量
            tool.setVariables(variables);
            tool.setAiAccessUserVO(user);
            resultList.add(toolMethod);
        }
        return resultList;
    }

    public static CompletableFuture<Boolean> toFutureBoolean(TokenStream tokenStream) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        tokenStream.onComplete(response -> {
                    future.complete("true".equalsIgnoreCase(response.content().text()));
                })
                .onError(future::completeExceptionally)
                .onNext(string -> {

                })
                .start();
        return future;
    }

    public static CompletableFuture<String> toFutureString(TokenStream tokenStream) {
        CompletableFuture<String> future = new CompletableFuture<>();
        tokenStream.onComplete(response -> {
                    future.complete(response.content().text());
                })
                .onError(future::completeExceptionally)
                .onNext(string -> {

                })
                .start();
        return future;
    }

    public static <T> CompletableFuture<T> toFutureJson(TokenStream tokenStream, Class<T> type, Class<?> jsonSchemaClass) {
        CompletableFuture<T> future = new CompletableFuture<>();
        tokenStream.onComplete(response -> {
                    String text = response.content().text();
                    T json;
                    try {
                        json = aiJsonToBean(text, type);
                    } catch (Exception e) {
                        future.completeExceptionally(new JsonschemaResultParseException(
                                String.format("%s#toFutureJson#aiJsonToBean('%s','%s'); parseError=%s", jsonSchemaClass == null ? null : jsonSchemaClass.getName(), text, type, e), e, jsonSchemaClass));
                        return;
                    }
                    try {
                        future.complete(json);
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                })
                .onError(future::completeExceptionally)
                .onNext(string -> {

                })
                .start();
        return future;
    }

    public static Response<AiMessage> filterErrorToolRequestId(Response<AiMessage> response) {
        AiMessage aiMessage = new AiMessage(response.content().toolExecutionRequests().stream().filter(e -> StringUtils.hasText(e.id())).collect(Collectors.toList()));
        return new Response<>(aiMessage, response.tokenUsage(), response.finishReason(), response.metadata());
    }

    public static boolean isErrorAiToolMessage(Response<AiMessage> response) {
        return response.content().hasToolExecutionRequests()
                && response.content().toolExecutionRequests().stream().anyMatch(e -> !StringUtils.hasText(e.id()));
    }

    /**
     * 是否存在需要用户确认工具调用
     * 例：我将再次尝试搜索，这次我会扩大搜索范围，包括更多的岗位类型，希望能找到更适合你的机会。
     * 例：让我再试一次，这次我会特别注意城市的一线要求。
     * 例：接下来，我会给你一些建议，并通过我们内部的工具为你推荐适合你的岗位。
     * 例：接下来，我将使用我们的工具为你推荐一些适合的岗位。让我们开始吧！
     * 例：接下来，我会使用我们的工具来为你推荐一些合适的岗位。让我们先通过“findJob”函数来看看有哪些岗位符合你的需求吧。
     * 例：接下来，我将使用工具来为你推荐一些岗位。
     * 例：接下来，我会使用我们的岗位推荐工具来为你寻找一些适合的岗位。请稍等片刻。
     * 例：考虑到你目前的情况，我会通过我们的系统来帮你查找一些适合的岗位。让我们试试看能否找到符合你条件的职位。
     * 例：为了更具体地为你推荐岗位，我将使用我们的recommendedJob功能来获取一些建议。
     * 例：首先，让我们看看有哪些岗位适合你。我将调用岗位库系统来查找一些与经济学相关且位于一线城市的岗位信息。
     * 例：当然可以！考虑到您在北京或天津寻找非计算机相关的岗位，我将使用我们的岗位库系统为您查找一些合适的职位。请您稍等片刻，我将为您提供几个推荐的岗位。
     * 例：了解你的需求后，我会帮你寻找一些非计算机专业的岗位机会。考虑到你的教育背景和期望工作的城市，我将使用我们的岗位库系统来查找适合你的职位。
     * 例：当然可以，考虑到您不希望局限于计算机相关的岗位，我会根据您的专业背景和期望工作城市为您推荐一些其他类型的岗位。请您稍等一下，我将为您查找一些合适的岗位信息。
     * 例：接下来，我将重新调用岗位库系统，这次我会更加精确地筛选出符合您条件的岗位。
     * 例：让我们开始查询适合您的岗位吧：
     * 例：请您稍等片刻，我马上为您查询
     * 例：接下来，我会基于这些城市为您查找一些适合土木专业的岗位。请允许我稍后为您进行查询。
     * 反例：如果您对上述岗位感兴趣或者希望了解更多不同类型的岗位，请告诉我，我会继续为您提供更多选择。希望这些建议能帮助您找到心仪的岗位！
     * 反例：太好了！很高兴您对这些岗位感兴趣。接下来您可以根据自己的实际情况，选择一个或几个感兴趣的岗位进行进一步了解。如果您需要更详细的岗位信息，比如具体的岗位职责、任职要求或是申请流程等，随时可以告诉我，我会尽力为您提供帮助。
     *
     * @param tools    tools
     * @param response response
     * @return 是否存在需要用户确认工具调用
     */
    public static boolean isNeedConfirmToolCall(Response<AiMessage> response, Collection<Tools.ToolMethod> tools) {
        AiMessage message = response.content();
        if (!message.hasToolExecutionRequests()) {
            String text = message.text();
            if (text != null && text.length() > 2) {
                int maxNameLength = tools.stream().map(Tools.ToolMethod::name).max(Comparator.comparing(String::length)).orElse("").length();
                if (isTokens(text, maxNameLength)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isTokens(String text, int maxNameLength) {
        // 让我们开始查询适合您的岗位吧
//                if (isTokens(text, 0, 5 + maxNameLength, "让我们", "开始", "查询")) {
//                    return true;
//                }
        // 请您稍等片刻，我马上为您查询
//                if (isTokens(text, 0, 12 + maxNameLength, "稍等", "我", "为您")) {
//                    return true;
//                }
        // 了解你的需求后，我会帮你寻找一些非计算机专业的岗位机会。考虑到你的教育背景和期望工作的城市，我将使用我们的岗位库系统来查找适合你的职位。
//                if (isTokens(text, 0, 12 + maxNameLength, "我将", "使用")) {
//                    return true;
//                }
        // 接下来，我将重新调用岗位库系统，这次我会更加精确地筛选出符合您条件的岗位。
        if (isTokens(text, 0, 12 + maxNameLength, "我将", "重新调用")) {
            return true;
        }
        // 当然可以！考虑到您在北京或天津寻找非计算机相关的岗位，我将使用我们的岗位库系统为您查找一些合适的职位。请您稍等片刻，我将为您提供几个推荐的岗位。
        if (isTokens(text, 0, 25 + maxNameLength, "我", "您稍等")) {
            return true;
        }
        // 当然可以，考虑到您不希望局限于计算机相关的岗位，我会根据您的专业背景和期望工作城市为您推荐一些其他类型的岗位。请您稍等一下，我将为您查找一些合适的岗位信息。
        if (isTokens(text, 0, 12 + maxNameLength, "稍等", "我将")) {
            return true;
        }
        // 首先，让我们看看有哪些岗位适合你。我将调用岗位库系统来查找一些与经济学相关且位于一线城市的岗位信息。
//                if (isTokens(text, 0, 12 + maxNameLength, "我", "调用", "来")) {
//                    return true;
//                }
        // 我将再次尝试搜索，这次我会扩大搜索范围，包括更多的岗位类型，希望能找到更适合你的机会。
//                if (isTokens(text, 0, 12 + maxNameLength, "我", "尝试", "我会")) {
//                    return true;
//                }
        // 我会使用我们企业的岗位库为您推荐一些适合您的岗位。
//                if (isTokens(text, 0, 12 + maxNameLength, "我会使用", "为您")) {
//                    return true;
//                }
        // 让我再试一次，这次我会特别注意城市的一线要求。
//                if (isTokens(text, 0, 12 + maxNameLength, "让我再试一次", "我会")) {
//                    return true;
//                }
        // 接下来，我会使用我们的岗位推荐工具来为你寻找一些适合的岗位。请稍等片刻。
//                if (isTokens(text, 0, 12 + maxNameLength, "我", "使用", "为", "你")) {
//                    return true;
//                }
        // 考虑到你目前的情况，我会通过我们的系统来帮你查找一些适合的岗位。让我们试试看能否找到符合你条件的职位。
//                if (isTokens(text, 0, 12 + maxNameLength, "我", "来帮你")) {
//                    return true;
//                }
        // 好的，朱宣霖同学，我会再次尝试调整参数，以期为你找到更多一线城市的合适岗位。请稍等片刻。
        // 让我再尝试调整一下请求参数，以便更准确地为你找到符合预期的一线城市岗位。请稍等，我将再次尝试。
        // 接下来，我将根据你的一线城市偏好和经济学专业背景，为你推荐几个具体的岗位。请稍等片刻。
        if (isTokens(text, 0, 50 + maxNameLength, "我", "请稍等")) {
            return true;
        }
        // 接下来，我将调用岗位查询工具，寻找适合你的传媒公司岗位。我们将重点关注北京和上海这两个城市，因为它们也是传媒产业的重要中心。
        if (isTokens(text, 0, 12 + maxNameLength, "接下来", "我将调用")) {
            return true;
        }
        // 接下来，我会基于这些城市为您查找一些适合土木专业的岗位。请允许我稍后为您进行查询。
        if (isTokens(text, 0, 12 + maxNameLength, "接下来", "我会", "稍")) {
            return true;
        }
        return false;
    }

    public static <T> CompletableFuture<List<T>> toFutureJsonList(TokenStream tokenStream, Class<T> type, Class<?> jsonSchemaClass) {
        CompletableFuture<List<T>> future = new CompletableFuture<>();
        tokenStream.onComplete(response -> {

                    String text = response.content().text();
                    List<T> json;
                    try {
                        json = aiJsonToList(text, type);
                    } catch (Exception e) {
                        future.completeExceptionally(new JsonschemaResultParseException(
                                String.format("%s#toFutureJsonList#aiJsonToList('%s','%s'); parseError=%s", jsonSchemaClass == null ? null : jsonSchemaClass.getName(), text, type, e), e, jsonSchemaClass));
                        return;
                    }
                    try {
                        future.complete(json);
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                })
                .onError(future::completeExceptionally)
                .onNext(string -> {

                })
                .start();
        return future;
    }

    public static <T> List<T> aiJsonToList(String aiJson, Class<T> type) throws IOException {
        if (aiJson == null || aiJson.isEmpty()) {
            return new ArrayList<>();
        }
        JsonUtil.ObjectReader objectReader = JsonUtil.objectReader();
        try {
            String json = removeTrailingComma(aiJson);
            return objectReader.readValueList(json, type);
        } catch (Exception e) {
            String errorClassName = e.getClass().getSimpleName();
            if (errorClassName.toLowerCase().contains("json")) {
                String[] endWiths = new String[]{"\"]", "\"]", "]"};
                for (String end : endWiths) {
                    try {
                        return objectReader.readValueList(aiJson + end, type);
                    } catch (Exception ignored) {
                    }
                }
            }
            throw e;
        }
    }

    /**
     * 是否可以命中所有命中词
     *
     * @param source    源字符串
     * @param fromIndex 从哪个下标开始搜索
     * @param minSpace  每个命中词间的最小间隔
     * @param keywords  命中词
     * @return true=全部命中
     */
    private static boolean isTokens(String source, int fromIndex, int minSpace, String... keywords) {
        int indexOf = fromIndex;
        for (String keyword : keywords) {
            int nextIndexOf = source.indexOf(keyword, indexOf);
            if (nextIndexOf == -1) {
                return false;
            }
            if ((nextIndexOf - indexOf) > minSpace) {
                // 回溯
                return isTokens(source, indexOf + 1, minSpace, keywords);
            }
            indexOf = nextIndexOf;
        }
        return true;
    }

    public static Long scoreToLong(Double score) {
        return score == null ? null : Math.round(score * 100D);
    }

    public static Double scoreToDouble(Long score) {
        return score == null ? null : score / 100D;
    }

    public static Prompt toPrompt(String promptMessage, Map<String, Object> variables) throws IllegalArgumentException {
        return PromptTemplate.from(promptMessage).apply(variables);
    }

    public static Prompt toPrompt(String promptMessage, AiVariablesVO variables) throws IllegalArgumentException {
        return PromptTemplate.from(promptMessage).apply(BeanUtil.toMap(variables));
    }

    public static int sumUserLength(Collection<ChatMessage> list) {
        int length = 0;
        for (ChatMessage chatMessage : list) {
            if (chatMessage instanceof UserMessage) {
                for (Content content : ((UserMessage) chatMessage).contents()) {
                    if (content instanceof TextContent) {
                        length += Objects.toString(((TextContent) content).text(), "").length();
                    }
                }
            }
        }
        return length;
    }

    public static String toAiXmlString(Map<String, ?> state) {
        if (state == null) {
            return null;
        }
        StringJoiner joiner = new StringJoiner(",\n");
        for (Map.Entry<String, ?> entry : state.entrySet()) {
            joiner.add(toAiXmlString(entry.getKey(), Objects.toString(entry.getValue(), null)));
        }
        return joiner.toString();
    }

    public static String toAiXmlString(String key, String value) {
        return toAiXmlString(key, value, MAX_KEY_LENGTH);
    }

    public static String toAiXmlString(String key, String value, int maxKeyLength) {
        String key64 = StringUtils.left(key, maxKeyLength, true);
        return "<" + key64 + ">" + value + "</" + key64 + ">";
    }

    public static String toJsonString(Object object) {
        try {
            return JsonUtil.objectWriter().writeValueAsString(object);
        } catch (IOException e) {
            ThrowableUtil.sneakyThrows(e);
            return null;
        }
    }

    public static int sumLength(Collection<ChatMessage> list) {
        int length = 0;
        for (ChatMessage chatMessage : list) {
            if (chatMessage instanceof AiMessage) {
                length += Objects.toString(((AiMessage) chatMessage).text(), "").length();
            } else if (chatMessage instanceof SystemMessage) {
                length += Objects.toString(((SystemMessage) chatMessage).text(), "").length();
            } else if (chatMessage instanceof ToolExecutionResultMessage) {
                length += Objects.toString(((ToolExecutionResultMessage) chatMessage).text(), "").length();
            } else if (chatMessage instanceof UserMessage) {
                for (Content content : ((UserMessage) chatMessage).contents()) {
                    if (content instanceof TextContent) {
                        length += Objects.toString(((TextContent) content).text(), "").length();
                    }
                }
            }
        }
        return length;
    }

    public static int sumAiLength(Collection<ChatMessage> list) {
        int length = 0;
        for (ChatMessage chatMessage : list) {
            if (chatMessage instanceof AiMessage) {
                length += Objects.toString(((AiMessage) chatMessage).text(), "").length();
            } else if (chatMessage instanceof SystemMessage) {
                length += Objects.toString(((SystemMessage) chatMessage).text(), "").length();
            } else if (chatMessage instanceof ToolExecutionResultMessage) {
                length += Objects.toString(((ToolExecutionResultMessage) chatMessage).text(), "").length();
            }
        }
        return length;
    }

    public static int sumKnowledgeLength(Collection<ChatMessage> list) {
        int length = 0;
        for (ChatMessage chatMessage : list) {
            if (chatMessage instanceof KnowledgeAiMessage) {
                length += Objects.toString(((KnowledgeAiMessage) chatMessage).text(), "").length();
            }
        }
        return length;
    }

    public static boolean isTypeThinkingAiMessage(ChatMessage message) {
        return message instanceof ThinkingAiMessage;
    }

    public static boolean isTypeFewshot(ChatMessage message) {
        return message instanceof Fewshot;
    }

    public static boolean isTypeUser(ChatMessage message) {
        return message != null && message.getClass() == UserMessage.class;
    }

    public static List<ChatMessage> deserializeFewshot(List<AiAssistantFewshot> dbList, AiVariablesVO variables) throws FewshotConfigException {
        List<ChatMessage> list = new ArrayList<>();
        if (dbList.isEmpty()) {
            return list;
        }
        Map<String, Object> variablesMap = BeanUtil.toMap(variables);
        for (AiAssistantFewshot db : dbList) {
            String text = db.getMessageText();
            String messageTypeEnum = db.getMessageTypeEnum();
            MessageTypeEnum typeEnum = MessageTypeEnum.getByCode(messageTypeEnum);
            ChatMessage message;
            if (StringUtils.hasText(text)) {
                String promptText;
                try {
                    promptText = toPrompt(text, variablesMap).text();
                } catch (Exception e) {
                    throw new FewshotConfigException(String.format("ai_assistant_fewshot[message_text] config error! messageTypeEnum:[%s], ID[%s], detail:%s", messageTypeEnum, db.getId(), e.toString()),
                            e, db);
                }
                switch (typeEnum) {
                    case Ai: {
                        message = new Fewshot.AiMessage(promptText);
                        break;
                    }
                    case User: {
                        message = new Fewshot.UserMessage(promptText);
                        break;
                    }
                    default: {
                        message = NULL;
                    }
                }
            } else {
                message = NULL;
            }
            list.add(message);
        }
        return list;
    }

    public static List<ChatMessage> deserializeMemory(List<AiMemoryMessage> dbList, List<AiMemoryMessageTool> toolList, boolean retainFunctionCall) {
        Map<Integer, List<AiMemoryMessageTool>> toolMap = toolList.stream().collect(Collectors.groupingBy(e -> e.getAiMemoryMessageId()));
        List<ChatMessage> list = new ArrayList<>();
        for (AiMemoryMessage db : dbList) {
            String messageTypeEnum = db.getMessageTypeEnum();
            MessageTypeEnum typeEnum = MessageTypeEnum.getByCode(messageTypeEnum);
            String text = db.getMessageText();
            Integer id = db.getId();
            ChatMessage message;
            switch (typeEnum) {
                case User: {
                    message = new UserMessage(text);
                    break;
                }
                case Ai: {
                    List<ToolExecutionRequest> messageToolList;
                    if (retainFunctionCall) {
                        messageToolList = toolMap.getOrDefault(id, Collections.emptyList()).stream()
                                .map(e -> ToolExecutionRequest.builder()
                                        .id(e.getToolRequestId())
                                        .arguments(e.getToolArguments())
                                        .name(e.getToolName())
                                        .build())
                                .collect(Collectors.toList());
                    } else {
                        messageToolList = Collections.emptyList();
                    }
                    boolean hasText = StringUtils.hasText(text);
                    if (hasText && !messageToolList.isEmpty()) {
                        message = new AiMessage(text, messageToolList);
                    } else if (hasText) {
                        message = new AiMessage(text);
                    } else if (!messageToolList.isEmpty()) {
                        message = new AiMessage(messageToolList);
                    } else {
                        message = NULL;
                    }
                    break;
                }
                case ToolResult: {
                    if (retainFunctionCall) {
                        message = new ToolExecutionResultMessage(db.getReplyToolRequestId(), db.getReplyToolName(), text);
                    } else {
                        message = NULL;
                    }
                    break;
                }
                case MState:
                case Knowledge:
                case System:
                case Thinking:
                case LangChainUser: {
                    message = NULL;
                    break;
                }
                default: {
                    throw new IllegalArgumentException("unknown message type: " + messageTypeEnum);
                }
            }
            list.add(message);
        }
        return list;
    }

    public static String getLastUserQuestion(List<ChatMessage> historyList) {
        if (historyList != null && !historyList.isEmpty()) {
            for (int i = historyList.size() - 1; i >= 0; i--) {
                ChatMessage message = historyList.get(i);
                if (message instanceof UserMessage) {
                    return userMessageToString((UserMessage) message);
                }
            }
        }
        return null;
    }

    public static String userMessageToString(UserMessage userMessage) {
        if (userMessage == null) {
            return null;
        }
        return userMessage.contents().stream().filter(e -> e instanceof TextContent).map(e -> (TextContent) e).map(TextContent::text).collect(Collectors.joining("\n"));
    }

    public static List<ChatMessage> beforeGenerate(List<ChatMessage> messages) {
        List<ChatMessage> list = messages.stream().filter(e -> e != NULL).collect(Collectors.toList());
        // 解决模型认为之前AI已经回复过，不返回结果
        return distinctMessage(removeLastAiMessage(list));
    }

    /**
     * 去掉重复消息
     */
    private static List<ChatMessage> distinctMessage(List<ChatMessage> list) {
        List<ChatMessage> result = new ArrayList<>(list.size());
        String lastMessage = null;
        Class lastType = null;
        for (ChatMessage message : list) {
            if (message instanceof AiMessage) {
                String s = ((AiMessage) message).text();
                if (lastType == null || !AiMessage.class.isAssignableFrom(lastType) || !Objects.equals(s, lastMessage)) {
                    result.add(message);
                }
                lastMessage = s;
            } else if (message instanceof UserMessage) {
                String s = userMessageToString((UserMessage) message);
                if (lastType == null || !UserMessage.class.isAssignableFrom(lastType) || !Objects.equals(s, lastMessage)) {
                    result.add(message);
                }
                lastMessage = s;
            } else {
                result.add(message);
                lastMessage = null;
            }
            lastType = message.getClass();
        }
        return result;
    }

    private static List<ChatMessage> removeLastAiMessage(List<ChatMessage> list) {
        for (int i = list.size() - 1; i > 0; i--) {
            ChatMessage message = list.get(i);
            if (message.getClass() != AiMessage.class) {
                return list.subList(0, i + 1);
            }
        }
        return list;
    }

    public static <T> String toArrayJson(Collection<T> elements, Function<T, String> getter) {
        StringJoiner joiner = new StringJoiner("\",\"", "[\"", "\"]");
        for (T cs : elements) {
            joiner.add(getter.apply(cs));
        }
        return joiner.toString();
    }

    public static List<ChatMessage> removeSystemMessage(Collection<ChatMessage> historyList) {
        return historyList.stream().filter(e -> !(e instanceof SystemMessage)).collect(Collectors.toList());
    }

    public static boolean existPromptVariable(String template, String varKey) {
        return existPromptVariable(template, Collections.singletonList(varKey));
    }

    public static boolean existPromptVariable(String template, Collection<String> varKeys) {
        if (template == null || template.isEmpty()) {
            return false;
        }
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        while (matcher.find()) {
            String varKey = matcher.group(1);
            if (varKeys.contains(varKey)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Removes trailing commas before closing braces or brackets in JSON strings.
     * [1, 2, 3, ] => [1, 2, 3]
     *
     * @param json the JSON string
     * @return the corrected JSON string
     */
    private static String removeTrailingComma(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }
        Matcher matcher = TRAILING_COMMA_PATTERN.matcher(json);
        return matcher.replaceAll("$1");
    }

    public static <T> T aiJsonToBean(String aiJson, Class<T> type) throws IOException {
        if (aiJson == null || aiJson.isEmpty()) {
            return null;
        }
        aiJson = removeJsonMarkdown(aiJson);
        String json = removeTrailingComma(aiJson);
        JsonUtil.ObjectReader objectReader = JsonUtil.objectReader();
        try {
            return objectReader.readValue(json, type);
        } catch (Exception e) {
            String errorClassName = e.getClass().getSimpleName();
            if (errorClassName.toLowerCase().contains("json")) {
                String[] endWiths = new String[]{"\"}", "\"]}", "}"};
                for (String end : endWiths) {
                    try {
                        return objectReader.readValue(aiJson + end, type);
                    } catch (Exception ignored) {
                    }
                }
            }

            if (json.startsWith("[") && json.endsWith("]")) {
                List<Field> arrayFieldList = Arrays.stream(type.getDeclaredFields()).filter(o -> Collection.class.isAssignableFrom(o.getType())).collect(Collectors.toList());
                if (arrayFieldList.size() == 1) {
                    Field field = arrayFieldList.get(0);
                    String fieldName = field.getName();
                    if (json.contains(fieldName)) {
                        try {
                            String wrapJson = json.substring(1, json.length() - 1);
                            return objectReader.readValue(wrapJson, type);
                        } catch (Exception ignored) {
                        }
                    } else {
                        try {
                            String wrapJson = "{\"" + fieldName + "\":" + json + "}";
                            return objectReader.readValue(wrapJson, type);
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
            throw e;
        }
    }

    public static String removeJsonMarkdown(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }
        json = json.trim();
        while (json.startsWith("`") || json.startsWith("'") || json.startsWith("(")) {
            json = json.substring(1).trim();
        }
        if (json.startsWith("json")) {
            json = json.substring("json".length()).trim();
        }
        while (json.endsWith("`")) {
            json = json.substring(0, json.length() - 1).trim();
        }
        return json;
    }

    public static boolean isNull(ChatMessage message) {
        return message == NULL;
    }

    public static void addToHistoryList(List<ChatMessage> historyList, SystemMessage systemMessage, List<ChatMessage> fewshotMessageList) {
        // 置顶
        if (fewshotMessageList != null && !fewshotMessageList.isEmpty()) {
            historyList.addAll(0, fewshotMessageList);
        }
        if (systemMessage != null) {
            historyList.add(0, systemMessage);
        }
    }

}
