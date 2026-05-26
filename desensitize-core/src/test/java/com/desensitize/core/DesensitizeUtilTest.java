package com.desensitize.core;

import com.desensitize.core.engine.DesensitizeUtil;
import com.desensitize.core.model.SensitiveTypeConfig;
import com.desensitize.core.registry.DesensitizeRuleRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DesensitizeUtilTest {

    @BeforeEach
    void setUp() {
        DesensitizeRuleRegistry registry = new DesensitizeRuleRegistry();

        registry.register(SensitiveTypeConfig.builder()
                .typeId("chinese_name")
                .regexPattern("[\u4e00-\u9fff]{2,4}")
                .maskFormat("nameMask()")
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

        registry.register(SensitiveTypeConfig.builder()
                .typeId("id_card")
                .regexPattern("[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]")
                .maskFormat("preserve(3,4)")
                .maskChar("*")
                .maskFlag(true)
                .build());

        registry.register(SensitiveTypeConfig.builder()
                .typeId("nationality")
                .regexPattern("[\u4e00-\u9fff]{2,10}")
                .maskFormat("maskAll()")
                .maskChar("*")
                .maskFlag(false)
                .build());

        registry.register(SensitiveTypeConfig.builder()
                .typeId("ethnicity")
                .regexPattern("[\u4e00-\u9fff]{1,3}族")
                .maskFormat("maskAll()")
                .maskChar("*")
                .maskFlag(false)
                .build());

        DesensitizeUtil.setRegistry(registry);
    }

    @Test
    void testMaskChineseName() {
        assertEquals("张*", DesensitizeUtil.mask("张三", "chinese_name"));
        assertEquals("王小*", DesensitizeUtil.mask("王小明", "chinese_name"));
        assertEquals("李*", DesensitizeUtil.mask("李四", "chinese_name"));
    }

    @Test
    void testMaskPhone() {
        assertEquals("138****8000", DesensitizeUtil.mask("13800138000", "phone"));
        assertEquals("199****5678", DesensitizeUtil.mask("19912345678", "phone"));
    }

    @Test
    void testMaskIdCard() {
        assertEquals("110***********1234", DesensitizeUtil.mask("110101199001011234", "id_card"));
    }

    @Test
    void testMaskNationality() {
        assertEquals("中国", DesensitizeUtil.mask("中国", "nationality"));
        assertEquals("美国", DesensitizeUtil.mask("美国", "nationality"));
    }

    @Test
    void testMaskFlagFalse() {
        assertEquals("汉族", DesensitizeUtil.mask("汉族", "ethnicity"));
        assertEquals("藏族", DesensitizeUtil.mask("藏族", "ethnicity"));
    }

    @Test
    void testMaskNull() {
        assertNull(DesensitizeUtil.mask(null, "phone"));
    }

    @Test
    void testMaskEmpty() {
        assertEquals("", DesensitizeUtil.mask("", "phone"));
    }

    @Test
    void testMaskCustomChar() {
        DesensitizeRuleRegistry registry = new DesensitizeRuleRegistry();
        registry.register(SensitiveTypeConfig.builder()
                .typeId("phone")
                .regexPattern("1[3-9]\\d{9}")
                .maskFormat("preserve(3,4)")
                .maskChar("#")
                .maskFlag(true)
                .build());
        DesensitizeUtil.setRegistry(registry);

        assertEquals("138####8000", DesensitizeUtil.mask("13800138000", "phone"));
    }

    @Test
    void testMaskLongText() {
        String text = "我叫张三，我的手机号是13800138000，身份证号是110101199001011234";
        String result = DesensitizeUtil.maskLongText(text);

        assertTrue(result.contains("张*"));
        assertTrue(result.contains("138****8000"));
        assertTrue(result.contains("110***********1234"));
        assertFalse(result.contains("张三"));
        assertFalse(result.contains("13800138000"));
    }

    @Test
    void testMaskLongTextNoMatch() {
        String text = "这是一段普通的文本，没有敏感信息";
        String result = DesensitizeUtil.maskLongText(text);
        assertEquals(text, result);
    }
}
