package com.github.aiassistant.util;

import com.github.aiassistant.entity.AiAssistantFewshot;
import com.github.aiassistant.entity.AiMemoryMessage;
import com.github.aiassistant.entity.model.chat.AiVariables;
import com.github.aiassistant.entity.model.chat.FewshotAiMessage;
import com.github.aiassistant.entity.model.chat.FewshotUserMessage;
import com.github.aiassistant.entity.model.chat.KnowledgeAiMessage;
import com.github.aiassistant.entity.model.user.AiAccessUserVO;
import com.github.aiassistant.enums.MessageTypeEnum;
import com.github.aiassistant.platform.JsonUtil;
import com.github.aiassistant.service.text.tools.Tools;
import com.github.aiassistant.service.text.tools.WebSearch;
import com.github.aiassistant.service.text.tools.functioncall.WebSearchTools;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.service.TokenStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AiUtil {
    public static final AiMessage NULL = new AiMessage("null");
    public static final String ERROR_TYPE_LIMIT_REQUESTS = "limit_requests";
    public static final String ERROR_TYPE_DATA_INSPECTION_FAILED = "data_inspection_failed";
    public static final String ERROR_TYPE_TOKEN_READ_TIMEOUT = "token_read_timeout";
    public static final String ERROR_TYPE_UNKNOWN_ERROR = "unknown_error";
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{(.+?)\\}\\}");
    private static final Pattern TRAILING_COMMA_PATTERN = Pattern.compile(",(\\s*[}\\]])");
    private static final Logger log = LoggerFactory.getLogger(AiUtil.class);

    /**
     * 是否是联网工具
     */
    private static boolean isWebsearchTool(Tools tool) {
        return tool instanceof WebSearch || tool instanceof WebSearchTools;
    }

    public static List<Tools.ToolMethod> initTool(List<Tools.ToolMethod> toolMethodList, AiVariables variables, AiAccessUserVO user) {
        boolean websearch = Optional.ofNullable(variables).map(AiVariables::getRequest).map(AiVariables.Request::getWebsearch).orElse(true);
        List<Tools.ToolMethod> resultList = new ArrayList<>();
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

    public static List<String> splitString(String string) {
        if (StringUtils.hasText(string)) {
            return Arrays.asList(string.split(","));
        } else {
            return Collections.emptyList();
        }
    }

    public static String getErrorType(String errorString) {
        String errorType;
        if (errorString.contains("limit_requests")) {
            errorType = ERROR_TYPE_LIMIT_REQUESTS;
        } else if (errorString.contains("data_inspection_failed")) {
            errorType = ERROR_TYPE_DATA_INSPECTION_FAILED;
        } else if (errorString.contains("onTokenReadTimeout")) {
            errorType = ERROR_TYPE_TOKEN_READ_TIMEOUT;
        } else {
            errorType = ERROR_TYPE_UNKNOWN_ERROR;
        }
        return errorType;
    }

    public static CompletableFuture<Boolean> toFutureBoolean(TokenStream tokenStream) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        tokenStream.onComplete(response -> {
                    if (NULL == response.content()) {
                        // 思考模型会进入这里，这里没有解析思考模型的思考过程。
                        // 如果不是思考模型进入了这里，就是模型供应商的bug
                        log.warn("toFutureBoolean NULL_RESPONSE ");
                    } else {
                        future.complete("true".equalsIgnoreCase(response.content().text()));
                    }
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
                    if (NULL == response.content()) {
                        // 思考模型会进入这里，这里没有解析思考模型的思考过程。
                        // 如果不是思考模型进入了这里，就是模型供应商的bug
                        log.warn("toFutureString NULL_RESPONSE ");
                    } else {
                        future.complete(response.content().text());
                    }
                })
                .onError(future::completeExceptionally)
                .onNext(string -> {

                })
                .start();
        return future;
    }

    public static <T> CompletableFuture<T> toFutureJson(TokenStream tokenStream, Class<T> type) {
        CompletableFuture<T> future = new CompletableFuture<>();
        tokenStream.onComplete(response -> {
                    if (NULL == response.content()) {
                        // 思考模型会进入这里，这里没有解析思考模型的思考过程。
                        // 如果不是思考模型进入了这里，就是模型供应商的bug
                        log.warn("toFutureJson NULL_RESPONSE ");
                    } else {
                        String text = response.content().text();
                        try {
                            T json = toBean(text, type);
                            future.complete(json);
                        } catch (Exception e) {
                            future.completeExceptionally(e);
                        }
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
     * <p>
     * <p>
     * 反例：如果您对上述岗位感兴趣或者希望了解更多不同类型的岗位，请告诉我，我会继续为您提供更多选择。希望这些建议能帮助您找到心仪的岗位！
     * 反例：太好了！很高兴您对这些岗位感兴趣。接下来您可以根据自己的实际情况，选择一个或几个感兴趣的岗位进行进一步了解。如果您需要更详细的岗位信息，比如具体的岗位职责、任职要求或是申请流程等，随时可以告诉我，我会尽力为您提供帮助。
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

    public static Prompt toPrompt(String promptMessage, Map<String, Object> variables) {
        return PromptTemplate.from(promptMessage).apply(variables);
    }

    public static Prompt toPrompt(String promptMessage, AiVariables variables) {
        return toPrompt(promptMessage, toMap(variables));
    }

    public static String limit(String string, int limit, boolean notnull) {
        if (string == null || string.isEmpty()) {
            return notnull ? "" : string;
        }
        return string.length() > limit ? string.substring(0, limit) : string;
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
        String key64 = limit(key, 64);
        return "<" + key64 + ">" + value + "</" + key64 + ">";
    }

    private static String limit(String str, int limit) {
        return str != null && str.length() > limit ? str.substring(0, limit) : Objects.toString(str, "");
    }

    //    public static Function<String,String> tokenChunkFilter(){
//        return new Function<String, String>() {
//            private String prefix = "|\n| ---";
//            private String suffix = " |";
//            private StringBuilder builder = new StringBuilder();
//            private boolean start;
//
//            private boolean isEnd(String q, int s){
//                if((q.length()-s)<=suffix.length()){
//                    return false;
//                }
//                int j =0;
//                for (int i = s,a=0; i < q.length(); i++,a++) {
//                    if(q.charAt(i) != suffix.charAt(j+a)){
//                        return false;
//                    }
//                }
//                return true;
//            }
//            @Override
//            public String apply(String question) {
//                int builderLength = builder.length();
//
//                String result = question;
//                builder.append(question);
//                if(!start) {
//                    int i = builder.indexOf(prefix);
//                    if(i != -1) {
//                        start = true;
//                       int s = i-builderLength;
//                        for (int i1 = s; i1 < question.length(); i1++) {
//                            if()
//                        }
//                        result = question.substring(0,s);
//                    }
//                }else {
//
//                }
//                int outof = builder.length() - prefix.length();
//                if(outof>0) {
//                    builder.delete(0,outof);
//                }
//
//            }
//        };
//    }

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

    public static boolean isFewshot(ChatMessage message) {
        return message instanceof FewshotUserMessage || message instanceof FewshotAiMessage;
    }

    public static boolean isUser(ChatMessage message) {
        return message != null && message.getClass() == UserMessage.class;
    }

    public static List<ChatMessage> deserializeFewshot(List<AiAssistantFewshot> dbList, AiVariables variables) {
        List<ChatMessage> list = new ArrayList<>();
        if (dbList.isEmpty()) {
            return list;
        }
        Map<String, Object> variablesMap = toMap(variables);
        for (AiAssistantFewshot db : dbList) {
            String text = db.getMessageText();
            String messageTypeEnum = db.getMessageTypeEnum();
            MessageTypeEnum typeEnum = MessageTypeEnum.getByCode(messageTypeEnum);
            ChatMessage message;
            if (StringUtils.hasText(text)) {
                Prompt prompt = toPrompt(text, variablesMap);
                String promptText = prompt.text();
                switch (typeEnum) {
                    case Ai: {
                        message = new FewshotAiMessage(promptText);
                        break;
                    }
                    case User: {
                        message = new FewshotUserMessage(promptText);
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

    public static List<ChatMessage> deserializeMemory(List<AiMemoryMessage> dbList) {
        List<ChatMessage> list = new ArrayList<>();
        for (AiMemoryMessage db : dbList) {
            String messageTypeEnum = db.getMessageTypeEnum();
            MessageTypeEnum typeEnum = MessageTypeEnum.getByCode(messageTypeEnum);
            String text = db.getMessageText();
            ChatMessage message;
            switch (typeEnum) {
                case User: {
                    message = new UserMessage(text);
                    break;
                }
                case Ai: {
                    if (StringUtils.hasText(text)) {
                        message = new AiMessage(text);
                    } else {
                        message = NULL;
                    }
                    break;
                }
                case MState:
                case OutofScope:
                case Knowledge:
                case System:
                case ToolResult:
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

    public static boolean isJavaPackage(Class clazz) {
        Package aPackage = clazz.getPackage();
        return aPackage == null || aPackage.getName().startsWith("java.");
    }

    private static void flatMap(Map<String, Object> map, String prefix, Object bean) {
        if (bean == null || "".equals(bean)) {
            map.put(prefix, " ");
            return;
        }
        if (!(bean instanceof Map) && isJavaPackage(bean.getClass())) {
            map.put(prefix, bean);
        } else {
            Map<String, Object> beanMap = bean instanceof Map ? (Map<String, Object>) bean : new BeanMap(bean);
            for (Map.Entry<String, Object> entry : beanMap.entrySet()) {
                String p = (prefix.isEmpty() ? "" : prefix + ".") + entry.getKey();
                Object value = entry.getValue();
                flatMap(map, p, value);
            }
        }
    }

    public static Map<String, Object> toMap(Object bean) {
        Map<String, Object> map = new LinkedHashMap<>();
        Map<String, Object> beanMap = bean instanceof Map ? (Map<String, Object>) bean : new BeanMap(bean);
        for (Map.Entry<String, Object> entry : beanMap.entrySet()) {
            flatMap(map, entry.getKey(), entry.getValue());
        }
        return map;
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

    public static List<ChatMessage> removeSystemMessage(List<ChatMessage> historyList) {
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

    private static <T> T toBean(String aiJson, Class<T> type) throws IOException {
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

    private static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        int sz = str.length();
        for (int i = 0; i < sz; i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static Integer[] parseIntegerNumbers(String str) {
        if (str == null) {
            return new Integer[0];
        }
        List<Integer> result = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c >= '0' && c <= '9') {
                builder.append(c);
            } else if (builder.length() > 0) {
                try {
                    result.add(Integer.valueOf(builder.toString()));
                } catch (Exception e) {
                    return new Integer[0];
                }
                builder.setLength(0);
            }
        }
        if (builder.length() > 0) {
            try {
                result.add(Integer.valueOf(builder.toString()));
            } catch (Exception e) {
                return new Integer[0];
            }
        }
        return result.toArray(new Integer[0]);
    }

    public static Timestamp parseDate(String noHasZoneAnyDateString) {
        if (noHasZoneAnyDateString == null || noHasZoneAnyDateString.isEmpty()) {
            return null;
        }
        int shotTimestampLength = 10;
        int longTimestampLength = 13;
        if (noHasZoneAnyDateString.length() == shotTimestampLength || noHasZoneAnyDateString.length() == longTimestampLength) {
            if (isNumeric(noHasZoneAnyDateString)) {
                long timestamp = Long.parseLong(noHasZoneAnyDateString);
                if (noHasZoneAnyDateString.length() == shotTimestampLength) {
                    timestamp = timestamp * 1000;
                }
                return new Timestamp(timestamp);
            }
        }
        if ("null".equals(noHasZoneAnyDateString)) {
            return null;
        }
        if ("NULL".equals(noHasZoneAnyDateString)) {
            return null;
        }
        Integer[] numbers = parseIntegerNumbers(noHasZoneAnyDateString);
        if (numbers.length == 0) {
            return null;
        } else {
            if (numbers[0] > 2999 || numbers[0] < 1900) {
                return null;
            }
            if (numbers.length >= 2) {
                if (numbers[1] > 12 || numbers[1] <= 0) {
                    return null;
                }
            }
            if (numbers.length >= 3) {
                if (numbers[2] > 31 || numbers[2] <= 0) {
                    return null;
                }
            }
            if (numbers.length >= 4) {
                if (numbers[3] > 24 || numbers[3] < 0) {
                    return null;
                }
            }
            if (numbers.length >= 5) {
                if (numbers[4] >= 60 || numbers[4] < 0) {
                    return null;
                }
            }
            if (numbers.length >= 6) {
                if (numbers[5] >= 60 || numbers[5] < 0) {
                    return null;
                }
            }
            try {
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.MONTH, 0);
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                if (numbers.length == 1) {
                    calendar.set(Calendar.YEAR, numbers[0]);
                } else if (numbers.length == 2) {
                    calendar.set(Calendar.YEAR, numbers[0]);
                    if (noHasZoneAnyDateString.contains("Q") &&
                            (noHasZoneAnyDateString.contains("Q1") || noHasZoneAnyDateString.contains("Q2") || noHasZoneAnyDateString.contains("Q3") || noHasZoneAnyDateString.contains("Q4"))) {
                        calendar.set(Calendar.MONTH, ((numbers[1] - 1) * 3));
                    } else {
                        calendar.set(Calendar.MONTH, numbers[1] - 1);
                    }
                } else if (numbers.length == 3) {
                    calendar.set(Calendar.YEAR, numbers[0]);
                    calendar.set(Calendar.MONTH, numbers[1] - 1);
                    calendar.set(Calendar.DAY_OF_MONTH, numbers[2]);
                } else if (numbers.length == 4) {
                    calendar.set(Calendar.YEAR, numbers[0]);
                    calendar.set(Calendar.MONTH, numbers[1] - 1);
                    calendar.set(Calendar.DAY_OF_MONTH, numbers[2]);
                    calendar.set(Calendar.HOUR_OF_DAY, numbers[3]);
                } else if (numbers.length == 5) {
                    calendar.set(Calendar.YEAR, numbers[0]);
                    calendar.set(Calendar.MONTH, numbers[1] - 1);
                    calendar.set(Calendar.DAY_OF_MONTH, numbers[2]);
                    calendar.set(Calendar.HOUR_OF_DAY, numbers[3]);
                    calendar.set(Calendar.MINUTE, numbers[4]);
                } else {
                    calendar.set(Calendar.YEAR, numbers[0]);
                    calendar.set(Calendar.MONTH, numbers[1] - 1);
                    calendar.set(Calendar.DAY_OF_MONTH, numbers[2]);
                    calendar.set(Calendar.HOUR_OF_DAY, numbers[3]);
                    calendar.set(Calendar.MINUTE, numbers[4]);
                    calendar.set(Calendar.SECOND, numbers[5]);
                }
                return new Timestamp(calendar.getTimeInMillis());
            } catch (Exception e) {
                return null;
            }
        }
    }
}
