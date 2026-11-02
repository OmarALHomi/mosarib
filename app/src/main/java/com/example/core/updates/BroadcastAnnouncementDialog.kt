package com.example.core.updates

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Star
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

    val (tone, icon, badgeText) = when (broadcast.type.uppercase()) {
        "UPDATE" -> Triple(DialogTone.Info, Icons.Default.SystemUpdate, "تحديث جديد")
        "WARNING", "TECHNICAL", "MAINTENANCE", "CRITICAL" -> Triple(DialogTone.Danger, Icons.Default.WarningAmber, "تنبيه فني هام")
        "OFFER", "PROMO", "GIFT" -> Triple(DialogTone.Warning, Icons.Default.Star, "عرض خاص")
        else -> Triple(DialogTone.Neutral, Icons.Default.Campaign, "نشرة الأخبار")
    }

    val hasAction = broadcast.actionUrl.isNotBlank()

    MosaribDialog(
        onDismissRequest = onDismiss,
        title = broadcast.title.ifBlank { "إشعار جديد" },
        icon = icon,
        tone = tone,
        confirmText = if (hasAction) broadcast.actionText.ifBlank { "عرض التفاصيل" } else "حسناً، فهمت ذلك",
        onConfirm = {
            if (hasAction) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(broadcast.actionUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                runCatching { context.startActivity(intent) }
            }
            onDismiss()
        },
        dismissText = if (hasAction) "حسناً، فهمت ذلك" else null,
        onDismiss = onDismiss
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
                    .heightIn(min = 60.dp, max = 240.dp),
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
        }
    }
}
