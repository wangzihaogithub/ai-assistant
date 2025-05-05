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
     * 开启联网搜索的参数
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
     * # 是否开启思考模式，适用于 Qwen3 模型。
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

    public Boolean getEnableSearch() {
        return enableSearch;
    }

    public void setEnableSearch(Boolean enableSearch) {
        this.enableSearch = enableSearch;
    }

    public Map<String, Object> getSearchOptions() {
        return searchOptions;
    }

    public void setSearchOptions(Map<String, Object> searchOptions) {
        this.searchOptions = searchOptions;
    }

    public Boolean getEnableThinking() {
        return enableThinking;
    }

    public void setEnableThinking(Boolean enableThinking) {
        this.enableThinking = enableThinking;
    }

    public Integer getThinkingBudget() {
        return thinkingBudget;
    }

    public void setThinkingBudget(Integer thinkingBudget) {
        this.thinkingBudget = thinkingBudget;
    }

    public List<String> getModalities() {
        return modalities;
    }

    public void setModalities(List<String> modalities) {
        this.modalities = modalities;
    }

    public List<ChatMessage> getMessageList() {
        return messageList;
    }

    public List<ToolSpecification> getToolSpecificationList() {
        return toolSpecificationList;
    }

    @Override
    public GenerateRequest clone() {
        try {
            GenerateRequest clone = (GenerateRequest) super.clone();
            return clone;
        } catch (CloneNotSupportedException ignored) {
            return new GenerateRequest();
        }
    }

    @Override
    public String toString() {
        return AiUtil.getLastUserQuestion(messageList);
    }
}
