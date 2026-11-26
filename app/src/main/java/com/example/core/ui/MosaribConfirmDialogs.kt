package com.example.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.example.core.license.RemoteConfigManager
import androidx.compose.ui.unit.dp
import com.example.ui.theme.mosarib
import java.io.File

/**
 * نافذة منبثقة موحدة ومتجاوبة لفتح أو مشاركة الملفات (تقارير PDF، إكسل، كشوفات الحسابات).
 */
@Composable
fun MosaribFileActionDialog(
    file: File,
    title: String,
    subtitle: String = "تم تجهيز المستند بنجاح. يمكنك فتحه للاطلاع عليه مباشرة أو إرساله ومشاركته عبر واتساب والتطبيقات الأخرى.",
    fileType: String = "PDF",
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isPdfFile = file.name.endsWith(".pdf", true) ||
        (!file.name.endsWith(".xlsx", true) && !file.name.endsWith(".xls", true) && !file.name.endsWith(".csv", true) && fileType.equals("PDF", true))
    if (isPdfFile && !RemoteConfigManager.isPdfExportAllowed(context)) {
        MosaribDialog(
            onDismissRequest = onDismiss,
            title = "الميزة غير متاحة حالياً",
            message = RemoteConfigManager.FEATURE_DISABLED_MESSAGE,
            icon = Icons.Default.Warning,
            tone = DialogTone.Warning,
            confirmText = "حسناً",
            onConfirm = onDismiss
        )
        return
    }
    val (typeIcon, typeColor, typeBadge) = when {
        file.name.endsWith(".xlsx", true) || file.name.endsWith(".xls", true) || fileType.contains("Excel", true) -> {
            Triple(Icons.Default.TableChart, MaterialTheme.mosarib.success, "جدول Excel")
        }
        file.name.endsWith(".csv", true) || fileType.contains("CSV", true) -> {
            Triple(Icons.Default.Description, MaterialTheme.mosarib.info, "ملف CSV")
        }
        else -> {
            Triple(Icons.Default.PictureAsPdf, MaterialTheme.mosarib.debt, "مستند PDF")
        }
    }

    val fileSizeText = try {
        val bytes = file.length()
        when {
            bytes >= 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> "${bytes / 1024} KB"
            else -> "$bytes B"
        }
    } catch (_: Exception) {
        ""
    }

    MosaribDialog(
        onDismissRequest = onDismiss,
        title = title,
        icon = typeIcon,
        tone = DialogTone.Info,
        confirmText = "فتح الملف",
        onConfirm = onOpen,
        dismissText = "إلغاء والعودة",
        onDismiss = onDismiss
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = typeColor.copy(alpha = 0.12f)
            ) {
                Text(
                    text = typeBadge,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = typeColor,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            if (fileSizeText.isNotBlank()) {
                Text(
                    text = "• $fileSizeText",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (subtitle.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onShare,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.mosarib.success,
                contentColor = MaterialTheme.mosarib.onSuccess
            )
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "مشاركة الملف",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

/**
 * نافذة تأكيد الخروج الموحدة من التطبيق (Exit Confirmation Dialog).
 */
@Composable
fun MosaribExitConfirmDialog(
    onConfirmExit: () -> Unit,
    onDismiss: () -> Unit
) {
    MosaribDialog(
        onDismissRequest = onDismiss,
        title = "الخروج من تطبيق المُسَرِّب",
        message = "هل أنت متأكد من رغبتك في إغلاق التطبيق؟ جميع عملياتك وبياناتك محفوظة بأمان.",
        icon = Icons.AutoMirrored.Filled.ExitToApp,
        tone = DialogTone.Danger,
        confirmText = "خروج",
        onConfirm = onConfirmExit,
        dismissText = "البقاء بالتطبيق",
        onDismiss = onDismiss
    )
}

/**
 * نافذة تأكيد الإجراءات والحذف العامة (General Action & Deletion Confirmation Dialog).
 */
@Composable
fun MosaribActionConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "تأكيد",
    dismissText: String = "إلغاء",
    confirmColor: Color = Color(0xFFE53935),
    icon: ImageVector = Icons.Default.Warning,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val tone = if (confirmColor == Color(0xFFE53935) || confirmColor == Color.Red) {
        DialogTone.Danger
    } else {
        DialogTone.Neutral
    }
    MosaribDialog(
        onDismissRequest = onDismiss,
        title = title,
        message = message,
        icon = icon,
        tone = tone,
        confirmText = confirmText,
        onConfirm = onConfirm,
        dismissText = dismissText,
        onDismiss = onDismiss
    )
}
