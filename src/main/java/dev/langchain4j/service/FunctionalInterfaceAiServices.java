package dev.langchain4j.service;

import com.github.aiassistant.entity.model.chat.WebSearchResultVO;
import com.github.aiassistant.service.text.ChatStreamingResponseHandler;
import com.github.aiassistant.service.text.tools.Tools;
import com.github.aiassistant.service.text.tools.functioncall.UrlReadTools;
import com.github.aiassistant.util.ReflectUtil;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.*;
import dev.langchain4j.exception.IllegalConfigurationException;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.input.structured.StructuredPrompt;
import dev.langchain4j.model.input.structured.StructuredPromptProcessor;
import dev.langchain4j.model.moderation.Moderation;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.rag.AugmentationRequest;
import dev.langchain4j.rag.AugmentationResult;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.query.Metadata;
import dev.langchain4j.service.output.ServiceOutputParser;
import dev.langchain4j.service.tool.ToolExecution;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import dev.langchain4j.spi.services.TokenStreamAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static dev.langchain4j.exception.IllegalConfigurationException.illegalConfiguration;
import static dev.langchain4j.internal.Exceptions.illegalArgument;
import static dev.langchain4j.internal.Exceptions.runtime;
import static dev.langchain4j.internal.Utils.isNotNullOrBlank;
import static dev.langchain4j.internal.ValidationUtils.ensureNotEmpty;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;
import static dev.langchain4j.model.chat.Capability.RESPONSE_FORMAT_JSON_SCHEMA;
import static dev.langchain4j.model.chat.request.ResponseFormatType.JSON;
import static dev.langchain4j.service.TypeUtils.typeHasRawClass;
import static dev.langchain4j.service.output.JsonSchemas.jsonSchemaFrom;
import static dev.langchain4j.spi.ServiceHelper.loadFactories;

public class FunctionalInterfaceAiServices<T> extends AiServices<T> {

    private static final int MAX_SEQUENTIAL_TOOL_EXECUTIONS = 100;

    private static final Logger log = LoggerFactory.getLogger(FunctionalInterfaceAiServices.class);
    private static final Map<Class, AtomicInteger> threadExecutorCounter = new ConcurrentHashMap<>();

    private final ServiceOutputParser serviceOutputParser = new ServiceOutputParser();
    private final Collection<TokenStreamAdapter> tokenStreamAdapters = loadFactories(TokenStreamAdapter.class);
    private final String systemMessage;
    private final String userMessage;
    private final String knPromptText;
    private final Map<String, Object> variables;
    private final ChatStreamingResponseHandler responseHandler;
    private final List<Tools.ToolMethod> toolMethodList;
    private final boolean isSupportChineseToolName;
    private final Object memoryId;
    private final String modelName;

    public FunctionalInterfaceAiServices(AiServiceContext context, String systemMessage,
                                         String userMessage, String knPromptText,
                                         Map<String, Object> variables,
                                         ChatStreamingResponseHandler responseHandler,
                                         List<Tools.ToolMethod> toolMethodList,
                                         boolean isSupportChineseToolName,
                                         String modelName,
                                         Object memoryId) {
        super(context);
        this.memoryId = memoryId;
        this.modelName = modelName;
        this.isSupportChineseToolName = isSupportChineseToolName;
        this.toolMethodList = toolMethodList;
        Class<?> aiServiceClass = context.aiServiceClass;
        if (!aiServiceClass.isAnnotationPresent(FunctionalInterface.class)) {
            throw new UnsupportedOperationException("must FunctionalInterface");
        }
        this.responseHandler = responseHandler == null ? ChatStreamingResponseHandler.EMPTY : responseHandler;
        this.systemMessage = systemMessage;
        this.userMessage = userMessage;
        this.knPromptText = knPromptText;
        this.variables = variables;
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

    private static ThreadPoolExecutor getExecutor(Class aiServiceClass) {
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(
                1, 10,
                1, TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                runnable -> {
                    int tid = threadExecutorCounter.computeIfAbsent(aiServiceClass, e -> new AtomicInteger()).incrementAndGet();
                    Thread thread = new Thread(runnable, aiServiceClass.getSimpleName() + "-" + tid);
                    thread.setDaemon(true);
                    return thread;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

    public T build() {

        performBasicValidation();

        for (Method method : context.aiServiceClass.getMethods()) {
            if (method.isAnnotationPresent(Moderate.class) && context.moderationModel == null) {
                throw illegalConfiguration("The @Moderate annotation is present, but the moderationModel is not set up. " +
                        "Please ensure a valid moderationModel is configured before using the @Moderate annotation.");
            }
            if (method.getReturnType() == Result.class ||
                    method.getReturnType() == List.class ||
                    method.getReturnType() == Set.class) {
                TypeUtils.validateReturnTypesAreProperlyParametrized(method.getName(), method.getGenericReturnType());
            }
        }
        List<Class<?>> interfaces = new ArrayList<>(Arrays.asList(context.aiServiceClass.getInterfaces()));
        interfaces.add(context.aiServiceClass);
        Object proxyInstance = Proxy.newProxyInstance(
                context.aiServiceClass.getClassLoader(),
                interfaces.toArray(new Class[interfaces.size()]),
                new InvocationHandler() {

                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                        if (method.getDeclaringClass() == Object.class) {
                            // methods like equals(), hashCode() and toString() should not be handled by this proxy
                            return method.invoke(this, args);
                        }

                        // wangzihao 12-11， 支持默认方法
                        if (method.isDefault()) {
                            return ReflectUtil.invokeMethodHandle(true, proxy, method, args);
                        }

                        validateParameters(method);

                        Object memoryId = findMemoryId(method, args).orElse(FunctionalInterfaceAiServices.this.memoryId);

                        Optional<SystemMessage> systemMessage = prepareSystemMessage(memoryId, method, args);
                        UserMessage userMessage = prepareUserMessage(method, args);
                        AugmentationResult augmentationResult = null;
                        if (context.retrievalAugmentor != null) {
                            List<ChatMessage> chatMemory = context.hasChatMemory()
                                    ? context.chatMemory(memoryId).messages()
                                    : null;
                            Metadata metadata = Metadata.from(userMessage, memoryId, chatMemory);
                            AugmentationRequest augmentationRequest = new AugmentationRequest(userMessage, metadata);
                            augmentationResult = context.retrievalAugmentor.augment(augmentationRequest);
                            userMessage = (UserMessage) augmentationResult.chatMessage();
                        }

                        // TODO give user ability to provide custom OutputParser
                        Type returnType = method.getGenericReturnType();

                        boolean streaming = returnType == TokenStream.class || canAdaptTokenStreamTo(returnType);

                        boolean supportsJsonSchema = supportsJsonSchema();
                        Optional<JsonSchema> jsonSchema = Optional.empty();
                        if (supportsJsonSchema && !streaming) {
                            jsonSchema = jsonSchemaFrom(returnType);
                        }

                        if ((!supportsJsonSchema || !jsonSchema.isPresent()) && !streaming) {
                            // TODO append after storing in the memory?
                            userMessage = appendOutputFormatInstructions(returnType, userMessage);
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

                        Future<Moderation> moderationFuture = triggerModerationIfNeeded(method, messages);

                        List<ToolSpecification> toolSpecifications = context.toolSpecifications;
                        Map<String, ToolExecutor> toolExecutors = context.toolExecutors;

                        if (context.toolProvider != null) {
                            toolSpecifications = new ArrayList<>();
                            toolExecutors = new HashMap<>();
                            ToolProviderRequest toolProviderRequest = new ToolProviderRequest(memoryId, userMessage);
                            ToolProviderResult toolProviderResult = context.toolProvider.provideTools(toolProviderRequest);
                            if (toolProviderResult != null) {
                                Map<ToolSpecification, ToolExecutor> tools = toolProviderResult.tools();
                                for (ToolSpecification toolSpecification : tools.keySet()) {
                                    toolSpecifications.add(toolSpecification);
                                    toolExecutors.put(toolSpecification.name(), tools.get(toolSpecification));
                                }
                            }
                        }

                        if (streaming) {
                            TokenStream tokenStream = new AiServiceTokenStream(
                                    modelName,
                                    messages,
                                    augmentationResult != null ? augmentationResult.contents() : null,
                                    context,
                                    toolMethodList,
                                    isSupportChineseToolName,
                                    memoryId,
                                    userMessage,
                                    systemMessage.orElse(null),
                                    responseHandler
                            );
                            // TODO moderation
                            if (returnType == TokenStream.class) {
                                return tokenStream;
                            } else {
                                return adapt(tokenStream, returnType);
                            }
                        }

                        Response<AiMessage> response;
                        if (supportsJsonSchema && jsonSchema.isPresent()) {
                            ChatRequest chatRequest = ChatRequest.builder()
                                    .messages(messages)
                                    .toolSpecifications(toolSpecifications)
                                    .responseFormat(ResponseFormat.builder()
                                            .type(JSON)
                                            .jsonSchema(jsonSchema.get())
                                            .build())
                                    .build();
                            ChatResponse chatResponse = context.chatModel.chat(chatRequest);

                            response = new Response<>(
                                    chatResponse.aiMessage(),
                                    chatResponse.tokenUsage(),
                                    chatResponse.finishReason()
                            );
                        } else {
                            // TODO migrate to new API
                            response = toolSpecifications == null
                                    ? context.chatModel.generate(messages)
                                    : context.chatModel.generate(messages, toolSpecifications);
                        }
                        responseHandler.onJsonSchema(context.aiServiceClass, systemMessage.orElse(null), userMessage, response.content());

                        TokenUsage tokenUsageAccumulator = response.tokenUsage();

                        verifyModerationIfNeeded(moderationFuture);

                        int executionsLeft = MAX_SEQUENTIAL_TOOL_EXECUTIONS;
                        List<ToolExecution> toolExecutions = new ArrayList<>();
                        while (true) {

                            if (executionsLeft-- == 0) {
                                throw runtime("Something is wrong, exceeded %s sequential tool executions",
                                        MAX_SEQUENTIAL_TOOL_EXECUTIONS);
                            }

                            AiMessage aiMessage = response.content();

                            if (context.hasChatMemory()) {
                                context.chatMemory(memoryId).add(aiMessage);
                            } else {
                                messages = new ArrayList<>(messages);
                                messages.add(aiMessage);
                            }

                            if (!aiMessage.hasToolExecutionRequests()) {
                                break;
                            }

                            for (ToolExecutionRequest toolExecutionRequest : aiMessage.toolExecutionRequests()) {
                                ToolExecutor toolExecutor = toolExecutors.get(toolExecutionRequest.name());
                                String toolExecutionResult = toolExecutor.execute(toolExecutionRequest, memoryId);
                                toolExecutions.add(ToolExecution.builder()
                                        .request(toolExecutionRequest)
                                        .result(toolExecutionResult)
                                        .build());
                                ToolExecutionResultMessage toolExecutionResultMessage = ToolExecutionResultMessage.from(
                                        toolExecutionRequest,
                                        toolExecutionResult
                                );
                                if (context.hasChatMemory()) {
                                    context.chatMemory(memoryId).add(toolExecutionResultMessage);
                                } else {
                                    messages.add(toolExecutionResultMessage);
                                }
                            }

                            if (context.hasChatMemory()) {
                                messages = context.chatMemory(memoryId).messages();
                            }

                            response = context.chatModel.generate(messages, toolSpecifications);
                            tokenUsageAccumulator = TokenUsage.sum(tokenUsageAccumulator, response.tokenUsage());
                        }

                        response = Response.from(response.content(), tokenUsageAccumulator, response.finishReason());

                        Object parsedResponse = serviceOutputParser.parse(response, returnType);
                        if (typeHasRawClass(returnType, Result.class)) {
                            return Result.builder()
                                    .content(parsedResponse)
                                    .tokenUsage(tokenUsageAccumulator)
                                    .sources(augmentationResult == null ? null : augmentationResult.contents())
                                    .finishReason(response.finishReason())
                                    .toolExecutions(toolExecutions)
                                    .build();
                        } else {
                            return parsedResponse;
                        }
                    }

                    private boolean canAdaptTokenStreamTo(Type returnType) {
                        for (TokenStreamAdapter tokenStreamAdapter : tokenStreamAdapters) {
                            if (tokenStreamAdapter.canAdaptTokenStreamTo(returnType)) {
                                return true;
                            }
                        }
                        return false;
                    }

                    private Object adapt(TokenStream tokenStream, Type returnType) {
                        for (TokenStreamAdapter tokenStreamAdapter : tokenStreamAdapters) {
                            if (tokenStreamAdapter.canAdaptTokenStreamTo(returnType)) {
                                return tokenStreamAdapter.adapt(tokenStream);
                            }
                        }
                        throw new IllegalStateException("Can't find suitable TokenStreamAdapter");
                    }

                    private boolean supportsJsonSchema() {
                        return context.chatModel != null
                                && context.chatModel.supportedCapabilities().contains(RESPONSE_FORMAT_JSON_SCHEMA);
                    }

                    private UserMessage appendOutputFormatInstructions(Type returnType, UserMessage userMessage) {
                        String outputFormatInstructions = serviceOutputParser.outputFormatInstructions(returnType);
                        String text = userMessage.singleText() + outputFormatInstructions;
                        if (isNotNullOrBlank(userMessage.name())) {
                            userMessage = UserMessage.from(userMessage.name(), text);
                        } else {
                            userMessage = UserMessage.from(text);
                        }
                        return userMessage;
                    }

                    private Future<Moderation> triggerModerationIfNeeded(Method method, List<ChatMessage> messages) {
                        if (method.isAnnotationPresent(Moderate.class)) {
                            return getExecutor(context.aiServiceClass).submit(() -> {
                                List<ChatMessage> messagesToModerate = removeToolMessages(messages);
                                return context.moderationModel.moderate(messagesToModerate).content();
                            });
                        }
                        return null;
                    }
                });

        return (T) proxyInstance;
    }

    private Optional<SystemMessage> prepareSystemMessage(Object memoryId, Method method, Object[] args) {
        Optional<String> template = findSystemMessageTemplate(memoryId, method);
        try {
            return template.map(systemMessageTemplate -> PromptTemplate.from(systemMessageTemplate)
                    .apply(findTemplateVariables(systemMessageTemplate, method, args))
                    .toSystemMessage());
        } catch (Exception e) {
            throw new IllegalArgumentException("prepareSystemMessage " + context.aiServiceClass.getSimpleName() + " failed. template=" + template.orElse("") + ", cause=" + e.getMessage(), e);
        }
    }

    private Optional<String> findSystemMessageTemplate(Object memoryId, Method method) {
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

    private Map<String, Object> findTemplateVariables(String template, Method method, Object[] args) {
        Parameter[] parameters = method.getParameters();

        // wangzihao 12-09 增加支持内置变量 new HashMap<>(this.variables);
        Map<String, Object> variables = new HashMap<>(this.variables);
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

    private UserMessage prepareUserMessage(Method method, Object[] args) {
        String template = getUserMessageTemplate(method, args);
        Map<String, Object> variables = findTemplateVariables(template, method, args);
        Prompt prompt;
        try {
            prompt = PromptTemplate.from(template).apply(variables);
        } catch (Exception e) {
            throw new IllegalArgumentException("prepareSystemMessage " + context.aiServiceClass.getSimpleName() + " failed. template=" + template + ", cause=" + e.getMessage(), e);
        }

        Optional<String> maybeUserName = findUserName(method.getParameters(), args);
        return maybeUserName.map(userName -> UserMessage.from(userName, prompt.text()))
                .orElseGet(prompt::toUserMessage);
    }

    private String getUserMessageTemplate(Method method, Object[] args) {
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

    public static class AiServiceTokenStream implements TokenStream {

        private final List<ChatMessage> messages;
        private final List<Content> retrievedContents;
        private final AiServiceContext context;
        private final List<Tools.ToolMethod> toolMethodList;

        private final ChatStreamingResponseHandler responseHandler;
        private final UserMessage userMessage;
        private final SystemMessage systemMessage;
        private final boolean isSupportChineseToolName;
        private final Object memoryId;
        private final String modelName;
        private Consumer<String> tokenHandler;
        private Consumer<List<Content>> contentsHandler;
        private Consumer<Throwable> errorHandler;
        private Consumer<Response<AiMessage>> completionHandler;
        private int onNextInvoked;
        private int onCompleteInvoked;
        private int onRetrievedInvoked;
        private int onErrorInvoked;
        private int ignoreErrorsInvoked;

        public AiServiceTokenStream(String modelName, List<ChatMessage> messages,
                                    List<Content> retrievedContents,
                                    AiServiceContext context,
                                    List<Tools.ToolMethod> toolMethodList,
                                    boolean isSupportChineseToolName,
                                    Object memoryId,
                                    UserMessage userMessage,
                                    SystemMessage systemMessage,
                                    ChatStreamingResponseHandler responseHandler) {
            this.modelName = modelName;
            this.memoryId = memoryId;
            this.isSupportChineseToolName = isSupportChineseToolName;
            this.toolMethodList = toolMethodList;
            this.messages = ensureNotEmpty(messages, "messages");
            this.retrievedContents = retrievedContents;
            this.context = ensureNotNull(context, "context");
            ensureNotNull(context.streamingChatModel, "streamingChatModel");
            this.responseHandler = responseHandler;
            this.userMessage = userMessage;
            this.systemMessage = systemMessage;
        }

        @Override
        public TokenStream onNext(Consumer<String> tokenHandler) {
            this.tokenHandler = tokenHandler;
            this.onNextInvoked++;
            return this;
        }

        @Override
        public TokenStream onRetrieved(Consumer<List<Content>> contentsHandler) {
            this.contentsHandler = contentsHandler;
            this.onRetrievedInvoked++;
            return this;
        }

        @Override
        public TokenStream onComplete(Consumer<Response<AiMessage>> completionHandler) {
            this.completionHandler = completionHandler;
            this.onCompleteInvoked++;
            return this;
        }

        @Override
        public TokenStream onError(Consumer<Throwable> errorHandler) {
            this.errorHandler = errorHandler;
            this.onErrorInvoked++;
            return this;
        }

        @Override
        public TokenStream ignoreErrors() {
            this.errorHandler = null;
            this.ignoreErrorsInvoked++;
            return this;
        }

        @Override
        public void start() {
            validateConfiguration();

            ChatStreamingResponseHandler csrh = new ChatStreamingResponseHandler() {
                @Override
                public void onError(Throwable error, int baseMessageIndex, int addMessageCount, int generateCount) {
                    if (errorHandler != null) {
                        errorHandler.accept(error);
                    }
                }

                @Override
                public void onToolCalls(Response<AiMessage> response) {
                    try {
                        responseHandler.onJsonSchemaToolCalls(response);
                    } catch (Exception e) {
                        log.warn("JsonSchema onJsonSchemaToolCalls error {}", e.toString(), e);
                    }
                }

                @Override
                public void beforeUrlRead(String sourceEnum, String providerName, String question, UrlReadTools urlReadTools, WebSearchResultVO.Row row) {
                    try {
                        responseHandler.beforeUrlRead(sourceEnum, providerName, question, urlReadTools, row);
                    } catch (Exception e) {
                        log.warn("JsonSchema beforeUrlRead error {}", e.toString(), e);
                    }
                }

                @Override
                public void beforeWebSearch(String sourceEnum, String providerName, String question) {
                    try {
                        responseHandler.beforeWebSearch(sourceEnum, providerName, question);
                    } catch (Exception e) {
                        log.warn("JsonSchema beforeWebSearch error {}", e.toString(), e);
                    }
                }

                @Override
                public void afterUrlRead(String sourceEnum, String providerName, String question, UrlReadTools urlReadTools, WebSearchResultVO.Row row, String content, String merge, long cost) {
                    try {
                        responseHandler.afterUrlRead(sourceEnum, providerName, question, urlReadTools, row, content, merge, cost);
                    } catch (Exception e) {
                        log.warn("JsonSchema afterUrlRead error {}", e.toString(), e);
                    }
                }

                @Override
                public void afterWebSearch(String sourceEnum, String providerName, String question, WebSearchResultVO resultVO, long cost) {
                    try {
                        responseHandler.afterWebSearch(sourceEnum, providerName, question, resultVO, cost);
                    } catch (Exception e) {
                        log.warn("JsonSchema afterWebSearch error {}", e.toString(), e);
                    }
                }

                @Override
                public void onComplete(Response<AiMessage> response, int baseMessageIndex, int addMessageCount, int generateCount) {
                    try {
                        responseHandler.onJsonSchema(context.aiServiceClass, systemMessage, userMessage, response.content());
                    } catch (Exception e) {
                        log.warn("JsonSchema onJsonSchema error {}", e.toString(), e);
                    }
                    if (completionHandler != null) {
                        completionHandler.accept(response);
                    }
                }

                @Override
                public void onToken(String token, int baseMessageIndex, int addMessageCount) {
                    if (tokenHandler != null) {
                        tokenHandler.accept(token);
                    }
                }
            };
            ThreadPoolExecutor executor = getExecutor(context.aiServiceClass);
            ChatMemory chatMemory = MessageWindowChatMemory.builder().id(memoryId).maxMessages(Integer.MAX_VALUE).build();
            messages.forEach(chatMemory::add);
            JsonschemaToolCallStreamingResponseHandler handler = new JsonschemaToolCallStreamingResponseHandler(
                    modelName,
                    context.streamingChatModel,
                    chatMemory,
                    csrh,
                    null,
                    toolMethodList,
                    isSupportChineseToolName,
                    0, 0, null, executor);

            if (contentsHandler != null && retrievedContents != null) {
                contentsHandler.accept(retrievedContents);
            }
            handler.generate();
        }

        private void validateConfiguration() {
            if (onNextInvoked != 1) {
                throw new IllegalConfigurationException("onNext must be invoked exactly 1 time");
            }

            if (onCompleteInvoked > 1) {
                throw new IllegalConfigurationException("onComplete must be invoked at most 1 time");
            }

            if (onRetrievedInvoked > 1) {
                throw new IllegalConfigurationException("onRetrieved must be invoked at most 1 time");
            }

            if (onErrorInvoked + ignoreErrorsInvoked != 1) {
                throw new IllegalConfigurationException("One of onError or ignoreErrors must be invoked exactly 1 time");
            }
        }
    }

}
