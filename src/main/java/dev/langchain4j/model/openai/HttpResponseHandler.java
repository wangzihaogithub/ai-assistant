package dev.langchain4j.model.openai;

import okhttp3.Call;

/**
 * Http返回监听，在吐字前(你可以在这里记日志)
 */
public interface HttpResponseHandler {

    void onHttpResponse(Call call, okhttp3.Response response);

}
