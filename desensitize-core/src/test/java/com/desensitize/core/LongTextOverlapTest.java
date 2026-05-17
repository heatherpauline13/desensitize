package com.desensitize.core;

import com.desensitize.core.engine.DesensitizeUtil;
import com.desensitize.core.model.SensitiveTypeConfig;
import com.desensitize.core.registry.DesensitizeRuleRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LongTextOverlapTest {

    @BeforeEach
    void setUp() {
        DesensitizeRuleRegistry registry = new DesensitizeRuleRegistry();

        registry.register(SensitiveTypeConfig.builder()
                .typeId("short_match")
                .regexPattern("1380")
                .maskFormat("maskAll()")
                .maskChar("*")
                .maskFlag(true)
                .build());

        registry.register(SensitiveTypeConfig.builder()
                .typeId("phone")
                .regexPattern("1[3-9]\\d{9}")
                .maskFormat("preserve(3,4)")
                .maskChar("*")
                .maskFlag(true)
                .build());

        DesensitizeUtil.setRegistry(registry);
    }

    @Test
    void testLongMatchWins() {
        String text = "我的手机是13800138000";
        String result = DesensitizeUtil.maskLongText(text);

        assertEquals("我的手机是138****8000", result);
    }
}
