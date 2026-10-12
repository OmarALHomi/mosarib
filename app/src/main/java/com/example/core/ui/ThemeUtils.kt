package com.example.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.luminance

/**
 * دالة ذكية تحدد بدقة ما إذا كان التطبيق حالياً في الوضع المظلم أم الفاتح
 * بالاعتماد على درجات ألوان MaterialTheme الفعلية، وليس على إعداد نظام الهاتف فقط.
 */
@Composable
fun isAppDark(): Boolean {
    return MaterialTheme.colorScheme.surface.luminance() < 0.5f
}
