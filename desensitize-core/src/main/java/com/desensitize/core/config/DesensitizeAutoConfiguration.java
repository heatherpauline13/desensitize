package com.desensitize.core.config;

import com.desensitize.core.engine.DesensitizeUtil;
import com.desensitize.core.model.SensitiveTypeConfig;
import com.desensitize.core.registry.DesensitizeRuleRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Slf4j
@Configuration
@EnableConfigurationProperties(DesensitizeConfigProperties.class)
public class DesensitizeAutoConfiguration {

    @Autowired
    private DesensitizeConfigProperties configProperties;

    @Bean
    @ConditionalOnMissingBean
    public DesensitizeRuleRegistry desensitizeRuleRegistry() {
        DesensitizeRuleRegistry registry = new DesensitizeRuleRegistry();

        loadFromConfigProperties(registry);

        DesensitizeUtil.setRegistry(registry);

        if (registry.getTypeCount() == 0) {
            log.warn("未加载到任何脱敏规则配置，请检查 desensitize-config.yml 文件");
        } else {
            log.info("脱敏规则注册中心初始化完成，共加载 {} 种敏感数据类型", registry.getTypeCount());
        }

        return registry;
    }

    private void loadFromConfigProperties(DesensitizeRuleRegistry registry) {
        List<SensitiveTypeConfig> types = configProperties.getTypes();
        if (types != null && !types.isEmpty()) {
            for (SensitiveTypeConfig config : types) {
                if (config.getMaskChar() == null || config.getMaskChar().isBlank()) {
                    config.setMaskChar(configProperties.getMaskChar());
                }
                registry.register(config);
            }
        }
    }
}
