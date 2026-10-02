package com.baynana.core.util

import org.junit.Assert.*
import org.junit.Test

class LinkCodeGeneratorTest {

    @Test
    fun testGenerateLengthAndCharacters() {
        val code = LinkCodeGenerator.generate()
        assertEquals(5, code.length)
        assertTrue(LinkCodeGenerator.isValid(code))
        // Verify no ambiguous characters (0, O, 1, I)
        assertFalse(code.contains('0'))
        assertFalse(code.contains('O'))
        assertFalse(code.contains('1'))
        assertFalse(code.contains('I'))
    }

    @Test
    fun testUniqueness() {
        val codes = (1..50).map { LinkCodeGenerator.generate() }.toSet()
        assertEquals("50 generated codes should all be distinct", 50, codes.size)
    }

    @Test
    fun testValidation() {
        assertTrue(LinkCodeGenerator.isValid("7K9P2"))
        assertTrue(LinkCodeGenerator.isValid("  7k9p2  "))
        assertFalse(LinkCodeGenerator.isValid("7K9P")) // Too short
        assertFalse(LinkCodeGenerator.isValid("7K9P2X")) // Too long
        assertFalse(LinkCodeGenerator.isValid("7K9P0")) // Contains '0'
        assertFalse(LinkCodeGenerator.isValid("7K9PO")) // Contains 'O'
    }
}
