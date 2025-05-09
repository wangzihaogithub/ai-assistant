package dev.ai4j.openai4j.chat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.List;
import java.util.Objects;

@JsonDeserialize(builder = Content.Builder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public final class Content {

    @JsonProperty
    private final ContentType type;
    @JsonProperty
    private final String text;
    @JsonProperty
    private final ImageUrl imageUrl;
    /**
     * video array
     * 输入的图片列表形式的视频信息。使用方法请参见：视频理解（Qwen-VL）、视频理解（QVQ）或视频理解（Qwen-Omni）。
     */
    @JsonProperty
    private final List<String> video;
    /**
     * 当使用Qwen-Omni模型，且当type为"input_audio"时，是必选参数。
     */
    @JsonProperty
    private final InputAudio inputAudio;
    /**
     * 输入的视频文件信息。
     * 当使用Qwen-VL 模型、QVQ模型或Qwen-Omni 模型，且type参数为"video_url"时是必选参数。
     * 对于Qwen-VL 模型，qwen-vl-max、qwen-vl-plus、qwen-vl-max-latest、qwen-vl-max-1119、qwen-vl-max-2025-01-25、qwen-vl-max-2025-04-02、qwen2.5-vl-32b-instruct、qwen2.5-vl-72b-instruct模型支持直接传入视频文件；如需体验qwen-vl-max-0809、qwen-vl-max-1030、qwen-vl-plus-latest、qwen-vl-plus-0809、qwen2.5-vl-3b-instruct、qwen2.5-vl-7b-instruct模型，请提交工单进行申请。
     * 对于QVQ和Qwen-Omni 模型，可直接传入视频文件。
     * Qwen-Omni 模型可以理解视频文件中的视觉与音频信息。
     */
    @JsonProperty
    private final VideoUrl videoUrl;
    /**
     * 当通义千问OCR模型限制输入图像的最小像素时需要设置的参数。
     * 与image_url参数一起使用，默认值：3136，最小值：100。
     * 当输入图像像素小于min_pixels时，会将图像按原比例放大，直到总像素高于min_pixels。
     */
    @JsonProperty
    private Integer minPixels;
    /**
     * 当通义千问OCR模型限制输入图像的最大像素时需要设置的参数。
     * 与image_url参数一起使用，默认值：6422528，最大值：23520000。
     * 当输入图像像素在[min_pixels, max_pixels]区间内时，模型会按原图进行识别。当输入图像像素大于max_pixels时，会将图像按原比例缩小，直到总像素低于max_pixels。
     */
    @JsonProperty
    private Integer maxPixels;

    public Content(Builder builder) {
        this.type = builder.type;
        this.text = builder.text;
        this.imageUrl = builder.imageUrl;
        this.minPixels = builder.minPixels;
        this.maxPixels = builder.maxPixels;
        this.video = builder.video;
        this.inputAudio = builder.inputAudio;
        this.videoUrl = builder.videoUrl;
    }

    public static Builder builder() {
        return new Builder();
    }

    public ContentType type() {
        return type;
    }

    public String text() {
        return text;
    }

    public VideoUrl videoUrl() {
        return videoUrl;
    }

    public ImageUrl imageUrl() {
        return imageUrl;
    }

    public Integer maxPixels() {
        return maxPixels;
    }

    public Integer minPixels() {
        return minPixels;
    }

    public List<String> video() {
        return video;
    }

    public InputAudio inputAudio() {
        return inputAudio;
    }

    @Override
    public boolean equals(Object another) {
        if (this == another) return true;
        return another instanceof Content
                && equalTo((Content) another);
    }

    private boolean equalTo(Content another) {
        return Objects.equals(type, another.type)
                && Objects.equals(text, another.text)
                && Objects.equals(imageUrl, another.imageUrl);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + Objects.hashCode(type);
        h += (h << 5) + Objects.hashCode(text);
        h += (h << 5) + Objects.hashCode(imageUrl);
        return h;
    }

    @Override
    public String toString() {
        return "Content{" +
                "type=" + type +
                ", text=" + text +
                ", imageUrl=" + imageUrl +
                "}";
    }

    @JsonPOJOBuilder(withPrefix = "")
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static final class Builder {

        private ContentType type;
        private String text;
        private ImageUrl imageUrl;
        /**
         * video array
         * 输入的图片列表形式的视频信息。使用方法请参见：视频理解（Qwen-VL）、视频理解（QVQ）或视频理解（Qwen-Omni）。
         */
        private List<String> video;
        /**
         * 当使用Qwen-Omni模型，且当type为"input_audio"时，是必选参数。
         */
        private InputAudio inputAudio;
        /**
         * 输入的视频文件信息。
         * 当使用Qwen-VL 模型、QVQ模型或Qwen-Omni 模型，且type参数为"video_url"时是必选参数。
         * 对于Qwen-VL 模型，qwen-vl-max、qwen-vl-plus、qwen-vl-max-latest、qwen-vl-max-1119、qwen-vl-max-2025-01-25、qwen-vl-max-2025-04-02、qwen2.5-vl-32b-instruct、qwen2.5-vl-72b-instruct模型支持直接传入视频文件；如需体验qwen-vl-max-0809、qwen-vl-max-1030、qwen-vl-plus-latest、qwen-vl-plus-0809、qwen2.5-vl-3b-instruct、qwen2.5-vl-7b-instruct模型，请提交工单进行申请。
         * 对于QVQ和Qwen-Omni 模型，可直接传入视频文件。
         * Qwen-Omni 模型可以理解视频文件中的视觉与音频信息。
         */
        private VideoUrl videoUrl;
        /**
         * 当通义千问OCR模型限制输入图像的最小像素时需要设置的参数。
         * 与image_url参数一起使用，默认值：3136，最小值：100。
         * 当输入图像像素小于min_pixels时，会将图像按原比例放大，直到总像素高于min_pixels。
         */
        private Integer minPixels;
        /**
         * 当通义千问OCR模型限制输入图像的最大像素时需要设置的参数。
         * 与image_url参数一起使用，默认值：6422528，最大值：23520000。
         * 当输入图像像素在[min_pixels, max_pixels]区间内时，模型会按原图进行识别。当输入图像像素大于max_pixels时，会将图像按原比例缩小，直到总像素低于max_pixels。
         */
        private Integer maxPixels;

        public Builder videoUrl(VideoUrl videoUrl) {
            this.videoUrl = videoUrl;
            return this;
        }

        public Builder video(List<String> video) {
            this.video = video;
            return this;
        }

        public Builder inputAudio(InputAudio inputAudio) {
            this.inputAudio = inputAudio;
            return this;
        }

        public Builder type(ContentType type) {
            this.type = type;
            return this;
        }

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public Builder imageUrl(ImageUrl imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        public Builder minPixels(Integer minPixels) {
            this.minPixels = minPixels;
            return this;
        }

        public Builder maxPixels(Integer maxPixels) {
            this.maxPixels = maxPixels;
            return this;
        }

        public Content build() {
            return new Content(this);
        }
    }
}
