package com.example.core.voice

import com.example.features.customers.Customer
import kotlin.math.min

/**
 * Pure customer matching engine combining Arabic normalization, token overlap,
 * token Levenshtein edit distance, and learned voice aliases.
 */
object CustomerMatcher {

    data class CustomerMatch(
        val customer: Customer,
        val score: Float,
        val isExactOrLearned: Boolean
    )

    /**
     * Normalizes a customer name for robust comparison:
     * - Strips diacritics and tatweel
     * - Normalizes alef, taa marbuta, and alef maqsura
     * - Drops "بن" and "ابن"
     * - Drops leading "ال" on individual tokens
     */
    fun normalizeForMatching(text: String): List<String> {
        val norm = SpokenNumberParser.normalizeToken(text)
        return norm.split(Regex("[\\s,،\\-_.]+"))
            .map { token ->
                var t = token.trim()
                // Drop leading "ال" if word length > 3
                if (t.startsWith("ال") && t.length > 3) {
                    t = t.substring(2)
                }
                t
            }
            .filter { it.isNotBlank() && it !in listOf("بن", "ابن", "ابنه", "ابنة", "ال") }
    }

    /**
     * Standard Levenshtein distance between two strings.
     */
    fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(
                    min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s1.length][s2.length]
    }

    /**
     * Token similarity: 1.0 for exact, 0.85 for 1-char edit distance (len >= 4), 0.70 for 2-char (len >= 6).
     */
    private fun tokenSimilarity(t1: String, t2: String): Float {
        if (t1 == t2) return 1.0f
        val maxLen = maxOf(t1.length, t2.length)
        if (maxLen < 3) return 0.0f

        val dist = levenshteinDistance(t1, t2)
        return when {
            dist == 1 && maxLen >= 4 -> 0.85f
            dist == 2 && maxLen >= 6 -> 0.70f
            else -> 0.0f
        }
    }

    /**
     * Matches a query against a customer list, incorporating learned aliases.
     * Returns a ranked list sorted by descending match score.
     */
    fun match(
        query: String,
        customers: List<Customer>,
        aliases: Map<String, Long> = emptyMap()
    ): List<CustomerMatch> {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank() || customers.isEmpty()) return emptyList()

        val normalizedQueryKey = SpokenNumberParser.normalizeToken(cleanQuery).trim()
        val aliasCustId = aliases[normalizedQueryKey]

        val queryTokens = normalizeForMatching(cleanQuery)
        if (queryTokens.isEmpty()) return emptyList()

        val matches = mutableListOf<CustomerMatch>()

        for (c in customers) {
            // 1. Check learned aliases (highest priority)
            if (aliasCustId != null && c.id == aliasCustId) {
                matches.add(CustomerMatch(customer = c, score = 2.0f, isExactOrLearned = true))
                continue
            }

            val custTokens = normalizeForMatching(c.name)
            if (custTokens.isEmpty()) continue

            // 2. Exact full token match
            if (queryTokens == custTokens) {
                matches.add(CustomerMatch(customer = c, score = 1.0f, isExactOrLearned = true))
                continue
            }

            // 3. Token overlap and similarity score
            var tokenScoreSum = 0.0f
            for (q in queryTokens) {
                var bestSim = 0.0f
                for (ct in custTokens) {
                    val sim = tokenSimilarity(q, ct)
                    if (sim > bestSim) bestSim = sim
                }
                tokenScoreSum += bestSim
            }

            val overlapRatio = tokenScoreSum / queryTokens.size.toFloat()

            // Bonus if all query tokens are present in customer name
            val isSubset = queryTokens.all { q ->
                custTokens.any { ct -> tokenSimilarity(q, ct) >= 0.85f }
            }

            val finalScore = if (isSubset) {
                // Prioritize closer match on extra tokens: fewer extra tokens gives higher score
                val penalty = (custTokens.size - queryTokens.size).coerceAtLeast(0) * 0.05f
                (0.95f - penalty).coerceIn(0.70f, 0.95f)
            } else {
                overlapRatio * 0.80f
            }

            if (finalScore >= 0.35f) {
                matches.add(CustomerMatch(customer = c, score = finalScore, isExactOrLearned = false))
            }
        }

        return matches.sortedByDescending { it.score }
    }

    /**
     * Determines whether the best match is confident enough to accept automatically without showing a chooser dialog.
     * Criteria:
     * - Best score >= 0.85
     * - If there is a runner-up, the margin between best and second must be >= 0.20
     */
    fun isHighConfidence(matches: List<CustomerMatch>): Boolean {
        val best = matches.firstOrNull() ?: return false
        if (best.isExactOrLearned) return true
        if (best.score < 0.85f) return false
        val second = matches.getOrNull(1) ?: return true
        return (best.score - second.score) >= 0.20f
    }
}
