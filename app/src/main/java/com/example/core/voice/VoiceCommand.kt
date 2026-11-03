package com.example.core.voice

enum class VoiceCommandType {
    SESSION,
    RECEIPT,
    EXPENSE,
    PURCHASE,
    DISCOUNT
}

data class VoiceCommand(
    val type: VoiceCommandType?,
    val customerQuery: String? = null,
    val durationMinutes: Int? = null,
    val ratePerHour: Double? = null,
    val amount: Double? = null,
    val paid: Double? = null,
    val category: String? = null,
    val notes: String? = null,
    val unparsed: List<String> = emptyList(),
    val rawText: String = ""
)
