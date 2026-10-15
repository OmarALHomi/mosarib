package com.example.features.feedback

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ui.MosaribInlineLoading
import com.example.core.ui.isAppDark
import com.example.core.license.FirebaseLicenseService
import com.example.core.license.LicenseManager
import com.example.core.license.UserDeviceTracker
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isDark = isAppDark()
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val actRecord = remember { LicenseManager.getActivationRecord(context) }
    var name by remember {
        val registered = UserDeviceTracker.getRegisteredName(context)
        mutableStateOf(if (actRecord?.subscriberName.isNullOrBlank()) registered else actRecord!!.subscriberName)
    }
    var phone by remember { mutableStateOf(UserDeviceTracker.getRegisteredPhone(context)) }
    var rating by remember { mutableIntStateOf(5) }
    var category by remember { mutableStateOf("اقتراح تحسين") }
    var message by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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

    val categories = listOf("اقتراح تحسين", "إبلاغ عن مشكلة", "شكر وتقدير", "طلب ميزة جديدة")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق")
                }
                Text(
                    text = "شاركنا رأيك ومقترحاتك 💬",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                )
                Spacer(modifier = Modifier.width(48.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "رأيك واقتراحاتك تصل مباشرة إلى المهندس عمر الحومي لتطوير التطبيق وخدمتكم بأفضل صورة.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Star Rating
            Text(
                text = "تقييمك لتجربة التطبيق:",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                for (star in 1..5) {
                    IconButton(onClick = { rating = star }) {
                        Icon(
                            imageVector = if (star <= rating) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = "$star نجوم",
                            tint = if (star <= rating) AccentGold else MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category Chips
            Text(
                text = "نوع المشاركة:",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Right
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = category == cat
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) PrimaryTeal else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { category = cat }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cat,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            ),
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("الاسم الكريم") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Phone
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("رقم الهاتف أو الواتساب (اختياري للرد عليك)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Message Body
            OutlinedTextField(
                value = message,
                onValueChange = { message = it; errorMessage = null },
                label = { Text("اكتب رسالتك أو اقتراحك أو مشكلتك بالتفصيل...") },
                minLines = 4,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.error),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (message.isBlank()) {
                        errorMessage = "يرجى كتابة نص الملاحظة أو المقترح أولاً"
                        return@Button
                    }
                    if (!LicenseManager.isInternetAvailable(context)) {
                        errorMessage = "يلزم توفر اتصال بالإنترنت لإرسال الرسالة إلى الإدارة"
                        return@Button
                    }

                    isSubmitting = true
                    errorMessage = null
                    val deviceCode = LicenseManager.getDeviceCode(context)

                    coroutineScope.launch {
                        val result = FirebaseLicenseService.sendFeedbackOnline(
                            subscriberName = if (name.isBlank()) "مستخدم مُسَرِّب" else name.trim(),
                            phone = phone.trim(),
                            rating = rating,
                            category = category,
                            message = message.trim(),
                            deviceCode = deviceCode
                        )
                        isSubmitting = false
                        if (result.isSuccess) {
                            Toast.makeText(context, "شكراً لك! تم إرسال رسالتك بنجاح وسنأخذها بعين الاعتبار.", Toast.LENGTH_LONG).show()
                            onDismiss()
                        } else {
                            errorMessage = result.errorMessage ?: "تعذر الإرسال حالياً، يرجى المحاولة لاحقاً."
                        }
                    }
                },
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryTeal,
                    contentColor = Color.White
                )
            ) {
                if (isSubmitting) {
                    MosaribInlineLoading(
                        size = 22.dp,
                        primaryColor = Color.White,
                        accentColor = Color.White.copy(alpha = 0.5f)
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(20.dp))
                        Text("إرسال الملاحظات للمطور", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }

            if (errorMessage != null && !isSubmitting) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        val deviceCode = LicenseManager.getDeviceCode(context)
                        val waMessage = buildString {
                            append("السلام عليكم م. عمر، لدي ملاحظة بخصوص تطبيق المُسَرِّب:\n\n")
                            append("👤 الاسم: ${if (name.isBlank()) "مستخدم مُسَرِّب" else name.trim()}\n")
                            if (phone.isNotBlank()) append("📞 الهاتف: ${phone.trim()}\n")
                            append("⭐ التقييم: $rating / 5\n")
                            append("🏷️ القسم: $category\n\n")
                            append("📝 نص الرسالة:\n${message.trim()}\n\n")
                            append("📱 معرّف الجهاز: $deviceCode")
                        }
                        val uri = android.net.Uri.parse("https://wa.me/967773712030?text=${android.net.Uri.encode(waMessage)}")
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                        try {
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            Toast.makeText(context, "تطبيق واتساب غير مثبت على الجهاز", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                ) {
                    Text("إرسال الملاحظة مباشرة عبر واتساب للمطور 💬", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Color.White)
                }
            }
        }
    }
}
