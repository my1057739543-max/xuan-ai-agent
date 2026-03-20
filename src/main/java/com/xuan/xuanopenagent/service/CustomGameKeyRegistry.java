package com.xuan.xuanopenagent.service;

import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Locale;

/**
 * 动态游戏 gameKey 注册表
 * 用于记录用户从文档上传、对话中发现的自定义游戏名称
 * 例如：用户上传"织湖"的知识，后续对话中提到"织湖"时能正确识别
 */
@Component
public class CustomGameKeyRegistry {

    /**
     * customGameName -> gameKey 的映射
     * 例如：织湖 -> rusty-lake, 逆序 -> reverse-order
     */
    private final ConcurrentHashMap<String, String> customGameMapping = new ConcurrentHashMap<>();

    /**
     * 注册一个自定义游戏名称
     * @param gameKey 游戏标准 key（如 rusty-lake）
     * @param customNames 自定义游戏名称（可以包含中英文、昵称等），如 织湖 逆序等
     */
    public void registerCustomGameNames(String gameKey, String... customNames) {
        if (gameKey == null || gameKey.isBlank()) {
            return;
        }
        String normalized = gameKey.trim().toLowerCase(Locale.ROOT);
        for (String name : customNames) {
            if (name != null && !name.isBlank()) {
                String nameLower = name.trim().toLowerCase(Locale.ROOT);
                customGameMapping.putIfAbsent(nameLower, normalized);
            }
        }
    }

    /**
     * 从消息中检测是否提及了任何注册的自定义游戏
     * @param message 用户消息
     * @return 检测到的 gameKey，或 null
     */
    public String detectFromMessage(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        String lowered = message.toLowerCase(Locale.ROOT);
        
        // 按长度倒序（优先匹配长名字） 
        return customGameMapping.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()))
                .filter(entry -> lowered.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取某个 gameKey 注册过的所有自定义名称
     * @param gameKey
     * @return
     */
    public Set<String> getCustomNamesForGameKey(String gameKey) {
        if (gameKey == null || gameKey.isBlank()) {
            return Set.of();
        }
        String normalized = gameKey.trim().toLowerCase(Locale.ROOT);
        return customGameMapping.entrySet().stream()
                .filter(e -> e.getValue().equals(normalized))
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * 清空所有注册
     */
    public void clear() {
        customGameMapping.clear();
    }

    /**
     * 获取注册表大小
     */
    public int size() {
        return customGameMapping.size();
    }
}
