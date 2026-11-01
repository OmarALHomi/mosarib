package com.example.features.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.license.LicenseManager
import com.example.core.license.RemoteConfigManager
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal

// Section: الترخيص والتفعيل (200 عملية مجانية / تفعيل دائم)
@Composable
internal fun LicenseSection(
    isActivated: Boolean,
    operationsCount: Int,
    deviceCode: String,
    onOpenLicenseDialog: () -> Unit
) {
    val context = LocalContext.current
    SettingsSectionCard {
        val customDeviceLimit = RemoteConfigManager.getCustomDeviceLimit(context)
        val headerLimit = LicenseManager.getFreeOperationsLimit(context)

        LicenseHeader(
            isActivated = isActivated,
            customDeviceLimit = customDeviceLimit,
            headerLimit = headerLimit
        )

        Spacer(modifier = Modifier.height(14.dp))

        DeviceCodeRow(context = context, deviceCode = deviceCode)

        Spacer(modifier = Modifier.height(12.dp))

        if (!isActivated || customDeviceLimit > 0) {
            TrialUsagePanel(
                context = context,
                operationsCount = operationsCount,
                customDeviceLimit = customDeviceLimit,
                onOpenLicenseDialog = onOpenLicenseDialog
            )
        } else {
            ActiveSubscriptionPanel(
                context = context,
                isActivated = isActivated,
                onOpenLicenseDialog = onOpenLicenseDialog
            )
        }
    }
}

@Composable
private fun LicenseHeader(
    isActivated: Boolean,
    customDeviceLimit: Int,
    headerLimit: Int
) {
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
                    .background(if (isActivated) AccentEmerald.copy(alpha = 0.15f) else AccentGold.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isActivated) Icons.Default.Verified else Icons.Default.Key,
                    contentDescription = null,
                    tint = if (isActivated) AccentEmerald else AccentGold
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "الترخيص والتفعيل",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = if (isActivated) {
                        if (customDeviceLimit > 0) "نسخة مفعّلة (حد مخصص: $customDeviceLimit عملية)" else "نسخة مفعّلة"
                    } else {
                        if (customDeviceLimit > 0) "نسخة تجريبية (حد مخصص: $customDeviceLimit عملية)" else "النسخة التجريبية ($headerLimit عملية)"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isActivated) AccentEmerald else AccentGold,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        // Badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (isActivated) AccentEmerald.copy(alpha = 0.15f) else Color(0xFFFF9800).copy(alpha = 0.15f))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (isActivated) "مفعّل" else "تجريبي",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (isActivated) AccentEmerald else Color(0xFFFF9800),
                    fontWeight = FontWeight.ExtraBold
                )
            )
        }
    }
}

// Device Code row
@Composable
private fun DeviceCodeRow(context: Context, deviceCode: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "رمز الجهاز الفريد (Device Token):",
                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B))
            )
            Text(
                text = deviceCode,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            )
        }

        IconButton(
            onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("كود جهاز المسرب", deviceCode)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "تم نسخ كود الجهاز إلى الحافظة", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", modifier = Modifier.size(16.dp))
        }
    }
}

// Free/limited usage progress + activation call to action
@Composable
private fun TrialUsagePanel(
    context: Context,
    operationsCount: Int,
    customDeviceLimit: Int,
    onOpenLicenseDialog: () -> Unit
) {
    val freeLimit = LicenseManager.getFreeOperationsLimit(context)
    val remaining = LicenseManager.getRemainingOperations(context, operationsCount)
    val progress = if (freeLimit > 0) (operationsCount.toFloat() / freeLimit).coerceIn(0f, 1f) else 1f

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (customDeviceLimit > 0) "العمليات المستخدمة من الحد المخصص:" else "العمليات المجانية المستخدمة:",
                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Text(
                text = "$operationsCount / $freeLimit",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = if (operationsCount >= freeLimit) Color(0xFFE53935) else PrimaryTeal,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (remaining > 0)
                "متبقي لديك $remaining عملية مجانية قبل الحاجة للتفعيل."
            else
                "لقد استنفدت العمليات المجانية بالكامل ($freeLimit/$freeLimit). يرجى التفعيل للمتابعة.",
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (remaining == 0) Color(0xFFE53935) else Color(0xFF64748B),
                fontWeight = if (remaining == 0) FontWeight.Bold else FontWeight.Normal
            )
        )
    }

    Spacer(modifier = Modifier.height(14.dp))

    Button(
        onClick = onOpenLicenseDialog,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
        shape = RoundedCornerShape(10.dp)
    ) {
        Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("تفعيل النسخة الكاملة الآن", fontWeight = FontWeight.Bold)
    }
}

// Active subscription details + renew/upgrade action
@Composable
private fun ActiveSubscriptionPanel(
    context: Context,
    isActivated: Boolean,
    onOpenLicenseDialog: () -> Unit
) {
    val plan = LicenseManager.getActivePlan(context)
    val remDays = LicenseManager.getRemainingDays(context)
    val actRecord = remember(isActivated) { LicenseManager.getActivationRecord(context) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "حالة الاشتراك: ${plan?.titleArabic ?: "اشتراك سارٍ"}",
            style = MaterialTheme.typography.bodyMedium.copy(color = AccentEmerald, fontWeight = FontWeight.Bold)
        )
        if (actRecord != null && actRecord.subscriberName.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "مسجل باسم: ${actRecord.subscriberName} • ${actRecord.deviceModel}",
                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "متبقي في اشتراكك الحالي: $remDays يوماً.",
            style = MaterialTheme.typography.bodySmall.copy(
                color = if (remDays <= 5) Color(0xFFE53935) else PrimaryTeal,
                fontWeight = FontWeight.SemiBold
            )
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedButton(
            onClick = onOpenLicenseDialog,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("تجديد أو ترقية الاشتراك", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}
