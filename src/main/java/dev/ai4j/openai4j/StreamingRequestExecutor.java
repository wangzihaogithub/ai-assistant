package dev.ai4j.openai4j;

import com.github.aiassistant.platform.JsonUtil;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import okio.BufferedSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * wangzihao 2025-03-25
 * 大模型StreamingRequestExecutor#stream#RequestBody需要4.12.0, 其他需要okhttp3.14.9，这里重写StreamingRequestExecutor保持兼容
 *
 * @param <Request>         Request
 * @param <Response>        Response
 * @param <ResponseContent> ResponseContent
 */
class StreamingRequestExecutor<Request, Response, ResponseContent> {

    private static final JsonUtil.ObjectWriter objectWriter = JsonUtil.objectWriter();
    private static final JsonUtil.ObjectReader objectReader = JsonUtil.objectReader();
    private static final Logger log = LoggerFactory.getLogger(StreamingRequestExecutor.class);
    private static final MediaType mediaType = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient okHttpClient;
    private final String endpointUrl;
    private final Supplier<Request> requestWithStreamSupplier;
    private final Class<Response> responseClass;
    private final Function<Response, ResponseContent> streamEventContentExtractor;
    private final boolean logStreamingResponses;
    private final ResponseLoggingInterceptor responseLogger = new ResponseLoggingInterceptor();

    StreamingRequestExecutor(
            OkHttpClient okHttpClient,
            String endpointUrl,
            Supplier<Request> requestWithStreamSupplier,
            Class<Response> responseClass,
            Function<Response, ResponseContent> streamEventContentExtractor,
            boolean logStreamingResponses
    ) {
        this.okHttpClient = okHttpClient;
        this.endpointUrl = endpointUrl;
        this.requestWithStreamSupplier = requestWithStreamSupplier;
        this.responseClass = responseClass;
        this.streamEventContentExtractor = streamEventContentExtractor;
        this.logStreamingResponses = logStreamingResponses;
    }

    StreamingResponseHandling onPartialResponse(Consumer<ResponseContent> partialResponseHandler) {

        return new StreamingResponseHandling() {

            @Override
            public StreamingCompletionHandling onComplete(Runnable streamingCompletionCallback) {
                return new StreamingCompletionHandling() {

                    @Override
                    public ErrorHandling onError(Consumer<Throwable> errorHandler) {
                        return new ErrorHandling() {

                            @Override
                            public ResponseHandle execute() {
                                return stream(
                                        partialResponseHandler,
                                        streamingCompletionCallback,
                                        errorHandler
                                );
                            }
                        };
                    }

                    @Override
                    public ErrorHandling ignoreErrors() {
                        return new ErrorHandling() {

                            @Override
                            public ResponseHandle execute() {
                                return stream(
                                        partialResponseHandler,
                                        streamingCompletionCallback,
                                        (e) -> {
                                            // intentionally ignoring because user called ignoreErrors()
                                        }
                                );
                            }
                        };
                    }
                };
            }

            @Override
            public ErrorHandling onError(Consumer<Throwable> errorHandler) {
                return new ErrorHandling() {

                    @Override
                    public ResponseHandle execute() {
                        return stream(
                                partialResponseHandler,
                                () -> {
                                    // intentionally ignoring because user did not provide callback
                                },
                                errorHandler
                        );
                    }
                };
            }

            @Override
            public ErrorHandling ignoreErrors() {
                return new ErrorHandling() {

                    @Override
                    public ResponseHandle execute() {
                        return stream(
                                partialResponseHandler,
                                () -> {
                                    // intentionally ignoring because user did not provide callback
                                },
                                (e) -> {
                                    // intentionally ignoring because user called ignoreErrors()
                                }
                        );
                    }
                };
            }
        };
    }

    private ResponseHandle stream(
            Consumer<ResponseContent> partialResponseHandler,
            Runnable streamingCompletionCallback,
            Consumer<Throwable> errorHandler
    ) {

        Request request = requestWithStreamSupplier.get();


        okhttp3.Request okHttpRequest = new okhttp3.Request.Builder()
                .url(endpointUrl)
                // wangzihao 2025-03-25
                // 大模型StreamingRequestExecutor#stream#RequestBody需要4.12.0, 其他需要okhttp3.14.9，这里重写StreamingRequestExecutor保持兼容
                .post(new RequestBody() {
                    @Override
                    public MediaType contentType() {
                        return mediaType;
                    }

                    @Override
                    public void writeTo(BufferedSink bufferedSink) throws IOException {
                        objectWriter.writeValue(new OutputStream() {
                            @Override
                            public void write(int b) throws IOException {
                                bufferedSink.write(new byte[]{(byte) b}, 0, 1);
                            }

                            @Override
                            public void write(byte[] b, int off, int len) throws IOException {
                                bufferedSink.write(b, off, len);
                            }

                            @Override
                            public void flush() throws IOException {
                                bufferedSink.flush();
                            }
                        }, request);
                    }
                })
                .build();

        ResponseHandle responseHandle = new ResponseHandle();

        EventSourceListener eventSourceListener = new EventSourceListener() {

            @Override
            public void onOpen(EventSource eventSource, okhttp3.Response response) {
                if (responseHandle.cancelled) {
                    eventSource.cancel();
                    return;
                }

                if (logStreamingResponses) {
                    responseLogger.log(response);
                }
            }

            @Override
            public void onEvent(EventSource eventSource, String id, String type, String data) {
                if (responseHandle.cancelled) {
                    eventSource.cancel();
                    return;
                }

                if (logStreamingResponses) {
                    log.debug("onEvent() {}", data);
                }

                if ("[DONE]".equals(data)) {
                    streamingCompletionCallback.run();
                    return;
                }

                try {
                    Response response = objectReader.readValue(data, responseClass);
                    ResponseContent responseContent = streamEventContentExtractor.apply(response);
                    if (responseContent != null) {
                        partialResponseHandler.accept(responseContent); // do not handle exception, fail-fast
                    }
                } catch (Exception e) {
                    errorHandler.accept(e);
                }
            }

            @Override
            public void onClosed(EventSource eventSource) {
//                if (responseHandle.cancelled) {
//                    eventSource.cancel();
//                    return;
//                }

                if (logStreamingResponses) {
                    log.debug("onClosed()");
                }
            }

            @Override
            public void onFailure(EventSource eventSource, Throwable t, okhttp3.Response response) {
                if (responseHandle.cancelled) {
                    return;
                }

                // TODO remove this when migrating from okhttp
                if (t instanceof IllegalArgumentException && "byteCount < 0: -1".equals(t.getMessage())) {
                    streamingCompletionCallback.run();
                    return;
                }

                if (logStreamingResponses) {
                    log.debug("onFailure()", t);
                    responseLogger.log(response);
                }

                OpenAiHttpException openAiHttpException = null;
                if (response != null) {
                    String bodyString;
                    int code = response.code();
                    try {
                        bodyString = response.body().string();
                    } catch (Exception e) {
                        bodyString = "response.body().string() fail:" + e.toString();
                    }
                    openAiHttpException = new OpenAiHttpException(code, bodyString);
                }
                if (t != null) {
                    if (openAiHttpException != null) {
                        openAiHttpException.initCause(t);
                        errorHandler.accept(openAiHttpException);
                    } else {
                        errorHandler.accept(t);
                    }
                } else if (openAiHttpException != null) {
                    errorHandler.accept(openAiHttpException);
                } else {
                    errorHandler.accept(new OpenAiHttpException(400, "onFailure"));
                }
            }
        };

        EventSources.createFactory(okHttpClient)
                .newEventSource(okHttpRequest, eventSourceListener);

        return responseHandle;
    }
}
