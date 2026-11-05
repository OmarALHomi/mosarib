package com.example.core.notifications

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Screens a notification may open with a `mosarib://` link. Unknown links are ignored. */
enum class DeepLinkTarget { HOME, OPERATIONS, CUSTOMERS, REPORTS, SETTINGS, SETTINGS_BACKUP }

/**
 * `mosarib://` links from notifications (task 036). They reach MainActivity only through an explicit
 * intent built here, so no other app can trigger them.
 */
object DeepLinks {
    const val SCHEME = "mosarib"

    private val _pending = MutableStateFlow<DeepLinkTarget?>(null)
    val pending: StateFlow<DeepLinkTarget?> = _pending.asStateFlow()

    /** Pure parser: "mosarib://settings/backup" -> SETTINGS_BACKUP. */
    fun parse(link: String?): DeepLinkTarget? {
        val clean = link?.trim()?.lowercase() ?: return null
        if (!clean.startsWith("$SCHEME://")) return null
        val path = clean.removePrefix("$SCHEME://").trim('/')
        return when (path) {
            "", "home" -> DeepLinkTarget.HOME
            "operations", "vouchers", "sessions" -> DeepLinkTarget.OPERATIONS
            "customers" -> DeepLinkTarget.CUSTOMERS
            "reports" -> DeepLinkTarget.REPORTS
            "settings" -> DeepLinkTarget.SETTINGS
            "settings/backup", "backup" -> DeepLinkTarget.SETTINGS_BACKUP
            else -> null
        }
    }

    fun isDeepLink(link: String?): Boolean = link?.trim()?.lowercase()?.startsWith("$SCHEME://") == true

    fun appIntent(context: Context, link: String?): Intent =
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (isDeepLink(link)) data = Uri.parse(link!!.trim())
        }

    /** Called by MainActivity for the launch intent and for onNewIntent. */
    fun handleIntent(intent: Intent?) {
        val target = parse(intent?.data?.toString()) ?: return
        _pending.value = target
    }

    fun consume() {
        _pending.value = null
    }
}
