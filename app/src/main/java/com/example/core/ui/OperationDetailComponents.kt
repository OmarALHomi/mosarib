package com.example.core.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.Formatters
import com.example.ui.theme.mosarib

/**
 * كتلة المبلغ المالية الكبيرة الموحدة بعرض كامل وخلفية بلون العملية 8% مع التفقيط
 */
@Composable
fun OperationDetailAmountBlock(
    amount: Double,
    currencySymbol: String,
    typeColor: Color,
    modifier: Modifier = Modifier,
    extraContent: (@Composable () -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = typeColor.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, typeColor.copy(alpha = 0.20f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = Formatters.formatCurrency(amount, currencySymbol),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = typeColor
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = Formatters.amountToArabicWords(amount, currencySymbol),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.fillMaxWidth()
            )
            if (extraContent != null) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(
                    thickness = 1.dp,
                    color = typeColor.copy(alpha = 0.15f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                extraContent()
            }
        }
    }
}

/**
 * الكرت الموحد لقائمة تفاصيل العملية (surface, 1dp outlineVariant, 12dp radius)
 */
@Composable
fun OperationDetailCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

/**
 * صف معلومة في كرت التفاصيل بارتفاع 48dp مع أيقونة وعنوان باهت على اليمين وقيمة على اليسار
 */
@Composable
fun OperationDetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: (() -> Unit)? = null,
    trailingActions: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = valueColor
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (trailingActions != null) {
                Spacer(modifier = Modifier.width(8.dp))
                trailingActions()
            }
        }
    }
}

/**
 * فاصل 1dp بين صفوف التفاصيل
 */
@Composable
fun OperationDetailDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    )
}

/**
 * صندوق البيان الموحد مطابق لبطاقات العمليات (لون العملية 8% وأيقونة أسطر)
 */
@Composable
fun OperationDetailStatement(
    statement: String,
    typeColor: Color,
    modifier: Modifier = Modifier
) {
    if (statement.isBlank() || statement == "عام") return
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = typeColor.copy(alpha = if (isDark) 0.12f else 0.08f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, typeColor.copy(alpha = if (isDark) 0.35f else 0.20f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Notes,
                contentDescription = null,
                tint = typeColor,
                modifier = Modifier.size(18.dp).padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "البيان:",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = typeColor
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = statement,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    }
}

/**
 * قائمة الخيارات الإضافية في ترويسة الشاشة المنبثقة ⋮ (تتضمن PDF وحذف بشكل تخريبي)
 */
@Composable
fun OperationDetailHeaderMenu(
    onPdf: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    deleteLabel: String = "حذف"
) {
    if (onPdf == null && onDelete == null) return
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "خيارات إضافية",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        MosaribMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (onPdf != null) {
                MosaribMenuItem(
                    text = "تصدير PDF",
                    icon = Icons.Default.PictureAsPdf,
                    onClick = {
                        expanded = false
                        onPdf()
                    }
                )
            }
            if (onDelete != null) {
                if (onPdf != null) {
                    MosaribMenuDivider()
                }
                MosaribMenuItem(
                    text = deleteLabel,
                    icon = Icons.Default.Delete,
                    destructive = true,
                    onClick = {
                        expanded = false
                        onDelete()
                    }
                )
            }
        }
    }
}

/**
 * أزرار اتصال وواتساب سريعة لرقم الهاتف
 */
@Composable
fun OperationDetailContactActions(phone: String) {
    val context = LocalContext.current

    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        IconButton(
            onClick = {
                try {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                    context.startActivity(intent)
                } catch (_: Exception) {}
            },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Phone,
                contentDescription = "اتصال",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }

        IconButton(
            onClick = {
                try {
                    val cleanPhone = phone.replace("+", "").replace(" ", "").trim()
                    val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone")
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                } catch (_: Exception) {}
            },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = "واتساب",
                tint = MaterialTheme.mosarib.success,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
