package com.example.core.license

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicenseDialog(
    onDismiss: () -> Unit,
    onActivated: () -> Unit,
    isMandatory: Boolean = false // If true, cannot dismiss until activated
) {
    val context = LocalContext.current
    val deviceCode = remember { LicenseManager.getDeviceCode(context) }
    val deviceModel = remember { LicenseManager.getDeviceModelName() }
    val androidVersion = remember { LicenseManager.getAndroidVersionName() }
    val isCurrentlyActivated = remember { LicenseManager.isActivated(context) }
    val existingRecord = remember { LicenseManager.getActivationRecord(context) }

    var subscriberName by remember { mutableStateOf(existingRecord?.subscriberName ?: "") }
    var enteredKey by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedPlan by remember { mutableStateOf(LicenseManager.SubscriptionPlan.MONTHLY) }
    var isOnline by remember { mutableStateOf(LicenseManager.isInternetAvailable(context)) }

    fun refreshInternet() {
        isOnline = LicenseManager.isInternetAvailable(context)
    }

    fun copyDeviceCode() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("كود جهاز المسرب", deviceCode)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "تم نسخ كود الجهاز إلى الحافظة", Toast.LENGTH_SHORT).show()
    }

    fun openWhatsApp() {
        val url = LicenseManager.getWhatsAppActivationUrl(deviceCode, subscriberName, selectedPlan)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun submitActivation() {
        errorMessage = null
        refreshInternet()
        if (!isOnline) {
            errorMessage = "تعذر التفعيل: يلزم الاتصال بالإنترنت لإتمام وتوثيق عملية التفعيل بالسيرفر."
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
                .padding(horizontal = 22.dp, vertical = 12.dp)
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
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 1. Internet Connection Status Indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isOnline) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))
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
                            tint = if (isOnline) Color(0xFF2E7D32) else Color(0xFFD32F2F),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isOnline) "متصل بالإنترنت • جاهز للتوثيق والتفعيل" else "غير متصل بالإنترنت • يلزم الاتصال لتوثيق التفعيل",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isOnline) Color(0xFF2E7D32) else Color(0xFFD32F2F),
                                fontSize = 11.sp
                            )
                        )
                    }

                    if (!isOnline) {
                        androidx.compose.material3.TextButton(
                            onClick = { refreshInternet() },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("إعادة فحص", fontSize = 10.5.sp, color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
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
            Text(
                text = if (isMandatory)
                    "لقد استنفدت 200 عملية مجانية في النسخة التجريبية. يرجى إدخال اسمك وطلب كود التفعيل عبر واتساب وتفعيله مع الاتصال بالإنترنت للاستمرار بلا حدود."
                else
                    "لتفعيل التطبيق، اكتب اسمك، واختر الباقة، ثم انسخ الكود وأرسله عبر الواتساب للمطور لاستلام رمز التفعيل وتأكيده مع توفر الإنترنت.",
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
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 5. Detected Device Info Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Smartphone, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "جهازك المسجل: $deviceModel • $androidVersion",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.5.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Plan Toggle Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { selectedPlan = LicenseManager.SubscriptionPlan.MONTHLY },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedPlan == LicenseManager.SubscriptionPlan.MONTHLY) PrimaryTeal else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (selectedPlan == LicenseManager.SubscriptionPlan.MONTHLY) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("اشتراك شهري (30 يوماً)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Button(
                    onClick = { selectedPlan = LicenseManager.SubscriptionPlan.YEARLY },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedPlan == LicenseManager.SubscriptionPlan.YEARLY) AccentGold else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (selectedPlan == LicenseManager.SubscriptionPlan.YEARLY) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("اشتراك سنوي ⭐ (365 يوماً)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

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
                            Text("طلب رمز التفعيل عبر واتساب", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Activation Key Input
            OutlinedTextField(
                value = enteredKey,
                onValueChange = { enteredKey = it.uppercase() },
                label = { Text("أدخل رمز التفعيل (ACTV-XXXX-XXXX)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
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

            // Submit Button
            Button(
                onClick = { submitActivation() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
            ) {
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "تفعيل التطبيق الآن",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}
