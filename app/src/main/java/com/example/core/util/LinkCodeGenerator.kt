package com.example.core.util

import java.security.SecureRandom

/**
 * Generates and validates clean, human-friendly 5-character link codes
 * for pairing Musrib and Farmer accounts (e.g., 7K9P2).
 * Excludes ambiguous characters (0, O, 1, I).
 */
object LinkCodeGenerator {
    private const val ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
    private val random = SecureRandom()

    fun generate(length: Int = 5): String {
        return (1..length)
            .map { ALPHABET[random.nextInt(ALPHABET.length)] }
            .joinToString("")
    }

    fun isValid(code: String): Boolean {
        val clean = code.trim().uppercase()
        if (clean.length != 5) return false
        return clean.all { it in ALPHABET }
    }

    fun format(code: String): String = code.trim().uppercase()
}
