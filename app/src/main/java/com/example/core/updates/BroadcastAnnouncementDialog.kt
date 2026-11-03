package com.example.core.updates

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ui.DialogTone
import com.example.core.ui.MosaribDialog
import com.example.core.util.Formatters

@Composable
fun BroadcastAnnouncementDialog(
    broadcast: AppBroadcast,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val isUpdate = broadcast.type.uppercase() == "UPDATE" || broadcast.actionText.contains("تحديث") || broadcast.minRequiredVersionCode > 1
    val isMaintenance = when (broadcast.type.uppercase()) {
        "WARNING", "MAINTENANCE", "TECHNICAL", "CRITICAL" -> true
        else -> false
    }

    val tone = when {
        isUpdate -> DialogTone.Success
        isMaintenance -> DialogTone.Warning
        else -> DialogTone.Info
    }

    val icon = when {
        isUpdate -> Icons.Default.SystemUpdate
        isMaintenance -> Icons.Default.WarningAmber
        else -> Icons.Default.Campaign
    }

    val badgeText = when {
        isUpdate -> "تحديث متوفر"
        isMaintenance -> "تنبيه فني"
        else -> "إعلان جديد"
    }

    val hasAction = broadcast.actionUrl.isNotBlank()

    val confirmText = when {
        isUpdate -> "تحديث"
        hasAction -> broadcast.actionText.ifBlank { "عرض التفاصيل" }
        else -> "حسناً"
    }

    val dismissText = when {
        isUpdate || hasAction -> "لاحقاً"
        else -> null
    }

    MosaribDialog(
        onDismissRequest = {
            AppUpdateManager.dismissBroadcastForToday(context, broadcast.id)
            onDismiss()
        },
        title = broadcast.title.ifBlank { if (isUpdate) "تحديث متوفر" else "إشعار جديد" },
        icon = icon,
        tone = tone,
        confirmText = confirmText,
        onConfirm = {
            if (hasAction) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(broadcast.actionUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                runCatching { context.startActivity(intent) }
            }
            AppUpdateManager.markBroadcastSeen(context, broadcast.id)
            onDismiss()
        },
        dismissText = dismissText,
        onDismiss = {
            AppUpdateManager.dismissBroadcastForToday(context, broadcast.id)
            onDismiss()
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Text(
                    text = badgeText,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }

            if (broadcast.publishedAt > 0L) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "بتاريخ: ${Formatters.formatDate(broadcast.publishedAt)}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp, max = 220.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp)
                ) {
                    Text(
                        text = broadcast.message,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 22.sp
                        ),
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Bulleted release notes if present
            if (broadcast.releaseNotes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "ملاحظات التحديث:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        broadcast.releaseNotes.forEach { note ->
                            Row(
                                modifier = Modifier.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "• ",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = note,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 20.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
