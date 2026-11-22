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
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import android.os.SystemClock
import com.example.core.ui.DialogTone
import com.example.core.ui.MosaribDialog
import com.example.core.ui.MosaribMenu
import com.example.core.ui.MosaribPrimaryButton
import com.example.core.ui.MosaribSecondaryButton
import androidx.compose.foundation.layout.heightIn
import com.example.ui.theme.mosarib
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentGold


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
    var isPaymentAccountsExpanded by remember { mutableStateOf(true) }
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

                            // A subscription is restored only from this device's /devices record, which
                            // only the admin dashboard can write (e.g. after moving a license here).
                            LicenseManager.syncRemoteSubscriptionStatus(context)
                            if (LicenseManager.isActivated(context)) {
                                Toast.makeText(context, "أهلاً بك مجدداً ${checkRes.subscriberName}! تم استعادة ترخيصك وتفعيل جهازك بنجاح.", Toast.LENGTH_LONG).show()
                                onActivated()
                                return@launch
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
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.mosarib.textMuted,
        focusedPlaceholderColor = MaterialTheme.mosarib.textMuted,
        unfocusedPlaceholderColor = MaterialTheme.mosarib.textMuted,
        cursorColor = MaterialTheme.colorScheme.primary,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
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
            errorMessage = "يلزم الاتصال بالإنترنت للتحقق من رمز التفعيل عبر السيرفر."
            return
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
                        .shadow(24.dp, RoundedCornerShape(24.dp), spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        .clip(RoundedCornerShape(24.dp))
                        .border(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), AccentGold.copy(alpha = 0.35f))
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
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
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
                                        isExpired || remainingDays <= 5 -> MaterialTheme.mosarib.debt
                                        else -> MaterialTheme.mosarib.success
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
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "نسخ الكود",
                                            tint = MaterialTheme.colorScheme.primary,
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
                                        tint = MaterialTheme.colorScheme.primary
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

                            MosaribMenu(
                                expanded = isPaymentDropdownExpanded,
                                onDismissRequest = { isPaymentDropdownExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                if (paymentAccounts.isNotEmpty()) {
                                    paymentAccounts.forEach { acc ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(acc.bankName, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                                    Text("حساب: ${acc.accountNumber} (${acc.accountCurrency})", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary))
                                                    if (acc.accountHolder.isNotBlank()) {
                                                        Text("باسم: ${acc.accountHolder}", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
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
                                            Text("بنك الكريمي للتمويل الأصغر", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                            Text("حساب أو إيداع مباشر / محفظة كريمي جواد", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary))
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
                                            Text("بنك التضامن الإسلامي / محفظة محفظتي", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                            Text("تحويل لحساب البنك أو المحفظة", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary))
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
                                        Text("حوالة عبر صراف آخر / شبكة صرافة", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                                    },
                                    onClick = {
                                        selectedPaymentMethod = "حوالة صرافة / شبكة أخرى"
                                        isPaymentDropdownExpanded = false
                                    },
                                    leadingIcon = {
                                        Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
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
                                Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
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
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            )
                        ) {
                            if (isRequestingOnline) {
                                MosaribInlineLoading(size = 18.dp, primaryColor = Color.White, accentColor = Color.White.copy(alpha = 0.5f))
                            } else {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(renewalButtonTitle, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
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
                                Text("طلب ترقية إلى سنوي$upgradePriceStr", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
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
                                containerColor = MaterialTheme.mosarib.success,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("طلب عبر واتساب", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }

                        if (requestSuccessMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = requestSuccessMessage!!,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.mosarib.success,
                                    fontWeight = FontWeight.Bold
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
                            label = { Text("أدخل رمز التجديد المستلم (ACTV-X-XXXX-XXXX-XXXX)") },
                            leadingIcon = {
                                Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
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
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.mosarib.debt,
                                    fontWeight = FontWeight.Bold
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
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White,
                                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
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
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    } else {
        var step by remember { mutableStateOf(0) }
        val maxDialogHeight = (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp * 0.8f).dp
        val stepTitles = listOf("الباقة", "الدفع", "بياناتك", "رمز التفعيل")
        val dialogLimit = LicenseManager.getFreeOperationsLimit(context)
        val trialRemaining = (dialogLimit - LicenseManager.getCumulativeOperations(context)).coerceAtLeast(0)
        // Back is handled here (never closes the app): step -> previous step -> card -> dismiss.
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(dismissOnBackPress = false, usePlatformDefaultWidth = false)
        ) {
            BackHandler { if (step > 0) step-- else onDismiss() }
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl
            ) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.9f).heightIn(max = maxDialogHeight).imePadding(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 12.dp
            ) {
            if (step == 0) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(76.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isMandatory) Icons.Default.Lock else Icons.Default.Key,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = if (isMandatory) "انتهت الفترة التجريبية" else "اشترك في المسرّب",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isMandatory) "بياناتك محفوظة ويمكنك تصفح حساباتك. لإضافة عمليات جديدة اشترك."
                        else "بقي لك $trialRemaining عملية مجانية",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.mosarib.textMuted),
                        textAlign = TextAlign.Center
                    )
                    if (isMandatory && !mandatoryReason.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = mandatoryReason,
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.mosarib.textMuted),
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    MosaribPrimaryButton(text = "طلب اشتراك", onClick = { step = 1 }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(10.dp))
                    MosaribSecondaryButton(text = "إغلاق", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(onClick = { step = 4 }) {
                        Text("لديّ رمز تفعيل", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                    if (isMandatory) {
                        TextButton(
                            onClick = { (context as? android.app.Activity)?.finishAffinity() },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.mosarib.debt)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("خروج من التطبيق", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                StepperProgress(
                    current = step,
                    titles = stepTitles,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 14.dp)
                )
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(sheetScrollState)
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                ) {
            // 1. Internet Connection Status Indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isOnline) MaterialTheme.mosarib.successContainer else MaterialTheme.colorScheme.errorContainer
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
                            tint = if (isOnline) MaterialTheme.mosarib.success else MaterialTheme.mosarib.debt,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isOnline) "متصل بالإنترنت" else "لا يوجد اتصال بالإنترنت",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isOnline) MaterialTheme.mosarib.success else MaterialTheme.mosarib.debt
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
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.mosarib.debt,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            if (step == 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val monthlyPriceText = if (pricing.monthly.price.isNotBlank()) {
                        "${pricing.monthly.price}${pricing.monthly.currency} شهرياً"
                    } else if (isSyncingConfig) {
                        "—"
                    } else {
                        "—"
                    }

                    Button(
                        onClick = { selectedPlan = LicenseManager.SubscriptionPlan.MONTHLY },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedPlan == LicenseManager.SubscriptionPlan.MONTHLY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedPlan == LicenseManager.SubscriptionPlan.MONTHLY) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("اشتراك شهري", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            Text(monthlyPriceText, style = MaterialTheme.typography.labelSmall)
                            if (pricing.monthly.discountText.isNotBlank()) {
                                Text(pricing.monthly.discountText, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold))
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
                        "—"
                    } else {
                        "—"
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
                            Text("اشتراك سنوي", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            Text(yearlyPriceText, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold))
                            if (pricing.yearly.discountText.isNotBlank()) {
                                Text(pricing.yearly.discountText, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
                if (!pricing.isConfigured) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isOnline) "تعذر عرض الأسعار حالياً" else "الأسعار تتطلب اتصالاً بالإنترنت لأول مرة",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = { coroutineScope.launch { refreshConfig() } },
                            enabled = !isSyncingConfig,
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إعادة المحاولة", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            if (step == 2) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isPaymentAccountsExpanded = !isPaymentAccountsExpanded },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                                style = MaterialTheme.typography.labelSmall.copy(
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
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.ExtraBold
                                                    )
                                                )
                                                if (acc.accountHolder.isNotBlank()) {
                                                    Text(
                                                        text = "باسم: ${acc.accountHolder}",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    )
                                                }
                                                if (acc.notes.isNotBlank()) {
                                                    Text(
                                                        text = acc.notes,
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                                    tint = MaterialTheme.colorScheme.primary,
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
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = if (isOnline) "جاري مزامنة بيانات حسابات الدفع من السيرفر..." else "يلزم توفر إنترنت لأول مرة لجلب بيانات حسابات الدفع المعتمدة",
                                            style = MaterialTheme.typography.labelSmall.copy(
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
                                            Text("مزامنة الآن", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = receiptNumber,
                onValueChange = { receiptNumber = it },
                label = { Text("رقم سند التحويل أو الإيداع (إن وجد)") },
                placeholder = { Text("مثال: 94827103") },
                leadingIcon = {
                    Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors,
                singleLine = true
            )
            }
            Spacer(modifier = Modifier.height(10.dp))
            if (step == 3) {
            OutlinedTextField(
                value = subscriberName,
                onValueChange = { subscriberName = it },
                label = { Text("اسم المشترك / الموزع (مطلوب للتوثيق)") },
                placeholder = { Text("مثال: عبدالملك الحومي") },
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors,
                singleLine = true
            )
                Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("رقم الهاتف (للتواصل واستلام الرمز)") },
                placeholder = { Text("مثال: 773712030") },
                leadingIcon = {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true
            )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { openWhatsApp() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.mosarib.success)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إرسال الطلب عبر واتساب", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                }
                Spacer(modifier = Modifier.height(6.dp))
                TextButton(
                    onClick = { submitOnlineSubscriptionRequest() },
                    enabled = !isRequestingOnline,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isRequestingOnline) {
                        MosaribInlineLoading(size = 18.dp, primaryColor = MaterialTheme.colorScheme.primary, accentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    } else {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("أو أرسل الطلب إلى الإدارة من داخل التطبيق", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
                if (requestSuccessMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = requestSuccessMessage!!,
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.mosarib.success, fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            Spacer(modifier = Modifier.height(12.dp))
            var showExtraDetails by remember { mutableStateOf(false) }
            CollapsibleHeader("تفاصيل إضافية", showExtraDetails) { showExtraDetails = !showExtraDetails }
            if (showExtraDetails) {
                Spacer(modifier = Modifier.height(8.dp))
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
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                        ) {
                            if (isCheckingGoogleLogin) {
                                MosaribInlineLoading(size = 18.dp, primaryColor = Color.White, accentColor = Color.White.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("جاري فحص الحساب في السحابة...", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            } else {
                                Icon(Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تسجيل الدخول واستعادة الحساب عبر Google", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = region,
                onValueChange = { region = it },
                label = { Text("المنطقة / المحافظة") },
                placeholder = { Text("مثال: صنعاء / ذمار / إب / الحديدة") },
                leadingIcon = {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors,
                singleLine = true
            )
                Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Smartphone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "جهازك المسجل: $deviceModel • $androidVersion",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.mosarib.textMuted
                    )
                )
            }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { copyDeviceCode() }
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "كود الجهاز: $deviceCode",
                        style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    )
                    Icon(Icons.Default.ContentCopy, contentDescription = "نسخ الكود", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            var showFeatures by remember { mutableStateOf(false) }
            CollapsibleHeader("مميزات الاشتراك", showFeatures) { showFeatures = !showFeatures }
            if (showFeatures) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.mosarib.successContainer
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.mosarib.success.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "مميزات الاشتراك في تطبيق المُسَرِّب:",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.mosarib.onSuccessContainer
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SupportAgent, contentDescription = null, tint = MaterialTheme.mosarib.success, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("دعم فني مخصص عند الحاجة", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Sync, contentDescription = null, tint = MaterialTheme.mosarib.success, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تحديثات متواصلة وحل المشاكل بشكل فوري", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Smartphone, contentDescription = null, tint = MaterialTheme.mosarib.success, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إمكانية تغيير الهاتف ونقل الاشتراك بأي وقت", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                        }
                    }
                }
            }
            }
            if (step == 4) {
                Text("ستصلك الرسالة بالرمز بعد تأكيد الدفع", style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.mosarib.textMuted))
                Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = enteredKey,
                onValueChange = { enteredKey = it.uppercase() },
                label = { Text(if (isRegistered) "أدخل رمز التجديد / الترقية المستلم (اختياري)" else "أدخل رمز التفعيل (ACTV-X-XXXX-XXXX-XXXX)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors,
                singleLine = true
            )
            }
            if (errorMessage != null && step >= 3) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.mosarib.debt, fontWeight = FontWeight.Bold)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MosaribSecondaryButton(text = "السابق", onClick = { step-- }, modifier = Modifier.weight(1f))
                    MosaribPrimaryButton(
                        text = if (step == 4) "تفعيل" else "التالي",
                        onClick = { if (step == 4) submitActivation() else step++ },
                        modifier = Modifier.weight(1f),
                        enabled = step != 4 || isKeyFormatValid
                    )
                }
            }
            }
            }
            }
        }
    }

    if (showNotRegisteredDialog) {
        MosaribDialog(
            onDismissRequest = { showNotRegisteredDialog = false },
            title = "لا يوجد حساب مسجل مسبقاً",
            icon = Icons.Default.WarningAmber,
            tone = DialogTone.Warning,
            message = "لا يوجد حساب أو اشتراك مرتبط بهذا البريد الإلكتروني مسبقاً:\n($unregisteredEmailAttempt)\n\nيمكنك تسجيل حساب جديد ببياناتك الآن لطلب التفعيل، أو المتابعة بالنسخة المجانية.",
            confirmText = "تسجيل حساب جديد",
            onConfirm = { showNotRegisteredDialog = false },
            dismissText = if (!isMandatory) "المتابعة مجاناً" else null,
            onDismiss = if (!isMandatory) {
                {
                    showNotRegisteredDialog = false
                    onDismiss()
                }
            } else null
        )
    }
}

@Composable
private fun StepperProgress(current: Int, titles: List<String>, modifier: Modifier = Modifier) {
    val digits = listOf("١", "٢", "٣", "٤")
    val primary = MaterialTheme.colorScheme.primary
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            titles.indices.forEach { i ->
                val active = i + 1 <= current
                if (i > 0) {
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(2.dp)
                            .background(if (active) primary else MaterialTheme.colorScheme.outlineVariant)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (active) primary else MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        digits[i],
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = if (active) Color.White else MaterialTheme.mosarib.textMuted
                        )
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(titles[current - 1], style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
    }
}

@Composable
private fun StepCard(number: String, title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(number, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, color = Color.White))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun CollapsibleHeader(title: String, expanded: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant))
        Icon(
            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
