package com.baynana.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.baynana.R

val CairoFontFamily = FontFamily(
    Font(R.font.cairo_regular, FontWeight.Normal),
    Font(R.font.cairo_medium, FontWeight.Medium),
    Font(R.font.cairo_semibold, FontWeight.SemiBold),
    Font(R.font.cairo_bold, FontWeight.Bold),
    Font(R.font.cairo_extrabold, FontWeight.ExtraBold)
)

val Typography = Typography(
    headlineLarge   = TextStyle(fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold,     fontSize = 28.sp, lineHeight = 38.sp, letterSpacing = 0.sp),
    headlineMedium  = TextStyle(fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold,     fontSize = 24.sp, lineHeight = 34.sp, letterSpacing = 0.sp),
    headlineSmall   = TextStyle(fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold,     fontSize = 20.sp, lineHeight = 30.sp, letterSpacing = 0.sp),
    titleLarge      = TextStyle(fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold,     fontSize = 18.sp, lineHeight = 26.sp, letterSpacing = 0.sp),
    titleMedium     = TextStyle(fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold,     fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.sp),
    titleSmall      = TextStyle(fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold,     fontSize = 14.sp, lineHeight = 22.sp, letterSpacing = 0.sp),
    bodyLarge       = TextStyle(fontFamily = CairoFontFamily, fontWeight = FontWeight.Normal,   fontSize = 16.sp, lineHeight = 26.sp, letterSpacing = 0.sp),
    bodyMedium      = TextStyle(fontFamily = CairoFontFamily, fontWeight = FontWeight.Normal,   fontSize = 14.sp, lineHeight = 22.sp, letterSpacing = 0.sp),
    bodySmall       = TextStyle(fontFamily = CairoFontFamily, fontWeight = FontWeight.Medium,   fontSize = 12.sp, lineHeight = 18.sp, letterSpacing = 0.sp),
    labelLarge      = TextStyle(fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold,     fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.sp),
    labelMedium     = TextStyle(fontFamily = CairoFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 18.sp, letterSpacing = 0.sp),
    labelSmall      = TextStyle(fontFamily = CairoFontFamily, fontWeight = FontWeight.Medium,   fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 0.sp)
)
