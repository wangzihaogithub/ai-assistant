package com.github.aiassistant.service.text.tools;

import com.github.aiassistant.platform.JsonUtil;
import com.github.aiassistant.service.text.sseemitter.SseHttpResponse;
import com.github.aiassistant.util.AiUtil;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolMemoryId;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 实现摘抄自：dev.langchain4j.service.tool.DefaultToolExecutor
 */
public class ResultToolExecutor extends CompletableFuture<ToolExecutionResultMessage> {
    private static final Logger log = LoggerFactory.getLogger(ResultToolExecutor.class);
    private static final Pattern TRAILING_COMMA_PATTERN = Pattern.compile(",(\\s*[}\\]])");
    private final Object instance;
    private final Method method;
    private final ToolExecutionRequest request;
    private final ParamValid paramValid;
    private final Object[] paramValidArguments;
    private final Object[] methodArguments;
    private final Object memoryId;
    private final SseHttpResponse emitter;

    public ResultToolExecutor(Object instance, Method method, String[] paramNames, Map<String, String> parameterDefaultValueMap, ToolExecutionRequest toolExecutionRequest, SseHttpResponse emitter, Object memoryId) {
        this.instance = instance;
        Objects.requireNonNull(toolExecutionRequest, "toolExecutionRequest");
        this.method = method;
        this.request = toolExecutionRequest;
        this.emitter = emitter;
        this.paramValid = method == null ? null : method.getDeclaredAnnotation(ParamValid.class);
        String argumentsString = toolExecutionRequest.arguments();
        JsonUtil.ObjectReader objectReader = JsonUtil.objectReader();
        JsonUtil.ObjectWriter objectWriter = JsonUtil.objectWriter();
        Map<String, Object> argumentsMap = null;
        if (method != null) {
            try {
                argumentsMap = argumentsAsMap(objectReader, paramNames, argumentsString);
            } catch (Exception e) {
                argumentsMap = retryArgumentsAsMap(objectReader, argumentsString, paramNames, e);
                if (argumentsMap == null) {
                    log.error("argumentsAsMap argumentsString {}, fail {}", argumentsString, e.toString(), e);
                    throw new IllegalStateException("argumentsAsMap error '" + argumentsString + "'. cause:" + e, e);
                }
            }
        }
        this.memoryId = memoryId;
        try {
            this.methodArguments = method == null ? null : prepareArguments(objectReader, objectWriter, method, paramNames, argumentsMap, memoryId, parameterDefaultValueMap, ToolInvokeEnum.METHOD);
            this.paramValidArguments = paramValid == null ? null : prepareArguments(objectReader, objectWriter, method, paramNames, argumentsMap, memoryId, parameterDefaultValueMap, ToolInvokeEnum.PARAM_VALID);
        } catch (Exception e) {
            log.error("prepareArguments fail, argumentsMap {}, argumentsString {}, cause={}", argumentsMap, argumentsString, e.toString(), e);
            throw new IllegalStateException("prepareArguments fail. argumentsMap=" + argumentsMap + ", argumentsString=" + argumentsString + ". cause=" + e, e);
        }
    }

    public static boolean isEmitterMethod(Method method) {
        for (Parameter parameter : method.getParameters()) {
            if (SseHttpResponse.class.isAssignableFrom(parameter.getType())) {
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
    static String removeTrailingComma(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }
        Matcher matcher = TRAILING_COMMA_PATTERN.matcher(json);
        return matcher.replaceAll("$1");
    }

    private static double getDoubleValue(
            Object argument,
            String parameterName,
            Class<?> parameterType
    ) {
        if (argument instanceof String) {
            try {
                return Double.parseDouble(argument.toString());
            } catch (Exception e) {
                // nothing, will be handled with bellow code
            }
        }
        if (!(argument instanceof Number)) {
            throw new IllegalArgumentException(String.format(
                    "Argument \"%s\" is not convertable to %s, got %s: <%s>",
                    parameterName, parameterType.getName(), argument.getClass().getName(), argument));
        }
        return ((Number) argument).doubleValue();
    }

    private static double getNonFractionalDoubleValue(
            Object argument,
            String parameterName,
            Class<?> parameterType
    ) {
        double doubleValue = getDoubleValue(argument, parameterName, parameterType);
        if (!hasNoFractionalPart(doubleValue)) {
            throw new IllegalArgumentException(String.format(
                    "Argument \"%s\" has non-integer value for %s: <%s>",
                    parameterName, parameterType.getName(), argument));
        }
        return doubleValue;
    }

    private static void checkBounds(
            double doubleValue,
            String parameterName,
            Class<?> parameterType,
            double minValue,
            double maxValue
    ) {
        if (doubleValue < minValue || doubleValue > maxValue) {
            throw new IllegalArgumentException(String.format(
                    "Argument \"%s\" is out of range for %s: <%s>",
                    parameterName, parameterType.getName(), doubleValue));
        }
    }

    private static long getBoundedLongValue(
            Object argument,
            String parameterName,
            Class<?> parameterType,
            long minValue,
            long maxValue
    ) {
        double doubleValue = getNonFractionalDoubleValue(argument, parameterName, parameterType);
        checkBounds(doubleValue, parameterName, parameterType, minValue, maxValue);
        return (long) doubleValue;
    }

    static boolean hasNoFractionalPart(Double doubleValue) {
        return doubleValue.equals(Math.floor(doubleValue));
    }

    private static <T> void await(Object invokeResult, CompletableFuture<T> future, Function<Object, T> map) {
        // ToolExecutionResultMessage 功能扩展 wangzihao
        if (invokeResult instanceof CompletableFuture) {
            CompletableFuture<ToolExecutionResultMessage> f = (CompletableFuture) invokeResult;
            f.whenComplete((rst, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(throwable);
                } else {
                    await(rst, future, map);
                }
            });
        } else {
            future.complete(map.apply(invokeResult));
        }
    }

    private static ToolExecutionResultMessage convertResultMessage(ToolExecutionRequest toolExecutionRequest, Object invokeResult) {
        ToolExecutionResultMessage result;
        if (invokeResult instanceof ToolExecutionResultMessage) {
            result = (ToolExecutionResultMessage) invokeResult;
        } else if (invokeResult == null) {
            result = ToolExecutionResultMessage.from(toolExecutionRequest, "Success");
        } else if (invokeResult instanceof String) {
            result = ToolExecutionResultMessage.from(toolExecutionRequest, (String) invokeResult);
        } else {
            result = ToolExecutionResultMessage.from(toolExecutionRequest, invokeResult.toString());
        }
        return result;
    }

    private static Map<String, Object> argumentsAsMap(JsonUtil.ObjectReader objectReader, String[] paramNames, String arguments) throws IOException {
        String s = removeTrailingComma(arguments);
        if (s == null || s.isEmpty()) {
            return new HashMap<>();
        }
        s = AiUtil.removeJsonMarkdown(s);
        Map map = objectReader.readValue(s, Map.class);
        return extractParamsNames(paramNames, map);
    }

    /**
     * 解决AI返回的json里，把参数又包了一层。
     */
    private static Map<String, Object> extractParamsNames(String[] paramNames, Map map) {
        for (String paramName : paramNames) {
            if (map.containsKey(paramName)) {
                return map;
            }
        }
        if (map.size() == 1) {
            Object next = map.values().iterator().next();
            if (next instanceof Map) {
                return (Map<String, Object>) next;
            }
        }
        return map;
    }

    private static Map<String, Object> retryArgumentsAsMap(JsonUtil.ObjectReader objectReader, String argumentsString, String[] paramNames, Exception e) {
        String errorClassName = e.getClass().getSimpleName();
        if (!errorClassName.toLowerCase().contains("json")) {
            return null;
        }
        String[] endWiths = {"\"}", "\"]}", "}"};
        for (String end : endWiths) {
            try {
                return argumentsAsMap(objectReader, paramNames, argumentsString + end);
            } catch (Exception ignored) {
            }
        }
        String[] endWithsPlus = {"}"};
        for (String end : endWiths) {
            for (String withsPlus : endWithsPlus) {
                try {
                    return argumentsAsMap(objectReader, paramNames, argumentsString + end + withsPlus);
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        if (instance == null || method == null) {
            return request.toString();
        }
        return String.format("%s.%s(%s)", instance.getClass().getSimpleName(), method.getName(), request.arguments());
    }

    public ToolExecutionRequest getRequest() {
        return request;
    }

    Object[] prepareArguments(
            JsonUtil.ObjectReader objectReader,
            JsonUtil.ObjectWriter objectWriter,
            Method method,
            String[] paramNames,
            Map<String, Object> argumentsMap,
            Object memoryId,
            Map<String, String> parameterDefaultValueMap,
            ToolInvokeEnum toolInvokeEnum
    ) throws IOException {
        Parameter[] parameters = method.getParameters();
        Object[] arguments = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {

            if (parameters[i].isAnnotationPresent(ToolMemoryId.class)) {
                Class<?> parametersType = parameters[i].getType();
                if (parametersType == ToolInvokeEnum.class) {
                    arguments[i] = toolInvokeEnum;
                } else if (parametersType.isAssignableFrom(ToolExecutionRequest.class)) {
                    arguments[i] = this.request;
                } else if (SseHttpResponse.class.isAssignableFrom(parametersType)) {
                    arguments[i] = emitter;
                } else {
                    arguments[i] = memoryId;
                }
                continue;
            }

            String parameterName = paramNames != null && paramNames.length > 0 ? paramNames[i] : parameters[i].getName();
            Class<?> parameterType = parameters[i].getType();
            if (argumentsMap.containsKey(parameterName)) {
                Object argument = argumentsMap.get(parameterName);

                // 空指针， 功能扩展 wangzihao
                arguments[i] = argument == null ? null : coerceArgument(objectReader, objectWriter, argument, parameterName, parameterType);
            } else if (parameterDefaultValueMap != null) {
                String argument = parameterDefaultValueMap.get(parameterName);
                arguments[i] = argument == null || argument.isEmpty() ? null : coerceArgument(objectReader, objectWriter, argument, parameterName, parameterType);
            }
        }
        return arguments;
    }

    Object coerceArgument(
            JsonUtil.ObjectReader objectReader,
            JsonUtil.ObjectWriter objectWriter,
            Object argument,
            String parameterName,
            Class<?> parameterType
    ) throws IOException {
        if (parameterType == String.class) {
            return argument.toString();
        }
        if (parameterType.isEnum()) {
            try {
                @SuppressWarnings({"unchecked", "rawtypes"})
                Class<Enum> enumClass = (Class<Enum>) parameterType;
                return Enum.valueOf(enumClass, Objects.requireNonNull(argument.toString()));
            } catch (Exception | Error e) {
                throw new IllegalArgumentException(String.format(
                        "Argument \"%s\" is not a valid enum value for %s: <%s>",
                        parameterName, parameterType.getName(), argument), e);
            }
        }

        if (parameterType == Boolean.class || parameterType == boolean.class) {
            if (argument instanceof Boolean) {
                return argument;
            }
            throw new IllegalArgumentException(String.format(
                    "Argument \"%s\" is not convertable to %s, got %s: <%s>",
                    parameterName, parameterType.getName(), argument.getClass().getName(), argument));
        }

        if (parameterType == Double.class || parameterType == double.class) {
            return getDoubleValue(argument, parameterName, parameterType);
        }

        if (parameterType == Float.class || parameterType == float.class) {
            double doubleValue = getDoubleValue(argument, parameterName, parameterType);
            checkBounds(doubleValue, parameterName, parameterType, -Float.MIN_VALUE, Float.MAX_VALUE);
            return (float) doubleValue;
        }

        if (parameterType == BigDecimal.class) {
            return BigDecimal.valueOf(getDoubleValue(argument, parameterName, parameterType));
        }

        if (parameterType == Integer.class || parameterType == int.class) {
            return (int) getBoundedLongValue(
                    argument, parameterName, parameterType, Integer.MIN_VALUE, Integer.MAX_VALUE);
        }

        if (parameterType == Long.class || parameterType == long.class) {
            return getBoundedLongValue(
                    argument, parameterName, parameterType, Long.MIN_VALUE, Long.MAX_VALUE);
        }

        if (parameterType == Short.class || parameterType == short.class) {
            return (short) getBoundedLongValue(
                    argument, parameterName, parameterType, Short.MIN_VALUE, Short.MAX_VALUE);
        }

        if (parameterType == Byte.class || parameterType == byte.class) {
            return (byte) getBoundedLongValue(
                    argument, parameterName, parameterType, Byte.MIN_VALUE, Byte.MAX_VALUE);
        }

        if (parameterType == BigInteger.class) {
            return BigDecimal.valueOf(
                    getNonFractionalDoubleValue(argument, parameterName, parameterType)).toBigInteger();
        }

        if (parameterType.isArray() && argument instanceof Collection) {
            Class<?> type = parameterType.getComponentType();
            if (type == String.class) {
                return ((Collection<String>) argument).toArray(new String[0]);
            }
            // TODO: Consider full type coverage.
        }

        String string = objectWriter.writeValueAsString(argument);
        return objectReader.readValue(string, parameterType);
    }

    public CompletableFuture<ToolExecutionResultMessage> execute() {
        ToolExecutionRequest toolExecutionRequest = this.request;
        if (log.isDebugEnabled()) {
            log.debug("About to execute {} for memoryId {}", toolExecutionRequest, memoryId);
        }
        CompletableFuture<ToolExecutionResultMessage> result = new CompletableFuture<>();
        try {
            Object invokeResult = invoke(methodArguments);
            await(invokeResult, result, e -> convertResultMessage(toolExecutionRequest, e));
            if (log.isDebugEnabled()) {
                log.debug("Tool execution result: {}", invokeResult);
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Error while executing tool {} {}", method, e.toString(), e);
            }
            result.completeExceptionally(e);
        }
        result.whenComplete((toolExecutionResultMessage, throwable) -> {
            if (throwable != null) {
                completeExceptionally(throwable);
            } else {
                complete(toolExecutionResultMessage);
            }
        });
        return this;
    }

    public CompletableFuture<Object> validation() {
        if (paramValidArguments == null) {
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<Object> result = new CompletableFuture<>();
        try {
            Object invokeResult = invoke(paramValidArguments);
            await(invokeResult, result, Function.identity());
        } catch (Exception e) {
            result.completeExceptionally(e);
        }
        return result;
    }

    private Object invoke(Object[] arguments) throws InvocationTargetException, IllegalAccessException {
        if (method == null) {
            return "Success";
        }
        return method.invoke(instance, arguments);
    }

    public Method method() {
        return method;
    }

    public Object instance() {
        return instance;
    }

    public boolean isEmitter() {
        return emitter != null;
    }

    @Retention(RUNTIME)
    @Target({METHOD})
    public static @interface ParamValid {

    }

}
