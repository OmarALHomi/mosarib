package com.example.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.util.Formatters
import com.example.features.customers.Customer
import com.example.features.wellowners.WellOwnerPurchase
import com.example.features.wellowners.WellOwnerPurchaseMath
import com.example.ui.theme.mosarib

/**
 * شاشة تفاصيل عائمة موحدة لعمليات شراء الساعات من صاحب بئر متناسقة مع لغة بطاقات العمليات (OperationCard)
 */
@Composable
fun WellOwnerPurchaseDetailBottomSheet(
    purchase: WellOwnerPurchase,
    owner: Customer?,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onPdf: (() -> Unit)? = null,
    onCustomerClick: ((Long) -> Unit)? = null
) {
    val typeColor = MaterialTheme.mosarib.warning

    val chargeableMinutes = WellOwnerPurchaseMath.chargeableMinutes(purchase)
    val totalPayable = WellOwnerPurchaseMath.payableAmount(purchase)
    val wasteCredit = WellOwnerPurchaseMath.ownerWasteCredit(purchase)
    val paidAmount = purchase.amountPaid
    val remainingDebt = (totalPayable - paidAmount).coerceAtLeast(0.0)
    val isSettled = remainingDebt <= 0.0

    val (badgeText, badgeBg, badgeFg) = if (isSettled) {
        Triple("مسدد بالكامل", MaterialTheme.mosarib.success.copy(alpha = 0.12f), MaterialTheme.mosarib.success)
    } else {
        Triple("باقي ${Formatters.formatCurrency(remainingDebt, currencySymbol)}", MaterialTheme.mosarib.debt.copy(alpha = 0.12f), MaterialTheme.mosarib.debt)
    }

    val subtitleText = "${Formatters.formatDateFull(purchase.date)} · ${Formatters.formatTime(purchase.date)}"

    MosaribBottomSheet(
        onDismissRequest = onDismiss,
        title = "شراء ساعات #${purchase.id}",
        subtitle = subtitleText,
        icon = Icons.Default.AccessTime,
        iconTint = typeColor,
        headerBadge = {
            Surface(
                color = badgeBg,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = badgeFg
                    ),
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
                    maxLines = 1
                )
            }
        },
        headerActions = {
            OperationDetailHeaderMenu(
                onPdf = onPdf,
                onDelete = onDelete,
                deleteLabel = "حذف الشراء"
            )
        },
        footer = {
            if (onShare != null) {
                MosaribPrimaryButton(
                    text = "إرسال",
                    onClick = onShare,
                    tone = DialogTone.Success,
                    modifier = Modifier.weight(1f)
                )
            }
            if (onPdf != null) {
                MosaribSecondaryButton(
                    text = "PDF",
                    onClick = onPdf,
                    modifier = Modifier.weight(1f)
                )
            }
            if (onEdit != null) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "تعديل",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 2. كتلة المبلغ وتفقيطه
            OperationDetailAmountBlock(
                amount = totalPayable,
                currencySymbol = currencySymbol,
                typeColor = typeColor
            )

            // 3. كرت قائمة التفاصيل
            OperationDetailCard {
                // صاحب البئر
                OperationDetailRow(
                    icon = Icons.Default.Person,
                    label = "صاحب البئر",
                    value = owner?.name ?: "صاحب بئر #${purchase.ownerCustomerId}",
                    valueColor = MaterialTheme.colorScheme.primary,
                    onClick = if (owner != null && onCustomerClick != null) {
                        { onCustomerClick(owner.id) }
                    } else null
                )

                // الهاتف مع أزرار الاتصال والواتساب
                if (!owner?.phone.isNullOrBlank()) {
                    OperationDetailDivider()
                    OperationDetailRow(
                        icon = Icons.Default.Phone,
                        label = "الهاتف",
                        value = owner!!.phone,
                        trailingActions = {
                            OperationDetailContactActions(phone = owner.phone)
                        }
                    )
                }

                OperationDetailDivider()
                // المدة الإجمالية
                OperationDetailRow(
                    icon = Icons.Default.Schedule,
                    label = "المدة الإجمالية",
                    value = Formatters.formatDurationArabic(purchase.durationMinutes)
                )

                // فترة التشغيل إن وجدت
                if (purchase.startTime > 0 || purchase.endTime > 0) {
                    OperationDetailDivider()
                    val fromStr = if (purchase.startTime > 0) Formatters.formatTime(purchase.startTime) else "—"
                    val toStr = if (purchase.endTime > 0) Formatters.formatTime(purchase.endTime) else "—"
                    OperationDetailRow(
                        icon = Icons.Default.AccessTime,
                        label = "فترة التشغيل",
                        value = "$fromStr إلى $toStr"
                    )
                }

                // الهدر على المالك إن وجد
                if (purchase.wastedMinutesOnOwner > 0) {
                    OperationDetailDivider()
                    OperationDetailRow(
                        icon = Icons.Default.Warning,
                        label = "الهدر على المالك",
                        value = "- ${Formatters.formatDurationArabic(purchase.wastedMinutesOnOwner)} (${Formatters.formatCurrency(wasteCredit, currencySymbol)})",
                        valueColor = MaterialTheme.mosarib.warning
                    )
                }

                OperationDetailDivider()
                // الساعات الصافية المحتسبة
                OperationDetailRow(
                    icon = Icons.Default.Speed,
                    label = "الساعات الصافية المحتسبة",
                    value = Formatters.formatDurationArabic(chargeableMinutes)
                )

                OperationDetailDivider()
                // سعر شراء الساعة
                OperationDetailRow(
                    icon = Icons.Default.AttachMoney,
                    label = "سعر شراء الساعة",
                    value = "${Formatters.formatCurrency(purchase.purchaseRatePerHour, currencySymbol)} / س"
                )

                // المدفوع نقداً إن وجد
                if (paidAmount > 0) {
                    OperationDetailDivider()
                    OperationDetailRow(
                        icon = Icons.Default.Payments,
                        label = "صرف (المدفوع نقداً)",
                        value = Formatters.formatCurrency(paidAmount, currencySymbol),
                        valueColor = MaterialTheme.mosarib.debt
                    )
                }
            }

            // 4. البيان إن وجد
            OperationDetailStatement(
                statement = purchase.notes,
                typeColor = typeColor
            )
        }
    }
}
