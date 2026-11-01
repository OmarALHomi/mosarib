package com.example.core.license

import android.content.Context
import android.util.Log
import com.example.core.network.FirebaseRest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * License, subscription and support operations against Firebase Realtime Database.
 * All HTTP goes through [FirebaseRest].
 */
object FirebaseLicenseService {
    private const val TAG = "FirebaseLicenseService"
    private const val PREFS_FILE = "mosarib_license_prefs"

    data class LicenseCheckResult(
        val isValid: Boolean,
        val isRevoked: Boolean,
        val isBoundToOtherDevice: Boolean,
        val boundDeviceCode: String? = null,
        val serverExpiresAt: Long? = null,
        val plan: String? = null,
        val errorMessage: String? = null,
        val isNetworkError: Boolean = false
    )

    /**
     * Looks up an activation key issued from the admin dashboard. The app never creates license
     * records: a key is valid only if its record exists and is bound to this device. Only the
     * informational fields (last check, device model, name) are written back.
     */
    suspend fun verifyKeyOnline(
        key: String,
        currentDeviceCode: String,
        subscriberName: String
    ): LicenseCheckResult = withContext(Dispatchers.IO) {
        val cleanKey = key.replace("-", "").replace(" ", "").trim().uppercase()
        val candidates = if (cleanKey.startsWith("ACTV")) listOf(cleanKey) else listOf(cleanKey, "ACTV$cleanKey")

        var foundKey: String? = null
        var license: JSONObject? = null
        for (candidate in candidates) {
            val response = FirebaseRest.get("licenses/$candidate.json")
            if (!response.isSuccess) {
                return@withContext LicenseCheckResult(
                    isValid = false,
                    isRevoked = false,
                    isBoundToOtherDevice = false,
                    errorMessage = "خطأ في استجابة الخادم: ${response.code}",
                    isNetworkError = true
                )
            }
            license = response.jsonObject()
            if (license != null) {
                foundKey = candidate
                break
            }
        }

        if (foundKey == null || license == null) {
            return@withContext LicenseCheckResult(isValid = false, isRevoked = false, isBoundToOtherDevice = false, errorMessage = "رمز التفعيل غير موجود")
        }

        if (license.optBoolean("isRevoked", false)) {
            return@withContext LicenseCheckResult(
                isValid = false,
                isRevoked = true,
                isBoundToOtherDevice = false,
                errorMessage = "تم سحب أو إلغاء هذا الاشتراك من قِبل الإدارة"
            )
        }

        val boundDevice = license.optString("deviceCode", "")
        if (boundDevice != currentDeviceCode) {
            return@withContext LicenseCheckResult(
                isValid = false,
                isRevoked = false,
                isBoundToOtherDevice = boundDevice.isNotBlank(),
                boundDeviceCode = boundDevice.ifBlank { null },
                errorMessage = "هذا الكود مفعّل لجهاز آخر مسبقاً ولا يمكن استخدامه على جهازين!"
            )
        }

        val updatePayload = JSONObject().apply {
            put("lastCheckedAt", System.currentTimeMillis())
            put("deviceModel", LicenseManager.getDeviceModelName())
            put("androidVersion", LicenseManager.getAndroidVersionName())
            if (subscriberName.isNotBlank()) put("subscriberName", subscriberName.take(100))
        }
        FirebaseRest.patch("licenses/$foundKey.json", updatePayload.toString())

        val serverExpiresAt = license.optLong("expiresAt", 0L)
        LicenseCheckResult(
            isValid = true,
            isRevoked = false,
            isBoundToOtherDevice = false,
            boundDeviceCode = currentDeviceCode,
            serverExpiresAt = if (serverExpiresAt > 0) serverExpiresAt else null,
            plan = license.optString("plan", "").ifBlank { null }
        )
    }

    data class DeviceRemoteLicense(
        val isFound: Boolean,
        val isRevoked: Boolean,
        val expiresAt: Long = 0L,
        val plan: String = "MONTHLY",
        val subscriberName: String = "",
        val key: String = ""
    )

    suspend fun getDeviceRemoteLicense(deviceCode: String): DeviceRemoteLicense = withContext(Dispatchers.IO) {
        val obj = FirebaseRest.get("devices/$deviceCode.json", timeoutMs = 4000).jsonObject()
            ?: return@withContext DeviceRemoteLicense(isFound = false, isRevoked = false)
        DeviceRemoteLicense(
            isFound = true,
            isRevoked = obj.optBoolean("isRevoked", false),
            expiresAt = obj.optLong("expiresAt", 0L),
            plan = obj.optString("plan", "MONTHLY"),
            subscriberName = obj.optString("subscriberName", "المشترك"),
            key = obj.optString("key", obj.optString("displayKey", ""))
        )
    }

    /**
     * Periodic background check for license revocation.
     * If the developer revoked the license from the admin dashboard, returns true.
     */
    suspend fun isDeviceRevokedOnline(deviceCode: String, keyUsed: String?): Boolean = withContext(Dispatchers.IO) {
        val remoteLicense = getDeviceRemoteLicense(deviceCode)
        if (remoteLicense.isFound && remoteLicense.isRevoked) return@withContext true
        if (keyUsed.isNullOrBlank()) return@withContext false

        val cleanKey = keyUsed.replace("-", "").trim().uppercase()
        FirebaseRest.get("licenses/$cleanKey.json", timeoutMs = 5000).jsonObject()
            ?.optBoolean("isRevoked", false) == true
    }

    data class FeedbackSubmitResult(
        val isSuccess: Boolean,
        val errorMessage: String? = null
    )

    data class SubscriptionRequestResult(
        val isSuccess: Boolean,
        val errorMessage: String? = null
    )

    /**
     * Submits a direct subscription request to Firebase (Realtime Database)
     */
    suspend fun sendSubscriptionRequestOnline(
        subscriberName: String,
        phone: String,
        plan: LicenseManager.SubscriptionPlan,
        deviceCode: String,
        requestType: String = "NEW",
        receiptNumber: String = "",
        region: String = "",
        paymentMethod: String = ""
    ): SubscriptionRequestResult = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val prefix = when (requestType) {
            "UPGRADE" -> "UPG"
            "RENEWAL" -> "REN"
            else -> "REQ"
        }
        val requestId = "${prefix}_${timestamp}_${deviceCode.takeLast(4)}"

        val payload = JSONObject().apply {
            put("id", requestId)
            put("subscriberName", subscriberName)
            put("phone", phone)
            put("plan", plan.name)
            put("requestType", requestType)
            put("deviceCode", deviceCode)
            put("deviceModel", LicenseManager.getDeviceModelName())
            put("androidVersion", LicenseManager.getAndroidVersionName())
            put("status", "PENDING")
            put("receiptNumber", receiptNumber)
            put("region", region)
            put("paymentMethod", paymentMethod)
            put("timestamp", timestamp)
        }
        if (FirebaseRest.put("subscription_requests/$requestId.json", payload.toString()).isSuccess) {
            SubscriptionRequestResult(isSuccess = true)
        } else {
            SubscriptionRequestResult(isSuccess = false, errorMessage = "تعذر إرسال الطلب، تحقق من الاتصال")
        }
    }

    data class PaymentAccount(
        val id: String = "",
        val bankName: String = "",
        val accountNumber: String = "",
        val accountCurrency: String = "YER",
        val accountHolder: String = "",
        val notes: String = "",
        val logoUrl: String = "",
        val isActive: Boolean = true
    )

    private fun parsePaymentAccount(obj: JSONObject, fallbackId: String): PaymentAccount? {
        if (!obj.optBoolean("is_active", true)) return null
        return PaymentAccount(
            id = obj.optString("id", fallbackId),
            bankName = obj.optString("bank_name", obj.optString("bankName", "")),
            accountNumber = obj.optString("account_number", obj.optString("accountNumber", "")),
            accountCurrency = obj.optString("account_currency", obj.optString("accountCurrency", "YER")),
            accountHolder = obj.optString("account_holder", obj.optString("accountHolder", "عمر الحومي")),
            notes = obj.optString("notes", ""),
            logoUrl = obj.optString("logo_url", obj.optString("logoUrl", "")),
            isActive = true
        )
    }

    /** The dashboard stores payment methods either as a JSON array or as a keyed map. */
    private fun parsePaymentMethodsJson(jsonStr: String): List<PaymentAccount> {
        val trimmed = jsonStr.trim()
        return when {
            trimmed.startsWith("[") -> {
                val array = JSONArray(trimmed)
                (0 until array.length()).mapNotNull { i ->
                    array.optJSONObject(i)?.let { parsePaymentAccount(it, "PAY_$i") }
                }
            }
            trimmed.startsWith("{") -> {
                val map = JSONObject(trimmed)
                map.keys().asSequence().mapNotNull { k ->
                    map.optJSONObject(k)?.let { parsePaymentAccount(it, k) }
                }.toList()
            }
            else -> emptyList()
        }
    }

    /**
     * Fetches official payment methods and accounts configured from Admin Dashboard.
     * Caches data locally in SharedPreferences for 100% offline access when network is unavailable.
     * ZERO hardcoded bank/wallet accounts: returns strictly what comes from Firebase / local cache.
     */
    suspend fun getPaymentMethods(context: Context? = null): List<PaymentAccount> = withContext(Dispatchers.IO) {
        val sp = context?.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        val cachedJson = sp?.getString("cached_payment_methods", null)
        val cachedList = if (!cachedJson.isNullOrBlank()) runCatching { parsePaymentMethodsJson(cachedJson) }.getOrNull() else null

        try {
            val jsonStr = fetchJson("payment_methods.json")
            if (jsonStr != null) {
                val list = parsePaymentMethodsJson(jsonStr)
                if (list.isNotEmpty()) {
                    sp?.edit()?.putString("cached_payment_methods", jsonStr)?.apply()
                    return@withContext list
                }
            }
            cachedList ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching payment methods: ${e.message}")
            cachedList ?: emptyList()
        }
    }

    data class PlanPricingInfo(
        val price: String = "",
        val originalPrice: String = "",
        val discountText: String = "",
        val currency: String = "$"
    )

    data class SubscriptionPricing(
        val monthly: PlanPricingInfo = PlanPricingInfo(),
        val yearly: PlanPricingInfo = PlanPricingInfo()
    ) {
        val isConfigured: Boolean
            get() = monthly.price.isNotBlank() || yearly.price.isNotBlank()
    }

    private fun parsePlanPricing(obj: JSONObject?): PlanPricingInfo {
        if (obj == null) return PlanPricingInfo()
        return PlanPricingInfo(
            price = obj.optString("price", ""),
            originalPrice = obj.optString("original_price", obj.optString("originalPrice", "")),
            discountText = obj.optString("discount_text", obj.optString("discountText", "")),
            currency = obj.optString("currency", "$")
        )
    }

    private fun parseSubscriptionPricingJson(jsonStr: String): SubscriptionPricing {
        val root = JSONObject(jsonStr)
        return SubscriptionPricing(
            monthly = parsePlanPricing(root.optJSONObject("monthly")),
            yearly = parsePlanPricing(root.optJSONObject("yearly"))
        )
    }

    /**
     * Fetches current subscription pricing and discounts from Firebase RTDB (/subscription_pricing.json).
     * Automatically caches the pricing locally in SharedPreferences for seamless offline display.
     * ZERO hardcoded pricing: strictly loads from Firebase / local cache.
     */
    suspend fun getSubscriptionPricing(context: Context? = null): SubscriptionPricing = withContext(Dispatchers.IO) {
        val sp = context?.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        val cachedJson = sp?.getString("cached_subscription_pricing", null)
        val cachedPricing = if (!cachedJson.isNullOrBlank()) {
            runCatching { parseSubscriptionPricingJson(cachedJson) }.getOrNull()
        } else null

        try {
            val jsonStr = fetchJson("subscription_pricing.json")
            if (jsonStr != null) {
                val parsed = parseSubscriptionPricingJson(jsonStr)
                if (parsed.isConfigured) {
                    sp?.edit()?.putString("cached_subscription_pricing", jsonStr)?.apply()
                    return@withContext parsed
                }
            }
            cachedPricing ?: SubscriptionPricing()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching subscription pricing: ${e.message}")
            cachedPricing ?: SubscriptionPricing()
        }
    }

    /**
     * Submits user feedback directly to Firebase Realtime Database.
     */
    suspend fun sendFeedbackOnline(
        subscriberName: String,
        phone: String,
        rating: Int,
        category: String,
        message: String,
        deviceCode: String
    ): FeedbackSubmitResult = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val feedbackId = "FB_${timestamp}_${deviceCode.takeLast(4)}"

        val payload = JSONObject().apply {
            put("id", feedbackId)
            put("subscriberName", subscriberName)
            put("phone", phone)
            put("rating", rating)
            put("category", category)
            put("message", message)
            put("deviceCode", deviceCode)
            put("deviceModel", LicenseManager.getDeviceModelName())
            put("androidVersion", LicenseManager.getAndroidVersionName())
            put("timestamp", timestamp)
        }

        val response = FirebaseRest.put("feedbacks/$feedbackId.json", payload.toString())
        when {
            response.isSuccess -> FeedbackSubmitResult(isSuccess = true)
            response.isNetworkError -> FeedbackSubmitResult(
                isSuccess = false,
                errorMessage = "تعذر الاتصال بخادم Firebase (${response.body ?: "انقطاع الاتصال"})."
            )
            else -> FeedbackSubmitResult(
                isSuccess = false,
                errorMessage = when (response.code) {
                    401, 403 -> "قواعد أمان Firebase ترفض الكتابة (Permission Denied). يرجى التأكد من نشر القواعد في تبويب Rules في Firebase Console."
                    404 -> "قاعدة بيانات Firebase غير موجودة بهذا الرابط (${FirebaseRest.DB_URL})."
                    else -> "فشل الإرسال عبر الخادم (كود الخطأ: ${response.code})"
                }
            )
        }
    }

    /** Body of a successful GET, or null when it failed or the node is empty. */
    private fun fetchJson(path: String): String? {
        val response = FirebaseRest.get(path)
        val text = response.body?.trim()
        return if (response.isSuccess && !text.isNullOrEmpty() && text != "null") text else null
    }

    sealed class ExistingUserResult {
        data class Found(
            val subscriberName: String,
            val phone: String,
            val isActivated: Boolean,
            val plan: String
        ) : ExistingUserResult()

        object NotFound : ExistingUserResult()
        data class Error(val message: String) : ExistingUserResult()
    }

    /** Key under /email_index and /blocked_emails for a Google email. */
    fun emailIndexKey(email: String): String =
        email.trim().lowercase().replace(".", "_").replace("@", "_at_")

    /**
     * Looks up this Google email in /email_index (written by each device's presence sync). The
     * whole /all_users list is no longer readable, so only this one entry is fetched.
     * Read-only: nothing is written when the email is not found.
     */
    suspend fun findExistingUserByGoogleEmail(email: String): ExistingUserResult = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank()) return@withContext ExistingUserResult.NotFound

        val response = FirebaseRest.get("email_index/${emailIndexKey(cleanEmail)}.json", timeoutMs = 7000)
        if (!response.isSuccess) {
            return@withContext ExistingUserResult.Error(
                if (response.isNetworkError) response.body ?: "فشل الاتصال بالخادم للتحقق من الحساب"
                else "فشل الاتصال بالخادم للتحقق من الحساب (${response.code})"
            )
        }
        val entry = response.jsonObject() ?: return@withContext ExistingUserResult.NotFound
        // The subscription itself is never restored from this entry: it is client written.
        // A license follows the device and is restored from /devices.
        ExistingUserResult.Found(
            subscriberName = entry.optString("subscriberName", "").ifBlank { "مشترك" },
            phone = entry.optString("phone", ""),
            isActivated = false,
            plan = "TRIAL"
        )
    }
}
