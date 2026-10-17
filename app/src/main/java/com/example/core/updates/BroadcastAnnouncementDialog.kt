package com.example.core.updates

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.ui.isAppDark
import com.example.core.util.Formatters
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal

private data class BroadcastTypeStyle(
    val badgeText: String,
    val badgeIcon: ImageVector,
    val accentColor: Color,
    val badgeBg: Color
)

@Composable
fun BroadcastAnnouncementDialog(
    broadcast: AppBroadcast,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isDark = isAppDark()

    val style = when (broadcast.type.uppercase()) {
        "UPDATE" -> BroadcastTypeStyle(
            badgeText = "تحديث جديد متوفر",
            badgeIcon = Icons.Default.SystemUpdate,
            accentColor = Color(0xFF0284C7),
            badgeBg = if (isDark) Color(0xFF0C2A3A) else Color(0xFFE0F2FE)
        )
        "WARNING" -> BroadcastTypeStyle(
            badgeText = "تنبيه وإشعار هام",
            badgeIcon = Icons.Default.WarningAmber,
            accentColor = Color(0xFFEA580C),
            badgeBg = if (isDark) Color(0xFF381A08) else Color(0xFFFFF7ED)
        )
        "OFFER", "PROMO" -> BroadcastTypeStyle(
            badgeText = "عرض خاص ومميز",
            badgeIcon = Icons.Default.Star,
            accentColor = AccentGold,
            badgeBg = if (isDark) Color(0xFF302306) else Color(0xFFFEF9C3)
        )
        else -> BroadcastTypeStyle(
            badgeText = "إعلان رسمي من الإدارة",
            badgeIcon = Icons.Default.Campaign,
            accentColor = PrimaryTeal,
            badgeBg = if (isDark) Color(0xFF0F2B2B) else Color(0xFFE6F4F1)
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(vertical = 16.dp)
                    .shadow(24.dp, RoundedCornerShape(26.dp), spotColor = style.accentColor.copy(alpha = 0.35f))
                    .clip(RoundedCornerShape(26.dp))
                    .border(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(style.accentColor.copy(alpha = 0.65f), style.accentColor.copy(alpha = 0.2f))
                        ),
                        RoundedCornerShape(26.dp)
                    ),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Bar: Type Badge + Close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = style.badgeBg,
                            border = BorderStroke(1.dp, style.accentColor.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = style.badgeIcon,
                                    contentDescription = null,
                                    tint = style.accentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = style.badgeText,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = style.accentColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp
                                    )
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Title
                    Text(
                        text = broadcast.title.ifBlank { "إعلان جديد" },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        textAlign = TextAlign.Center
                    )

                    if (broadcast.publishedAt > 0L) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = Formatters.formatDate(broadcast.publishedAt),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.5.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Message Box
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 60.dp, max = 260.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = if (isDark) Color(0xFF131D22) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
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
                                    lineHeight = 23.sp,
                                    fontSize = 13.5.sp
                                ),
                                textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    if (broadcast.actionUrl.isNotBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(broadcast.actionUrl)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                runCatching { context.startActivity(intent) }
                                onDismiss()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = style.accentColor,
                                contentColor = if (style.accentColor == AccentGold) Color.Black else Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = broadcast.actionText.ifBlank { "عرض التفاصيل" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = "تم الاطلاع / إغلاق",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.5.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
