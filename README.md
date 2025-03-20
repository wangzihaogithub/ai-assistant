# ai-assistant

#### 介绍

AI助手jar包，可二次开发复用 (基于langchain4j的java8版本开发)

#### 模块

`

         - entity(数据实体)
         - dao(数据访问)
         - serviceintercept（业务拦截器）
         - service（业务）
            - accessuser（登陆用户）
            - jsonschema（json模型）
            - text（文本模型）
              - reasoning（思考）
              - acting（行动）
              - memory（记忆）
              - chat（聊天）
              - embedding（向量模型）
              - repository（存储）
              - sseemitter（sse推送）
              - tools（工具）
              - variables（变量）
         AiApplication.class（应用入口）

`

### maven

https://github.com/wangzihaogithub/ai-assistant

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.wangzihaogithub/ai-assistant/badge.svg)](https://search.maven.org/search?q=g:com.github.wangzihaogithub%20AND%20a:ai-assistant)

```xml
<!-- https://github.com/wangzihaogithub/ai-assistant -->
<!-- https://mvnrepository.com/artifact/com.github.wangzihaogithub/ai-assistant -->
<dependency>
    <groupId>com.github.wangzihaogithub</groupId>
    <artifactId>ai-assistant</artifactId>
    <version>1.0.0</version>
</dependency>
```
