package com.example.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.Formatters
import com.example.features.customers.Customer
import com.example.features.wellowners.WellOwnerPurchase
import com.example.features.wellowners.WellOwnerPurchaseMath
import com.example.ui.theme.AccentEmerald

/**
 * شاشة تفاصيل عائمة احترافية ومستجيبة لعملية شراء ساعات من بئر
 */
@Composable
fun WellOwnerPurchaseDetailBottomSheet(
    purchase: WellOwnerPurchase,
    owner: Customer?,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null
) {
    val purchaseColor = Color(0xFF0288D1)

    val chargeableMinutes = WellOwnerPurchaseMath.chargeableMinutes(purchase)
    val totalPayable = WellOwnerPurchaseMath.payableAmount(purchase)
    val wasteCredit = WellOwnerPurchaseMath.ownerWasteCredit(purchase)
    val paidAmount = purchase.amountPaid
    val remainingDebt = (totalPayable - paidAmount).coerceAtLeast(0.0)
    val isSettled = remainingDebt <= 0.0

    OperationDetailSheetScaffold(onDismiss = onDismiss) {
        // 1. رأس الشاشة
        OperationDetailHeader(
            icon = Icons.Default.AccessTime,
            accentColor = purchaseColor,
            iconSize = 20.dp,
            title = "شراء ساعات #${purchase.id}",
            subtitle = Formatters.formatDateTime(purchase.date),
            badgeText = "شراء ساعات من بئر",
            badgeContainerColor = purchaseColor.copy(alpha = 0.14f),
            badgeContentColor = purchaseColor
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 2. كرت المبلغ الصافي المستحق والتفقيط
        OperationDetailAmountCard(
            label = "المبلغ الصافي المستحق لصاحب البئر",
            amount = totalPayable,
            currencySymbol = currencySymbol,
            accentColor = purchaseColor
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 3. كرت صاحب البئر
        val phone = owner?.phone
        OperationDetailPartyCard(
            avatarIcon = Icons.Default.Person,
            avatarTint = purchaseColor,
            avatarBackground = purchaseColor.copy(alpha = 0.12f),
            name = owner?.name ?: "صاحب بئر #${purchase.ownerCustomerId}",
            nameFontSize = 13.5.sp,
            subtitle = {
                if (!phone.isNullOrBlank()) {
                    OperationDetailPhoneText(phone = phone, fontSize = 11.sp)
                }
            },
            trailing = {
                if (!phone.isNullOrBlank()) {
                    OperationDetailContactActions(phone = phone)
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 4. كرت تفاصيل الوقت والمدة والسعر
        PurchaseTimingCard(
            purchase = purchase,
            chargeableMinutes = chargeableMinutes,
            wasteCredit = wasteCredit,
            currencySymbol = currencySymbol,
            purchaseColor = purchaseColor
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 5. كرت الموقف المالي (المدفوع نقداً والمتبقي)
        PurchaseFinancialStatusCard(
            paidAmount = paidAmount,
            remainingDebt = remainingDebt,
            isSettled = isSettled,
            currencySymbol = currencySymbol
        )

        // 6. الملاحظات إن وجدت
        if (purchase.notes.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            OperationDetailCard {
                OperationDetailNotes(
                    notes = purchase.notes,
                    accentColor = purchaseColor,
                    iconSize = 18.dp,
                    iconSpacing = 6.dp,
                    labelFontSize = 11.5.sp,
                    labelSpacing = 2.dp,
                    notesFontSize = 13.sp,
                    notesLineHeight = 18.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 7. أزرار الإجراءات السفلية
        OperationDetailActions {
            if (onShare != null) {
                OperationDetailFilledButton(
                    text = "مشاركة فاتورة الشراء",
                    icon = Icons.AutoMirrored.Filled.Send,
                    containerColor = Color(0xFF2E7D32),
                    onClick = onShare,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OperationDetailEditDeleteRow(
                editLabel = "تعديل الشراء",
                deleteLabel = "حذف الشراء",
                editAccentColor = purchaseColor,
                onEdit = onEdit,
                onDelete = onDelete
            )
        }
    }
}

@Composable
private fun PurchaseTimingCard(
    purchase: WellOwnerPurchase,
    chargeableMinutes: Int,
    wasteCredit: Double,
    currencySymbol: String,
    purchaseColor: Color
) {
    OperationDetailCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // المدة المسجلة
            OperationDetailInfoRow(
                label = "المدة الإجمالية المسجلة",
                value = Formatters.formatDurationArabic(purchase.durationMinutes)
            )

            // سعر شراء الساعة
            OperationDetailInfoRow(
                label = "سعر شراء الساعة",
                value = Formatters.formatCurrency(purchase.purchaseRatePerHour, currencySymbol)
            )

            // الهدر إن وجد
            if (purchase.wastedMinutesOnOwner > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "هدر على صاحب البئر",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFFDC2626),
                                fontSize = 12.5.sp
                            )
                        )
                    }
                    Text(
                        text = "- ${Formatters.formatDurationArabic(purchase.wastedMinutesOnOwner)} (${Formatters.formatCurrency(wasteCredit, currencySymbol)})",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626),
                            fontSize = 12.5.sp
                        )
                    )
                }
            }

            // الساعات الصافية المحتسبة
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الساعات الصافية المحتسبة",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    text = Formatters.formatDurationArabic(chargeableMinutes),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = purchaseColor,
                        fontSize = 13.sp
                    )
                )
            }

            // توقيت البداية والنهاية إن وجد
            if (purchase.startTime > 0 || purchase.endTime > 0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                OperationDetailInfoRow(
                    label = "فترة التشغيل",
                    value = "${if (purchase.startTime > 0) Formatters.formatTime(purchase.startTime) else "—"} إلى ${if (purchase.endTime > 0) Formatters.formatTime(purchase.endTime) else "—"}",
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun PurchaseFinancialStatusCard(
    paidAmount: Double,
    remainingDebt: Double,
    isSettled: Boolean,
    currencySymbol: String
) {
    OperationDetailCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = if (paidAmount > 0) Color(0xFFEF4444) else Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "صرف (المدفوع نقداً)",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.5.sp
                        )
                    )
                }
                Text(
                    text = Formatters.formatCurrency(paidAmount, currencySymbol),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (paidAmount > 0) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.5.sp
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (remainingDebt > 0) "عليك (المتبقي لصاحب البئر)" else "حالة التسوية",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.5.sp
                    )
                )
                if (isSettled) {
                    Surface(
                        color = AccentEmerald.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "مسدد بالكامل ✓",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = AccentEmerald,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                        )
                    }
                } else {
                    Surface(
                        color = Color(0xFFEF4444).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = Formatters.formatCurrency(remainingDebt, currencySymbol),
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color(0xFFEF4444),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp
                            ),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                        )
                    }
                }
            }
        }
    }
}
