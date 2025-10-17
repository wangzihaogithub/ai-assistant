package com.github.aiassistant.service.text;

import com.github.aiassistant.util.AiUtil;
import dev.ai4j.openai4j.chat.Audio;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.request.json.JsonSchema;

import java.util.List;
import java.util.Map;

/**
 * 供应商支持的接口参数
 */
public class GenerateRequest implements Cloneable {
    /**
     * 由历史对话组成的消息列表
     * (数据由FunctionCallStreamingResponseHandler自动维护)
     */
    List<ChatMessage> messageList;
    /**
     * 可供模型调用的工具数组，可以包含一个或多个工具对象。一次Function Calling流程模型会从中选择一个工具。
     * 目前不支持通义千问VL/Audio，也不建议用于数学和代码模型。
     * (数据由FunctionCallStreamingResponseHandler自动维护)
     */
    List<ToolSpecification> toolSpecificationList;
    /**
     * 默认情况下，模型将决定何时以及使用多少工具。您可以使用tool_choice参数强制特定行为。
     * tool_choice string 或 object （可选）默认值为 "auto"
     * 如果您希望对于某一类问题，大模型能够采取制定好的工具选择策略（如强制使用某个工具、强制使用至少一个工具、强制不使用工具等），可以通过修改tool_choice参数来强制指定工具调用的策略。可选值：
     * "auto"
     * 表示由大模型进行工具策略的选择。
     * "none"
     * 如果您希望无论输入什么问题，Function Calling 都不会进行工具调用，可以设定tool_choice参数为"none"；
     * {"type": "function", "function": {"name": "the_function_to_call"}}
     * 如果您希望对于某一类问题，Function Calling 能够强制调用某个工具，可以设定tool_choice参数为{"type": "function", "function": {"name": "the_function_to_call"}}，其中the_function_to_call是您指定的工具函数名称。
     */
    private Boolean toolChoiceRequired;
    /**
     * 开启联网搜索的参数（供应商提供的实现）
     */
    private Boolean enableSearch;
    /**
     * 开启联网搜索的参数
     * search_options = {
     * "forced_search": True, # 强制开启联网搜索
     * "enable_source": True, # 使返回结果包含搜索来源的信息，OpenAI 兼容方式暂不支持返回
     * "enable_citation": True, # 开启角标标注功能
     * "citation_format":  # 角标形式为[ref_i]
     * "search_strategy": "pro" # 模型将搜索10条互联网信息
     * },
     */
    private Map<String, Object> searchOptions;
    /**
     * # 是否开启思考模式（供应商提供的实现）
     * 适用于 Qwen3 模型。
     * Qwen3 商业版模型默认值为 False，Qwen3 开源版模型默认值为 True。
     */
    private Boolean enableThinking;
    /**
     * 参数设置最大推理过程 Token 数，不能超过供应商要求的最大思维链Token数：例：38912。两个参数对 QwQ 与 DeepSeek-R1 模型无效
     */
    private Integer thinkingBudget;
    /**
     * （可选）默认值为["text"]
     * 输出数据的模态，仅支持 Qwen-Omni 模型指定。可选值：
     * ["text","audio"]：输出文本与音频；
     * ["text"]：输出文本。
     */
    private List<String> modalities;
    /**
     * 输出音频的音色与文件格式（只支持设定为"wav"）通过audio参数来配置，
     * 如：audio={"voice": "Cherry", "format": "wav"}，
     * 其中商业版模型voice参数可选值为：["Cherry", "Serena", "Ethan", "Chelsie"]，
     * 开源版模型voice参数可选值为：["Ethan", "Chelsie"]。
     */
    private Audio audio;

    /**
     * JsonSchema
     * response_format 参数的新选项
     * 开发者现在可以通过 response_format 参数的新选项 json_schema 提供 JSON 架构。
     * 当模型不调用工具时，但以结构化方式响应用户时，这非常有用。
     * 此功能适用于我们最新的 GPT-4o 模型：今天发布的 gpt-4o-2024-08-06 和 gpt-4o-mini-2024-07-18。
     * 当提供了 response_format 并设置 strict: true 时，模型输出将匹配提供的架构。
     */
    private JsonSchema jsonSchema;

    /**
     * 使用场景和效果
     * ‌并行调用‌：在需要同时执行多个独立任务时，使用并行调用可以提高效率。例如，在处理多个天气查询时，可以并行调用获取不同城市的天气信息。
     * ‌顺序调用‌：在某些情况下，可能需要按顺序执行任务，以确保前一个任务的输出是后一个任务的输入。例如，在处理复杂的逻辑链时，每个步骤都需要前一步的输出结果。
     */
    private Boolean parallelToolCalls;
    private Double temperature;

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    /**
     * 使用场景和效果
     * ‌并行调用‌：在需要同时执行多个独立任务时，使用并行调用可以提高效率。例如，在处理多个天气查询时，可以并行调用获取不同城市的天气信息。
     * ‌顺序调用‌：在某些情况下，可能需要按顺序执行任务，以确保前一个任务的输出是后一个任务的输入。例如，在处理复杂的逻辑链时，每个步骤都需要前一步的输出结果。
     *
     * @return * 使用场景和效果
     * * ‌并行调用‌：在需要同时执行多个独立任务时，使用并行调用可以提高效率。例如，在处理多个天气查询时，可以并行调用获取不同城市的天气信息。
     * * ‌顺序调用‌：在某些情况下，可能需要按顺序执行任务，以确保前一个任务的输出是后一个任务的输入。例如，在处理复杂的逻辑链时，每个步骤都需要前一步的输出结果。
     * *
     */
    public Boolean getParallelToolCalls() {
        return parallelToolCalls;
    }

    /**
     * 使用场景和效果
     * ‌并行调用‌：在需要同时执行多个独立任务时，使用并行调用可以提高效率。例如，在处理多个天气查询时，可以并行调用获取不同城市的天气信息。
     * ‌顺序调用‌：在某些情况下，可能需要按顺序执行任务，以确保前一个任务的输出是后一个任务的输入。例如，在处理复杂的逻辑链时，每个步骤都需要前一步的输出结果。
     *
     * @param parallelToolCalls * 使用场景和效果
     *                          * ‌并行调用‌：在需要同时执行多个独立任务时，使用并行调用可以提高效率。例如，在处理多个天气查询时，可以并行调用获取不同城市的天气信息。
     *                          * ‌顺序调用‌：在某些情况下，可能需要按顺序执行任务，以确保前一个任务的输出是后一个任务的输入。例如，在处理复杂的逻辑链时，每个步骤都需要前一步的输出结果。
     *                          *
     */
    public void setParallelToolCalls(Boolean parallelToolCalls) {
        this.parallelToolCalls = parallelToolCalls;
    }

    /**
     * JsonSchema
     * response_format 参数的新选项
     * 开发者现在可以通过 response_format 参数的新选项 json_schema 提供 JSON 架构。
     * 当模型不调用工具时，但以结构化方式响应用户时，这非常有用。
     * 此功能适用于我们最新的 GPT-4o 模型：今天发布的 gpt-4o-2024-08-06 和 gpt-4o-mini-2024-07-18。
     * 当提供了 response_format 并设置 strict: true 时，模型输出将匹配提供的架构。
     *
     * @return * JsonSchema
     * * response_format 参数的新选项
     * * 开发者现在可以通过 response_format 参数的新选项 json_schema 提供 JSON 架构。
     * * 当模型不调用工具时，但以结构化方式响应用户时，这非常有用。
     * * 此功能适用于我们最新的 GPT-4o 模型：今天发布的 gpt-4o-2024-08-06 和 gpt-4o-mini-2024-07-18。
     * * 当提供了 response_format 并设置 strict: true 时，模型输出将匹配提供的架构。
     */
    public JsonSchema getJsonSchema() {
        return jsonSchema;
    }

    /**
     * JsonSchema
     * response_format 参数的新选项
     * 开发者现在可以通过 response_format 参数的新选项 json_schema 提供 JSON 架构。
     * 当模型不调用工具时，但以结构化方式响应用户时，这非常有用。
     * 此功能适用于我们最新的 GPT-4o 模型：今天发布的 gpt-4o-2024-08-06 和 gpt-4o-mini-2024-07-18。
     * 当提供了 response_format 并设置 strict: true 时，模型输出将匹配提供的架构。
     *
     * @param jsonSchema * JsonSchema
     *                   * response_format 参数的新选项
     *                   * 开发者现在可以通过 response_format 参数的新选项 json_schema 提供 JSON 架构。
     *                   * 当模型不调用工具时，但以结构化方式响应用户时，这非常有用。
     *                   * 此功能适用于我们最新的 GPT-4o 模型：今天发布的 gpt-4o-2024-08-06 和 gpt-4o-mini-2024-07-18。
     *                   * 当提供了 response_format 并设置 strict: true 时，模型输出将匹配提供的架构。
     */
    public void setJsonSchema(JsonSchema jsonSchema) {
        this.jsonSchema = jsonSchema;
    }

    /**
     * 默认情况下，模型将决定何时以及使用多少工具。您可以使用tool_choice参数强制特定行为。
     * tool_choice string 或 object （可选）默认值为 "auto"
     * 如果您希望对于某一类问题，大模型能够采取制定好的工具选择策略（如强制使用某个工具、强制使用至少一个工具、强制不使用工具等），可以通过修改tool_choice参数来强制指定工具调用的策略。可选值：
     * "auto"
     * 表示由大模型进行工具策略的选择。
     * "none"
     * 如果您希望无论输入什么问题，Function Calling 都不会进行工具调用，可以设定tool_choice参数为"none"；
     * {"type": "function", "function": {"name": "the_function_to_call"}}
     * 如果您希望对于某一类问题，Function Calling 能够强制调用某个工具，可以设定tool_choice参数为{"type": "function", "function": {"name": "the_function_to_call"}}，其中the_function_to_call是您指定的工具函数名称。
     *
     * @return * 默认情况下，模型将决定何时以及使用多少工具。您可以使用tool_choice参数强制特定行为。
     * * tool_choice string 或 object （可选）默认值为 "auto"
     * * 如果您希望对于某一类问题，大模型能够采取制定好的工具选择策略（如强制使用某个工具、强制使用至少一个工具、强制不使用工具等），可以通过修改tool_choice参数来强制指定工具调用的策略。可选值：
     * * "auto"
     * * 表示由大模型进行工具策略的选择。
     * * "none"
     * * 如果您希望无论输入什么问题，Function Calling 都不会进行工具调用，可以设定tool_choice参数为"none"；
     * * {"type": "function", "function": {"name": "the_function_to_call"}}
     * * 如果您希望对于某一类问题，Function Calling 能够强制调用某个工具，可以设定tool_choice参数为{"type": "function", "function": {"name": "the_function_to_call"}}，其中the_function_to_call是您指定的工具函数名称。
     * *
     */
    public Boolean getToolChoiceRequired() {
        return toolChoiceRequired;
    }

    /**
     * 默认情况下，模型将决定何时以及使用多少工具。您可以使用tool_choice参数强制特定行为。
     * tool_choice string 或 object （可选）默认值为 "auto"
     * 如果您希望对于某一类问题，大模型能够采取制定好的工具选择策略（如强制使用某个工具、强制使用至少一个工具、强制不使用工具等），可以通过修改tool_choice参数来强制指定工具调用的策略。可选值：
     * "auto"
     * 表示由大模型进行工具策略的选择。
     * "none"
     * 如果您希望无论输入什么问题，Function Calling 都不会进行工具调用，可以设定tool_choice参数为"none"；
     * {"type": "function", "function": {"name": "the_function_to_call"}}
     * 如果您希望对于某一类问题，Function Calling 能够强制调用某个工具，可以设定tool_choice参数为{"type": "function", "function": {"name": "the_function_to_call"}}，其中the_function_to_call是您指定的工具函数名称。
     *
     * @param toolChoiceRequired * 默认情况下，模型将决定何时以及使用多少工具。您可以使用tool_choice参数强制特定行为。
     *                           * tool_choice string 或 object （可选）默认值为 "auto"
     *                           * 如果您希望对于某一类问题，大模型能够采取制定好的工具选择策略（如强制使用某个工具、强制使用至少一个工具、强制不使用工具等），可以通过修改tool_choice参数来强制指定工具调用的策略。可选值：
     *                           * "auto"
     *                           * 表示由大模型进行工具策略的选择。
     *                           * "none"
     *                           * 如果您希望无论输入什么问题，Function Calling 都不会进行工具调用，可以设定tool_choice参数为"none"；
     *                           * {"type": "function", "function": {"name": "the_function_to_call"}}
     *                           * 如果您希望对于某一类问题，Function Calling 能够强制调用某个工具，可以设定tool_choice参数为{"type": "function", "function": {"name": "the_function_to_call"}}，其中the_function_to_call是您指定的工具函数名称。
     *                           *
     */
    public void setToolChoiceRequired(Boolean toolChoiceRequired) {
        this.toolChoiceRequired = toolChoiceRequired;
    }

    /**
     * 输出音频的音色与文件格式（只支持设定为"wav"）通过audio参数来配置，
     * 如：audio={"voice": "Cherry", "format": "wav"}，
     * 其中商业版模型voice参数可选值为：["Cherry", "Serena", "Ethan", "Chelsie"]，
     * 开源版模型voice参数可选值为：["Ethan", "Chelsie"]。
     *
     * @return * 输出音频的音色与文件格式（只支持设定为"wav"）通过audio参数来配置，
     * * 如：audio={"voice": "Cherry", "format": "wav"}，
     * * 其中商业版模型voice参数可选值为：["Cherry", "Serena", "Ethan", "Chelsie"]，
     * * 开源版模型voice参数可选值为：["Ethan", "Chelsie"]。
     */
    public Audio getAudio() {
        return audio;
    }

    /**
     * * 输出音频的音色与文件格式（只支持设定为"wav"）通过audio参数来配置，
     * * 如：audio={"voice": "Cherry", "format": "wav"}，
     * * 其中商业版模型voice参数可选值为：["Cherry", "Serena", "Ethan", "Chelsie"]，
     * * 开源版模型voice参数可选值为：["Ethan", "Chelsie"]。
     *
     * @param audio * 输出音频的音色与文件格式（只支持设定为"wav"）通过audio参数来配置，
     *              * 如：audio={"voice": "Cherry", "format": "wav"}，
     *              * 其中商业版模型voice参数可选值为：["Cherry", "Serena", "Ethan", "Chelsie"]，
     *              * 开源版模型voice参数可选值为：["Ethan", "Chelsie"]。
     */
    public void setAudio(Audio audio) {
        this.audio = audio;
    }

    /**
     * 开启联网搜索的参数（供应商提供的实现）
     *
     * @return 开启联网搜索的参数（供应商提供的实现）
     */
    public Boolean getEnableSearch() {
        return enableSearch;
    }

    /**
     * 开启联网搜索的参数（供应商提供的实现）
     *
     * @param enableSearch 开启联网搜索的参数（供应商提供的实现）
     */
    public void setEnableSearch(Boolean enableSearch) {
        this.enableSearch = enableSearch;
    }

    /**
     * 开启联网搜索的参数
     * search_options = {
     * "forced_search": True, # 强制开启联网搜索
     * "enable_source": True, # 使返回结果包含搜索来源的信息，OpenAI 兼容方式暂不支持返回
     * "enable_citation": True, # 开启角标标注功能
     * "citation_format":  # 角标形式为[ref_i]
     * "search_strategy": "pro" # 模型将搜索10条互联网信息
     * },
     *
     * @return * 开启联网搜索的参数
     * * search_options = {
     * * "forced_search": True, # 强制开启联网搜索
     * * "enable_source": True, # 使返回结果包含搜索来源的信息，OpenAI 兼容方式暂不支持返回
     * * "enable_citation": True, # 开启角标标注功能
     * * "citation_format":  # 角标形式为[ref_i]
     * * "search_strategy": "pro" # 模型将搜索10条互联网信息
     * * },
     */
    public Map<String, Object> getSearchOptions() {
        return searchOptions;
    }

    /**
     * 开启联网搜索的参数
     * search_options = {
     * "forced_search": True, # 强制开启联网搜索
     * "enable_source": True, # 使返回结果包含搜索来源的信息，OpenAI 兼容方式暂不支持返回
     * "enable_citation": True, # 开启角标标注功能
     * "citation_format":  # 角标形式为[ref_i]
     * "search_strategy": "pro" # 模型将搜索10条互联网信息
     * },
     *
     * @param searchOptions * 开启联网搜索的参数
     *                      * search_options = {
     *                      * "forced_search": True, # 强制开启联网搜索
     *                      * "enable_source": True, # 使返回结果包含搜索来源的信息，OpenAI 兼容方式暂不支持返回
     *                      * "enable_citation": True, # 开启角标标注功能
     *                      * "citation_format":  # 角标形式为[ref_i]
     *                      * "search_strategy": "pro" # 模型将搜索10条互联网信息
     *                      * },
     */
    public void setSearchOptions(Map<String, Object> searchOptions) {
        this.searchOptions = searchOptions;
    }

    /**
     * # 是否开启思考模式（供应商提供的实现）
     * 适用于 Qwen3 模型。
     * Qwen3 商业版模型默认值为 False，Qwen3 开源版模型默认值为 True。
     *
     * @return * # 是否开启思考模式（供应商提供的实现）
     * * 适用于 Qwen3 模型。
     * * Qwen3 商业版模型默认值为 False，Qwen3 开源版模型默认值为 True。
     */
    public Boolean getEnableThinking() {
        return enableThinking;
    }

    /**
     * # 是否开启思考模式（供应商提供的实现）
     * 适用于 Qwen3 模型。
     * Qwen3 商业版模型默认值为 False，Qwen3 开源版模型默认值为 True。
     *
     * @param enableThinking * # 是否开启思考模式（供应商提供的实现）
     *                       * 适用于 Qwen3 模型。
     *                       * Qwen3 商业版模型默认值为 False，Qwen3 开源版模型默认值为 True。
     */
    public void setEnableThinking(Boolean enableThinking) {
        this.enableThinking = enableThinking;
    }

    /**
     * 参数设置最大推理过程 Token 数，不能超过供应商要求的最大思维链Token数：例：38912。两个参数对 QwQ 与 DeepSeek-R1 模型无效
     *
     * @return * 参数设置最大推理过程 Token 数，不能超过供应商要求的最大思维链Token数：例：38912。两个参数对 QwQ 与 DeepSeek-R1 模型无效
     */
    public Integer getThinkingBudget() {
        return thinkingBudget;
    }

    /**
     * 参数设置最大推理过程 Token 数，不能超过供应商要求的最大思维链Token数：例：38912。两个参数对 QwQ 与 DeepSeek-R1 模型无效
     *
     * @param thinkingBudget * 参数设置最大推理过程 Token 数，不能超过供应商要求的最大思维链Token数：例：38912。两个参数对 QwQ 与 DeepSeek-R1 模型无效
     */
    public void setThinkingBudget(Integer thinkingBudget) {
        this.thinkingBudget = thinkingBudget;
    }

    /**
     * （可选）默认值为["text"]
     * 输出数据的模态，仅支持 Qwen-Omni 模型指定。可选值：
     * ["text","audio"]：输出文本与音频；
     * ["text"]：输出文本。
     *
     * @return * （可选）默认值为["text"]
     * * 输出数据的模态，仅支持 Qwen-Omni 模型指定。可选值：
     * * ["text","audio"]：输出文本与音频；
     * * ["text"]：输出文本。
     */
    public List<String> getModalities() {
        return modalities;
    }

    /**
     * （可选）默认值为["text"]
     * 输出数据的模态，仅支持 Qwen-Omni 模型指定。可选值：
     * ["text","audio"]：输出文本与音频；
     * ["text"]：输出文本。
     *
     * @param modalities * （可选）默认值为["text"]
     *                   * 输出数据的模态，仅支持 Qwen-Omni 模型指定。可选值：
     *                   * ["text","audio"]：输出文本与音频；
     *                   * ["text"]：输出文本。
     */
    public void setModalities(List<String> modalities) {
        this.modalities = modalities;
    }

    /**
     * 由历史对话组成的消息列表
     * (数据由FunctionCallStreamingResponseHandler自动维护)
     *
     * @return * 由历史对话组成的消息列表
     * * (数据由FunctionCallStreamingResponseHandler自动维护)
     */
    public List<ChatMessage> getMessageList() {
        return messageList;
    }

    public void setMessageList(List<ChatMessage> messageList) {
        this.messageList = messageList;
    }

    /**
     * 可供模型调用的工具数组，可以包含一个或多个工具对象。一次Function Calling流程模型会从中选择一个工具。
     * 目前不支持通义千问VL/Audio，也不建议用于数学和代码模型。
     * (数据由FunctionCallStreamingResponseHandler自动维护)
     *
     * @return * 可供模型调用的工具数组，可以包含一个或多个工具对象。一次Function Calling流程模型会从中选择一个工具。
     * * 目前不支持通义千问VL/Audio，也不建议用于数学和代码模型。
     * * (数据由FunctionCallStreamingResponseHandler自动维护)
     */
    public List<ToolSpecification> getToolSpecificationList() {
        return toolSpecificationList;
    }

    public void setToolSpecificationList(List<ToolSpecification> toolSpecificationList) {
        this.toolSpecificationList = toolSpecificationList;
    }

    /**
     * 最后一条消息是否（工具结果）
     *
     * @return true=工具结果
     */
    public boolean isLastToolExecutionResultMessage() {
        if (messageList == null || messageList.isEmpty()) {
            return false;
        }
        return messageList.get(messageList.size() - 1) instanceof ToolExecutionResultMessage;
    }

    /**
     * 最后一条消息是否（AI消息）
     *
     * @return true=AI消息
     */
    public boolean isLastAiMessage() {
        if (messageList == null || messageList.isEmpty()) {
            return false;
        }
        return messageList.get(messageList.size() - 1) instanceof AiMessage;
    }

    /**
     * 最后一条消息是否（用户消息）
     *
     * @return true=用户消息
     */
    public boolean isLastUserMessage() {
        if (messageList == null || messageList.isEmpty()) {
            return false;
        }
        return messageList.get(messageList.size() - 1) instanceof UserMessage;
    }

    /**
     * 最后一条消息是否（系统消息）
     *
     * @return true=系统消息
     */
    public boolean isLastSystemMessage() {
        if (messageList == null || messageList.isEmpty()) {
            return false;
        }
        return messageList.get(messageList.size() - 1) instanceof SystemMessage;
    }

    /**
     * 克隆一个新的对象
     *
     * @return 新的对象
     */
    @Override
    public GenerateRequest clone() {
        try {
            GenerateRequest clone = (GenerateRequest) super.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e.toString(), e);
        }
    }

    @Override
    public String toString() {
        return AiUtil.getLastUserQuestion(messageList);
    }
}
