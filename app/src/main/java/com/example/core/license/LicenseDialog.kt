package com.example.core.license

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal

import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.CircularProgressIndicator
import com.example.core.ui.MosaribInlineLoading
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

import com.example.core.ui.isAppDark
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.activity.compose.BackHandler

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.Brush
import com.example.core.util.GoogleDriveBackupHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicenseDialog(
    onDismiss: () -> Unit,
    onActivated: () -> Unit,
    isMandatory: Boolean = false, // If true, cannot dismiss until activated
    mandatoryReason: String? = null
) {
    val context = LocalContext.current
    val isDark = isAppDark()
    val deviceCode = remember { LicenseManager.getDeviceCode(context) }
    val deviceModel = remember { LicenseManager.getDeviceModelName() }
    val androidVersion = remember { LicenseManager.getAndroidVersionName() }
    val isCurrentlyActivated = remember { LicenseManager.isActivated(context) }
    val existingRecord = remember { LicenseManager.getActivationRecord(context) }

    var subscriberName by remember { mutableStateOf(existingRecord?.subscriberName ?: UserDeviceTracker.getRegisteredName(context)) }
    var phone by remember { mutableStateOf(UserDeviceTracker.getRegisteredPhone(context)) }
    var region by remember { mutableStateOf("") }
    var receiptNumber by remember { mutableStateOf("") }
    var selectedPaymentMethod by remember { mutableStateOf("") }
    var isPaymentDropdownExpanded by remember { mutableStateOf(false) }
    var paymentAccounts by remember { mutableStateOf<List<FirebaseLicenseService.PaymentAccount>>(emptyList()) }
    var isPaymentAccountsExpanded by remember { mutableStateOf(false) }
    var pricing by remember { mutableStateOf(FirebaseLicenseService.SubscriptionPricing()) }
    var isSyncingConfig by remember { mutableStateOf(false) }
    var enteredKey by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var requestSuccessMessage by remember { mutableStateOf<String?>(null) }
    var isRequestingOnline by remember { mutableStateOf(false) }
    var isCheckingGoogleLogin by remember { mutableStateOf(false) }
    var showNotRegisteredDialog by remember { mutableStateOf(false) }
    var unregisteredEmailAttempt by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()

    suspend fun refreshConfig() {
        isSyncingConfig = true
        val methods = FirebaseLicenseService.getPaymentMethods(context)
        paymentAccounts = methods
        if (methods.isNotEmpty() && selectedPaymentMethod.isBlank()) {
            selectedPaymentMethod = "${methods.first().bankName} (${methods.first().accountCurrency})"
        }
        pricing = FirebaseLicenseService.getSubscriptionPricing(context)
        isSyncingConfig = false
    }

    LaunchedEffect(Unit) {
        refreshConfig()
    }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { sheetValue ->
            if (isMandatory) sheetValue != androidx.compose.material3.SheetValue.Hidden else true
        }
    )

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                isCheckingGoogleLogin = true
                val email = account.email ?: ""
                coroutineScope.launch {
                    val checkRes = FirebaseLicenseService.findExistingUserByGoogleEmail(email)
                    isCheckingGoogleLogin = false
                    when (checkRes) {
                        is FirebaseLicenseService.ExistingUserResult.Found -> {
                            UserDeviceTracker.saveUserRegistrationInfo(context, checkRes.subscriberName, checkRes.phone)
                            UserDeviceTracker.setGoogleAccountAuthenticatedForApp(context, true)
                            subscriberName = checkRes.subscriberName
                            if (checkRes.phone.isNotBlank()) phone = checkRes.phone

                            if (checkRes.isActivated) {
                                val plan = runCatching { LicenseManager.SubscriptionPlan.valueOf(checkRes.plan) }.getOrDefault(LicenseManager.SubscriptionPlan.YEARLY)
                                val devKey = LicenseManager.generateActivationKey(deviceCode, plan)
                                val actResult = LicenseManager.verifyAndActivate(context, devKey, checkRes.subscriberName)
                                if (actResult is LicenseManager.ActivationResult.Success) {
                                    Toast.makeText(context, "أهلاً بك مجدداً ${checkRes.subscriberName}! تم استعادة ترخيصك وتفعيل جهازك بنجاح.", Toast.LENGTH_LONG).show()
                                    onActivated()
                                    return@launch
                                }
                            }
                            Toast.makeText(context, "أهلاً بك مجدداً ${checkRes.subscriberName}! تم استعادة بيانات حسابك بنجاح.", Toast.LENGTH_LONG).show()
                        }
                        is FirebaseLicenseService.ExistingUserResult.NotFound -> {
                            // CRITICAL ANTI-TAMPERING: Sign out Google, do NOT save email to Firebase
                            val client = GoogleDriveBackupHelper.getGoogleSignInClient(context)
                            client.signOut()
                            UserDeviceTracker.setGoogleAccountAuthenticatedForApp(context, false)
                            unregisteredEmailAttempt = email
                            showNotRegisteredDialog = true
                        }
                        is FirebaseLicenseService.ExistingUserResult.Error -> {
                            errorMessage = checkRes.message
                        }
                    }
                }
            }
        } catch (_: Exception) {
            isCheckingGoogleLogin = false
        }
    }

    if (isMandatory) {
        BackHandler {
            (context as? android.app.Activity)?.finishAffinity()
        }
    }

    var selectedPlan by remember { mutableStateOf(LicenseManager.SubscriptionPlan.YEARLY) }
    var isOnline by remember { mutableStateOf(LicenseManager.isInternetAvailable(context)) }

    val isKeyFormatValid = remember(enteredKey) {
        val clean = enteredKey.trim().uppercase()
        clean.matches(Regex("""^ACTV(-[MY])?-[A-Z0-9]{4}-[A-Z0-9]{4}$"""))
    }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
        unfocusedTextColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
        focusedLabelColor = PrimaryTeal,
        unfocusedLabelColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
        focusedPlaceholderColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
        unfocusedPlaceholderColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
        cursorColor = PrimaryTeal,
        focusedBorderColor = PrimaryTeal,
        unfocusedBorderColor = if (isDark) Color(0xFF334B56) else MaterialTheme.colorScheme.outlineVariant,
        focusedContainerColor = if (isDark) Color(0xFF162329) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        unfocusedContainerColor = if (isDark) Color(0xFF142026) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    )

    fun refreshInternet() {
        isOnline = LicenseManager.isInternetAvailable(context)
    }

    fun copyDeviceCode() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("كود جهاز المسرب", deviceCode)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "تم نسخ كود الجهاز إلى الحافظة", Toast.LENGTH_SHORT).show()
    }

    val isExpired = remember { existingRecord != null && !isCurrentlyActivated }
    val isRegistered = remember(isCurrentlyActivated, existingRecord) {
        isCurrentlyActivated || existingRecord != null
    }
    val currentPlan = existingRecord?.plan
    val isYearlySubscriber = remember(isRegistered, currentPlan) {
        isRegistered && currentPlan == LicenseManager.SubscriptionPlan.YEARLY
    }
    val isUpgrade = remember(selectedPlan, isCurrentlyActivated, existingRecord) {
        isCurrentlyActivated && existingRecord?.plan == LicenseManager.SubscriptionPlan.MONTHLY && selectedPlan == LicenseManager.SubscriptionPlan.YEARLY
    }

    fun submitOnlineSubscriptionRequest(overridePlan: LicenseManager.SubscriptionPlan? = null, overrideIsUpgrade: Boolean? = null) {
        val targetPlan = overridePlan ?: selectedPlan
        val targetUpgrade = overrideIsUpgrade ?: isUpgrade
        val cleanName = subscriberName.trim()
        val cleanPhone = phone.trim()
        if (cleanName.isBlank()) {
            errorMessage = "يرجى كتابة اسم المشترك أولاً لتوثيق طلب الاشتراك"
            return
        }
        val requestType = when {
            targetUpgrade -> "UPGRADE"
            isExpired || isCurrentlyActivated -> "RENEWAL"
            else -> "NEW"
        }
        isRequestingOnline = true
        errorMessage = null
        requestSuccessMessage = null
        coroutineScope.launch {
            UserDeviceTracker.saveUserRegistrationInfo(context, cleanName, cleanPhone)
            val res = FirebaseLicenseService.sendSubscriptionRequestOnline(
                subscriberName = cleanName,
                phone = cleanPhone,
                plan = targetPlan,
                deviceCode = deviceCode,
                requestType = requestType,
                receiptNumber = receiptNumber.trim(),
                region = region.trim(),
                paymentMethod = selectedPaymentMethod.trim()
            )
            isRequestingOnline = false
            if (res.isSuccess) {
                val successText = when (requestType) {
                    "UPGRADE" -> "تم إرسال طلب ترقية اشتراكك إلى الإدارة بنجاح! سيتم مراجعة طلبك وتحديث الترخيص قريباً."
                    "RENEWAL" -> "تم إرسال طلب تجديد اشتراكك إلى الإدارة بنجاح! سيصلك رمز التجديد قريباً."
                    else -> "تم إرسال طلب اشتراكك إلى الإدارة بنجاح! سيصلك رمز التفعيل قريباً."
                }
                requestSuccessMessage = successText
                Toast.makeText(context, successText, Toast.LENGTH_LONG).show()
            } else {
                errorMessage = res.errorMessage ?: "تعذر إرسال الطلب، يمكنك إرساله عبر واتساب مباشرة."
            }
        }
    }

    fun openWhatsApp(overridePlan: LicenseManager.SubscriptionPlan? = null, overrideIsUpgrade: Boolean? = null) {
        val targetPlan = overridePlan ?: selectedPlan
        val targetUpgrade = overrideIsUpgrade ?: isUpgrade
        val cleanName = subscriberName.trim()
        val cleanPhone = phone.trim()
        val requestType = when {
            targetUpgrade -> "UPGRADE"
            isExpired || isCurrentlyActivated -> "RENEWAL"
            else -> "NEW"
        }
        if (cleanName.isNotBlank()) {
            coroutineScope.launch {
                UserDeviceTracker.saveUserRegistrationInfo(context, cleanName, cleanPhone)
                FirebaseLicenseService.sendSubscriptionRequestOnline(
                    subscriberName = cleanName,
                    phone = cleanPhone,
                    plan = targetPlan,
                    deviceCode = deviceCode,
                    requestType = requestType,
                    receiptNumber = receiptNumber.trim(),
                    region = region.trim(),
                    paymentMethod = selectedPaymentMethod.trim()
                )
            }
        }
        val planText = if (targetPlan == LicenseManager.SubscriptionPlan.MONTHLY) {
            val priceStr = if (pricing.monthly.price.isNotBlank()) " ${pricing.monthly.price}${pricing.monthly.currency}" else ""
            val disc = if (pricing.monthly.discountText.isNotBlank()) " - ${pricing.monthly.discountText}" else ""
            "اشتراك شهري$priceStr (30 يوماً$disc)"
        } else {
            val priceStr = if (pricing.yearly.price.isNotBlank()) " ${pricing.yearly.price}${pricing.yearly.currency}" else ""
            val orig = if (pricing.yearly.originalPrice.isNotBlank() && pricing.yearly.price.isNotBlank()) " بدلاً من ${pricing.yearly.originalPrice}${pricing.yearly.currency}" else ""
            val disc = if (pricing.yearly.discountText.isNotBlank()) " - ${pricing.yearly.discountText}" else ""
            "اشتراك سنوي$priceStr$orig (365 يوماً$disc)"
        }
        val url = if (isExpired || isCurrentlyActivated) {
            LicenseManager.getWhatsAppRenewalUrl(
                deviceCode = deviceCode,
                subscriberName = subscriberName,
                requestedPlan = targetPlan,
                isUpgrade = targetUpgrade,
                receiptNumber = receiptNumber.trim(),
                region = region.trim(),
                customPlanText = planText,
                paymentMethod = selectedPaymentMethod.trim()
            )
        } else {
            LicenseManager.getWhatsAppActivationUrl(
                deviceCode = deviceCode,
                subscriberName = subscriberName,
                requestedPlan = targetPlan,
                receiptNumber = receiptNumber.trim(),
                region = region.trim(),
                customPlanText = planText,
                paymentMethod = selectedPaymentMethod.trim()
            )
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun submitActivation() {
        errorMessage = null
        if (!isOnline) {
            val cleanKey = enteredKey.replace("-", "").replace(" ", "").trim().uppercase()
            val isOfflineMathValid = LicenseManager.SubscriptionPlan.entries.any { plan ->
                val exp = LicenseManager.generateActivationKey(deviceCode, plan).replace("-", "").uppercase()
                cleanKey == exp || cleanKey == exp.removePrefix("ACTV")
            }
            if (!isOfflineMathValid) {
                errorMessage = "تعذر التفعيل بدون إنترنت: الرمز غير مطابق لهذا الجهاز، أو يلزم الاتصال بالإنترنت للتحقق منه عبر السيرفر."
                return
            }
        }

        if (subscriberName.trim().isBlank()) {
            errorMessage = "يرجى كتابة اسم المشترك / المسجل أولاً لتوثيق الترخيص باسمك"
            return
        }

        if (enteredKey.isBlank()) {
            errorMessage = "يرجى كتابة رمز التفعيل أولاً"
            return
        }

        val result = LicenseManager.verifyAndActivate(context, enteredKey, subscriberName)
        when (result) {
            is LicenseManager.ActivationResult.NoInternet -> {
                errorMessage = "تعذر الاتصال بالإنترنت! يلزم توفر اتصال نشط بالإنترنت لتوثيق وتفعيل التطبيق."
            }
            is LicenseManager.ActivationResult.MissingSubscriberName -> {
                errorMessage = "يرجى كتابة اسم المشترك / صاحب الحساب أولاً"
            }
            is LicenseManager.ActivationResult.BoundToOtherDevice -> {
                errorMessage = "عذراً! رمز التفعيل هذا مربوط ومستعمل لجهاز آخر مسبقاً، ولا يمكن تشغيله على أكثر من هاتف."
            }
            is LicenseManager.ActivationResult.SubscriptionRevoked -> {
                errorMessage = "تم سحب أو إلغاء تفعيل هذا الاشتراك من قِبل إدارة التطبيق."
            }
            is LicenseManager.ActivationResult.InvalidKey -> {
                errorMessage = "رمز التفعيل غير صالح لهذا الجهاز، يرجى التأكد والتواصل مع المطور"
            }
            is LicenseManager.ActivationResult.Success -> {
                Toast.makeText(
                    context,
                    "تهانينا ${result.record.subscriberName}! تم تفعيل ${result.plan.titleArabic} بنجاح لهاتف (${result.record.deviceModel})",
                    Toast.LENGTH_LONG
                ).show()
                onActivated()
            }
        }
    }

    val sheetScrollState = rememberScrollState()

    if (isRegistered) {
        Dialog(
            onDismissRequest = {
                if (!isMandatory) onDismiss()
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 16.dp)
                        .shadow(24.dp, RoundedCornerShape(24.dp), spotColor = PrimaryTeal.copy(alpha = 0.3f))
                        .clip(RoundedCornerShape(24.dp))
                        .border(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(PrimaryTeal.copy(alpha = 0.6f), AccentGold.copy(alpha = 0.35f))
                            ),
                            RoundedCornerShape(24.dp)
                        ),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    val dialogScrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(dialogScrollState)
                            .padding(horizontal = 20.dp, vertical = 18.dp)
                            .imePadding()
                    ) {
                        // 1. Header (Icon, Title, Close Button)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(AccentGold.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Key,
                                        contentDescription = null,
                                        tint = AccentGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "تجديد أو ترقية الاشتراك",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }

                            if (!isMandatory) {
                                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "إغلاق", modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 2. Minimalist Subscriber & Status Badge (Clean, zero clutter)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDark) Color(0xFF14242A) else PrimaryTeal.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, PrimaryTeal.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "المشترك: ${existingRecord?.subscriberName ?: subscriberName}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )

                                    val remainingDays = LicenseManager.getRemainingDays(context)
                                    val statusText = when {
                                        isExpired -> "منتهي الصلاحية"
                                        remainingDays <= 5 -> "متبقي $remainingDays يوم فقط"
                                        else -> "متبقي $remainingDays يوماً"
                                    }
                                    val statusColor = when {
                                        isExpired || remainingDays <= 5 -> Color(0xFFEF4444)
                                        else -> AccentEmerald
                                    }
                                    Text(
                                        text = statusText,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = statusColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "الباقة: ${existingRecord?.plan?.titleArabic ?: "اشتراك مفعّل"}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { copyDeviceCode() }
                                    ) {
                                        Text(
                                            text = "كود الجهاز: $deviceCode",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = PrimaryTeal,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "نسخ الكود",
                                            tint = PrimaryTeal,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 3. خيارات الدفع (Combobox / Dropdown)
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedPaymentMethod.ifBlank { "اختر طريقة التحويل / المحفظة" },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("خيارات وطرق الدفع") },
                                leadingIcon = {
                                    Icon(Icons.Default.AccountBalance, contentDescription = null, tint = AccentGold, modifier = Modifier.size(18.dp))
                                },
                                trailingIcon = {
                                    Icon(
                                        imageVector = if (isPaymentDropdownExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = PrimaryTeal
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = textFieldColors,
                                singleLine = true
                            )

                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { isPaymentDropdownExpanded = !isPaymentDropdownExpanded }
                            )

                            DropdownMenu(
                                expanded = isPaymentDropdownExpanded,
                                onDismissRequest = { isPaymentDropdownExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                if (paymentAccounts.isNotEmpty()) {
                                    paymentAccounts.forEach { acc ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(acc.bankName, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                                    Text("حساب: ${acc.accountNumber} (${acc.accountCurrency})", fontSize = 11.sp, color = PrimaryTeal)
                                                    if (acc.accountHolder.isNotBlank()) {
                                                        Text("باسم: ${acc.accountHolder}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
                                            },
                                            onClick = {
                                                selectedPaymentMethod = "${acc.bankName} (${acc.accountNumber})"
                                                isPaymentDropdownExpanded = false
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = AccentGold, modifier = Modifier.size(16.dp))
                                            }
                                        )
                                    }
                                }
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text("بنك الكريمي للتمويل الأصغر", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                            Text("حساب أو إيداع مباشر / محفظة كريمي جواد", fontSize = 11.sp, color = PrimaryTeal)
                                        }
                                    },
                                    onClick = {
                                        selectedPaymentMethod = "بنك الكريمي"
                                        isPaymentDropdownExpanded = false
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = AccentGold, modifier = Modifier.size(16.dp))
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text("بنك التضامن الإسلامي / محفظة محفظتي", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                            Text("تحويل لحساب البنك أو المحفظة", fontSize = 11.sp, color = PrimaryTeal)
                                        }
                                    },
                                    onClick = {
                                        selectedPaymentMethod = "بنك التضامن"
                                        isPaymentDropdownExpanded = false
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = AccentGold, modifier = Modifier.size(16.dp))
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text("حوالة عبر صراف آخر / شبكة صرافة", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                                    },
                                    onClick = {
                                        selectedPaymentMethod = "حوالة صرافة / شبكة أخرى"
                                        isPaymentDropdownExpanded = false
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(16.dp))
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 4. صندوق رقم التحويل (Transfer/Receipt Number Box)
                        OutlinedTextField(
                            value = receiptNumber,
                            onValueChange = { receiptNumber = it },
                            label = { Text("رقم سند التحويل / الحوالة (إن وجد)") },
                            placeholder = { Text("مثال: 94827103") },
                            leadingIcon = {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(18.dp))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // 5. زر طلب تجديد وزر ترقية وزر واتساب
                        val currentRecordPlan = existingRecord?.plan ?: LicenseManager.SubscriptionPlan.MONTHLY
                        val isYearly = currentRecordPlan == LicenseManager.SubscriptionPlan.YEARLY
                        val renewalPriceStr = if (isYearly) {
                            if (pricing.yearly.price.isNotBlank()) " (${pricing.yearly.price}${pricing.yearly.currency})" else ""
                        } else {
                            if (pricing.monthly.price.isNotBlank()) " (${pricing.monthly.price}${pricing.monthly.currency})" else ""
                        }
                        val renewalButtonTitle = if (isYearly) "طلب تجديد الاشتراك السنوي$renewalPriceStr" else "طلب تجديد الاشتراك$renewalPriceStr"

                        Button(
                            onClick = {
                                if (!isOnline) {
                                    Toast.makeText(context, "يلزم توفر إنترنت لإرسال الطلب أونلاين، أو يمكنك الطلب عبر واتساب مباشرة.", Toast.LENGTH_LONG).show()
                                } else {
                                    submitOnlineSubscriptionRequest(currentRecordPlan, overrideIsUpgrade = false)
                                }
                            },
                            enabled = !isRequestingOnline,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(11.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryTeal,
                                contentColor = Color.White
                            )
                        ) {
                            if (isRequestingOnline) {
                                MosaribInlineLoading(size = 18.dp, primaryColor = Color.White, accentColor = Color.White.copy(alpha = 0.5f))
                            } else {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(renewalButtonTitle, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (!isYearly) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val upgradePriceStr = if (pricing.yearly.price.isNotBlank()) " (${pricing.yearly.price}${pricing.yearly.currency})" else ""
                            Button(
                                onClick = {
                                    if (!isOnline) {
                                        Toast.makeText(context, "يلزم توفر إنترنت لإرسال الطلب أونلاين، أو يمكنك الطلب عبر واتساب مباشرة.", Toast.LENGTH_LONG).show()
                                    } else {
                                        submitOnlineSubscriptionRequest(LicenseManager.SubscriptionPlan.YEARLY, overrideIsUpgrade = true)
                                    }
                                },
                                enabled = !isRequestingOnline,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(11.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AccentGold,
                                    contentColor = Color.Black
                                )
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("طلب ترقية إلى سنوي$upgradePriceStr", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                openWhatsApp(
                                    overridePlan = if (isYearly) LicenseManager.SubscriptionPlan.YEARLY else LicenseManager.SubscriptionPlan.MONTHLY,
                                    overrideIsUpgrade = false
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(11.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF25D366),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("طلب عبر واتساب", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }

                        if (requestSuccessMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = requestSuccessMessage!!,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF16A34A),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f).height(1.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            ) {}
                            Text(
                                text = "تفعيل رمز التجديد المستلم",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Surface(
                                modifier = Modifier.weight(1f).height(1.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            ) {}
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 6. حقل إدخال رمز التجديد "تفعيل"
                        OutlinedTextField(
                            value = enteredKey,
                            onValueChange = { enteredKey = it.uppercase() },
                            label = { Text("أدخل رمز التجديد المستلم (ACTV-XXXX-XXXX)") },
                            leadingIcon = {
                                Icon(Icons.Default.Key, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(18.dp))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors,
                            singleLine = true
                        )

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = errorMessage!!,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFFE53935),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { submitActivation() },
                            enabled = isKeyFormatValid,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(11.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryTeal,
                                contentColor = Color.White,
                                disabledContainerColor = PrimaryTeal.copy(alpha = 0.35f),
                                disabledContentColor = Color.White.copy(alpha = 0.5f)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "تأكيد الرمز والتفعيل",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )
                        }
                    }
                }
            }
        }
    } else {
        ModalBottomSheet(
        onDismissRequest = {
            if (!isMandatory) onDismiss()
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(sheetScrollState)
                .padding(horizontal = 22.dp, vertical = 12.dp)
                .imePadding()
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(AccentGold.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = AccentGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "الاشتراكات والتفعيل",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                if (!isMandatory) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                } else {
                    TextButton(
                        onClick = { (context as? android.app.Activity)?.finishAffinity() },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "خروج", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "خروج",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            if (isMandatory) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0xFF3B1E1E) else Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = mandatoryReason ?: "لقد تجاوزت الحد التجريبي المجاني (${LicenseManager.FREE_OPERATIONS_LIMIT} عملية) أو قمت باستعادة نسخة تتجاوزه. تم حفظ كافة بياناتك بأمان تام، ويرجى تفعيل الاشتراك لمتابعة العمل بالتطبيق.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 1. Internet Connection Status Indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isOnline) {
                            if (isDark) Color(0xFF143022) else Color(0xFFE8F5E9)
                        } else {
                            if (isDark) Color(0xFF381A1A) else Color(0xFFFFEBEE)
                        }
                    )
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = if (isOnline) (if (isDark) Color(0xFF86EFAC) else Color(0xFF2E7D32)) else (if (isDark) Color(0xFFFCA5A5) else Color(0xFFD32F2F)),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isOnline) "متصل بالإنترنت" else "لا يوجد اتصال بالإنترنت",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isOnline) (if (isDark) Color(0xFF86EFAC) else Color(0xFF2E7D32)) else (if (isDark) Color(0xFFFCA5A5) else Color(0xFFD32F2F)),
                                fontSize = 11.5.sp
                            )
                        )
                    }

                    if (!isOnline) {
                        androidx.compose.material3.TextButton(
                            onClick = { refreshInternet() },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "إعادة فحص",
                                fontSize = 10.5.sp,
                                color = if (isDark) Color(0xFFFCA5A5) else Color(0xFFD32F2F),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Active Registration Summary (if already activated)
            if (isCurrentlyActivated && existingRecord != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimaryTeal.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "بيانات الترخيص النشط:",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = PrimaryTeal)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "المشترك: ${existingRecord.subscriberName}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "الجهاز: ${existingRecord.deviceModel} (${existingRecord.androidVersion})",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Text(
                            text = "الباقة: ${existingRecord.plan.titleArabic} • المتبقي: ${LicenseManager.getRemainingDays(context)} يوماً",
                            style = MaterialTheme.typography.labelSmall.copy(color = AccentEmerald, fontWeight = FontWeight.Bold)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 3. Explanation Text
            val dialogLimit = LicenseManager.getFreeOperationsLimit(context)
            Text(
                text = when {
                    isMandatory -> "لقد استنفدت العمليات المجانية بالكامل ($dialogLimit عملية) في النسخة التجريبية. يرجى إدخال اسمك وطلب كود التفعيل للاستمرار."
                    isRegistered -> "بيانات اشتراكك مسجلة في النظام. يمكنك إرسال طلب تجديد اشتراكك أو ترقية الباقة مباشرة."
                    else -> "لتفعيل التطبيق، اكتب اسمك، واختر الباقة، ثم انسخ الكود وأرسله عبر الواتساب للمطور لاستلام رمز التفعيل وتأكيده مع توفر الإنترنت."
                },
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp,
                    fontSize = 11.5.sp
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Subscriber Name Input
            OutlinedTextField(
                value = subscriberName,
                onValueChange = { subscriberName = it },
                label = { Text("اسم المشترك / الموزع (مطلوب للتوثيق)") },
                placeholder = { Text("مثال: عبدالملك الحومي") },
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(18.dp))
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors,
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Phone Input
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("رقم الهاتف (للتواصل واستلام الرمز)") },
                placeholder = { Text("مثال: 773712030") },
                leadingIcon = {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(18.dp))
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Region Input
            OutlinedTextField(
                value = region,
                onValueChange = { region = it },
                label = { Text("المنطقة / المحافظة") },
                placeholder = { Text("مثال: صنعاء / ذمار / إب / الحديدة") },
                leadingIcon = {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(18.dp))
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors,
                singleLine = true
            )

            // Payment Method Selector (Combobox / Dropdown)
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedPaymentMethod.ifBlank { if (isSyncingConfig) "جاري جلب طرق الدفع..." else "اختر طريقة الدفع / المحفظة" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("طريقة التحويل / المحفظة البنكية *") },
                    leadingIcon = {
                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = AccentGold, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = if (isPaymentDropdownExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = PrimaryTeal
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors,
                    singleLine = true
                )

                // Transparent clickable overlay so tapping anywhere opens the dropdown menu
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { isPaymentDropdownExpanded = !isPaymentDropdownExpanded }
                )

                DropdownMenu(
                    expanded = isPaymentDropdownExpanded,
                    onDismissRequest = { isPaymentDropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    if (paymentAccounts.isNotEmpty()) {
                        paymentAccounts.forEach { acc ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(acc.bankName, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                        Text("${acc.accountNumber} (${acc.accountCurrency})", fontSize = 11.sp, color = PrimaryTeal)
                                    }
                                },
                                onClick = {
                                    selectedPaymentMethod = "${acc.bankName} (${acc.accountCurrency})"
                                    isPaymentDropdownExpanded = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.AccountBalance, contentDescription = null, tint = AccentGold, modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                    } else {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (isOnline) "جاري جلب طرق الدفع من الخادم..." else "يلزم توفر إنترنت لجلب الحسابات (إضغط للتحديث)",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                coroutineScope.launch { refreshConfig() }
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(16.dp))
                            }
                        )
                    }

                    DropdownMenuItem(
                        text = {
                            Text("حوالة عبر صراف آخر / شبكة صرافة", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                        },
                        onClick = {
                            selectedPaymentMethod = "حوالة صرافة / شبكة أخرى"
                            isPaymentDropdownExpanded = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(16.dp))
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Receipt / Transfer Number Input
            OutlinedTextField(
                value = receiptNumber,
                onValueChange = { receiptNumber = it },
                label = { Text("رقم سند التحويل أو الإيداع (إن وجد)") },
                placeholder = { Text("مثال: 94827103") },
                leadingIcon = {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(18.dp))
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors,
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 5. Detected Device Info Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDark) Color(0xFF162329) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Smartphone, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "جهازك المسجل: $deviceModel • $androidVersion",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isDark) Color(0xFF94A3B8) else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.5.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // If not registered: Show Plan Toggle Chips & Google Restore
            if (!isRegistered) {
                // Plan Toggle Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val monthlyPriceText = if (pricing.monthly.price.isNotBlank()) {
                        "${pricing.monthly.price}${pricing.monthly.currency} شهرياً"
                    } else if (isSyncingConfig) {
                        "جاري التحميل..."
                    } else {
                        "باقة 30 يوماً"
                    }

                    Button(
                        onClick = { selectedPlan = LicenseManager.SubscriptionPlan.MONTHLY },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedPlan == LicenseManager.SubscriptionPlan.MONTHLY) PrimaryTeal else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedPlan == LicenseManager.SubscriptionPlan.MONTHLY) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("اشتراك شهري", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(monthlyPriceText, fontSize = 10.sp)
                            if (pricing.monthly.discountText.isNotBlank()) {
                                Text(pricing.monthly.discountText, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    val yearlyPriceText = if (pricing.yearly.price.isNotBlank()) {
                        if (pricing.yearly.originalPrice.isNotBlank()) {
                            "${pricing.yearly.price}${pricing.yearly.currency} (بدلاً من ${pricing.yearly.originalPrice}${pricing.yearly.currency})"
                        } else {
                            "${pricing.yearly.price}${pricing.yearly.currency} سنوياً"
                        }
                    } else if (isSyncingConfig) {
                        "جاري التحميل..."
                    } else {
                        "باقة 365 يوماً"
                    }

                    Button(
                        onClick = { selectedPlan = LicenseManager.SubscriptionPlan.YEARLY },
                        modifier = Modifier.weight(1.2f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedPlan == LicenseManager.SubscriptionPlan.YEARLY) AccentGold else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedPlan == LicenseManager.SubscriptionPlan.YEARLY) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("اشتراك سنوي ⭐", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(yearlyPriceText, fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold)
                            if (pricing.yearly.discountText.isNotBlank()) {
                                Text(pricing.yearly.discountText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (!pricing.isConfigured) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color(0xFF1C242B) else Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isOnline) "جاري تحديث الأسعار من السيرفر..." else "الأسعار تتطلب اتصالاً بالإنترنت لأول مرة",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp)
                            )
                            IconButton(
                                onClick = { coroutineScope.launch { refreshConfig() } },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "تحديث الأسعار", tint = PrimaryTeal, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Perks / Features Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF132228) else Color(0xFFF0FDF4)
                    ),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF1E3A3A) else Color(0xFFBBF7D0))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "مميزات الاشتراك في تطبيق المُسَرِّب:",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) AccentEmerald else Color(0xFF15803D)
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SupportAgent, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("دعم فني مخصص عند الحاجة", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Sync, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تحديثات متواصلة وحل المشاكل بشكل فوري", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Smartphone, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إمكانية تغيير الهاتف ونقل الاشتراك بأي وقت", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Google Account Restore Button
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "مسجل مسبقاً ولديك اشتراك؟",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val client = GoogleDriveBackupHelper.getGoogleSignInClient(context)
                                googleSignInLauncher.launch(client.signInIntent)
                            },
                            enabled = !isCheckingGoogleLogin,
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E), contentColor = Color.White)
                        ) {
                            if (isCheckingGoogleLogin) {
                                MosaribInlineLoading(size = 18.dp, primaryColor = Color.White, accentColor = Color.White.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("جاري فحص الحساب في السحابة...", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تسجيل الدخول واستعادة الحساب عبر Google", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // 💳 Payment Accounts Card (Collapsible)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isPaymentAccountsExpanded = !isPaymentAccountsExpanded },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF1B2A32) else Color(0xFFF8FAFC)
                    ),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF2C3E49) else Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = AccentGold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "حسابات التحويل ومحافظ الدفع المعتمدة",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                            Icon(
                                imageVector = if (isPaymentAccountsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isPaymentAccountsExpanded) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "يمكنك التحويل إلى أي من الحسابات المعتمدة التالية، ثم تدوين رقم السند:",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            if (paymentAccounts.isNotEmpty()) {
                                paymentAccounts.forEach { acc ->
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isDark) Color(0xFF142026) else Color(0xFFFFFFFF),
                                        border = BorderStroke(1.dp, if (isDark) Color(0xFF263843) else Color(0xFFE2E8F0))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = acc.bankName,
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                                )
                                                Text(
                                                    text = "رقم الحساب: ${acc.accountNumber} (${acc.accountCurrency})",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = PrimaryTeal,
                                                        fontWeight = FontWeight.ExtraBold
                                                    )
                                                )
                                                if (acc.accountHolder.isNotBlank()) {
                                                    Text(
                                                        text = "باسم: ${acc.accountHolder}",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            fontSize = 10.sp
                                                        )
                                                    )
                                                }
                                                if (acc.notes.isNotBlank()) {
                                                    Text(
                                                        text = acc.notes,
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            fontSize = 9.5.sp
                                                        )
                                                    )
                                                }
                                            }

                                            IconButton(
                                                onClick = {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    val clip = ClipData.newPlainText(acc.bankName, acc.accountNumber)
                                                    clipboard.setPrimaryClip(clip)
                                                    Toast.makeText(context, "تم نسخ رقم حساب ${acc.bankName}", Toast.LENGTH_SHORT).show()
                                                }
                                            ) {
                                                Icon(
                                                    Icons.Default.ContentCopy,
                                                    contentDescription = "نسخ رقم الحساب",
                                                    tint = PrimaryTeal,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isDark) Color(0xFF1E2830) else Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = if (isOnline) "جاري مزامنة بيانات حسابات الدفع من السيرفر..." else "يلزم توفر إنترنت لأول مرة لجلب بيانات حسابات الدفع المعتمدة",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        OutlinedButton(
                                            onClick = { coroutineScope.launch { refreshConfig() } },
                                            modifier = Modifier.height(32.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("مزامنة الآن 🔄", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // Device Code Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "كود جهازك الفريد (Device Token):",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = deviceCode,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = 2.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (!isRegistered) {
                        // User not registered: Standard copy + WhatsApp activation + Online subscription request
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { copyDeviceCode() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("نسخ الكود", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { openWhatsApp() },
                                modifier = Modifier.weight(1.4f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("طلب تفعيل عبر واتساب", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { submitOnlineSubscriptionRequest() },
                            enabled = !isRequestingOnline,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryTeal,
                                contentColor = Color.White
                            )
                        ) {
                            if (isRequestingOnline) {
                                MosaribInlineLoading(size = 18.dp, primaryColor = Color.White, accentColor = Color.White.copy(alpha = 0.5f))
                            } else {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إرسال طلب اشتراك", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else if (isYearlySubscriber) {
                        // Yearly subscriber: ONLY Renewal option
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { copyDeviceCode() },
                                modifier = Modifier.weight(0.9f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("نسخ الكود", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { openWhatsApp(LicenseManager.SubscriptionPlan.YEARLY, overrideIsUpgrade = false) },
                                modifier = Modifier.weight(1.1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تجديد عبر واتساب", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { submitOnlineSubscriptionRequest(LicenseManager.SubscriptionPlan.YEARLY, overrideIsUpgrade = false) },
                            enabled = !isRequestingOnline,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryTeal,
                                contentColor = Color.White
                            )
                        ) {
                            if (isRequestingOnline) {
                                MosaribInlineLoading(size = 18.dp, primaryColor = Color.White, accentColor = Color.White.copy(alpha = 0.5f))
                            } else {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("طلب تجديد الاشتراك", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // Monthly / non-yearly subscriber: Show TWO options: Renew OR Upgrade!
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { copyDeviceCode() },
                                modifier = Modifier.weight(0.9f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("نسخ الكود", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { openWhatsApp(LicenseManager.SubscriptionPlan.MONTHLY, overrideIsUpgrade = false) },
                                modifier = Modifier.weight(1.1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تجديد عبر واتساب", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Button 1: طلب تجديد الاشتراك (الشهري)
                        Button(
                            onClick = { submitOnlineSubscriptionRequest(LicenseManager.SubscriptionPlan.MONTHLY, overrideIsUpgrade = false) },
                            enabled = !isRequestingOnline,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryTeal,
                                contentColor = Color.White
                            )
                        ) {
                            if (isRequestingOnline) {
                                MosaribInlineLoading(size = 18.dp, primaryColor = Color.White, accentColor = Color.White.copy(alpha = 0.5f))
                            } else {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("طلب تجديد الاشتراك", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Button 2: طلب ترقية الاشتراك إلى سنوي
                        Button(
                            onClick = { submitOnlineSubscriptionRequest(LicenseManager.SubscriptionPlan.YEARLY, overrideIsUpgrade = true) },
                            enabled = !isRequestingOnline,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentGold,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            val yearlyUpgradeSuffix = if (pricing.yearly.price.isNotBlank()) " (${pricing.yearly.price}${pricing.yearly.currency})" else ""
                            Text("طلب ترقية الاشتراك إلى سنوي$yearlyUpgradeSuffix", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (requestSuccessMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = requestSuccessMessage!!,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF16A34A),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Activation Key Input
            OutlinedTextField(
                value = enteredKey,
                onValueChange = { enteredKey = it.uppercase() },
                label = { Text(if (isRegistered) "أدخل رمز التجديد / الترقية المستلم (اختياري)" else "أدخل رمز التفعيل (ACTV-XXXX-XXXX)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors,
                singleLine = true
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFE53935),
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Submit Button (only enabled if entered key matches format)
            Button(
                onClick = { submitActivation() },
                enabled = isKeyFormatValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryTeal,
                    contentColor = Color.White,
                    disabledContainerColor = PrimaryTeal.copy(alpha = 0.35f),
                    disabledContentColor = Color.White.copy(alpha = 0.5f)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRegistered) "تأكيد رمز التجديد / الترقية" else "تفعيل التطبيق الآن",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    }

    if (showNotRegisteredDialog) {
        AlertDialog(
            onDismissRequest = { showNotRegisteredDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = AccentGold,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "لا يوجد حساب مسجل مسبقاً",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "لا يوجد حساب أو اشتراك مرتبط بهذا البريد الإلكتروني مسبقاً:\n($unregisteredEmailAttempt)\n\nيمكنك تسجيل حساب جديد ببياناتك الآن لطلب التفعيل، أو المتابعة بالنسخة المجانية.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = { showNotRegisteredDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("تسجيل وتفعيل حساب جديد", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                if (!isMandatory) {
                    TextButton(
                        onClick = {
                            showNotRegisteredDialog = false
                            onDismiss()
                        }
                    ) {
                        Text("المتابعة بالنسخة المجانية", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        )
    }
}
