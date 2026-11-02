package com.example.core.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.mosarib
import com.example.core.util.Formatters

/**
 * الهيكل الموحّد للشاشات العائمة لتفاصيل العمليات (اتجاه عربي + تمرير عمودي)
 */
@Composable
internal fun OperationDetailSheetScaffold(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    MosaribBottomSheet(
        onDismissRequest = onDismiss,
        title = "",
        content = content
    )
}

/**
 * رأس الشاشة: أيقونة دائرية + العنوان والتاريخ + شارة الحالة
 */
@Composable
internal fun OperationDetailHeader(
    icon: ImageVector,
    accentColor: Color,
    iconSize: Dp,
    title: String,
    subtitle: String,
    badgeText: String,
    badgeContainerColor: Color,
    badgeContentColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(iconSize)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.5.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // شارة الحالة
        Surface(
            color = badgeContainerColor,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = badgeText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = badgeContentColor,
                    fontSize = 11.sp
                ),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                maxLines = 1
            )
        }
    }
}

/**
 * الكرت الرمادي الموحّد لأقسام التفاصيل
 */
@Composable
internal fun OperationDetailCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        content = content
    )
}

/**
 * كرت المبلغ الفخم مع التفقيط بالعربية
 */
@Composable
internal fun OperationDetailAmountCard(
    label: String,
    amount: Double,
    currencySymbol: String,
    accentColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = accentColor.copy(alpha = 0.08f)
        ),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp
                )
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = Formatters.formatCurrency(amount, currencySymbol),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = accentColor,
                    fontSize = 26.sp
                ),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = Formatters.amountToArabicWords(amount, currencySymbol),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    fontSize = 11.5.sp
                ),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

/**
 * كرت الطرف المقابل (عميل / مستفيد / صاحب بئر): صورة رمزية + الاسم + سطر فرعي + إجراءات جانبية
 */
@Composable
internal fun OperationDetailPartyCard(
    avatarIcon: ImageVector,
    avatarTint: Color,
    avatarBackground: Color,
    name: String,
    nameFontSize: TextUnit,
    subtitle: @Composable ColumnScope.() -> Unit = {},
    trailing: @Composable RowScope.() -> Unit = {}
) {
    OperationDetailCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(avatarBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = avatarIcon,
                        contentDescription = null,
                        tint = avatarTint,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = nameFontSize
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    subtitle()
                }
            }

            trailing()
        }
    }
}

@Composable
internal fun OperationDetailPhoneText(phone: String, fontSize: TextUnit) {
    Text(
        text = phone,
        style = MaterialTheme.typography.bodySmall.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = fontSize
        ),
        maxLines = 1
    )
}

/**
 * أزرار اتصال وواتساب سريعة للطرف المقابل
 */
@Composable
internal fun OperationDetailContactActions(phone: String) {
    val context = LocalContext.current

    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilledTonalIconButton(
            onClick = {
                try {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                    context.startActivity(intent)
                } catch (_: Exception) {}
            },
            modifier = Modifier.size(34.dp)
        ) {
            Icon(Icons.Default.Phone, contentDescription = "اتصال", modifier = Modifier.size(16.dp))
        }

        FilledTonalIconButton(
            onClick = {
                try {
                    val cleanPhone = phone.replace("+", "").replace(" ", "").trim()
                    val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone")
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                } catch (_: Exception) {}
            },
            modifier = Modifier.size(34.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.mosarib.success.copy(alpha = 0.15f),
                contentColor = MaterialTheme.mosarib.success
            )
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "واتساب", modifier = Modifier.size(16.dp))
        }
    }
}

/**
 * صف معلومة بسيط: عنوان باهت يميناً وقيمة بارزة يساراً
 */
@Composable
internal fun OperationDetailInfoRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    fontSize: TextUnit = 12.5.sp,
    valueMaxLines: Int = Int.MAX_VALUE,
    valueOverflow: TextOverflow = TextOverflow.Clip
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = fontSize
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = valueColor,
                fontSize = fontSize
            ),
            maxLines = valueMaxLines,
            overflow = valueOverflow
        )
    }
}

/**
 * كتلة البيان / الملاحظات بأيقونة وعنوان ونص بارز
 */
@Composable
internal fun OperationDetailNotes(
    notes: String,
    accentColor: Color,
    iconSize: Dp,
    iconSpacing: Dp,
    labelFontSize: TextUnit,
    labelSpacing: Dp,
    notesFontSize: TextUnit,
    notesLineHeight: TextUnit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Notes,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(iconSize).padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(iconSpacing))
        Column {
            Text(
                text = "البيان / الملاحظات:",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    fontSize = labelFontSize
                )
            )
            Spacer(modifier = Modifier.height(labelSpacing))
            Text(
                text = notes,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = notesFontSize,
                    lineHeight = notesLineHeight
                )
            )
        }
    }
}

/**
 * عمود أزرار الإجراءات السفلية
 */
@Composable
internal fun OperationDetailActions(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

/**
 * زر إجراء ممتلئ (مشاركة / إرسال / PDF)
 */
@Composable
internal fun OperationDetailFilledButton(
    text: String,
    icon: ImageVector,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, maxLines = 1)
    }
}

/**
 * صف التعديل والحذف (يظهر فقط عند توفر أحدهما)
 */
@Composable
internal fun OperationDetailEditDeleteRow(
    editLabel: String,
    deleteLabel: String,
    editAccentColor: Color,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    if (onEdit != null || onDelete != null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (onEdit != null) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, editAccentColor.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = editAccentColor)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(editLabel, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            if (onDelete != null) {
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.mosarib.debt),
                    border = BorderStroke(1.dp, MaterialTheme.mosarib.debt.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.mosarib.debt)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(deleteLabel, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, color = MaterialTheme.mosarib.debt)
                }
            }
        }
    }
}
