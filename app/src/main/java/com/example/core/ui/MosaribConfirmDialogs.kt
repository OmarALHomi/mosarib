package com.example.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.util.Formatters
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal
import java.io.File

/**
 * نافذة منبثقة فاخرة ومتجاوبة لفتح أو مشاركة الملفات (تقارير PDF، إكسل، كشوفات الحسابات).
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
    val isDark = isAppDark()
    val colorScheme = MaterialTheme.colorScheme
    val bgColor = colorScheme.surface
    val cardBg = if (isDark) colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color(0xFFF8FAFC)
    val cardBorder = colorScheme.outline.copy(alpha = if (isDark) 0.35f else 0.25f)
    val textColor = colorScheme.onSurface
    val subtitleColor = colorScheme.onSurfaceVariant

    val (typeIcon, typeColor, typeBadge) = when {
        file.name.endsWith(".xlsx", true) || file.name.endsWith(".xls", true) || fileType.contains("Excel", true) -> {
            Triple(Icons.Default.TableChart, Color(0xFF107C41), "جدول Excel")
        }
        file.name.endsWith(".csv", true) || fileType.contains("CSV", true) -> {
            Triple(Icons.Default.Description, Color(0xFF0284C7), "ملف CSV")
        }
        else -> {
            Triple(Icons.Default.PictureAsPdf, Color(0xFFE53935), "مستند PDF")
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(vertical = 16.dp)
                    .shadow(24.dp, RoundedCornerShape(26.dp), spotColor = PrimaryTeal.copy(alpha = 0.35f))
                    .clip(RoundedCornerShape(26.dp))
                    .border(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(PrimaryTeal.copy(alpha = 0.6f), AccentGold.copy(alpha = 0.3f))
                        ),
                        RoundedCornerShape(26.dp)
                    ),
                color = bgColor
            ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row (Icon + Title + Close)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(typeColor.copy(alpha = 0.15f))
                            .border(1.dp, typeColor.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = typeIcon,
                            contentDescription = null,
                            tint = typeColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = textColor,
                                fontSize = 16.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(typeColor.copy(alpha = 0.12f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = typeBadge,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = typeColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.5.sp
                                    )
                                )
                            }
                            if (fileSizeText.isNotBlank()) {
                                Text(
                                    text = "• $fileSizeText",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = subtitleColor,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = subtitleColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // File metadata box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(cardBg)
                        .padding(12.dp)
                ) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = subtitleColor,
                            lineHeight = 20.sp,
                            fontSize = 12.5.sp
                        ),
                        textAlign = TextAlign.Start
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Primary Action 1: Open / View
                Button(
                    onClick = onOpen,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "فتح وعرض الملف الآن",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Primary Action 2: Share / Send via WhatsApp
                Button(
                    onClick = onShare,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "مشاركة الملف عبر التطبيقات / واتساب",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Dismiss Text Button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "إلغاء والعودة",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = subtitleColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}
}

/**
 * نافذة تأكيد الخروج الفاخرة من التطبيق (Exit Confirmation Dialog).
 */
@Composable
fun MosaribExitConfirmDialog(
    onConfirmExit: () -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = isAppDark()
    val colorScheme = MaterialTheme.colorScheme
    val bgColor = colorScheme.surface
    val textColor = colorScheme.onSurface
    val subtitleColor = colorScheme.onSurfaceVariant
    val exitRed = if (isDark) Color(0xFFFF5252) else Color(0xFFE53935)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .padding(vertical = 16.dp)
                    .shadow(24.dp, RoundedCornerShape(26.dp), spotColor = exitRed.copy(alpha = 0.25f))
                    .clip(RoundedCornerShape(26.dp))
                    .border(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(exitRed.copy(alpha = 0.45f), colorScheme.outline.copy(alpha = 0.25f))
                        ),
                        RoundedCornerShape(26.dp)
                    ),
                color = bgColor
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(exitRed.copy(alpha = 0.12f))
                            .border(1.dp, exitRed.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = null,
                            tint = exitRed,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "الخروج من تطبيق المُسَرِّب",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = textColor,
                            fontSize = 17.sp
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "هل أنت متأكد من رغبتك في إغلاق التطبيق؟ جميع عملياتك وبياناتك محفوظة بأمان.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = subtitleColor,
                            fontSize = 12.5.sp,
                            lineHeight = 20.sp
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // إلغاء (البقاء)
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) colorScheme.surfaceVariant else PrimaryTeal,
                                contentColor = if (isDark) colorScheme.onSurface else Color.White
                            )
                        ) {
                            Text(
                                text = "البقاء بالتطبيق",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            )
                        }

                        // خروج
                        OutlinedButton(
                            onClick = onConfirmExit,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, exitRed.copy(alpha = 0.7f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = exitRed)
                        ) {
                            Text(
                                text = "خروج",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
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
    val isDark = isAppDark()
    val colorScheme = MaterialTheme.colorScheme
    val bgColor = colorScheme.surface
    val textColor = colorScheme.onSurface
    val subtitleColor = colorScheme.onSurfaceVariant

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .padding(vertical = 16.dp)
                    .shadow(20.dp, RoundedCornerShape(24.dp), spotColor = confirmColor.copy(alpha = 0.25f))
                    .clip(RoundedCornerShape(24.dp))
                    .border(
                        1.dp,
                        confirmColor.copy(alpha = 0.35f),
                        RoundedCornerShape(24.dp)
                    ),
                color = bgColor
            ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(confirmColor.copy(alpha = 0.12f))
                        .border(1.dp, confirmColor.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = confirmColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = textColor,
                        fontSize = 16.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = subtitleColor,
                        fontSize = 12.5.sp,
                        lineHeight = 19.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = dismissText,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = confirmColor)
                    ) {
                        Text(
                            text = confirmText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}
}
