package com.example.core.security

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat

/**
 * Biometric and device credential authentication helper using native Android framework APIs.
 * Requires zero external dependencies.
 */
object BiometricHelper {

    /** Returns true when the device has enrolled biometrics or a device credential (PIN/pattern/password). */
    fun isAvailable(context: Context): Boolean {
        // 1. Modern BiometricManager check (API 29+)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val bm = context.getSystemService(android.hardware.biometrics.BiometricManager::class.java)
                val authenticators = android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
                if (bm?.canAuthenticate(authenticators) == android.hardware.biometrics.BiometricManager.BIOMETRIC_SUCCESS) {
                    return true
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                @Suppress("DEPRECATION")
                val bm = context.getSystemService(android.hardware.biometrics.BiometricManager::class.java)
                if (bm?.canAuthenticate() == android.hardware.biometrics.BiometricManager.BIOMETRIC_SUCCESS) {
                    return true
                }
            }
        } catch (_: Throwable) {
            // Ignore and fall through to keyguard check
        }

        // 2. Keyguard check for secure screen lock (PIN, pattern, password)
        try {
            val km = context.getSystemService(android.app.KeyguardManager::class.java)
            if (km != null && km.isDeviceSecure) {
                return true
            }
        } catch (_: Throwable) {
            // Ignore
        }

        // 3. Legacy FingerprintManager fallback for Android 6.0 to 9.0 (API 23..28)
        try {
            if (Build.VERSION.SDK_INT in Build.VERSION_CODES.M..Build.VERSION_CODES.P) {
                @Suppress("DEPRECATION")
                val fm = context.getSystemService(android.hardware.fingerprint.FingerprintManager::class.java)
                if (fm != null && fm.isHardwareDetected && fm.hasEnrolledFingerprints()) {
                    return true
                }
            }
        } catch (_: Throwable) {
            // Ignore
        }

        return false
    }

    /**
     * Show the native system biometric prompt, with native phone PIN/Pattern/Password option.
     */
    fun authenticate(
        activity: Activity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isAvailable(activity)) {
            // If device has neither biometric nor device credential configured, bypass safely
            onSuccess()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val executor = ContextCompat.getMainExecutor(activity)
            val cancellationSignal = CancellationSignal()

            val callback = object : android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: android.hardware.biometrics.BiometricPrompt.AuthenticationResult?) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                    super.onAuthenticationError(errorCode, errString)
                    val msg = when (errorCode) {
                        android.hardware.biometrics.BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED,
                        android.hardware.biometrics.BiometricPrompt.BIOMETRIC_ERROR_CANCELED -> "تم إلغاء المصادقة"
                        android.hardware.biometrics.BiometricPrompt.BIOMETRIC_ERROR_LOCKOUT -> "تم حظر المحاولات مؤقتاً، يرجى المحاولة لاحقاً"
                        android.hardware.biometrics.BiometricPrompt.BIOMETRIC_ERROR_LOCKOUT_PERMANENT -> "تم قفل البصمة، استخدم رمز الهاتف"
                        else -> errString?.toString() ?: "فشل التحقق من الهوية"
                    }
                    onError(msg)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                }
            }

            val builder = android.hardware.biometrics.BiometricPrompt.Builder(activity)
                .setTitle("المُسَرِّب")
                .setSubtitle("تأكيد الهوية")
                .setDescription("البصمة أو رمز الهاتف للمتابعة")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+ (API 30+): Allow biometric or device credential (PIN / Pattern / Password).
                // When DEVICE_CREDENTIAL is set, setNegativeButton MUST NOT be called.
                val authenticators = android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
                builder.setAllowedAuthenticators(authenticators)
            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                // Android 10 (API 29): Allow device credential fallback.
                // When setDeviceCredentialAllowed(true) is called, setNegativeButton MUST NOT be called.
                @Suppress("DEPRECATION")
                builder.setDeviceCredentialAllowed(true)
            } else {
                // Android 9 (API 28): BiometricPrompt requires negative button
                builder.setNegativeButton("إلغاء", executor) { _: DialogInterface, _: Int ->
                    cancellationSignal.cancel()
                    onError("تم إلغاء المصادقة")
                }
            }

            try {
                val prompt = builder.build()
                prompt.authenticate(cancellationSignal, executor, callback)
            } catch (e: Exception) {
                onError(e.message ?: "تعذر بدء التحقق من الهوية")
            }
        } else {
            // Older than Android 9
            onSuccess()
        }
    }
}
