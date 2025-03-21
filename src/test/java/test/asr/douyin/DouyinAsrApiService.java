package test.asr.douyin;


import test.asr.AsrApiService;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * 抖音：火山引擎/语音识别/一句话识别
 * ASR 服务使用的域名是 wss://openspeech.bytedance.com/api/v2/asr。
 * 接口文档：https://www.volcengine.com/docs/6561/80816
 */
//@Component
public class DouyinAsrApiService implements AsrApiService {
    private String url;
    private String appid;  // 项目的 appid
    private String token;  // 项目的 token
    private String cluster;  // 请求的集群

    public DouyinAsrApiService(
            String appid,  // 项目的 appid
            String token,  // 项目的 token
            String cluster) {
        this("wss://nls-gateway-cn-shanghai.aliyuncs.com/ws/v1", appid, token, cluster);
    }

    public DouyinAsrApiService(String url,
                               String appid,  // 项目的 appid
                               String token,  // 项目的 token
                               String cluster) {
        this.url = url;
        this.appid = appid;
        this.token = token;
        this.cluster = cluster;
    }

    @Override
    public CompletableFuture<String[]> audioToString(InputStream inputStream, String audio_format) {
        CompletableFuture<String[]> future = new CompletableFuture<>();
        try {
            Result[] result1 = parseAudio(inputStream, audio_format);
            String[] strings = new String[result1.length];
            for (int i = 0; i < result1.length; i++) {
                strings[i] = result1[i].text;
            }
            future.complete(strings);
        } catch (AsrException e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    public Tts stringToWav(String voice) throws AsrException {
        return null;
    }

    public Result[] parseAudio(InputStream inputStream, String audio_format) throws AsrException {
        Objects.requireNonNull(inputStream, "audioToString inputStream is null!");
        Objects.requireNonNull(audio_format, "audioToString audio_format is null!");
        AsrClient asrClient = null;
        try {
            asrClient = newClient(audio_format);
            byte[] b = new byte[16000];
            int len;
            AsrResponse asr_response = new AsrResponse();
            while ((len = inputStream.read(b)) > 0) {
                if (asrClient.isClosed()) {
                    byte[] errorMessage = asrClient.getErrorMessage();
                    if (errorMessage != null) {
                        throw new AsrException("asr fail! msg: " + new String(errorMessage));
                    } else {
                        throw new AsrException("asr fail!");
                    }
                }
                asr_response = asrClient.asr_send(Arrays.copyOfRange(b, 0, len), inputStream.available() == 0);
            }

//            AsrResponse response = asr_client.getAsrResponse();
            return Arrays.stream(asr_response.getResult()).map(e -> {
                Result result = new Result();
                result.text = (e.getText());
                result.confidence = (e.getConfidence());
                result.language = (e.getLanguage());
                result.utterances = (e.getUtterances());
                result.global_confidence = (e.getGlobal_confidence());
                return result;
            }).toArray(Result[]::new);
        } catch (Throwable e) {
            if (e instanceof AsrException) {
                throw (AsrException) e;
            } else {
                throw new AsrException("asr toString fail!", e);
            }
        } finally {
            try {
                inputStream.close();
            } catch (IOException ignored) {

            }
            if (asrClient != null) {
                asrClient.close();
            }
        }
    }

    private AsrClient newClient(String audio_format) throws IOException, NoSuchAlgorithmException, InvalidKeyException, InterruptedException, URISyntaxException {
        AsrClient asr_client = new AsrClient(url);
        asr_client.setAppid(appid);
        asr_client.setToken(token);
        asr_client.setCluster(cluster);
        asr_client.setFormat(audio_format);
        asr_client.setShow_utterances(true);
        asr_client.asr_sync_connect();
        return asr_client;
    }

    public String getAppid() {
        return appid;
    }

    public void setAppid(String appid) {
        this.appid = appid;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public static class Result {
        private String text;
        private int confidence;
        private String language;
        private AsrResponse.Utterances[] utterances;
        private float global_confidence;
    }

}
