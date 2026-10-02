package com.example.core.security

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat

/** Native biometric authentication. Only an actual successful system callback can unlock. */
object BiometricHelper {

    @androidx.annotation.VisibleForTesting
    var overrideAvailabilityForTesting: Boolean? = null

    /** Returns true when the device has enrolled biometrics the app can use. */
    @Suppress("DEPRECATION")
    fun isAvailable(context: Context): Boolean {
        overrideAvailabilityForTesting?.let { return it }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val bm = context.getSystemService(android.hardware.biometrics.BiometricManager::class.java)
                val authenticators = android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK
                if (bm?.canAuthenticate(authenticators) != android.hardware.biometrics.BiometricManager.BIOMETRIC_SUCCESS) {
                    return false
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val bm = context.getSystemService(android.hardware.biometrics.BiometricManager::class.java)
                if (bm?.canAuthenticate() != android.hardware.biometrics.BiometricManager.BIOMETRIC_SUCCESS) {
                    return false
                }
            }

            val fm = context.getSystemService(FingerprintManager::class.java)
            if (fm == null || !fm.isHardwareDetected || !fm.hasEnrolledFingerprints()) {
                return false
            }
            return true
        } catch (_: RuntimeException) {
            // Missing permission or an unavailable system service is not authentication.
        }
        return false
    }

    /** The caller must cancel the returned request when its screen is disposed. */
    @Suppress("DEPRECATION")
    fun authenticate(
        activity: Activity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ): CancellationSignal {
        val signal = CancellationSignal()
        if (!isAvailable(activity)) {
            onError("البصمة غير متاحة. يمكنك استخدام قفل الهاتف إذا كان مفعّلاً")
            return signal
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val executor = ContextCompat.getMainExecutor(activity)
                val callback = object : android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: android.hardware.biometrics.BiometricPrompt.AuthenticationResult?) {
                        if (!signal.isCanceled) onSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                        if (!signal.isCanceled) onError(errString?.toString() ?: "فشل التحقق من البصمة")
                    }
                }
                val prompt = android.hardware.biometrics.BiometricPrompt.Builder(activity)
                    .setTitle("جِربة")
                    .setSubtitle("أدخل البصمة لتأكيد الهوية وفتح التطبيق")
                    .setNegativeButton("إلغاء", executor) { _: DialogInterface, _: Int ->
                        if (!signal.isCanceled) onError("تم إلغاء المصادقة")
                        signal.cancel()
                    }
                    .build()
                prompt.authenticate(signal, executor, callback)
            } else {
                // Android 7/8 are supported by minSdk: authenticate, never silently pass.
                val fm = activity.getSystemService(FingerprintManager::class.java)
                if (fm == null) {
                    onError("خدمة البصمة غير متاحة")
                } else {
                    fm.authenticate(null, signal, 0, object : FingerprintManager.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: FingerprintManager.AuthenticationResult?) {
                            if (!signal.isCanceled) onSuccess()
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                            if (!signal.isCanceled) onError(errString?.toString() ?: "فشل التحقق من البصمة")
                        }

                        override fun onAuthenticationFailed() {
                            if (!signal.isCanceled) onError("لم يتم التعرف على البصمة، حاول مرة أخرى")
                        }
                    }, null)
                }
            }
        } catch (_: RuntimeException) {
            signal.cancel()
            onError("تعذر بدء التحقق من البصمة. استخدم قفل الهاتف أو أعد المحاولة")
        }
        return signal
    }
}
