package com.example.core.ui.guide

import android.content.Context
import com.example.core.license.LicenseManager

/**
 * Manages persistence for spotlight tours and first-time hint tooltips.
 * Uses dedicated SharedPreferences file `mosarib_guides`.
 */
object GuidePreferences {
    private const val PREFS_FILE = "mosarib_guides"
    private const val PREFIX_TOUR = "tour_"
    private const val PREFIX_HINT = "hint_"

    const val TOUR_HOME = "home"
    const val TOUR_SESSION_FORM = "session_form"
    const val TOUR_VOUCHER_FORM = "voucher_form"
    const val TOUR_PURCHASE_FORM = "purchase_form"
    const val TOUR_NAV = "nav"

    fun isTourDone(context: Context, screenId: String): Boolean {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        if (sp.contains(PREFIX_TOUR + screenId)) {
            return sp.getBoolean(PREFIX_TOUR + screenId, false)
        }
        // Migration: If the legacy home tour was completed in LicenseManager, treat "home" as done
        if (screenId == TOUR_HOME && LicenseManager.isHomeTourCompleted(context)) {
            sp.edit().putBoolean(PREFIX_TOUR + screenId, true).apply()
            return true
        }
        return false
    }

    fun markTourDone(context: Context, screenId: String, done: Boolean = true) {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        sp.edit().putBoolean(PREFIX_TOUR + screenId, done).apply()
    }

    fun isHintDone(context: Context, id: String): Boolean {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        return sp.getBoolean(PREFIX_HINT + id, false)
    }

    fun markHintDone(context: Context, id: String, done: Boolean = true) {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        sp.edit().putBoolean(PREFIX_HINT + id, done).apply()
    }

    fun resetAll(context: Context) {
        val sp = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        sp.edit().clear().apply()
        // Clear legacy home tour flag as well so it does not auto-migrate back to true
        LicenseManager.setHomeTourCompleted(context, false)
    }
}
