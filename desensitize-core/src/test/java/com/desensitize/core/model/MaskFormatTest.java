package com.desensitize.core.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MaskFormatTest {

    @Test
    void testParsePreserve() {
        MaskFormat format = MaskFormat.parse("preserve(3,4)");
        assertEquals(MaskFormatType.PRESERVE, format.getType());
        assertEquals(3, format.getParam1());
        assertEquals(4, format.getParam2());
    }

    @Test
    void testParsePreserveZero() {
        MaskFormat format = MaskFormat.parse("preserve(1,0)");
        assertEquals(MaskFormatType.PRESERVE, format.getType());
        assertEquals(1, format.getParam1());
        assertEquals(0, format.getParam2());
    }

    @Test
    void testParseMaskAll() {
        MaskFormat format = MaskFormat.parse("maskAll()");
        assertEquals(MaskFormatType.MASK_ALL, format.getType());
        assertNull(format.getParam1());
        assertNull(format.getParam2());
    }

    @Test
    void testParseReplace() {
        MaskFormat format = MaskFormat.parse("replace(2,4)");
        assertEquals(MaskFormatType.REPLACE, format.getType());
        assertEquals(2, format.getParam1());
        assertEquals(4, format.getParam2());
    }

    @Test
    void testParseInvalidFormat() {
        assertThrows(IllegalArgumentException.class, () -> MaskFormat.parse("invalid(1,2)"));
        assertThrows(IllegalArgumentException.class, () -> MaskFormat.parse(""));
        assertThrows(IllegalArgumentException.class, () -> MaskFormat.parse(null));
    }

    @Test
    void testParsePreserveWrongArgs() {
        assertThrows(IllegalArgumentException.class, () -> MaskFormat.parse("preserve(1)"));
        assertThrows(IllegalArgumentException.class, () -> MaskFormat.parse("preserve(1,2,3)"));
    }
}
