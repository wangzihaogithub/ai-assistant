package com.github.aiassistant.entity;

// @Data
// @TableName("kn_setting_websearch_blacklist")
public class KnSettingWebsearchBlacklist {
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public Long getSimilarity() {
        return similarity;
    }

    public void setSimilarity(Long similarity) {
        this.similarity = similarity;
    }

    public Integer getSorted() {
        return sorted;
    }

    public void setSorted(Integer sorted) {
        this.sorted = sorted;
    }

    // @TableId(value = "id", type = IdType.NONE)
    private String id;

    // @ApiModelProperty(value = "黑名单问题", required = true)
    private String question;

    // @ApiModelProperty(value = "相似度,0~100", required = true)
    private Long similarity;

    // @ApiModelProperty(value = "排序，从小到大，越容易命中的放前面", required = true)
    private Integer sorted;

}