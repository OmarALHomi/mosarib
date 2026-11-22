package com.example.core.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Sms
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
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.mosarib
import com.example.ui.theme.PrimaryTeal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.core.util.MessageFormatHelper

@Composable
fun SendMessageChoiceDialog(
    recipientName: String,
    recipientPhone: String,
    messageText: String,
    smsMessageText: String? = null,
    onDismiss: () -> Unit,
    onSendWhatsApp: () -> Unit,
    onSendSms: () -> Unit
) {
    val context = LocalContext.current
    val actualSmsText = smsMessageText ?: messageText
    var selectedChannel by remember { mutableStateOf(if (smsMessageText != null) "sms" else "whatsapp") }
    val currentPreviewText = if (selectedChannel == "sms") actualSmsText else messageText
    val smsSegments = MessageFormatHelper.calculateSmsSegments(actualSmsText)

    MosaribBottomSheet(
        onDismissRequest = onDismiss,
        title = "إرسال الإشعار والتفاصيل",
        subtitle = if (recipientPhone.isBlank()) "إلى: $recipientName (بدون رقم، اختر جهة الاتصال عند الإرسال)" else "إلى: $recipientName ($recipientPhone)",
        icon = Icons.AutoMirrored.Filled.Send,
        iconTint = PrimaryTeal
    ) {

            // Channel Preview Switcher (واتساب vs SMS)
            if (smsMessageText != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        onClick = { selectedChannel = "whatsapp" },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedChannel == "whatsapp") MaterialTheme.mosarib.success.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selectedChannel == "whatsapp") MaterialTheme.mosarib.success else Color.Transparent
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                tint = if (selectedChannel == "whatsapp") MaterialTheme.mosarib.success else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "واتساب (منسق)",
                                fontSize = 12.sp,
                                fontWeight = if (selectedChannel == "whatsapp") FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedChannel == "whatsapp") MaterialTheme.mosarib.success else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Card(
                        onClick = { selectedChannel = "sms" },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedChannel == "sms") PrimaryTeal.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selectedChannel == "sms") PrimaryTeal else Color.Transparent
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Sms,
                                contentDescription = null,
                                tint = if (selectedChannel == "sms") PrimaryTeal else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SMS (مختصر)",
                                fontSize = 12.sp,
                                fontWeight = if (selectedChannel == "sms") FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedChannel == "sms") PrimaryTeal else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // Concise message preview card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (selectedChannel == "sms") "معاينة نص SMS:" else "معاينة نص واتساب:",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            if (selectedChannel == "sms") {
                                androidx.compose.material3.Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (smsSegments <= 1) MaterialTheme.mosarib.success.copy(alpha = 0.15f) else PrimaryTeal.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "${actualSmsText.length} حرف ($smsSegments ${if (smsSegments == 1) "رسالة" else "رسائل"})",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (smsSegments <= 1) MaterialTheme.mosarib.success else PrimaryTeal,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("نص الفاتورة", currentPreviewText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "تم نسخ النص إلى الحافظة", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "نسخ",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = currentPreviewText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 18.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "اختر وسيلة الإرسال:",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Button 1: WhatsApp
            Button(
                onClick = {
                    onSendWhatsApp()
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.mosarib.success)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "إرسال عبر واتساب",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Button 2: SMS
            Button(
                onClick = {
                    onSendSms()
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
            ) {
                Icon(
                    Icons.Default.Sms,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "رسالة نصية (SMS) — $smsSegments ${if (smsSegments == 1) "رسالة" else "رسائل"}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
    }
}
