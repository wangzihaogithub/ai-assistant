package com.github.aiassistant.entity.model.langchain4j;

import dev.langchain4j.data.image.Image;
import dev.langchain4j.data.message.ImageContent;

import java.net.URI;

/**
 * 关于通义千问OCR模型进行文字提取更多用法，请参见文字提取。
 * <a href="https://help.aliyun.com/zh/model-studio/qwen-vl-ocr">https://help.aliyun.com/zh/model-studio/qwen-vl-ocr</a>
 */
public class OcrImageContent extends ImageContent {
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

    public OcrImageContent(URI url) {
        super(url);
    }

    public OcrImageContent(String url) {
        super(url);
    }

    public OcrImageContent(URI url, DetailLevel detailLevel) {
        super(url, detailLevel);
    }

    public OcrImageContent(String url, DetailLevel detailLevel) {
        super(url, detailLevel);
    }

    public OcrImageContent(String base64Data, String mimeType) {
        super(base64Data, mimeType);
    }

    public OcrImageContent(String base64Data, String mimeType, DetailLevel detailLevel) {
        super(base64Data, mimeType, detailLevel);
    }

    public OcrImageContent(Image image) {
        super(image);
    }

    public OcrImageContent(Image image, DetailLevel detailLevel) {
        super(image, detailLevel);
    }

    public OcrImageContent(URI url, Integer minPixels, Integer maxPixels) {
        super(url);
        this.minPixels = minPixels;
        this.maxPixels = maxPixels;
    }

    public OcrImageContent(String url, Integer minPixels, Integer maxPixels) {
        super(url);
        this.minPixels = minPixels;
        this.maxPixels = maxPixels;
    }

    public OcrImageContent(URI url, DetailLevel detailLevel, Integer minPixels, Integer maxPixels) {
        super(url, detailLevel);
        this.minPixels = minPixels;
        this.maxPixels = maxPixels;
    }

    public OcrImageContent(String url, DetailLevel detailLevel, Integer minPixels, Integer maxPixels) {
        super(url, detailLevel);
        this.minPixels = minPixels;
        this.maxPixels = maxPixels;
    }

    public OcrImageContent(String base64Data, String mimeType, Integer minPixels, Integer maxPixels) {
        super(base64Data, mimeType);
        this.minPixels = minPixels;
        this.maxPixels = maxPixels;
    }

    public OcrImageContent(String base64Data, String mimeType, DetailLevel detailLevel, Integer minPixels, Integer maxPixels) {
        super(base64Data, mimeType, detailLevel);
        this.minPixels = minPixels;
        this.maxPixels = maxPixels;
    }

    public OcrImageContent(Image image, Integer minPixels, Integer maxPixels) {
        super(image);
        this.minPixels = minPixels;
        this.maxPixels = maxPixels;
    }

    public OcrImageContent(Image image, DetailLevel detailLevel, Integer minPixels, Integer maxPixels) {
        super(image, detailLevel);
        this.minPixels = minPixels;
        this.maxPixels = maxPixels;
    }

    public Integer getMinPixels() {
        return minPixels;
    }

    public void setMinPixels(Integer minPixels) {
        this.minPixels = minPixels;
    }

    public Integer getMaxPixels() {
        return maxPixels;
    }

    public void setMaxPixels(Integer maxPixels) {
        this.maxPixels = maxPixels;
    }
}
