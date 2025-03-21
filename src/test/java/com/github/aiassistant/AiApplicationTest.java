package com.github.aiassistant;

import com.github.aiassistant.entity.AiAssistant;
import com.github.aiassistant.entity.model.chat.AiChatResp;
import com.github.aiassistant.entity.model.chat.ChatQueryRequest;
import com.github.aiassistant.entity.model.chat.MemoryIdVO;
import com.github.aiassistant.entity.model.user.AiAccessUserVO;
import com.github.aiassistant.enums.AiAssistantStatusEnum;
import com.github.aiassistant.enums.AiChatUidTypeEnum;
import com.github.aiassistant.platform.SpringWebSseEmitterResponseHandler;
import com.github.aiassistant.service.text.ToolCallStreamingResponseHandler;
import com.github.aiassistant.service.text.repository.JdbcSessionMessageRepository;
import com.github.aiassistant.service.text.tools.Tools;
import com.github.aiassistant.serviceintercept.AccessUserServiceIntercept;
import com.github.aiassistant.serviceintercept.ServiceIntercept;
import com.mysql.cj.jdbc.MysqlDataSource;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.sql.DataSource;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class AiApplicationTest {

    private static DataSource dataSource() throws Exception {
        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setUrl("jdbc:mysql://rm-2ze3p58061s26ev8i1o.mysql.rds.aliyuncs.com:3306/cnwy?allowMultiQueries=true&useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai");
        dataSource.setUser("test_cnwy");
        dataSource.setPassword("ZnC+7PVvGXG4");
        dataSource.setZeroDateTimeBehavior("CONVERT_TO_NULL");
        return dataSource;
    }

    private static RestClient embeddingStore() {
        return RestClient.builder(HttpHost.create("http://elasticsearch8.cnwyjob.com"))
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
                        public Serializable getCurrentUserId() {
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
        return new AiApplication(dataSource, embeddingStore, toolsMap, interceptMap);
    }

    public static void main(String[] args) throws Exception {
        AiChatUidTypeEnum uidTypeEnum = AiChatUidTypeEnum.create("student");
        AiApplication aiApplication = application();

        // 智能体
        Collection<AiAssistant> assistantList = aiApplication.getAiAssistantService().selectList(AiAssistantStatusEnum.enable);
        Collection<AiAssistant> assistantLis1t = aiApplication.getAiAssistantService().selectList(AiAssistantStatusEnum.enable);

        AiAssistant assistant = assistantList.iterator().next();

        // 会话
        AiChatResp chat = aiApplication.getAiChatService().insert(assistant.getId(), "", 1, uidTypeEnum);

        // 提问
        ChatQueryRequest chatQueryRequest = new ChatQueryRequest();
        chatQueryRequest.setQuestion("你好");
        chatQueryRequest.setChatId(chat.getId());
        chatQueryRequest.setWebsearch(true);
        chatQueryRequest.setUserQueryTraceNumber(ChatQueryRequest.newUserQueryTraceNumber());

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
            public void onToken(String token, int baseMessageIndex, int addMessageCount) {
                super.onToken(token, baseMessageIndex, addMessageCount);
                System.out.println(token);
            }

            @Override
            protected void afterSendToClient(Emitter emitter, String name, Object... data) {
                System.out.println("name = " + name);
            }

            @Override
            public void onError(Throwable error, int baseMessageIndex, int addMessageCount, int generateCount) {
                super.onError(error, baseMessageIndex, addMessageCount, generateCount);
                error.printStackTrace();
            }
        };
        // 提问
        CompletableFuture<ToolCallStreamingResponseHandler> question = aiApplication.getLlmTextApiService().question(currentUser, messageRepository,
                chatQueryRequest.getQuestion(),
                chatQueryRequest.getWebsearch(),
                chatQueryRequest.getReasoning(),
                memoryId,
                responseHandler
        );
        // 提问完成
        question.thenAccept(h -> h.generate()
                .thenAccept(unused -> messageRepository.commit()
                        .whenComplete((o, throwable) -> responseHandler.sendComplete(throwable))));

        // 查看结果
        while (true) {
            Thread.yield();
        }
    }

}
