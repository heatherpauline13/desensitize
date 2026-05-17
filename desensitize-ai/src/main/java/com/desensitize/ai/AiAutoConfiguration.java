package com.desensitize.ai;

import com.desensitize.core.config.DesensitizeConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

@Slf4j
@Configuration
@ConditionalOnClass(ChatClient.class)
@ConditionalOnProperty(prefix = "spring.ai.openai", name = "api-key")
public class AiAutoConfiguration {

    @Autowired(required = false)
    private ChatClient.Builder chatClientBuilder;

    @Autowired
    private DesensitizeConfigProperties configProperties;

    @EventListener(ContextRefreshedEvent.class)
    public void init() {
        if (chatClientBuilder == null) {
            log.warn("ChatClient.Builder 未配置，AI脱敏功能不可用");
            return;
        }

        ChatClient client = chatClientBuilder.build();
        AiDesensitizeUtil.initialize(client, configProperties);
        log.info("AI脱敏自动配置完成");
    }
}
