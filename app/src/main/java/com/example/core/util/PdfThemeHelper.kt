package com.example.core.util

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.example.R

/**
 * Loads the project's Cairo font family for high-quality, professional Arabic PDF rendering.
 * Falls back gracefully to system Typeface if resources are unavailable.
 */
object PdfThemeHelper {

    fun getCairoBold(context: Context): Typeface {
        return try {
            ResourcesCompat.getFont(context, R.font.cairo_bold) ?: Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        } catch (_: Exception) {
            Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }

    fun getCairoRegular(context: Context): Typeface {
        return try {
            ResourcesCompat.getFont(context, R.font.cairo_regular) ?: Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        } catch (_: Exception) {
            Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
    }

    fun getCairoSemiBold(context: Context): Typeface {
        return try {
            ResourcesCompat.getFont(context, R.font.cairo_semibold) ?: Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        } catch (_: Exception) {
            Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }

    // Professional Luxury Color Palette for Print & PDF
    const val COLOR_PRIMARY = 0xFF007A87.toInt()       // Deep Teal
    const val COLOR_PRIMARY_DARK = 0xFF004D56.toInt()  // Midnight Teal
    const val COLOR_GOLD = 0xFFD4AF37.toInt()          // Luxury Gold
    const val COLOR_DARK_TEXT = 0xFF0F172A.toInt()     // Slate 900
    const val COLOR_MUTED_TEXT = 0xFF475569.toInt()    // Slate 600
    const val COLOR_LIGHT_BG = 0xFFF8FAFC.toInt()      // Slate 50
    const val COLOR_CARD_BG = 0xFFF1F5F9.toInt()       // Slate 100
    const val COLOR_BORDER = 0xFFCBD5E1.toInt()        // Slate 300
    const val COLOR_SUCCESS = 0xFF16A34A.toInt()       // Green 600
    const val COLOR_DANGER = 0xFFDC2626.toInt()        // Red 600
}
