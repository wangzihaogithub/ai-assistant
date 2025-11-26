package com.github.aiassistant;

import com.github.aiassistant.entity.model.chat.WebSearchResultVO;
import com.github.aiassistant.platform.ApacheHttpClient;
import com.github.aiassistant.platform.OkHttp3Client;
import com.github.aiassistant.service.text.tools.functioncall.BaiduWebSearchTools;
import com.github.aiassistant.service.text.tools.functioncall.UrlReadTools;
import com.github.aiassistant.util.HttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class HttpClientTest {
    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        HttpClient ap = new OkHttp3Client("ap");

        HttpClient.HttpRequest request = ap.request("https://zhuanlan.zhihu.com/p/52572694", Collections.singletonMap("q", "qq"));

        HttpClient.HttpResponse httpResponse = request.connect().get();
        InputStream inputStream = httpResponse.getInputStream();

        UrlReadTools ss = new UrlReadTools("ss", 1000, 3000, 2);
        CompletableFuture<String> future = ss.readString("https://zhuanlan.zhihu.com/p/52572694");
        try {

            String s = future.get();
            System.out.println("inputStream = " + s);
        } catch (Throwable t) {

        }

        BaiduWebSearchTools baiduWebSearchTools = new BaiduWebSearchTools();
        CompletableFuture<WebSearchResultVO> future1 = baiduWebSearchTools.webSearch("解释下null", 10);
        try {
            WebSearchResultVO s = future1.get();
            System.out.println("inputStream = " + s);
        } catch (Throwable t) {

        }
        System.out.println("inputStream = ");
    }
}
