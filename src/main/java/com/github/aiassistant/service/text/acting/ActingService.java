package com.github.aiassistant.service.text.acting;

import com.github.aiassistant.entity.model.chat.MemoryIdVO;
import com.github.aiassistant.entity.model.chat.WebSearchResultVO;
import com.github.aiassistant.util.AiUtil;
import com.github.aiassistant.service.text.ChatStreamingResponseHandler;
import com.github.aiassistant.util.FutureUtil;
import com.github.aiassistant.service.text.tools.functioncall.UrlReadTools;
import com.github.aiassistant.service.text.tools.WebSearchService;
import com.github.aiassistant.service.jsonschema.ActingJsonSchema;
import com.github.aiassistant.service.jsonschema.LlmJsonSchemaApiService;
import com.github.aiassistant.service.jsonschema.ReasoningJsonSchema;
import com.github.aiassistant.util.StringUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

//// @Component
public class ActingService {

    public static final String WEB_SOURCE_ENUM = "ActingJsonSchema";
    private final WebSearchService webSearchService = new WebSearchService(Arrays.asList(UrlReadTools.PROXY1, UrlReadTools.PROXY2));
    //    // @Autowired
    private final LlmJsonSchemaApiService llmJsonSchemaApiService;

    public ActingService(LlmJsonSchemaApiService llmJsonSchemaApiService) {
        this.llmJsonSchemaApiService = llmJsonSchemaApiService;
    }

    private CompletableFuture<Void> executeTask(String question,
                                                Plan root, Plan curr,
                                                ActingJsonSchema schema,
                                                ChatStreamingResponseHandler responseHandler,
                                                boolean websearchOnFail) {
        CompletableFuture<Void> f = new CompletableFuture<>();
        schema.future(curr.task, question, root.toAiTaskListString(), root.toAiString()).thenAccept(result -> {
            curr.result = result;
            CompletableFuture<WebSearchResultVO> webSearchFuture;
            if (result.resolved) {
                webSearchFuture = CompletableFuture.completedFuture(null);
            } else if (websearchOnFail) {
                List<String> websearchKeyword = result.websearchKeyword;
                if (websearchKeyword == null || result.websearchKeyword.isEmpty()) {
                    websearchKeyword = Collections.singletonList(curr.task);
                }
                webSearchFuture = webSearchService.webSearchRead(websearchKeyword, 1, 10000, false, responseHandler.adapterWebSearch(WEB_SOURCE_ENUM));
            } else {
                webSearchFuture = CompletableFuture.completedFuture(null);
            }
            webSearchFuture.whenComplete((resultVO, throwable1) -> {
                curr.webSearchResult = resultVO;
                try {
                    responseHandler.onActing(question, root, curr);
                } finally {
                    f.complete(null);
                }
            }).exceptionally(throwable12 -> {
                f.completeExceptionally(throwable12);
                return null;
            });
        }).exceptionally(throwable12 -> {
            f.completeExceptionally(throwable12);
            return null;
        });
        return f;
    }

    public Plan toPlan(ReasoningJsonSchema.Result result) {
        if (result == null || !result.needSplitting || result.tasks.isEmpty()) {
            return null;
        }
        Plan root = new Plan(0, result.tasks.get(0));
        int index = 1;
        Plan curr = root;
        while (index < result.tasks.size()) {
            String task = result.tasks.get(index);
            curr.next = new Plan(index, task);
            curr = curr.next;
            index++;
        }
        return root;
    }

    public CompletableFuture<Plan> executeTask(Plan root,
                                               String question, MemoryIdVO memoryIdVO,
                                               boolean parallel,
                                               ChatStreamingResponseHandler responseHandler,
                                               boolean websearchOnFail) {
        if (root == null) {
            return CompletableFuture.completedFuture(null);
        }
        ActingJsonSchema schema = llmJsonSchemaApiService.getActingJsonSchema(memoryIdVO);
        if (schema == null) {
            return CompletableFuture.completedFuture(null);
        }
        Plan curr;
        if (parallel) {
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            curr = root;
            while (curr != null) {
                // 并行执行，好处是快
                futures.add(executeTask(question, root, curr, schema, responseHandler, websearchOnFail));
                curr = curr.next;
            }
            return FutureUtil.allOf(futures)
                    .thenApply(unused -> root);
        } else {
            class ExecuteTask extends CompletableFuture<Plan> implements Consumer<Void> {
                final Plan root;
                final Plan curr;
                final ExecuteTask parent;

                ExecuteTask(Plan root, Plan curr, ExecuteTask parent) {
                    this.root = root;
                    this.curr = curr;
                    this.parent = parent;
                }

                @Override
                public void accept(Void unused) {
                    if (curr == null) {
                        for (ExecuteTask i = this; i != null; i = i.parent) {
                            i.complete(root);
                        }
                    } else {
                        executeTask(question, root, curr, schema, responseHandler, websearchOnFail)
                                .thenAccept(new ExecuteTask(root, curr.next, this))
                                .exceptionally(throwable -> {
                                    for (ExecuteTask i = this; i != null; i = i.parent) {
                                        i.completeExceptionally(throwable);
                                    }
                                    return null;
                                });
                    }
                }
            }
            ExecuteTask task = new ExecuteTask(root, root, null);
            task.accept(null);
            return task;
        }
    }

    public static class Plan {
        private final String task;
        private final int index;
        private final Date createTime = new Date();
        private Plan next;
        private ActingJsonSchema.Result result;
        private WebSearchResultVO webSearchResult;
        private WebSearchResultVO reduceWebSearchResult;

        public Plan(int index, String task) {
            this.index = index;
            this.task = task;
        }

        public void setReduceWebSearchResult(WebSearchResultVO reduceWebSearchResult) {
            this.reduceWebSearchResult = reduceWebSearchResult;
        }

        public WebSearchResultVO reduceWebSearchResult() {
            return reduceWebSearchResult;
        }

        public Date getCreateTime() {
            return createTime;
        }

        public ActingJsonSchema.Result getResult() {
            return result;
        }

        public Plan getNext() {
            return next;
        }

        public int getIndex() {
            return index;
        }

        public String getTask() {
            return task;
        }

        @Override
        public String toString() {
            return task;
        }

        public String failMessage() {
            if (result != null && StringUtils.hasText(result.aiQuestion)) {
                if (StringUtils.hasText(result.answer)) {
                    return result.answer + "\n" + result.aiQuestion;
                } else {
                    return result.aiQuestion;
                }
            }
            return null;
        }

        public boolean isExistWebSearch() {
            return webSearchResult != null && !webSearchResult.isEmpty();
        }

        public WebSearchResultVO getWebSearchResult() {
            return webSearchResult;
        }

        public List<WebSearchResultVO> flatWebSearchResult() {
            List<WebSearchResultVO> results = new ArrayList<>();
            for (Plan curr = this; curr != null; curr = curr.next) {
                if (curr.result != null
                        && !curr.result.resolved
                        && curr.isExistWebSearch()
                ) {
                    results.add(curr.webSearchResult);
                }
            }
            return results;
        }

        public Plan failTask() {
            for (Plan curr = this; curr != null; curr = curr.next) {
                if (curr.result != null
                        && !curr.result.resolved
                        && StringUtils.hasText(curr.result.aiQuestion)
                        && !curr.isExistWebSearch()
                ) {
                    return curr;
                }
            }
            return null;
        }

        public String toAiTaskListString() {
            StringJoiner joiner = new StringJoiner("\n");
            int i = 1;
            for (Plan curr = this; curr != null; curr = curr.next, i++) {
                String taskName = "task-" + i;
                joiner.add(AiUtil.toAiXmlString(taskName, curr.task));
            }
            return joiner.toString();
        }

        public String toAiTitleString() {
            StringJoiner joiner = new StringJoiner("\",\"", "[\"", "\"]");
            int i = 1;
            for (Plan curr = this; curr != null; curr = curr.next, i++) {
//                String taskName = "title-" + i;
//                joiner.add(AiUtil.toAiXmlString(taskName, curr.task));
                joiner.add(curr.task);
            }
            return joiner.toString();
        }

        public String toAiString() {
            StringJoiner joiner = new StringJoiner("\n");
            for (Plan curr = this; curr != null && curr.result != null; curr = curr.next) {
                if (StringUtils.hasText(curr.result.answer)) {
                    joiner.add(AiUtil.toAiXmlString(curr.task, curr.result.answer));
                }
            }
            return joiner.toString();
        }

        public String toActingResult() {
            StringJoiner joiner = new StringJoiner("\n");
            for (Plan curr = this; curr != null && curr.result != null; curr = curr.next) {
                if (StringUtils.hasText(curr.result.answer)) {
                    joiner.add(AiUtil.toAiXmlString(curr.task, curr.result.answer));
                }
            }
            return joiner.toString();
        }

        public String toWebSearchActingResult() {
            List<WebSearchResultVO> results = new ArrayList<>();
            if (reduceWebSearchResult != null) {
                results.add(reduceWebSearchResult);
            } else {
                for (Plan curr = this; curr != null && curr.result != null; curr = curr.next) {
                    if (curr.isExistWebSearch()) {
                        results.add(curr.webSearchResult);
                    }
                }
            }
            return WebSearchResultVO.toSimpleAiString(results.toArray(new WebSearchResultVO[0]));
        }
    }
}
