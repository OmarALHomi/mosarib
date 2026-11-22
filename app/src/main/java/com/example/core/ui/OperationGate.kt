package com.example.core.ui

import androidx.compose.runtime.staticCompositionLocalOf

/** Runs [action] only when a new operation is allowed; otherwise MainScreen opens the subscription sheet. */
typealias OperationGate = (action: () -> Unit) -> Unit

val LocalOperationGate = staticCompositionLocalOf<OperationGate> {
    error("LocalOperationGate must be provided by MainScreen")
}

/** The subscription sheet is pushed once per app start; later it opens only on demand. */
fun shouldAutoShowLicenseSheet(limitExceeded: Boolean, alreadyShown: Boolean): Boolean =
    limitExceeded && !alreadyShown

/** Opens the subscription sheet on demand (used by the Home banner). */
val LocalOpenSubscription = staticCompositionLocalOf<() -> Unit> { {} }
