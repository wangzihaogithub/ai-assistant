package test.asr;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

/**
 * 语音服务
 */
public interface AsrApiService {
    /**
     * 语音转文本
     */
    CompletableFuture<String[]> audioToString(InputStream inputStream, String audio_format);

    /**
     * tts-字符串转wav语音
     *
     * @param voice 发音人
     * @return 实时语音
     */
    Tts stringToWav(String voice) throws AsrException;

    public static interface Tts {
        void send(String text);

        void sendEnd();

        void listener(StringToAudioListener listener);
    }

    public static interface StringToAudioListener {
        void chunk(byte[] buffer) throws IOException;

        void end(Throwable throwable);
    }

    public static class AsrException extends IOException {
        public AsrException(String message, Throwable cause) {
            super(message, cause);
        }

        public AsrException(String message) {
            super(message);
        }
    }
}
