package com.github.aiassistant.service.text.reasoning;

import com.github.aiassistant.entity.model.chat.MemoryIdVO;
import com.github.aiassistant.service.jsonschema.LlmJsonSchemaApiService;
import com.github.aiassistant.service.jsonschema.ReasoningJsonSchema;

import java.util.concurrent.CompletableFuture;

//// @Component
public class ReasoningService {
    //    // @Autowired
    private final LlmJsonSchemaApiService llmJsonSchemaApiService;

    public ReasoningService(LlmJsonSchemaApiService llmJsonSchemaApiService) {
        this.llmJsonSchemaApiService = llmJsonSchemaApiService;
    }

    public CompletableFuture<ReasoningJsonSchema.Result> makePlan(String question, MemoryIdVO memoryIdVO) {
        ReasoningJsonSchema schema = llmJsonSchemaApiService.getReasoningJsonSchema(memoryIdVO);
        if (schema == null) {
            return CompletableFuture.completedFuture(null);
        }
        return schema.future(question);
    }

}
