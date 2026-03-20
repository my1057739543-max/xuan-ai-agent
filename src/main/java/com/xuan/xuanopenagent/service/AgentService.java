package com.xuan.xuanopenagent.service;

import com.xuan.xuanopenagent.agent.XuanAgent;
import com.xuan.xuanopenagent.agent.model.AgentContext;
import com.xuan.xuanopenagent.agent.model.AgentEvent;
import com.xuan.xuanopenagent.config.RagProperties;
import com.xuan.xuanopenagent.model.ChatRequest;
import com.xuan.xuanopenagent.rag.RagRetrievalService;
import com.xuan.xuanopenagent.rag.model.RetrievalHit;
import com.xuan.xuanopenagent.tools.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    private final XuanAgent xuanAgent;
    private final ToolRegistry toolRegistry;
    private final RagRetrievalService ragRetrievalService;
    private final RagProperties ragProperties;
    private final CustomGameKeyRegistry customGameKeyRegistry;
    private final ConcurrentMap<String, String> sessionGameMemory = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, SessionSummary> sessionSummaryMemory = new ConcurrentHashMap<>();

    @Autowired(required = false)
    @Qualifier("chunkingChatClient")
    private ChatClient gameInferChatClient;

    @Value("${xuan.agent.intent-rewrite.enabled:true}")
    private boolean intentRewriteEnabled;

    @Value("${xuan.agent.memory-summary.enabled:true}")
    private boolean memorySummaryEnabled;

    @Value("${xuan.agent.memory-summary.every-n-turns:6}")
    private int memorySummaryEveryNTurns;

        @Value("${xuan.agent.game-infer.enabled:true}")
        private boolean gameInferEnabled;

        private static final Map<String, String> GAME_ENTITY_ALIAS_MAP = Map.ofEntries(
            Map.entry("jett", "valorant"),
            Map.entry("捷风", "valorant"),
            Map.entry("reyna", "valorant"),
            Map.entry("蕾娜", "valorant"),
            Map.entry("raze", "valorant"),
            Map.entry("雷兹", "valorant"),
            Map.entry("phoenix", "valorant"),
            Map.entry("不死鸟", "valorant"),
            Map.entry("sova", "valorant"),
            Map.entry("猎枭", "valorant"),
            Map.entry("cypher", "valorant"),
            Map.entry("零", "valorant"),
            Map.entry("omen", "valorant"),
            Map.entry("幽影", "valorant"),
            Map.entry("viper", "valorant"),
            Map.entry("蝰蛇", "valorant"),
            Map.entry("brimstone", "valorant"),
            Map.entry("炼狱", "valorant"),
            Map.entry("sage", "valorant"),
            Map.entry("贤者", "valorant"),
            Map.entry("mirage", "cs2"),
            Map.entry("dust2", "cs2"),
            Map.entry("a long", "cs2"),
            Map.entry("mid control", "cs2"),
            Map.entry("yasuo", "lol"),
            Map.entry("亚索", "lol"),
            Map.entry("jungler", "lol"),
            Map.entry("打野", "lol"),
            Map.entry("wraith", "apex"),
            Map.entry("寻血猎犬", "apex"),
            Map.entry("apex legends", "apex")
        );

    public AgentService(XuanAgent xuanAgent,
                        ToolRegistry toolRegistry,
                        RagRetrievalService ragRetrievalService,
                        RagProperties ragProperties,
                        CustomGameKeyRegistry customGameKeyRegistry) {
        this.xuanAgent = xuanAgent;
        this.toolRegistry = toolRegistry;
        this.ragRetrievalService = ragRetrievalService;
        this.ragProperties = ragProperties;
        this.customGameKeyRegistry = customGameKeyRegistry;
    }

    public SseEmitter streamChat(ChatRequest request) {
        SseEmitter emitter = new SseEmitter(0L);
        AgentContext context = toAgentContext(request);

        CompletableFuture.runAsync(() -> {
            try {
                maybeApplyKnowledgeContext(request, context, emitter);
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

    private void maybeApplyKnowledgeContext(ChatRequest request, AgentContext context, SseEmitter emitter) {
        if (!isKnowledgeBaseEnabled(request)) {
            return;
        }

        SessionSummary summary = sessionSummaryMemory.computeIfAbsent(
                request.getSessionId(),
                ignored -> SessionSummary.initial(request.getSessionId())
        );
        summary.bumpTurn();

        String fileIdFilter = request.getOptions() == null ? null : request.getOptions().getFileIdFilter();
        String explicitGameKey = request.getOptions() == null ? null : request.getOptions().getGameKey();
        String memoryGameKey = sessionGameMemory.get(request.getSessionId());
        boolean firstTurn = summary.turn() == 1;
        ResolvedGameKey resolvedGameKey = resolveGameKey(request.getMessage(), explicitGameKey, memoryGameKey, firstTurn);

        IntentResult intentResult = detectIntent(request.getMessage(), resolvedGameKey.finalGameKey());
        if (intentRewriteEnabled) {
            sendIntentDetectedEvent(emitter, context, intentResult);
        }

        String retrievalQuery = request.getMessage();
        if (intentRewriteEnabled) {
            retrievalQuery = rewriteQuery(request.getMessage(), intentResult, resolvedGameKey.finalGameKey());
            sendQueryRewrittenEvent(emitter, context, retrievalQuery, intentResult);
        }

        String retrievalGameKeyFilter = null;

        if (resolvedGameKey.conflictWith() != null && !resolvedGameKey.conflictWith().isBlank()) {
            sendGameConflictEvent(emitter, context, resolvedGameKey.finalGameKey(), resolvedGameKey.conflictWith());
        }

        if (ragProperties.isGameIsolationEnabled()) {
            if (resolvedGameKey.finalGameKey() == null || resolvedGameKey.finalGameKey().isBlank()) {
                sendClarificationEvent(emitter, context, "missing_game_key", "无法确定你当前咨询的游戏，请补充 gameKey（如 valorant/cs2/apex/lol）。");
                return;
            }
            retrievalGameKeyFilter = resolvedGameKey.finalGameKey();
            sendGameResolvedEvent(emitter, context, resolvedGameKey);
            sessionGameMemory.put(request.getSessionId(), retrievalGameKeyFilter);
        } else if (resolvedGameKey.finalGameKey() != null && !resolvedGameKey.finalGameKey().isBlank()) {
            retrievalGameKeyFilter = resolvedGameKey.finalGameKey();
            sendGameResolvedEvent(emitter, context, resolvedGameKey);
            sessionGameMemory.put(request.getSessionId(), retrievalGameKeyFilter);
        }

        log.info("[RAG] traceId={} sessionId={} fileIdFilter={} finalGameKey={} source={}",
                context.getTraceId(),
                context.getSessionId(),
                defaultIfBlank(fileIdFilter, ""),
                defaultIfBlank(retrievalGameKeyFilter, ""),
                resolvedGameKey.source());

        List<RetrievalHit> hits = ragRetrievalService.retrieve(
                retrievalQuery,
                fileIdFilter,
                retrievalGameKeyFilter
        );
        sendRetrievalEvent(emitter, context, hits);

        if (memorySummaryEnabled) {
            maybeUpdateSummary(request, emitter, context, summary, resolvedGameKey.finalGameKey(), intentResult, hits);
        }

        String ragContext = ragRetrievalService.buildPromptContext(hits);
        if (!ragContext.isBlank()) {
            String enhancedMessage = request.getMessage() + "\n\n" + ragContext;
            context.setMessage(enhancedMessage);
            context.addHistory("retrieval_hits=" + hits.size());
        }
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

    private void sendGameResolvedEvent(SseEmitter emitter, AgentContext context, ResolvedGameKey resolved) {
        try {
            emitter.send(SseEmitter.event()
                    .name("game_resolved")
                    .data(AgentEvent.of(
                            context.getTraceId(),
                            context.getSessionId(),
                            context.getCurrentStep(),
                            "game_resolved",
                            Map.of(
                                    "detectedGameKey", defaultIfBlank(resolved.detectedGameKey(), ""),
                                    "finalGameKey", defaultIfBlank(resolved.finalGameKey(), ""),
                                    "source", resolved.source(),
                                    "conflictWith", defaultIfBlank(resolved.conflictWith(), ""),
                                    "timestamp", Instant.now().toString()
                            )
                    )));
        } catch (IOException ioException) {
            throw new RuntimeException(ioException);
        }
    }

    private void sendGameConflictEvent(SseEmitter emitter, AgentContext context, String finalGameKey, String conflictWith) {
        try {
            emitter.send(SseEmitter.event()
                    .name("game_conflict_detected")
                    .data(AgentEvent.of(
                            context.getTraceId(),
                            context.getSessionId(),
                            context.getCurrentStep(),
                            "game_conflict_detected",
                            Map.of(
                                    "finalGameKey", defaultIfBlank(finalGameKey, ""),
                                    "conflictWith", defaultIfBlank(conflictWith, ""),
                                    "timestamp", Instant.now().toString()
                            )
                    )));
        } catch (IOException ioException) {
            throw new RuntimeException(ioException);
        }
    }

    private void sendClarificationEvent(SseEmitter emitter, AgentContext context, String reason, String message) {
        try {
            emitter.send(SseEmitter.event()
                    .name("clarification_needed")
                    .data(AgentEvent.of(
                            context.getTraceId(),
                            context.getSessionId(),
                            context.getCurrentStep(),
                            "clarification_needed",
                            Map.of(
                                    "reason", defaultIfBlank(reason, "unknown"),
                                    "message", message,
                                    "candidates", ragProperties.getSupportedGameKeys(),
                                    "timestamp", Instant.now().toString()
                            )
                    )));
        } catch (IOException ioException) {
            throw new RuntimeException(ioException);
        }
    }

    private void sendIntentDetectedEvent(SseEmitter emitter, AgentContext context, IntentResult intentResult) {
        try {
            emitter.send(SseEmitter.event()
                    .name("intent_detected")
                    .data(AgentEvent.of(
                            context.getTraceId(),
                            context.getSessionId(),
                            context.getCurrentStep(),
                            "intent_detected",
                            Map.of(
                                    "intentType", intentResult.intentType(),
                                    "confidence", intentResult.confidence(),
                                    "entities", intentResult.entities(),
                                    "detectedGameKey", defaultIfBlank(intentResult.detectedGameKey(), ""),
                                    "timestamp", Instant.now().toString()
                            )
                    )));
        } catch (IOException ioException) {
            throw new RuntimeException(ioException);
        }
    }

    private void sendQueryRewrittenEvent(SseEmitter emitter, AgentContext context, String rewrittenQuery, IntentResult intentResult) {
        try {
            emitter.send(SseEmitter.event()
                    .name("query_rewritten")
                    .data(AgentEvent.of(
                            context.getTraceId(),
                            context.getSessionId(),
                            context.getCurrentStep(),
                            "query_rewritten",
                            Map.of(
                                    "intentType", intentResult.intentType(),
                                    "rewrittenQuery", rewrittenQuery,
                                    "currentGameKey", defaultIfBlank(intentResult.detectedGameKey(), ""),
                                    "timestamp", Instant.now().toString()
                            )
                    )));
        } catch (IOException ioException) {
            throw new RuntimeException(ioException);
        }
    }

    private void sendMemoryUpdatedEvent(SseEmitter emitter, AgentContext context, SessionSummary summary) {
        try {
            emitter.send(SseEmitter.event()
                    .name("memory_updated")
                    .data(AgentEvent.of(
                            context.getTraceId(),
                            context.getSessionId(),
                            context.getCurrentStep(),
                            "memory_updated",
                            Map.of(
                                    "currentGameKey", defaultIfBlank(summary.currentGameKey(), ""),
                                    "userGoal", defaultIfBlank(summary.userGoal(), ""),
                                    "coveredSkills", summary.coveredSkills(),
                                    "unresolvedQuestions", summary.unresolvedQuestions(),
                                    "preferredStyle", defaultIfBlank(summary.preferredStyle(), ""),
                                    "summaryVersion", summary.version(),
                                    "timestamp", Instant.now().toString()
                            )
                    )));
        } catch (IOException ioException) {
            throw new RuntimeException(ioException);
        }
    }

    private IntentResult detectIntent(String message, String detectedGameKey) {
        String lowered = defaultIfBlank(message, "").toLowerCase(Locale.ROOT);
        String intentType = "skill_how_to";
        double confidence = 0.65;
        List<String> entities = new ArrayList<>();

        if (containsAny(lowered, "怎么练", "训练", "drill", "plan", "每天")) {
            intentType = "training_plan";
            confidence = 0.82;
        } else if (containsAny(lowered, "不准", "失误", "总是", "fix", "纠正", "改", "怎么办")) {
            intentType = "mistake_fix";
            confidence = 0.8;
        } else if (containsAny(lowered, "参数", "灵敏度", "设置", "config", "dpi")) {
            intentType = "config_tuning";
            confidence = 0.84;
        } else if (containsAny(lowered, "对比", "哪个", "区别", "better", "vs")) {
            intentType = "comparison";
            confidence = 0.76;
        }

        if (containsAny(lowered, "准星", "crosshair", "枪", "weapon")) {
            entities.add("weapon");
        }
        if (containsAny(lowered, "地图", "map", "点位", "a点", "b点")) {
            entities.add("map");
        }
        if (containsAny(lowered, "角色", "先锋", "决斗", "哨卫", "控场", "role")) {
            entities.add("role");
        }

        return new IntentResult(intentType, confidence, entities, detectedGameKey);
    }

    private String rewriteQuery(String originalMessage, IntentResult intentResult, String finalGameKey) {
        String msg = defaultIfBlank(originalMessage, "").trim();
        if (msg.isEmpty()) {
            return msg;
        }

        String normalized = msg
                .replace("急停", "急停 counter strafe")
                .replace("拉枪", "拉枪 tracking")
                .replace("定位", "预瞄 定位 pre-aim")
                .replace("身法", "身法 movement");

        StringBuilder builder = new StringBuilder();
        if (finalGameKey != null && !finalGameKey.isBlank()) {
            builder.append(finalGameKey).append(' ');
        }
        builder.append(normalized)
                .append(" | intent=")
                .append(intentResult.intentType());

        return builder.toString();
    }

    private void maybeUpdateSummary(ChatRequest request,
                                    SseEmitter emitter,
                                    AgentContext context,
                                    SessionSummary summary,
                                    String finalGameKey,
                                    IntentResult intentResult,
                                    List<RetrievalHit> hits) {
        boolean gameSwitched = finalGameKey != null
                && !finalGameKey.isBlank()
                && summary.currentGameKey() != null
                && !summary.currentGameKey().isBlank()
                && !summary.currentGameKey().equals(finalGameKey);

        if (finalGameKey != null && !finalGameKey.isBlank()) {
            summary.currentGameKey(finalGameKey);
        }

        if ("training_plan".equals(intentResult.intentType())) {
            summary.userGoal("training");
            summary.preferredStyle("step_by_step");
        } else if ("comparison".equals(intentResult.intentType())) {
            summary.userGoal("decision_support");
            summary.preferredStyle("principle_first");
        } else {
            summary.userGoal("skill_improvement");
        }

        for (RetrievalHit hit : hits) {
            if (hit.getContentSnippet() != null && !hit.getContentSnippet().isBlank()) {
                String snippet = hit.getContentSnippet().trim();
                String compact = snippet.length() > 28 ? snippet.substring(0, 28) : snippet;
                summary.coveredSkillsSet().add(compact);
                if (summary.coveredSkills().size() >= 5) {
                    break;
                }
            }
        }

        if (hits.isEmpty()) {
            summary.unresolvedQuestionsSet().add(defaultIfBlank(request.getMessage(), ""));
        }

        boolean onNthTurn = memorySummaryEveryNTurns > 0 && summary.turn() % memorySummaryEveryNTurns == 0;
        if (gameSwitched || onNthTurn) {
            summary.version(summary.version() + 1);
            sendMemoryUpdatedEvent(emitter, context, summary);
        }
    }

    private boolean containsAny(String source, String... candidates) {
        for (String candidate : candidates) {
            if (source.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private ResolvedGameKey resolveGameKey(String message, String explicitGameKey, String memoryGameKey, boolean firstTurn) {
        String normalizedExplicit = normalizeGameKey(explicitGameKey);
        String detected = detectGameKeyFromMessage(message);
        String entityDetected = detectGameKeyFromEntity(message);
        String normalizedMemory = normalizeGameKey(memoryGameKey);

        if (normalizedExplicit != null) {
            String conflict = (detected != null && !detected.equals(normalizedExplicit))
                    ? detected
                    : ((normalizedMemory != null && !normalizedMemory.equals(normalizedExplicit)) ? normalizedMemory : null);
            return new ResolvedGameKey(detected, normalizedExplicit, "explicit", conflict);
        }
        if (detected != null) {
            String conflict = (normalizedMemory != null && !normalizedMemory.equals(detected)) ? normalizedMemory : null;
            return new ResolvedGameKey(detected, detected, "alias_match", conflict);
        }
        if (entityDetected != null) {
            String conflict = (normalizedMemory != null && !normalizedMemory.equals(entityDetected)) ? normalizedMemory : null;
            return new ResolvedGameKey(entityDetected, entityDetected, "entity_match", conflict);
        }

        if (firstTurn && gameInferEnabled) {
            String inferred = inferGameKeyByLlm(message);
            if (inferred != null) {
                String conflict = (normalizedMemory != null && !normalizedMemory.equals(inferred)) ? normalizedMemory : null;
                return new ResolvedGameKey(inferred, inferred, "llm_infer", conflict);
            }
        }

        if (normalizedMemory != null) {
            return new ResolvedGameKey(null, normalizedMemory, "memory", null);
        }

        String defaultKey = normalizeGameKey(ragProperties.getDefaultGameKey());
        if (defaultKey != null) {
            return new ResolvedGameKey(null, defaultKey, "default", null);
        }
        return new ResolvedGameKey(null, null, "none", null);
    }

    private String detectGameKeyFromEntity(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        
        // 优先检查动态注册的自定义游戏名
        String customDetected = customGameKeyRegistry.detectFromMessage(message);
        if (customDetected != null) {
            log.debug("[RAG] game entity detected from custom registry: {}", customDetected);
            return customDetected;
        }
        
        String lowered = message.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> entry : GAME_ENTITY_ALIAS_MAP.entrySet()) {
            String key = entry.getKey().toLowerCase(Locale.ROOT);
            String mapped = normalizeGameKey(entry.getValue());
            if (mapped == null) {
                continue;
            }
            if (containsAlias(lowered, key)) {
                return mapped;
            }
        }
        return null;
    }

    private String inferGameKeyByLlm(String message) {
        if (gameInferChatClient == null || message == null || message.isBlank()) {
            return null;
        }
        try {
            String supported = String.join(",", ragProperties.getSupportedGameKeys());
            String content = gameInferChatClient.prompt()
                    .system(sys -> sys.text("You classify a player's game title. Output only one token from supported list or none."))
                    .user("supported=" + supported + "\nquery=" + message + "\nReturn only: one gameKey or none")
                    .call()
                    .content();
            String normalized = normalizeGameKey(defaultIfBlank(content, "").replace("\"", "").trim());
            if (normalized != null) {
                log.info("[RAG] first-turn game inferred by llm: {}", normalized);
            }
            return normalized;
        } catch (Exception ex) {
            log.warn("[RAG] llm game inference failed: {}", ex.getMessage());
            return null;
        }
    }

    private String detectGameKeyFromMessage(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        
        // 优先检查动态注册的自定义游戏名
        String customDetected = customGameKeyRegistry.detectFromMessage(message);
        if (customDetected != null) {
            log.debug("[RAG] game detected from custom registry: {}", customDetected);
            return customDetected;
        }
        
        String lowered = message.toLowerCase(Locale.ROOT);
        List<Map.Entry<String, String>> entries = new ArrayList<>(ragProperties.getGameAliasMap().entrySet());
        entries.sort(Comparator.comparingInt((Map.Entry<String, String> it) -> it.getKey() == null ? 0 : it.getKey().length())
                .reversed());

        for (Map.Entry<String, String> entry : entries) {
            String alias = entry.getKey() == null ? "" : entry.getKey().trim().toLowerCase(Locale.ROOT);
            String mapped = normalizeGameKey(entry.getValue());
            if (alias.isBlank() || mapped == null) {
                continue;
            }
            if (containsAlias(lowered, alias)) {
                return mapped;
            }
        }
        return null;
    }

    private String normalizeGameKey(String gameKey) {
        if (gameKey == null || gameKey.isBlank()) {
            return null;
        }
        String normalized = gameKey.trim().toLowerCase(Locale.ROOT);
        if (ragProperties.getGameAliasMap() != null && !ragProperties.getGameAliasMap().isEmpty()) {
            for (Map.Entry<String, String> entry : ragProperties.getGameAliasMap().entrySet()) {
                String alias = entry.getKey() == null ? "" : entry.getKey().trim().toLowerCase(Locale.ROOT);
                if (!alias.isBlank() && alias.equals(normalized)) {
                    String mapped = entry.getValue() == null ? "" : entry.getValue().trim().toLowerCase(Locale.ROOT);
                    if (!mapped.isBlank()) {
                        normalized = mapped;
                    }
                    break;
                }
            }
        }
        return normalized.isBlank() ? null : normalized;
    }

    private boolean containsAlias(String message, String alias) {
        if (isAsciiAlias(alias)) {
            String pattern = "(?<![a-z0-9_])" + Pattern.quote(alias) + "(?![a-z0-9_])";
            return Pattern.compile(pattern).matcher(message).find();
        }
        return message.contains(alias);
    }

    private boolean isAsciiAlias(String alias) {
        for (int i = 0; i < alias.length(); i++) {
            if (alias.charAt(i) > 127) {
                return false;
            }
        }
        return true;
    }

    private String defaultIfBlank(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    private record IntentResult(String intentType, double confidence, List<String> entities, String detectedGameKey) {
    }

    private record ResolvedGameKey(String detectedGameKey, String finalGameKey, String source, String conflictWith) {
    }

    private static final class SessionSummary {
        private final String sessionId;
        private int turn;
        private int version;
        private String currentGameKey;
        private String userGoal;
        private final LinkedHashSet<String> coveredSkills = new LinkedHashSet<>();
        private final LinkedHashSet<String> unresolvedQuestions = new LinkedHashSet<>();
        private String preferredStyle = "step_by_step";

        private SessionSummary(String sessionId) {
            this.sessionId = sessionId;
            this.turn = 0;
            this.version = 0;
        }

        static SessionSummary initial(String sessionId) {
            return new SessionSummary(sessionId);
        }

        void bumpTurn() {
            this.turn++;
        }

        int turn() {
            return turn;
        }

        int version() {
            return version;
        }

        void version(int version) {
            this.version = version;
        }

        String currentGameKey() {
            return currentGameKey;
        }

        void currentGameKey(String currentGameKey) {
            this.currentGameKey = currentGameKey;
        }

        String userGoal() {
            return userGoal;
        }

        void userGoal(String userGoal) {
            this.userGoal = userGoal;
        }

        List<String> coveredSkills() {
            return new ArrayList<>(coveredSkills);
        }

        LinkedHashSet<String> coveredSkillsSet() {
            return coveredSkills;
        }

        List<String> unresolvedQuestions() {
            return new ArrayList<>(unresolvedQuestions);
        }

        LinkedHashSet<String> unresolvedQuestionsSet() {
            return unresolvedQuestions;
        }

        String preferredStyle() {
            return preferredStyle;
        }

        void preferredStyle(String preferredStyle) {
            this.preferredStyle = preferredStyle;
        }
    }
}