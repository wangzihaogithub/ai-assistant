package com.github.aiassistant.service.text;

import com.github.aiassistant.util.AiUtil;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.ChatMessage;

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
     * 克隆一个新的对象
     *
     * @return 新的对象
     */
    @Override
    public GenerateRequest clone() {
        try {
            GenerateRequest clone = (GenerateRequest) super.clone();
            return clone;
        } catch (CloneNotSupportedException ignored) {
            return null;
        }
    }

    @Override
    public String toString() {
        return AiUtil.getLastUserQuestion(messageList);
    }
}
