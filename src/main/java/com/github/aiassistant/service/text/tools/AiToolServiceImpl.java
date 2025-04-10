package com.github.aiassistant.service.text.tools;

import com.github.aiassistant.dao.AiToolMapper;
import com.github.aiassistant.dao.AiToolParameterMapper;
import com.github.aiassistant.entity.AiTool;
import com.github.aiassistant.entity.AiToolParameter;
import com.github.aiassistant.util.ParameterNamesUtil;
import com.github.aiassistant.util.StringUtils;
import dev.langchain4j.agent.tool.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 工具库服务
 */
public class AiToolServiceImpl {
    private static final Logger log = LoggerFactory.getLogger(AiToolServiceImpl.class);
    // @Resource
    private final AiToolMapper aiToolMapper;
    // @Resource
    private final AiToolParameterMapper aiToolParameterMapper;
    // @Autowired
    private final Function<String, Tools> toolsMap;

    public AiToolServiceImpl(AiToolMapper aiToolMapper, AiToolParameterMapper aiToolParameterMapper, Function<String, Tools> toolsMap) {
        this.aiToolMapper = aiToolMapper;
        this.aiToolParameterMapper = aiToolParameterMapper;
        this.toolsMap = toolsMap;
    }

    private static Method getToolFunctionMethod(Class<?> clazz, String toolFunctionName) {
        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Method method : c.getDeclaredMethods()) {
                if (!method.isAnnotationPresent(Tool.class)) {
                    continue;
                }
                if (method.getName().equalsIgnoreCase(toolFunctionName)) {
                    return method;
                }
            }
        }
        return null;
    }

    public Tools getTools(String toolName) {
        return toolsMap.apply(toolName);
    }

    public List<Tools.ToolMethod> selectToolMethodList(Collection<? extends Serializable> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Tools.ToolMethod> result = new HashMap<>();
        Map<Integer, Map<String, AiToolParameter>> parameters = new LinkedHashMap<>();
        List<AiTool> toolList = selectToolList(ids, parameters);
        for (AiTool aiTool : toolList) {
            String toolEnum = aiTool.getToolEnum();
            Tools tool = getTools(toolEnum);
            if (tool == null) {
                continue;
            }
            tool.setBeanName(toolEnum);
            String toolFunctionName = aiTool.getToolFunctionName();
            String toolFunctionEnum = aiTool.getToolFunctionEnum();
            String englishName = toolEnum + "-" + toolFunctionEnum + "_" + aiTool.getId();
            Method functionMethod = getToolFunctionMethod(tool.getClass(), toolFunctionEnum);
            if (functionMethod == null) {
                log.warn("tool {} name {} not exist!", toolEnum, aiTool.getToolFunctionEnum());
                continue;
            }
            String[] parameterNames = ParameterNamesUtil.getParameterNames(functionMethod, e -> e.isAnnotationPresent(P.class));
            Map<String, AiToolParameter> parameterMap = parameters.get(aiTool.getId());
            Map<String, String> parameterDefaultValueMap = new HashMap<>();
            if (parameterMap != null) {
                parameterMap.forEach((k, v) -> parameterDefaultValueMap.put(k, v.getDefaultValue()));
            }

            ToolParameters toolParameters = convert(parameterMap, functionMethod, parameterNames);
            ToolSpecification rewriteToolSpecification = ToolSpecification.builder()
                    .name(toolFunctionName)
                    .description(aiTool.getToolFunctionDescription())
                    .parameters(toolParameters)
                    .build();
            Tools.ToolMethod toolMethod = new Tools.ToolMethod(tool, rewriteToolSpecification, englishName, parameterNames, functionMethod, parameterDefaultValueMap);
            if (result.put(toolMethod.name(), toolMethod) != null) {
                log.warn("tool name {} exist!", toolMethod.name());
            }
        }
        return new ArrayList<>(result.values());
    }

    private List<AiTool> selectToolList(Collection<? extends Serializable> ids, Map<Integer, Map<String, AiToolParameter>> parameterMap) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        List<AiTool> toolList = aiToolMapper.selectBatchIds(ids);
        if (!toolList.isEmpty()) {
            List<Integer> toolIdList = toolList.stream().map(AiTool::getId).collect(Collectors.toList());
            for (AiToolParameter parameter : aiToolParameterMapper.selectListByToolId(toolIdList)) {
                parameterMap.computeIfAbsent(parameter.getAiToolId(), e -> new LinkedHashMap<>())
                        .put(parameter.getParameterEnum(), parameter);
            }
        }
        return toolList;
    }

    private ToolParameters convert(Map<String, AiToolParameter> parameterMap, Method method, String[] parameterNames) {
        if (parameterMap == null || parameterMap.isEmpty()) {
            return null;
        }
        Set<String> disabledSet = parameterMap.values().stream().filter(e -> !Boolean.TRUE.equals(e.getEnableFlag())).map(AiToolParameter::getParameterEnum).collect(Collectors.toSet());
        Set<String> requiredSet = parameterMap.values().stream().filter(e -> Boolean.TRUE.equals(e.getRequiredFlag())).map(AiToolParameter::getParameterEnum).collect(Collectors.toSet());
        Map<String, String> parameterDescriptions = parameterMap.values().stream().filter(e -> StringUtils.hasText(e.getParameterDescription())).collect(Collectors.toMap(AiToolParameter::getParameterEnum, AiToolParameter::getParameterDescription));
        return ToolSpecificationsUtil.toolParameters(method, requiredSet, disabledSet, parameterNames, parameterDescriptions);
    }
}
