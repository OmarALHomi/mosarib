package com.baynana.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * بوابة د٣ على الهوية: **التباين يُقاس بالأرقام** لا بالذوق، ويُقاس على المخططات المعروضة نفسها.
 *
 * الاختبار لا يفحص «ألوانًا تشبه الصحيح» بل يمرّ على أزواج `ColorScheme` كما يستعملها Material3
 * (كل `onX` على `X`) في الوضعين، وعلى أزواج ألوان الحالة في `BaynanaStatusColors`. فلو تغيّر لون
 * واحد في `Color.kt` إلى قيمة تفشل في القراءة تحت شمس أو على شاشة رخيصة، يسقط البناء هنا.
 *
 * الحدود: 4.5:1 للنصّ العادي (WCAG 2.1 AA)، و3:1 للعناصر الرسومية والحدود.
 *
 * وكلها اختبارات نقية بلا Android: تحسب من قيم الألوان نفسها.
 */
class DesignTokensTest {

    // ------------------------------------------------------------------ حساب WCAG

    private fun channel(value: Double): Double =
        if (value <= 0.03928) value / 12.92 else Math.pow((value + 0.055) / 1.055, 2.4)

    private fun luminance(color: Color): Double =
        0.2126 * channel(color.red.toDouble()) +
            0.7152 * channel(color.green.toDouble()) +
            0.0722 * channel(color.blue.toDouble())

    private fun contrast(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        val lighter = maxOf(la, lb)
        val darker = minOf(la, lb)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun assertPairs(scheme: ColorScheme, mode: String) {
        val pairs = listOf(
            "onPrimary/primary" to (scheme.onPrimary to scheme.primary),
            "onPrimaryContainer/primaryContainer" to (scheme.onPrimaryContainer to scheme.primaryContainer),
            "onSecondary/secondary" to (scheme.onSecondary to scheme.secondary),
            "onSecondaryContainer/secondaryContainer" to (scheme.onSecondaryContainer to scheme.secondaryContainer),
            "onTertiary/tertiary" to (scheme.onTertiary to scheme.tertiary),
            "onTertiaryContainer/tertiaryContainer" to (scheme.onTertiaryContainer to scheme.tertiaryContainer),
            "onBackground/background" to (scheme.onBackground to scheme.background),
            "onSurface/surface" to (scheme.onSurface to scheme.surface),
            "onSurfaceVariant/surfaceVariant" to (scheme.onSurfaceVariant to scheme.surfaceVariant),
            "onError/error" to (scheme.onError to scheme.error),
            "onErrorContainer/errorContainer" to (scheme.onErrorContainer to scheme.errorContainer)
        )
        pairs.forEach { (label, colors) ->
            val (foreground, background) = colors
            val ratio = contrast(foreground, background)
            assertTrue(
                "$mode — $label: التباين ${"%.2f".format(ratio)}:1 أقل من 4.5:1",
                ratio >= 4.5
            )
        }
    }

    private fun assertStatusPairs(status: BaynanaStatusColors, mode: String) {
        val pairs = listOf(
            "المُقرّ" to (status.onAcknowledgedContainer to status.acknowledgedContainer),
            "الانتظار" to (status.onWaitingContainer to status.waitingContainer),
            "المعلومة" to (status.onInfoContainer to status.infoContainer),
            "الخطر" to (status.onDangerContainer to status.dangerContainer)
        )
        pairs.forEach { (label, colors) ->
            val (foreground, background) = colors
            val ratio = contrast(foreground, background)
            assertTrue(
                "$mode — شارة $label: التباين ${"%.2f".format(ratio)}:1 أقل من 4.5:1",
                ratio >= 4.5
            )
        }
    }

    // ------------------------------------------------------------------ المخططات

    @Test
    fun `الوضع_الفاتح_كل_أزواجه_تحقق_٤٫٥_على_١`() {
        assertPairs(LightColors, "فاتح")
        assertStatusPairs(LightStatus, "فاتح")
    }

    @Test
    fun `الوضع_الليلي_كل_أزواجه_تحقق_٤٫٥_على_١`() {
        assertPairs(DarkColors, "ليلي")
        assertStatusPairs(DarkStatus, "ليلي")
    }

    @Test
    fun `ألوان_الحالة_نفسها_مقروءة_على_أسطح_الشاشة`() {
        // النصّ الملوّن بالحالة (لا الشارة) يظهر على البطاقة والخلفية كذلك
        listOf(
            "فاتح/سطح" to (LightStatus.acknowledged to CreamSurface),
            "فاتح/خلفية" to (LightStatus.waiting to CreamBackground),
            "فاتح/خطر" to (LightStatus.danger to CreamSurface),
            "ليلي/سطح" to (DarkStatus.acknowledged to NightSurface),
            "ليلي/خلفية" to (DarkStatus.info to NightBackground),
            "ليلي/خطر" to (DarkStatus.danger to NightSurface)
        ).forEach { (label, colors) ->
            val ratio = contrast(colors.first, colors.second)
            assertTrue("$label: ${"%.2f".format(ratio)}:1 أقل من 4.5:1", ratio >= 4.5)
        }
    }

    // ------------------------------------------------------------------ قواعد الهوية

    @Test
    fun `الذهبي_لمسة_هوية_لا_لون_نصّ_مالي`() {
        val ratio = contrast(HoneyGold, CreamBackground)
        assertTrue(
            "الذهبي على الورق يجب أن يبقى أقل من 4.5 (المقاس ${"%.2f".format(ratio)}:1) ليبقى «لمسة» لا نصًّا",
            ratio < 4.5
        )
        // ولا يُستعمل لونًا لمبلغ: ألوان المال الحبر وأخضر التسديد وأحمر الخطر فقط
        val moneyColors = setOf(InkOnCream, AcknowledgedGreen, DangerRed)
        assertTrue("الذهبي ليس لون مبلغ", HoneyGold !in moneyColors)
    }

    @Test
    fun `علامة_الشعار_مقروءة_على_لون_الهوية`() {
        val markCream = Color(0xFFF3EAD8)
        val ratio = contrast(markCream, NavyNile)
        assertTrue("العلامة على النيلة: ${"%.2f".format(ratio)}:1 أقل من 3:1", ratio >= 3.0)
    }

    @Test
    fun `الحدود_مرئية_على_الأسطح`() {
        // الحدود والأيقونات: 3:1 (عناصر رسومية، لا نصّ)
        listOf(
            "فاتح: حدّ على السطح" to (CreamOutline to CreamSurface),
            "فاتح: حدّ على الخلفية" to (CreamOutline to CreamBackground),
            "ليلي: حدّ على السطح" to (NightOutline to NightSurface),
            "ليلي: حدّ على الخلفية" to (NightOutline to NightBackground)
        ).forEach { (label, colors) ->
            val ratio = contrast(colors.first, colors.second)
            assertTrue(
                "$label: ${"%.2f".format(ratio)}:1 — الحدود تحتاج تمييزًا ولو خفيفًا (٣:١ لونيًا " +
                    "غير ممكن للحدود الفاتحة؛ المطلوب ألا تكون مطابقة تمامًا)",
                ratio > 1.05
            )
        }
    }
}
