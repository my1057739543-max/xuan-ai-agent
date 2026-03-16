package com.xuan.xuanopenagent.config;

import com.xuan.xuanopenagent.agent.model.AgentContext;
import com.xuan.xuanopenagent.agent.model.AgentEvent;
import com.xuan.xuanopenagent.agent.model.AgentState;
import com.xuan.xuanopenagent.agent.model.ExecutionTrace;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentFoundationConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(
            ConfigurationPropertiesAutoConfiguration.class,
            ValidationAutoConfiguration.class
        ))
            .withUserConfiguration(AiConfig.class, StubChatModelConfiguration.class);

    @Test
    void shouldBindAgentProperties() {
        contextRunner
                .withPropertyValues(
                        "xuan.agent.max-steps=12",
                        "xuan.agent.max-tool-calls=5",
                        "xuan.agent.tool-timeout-seconds=240",
                        "xuan.agent.model-name=deepseek-reasoner"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(AgentProperties.class);
                    AgentProperties properties = context.getBean(AgentProperties.class);
                    assertThat(properties.getMaxSteps()).isEqualTo(12);
                    assertThat(properties.getMaxToolCalls()).isEqualTo(5);
                    assertThat(properties.getToolTimeoutSeconds()).isEqualTo(240);
                    assertThat(properties.getModelName()).isEqualTo("deepseek-reasoner");
                });
    }

    @Test
    void shouldCreateSpringAiChatClientBean() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ChatClient.class);
            assertThat(context).hasSingleBean(ChatModel.class);
        });
    }

    @Test
    void shouldInitializeAgentContextAndEventModels() {
        AgentContext context = AgentContext.initialize("session-001", "user-001", "现在几点了？");

        assertThat(context.getTraceId()).isNotBlank();
        assertThat(context.getSessionId()).isEqualTo("session-001");
        assertThat(context.getUserId()).isEqualTo("user-001");
        assertThat(context.getMessage()).isEqualTo("现在几点了？");
        assertThat(context.getHistory()).containsExactly("现在几点了？");
        assertThat(context.advanceStep()).isEqualTo(1);
        assertThat(context.incrementToolCallCount()).isEqualTo(1);

        ExecutionTrace trace = ExecutionTrace.of(1, AgentState.THINKING, "thought", "input", "output", 25L);
        context.addExecutionTrace(trace);

        AgentEvent event = AgentEvent.of(
                context.getTraceId(),
                context.getSessionId(),
                context.getCurrentStep(),
                "thought",
                Map.of("content", "先判断是否需要工具")
        );

        assertThat(context.getExecutionTraces()).hasSize(1);
        assertThat(context.getExecutionTraces().getFirst().getState()).isEqualTo(AgentState.THINKING);
        assertThat(event.getTraceId()).isEqualTo(context.getTraceId());
        assertThat(event.getType()).isEqualTo("thought");
        assertThat(event.getPayload()).containsEntry("content", "先判断是否需要工具");
        assertThat(event.getTimestamp()).isNotNull();
    }

    @Configuration
    static class StubChatModelConfiguration {

        @Bean
        ChatModel chatModel() {
            InvocationHandler handler = new ChatModelInvocationHandler();
            return (ChatModel) Proxy.newProxyInstance(
                    ChatModel.class.getClassLoader(),
                    new Class[]{ChatModel.class},
                    handler
            );
        }
    }

    static class ChatModelInvocationHandler implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "toString" -> "StubChatModel";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> null;
                };
            }

            Class<?> returnType = method.getReturnType();
            if (returnType == boolean.class) {
                return false;
            }
            if (returnType == byte.class) {
                return (byte) 0;
            }
            if (returnType == short.class) {
                return (short) 0;
            }
            if (returnType == int.class) {
                return 0;
            }
            if (returnType == long.class) {
                return 0L;
            }
            if (returnType == float.class) {
                return 0F;
            }
            if (returnType == double.class) {
                return 0D;
            }
            if (returnType == char.class) {
                return '\0';
            }
            return null;
        }
    }
}