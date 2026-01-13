package com.linkshortener;

import com.linkshortener.core.generator.ShortCodeGenerator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShortCodeGeneratorTest {

    @Test
    void testGenerateCodeLength() {
        int length = 8;
        String code = ShortCodeGenerator.generateCode(length);
        assertEquals(length, code.length());
    }

    @Test
    void testGenerateCodeUniqueness() {
        String code1 = ShortCodeGenerator.generateCode(6);
        String code2 = ShortCodeGenerator.generateCode(6);
        assertNotEquals(code1, code2);
    }

    @Test
    void testCodeUsedTracking() {
        String code = ShortCodeGenerator.generateCode(6);
        assertTrue(ShortCodeGenerator.isCodeUsed(code));

        ShortCodeGenerator.releaseCode(code);
        assertFalse(ShortCodeGenerator.isCodeUsed(code));
    }

    @Test
    void testAlphabetCharacters() {
        String code = ShortCodeGenerator.generateCode(10);
        assertTrue(code.matches("^[A-Za-z0-9]+$"));
    }
}