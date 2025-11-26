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
    private final List<ChatMessage> messageList;
    /**
     * 可供模型调用的工具数组，可以包含一个或多个工具对象。一次Function Calling流程模型会从中选择一个工具。
     * 目前不支持通义千问VL/Audio，也不建议用于数学和代码模型。
     * (数据由FunctionCallStreamingResponseHandler自动维护)
     */
    private final List<ToolSpecification> toolSpecificationList;
    /**
     * 选项
     */
    private final GenerateRequest.Options options = new GenerateRequest.Options();

    public GenerateRequest(List<ChatMessage> messageList) {
        this.messageList = messageList;
        this.toolSpecificationList = null;
    }

    public GenerateRequest(List<ChatMessage> messageList, List<ToolSpecification> toolSpecificationList) {
        this.messageList = messageList;
        this.toolSpecificationList = toolSpecificationList;
    }

    public static void copyTo(GenerateRequest.Options source, GenerateRequest.Options dist) {
        dist.toolChoiceRequired = source.toolChoiceRequired;
        dist.enableSearch = source.enableSearch;
        dist.searchOptions = source.searchOptions;
        dist.enableThinking = source.enableThinking;
        dist.thinkingBudget = source.thinkingBudget;
        dist.modalities = source.modalities;
        dist.audio = source.audio;
        dist.jsonSchema = source.jsonSchema;
        dist.parallelToolCalls = source.parallelToolCalls;
        dist.temperature = source.temperature;
        dist.topP = source.topP;
        dist.topK = source.topK;
        dist.presencePenalty = source.presencePenalty;
        dist.vlHighResolutionImages = source.vlHighResolutionImages;
        dist.n = source.n;
        dist.partialMode = source.partialMode;
        dist.enableCodeInterpreter = source.enableCodeInterpreter;
        dist.seed = source.seed;
        dist.dashScopeDataInspection = source.dashScopeDataInspection;
    }

    public Options getOptions() {
        return options;
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
        GenerateRequest clone = new GenerateRequest(this.messageList, this.toolSpecificationList);
        copyTo(this.options, clone.options);
        return clone;
    }

    @Override
    public String toString() {
        return AiUtil.getLastUserQuestion(messageList);
    }

    /**
     * 请求参数
     */
    public static class Options implements Cloneable {
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
         * 开启联网搜索的参数（可选）默认值为 false 是否开启联网搜索。相关文档：联网搜索 可选值： true：开启； 若开启后未联网搜索，可优化提示词，或设置search_options中的forced_search参数开启强制搜索。 false：不开启。 启用互联网搜索功能可能会增加 Token 的消耗。）
         */
        private Boolean enableSearch;
        /**
         * 开启联网搜索的参数
         * search_options = {
         * "forced_search": True, # boolean（可选）默认值为false是否强制开启联网搜索，仅当enable_search为true时生效。可选值：true：强制开启；false：不强制开启，由模型判断是否联网搜索。
         * "enable_search_extension":  # boolean（可选）默认值为false是否开启垂域搜索，仅当enable_search为true时生效。可选值：true：开启。false：不开启。
         * "search_strategy": "turbo" # string（可选）默认值为turbo 搜索量级策略，仅当enable_search为true时生效。 可选值： turbo （默认）: 兼顾响应速度与搜索效果，适用于大多数场景。 max: 采用更全面的搜索策略，可调用多源搜索引擎，以获取更详尽的搜索结果，但响应时间可能更长。 agent：可多次调用联网搜索工具与大模型，实现多轮信息检索与内容整合。 agent策略仅适用于 qwen3-max 与 qwen3-max-2025-09-23。 agent策略不可与其他联网搜索策略同时设定。
         * }
         */
        private Map<String, Object> searchOptions;
        /**
         * # 是否开启思考模式（供应商提供的实现）
         * 该参数非OpenAI标准参数
         * 适用于 Qwen3 模型。
         * Qwen3 商业版模型默认值为 False，Qwen3 开源版模型默认值为 True。
         */
        private Boolean enableThinking;
        /**
         * 参数设置最大推理过程 Token 数，不能超过供应商要求的最大思维链Token数：例：38912。两个参数对 QwQ 与 DeepSeek-R1 模型无效
         * 该参数非OpenAI标准参数
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
        /**
         * 是否开启代码解释器功能。boolean（可选）仅当model为qwen3-max-preview且enable_thinking为true时生效。相关文档：代码解释器
         * 代码解释器与 Function Calling 互斥，不可同时启用。
         * 同时启用会报错。
         * 启用代码解释器后，单次请求会触发多次模型推理，usage 字段汇总所有调用的 Token 消耗。
         */
        private Boolean enableCodeInterpreter;
        /**
         * 随机数种子。用于确保在相同输入和参数下生成结果可复现。若调用时传入相同的 seed 且其他参数不变，模型将尽可能返回相同结果。
         * 取值范围：[0,231−1]。
         */
        private Integer seed;
        /**
         * 在通义千问 API 的内容安全能力基础上，是否进一步识别输入输出内容的违规信息。取值如下：
         * '{"input":"cip","output":"cip"}'：进一步识别；
         * 不设置该参数：不进一步识别。
         * 通过 HTTP 调用时请放入请求头：-H "X-DashScope-DataInspection: {\"input\": \"cip\", \"output\": \"cip\"}"；
         */
        private String dashScopeDataInspection;
        /**
         * 采样温度，控制模型生成文本的多样性。
         * temperature越高，生成的文本更多样，反之，生成的文本更确定。
         * 取值范围： [0, 2)
         * temperature与top_p均可以控制生成文本的多样性，建议只设置其中一个值。更多说明，请参见文本生成模型概述。
         */
        private Double temperature;
        /**
         * 核采样的概率阈值，控制模型生成文本的多样性。
         * top_p越高，生成的文本更多样。反之，生成的文本更确定。
         * 取值范围：（0,1.0]
         * temperature与top_p均可以控制生成文本的多样性，建议只设置其中一个值。更多说明，请参见文本生成模型概述。
         */
        private Double topP;
        /**
         * 指定生成过程中用于采样的候选 Token 数量。值越大，输出越随机；值越小，输出越确定。若设为 null 或大于 100，则禁用 top_k 策略，仅 top_p 策略生效。取值必须为大于或等于 0 的整数。
         * 该参数非OpenAI标准参数
         */
        private Integer topK;
        /**
         * 控制模型生成文本时的内容重复度。
         * 取值范围：[-2.0, 2.0]。正值降低重复度，负值增加重复度。
         * 在创意写作或头脑风暴等需要多样性、趣味性或创造力的场景中，建议调高该值；在技术文档或正式文本等强调一致性与术语准确性的场景中，建议调低该值。
         */
        private Double presencePenalty;
        /**
         * 是否将输入图像的像素上限提升至 16384 Token 对应的像素值。相关文档：处理高分辨率图像。
         * vl_high_resolution_images：true，使用固定分辨率策略，忽略 max_pixels 设置，超过此分辨率时会将图像总像素缩小至此上限内。
         * vl_high_resolution_images为false，实际分辨率由 max_pixels 与默认上限共同决定，取二者计算结果的最大值。超过此像素上限时会将图像缩小至此上限内。
         */
        private Boolean vlHighResolutionImages;
        /**
         * 生成响应的数量，取值范围是1-4。适用于需生成多个候选响应的场景，例如创意写作或广告文案。
         * （可选） 默认值为1
         * 若传入 tools 参数， 请将n 设为 1。
         * 增大 n 会增加输出 Token 的消耗，但不增加输入 Token 消耗。
         */
        private Integer n;
        /**
         * 是否开启指定前缀续写（Partial Mode）
         * 在代码补全、文本续写等场景中，需要模型从已有的文本片段（前缀）开始继续生成。
         * Partial Mode 可提供精确控制能力，确保模型输出的内容紧密衔接提供的前缀，提升生成结果的准确性与可控性。
         * 需在messages 数组中将最后一条消息的 role 设置为 assistant，并在其 content 中提供前缀，在此消息中设置参数 "partial": true。messages格式如下：
         * <pre>
         * [
         *     {
         *         "role": "user",
         *         "content": "请补全这个斐波那契函数，勿添加其它内容"
         *     },
         *     {
         *         "role": "assistant",
         *         "content": "def calculate_fibonacci(n):\n    if n == 1:\n        return n\n    else:\n",
         *         "partial": true
         *     }
         * ]
         * </pre>
         */
        private Boolean partialMode;

        /**
         * 采样温度，控制模型生成文本的多样性。
         * temperature越高，生成的文本更多样，反之，生成的文本更确定。
         * 取值范围： [0, 2)
         * temperature与top_p均可以控制生成文本的多样性，建议只设置其中一个值。更多说明，请参见文本生成模型概述。
         *
         * @return 采样温度，控制模型生成文本的多样性。
         */
        public Double getTemperature() {
            return temperature;
        }

        /**
         * 采样温度，控制模型生成文本的多样性。
         * temperature越高，生成的文本更多样，反之，生成的文本更确定。
         * 取值范围： [0, 2)
         * temperature与top_p均可以控制生成文本的多样性，建议只设置其中一个值。更多说明，请参见文本生成模型概述。
         *
         * @param temperature 采样温度，控制模型生成文本的多样性。
         */
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
         * 开启联网搜索的参数（可选）默认值为 false 是否开启联网搜索。相关文档：联网搜索 可选值： true：开启； 若开启后未联网搜索，可优化提示词，或设置search_options中的forced_search参数开启强制搜索。 false：不开启。 启用互联网搜索功能可能会增加 Token 的消耗。）
         *
         * @return 开启联网搜索的参数（可选）默认值为 false 是否开启联网搜索。相关文档：联网搜索 可选值： true：开启； 若开启后未联网搜索，可优化提示词，或设置search_options中的forced_search参数开启强制搜索。 false：不开启。 启用互联网搜索功能可能会增加 Token 的消耗。）
         */
        public Boolean getEnableSearch() {
            return enableSearch;
        }

        /**
         * 开启联网搜索的参数（可选）默认值为 false 是否开启联网搜索。相关文档：联网搜索 可选值： true：开启； 若开启后未联网搜索，可优化提示词，或设置search_options中的forced_search参数开启强制搜索。 false：不开启。 启用互联网搜索功能可能会增加 Token 的消耗。）
         *
         * @param enableSearch 开启联网搜索的参数（可选）默认值为 false 是否开启联网搜索。相关文档：联网搜索 可选值： true：开启； 若开启后未联网搜索，可优化提示词，或设置search_options中的forced_search参数开启强制搜索。 false：不开启。 启用互联网搜索功能可能会增加 Token 的消耗。）
         */
        public void setEnableSearch(Boolean enableSearch) {
            this.enableSearch = enableSearch;
        }

        /**
         * 开启联网搜索的参数
         * search_options = {
         * "forced_search": True, # boolean（可选）默认值为false是否强制开启联网搜索，仅当enable_search为true时生效。可选值：true：强制开启；false：不强制开启，由模型判断是否联网搜索。
         * "enable_search_extension":  # boolean（可选）默认值为false是否开启垂域搜索，仅当enable_search为true时生效。可选值：true：开启。false：不开启。
         * "search_strategy": "turbo" # string（可选）默认值为turbo 搜索量级策略，仅当enable_search为true时生效。 可选值： turbo （默认）: 兼顾响应速度与搜索效果，适用于大多数场景。 max: 采用更全面的搜索策略，可调用多源搜索引擎，以获取更详尽的搜索结果，但响应时间可能更长。 agent：可多次调用联网搜索工具与大模型，实现多轮信息检索与内容整合。 agent策略仅适用于 qwen3-max 与 qwen3-max-2025-09-23。 agent策略不可与其他联网搜索策略同时设定。
         * }
         *
         * @return * 开启联网搜索的参数
         * * search_options = {
         * * "forced_search": True, # boolean（可选）默认值为false是否强制开启联网搜索，仅当enable_search为true时生效。可选值：true：强制开启；false：不强制开启，由模型判断是否联网搜索。
         * * "enable_search_extension":  # boolean（可选）默认值为false是否开启垂域搜索，仅当enable_search为true时生效。可选值：true：开启。false：不开启。
         * * "search_strategy": "turbo" # string（可选）默认值为turbo 搜索量级策略，仅当enable_search为true时生效。 可选值： turbo （默认）: 兼顾响应速度与搜索效果，适用于大多数场景。 max: 采用更全面的搜索策略，可调用多源搜索引擎，以获取更详尽的搜索结果，但响应时间可能更长。 agent：可多次调用联网搜索工具与大模型，实现多轮信息检索与内容整合。 agent策略仅适用于 qwen3-max 与 qwen3-max-2025-09-23。 agent策略不可与其他联网搜索策略同时设定。
         * * }
         */
        public Map<String, Object> getSearchOptions() {
            return searchOptions;
        }

        /**
         * 开启联网搜索的参数
         * search_options = {
         * "forced_search": True, # boolean（可选）默认值为false是否强制开启联网搜索，仅当enable_search为true时生效。可选值：true：强制开启；false：不强制开启，由模型判断是否联网搜索。
         * "enable_search_extension":  # boolean（可选）默认值为false是否开启垂域搜索，仅当enable_search为true时生效。可选值：true：开启。false：不开启。
         * "search_strategy": "turbo" # string（可选）默认值为turbo 搜索量级策略，仅当enable_search为true时生效。 可选值： turbo （默认）: 兼顾响应速度与搜索效果，适用于大多数场景。 max: 采用更全面的搜索策略，可调用多源搜索引擎，以获取更详尽的搜索结果，但响应时间可能更长。 agent：可多次调用联网搜索工具与大模型，实现多轮信息检索与内容整合。 agent策略仅适用于 qwen3-max 与 qwen3-max-2025-09-23。 agent策略不可与其他联网搜索策略同时设定。
         * }
         *
         * @param searchOptions * 开启联网搜索的参数
         *                      * search_options = {
         *                      * "forced_search": True, # boolean（可选）默认值为false是否强制开启联网搜索，仅当enable_search为true时生效。可选值：true：强制开启；false：不强制开启，由模型判断是否联网搜索。
         *                      * "enable_search_extension":  # boolean（可选）默认值为false是否开启垂域搜索，仅当enable_search为true时生效。可选值：true：开启。false：不开启。
         *                      * "search_strategy": "turbo" # string（可选）默认值为turbo 搜索量级策略，仅当enable_search为true时生效。 可选值： turbo （默认）: 兼顾响应速度与搜索效果，适用于大多数场景。 max: 采用更全面的搜索策略，可调用多源搜索引擎，以获取更详尽的搜索结果，但响应时间可能更长。 agent：可多次调用联网搜索工具与大模型，实现多轮信息检索与内容整合。 agent策略仅适用于 qwen3-max 与 qwen3-max-2025-09-23。 agent策略不可与其他联网搜索策略同时设定。
         *                      * }
         */
        public void setSearchOptions(Map<String, Object> searchOptions) {
            this.searchOptions = searchOptions;
        }

        /**
         * # 是否开启思考模式（供应商提供的实现）
         * 适用于 Qwen3 模型。
         * 该参数非OpenAI标准参数
         * Qwen3 商业版模型默认值为 False，Qwen3 开源版模型默认值为 True。
         *
         * @return * # 是否开启思考模式（供应商提供的实现）
         * * 适用于 Qwen3 模型。
         * 该参数非OpenAI标准参数
         * * Qwen3 商业版模型默认值为 False，Qwen3 开源版模型默认值为 True。
         */
        public Boolean getEnableThinking() {
            return enableThinking;
        }

        /**
         * # 是否开启思考模式（供应商提供的实现）
         * 适用于 Qwen3 模型。
         * 该参数非OpenAI标准参数
         * Qwen3 商业版模型默认值为 False，Qwen3 开源版模型默认值为 True。
         *
         * @param enableThinking * # 是否开启思考模式（供应商提供的实现）
         *                       * 适用于 Qwen3 模型。
         *                       该参数非OpenAI标准参数
         *                       * Qwen3 商业版模型默认值为 False，Qwen3 开源版模型默认值为 True。
         */
        public void setEnableThinking(Boolean enableThinking) {
            this.enableThinking = enableThinking;
        }

        /**
         * 参数设置最大推理过程 Token 数，不能超过供应商要求的最大思维链Token数：例：38912。两个参数对 QwQ 与 DeepSeek-R1 模型无效
         * 该参数非OpenAI标准参数
         *
         * @return * 参数设置最大推理过程 Token 数，不能超过供应商要求的最大思维链Token数：例：38912。两个参数对 QwQ 与 DeepSeek-R1 模型无效
         */
        public Integer getThinkingBudget() {
            return thinkingBudget;
        }

        /**
         * 参数设置最大推理过程 Token 数，不能超过供应商要求的最大思维链Token数：例：38912。两个参数对 QwQ 与 DeepSeek-R1 模型无效
         * 该参数非OpenAI标准参数
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
         * 是否开启代码解释器功能。boolean（可选）仅当model为qwen3-max-preview且enable_thinking为true时生效。相关文档：代码解释器
         * 代码解释器与 Function Calling 互斥，不可同时启用。
         * 同时启用会报错。
         * 启用代码解释器后，单次请求会触发多次模型推理，usage 字段汇总所有调用的 Token 消耗。
         *
         * @return * 是否开启代码解释器功能。boolean（可选）仅当model为qwen3-max-preview且enable_thinking为true时生效。相关文档：代码解释器
         * * 代码解释器与 Function Calling 互斥，不可同时启用。
         * * 同时启用会报错。
         * * 启用代码解释器后，单次请求会触发多次模型推理，usage 字段汇总所有调用的 Token 消耗。
         */
        public Boolean getEnableCodeInterpreter() {
            return enableCodeInterpreter;
        }

        /**
         * 是否开启代码解释器功能。boolean（可选）仅当model为qwen3-max-preview且enable_thinking为true时生效。相关文档：代码解释器
         * 代码解释器与 Function Calling 互斥，不可同时启用。
         * 同时启用会报错。
         * 启用代码解释器后，单次请求会触发多次模型推理，usage 字段汇总所有调用的 Token 消耗。
         *
         * @param enableCodeInterpreter * 是否开启代码解释器功能。boolean（可选）仅当model为qwen3-max-preview且enable_thinking为true时生效。相关文档：代码解释器
         *                              * 代码解释器与 Function Calling 互斥，不可同时启用。
         *                              * 同时启用会报错。
         *                              * 启用代码解释器后，单次请求会触发多次模型推理，usage 字段汇总所有调用的 Token 消耗。
         */
        public void setEnableCodeInterpreter(Boolean enableCodeInterpreter) {
            this.enableCodeInterpreter = enableCodeInterpreter;
        }

        /**
         * 随机数种子。用于确保在相同输入和参数下生成结果可复现。若调用时传入相同的 seed 且其他参数不变，模型将尽可能返回相同结果。
         * 取值范围：[0,231−1]。
         *
         * @return * 随机数种子。用于确保在相同输入和参数下生成结果可复现。若调用时传入相同的 seed 且其他参数不变，模型将尽可能返回相同结果。
         * * 取值范围：[0,231−1]。
         */
        public Integer getSeed() {
            return seed;
        }

        /**
         * 随机数种子。用于确保在相同输入和参数下生成结果可复现。若调用时传入相同的 seed 且其他参数不变，模型将尽可能返回相同结果。
         * 取值范围：[0,231−1]。
         *
         * @param seed * 随机数种子。用于确保在相同输入和参数下生成结果可复现。若调用时传入相同的 seed 且其他参数不变，模型将尽可能返回相同结果。
         *             * 取值范围：[0,231−1]。
         */
        public void setSeed(Integer seed) {
            this.seed = seed;
        }

        /**
         * 在通义千问 API 的内容安全能力基础上，是否进一步识别输入输出内容的违规信息。取值如下：
         * '{"input":"cip","output":"cip"}'：进一步识别；
         * 不设置该参数：不进一步识别。
         * 通过 HTTP 调用时请放入请求头：-H "X-DashScope-DataInspection: {\"input\": \"cip\", \"output\": \"cip\"}"；
         *
         * @return * 在通义千问 API 的内容安全能力基础上，是否进一步识别输入输出内容的违规信息。取值如下：
         * * '{"input":"cip","output":"cip"}'：进一步识别；
         * * 不设置该参数：不进一步识别。
         * * 通过 HTTP 调用时请放入请求头：-H "X-DashScope-DataInspection: {\"input\": \"cip\", \"output\": \"cip\"}"；
         */
        public String getDashScopeDataInspection() {
            return dashScopeDataInspection;
        }

        /**
         * 在通义千问 API 的内容安全能力基础上，是否进一步识别输入输出内容的违规信息。取值如下：
         * '{"input":"cip","output":"cip"}'：进一步识别；
         * 不设置该参数：不进一步识别。
         * 通过 HTTP 调用时请放入请求头：-H "X-DashScope-DataInspection: {\"input\": \"cip\", \"output\": \"cip\"}"；
         *
         * @param dashScopeDataInspection * 在通义千问 API 的内容安全能力基础上，是否进一步识别输入输出内容的违规信息。取值如下：
         *                                * '{"input":"cip","output":"cip"}'：进一步识别；
         *                                * 不设置该参数：不进一步识别。
         *                                * 通过 HTTP 调用时请放入请求头：-H "X-DashScope-DataInspection: {\"input\": \"cip\", \"output\": \"cip\"}"；
         */
        public void setDashScopeDataInspection(String dashScopeDataInspection) {
            this.dashScopeDataInspection = dashScopeDataInspection;
        }

        /**
         * 核采样的概率阈值，控制模型生成文本的多样性。
         * top_p越高，生成的文本更多样。反之，生成的文本更确定。
         * 取值范围：（0,1.0]
         * temperature与top_p均可以控制生成文本的多样性，建议只设置其中一个值。更多说明，请参见文本生成模型概述。
         *
         * @return * 核采样的概率阈值，控制模型生成文本的多样性。
         * * top_p越高，生成的文本更多样。反之，生成的文本更确定。
         * * 取值范围：（0,1.0]
         * * temperature与top_p均可以控制生成文本的多样性，建议只设置其中一个值。更多说明，请参见文本生成模型概述。
         */
        public Double getTopP() {
            return topP;
        }

        /**
         * 核采样的概率阈值，控制模型生成文本的多样性。
         * top_p越高，生成的文本更多样。反之，生成的文本更确定。
         * 取值范围：（0,1.0]
         * temperature与top_p均可以控制生成文本的多样性，建议只设置其中一个值。更多说明，请参见文本生成模型概述。
         *
         * @param topP * 核采样的概率阈值，控制模型生成文本的多样性。
         *             * top_p越高，生成的文本更多样。反之，生成的文本更确定。
         *             * 取值范围：（0,1.0]
         *             * temperature与top_p均可以控制生成文本的多样性，建议只设置其中一个值。更多说明，请参见文本生成模型概述。
         */
        public void setTopP(Double topP) {
            this.topP = topP;
        }

        /**
         * 指定生成过程中用于采样的候选 Token 数量。值越大，输出越随机；值越小，输出越确定。若设为 null 或大于 100，则禁用 top_k 策略，仅 top_p 策略生效。取值必须为大于或等于 0 的整数。
         * 该参数非OpenAI标准参数
         *
         * @return * 指定生成过程中用于采样的候选 Token 数量。值越大，输出越随机；值越小，输出越确定。若设为 null 或大于 100，则禁用 top_k 策略，仅 top_p 策略生效。取值必须为大于或等于 0 的整数。
         * * 该参数非OpenAI标准参数
         */
        public Integer getTopK() {
            return topK;
        }

        /**
         * 指定生成过程中用于采样的候选 Token 数量。值越大，输出越随机；值越小，输出越确定。若设为 null 或大于 100，则禁用 top_k 策略，仅 top_p 策略生效。取值必须为大于或等于 0 的整数。
         * 该参数非OpenAI标准参数
         *
         * @param topK * 指定生成过程中用于采样的候选 Token 数量。值越大，输出越随机；值越小，输出越确定。若设为 null 或大于 100，则禁用 top_k 策略，仅 top_p 策略生效。取值必须为大于或等于 0 的整数。
         *             * 该参数非OpenAI标准参数
         */
        public void setTopK(Integer topK) {
            this.topK = topK;
        }

        /**
         * 控制模型生成文本时的内容重复度。
         * 取值范围：[-2.0, 2.0]。正值降低重复度，负值增加重复度。
         * 在创意写作或头脑风暴等需要多样性、趣味性或创造力的场景中，建议调高该值；在技术文档或正式文本等强调一致性与术语准确性的场景中，建议调低该值。
         *
         * @return * 控制模型生成文本时的内容重复度。
         * * 取值范围：[-2.0, 2.0]。正值降低重复度，负值增加重复度。
         * * 在创意写作或头脑风暴等需要多样性、趣味性或创造力的场景中，建议调高该值；在技术文档或正式文本等强调一致性与术语准确性的场景中，建议调低该值。
         */
        public Double getPresencePenalty() {
            return presencePenalty;
        }

        /**
         * * 控制模型生成文本时的内容重复度。
         * * 取值范围：[-2.0, 2.0]。正值降低重复度，负值增加重复度。
         * * 在创意写作或头脑风暴等需要多样性、趣味性或创造力的场景中，建议调高该值；在技术文档或正式文本等强调一致性与术语准确性的场景中，建议调低该值。
         *
         * @param presencePenalty * 控制模型生成文本时的内容重复度。
         *                        * 取值范围：[-2.0, 2.0]。正值降低重复度，负值增加重复度。
         *                        * 在创意写作或头脑风暴等需要多样性、趣味性或创造力的场景中，建议调高该值；在技术文档或正式文本等强调一致性与术语准确性的场景中，建议调低该值。
         */
        public void setPresencePenalty(Double presencePenalty) {
            this.presencePenalty = presencePenalty;
        }

        /**
         * 是否将输入图像的像素上限提升至 16384 Token 对应的像素值。相关文档：处理高分辨率图像。
         * vl_high_resolution_images：true，使用固定分辨率策略，忽略 max_pixels 设置，超过此分辨率时会将图像总像素缩小至此上限内。
         * vl_high_resolution_images为false，实际分辨率由 max_pixels 与默认上限共同决定，取二者计算结果的最大值。超过此像素上限时会将图像缩小至此上限内。
         *
         * @return 是否将输入图像的像素上限提升至 16384 Token 对应的像素值。相关文档：处理高分辨率图像。
         * * vl_high_resolution_images：true，使用固定分辨率策略，忽略 max_pixels 设置，超过此分辨率时会将图像总像素缩小至此上限内。
         * * vl_high_resolution_images为false，实际分辨率由 max_pixels 与默认上限共同决定，取二者计算结果的最大值。超过此像素上限时会将图像缩小至此上限内。
         */
        public Boolean getVlHighResolutionImages() {
            return vlHighResolutionImages;
        }

        /**
         * 是否将输入图像的像素上限提升至 16384 Token 对应的像素值。相关文档：处理高分辨率图像。
         * vl_high_resolution_images：true，使用固定分辨率策略，忽略 max_pixels 设置，超过此分辨率时会将图像总像素缩小至此上限内。
         * vl_high_resolution_images为false，实际分辨率由 max_pixels 与默认上限共同决定，取二者计算结果的最大值。超过此像素上限时会将图像缩小至此上限内。
         *
         * @param vlHighResolutionImages 是否将输入图像的像素上限提升至 16384 Token 对应的像素值。相关文档：处理高分辨率图像。
         *                               * vl_high_resolution_images：true，使用固定分辨率策略，忽略 max_pixels 设置，超过此分辨率时会将图像总像素缩小至此上限内。
         *                               * vl_high_resolution_images为false，实际分辨率由 max_pixels 与默认上限共同决定，取二者计算结果的最大值。超过此像素上限时会将图像缩小至此上限内。
         */
        public void setVlHighResolutionImages(Boolean vlHighResolutionImages) {
            this.vlHighResolutionImages = vlHighResolutionImages;
        }

        /**
         * 生成响应的数量，取值范围是1-4。适用于需生成多个候选响应的场景，例如创意写作或广告文案。
         * （可选） 默认值为1
         * 若传入 tools 参数， 请将n 设为 1。
         * 增大 n 会增加输出 Token 的消耗，但不增加输入 Token 消耗。
         *
         * @return * 生成响应的数量，取值范围是1-4。适用于需生成多个候选响应的场景，例如创意写作或广告文案。
         * * （可选） 默认值为1
         * * 若传入 tools 参数， 请将n 设为 1。
         * * 增大 n 会增加输出 Token 的消耗，但不增加输入 Token 消耗。
         */
        public Integer getN() {
            return n;
        }

        /**
         * 生成响应的数量，取值范围是1-4。适用于需生成多个候选响应的场景，例如创意写作或广告文案。
         * （可选） 默认值为1
         * 若传入 tools 参数， 请将n 设为 1。
         * 增大 n 会增加输出 Token 的消耗，但不增加输入 Token 消耗。
         *
         * @param n * 生成响应的数量，取值范围是1-4。适用于需生成多个候选响应的场景，例如创意写作或广告文案。
         *          * （可选） 默认值为1
         *          * 若传入 tools 参数， 请将n 设为 1。
         *          * 增大 n 会增加输出 Token 的消耗，但不增加输入 Token 消耗。
         */
        public void setN(Integer n) {
            this.n = n;
        }

        /**
         * 是否开启指定前缀续写（Partial Mode）
         * 在代码补全、文本续写等场景中，需要模型从已有的文本片段（前缀）开始继续生成。
         * Partial Mode 可提供精确控制能力，确保模型输出的内容紧密衔接提供的前缀，提升生成结果的准确性与可控性。
         * 需在messages 数组中将最后一条消息的 role 设置为 assistant，并在其 content 中提供前缀，在此消息中设置参数 "partial": true。messages格式如下：
         * <pre>
         * [
         *     {
         *         "role": "user",
         *         "content": "请补全这个斐波那契函数，勿添加其它内容"
         *     },
         *     {
         *         "role": "assistant",
         *         "content": "def calculate_fibonacci(n):\n    if n == 1:\n        return n\n    else:\n",
         *         "partial": true
         *     }
         * ]
         * </pre>
         *
         * @return * 是否开启指定前缀续写（Partial Mode）
         * * 在代码补全、文本续写等场景中，需要模型从已有的文本片段（前缀）开始继续生成。
         * * Partial Mode 可提供精确控制能力，确保模型输出的内容紧密衔接提供的前缀，提升生成结果的准确性与可控性。
         * * 需在messages 数组中将最后一条消息的 role 设置为 assistant，并在其 content 中提供前缀，在此消息中设置参数 "partial": true。messages格式如下：
         * * <pre>
         *      * [
         *      *     {
         *      *         "role": "user",
         *      *         "content": "请补全这个斐波那契函数，勿添加其它内容"
         *      *     },
         *      *     {
         *      *         "role": "assistant",
         *      *         "content": "def calculate_fibonacci(n):\n    if n == 1:\n        return n\n    else:\n",
         *      *         "partial": true
         *      *     }
         *      * ]
         *      * </pre>
         */
        public Boolean getPartialMode() {
            return partialMode;
        }

        /**
         * 是否开启指定前缀续写（Partial Mode）
         * 在代码补全、文本续写等场景中，需要模型从已有的文本片段（前缀）开始继续生成。
         * Partial Mode 可提供精确控制能力，确保模型输出的内容紧密衔接提供的前缀，提升生成结果的准确性与可控性。
         * 需在messages 数组中将最后一条消息的 role 设置为 assistant，并在其 content 中提供前缀，在此消息中设置参数 "partial": true。messages格式如下：
         * <pre>
         * [
         *     {
         *         "role": "user",
         *         "content": "请补全这个斐波那契函数，勿添加其它内容"
         *     },
         *     {
         *         "role": "assistant",
         *         "content": "def calculate_fibonacci(n):\n    if n == 1:\n        return n\n    else:\n",
         *         "partial": true
         *     }
         * ]
         * </pre>
         *
         * @param partialMode * 是否开启指定前缀续写（Partial Mode）
         *                    * 在代码补全、文本续写等场景中，需要模型从已有的文本片段（前缀）开始继续生成。
         *                    * Partial Mode 可提供精确控制能力，确保模型输出的内容紧密衔接提供的前缀，提升生成结果的准确性与可控性。
         *                    * 需在messages 数组中将最后一条消息的 role 设置为 assistant，并在其 content 中提供前缀，在此消息中设置参数 "partial": true。messages格式如下：
         *                    * <pre>
         *                                                                                                                                           * [
         *                                                                                                                                           *     {
         *                                                                                                                                           *         "role": "user",
         *                                                                                                                                           *         "content": "请补全这个斐波那契函数，勿添加其它内容"
         *                                                                                                                                           *     },
         *                                                                                                                                           *     {
         *                                                                                                                                           *         "role": "assistant",
         *                                                                                                                                           *         "content": "def calculate_fibonacci(n):\n    if n == 1:\n        return n\n    else:\n",
         *                                                                                                                                           *         "partial": true
         *                                                                                                                                           *     }
         *                                                                                                                                           * ]
         *                                                                                                                                           * </pre>
         */
        public void setPartialMode(Boolean partialMode) {
            this.partialMode = partialMode;
        }

        @Override
        public Options clone() {
            try {
                return (Options) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new IllegalStateException(e.toString(), e);
            }
        }
    }
}
