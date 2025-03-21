package test.resumesdk.model;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 接口格式
 * 接口文档：https://www.resumesdk.com/docs/rs-parser.html#
 * 简历画像的输入为附件简历，其请求接口格式和简历解析完全一致，参考 简历解析请求接口 ；
 * 调用时仅需将请求url替换成上面简历画像对应的接口url；
 */
public class ParseRequest {
    /**
     * 必填
     * 若url中包含文件后缀，可不设置file_name;
     * 简历文件名。请务必带上正确的文件后缀名，否则部分简历可能解析失败。
     */
    private String fileName;
    /**
     * 任选其一必填【fileCont，fileUrl】字段
     * 2022.04新增基于url的请求方式：只需将file_cont替换为file_url字段（存放简历文件所在的url）即可，
     * 其余字段不变。若url中包含文件后缀，可不设置file_name;
     * 若url中不包含后缀，则请设置file_name字段，带上正确的文件后缀。
     */
    private String fileUrl;
    /**
     * 任选其一必填【fileCont，fileUrl】字段
     * 简历文件内容（以base64编码），其中：
     * 1）图片简历：based64编码后大小不超过8M，最短边至少100px，支持jpg/jpeg/png/bmp/tif/gif格式。
     * 图片越大OCR超时风险越高，建议大小不超过4M、长宽比小于4、分辨率600*800以上；
     * 2）非图片简历：based64编码后大小不超过10M（注：阿里云接口是不超过8M）；
     */
    private byte[] fileCont;
    /**
     * 可选
     * 是否需要解析头像，0为不需要，1为需要，默认为0。（注：解析头像会增加1倍左右耗时，如不需头像建议不开启）
     */
    private Boolean needAvatar;
    /**
     * 可选
     * 是否需要解析实践经历，0为不需要，1为需要，默认为0：
     * 1）若需要解析，则对“社会实践”及“在校活动”文本进行解析，解析结果放置在social_exp_objs字段中；
     */
    private Boolean needSocialExp;
    /**
     * 可选
     * ocr（图片解析所用到的文字识别）类型。（注：仅独立部署版需要，SaaS接口或者阿里云客户不需此字段）
     * ocr_type字段：根据配置的OCR服务进行选择
     * ocr_type=0：百度OCR；
     * ocr_type=1：腾讯OCR；
     * ocr_type=2：阿里读光OCR；
     * ocr_type=5：自定义OCR；
     */
    private Integer ocrType;

    /**
     * 可选
     * 接口版本，当前取值为0和1，默认为0：
     * 1）version=0：仅当字段在简历中有出现，才会在json结果中返回；
     * 2）version=1：不管字段在简历中有无出现，均在json结果中返回，若无出现则该字段取值为空；
     */
    private Integer version;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public byte[] getFileCont() {
        return fileCont;
    }

    public void setFileCont(byte[] fileCont) {
        this.fileCont = fileCont;
    }

    public Boolean getNeedAvatar() {
        return needAvatar;
    }

    public void setNeedAvatar(Boolean needAvatar) {
        this.needAvatar = needAvatar;
    }

    public Boolean getNeedSocialExp() {
        return needSocialExp;
    }

    public void setNeedSocialExp(Boolean needSocialExp) {
        this.needSocialExp = needSocialExp;
    }

    public Integer getOcrType() {
        return ocrType;
    }

    public void setOcrType(Integer ocrType) {
        this.ocrType = ocrType;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public Map<String, Object> toRequestBody() {
        Map<String, Object> body = new LinkedHashMap<>();
        if (fileName != null && !fileName.isEmpty()) {
            body.put("file_name", fileName);
        }
        if (fileUrl != null && !fileUrl.isEmpty()) {
            body.put("file_url", fileUrl);
        } else {
            body.put("file_cont", Base64.getEncoder().encodeToString(fileCont));
        }
        if (needAvatar != null) {
            body.put("need_avatar", needAvatar ? 1 : 0);
        }
        if (needSocialExp != null) {
            body.put("need_social_exp", needSocialExp ? 1 : 0);
        }
        if (ocrType != null) {
            body.put("ocr_type", ocrType);
        }
        if (version != null) {
            body.put("version", version);
        }
        return body;
    }
}
