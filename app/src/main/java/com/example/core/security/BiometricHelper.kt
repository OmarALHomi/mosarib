package com.example.core.security

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat

/**
 * Biometric authentication helper using native Android framework APIs.
 * Requires zero external dependencies.
 */
object BiometricHelper {

    /** Returns true when the device has enrolled biometrics the app can use. */
    fun isAvailable(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val bm = context.getSystemService(android.hardware.biometrics.BiometricManager::class.java)
                bm?.canAuthenticate(android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
                        android.hardware.biometrics.BiometricManager.BIOMETRIC_SUCCESS
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                @Suppress("DEPRECATION")
                val fm = context.getSystemService(android.hardware.fingerprint.FingerprintManager::class.java)
                fm != null && fm.isHardwareDetected && fm.hasEnrolledFingerprints()
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Show the native system biometric prompt.
     */
    fun authenticate(
        activity: Activity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
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
                    onError(errString?.toString() ?: "فشل التحقق من البصمة")
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                }
            }

            val prompt = android.hardware.biometrics.BiometricPrompt.Builder(activity)
                .setTitle("المُسَرِّب")
                .setSubtitle("أدخل البصمة لتأكيد الهوية وفتح التطبيق")
                .setNegativeButton("إلغاء", executor) { _: DialogInterface, _: Int ->
                    cancellationSignal.cancel()
                    onError("تم إلغاء المصادقة")
                }
                .build()

            prompt.authenticate(cancellationSignal, executor, callback)
        } else {
            // Older than Android 9 — let it pass or notify
            onSuccess()
        }
    }
}
