package test.asr.aliyun;

import test.asr.AsrApiService;
import com.alibaba.nls.client.AccessToken;
import com.alibaba.nls.client.protocol.InputFormatEnum;
import com.alibaba.nls.client.protocol.NlsClient;
import com.alibaba.nls.client.protocol.OutputFormatEnum;
import com.alibaba.nls.client.protocol.SampleRateEnum;
import com.alibaba.nls.client.protocol.asr.SpeechRecognizer;
import com.alibaba.nls.client.protocol.asr.SpeechRecognizerListener;
import com.alibaba.nls.client.protocol.asr.SpeechRecognizerResponse;
import com.alibaba.nls.client.protocol.tts.FlowingSpeechSynthesizer;
import com.alibaba.nls.client.protocol.tts.FlowingSpeechSynthesizerListener;
import com.alibaba.nls.client.protocol.tts.FlowingSpeechSynthesizerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

/**
 * 此示例演示了：
 * ASR一句话识别API调用。
 * 动态获取Token。获取Token具体操作，请参见：https://help.aliyun.com/document_detail/450514.html
 * 通过本地文件模拟实时流发送。
 * 识别耗时计算。
 */
public class AliyunAsrApiService implements AsrApiService {
    private static final Logger logger = LoggerFactory.getLogger(AliyunAsrApiService.class);
    private final String appKey;
    private final AccessToken accessToken;
    private final String url;
    private NlsClient client;

    public AliyunAsrApiService(String appKey, String id, String secret) throws IOException {
        this("wss://nls-gateway-cn-shanghai.aliyuncs.com/ws/v1", appKey, id, secret);
    }

    private AliyunAsrApiService(String url, String appKey, String id, String secret) throws IOException {
        this.appKey = appKey;
        this.url = url;
        accessToken = new AccessToken(id, secret);
        accessToken.apply();
    }

    //根据二进制数据大小计算对应的同等语音长度
    //sampleRate仅支持8000或16000。
    public static int getSleepDelta(int dataSize, int sampleRate) {
        // 仅支持16位采样。
        int sampleBytes = 16;
        // 仅支持单通道。
        int soundChannel = 1;
        return (dataSize * 10 * sampleRate) / (160 * sampleRate);
    }

    private NlsClient checkClient() throws IOException {
        //应用全局创建一个NlsClient实例，默认服务地址为阿里云线上服务地址。
        //获取Token，实际使用时注意在accessToken.getExpireTime()过期前再次获取。
        if (accessToken.getExpireTime() >= System.currentTimeMillis()) {
            accessToken.apply();
        }
        NlsClient client = this.client;
        if (client == null) {
            this.client = client = url.isEmpty() ? new NlsClient(accessToken.getToken()) : new NlsClient(url, accessToken.getToken());
        } else {
            client.setToken(accessToken.getToken());
        }
        return client;
    }

    @Override
    public CompletableFuture<String[]> audioToString(InputStream fis, String audio_format) {
        int sampleRate = 16000;
        CompletableFuture<String[]> future = new CompletableFuture<>();
        NlsClient client;
        try {
            client = checkClient();
        } catch (Exception e) {
            future.completeExceptionally(new AsrException("audioToString checkClient exception", e));
            return future;
        }
        try {
            //传递用户自定义参数
            SpeechRecognizerListener listener = new AudioToStringSpeechRecognizerListener(future);
            SpeechRecognizer recognizer = new SpeechRecognizer(client, listener);
            recognizer.setAppKey(appKey);
            //设置音频编码格式。如果是OPUS文件，请设置为InputFormatEnum.OPUS。
            recognizer.setFormat(audio_format == null || audio_format.isEmpty() ? InputFormatEnum.WAV : InputFormatEnum.valueOf(audio_format.toUpperCase()));
            //设置音频采样率
            if (sampleRate == 16000) {
                recognizer.setSampleRate(SampleRateEnum.SAMPLE_RATE_16K);
            } else if (sampleRate == 8000) {
                recognizer.setSampleRate(SampleRateEnum.SAMPLE_RATE_8K);
            }
            //设置是否返回中间识别结果
//            recognizer.setEnableIntermediateResult(true);
            //设置是否打开语音检测（即vad）
            recognizer.addCustomedParam("enable_voice_detection", true);
            //此方法将以上参数设置序列化为JSON发送给服务端，并等待服务端确认。
            long now = System.currentTimeMillis();
            recognizer.start();
            logger.info("ASR start latency : {} ms", System.currentTimeMillis() - now);
            byte[] b = new byte[3200];
            int len;
            while ((len = fis.read(b)) > 0) {
                recognizer.send(b, len);
                //本案例用读取本地文件的形式模拟实时获取语音流，因为读取速度较快，这里需要设置sleep时长。
                // 如果实时获取语音则无需设置sleep时长，如果是8k采样率语音第二个参数设置为8000。
                int deltaSleep = getSleepDelta(len, sampleRate);
                Thread.sleep(deltaSleep);
            }
            //通知服务端语音数据发送完毕，等待服务端处理完成。
            now = System.currentTimeMillis();
            recognizer.stop();
            //计算实际延迟，调用stop返回之后一般即是识别结果返回时间。
            logger.info("ASR stop latency : {} ms", System.currentTimeMillis() - now);
        } catch (Exception e) {
            future.completeExceptionally(e);
        } finally {
            try {
                fis.close();
            } catch (IOException ignored) {

            }
        }
        return future;
    }

    @Override
    public Tts stringToWav(String voice) throws AsrException {
        if (voice == null || voice.isEmpty()) {
            voice = "siyue";
        }

        NlsClient client;
        try {
            client = checkClient();
        } catch (Exception e) {
            throw new AsrException("stringToWav checkClient exception", e);
        }
        StringToAudioSpeechSynthesizerListener listener = new StringToAudioSpeechSynthesizerListener();
        try {
            //创建实例，建立连接。
            FlowingSpeechSynthesizer synthesizer = new FlowingSpeechSynthesizer(client, listener);
            listener.synthesizer = synthesizer;
            synthesizer.setAppKey(appKey);
            //设置返回音频的编码格式。
            synthesizer.setFormat(OutputFormatEnum.WAV);
            //设置返回音频的采样率。
            synthesizer.setSampleRate(SampleRateEnum.SAMPLE_RATE_16K);
            //发音人。
            synthesizer.setVoice(voice);
            //音量，范围是0~100，可选，默认50。
            synthesizer.setVolume(50);
            //语调，范围是-500~500，可选，默认是0。
            synthesizer.setPitchRate(0);
            //语速，范围是-500~500，默认是0。
            synthesizer.setSpeechRate(0);
            //设置连续两次发送文本的最小时间间隔（毫秒），如果当前调用send时距离上次调用时间小于此值，则会阻塞并等待直到满足条件再发送文本
            synthesizer.setMinSendIntervalMS(100);
            //等待语音合成结束
        } catch (Exception e) {
            throw new AsrException("stringToWav connect exception: " + e, e);
        }
        return listener;
    }

    static class AudioToStringSpeechRecognizerListener extends SpeechRecognizerListener {
        final CompletableFuture<String[]> future;

        AudioToStringSpeechRecognizerListener(CompletableFuture<String[]> future) {
            this.future = future;
        }

        private void close() {
            //关闭连接
            if (null != recognizer) {
                recognizer.close();
            }
        }

        //识别出中间结果。仅当setEnableIntermediateResult为true时，才会返回该消息。
        @Override
        public void onRecognitionResultChanged(SpeechRecognizerResponse response) {
            //getName是获取事件名称，getStatus是获取状态码，getRecognizedText是语音识别文本。
//             System.out.println("name: " + response.getName() + ", status: " + response.getStatus() + ", result: " + response.getRecognizedText());
        }

        //识别完毕
        @Override
        public void onRecognitionCompleted(SpeechRecognizerResponse response) {
            close();
            //getName是获取事件名称，getStatus是获取状态码，getRecognizedText是语音识别文本。
            // System.out.println("name: " + response.getName() + ", status: " + response.getStatus() + ", result: " + response.getRecognizedText());
            future.complete(new String[]{response.getRecognizedText()});
        }

        @Override
        public void onStarted(SpeechRecognizerResponse response) {
        }

        @Override
        public void onFail(SpeechRecognizerResponse response) {
            //关闭连接
            close();
            //task_id是调用方和服务端通信的唯一标识，当遇到问题时，需要提供此task_id。
            // System.out.println("task_id: " + response.getTaskId() + ", status: " + response.getStatus() + ", status_text: " + response.getStatusText());
            future.completeExceptionally(new AsrException(
                    "task_id: " + response.getTaskId() + ", status: " + response.getStatus() + ", status_text: " + response.getStatusText()));
        }
    }

    static class StringToAudioSpeechSynthesizerListener extends FlowingSpeechSynthesizerListener implements Tts {
        StringToAudioListener listener;
        FlowingSpeechSynthesizer synthesizer;

        volatile boolean close;

        //流式文本语音合成开始
        @Override
        public void onSynthesisStart(FlowingSpeechSynthesizerResponse response) {
            // System.out.println("name: " + response.getName() +
//                    ", status: " + response.getStatus());
        }

        //服务端检测到了一句话的开始
        @Override
        public void onSentenceBegin(FlowingSpeechSynthesizerResponse response) {
            // System.out.println("name: " + response.getName() +
//                    ", status: " + response.getStatus());
            // System.out.println("Sentence Begin");
        }

        //服务端检测到了一句话的结束，获得这句话的起止位置和所有时间戳
        @Override
        public void onSentenceEnd(FlowingSpeechSynthesizerResponse response) {
            // System.out.println("name: " + response.getName() +
//                    ", status: " + response.getStatus() + ", subtitles: " + response.getObject("subtitles"));

        }

        //流式文本语音合成结束
        @Override
        public void onSynthesisComplete(FlowingSpeechSynthesizerResponse response) {
            // 调用onSynthesisComplete时，表示所有TTS数据已经接收完成，所有文本都已经合成音频并返回。
            if (close) {
                return;
            }
            //关闭连接
            close();
            listener.end(null);
            //调用onComplete时表示所有TTS数据已接收完成，因此为整个合成数据的延迟。该延迟可能较大，不一定满足实时场景。
            // System.out.println("name: " + response.getName() +
//                    ", status: " + response.getStatus());
        }

        //收到语音合成的语音二进制数据
        @Override
        public void onAudioData(ByteBuffer message) {
            if (close) {
                return;
            }
            //计算首包语音流的延迟，收到第一包语音流时，即可以进行语音播放，以提升响应速度（特别是实时交互场景下）。
            byte[] bytesArray = new byte[message.remaining()];
            message.get(bytesArray, 0, bytesArray.length);
            try {
                listener.chunk(bytesArray);
            } catch (IOException e) {
                close();
                listener.end(e);
            }
        }

        //收到语音合成的增量音频时间戳
        @Override
        public void onSentenceSynthesis(FlowingSpeechSynthesizerResponse response) {

        }

        @Override
        public void onFail(FlowingSpeechSynthesizerResponse response) {
            // task_id是调用方和服务端通信的唯一标识，当遇到问题时，需要提供此task_id以便排查。
            if (close) {
                return;
            }
            //task_id是调用方和服务端通信的唯一标识，当遇到问题时需要提供task_id以便排查。
//                    // System.out.println(
//                            "task_id: " + response.getTaskId() +
//                                    //状态码 20000000 表示识别成功
//                                    ", status: " + response.getStatus() +
//                                    //错误信息
//                                    ", status_text: " + response.getStatusText());
            close();
            listener.end(new AsrException("tts error! session_id: " + getFlowingSpeechSynthesizer().getCurrentSessionId() +
                    ", task_id: " + response.getTaskId() +
                    //状态码
                    ", status: " + response.getStatus() +
                    //错误信息
                    ", status_text: " + response.getStatusText()));
        }

        private void close() {
            close = true;
            //关闭连接
            if (null != synthesizer) {
                synthesizer.close();
            }
        }

        @Override
        public void send(String text) {
            if (close) {
                return;
            }
            if (listener == null) {
                throw new IllegalStateException("listener is null");
            }
            synthesizer.send(text);
        }

        @Override
        public void sendEnd() {
            if (close) {
                return;
            }
            try {
                //通知服务端流式文本数据发送完毕，阻塞等待服务端处理完成。
                synthesizer.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void listener(StringToAudioListener listener) {
            if (close) {
                return;
            }
            this.listener = listener;
            //此方法将以上参数设置序列化为JSON发送给服务端，并等待服务端确认。
            long start = System.currentTimeMillis();
            try {
                synthesizer.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
            logger.info("tts start latency {} ms", System.currentTimeMillis() - start);
        }
    }
}