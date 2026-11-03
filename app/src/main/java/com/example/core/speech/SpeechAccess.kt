package com.example.core.speech

import android.content.Context
import com.example.core.license.LicenseManager

/**
 * Single authority for voice-related entitlement checks.
 * Reading aloud is free for everyone; only WhatsApp voice statement export requires an active license.
 */
object SpeechAccess {
    fun canSendVoiceStatement(context: Context): Boolean = LicenseManager.isActivated(context)
}
