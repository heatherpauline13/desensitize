package com.desensitize.core.config;

import com.desensitize.core.model.SensitiveTypeConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "desensitize")
public class DesensitizeConfigProperties {

    private String configPath;

    private String maskChar = "*";

    private List<SensitiveTypeConfig> types = new ArrayList<>();

    private AiConfig ai = new AiConfig();

    @Data
    public static class AiConfig {
        private String promptTemplate;

        private String auditPromptTemplate;

        private String stringAuditPromptTemplate;
    }
}
