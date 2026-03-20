package com.xuan.xuanopenagent.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration
@EnableConfigurationProperties({AgentProperties.class, RagProperties.class})
public class AiConfig {

    @Bean
    public ChatClient xuanAgentChatClient(@Qualifier("deepSeekChatModel") ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    @Bean("chunkingChatClient")
    public ChatClient chunkingChatClient(@Qualifier("dashScopeChatModel") ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}