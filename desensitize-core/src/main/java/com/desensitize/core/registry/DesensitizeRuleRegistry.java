package com.desensitize.core.registry;

import com.desensitize.core.model.SensitiveTypeConfig;
import com.desensitize.core.model.SubSensitiveTypeConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class DesensitizeRuleRegistry {

    private final Map<String, SensitiveTypeConfig> typeConfigMap = new ConcurrentHashMap<>();

    private final List<SensitiveTypeConfig> orderedTypes = new ArrayList<>();

    public void register(SensitiveTypeConfig config) {
        if (config.getTypeId() == null || config.getTypeId().isBlank()) {
            throw new IllegalArgumentException("敏感类型配置的typeId不能为空");
        }

        if (config.isStrategyType()) {
            registerStrategyType(config);
        } else {
            if (config.getRegexPattern() == null || config.getRegexPattern().isBlank()) {
                throw new IllegalArgumentException("敏感类型 [" + config.getTypeId() + "] 的regexPattern不能为空");
            }
            if (config.getMaskFormat() == null || config.getMaskFormat().isBlank()) {
                throw new IllegalArgumentException("敏感类型 [" + config.getTypeId() + "] 的maskFormat不能为空");
            }
            typeConfigMap.put(config.getTypeId(), config);
            orderedTypes.add(config);
            log.info("已注册敏感类型: typeId={}, name={}", config.getTypeId(), config.getName());
        }
    }

    private void registerStrategyType(SensitiveTypeConfig config) {
        typeConfigMap.put(config.getTypeId(), config);
        orderedTypes.add(config);
        log.info("已注册策略类型: typeId={}, name={}", config.getTypeId(), config.getName());

        if (config.getSubTypes() != null && !config.getSubTypes().isEmpty()) {
            String parentMaskChar = config.getMaskChar();
            for (SubSensitiveTypeConfig subType : config.getSubTypes()) {
                SensitiveTypeConfig subConfig = SensitiveTypeConfig.builder()
                        .typeId(subType.getTypeId())
                        .name(subType.getName())
                        .regexPattern(subType.getRegexPattern())
                        .maskFormat(subType.getMaskFormat())
                        .maskChar(parentMaskChar != null ? parentMaskChar : subType.getMaskChar())
                        .maskFlag(subType.isMaskFlag())
                        .build();
                typeConfigMap.put(subConfig.getTypeId(), subConfig);
                orderedTypes.add(subConfig);
                log.info("已注册子类型: typeId={}, name={}", subConfig.getTypeId(), subConfig.getName());
            }
        }
    }

    public SensitiveTypeConfig getTypeConfig(String typeId) {
        SensitiveTypeConfig config = typeConfigMap.get(typeId);
        if (config == null) {
            log.warn("未找到敏感类型配置: typeId={}", typeId);
        }
        return config;
    }

    public SensitiveTypeConfig getConfig(String typeId) {
        return getTypeConfig(typeId);
    }

    public boolean isStrategyType(String typeId) {
        SensitiveTypeConfig config = typeConfigMap.get(typeId);
        return config != null && config.isStrategyType();
    }

    public List<SubSensitiveTypeConfig> getSubTypes(String typeId) {
        SensitiveTypeConfig config = typeConfigMap.get(typeId);
        if (config != null && config.getSubTypes() != null) {
            return config.getSubTypes();
        }
        return Collections.emptyList();
    }

    public List<SensitiveTypeConfig> getAllTypes() {
        return new ArrayList<>(orderedTypes);
    }

    public List<SensitiveTypeConfig> getTypeConfigs() {
        return new ArrayList<>(orderedTypes);
    }

    public int getTypeCount() {
        return typeConfigMap.size();
    }

    public void clear() {
        typeConfigMap.clear();
        orderedTypes.clear();
    }
}