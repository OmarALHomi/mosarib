package com.example.core.ui

import androidx.compose.runtime.staticCompositionLocalOf

/** True when the owner picked "مبسّطة" in Settings; screens hide rarely-used fields. */
val LocalSimpleMode = staticCompositionLocalOf { false }
