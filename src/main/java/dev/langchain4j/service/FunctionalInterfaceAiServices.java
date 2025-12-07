package dev.langchain4j.service;

import com.github.aiassistant.entity.model.chat.QuestionClassifyListVO;
import com.github.aiassistant.entity.model.chat.WebSearchResultVO;
import com.github.aiassistant.enums.AiWebSearchSourceEnum;
import com.github.aiassistant.exception.JsonschemaConfigException;
import com.github.aiassistant.service.jsonschema.JsonSchemaApi;
import com.github.aiassistant.service.text.ChatStreamingResponseHandler;
import com.github.aiassistant.service.text.GenerateRequest;
import com.github.aiassistant.service.text.sseemitter.AiMessageString;
import com.github.aiassistant.service.text.tools.Tools;
import com.github.aiassistant.service.text.tools.functioncall.UrlReadTools;
import com.github.aiassistant.util.MergeChatStreamingResponseHandler;
import com.github.aiassistant.util.ReflectUtil;
import com.github.aiassistant.util.ThrowableUtil;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.input.structured.StructuredPrompt;
import dev.langchain4j.model.input.structured.StructuredPromptProcessor;
import dev.langchain4j.model.openai.JsonSchemasString;
import dev.langchain4j.model.openai.OpenAiChatClient;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.rag.AugmentationRequest;
import dev.langchain4j.rag.AugmentationResult;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.query.Metadata;
import dev.langchain4j.spi.services.TokenStreamAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static dev.langchain4j.exception.IllegalConfigurationException.illegalConfiguration;
import static dev.langchain4j.internal.Exceptions.illegalArgument;
import static dev.langchain4j.spi.ServiceHelper.loadFactories;

public class FunctionalInterfaceAiServices<T> extends AiServices<T> {
    private final Logger log;
    private final Collection<TokenStreamAdapter> tokenStreamAdapters = loadFactories(TokenStreamAdapter.class);
    private final String systemMessage;
    private final String userMessage;
    private final Map<String, Object> variables;
    private final ChatStreamingResponseHandler responseHandler;
    private final List<Tools.ToolMethod> toolMethodList;
    private final boolean isSupportChineseToolName;
    private final Object memoryId;
    private final String modelName;
    private final QuestionClassifyListVO classifyListVO;
    private final Boolean websearch;
    private final Boolean reasoning;
    private final Executor executor;
    private final Integer jsonschemaId;
    private final String jsonSchemaEnum;

    public FunctionalInterfaceAiServices(AiServiceContext context,
                                         Integer jsonschemaId,
                                         String jsonSchemaEnum, String systemMessage,
                                         String userMessage,
                                         Map<String, Object> variables,
                                         ChatStreamingResponseHandler responseHandler,
                                         List<Tools.ToolMethod> toolMethodList,
                                         boolean isSupportChineseToolName,
                                         QuestionClassifyListVO classifyListVO,
                                         Boolean websearch,
                                         Boolean reasoning,
                                         String modelName,
                                         Object memoryId,
                                         Executor executor) {
        super(context);
        this.jsonschemaId = jsonschemaId;
        this.jsonSchemaEnum = jsonSchemaEnum;
        this.executor = executor;
        this.classifyListVO = classifyListVO;
        this.websearch = websearch;
        this.reasoning = reasoning;
        this.memoryId = memoryId;
        this.modelName = modelName;
        this.isSupportChineseToolName = isSupportChineseToolName;
        this.toolMethodList = toolMethodList;
        Class<?> aiServiceClass = context.aiServiceClass;
        if (!aiServiceClass.isAnnotationPresent(FunctionalInterface.class)) {
            throw new UnsupportedOperationException(String.format("JsonSchema aiServiceClass must FunctionalInterface! %s", aiServiceClass));
        }
        Method method = Arrays.stream(aiServiceClass.getMethods()).filter(e -> !e.isDefault()).findFirst().orElse(null);
        if (method != null) {
            Class<?> returnType = method.getReturnType();
            boolean streaming = isReturnTypeTokenStream(method) || canAdaptTokenStreamTo(returnType);
            if (!streaming) {
                throw new UnsupportedOperationException(String.format("JsonSchema aiServiceClass must streaming! %s %s", aiServiceClass, method));
            }
        }
        this.log = LoggerFactory.getLogger(aiServiceClass);
        this.responseHandler = responseHandler == null ? ChatStreamingResponseHandler.EMPTY : responseHandler;
        this.systemMessage = systemMessage;
        this.userMessage = userMessage;
        this.variables = variables;
    }

    private static boolean isReturnTypeTokenStream(Method method) {
        Class<?> returnType = method.getReturnType();
        return TokenStream.class.isAssignableFrom(returnType);
    }

    static void validateParameters(Method method) {
        Parameter[] parameters = method.getParameters();
        if (parameters == null || parameters.length < 2) {
            return;
        }

        for (Parameter parameter : parameters) {
            V v = parameter.getAnnotation(V.class);
            dev.langchain4j.service.UserMessage userMessage = parameter.getAnnotation(dev.langchain4j.service.UserMessage.class);
            MemoryId memoryId = parameter.getAnnotation(MemoryId.class);
            UserName userName = parameter.getAnnotation(UserName.class);
            if (v == null && userMessage == null && memoryId == null && userName == null) {
                throw illegalConfiguration(
                        "Parameter '%s' of method '%s' should be annotated with @V or @UserMessage " +
                                "or @UserName or @MemoryId", parameter.getName(), method.getName()
                );
            }
        }
    }

    private static String getVariableName(Parameter parameter) {
        V annotation = parameter.getAnnotation(V.class);
        if (annotation != null) {
            return annotation.value();
        } else {
            return parameter.getName();
        }
    }

    private static String getValueOfVariableIt(Parameter[] parameters, Object[] args) {
        if (parameters.length == 1) {
            Parameter parameter = parameters[0];
            if (!parameter.isAnnotationPresent(MemoryId.class)
                    && !parameter.isAnnotationPresent(dev.langchain4j.service.UserMessage.class)
                    && !parameter.isAnnotationPresent(UserName.class)
                    && (!parameter.isAnnotationPresent(V.class) || isAnnotatedWithIt(parameter))) {
                return toString(args[0]);
            }
        }

        for (int i = 0; i < parameters.length; i++) {
            if (isAnnotatedWithIt(parameters[i])) {
                return toString(args[i]);
            }
        }

        throw illegalConfiguration("Error: cannot find the value of the prompt template variable \"{{it}}\".");
    }

    private static boolean isAnnotatedWithIt(Parameter parameter) {
        V annotation = parameter.getAnnotation(V.class);
        return annotation != null && "it".equals(annotation.value());
    }

    private static Optional<String> findUserMessageTemplateFromMethodAnnotation(Method method) {
        return Optional.ofNullable(method.getAnnotation(dev.langchain4j.service.UserMessage.class))
                .map(a -> getTemplate(method, "User", a.fromResource(), a.value(), a.delimiter()));
    }

    private static Optional<String> findUserMessageTemplateFromAnnotatedParameter(Parameter[] parameters, Object[] args) {
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(dev.langchain4j.service.UserMessage.class)) {
                return Optional.of(toString(args[i]));
            }
        }
        return Optional.empty();
    }

    private static Optional<String> findUserMessageTemplateFromTheOnlyArgument(Parameter[] parameters, Object[] args) {
        if (parameters != null && parameters.length == 1 && parameters[0].getAnnotations().length == 0) {
            return Optional.of(toString(args[0]));
        }
        return Optional.empty();
    }

    private static Optional<String> findUserName(Parameter[] parameters, Object[] args) {
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(UserName.class)) {
                return Optional.of(args[i].toString());
            }
        }
        return Optional.empty();
    }

    private static String getTemplate(Method method, String type, String resource, String[] value, String delimiter) {
        String messageTemplate;
        if (!resource.trim().isEmpty()) {
            messageTemplate = getResourceText(method.getDeclaringClass(), resource);
            if (messageTemplate == null) {
                throw illegalConfiguration("@%sMessage's resource '%s' not found", type, resource);
            }
        } else {
            messageTemplate = String.join(delimiter, value);
        }
        if (messageTemplate.trim().isEmpty()) {
            throw illegalConfiguration("@%sMessage's template cannot be empty", type);
        }
        return messageTemplate;
    }

    private static String getResourceText(Class<?> clazz, String resource) {
        InputStream inputStream = clazz.getResourceAsStream(resource);
        if (inputStream == null) {
            inputStream = clazz.getResourceAsStream("/" + resource);
        }
        return getText(inputStream);
    }

    private static String getText(InputStream inputStream) {
        if (inputStream == null) {
            return null;
        }
        try (Scanner scanner = new Scanner(inputStream);
             Scanner s = scanner.useDelimiter("\\A")) {
            return s.hasNext() ? s.next() : "";
        }
    }

    private static Optional<Object> findMemoryId(Method method, Object[] args) {
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(MemoryId.class)) {
                Object memoryId = args[i];
                if (memoryId == null) {
                    throw illegalArgument(
                            "The value of parameter '%s' annotated with @MemoryId in method '%s' must not be null",
                            parameters[i].getName(), method.getName()
                    );
                }
                return Optional.of(memoryId);
            }
        }
        return Optional.empty();
    }

    private static String toString(Object arg) {
        if (arg.getClass().isArray()) {
            return arrayToString(arg);
        } else if (arg.getClass().isAnnotationPresent(StructuredPrompt.class)) {
            return StructuredPromptProcessor.toPrompt(arg).text();
        } else {
            return arg.toString();
        }
    }

    private static String arrayToString(Object arg) {
        StringBuilder sb = new StringBuilder("[");
        int length = Array.getLength(arg);
        for (int i = 0; i < length; i++) {
            sb.append(toString(Array.get(arg, i)));
            if (i < length - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    public static StreamingChatLanguageModel adapter(OpenAiChatClient client) {
        return new OpenAiStreamingChatLanguageModel(client);
    }

    public static OpenAiChatClient unAdapter(StreamingChatLanguageModel model) {
        if (model instanceof OpenAiStreamingChatLanguageModel) {
            return ((OpenAiStreamingChatLanguageModel) model).client;
        }
        throw new IllegalArgumentException("Tools are currently not supported by this model");
    }

    private boolean canAdaptTokenStreamTo(Type returnType) {
        for (TokenStreamAdapter tokenStreamAdapter : tokenStreamAdapters) {
            if (tokenStreamAdapter.canAdaptTokenStreamTo(returnType)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public T build() {
        performBasicValidation();
        List<Class<?>> interfaces = new ArrayList<>(Arrays.asList(context.aiServiceClass.getInterfaces()));
        interfaces.add(context.aiServiceClass);
        Object proxyInstance = Proxy.newProxyInstance(
                context.aiServiceClass.getClassLoader(),
                interfaces.toArray(new Class[interfaces.size()]),
                new StreamInvocationHandler());
        return (T) proxyInstance;
    }

    private static class OpenAiStreamingChatLanguageModel implements StreamingChatLanguageModel {

        private final OpenAiChatClient client;

        OpenAiStreamingChatLanguageModel(OpenAiChatClient client) {
            this.client = client;
        }

        @Override
        public void generate(List<ChatMessage> messages, StreamingResponseHandler<AiMessage> handler) {
            throw new IllegalArgumentException("Tools are currently not supported by this model");
        }
    }

    private static class FunctionalInterfaceChatStreamingResponseHandler implements ChatStreamingResponseHandler {
        private final AiServiceTokenStream source;

        FunctionalInterfaceChatStreamingResponseHandler(AiServiceTokenStream source) {
            this.source = source;
        }

        @Override
        public void onError(Throwable error, int baseMessageIndex, int addMessageCount, int generateCount) {
            if (source.errorHandler != null) {
                source.errorHandler.accept(error);
            }
        }

        @Override
        public void onToolCalls(Response<AiMessage> response) {
            try {
                source.parentResponseHandler.onJsonSchemaToolCalls(response);
            } catch (Exception e) {
                source.log.warn("JsonSchema onJsonSchemaToolCalls error {}", e.toString(), e);
            }
        }

        @Override
        public void beforeUrlRead(AiWebSearchSourceEnum sourceEnum, String providerName, String question, UrlReadTools urlReadTools, WebSearchResultVO.Row row) {
            try {
                source.parentResponseHandler.beforeUrlRead(sourceEnum, providerName, question, urlReadTools, row);
            } catch (Exception e) {
                source.log.warn("JsonSchema beforeUrlRead error {}", e.toString(), e);
            }
        }

        @Override
        public void beforeWebSearch(AiWebSearchSourceEnum sourceEnum, String providerName, String question) {
            try {
                source.parentResponseHandler.beforeWebSearch(sourceEnum, providerName, question);
            } catch (Exception e) {
                source.log.warn("JsonSchema beforeWebSearch error {}", e.toString(), e);
            }
        }

        @Override
        public void afterUrlRead(AiWebSearchSourceEnum sourceEnum, String providerName, String question, UrlReadTools urlReadTools, WebSearchResultVO.Row row, String content, String merge, long cost) {
            try {
                source.parentResponseHandler.afterUrlRead(sourceEnum, providerName, question, urlReadTools, row, content, merge, cost);
            } catch (Exception e) {
                source.log.warn("JsonSchema afterUrlRead error {}", e.toString(), e);
            }
        }

        @Override
        public void afterWebSearch(AiWebSearchSourceEnum sourceEnum, String providerName, String question, WebSearchResultVO resultVO, long cost) {
            try {
                source.parentResponseHandler.afterWebSearch(sourceEnum, providerName, question, resultVO, cost);
            } catch (Exception e) {
                source.log.warn("JsonSchema afterWebSearch error {}", e.toString(), e);
            }
        }

        @Override
        public void onComplete(Response<AiMessage> response, int baseMessageIndex, int addMessageCount, int generateCount) {
            try {
                source.parentResponseHandler.onJsonSchema(source.context.aiServiceClass, source.systemMessage, source.userMessage, response.content());
            } catch (Exception e) {
                source.log.warn("JsonSchema onJsonSchema error {}", e.toString(), e);
            }
            if (source.completionHandler != null) {
                source.completionHandler.accept(response);
            }
        }

        @Override
        public void onToken(AiMessageString token, int baseMessageIndex, int addMessageCount) {
            if (source.tokenHandler != null) {
                source.tokenHandler.accept(token);
            }
        }
    }

    public static class AiServiceTokenStream implements TokenStream, JsonSchemaTokenStream {

        private final AiServiceContext context;
        private final List<Tools.ToolMethod> toolMethodList;
        private final ChatStreamingResponseHandler parentResponseHandler;
        private final boolean isSupportChineseToolName;
        private final Object memoryId;
        private final String modelName;
        private final QuestionClassifyListVO classifyListVO;
        private final Boolean websearch;
        private final Boolean reasoning;
        private final Object proxy;
        private final Logger log;
        private final Executor executor;
        private final FunctionalInterfaceChatStreamingResponseHandler functionalInterfaceResponseHandler;
        private final String systemMessageString;
        private final String userMessageString;
        private final Map<String, Object> variablesMap;
        private final Integer jsonschemaId;
        private final String jsonSchemaEnum;
        private final Method method;
        private final Object[] args;
        private List<ChatMessage> messages;
        private UserMessage userMessage;
        private SystemMessage systemMessage;
        private Consumer<AiMessageString> tokenHandler;
        private Consumer<List<Content>> contentsHandler;
        private Consumer<Throwable> errorHandler;
        private Consumer<Response<AiMessage>> completionHandler;
        private ChatStreamingResponseHandler responseHandler;
        private JsonSchema jsonSchema;

        public AiServiceTokenStream(Method method, Object[] args,
                                    String modelName,
                                    Integer jsonschemaId,
                                    String jsonSchemaEnum,
                                    String userMessageString,
                                    String systemMessageString,
                                    Map<String, Object> variablesMap,
                                    AiServiceContext context,
                                    List<Tools.ToolMethod> toolMethodList,
                                    boolean isSupportChineseToolName,
                                    Object memoryId,
                                    QuestionClassifyListVO classifyListVO,
                                    Boolean websearch,
                                    Boolean reasoning,
                                    ChatStreamingResponseHandler responseHandler,
                                    Object proxy,
                                    Logger log,
                                    Executor executor) {
            this.method = method;
            this.args = args;
            this.jsonschemaId = jsonschemaId;
            this.jsonSchemaEnum = jsonSchemaEnum;
            this.userMessageString = userMessageString;
            this.systemMessageString = systemMessageString;
            this.variablesMap = variablesMap;
            this.executor = executor;
            this.classifyListVO = classifyListVO;
            this.websearch = websearch;
            this.reasoning = reasoning;
            this.modelName = modelName;
            this.memoryId = memoryId;
            this.log = log;
            this.isSupportChineseToolName = isSupportChineseToolName;
            this.toolMethodList = toolMethodList;
            this.context = context;
            this.parentResponseHandler = responseHandler;
            this.proxy = proxy;
            this.responseHandler = this.functionalInterfaceResponseHandler = new FunctionalInterfaceChatStreamingResponseHandler(this);
        }

        private static Map<String, Object> findTemplateVariables(Map<String, Object> variablesMap, String template, Method method, Object[] args) {
            Parameter[] parameters = method.getParameters();

            // wangzihao 12-09 增加支持内置变量 new HashMap<>(this.variables);
            Map<String, Object> variables = new HashMap<>(variablesMap);
            for (int i = 0; i < parameters.length; i++) {
                String variableName = getVariableName(parameters[i]);
                Object variableValue = args[i];
                variables.put(variableName, variableValue);
            }

            if (template.contains("{{it}}") && !variables.containsKey("it")) {
                String itValue = getValueOfVariableIt(parameters, args);
                variables.put("it", itValue);
            }

            return variables;
        }

        private static Optional<SystemMessage> prepareSystemMessage(Integer jsonschemaId,
                                                                    String jsonSchemaEnum, AiServiceContext context, Map<String, Object> variablesMap,
                                                                    String systemMessageString, Object memoryId, Method method, Object[] args) throws JsonschemaConfigException {
            Optional<String> template = findSystemMessageTemplate(context, systemMessageString, memoryId, method);
            try {
                return template.map(systemMessageTemplate -> PromptTemplate.from(systemMessageTemplate)
                        .apply(findTemplateVariables(variablesMap, systemMessageTemplate, method, args))
                        .toSystemMessage());
            } catch (Exception e) {
                throw new JsonschemaConfigException(String.format("Jsonschema FunctionalInterfaceAiServices ai_jsonschema[system_prompt_text] config error! ai_jsonschema_id=%s, ai_jsonschema_enum=%s, JavaClass=%s, detail:%s", jsonschemaId, jsonSchemaEnum, context.aiServiceClass.getSimpleName(), e.toString()), e, context.aiServiceClass);
            }
        }

        private static Optional<String> findSystemMessageTemplate(AiServiceContext context, String systemMessage, Object memoryId, Method method) {
            // wangzihao 12-09 增加支持systemMessage从数据库读取 systemMessage != null && !systemMessage.isEmpty()
            if (systemMessage != null && !systemMessage.isEmpty()) {
                return Optional.of(systemMessage);
            }
            dev.langchain4j.service.SystemMessage annotation = method.getAnnotation(dev.langchain4j.service.SystemMessage.class);
            if (annotation != null) {
                return Optional.of(getTemplate(method, "System", annotation.fromResource(), annotation.value(), annotation.delimiter()));
            }
            return context.systemMessageProvider.apply(memoryId);
        }

        private static String getUserMessageTemplate(String userMessage, Method method, Object[] args) {
            // wangzihao 12-09 增加支持userMessage从数据库读取 userMessage != null && !userMessage.isEmpty()
            if (userMessage != null && !userMessage.isEmpty()) {
                return userMessage;
            }
            Optional<String> templateFromMethodAnnotation = findUserMessageTemplateFromMethodAnnotation(method);
            Optional<String> templateFromParameterAnnotation = findUserMessageTemplateFromAnnotatedParameter(method.getParameters(), args);

            if (templateFromMethodAnnotation.isPresent() && templateFromParameterAnnotation.isPresent()) {
                throw illegalConfiguration(
                        "Error: The method '%s' has multiple @UserMessage annotations. Please use only one.",
                        method.getName()
                );
            }

            if (templateFromMethodAnnotation.isPresent()) {
                return templateFromMethodAnnotation.get();
            }
            if (templateFromParameterAnnotation.isPresent()) {
                return templateFromParameterAnnotation.get();
            }

            Optional<String> templateFromTheOnlyArgument = findUserMessageTemplateFromTheOnlyArgument(method.getParameters(), args);
            if (templateFromTheOnlyArgument.isPresent()) {
                return templateFromTheOnlyArgument.get();
            }

            throw illegalConfiguration("Error: The method '%s' does not have a user message defined.", method.getName());
        }

        private static UserMessage prepareUserMessage(Class<?> aiServiceClass, Integer jsonschemaId,
                                                      String jsonSchemaEnum, Map<String, Object> variablesMap, String userMessageString, Method method, Object[] args) throws JsonschemaConfigException {
            String template = getUserMessageTemplate(userMessageString, method, args);
            Map<String, Object> variables = findTemplateVariables(variablesMap, template, method, args);
            Prompt prompt;
            try {
                prompt = PromptTemplate.from(template).apply(variables);
            } catch (IllegalArgumentException e) {
                throw new JsonschemaConfigException(String.format("Jsonschema FunctionalInterfaceAiServices ai_jsonschema[user_prompt_text] config error! ai_jsonschema_id=%s, ai_jsonschema_enum=%s, JavaClass=%s, detail:%s", jsonschemaId, jsonSchemaEnum, aiServiceClass.getSimpleName(), e.toString()), e, aiServiceClass);
            }

            Optional<String> maybeUserName = findUserName(method.getParameters(), args);
            return maybeUserName.map(userName -> UserMessage.from(userName, prompt.text()))
                    .orElseGet(prompt::toUserMessage);
        }

        private void prepareMessages(JsonSchema jsonSchema, AiServiceContext context,
                                     Integer jsonschemaId,
                                     String jsonSchemaEnum,
                                     Map<String, Object> variablesMap, String userMessageString, String systemMessageString, Object memoryId, Method method, Object[] args) throws JsonschemaConfigException {
            Map<String, Object> variablesMapCopy = new HashMap<>(variablesMap);
            variablesMapCopy.putIfAbsent(VAR_JSON_SCHEMA, JsonSchemasString.toJsonString(jsonSchema));

            Optional<SystemMessage> systemMessage = prepareSystemMessage(jsonschemaId, jsonSchemaEnum, context, variablesMapCopy, systemMessageString, memoryId, method, args);
            UserMessage userMessage = prepareUserMessage(context.aiServiceClass, jsonschemaId, jsonSchemaEnum, variablesMapCopy, userMessageString, method, args);
            AugmentationResult augmentationResult;
            if (context.retrievalAugmentor != null) {
                List<ChatMessage> chatMemory = context.hasChatMemory()
                        ? context.chatMemory(memoryId).messages()
                        : null;
                Metadata metadata = Metadata.from(userMessage, memoryId, chatMemory);
                AugmentationRequest augmentationRequest = new AugmentationRequest(userMessage, metadata);
                augmentationResult = context.retrievalAugmentor.augment(augmentationRequest);
                userMessage = (UserMessage) augmentationResult.chatMessage();
            }

            if (context.hasChatMemory()) {
                ChatMemory chatMemory = context.chatMemory(memoryId);
                systemMessage.ifPresent(chatMemory::add);
                chatMemory.add(userMessage);
            }

            List<ChatMessage> messages;
            if (context.hasChatMemory()) {
                messages = context.chatMemory(memoryId).messages();
            } else {
                messages = new ArrayList<>();
                systemMessage.ifPresent(messages::add);
                messages.add(userMessage);
            }
            this.userMessage = userMessage;
            systemMessage.ifPresent(message -> this.systemMessage = message);
            this.messages = messages;
        }

        @Override
        public JsonSchemaTokenStream onNext(Consumer<String> tokenHandler) {
            this.tokenHandler = aiMessageString -> tokenHandler.accept(aiMessageString.getChatString());
            return this;
        }

        @Override
        public JsonSchemaTokenStream onRetrieved(Consumer<List<Content>> contentsHandler) {
            this.contentsHandler = contentsHandler;
            return this;
        }

        @Override
        public JsonSchemaTokenStream onComplete(Consumer<Response<AiMessage>> completionHandler) {
            this.completionHandler = completionHandler;
            return this;
        }

        @Override
        public JsonSchemaTokenStream onError(Consumer<Throwable> errorHandler) {
            this.errorHandler = errorHandler;
            return this;
        }

        @Override
        public JsonSchemaTokenStream ignoreErrors() {
            this.errorHandler = null;
            return this;
        }

        @Override
        public JsonSchemaTokenStream responseHandler(ChatStreamingResponseHandler handler) {
            if (handler == null) {
                this.responseHandler = functionalInterfaceResponseHandler;
            } else {
                this.responseHandler = new MergeChatStreamingResponseHandler(Arrays.asList(handler, functionalInterfaceResponseHandler), handler);
            }
            return this;
        }

        @Override
        public JsonSchemaTokenStream jsonSchema(JsonSchema jsonSchema) {
            this.jsonSchema = jsonSchema;
            return this;
        }

        @Override
        public Class<?> getInterfaceClass() {
            return context.aiServiceClass;
        }

        @Override
        public void start() {
            JsonSchema jsonSchema = this.jsonSchema;
            if (jsonSchema == null && proxy instanceof JsonSchemaApi) {
                jsonSchema = ((JsonSchemaApi) proxy).getJsonSchema();
            }
            try {
                prepareMessages(jsonSchema, context, jsonschemaId, jsonSchemaEnum, variablesMap, userMessageString, systemMessageString, memoryId, method, args);
            } catch (JsonschemaConfigException e) {
                ThrowableUtil.sneakyThrows(e);
                return;
            }
            // JsonSchema默认不开启思考
            GenerateRequest.Options options = new GenerateRequest.Options();
            options.setEnableThinking(Boolean.FALSE);
            options.setJsonSchema(jsonSchema);

            ChatMemory chatMemory = MessageWindowChatMemory.builder().id(memoryId).maxMessages(Integer.MAX_VALUE).build();
            messages.forEach(chatMemory::add);
            JsonschemaFunctionCallStreamingResponseHandler handler = new JsonschemaFunctionCallStreamingResponseHandler(
                    modelName,
                    unAdapter(context.streamingChatModel),
                    chatMemory,
                    responseHandler,
                    Integer.MAX_VALUE,
                    toolMethodList,
                    isSupportChineseToolName,
                    0, new AtomicInteger(), null,
                    classifyListVO, websearch, reasoning, executor, context.aiServiceClass, options);

//            if (contentsHandler != null && retrievedContents != null) {
//                contentsHandler.accept(retrievedContents);
//            }
            handler.generate((handler1, request) -> {
                if (proxy instanceof JsonSchemaApi) {
                    JsonSchemaApi api = (JsonSchemaApi) proxy;
                    api.config(handler1, request);
                }
            });
        }
    }

    class StreamInvocationHandler implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            if (method.getDeclaringClass() == Object.class) {
                // methods like equals(), hashCode() and toString() should not be handled by this proxy
                return method.invoke(this, args);
            }

            // wangzihao 12-11， 支持默认方法
            if (method.isDefault()) {
                return ReflectUtil.invokeMethodHandle(true, proxy, ReflectUtil.overrideMethod(context.aiServiceClass, method).orElse(method), args);
            }

            validateParameters(method);

            Object memoryId = findMemoryId(method, args).orElse(FunctionalInterfaceAiServices.this.memoryId);

            TokenStream tokenStream = new AiServiceTokenStream(
                    method, args,
                    modelName,
                    jsonschemaId,
                    jsonSchemaEnum,
                    userMessage,
                    systemMessage,
                    variables,
                    context,
                    toolMethodList,
                    isSupportChineseToolName,
                    memoryId,
                    classifyListVO,
                    websearch, reasoning,
                    responseHandler,
                    proxy,
                    log,
                    executor
            );
            if (isReturnTypeTokenStream(method)) {
                return tokenStream;
            } else {
                return adapt(tokenStream, method.getReturnType());
            }
        }

        private Object adapt(TokenStream tokenStream, Type returnType) {
            for (TokenStreamAdapter tokenStreamAdapter : tokenStreamAdapters) {
                if (tokenStreamAdapter.canAdaptTokenStreamTo(returnType)) {
                    return tokenStreamAdapter.adapt(tokenStream);
                }
            }
            throw new IllegalStateException("Can't find suitable TokenStreamAdapter to adapt TokenStream to returnType " + returnType);
        }

    }

}
