package com.example.features.settings

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ui.DialogTone
import com.example.core.ui.MosaribDialog
import com.example.ui.theme.mosarib

// Restore confirmation with critical data-loss warning and automatic safety checkpoint notice
@Composable
internal fun RestoreWarningDialog(
    title: String,
    backupDateOrName: String,
    additionalInfo: String? = null,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    MosaribDialog(
        onDismissRequest = onDismiss,
        title = title,
        icon = Icons.Default.Restore,
        tone = DialogTone.Danger,
        confirmText = "تأكيد الاستعادة",
        onConfirm = onConfirm,
        dismissText = "إلغاء",
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // بطاقة بيانات النسخة المراد استعادتها
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "النسخة: $backupDateOrName",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        if (additionalInfo != null) {
                            Text(
                                text = additionalInfo,
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.mosarib.textMuted)
                            )
                        }
                    }
                }
            }

            // بطاقة الأمان والحفظ المسبق
            Surface(
                color = MaterialTheme.mosarib.successContainer,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MaterialTheme.mosarib.success.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.mosarib.success,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "حماية بياناتك الحالية مؤتمتة ومضمونة:",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.mosarib.onSuccessContainer
                            )
                        )
                    }

                    Text(
                        text = "لحمايتك من فقدان أي بيانات، سيقوم التطبيق تلقائياً بأخذ نقطة أمان احتياطية لبياناتك الحالية الآن قبل البدء وحفظها في قسم (النسخ التلقائي الدوري) باسم:\nmosarib_safety_before_restore_...\nبحيث يمكنك الرجوع إليها بالكامل في أي وقت بكل سهولة إن احتجت لذلك.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.mosarib.onSuccessContainer,
                            lineHeight = 16.sp,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            // شريط التنبيه السفلي مع خط أحمر في الأسفل
            Surface(
                color = MaterialTheme.mosarib.warningContainer,
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                border = BorderStroke(1.dp, MaterialTheme.mosarib.warning.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.mosarib.warning,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تنبيه: ستصبح بيانات هذه النسخة هي النشطة، وأي عمليات جديدة بعدها ستجدها محفوظة بنقطة الأمان.",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.mosarib.onWarningContainer,
                                lineHeight = 15.sp,
                                fontSize = 10.5.sp
                            )
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(MaterialTheme.mosarib.debt, shape = RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                    )
                }
            }
        }
    }
}
