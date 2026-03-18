package com.xuan.xuanopenagent.service;

import com.xuan.xuanopenagent.agent.XuanAgent;
import com.xuan.xuanopenagent.agent.model.AgentContext;
import com.xuan.xuanopenagent.agent.model.AgentEvent;
import com.xuan.xuanopenagent.config.RagAdvisorConfig;
import com.xuan.xuanopenagent.model.ChatRequest;
import com.xuan.xuanopenagent.rag.RagRetrievalService;
import com.xuan.xuanopenagent.rag.model.RetrievalHit;
import com.xuan.xuanopenagent.tools.ToolRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class AgentService {

    private final XuanAgent xuanAgent;
    private final ChatClient xuanAgentChatClient;
    private final ToolRegistry toolRegistry;
    private final RagRetrievalService ragRetrievalService;
    private final RagAdvisorConfig ragAdvisorConfig;

    public AgentService(XuanAgent xuanAgent,
                        ChatClient xuanAgentChatClient,
                        ToolRegistry toolRegistry,
                        RagRetrievalService ragRetrievalService,
                        RagAdvisorConfig ragAdvisorConfig) {
        this.xuanAgent = xuanAgent;
        this.xuanAgentChatClient = xuanAgentChatClient;
        this.toolRegistry = toolRegistry;
        this.ragRetrievalService = ragRetrievalService;
        this.ragAdvisorConfig = ragAdvisorConfig;
    }

    public SseEmitter streamChat(ChatRequest request) {
        SseEmitter emitter = new SseEmitter(0L);
        AgentContext context = toAgentContext(request);

        CompletableFuture.runAsync(() -> {
            try {
                if (isKnowledgeBaseEnabled(request)) {
                    runKnowledgeBaseChat(request, context, emitter);
                    emitter.complete();
                    return;
                }

                xuanAgent.run(context, event -> sendEvent(emitter, event));
                emitter.complete();
            } catch (Exception ex) {
                sendErrorEvent(emitter, context, ex);
                emitter.completeWithError(ex);
            }
        });

        return emitter;
    }

    public List<String> listTools() {
        return toolRegistry.getRegisteredToolNames();
    }

    private boolean isKnowledgeBaseEnabled(ChatRequest request) {
        ChatRequest.ChatOptions options = request.getOptions();
        return options != null && Boolean.TRUE.equals(options.getUseKnowledgeBase());
    }

    private void runKnowledgeBaseChat(ChatRequest request, AgentContext context, SseEmitter emitter) {
        String fileIdFilter = request.getOptions() == null ? null : request.getOptions().getFileIdFilter();

        List<RetrievalHit> hits = ragRetrievalService.retrieve(request.getMessage(), fileIdFilter);
        sendRetrievalEvent(emitter, context, hits);

        RetrievalAugmentationAdvisor advisor = ragAdvisorConfig.buildRetrievalAdvisor(fileIdFilter);
        String answer = xuanAgentChatClient.prompt()
                .advisors(advisor)
                .user(request.getMessage())
                .call()
                .content();

        String finalAnswer = answer == null ? "" : answer;
        sendEvent(emitter, AgentEvent.of(
                context.getTraceId(),
                context.getSessionId(),
                context.getCurrentStep(),
                "final",
                Map.of(
                        "state", "RESPONDING",
                        "content", finalAnswer,
                        "timestamp", Instant.now().toString()
                )
        ));

        sendEvent(emitter, AgentEvent.of(
                context.getTraceId(),
                context.getSessionId(),
                context.getCurrentStep(),
                "done",
                Map.of(
                        "state", "TERMINATED",
                        "reason", "rag_responded",
                        "timestamp", Instant.now().toString()
                )
        ));
    }

    private AgentContext toAgentContext(ChatRequest request) {
        AgentContext context = AgentContext.initialize(
                request.getSessionId(),
                request.getUserId(),
                request.getMessage()
        );
        if (request.getOptions() != null && request.getOptions().getMaxSteps() != null) {
            context.setRequestedMaxSteps(request.getOptions().getMaxSteps());
        }
        return context;
    }

    private void sendEvent(SseEmitter emitter, AgentEvent event) {
        try {
            emitter.send(SseEmitter.event()
                    .name("agent-event")
                    .data(event));
        } catch (IOException ioException) {
            throw new RuntimeException(ioException);
        }
    }

    private void sendErrorEvent(SseEmitter emitter, AgentContext context, Exception ex) {
        AgentEvent errorEvent = AgentEvent.of(
                context.getTraceId(),
                context.getSessionId(),
                context.getCurrentStep(),
                "error",
                Map.of(
                        "state", "FAILED",
                        "message", ex.getMessage(),
                        "timestamp", Instant.now().toString()
                )
        );
        sendEvent(emitter, errorEvent);
    }

    private void sendRetrievalEvent(SseEmitter emitter, AgentContext context, List<RetrievalHit> hits) {
        try {
            emitter.send(SseEmitter.event()
                    .name("retrieval")
                    .data(AgentEvent.of(
                            context.getTraceId(),
                            context.getSessionId(),
                            context.getCurrentStep(),
                            "retrieval",
                            Map.of(
                                    "hitCount", hits.size(),
                                    "hits", hits,
                                    "timestamp", Instant.now().toString()
                            )
                    )));
        } catch (IOException ioException) {
            throw new RuntimeException(ioException);
        }
    }
}