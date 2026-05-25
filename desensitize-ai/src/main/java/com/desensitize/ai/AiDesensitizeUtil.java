package com.desensitize.ai;

import com.desensitize.core.config.DesensitizeConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.time.Duration;

@Slf4j
public class AiDesensitizeUtil {

    private static volatile ChatClient chatClient;

    private static volatile String promptTemplate;

    private static volatile Duration timeout = Duration.ofSeconds(30);

    private AiDesensitizeUtil() {
    }

    public static void initialize(ChatClient client, DesensitizeConfigProperties properties) {
        chatClient = client;
        if (properties.getAi() != null && properties.getAi().getPromptTemplate() != null) {
            promptTemplate = properties.getAi().getPromptTemplate();
        } else {
            throw new IllegalStateException("AI脱敏提示词模板未配置，请在 desensitize-config.yml 的 ai.prompt-template 中配置");
        }
        log.info("AI脱敏模块初始化完成，提示词模板已加载");
    }

    public static String mask(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        if (!isAvailable()) {
            log.warn("AI脱敏模块未初始化，返回原值");
            return content;
        }

        try {
            String userMessage = promptTemplate + "\n\n待脱敏内容：\n" + content;

            String result = chatClient.prompt()
                    .user(userMessage)
                    .call()
                    .content();

            log.info("AI远程处理结果:\n{}\n 输入长度: {}, 输出长度: {}", result,content.length(), result != null ? result.length() : 0);

            return result != null ? result : content;
        } catch (Exception e) {
            log.error("AI脱敏调用失败: {}", e.getMessage());
            throw new RuntimeException("AI脱敏调用失败: " + e.getMessage(), e);
        }
    }

    public static boolean isAvailable() {
        return chatClient != null && promptTemplate != null && !promptTemplate.isBlank();
    }
}
