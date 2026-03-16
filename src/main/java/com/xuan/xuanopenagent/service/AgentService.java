package com.xuan.xuanopenagent.service;

import com.xuan.xuanopenagent.agent.XuanAgent;
import com.xuan.xuanopenagent.agent.model.AgentContext;
import com.xuan.xuanopenagent.agent.model.AgentEvent;
import com.xuan.xuanopenagent.model.ChatRequest;
import com.xuan.xuanopenagent.tools.ToolRegistry;
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
    private final ToolRegistry toolRegistry;

    public AgentService(XuanAgent xuanAgent, ToolRegistry toolRegistry) {
        this.xuanAgent = xuanAgent;
        this.toolRegistry = toolRegistry;
    }

    public SseEmitter streamChat(ChatRequest request) {
        SseEmitter emitter = new SseEmitter(0L);
        AgentContext context = toAgentContext(request);

        CompletableFuture.runAsync(() -> {
            try {
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

    private AgentContext toAgentContext(ChatRequest request) {
        return AgentContext.initialize(
                request.getSessionId(),
                request.getUserId(),
                request.getMessage()
        );
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
}