package com.xuan.xuanopenagent.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Configuration
@EnableConfigurationProperties(AgentProperties.class)
public class AiConfig {

    @Bean
    public ChatClient xuanAgentChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}