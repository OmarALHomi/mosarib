package com.example.core.security

import android.content.Context
import android.provider.Settings
import java.security.MessageDigest

/**
 * Binds the app to the first device it runs on.
 *
 * On first launch, the device identifier (hashed with a fixed salt) is stored
 * in private SharedPreferences. Subsequent launches compare the current device hash;
 * if it differs, the app refuses to open.
 *
 * ponytail: ANDROID_ID resets on factory-reset — acceptable ceiling for a
 *   local-only app. Upgrade path: server-side license check.
 */
object DeviceLockManager {

    private const val PREF_FILE = "device_lock_prefs"
    private const val KEY_DEVICE_HASH = "bound_device_hash"
    private const val SALT = "mosarib_salt_water_well_security_v1"

    private fun currentDeviceId(context: Context): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device"

    private fun hashDeviceId(deviceId: String): String {
        val bytes = (deviceId + SALT).toByteArray(Charsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Returns `true` if this device is authorized (or if this is the first launch
     * and the device was just registered).
     */
    fun verifyOrRegister(context: Context): Boolean {
        val sp = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
        val storedHash = sp.getString(KEY_DEVICE_HASH, null)
        val currentHash = hashDeviceId(currentDeviceId(context))

        return if (storedHash == null) {
            // First launch — bind to this device
            sp.edit().putString(KEY_DEVICE_HASH, currentHash).apply()
            true
        } else {
            storedHash == currentHash
        }
    }
}
