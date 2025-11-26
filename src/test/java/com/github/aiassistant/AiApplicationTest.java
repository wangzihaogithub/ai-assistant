package com.github.aiassistant;

import com.github.aiassistant.entity.AiAssistant;
import com.github.aiassistant.entity.model.chat.AiChatResp;
import com.github.aiassistant.entity.model.chat.ChatQueryReq;
import com.github.aiassistant.entity.model.chat.MemoryIdVO;
import com.github.aiassistant.entity.model.user.AiAccessUserVO;
import com.github.aiassistant.enums.AiAssistantStatusEnum;
import com.github.aiassistant.enums.AiChatUidTypeEnum;
import com.github.aiassistant.platform.SpringWebSseEmitterResponseHandler;
import com.github.aiassistant.service.text.FunctionCallStreamingResponseHandler;
import com.github.aiassistant.service.text.embedding.KnnApiService;
import com.github.aiassistant.service.text.repository.JdbcSessionMessageRepository;
import com.github.aiassistant.service.text.sseemitter.AiMessageString;
import com.github.aiassistant.service.text.tools.Tools;
import com.github.aiassistant.serviceintercept.AccessUserServiceIntercept;
import com.github.aiassistant.serviceintercept.ServiceIntercept;
import com.mysql.cj.jdbc.MysqlDataSource;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.sql.DataSource;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class AiApplicationTest {

    private static DataSource dataSource() throws Exception {
        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setUrl("jdbc:mysql://rm-xx.mysql.rds.aliyuncs.com:3306/xxx?allowMultiQueries=true&useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai");
        dataSource.setUser("xxx");
        dataSource.setPassword("xxx");
        dataSource.setZeroDateTimeBehavior("CONVERT_TO_NULL");
        return dataSource;
    }

    private static RestClient embeddingStore() {
        return RestClient.builder(HttpHost.create("http://elasticsearch8.xxx.com"))
                .build();
    }

    private static Function<String, Tools> toolsMap() {
        return new Function<String, Tools>() {

            @Override
            public Tools apply(String s) {
                return null;
            }
        };
    }

    private static Function<Class<? extends ServiceIntercept>, Collection<ServiceIntercept>> interceptMap() {
        return new Function<Class<? extends ServiceIntercept>, Collection<ServiceIntercept>>() {
            @Override
            public Collection<ServiceIntercept> apply(Class<? extends ServiceIntercept> aClass) {
                if (aClass == AccessUserServiceIntercept.class) {
                    return Arrays.asList(new AccessUserServiceIntercept() {
                        @Override
                        public Integer getCurrentUserId() {
                            return 1;
                        }
                    });
                }
                return Collections.emptyList();
            }
        };
    }

    private static AiApplication application() throws Exception {
        DataSource dataSource = dataSource();
        RestClient embeddingStore = embeddingStore();
        Function<String, Tools> toolsMap = toolsMap();
        Function<Class<? extends ServiceIntercept>, Collection<ServiceIntercept>> interceptMap = interceptMap();
        return new AiApplication(dataSource, new KnnApiService(embeddingStore), toolsMap, interceptMap);
    }

    public static void main(String[] args) throws Exception {
        new AiApplicationTest().run();
    }

    @Test
    public void run() throws Exception {
        AiChatUidTypeEnum uidTypeEnum = AiChatUidTypeEnum.create("student");
        AiApplication aiApplication = application();

        // 智能体
        Collection<AiAssistant> assistantList = aiApplication.getAiAssistantService().selectList(AiAssistantStatusEnum.enable);

        AiAssistant assistant = assistantList.iterator().next();

        // 会话
        AiChatResp chat = aiApplication.getAiChatService().insert(assistant.getId(), "", 1, uidTypeEnum, null);

        // 提问
        ChatQueryReq chatQueryRequest = new ChatQueryReq();
        chatQueryRequest.setQuestion("你好");
        chatQueryRequest.setChatId(chat.getId());
        chatQueryRequest.setWebsearch(true);
        chatQueryRequest.setUserQueryTraceNumber(ChatQueryReq.newUserQueryTraceNumber());

        // 登陆用户
        AiAccessUserVO currentUser = aiApplication.getAccessUserService().getCurrentUser();
        // 记忆
        MemoryIdVO memoryId = aiApplication.getAccessUserService().getMemoryId(chatQueryRequest.getChatId(), currentUser.getId(), uidTypeEnum);
        // 持久化
        JdbcSessionMessageRepository messageRepository = aiApplication.newJdbcSessionMessageRepository(chatQueryRequest, memoryId, currentUser);
        // 前端推送
        SseEmitter emitter = new SseEmitter();
        // 业务处理
        SpringWebSseEmitterResponseHandler responseHandler = new SpringWebSseEmitterResponseHandler(emitter, false, chatQueryRequest.getUserQueryTraceNumber(), chatQueryRequest.getWebsearch()) {
            @Override
            public void onToken(AiMessageString token, int baseMessageIndex, int addMessageCount) {
                super.onToken(token, baseMessageIndex, addMessageCount);
                System.out.println(token);
            }

            @Override
            protected void afterSendToClient(Emitter emitter, int id, String name, Map<Object, Object> map) {
                System.out.println("name = " + name);
            }

            @Override
            public void onError(Throwable error, int baseMessageIndex, int addMessageCount, int generateCount) {
                super.onError(error, baseMessageIndex, addMessageCount, generateCount);
                error.printStackTrace();
            }
        };
        // 提问
        CompletableFuture<FunctionCallStreamingResponseHandler> question = aiApplication.getLlmTextApiService().question(currentUser, messageRepository,
                chatQueryRequest.getQuestion(),
                chatQueryRequest.getWebsearch(),
                chatQueryRequest.getReasoning(),
                memoryId,
                responseHandler
        );
        // 提问
        question.thenAccept(
                        // 生成内容
                        h -> h.generate().thenAccept(
                                // 持久化
                                unused -> messageRepository.commit().whenComplete(
                                        // 告诉前端结束
                                        (o, throwable) -> responseHandler.sendComplete(throwable))))
                .exceptionally(throwable -> {
                    // 提问错误
                    responseHandler.onError(throwable, 0, 0, 0);
                    return null;
                });

        // 查看结果
        while (true) {
            Thread.yield();
        }
    }

}
