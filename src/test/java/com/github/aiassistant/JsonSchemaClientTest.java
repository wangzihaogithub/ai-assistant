package com.github.aiassistant;

import com.github.aiassistant.exception.JsonSchemaCreateException;
import com.github.aiassistant.service.jsonschema.LlmJsonSchemaApiService;
import com.github.aiassistant.service.jsonschema.ReasoningJsonSchema;
import com.github.aiassistant.service.text.tools.Tools;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class JsonSchemaClientTest {
    public static void main(String[] args) throws JsonSchemaCreateException {
        DataSource dataSource = AiBuilders.mysqlDataSource(
                "jdbc:mysql://rm-2zeq4k6xxxxxxxx.mysql.rds.aliyuncs.com:3306/xxx?useUnicode=true&characterEncoding=utf-8&useSSL=false&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=Asia/Shanghai&allowMultiQueries=true&rewriteBatchedStatements=true&zeroDateTimeBehavior=CONVERT_TO_NULL",
                "xxx",
                "xxx"
        );

        LlmJsonSchemaApiService jsonSchemaApiService = AiBuilders.jsonSchemaApiService(AiBuilders.daoProvider(dataSource), new Function<String, Tools>() {
            @Override
            public Tools apply(String s) {
                return null;
            }
        });


        ReasoningJsonSchema reasoningJsonSchema = jsonSchemaApiService.getSchemaByIdNoMemory(3, ReasoningJsonSchema.class);

        CompletableFuture<Map<String, Object>> future = reasoningJsonSchema.parse("帮我做一份旅游计划").toMapFuture();
        Map<String, Object> join = future.join();
        System.out.println(join);
    }
}
