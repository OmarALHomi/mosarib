package com.example.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.util.Formatters
import com.example.features.customers.Customer
import com.example.features.vouchers.Voucher
import com.example.features.vouchers.VoucherType
import com.example.ui.theme.mosarib

/**
 * شاشة تفاصيل عائمة موحدة للسندات (قبض / صرف / خصم) متناسقة مع لغة بطاقات العمليات (OperationCard)
 */
@Composable
fun VoucherDetailBottomSheet(
    voucher: Voucher,
    customer: Customer?,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onPdf: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onCustomerClick: ((Long) -> Unit)? = null,
    onSessionClick: ((Long) -> Unit)? = null
) {
    val isReceipt = voucher.type == VoucherType.RECEIPT
    val isDiscount = voucher.type == VoucherType.DISCOUNT

    val typeColor = when {
        isReceipt -> MaterialTheme.mosarib.success
        isDiscount -> MaterialTheme.mosarib.discount
        else -> MaterialTheme.mosarib.debt
    }

    val typeIcon = when {
        isReceipt -> Icons.AutoMirrored.Filled.ReceiptLong
        isDiscount -> Icons.Default.Discount
        else -> Icons.Default.Payments
    }

    val titleText = when {
        isReceipt -> "سند قبض #${voucher.voucherNumber.ifEmpty { voucher.id.toString() }}"
        isDiscount -> "سند خصم وتسوية #${voucher.voucherNumber.ifEmpty { voucher.id.toString() }}"
        else -> "سند صرف #${voucher.voucherNumber.ifEmpty { voucher.id.toString() }}"
    }

    val badgeText = when {
        isReceipt -> "قبض من عميل"
        isDiscount -> "خصم ومسامحة"
        else -> "صرف ونفقات"
    }

    val subtitleText = "${Formatters.formatDateFull(voucher.date)} · ${Formatters.formatTime(voucher.date)}"

    MosaribBottomSheet(
        onDismissRequest = onDismiss,
        title = titleText,
        subtitle = subtitleText,
        icon = typeIcon,
        iconTint = typeColor,
        headerBadge = {
            Surface(
                color = typeColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = typeColor
                    ),
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
                    maxLines = 1
                )
            }
        },
        headerActions = {
            OperationDetailHeaderMenu(
                onPdf = null,
                onDelete = onDelete,
                deleteLabel = "حذف السند"
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
                amount = voucher.amount,
                currencySymbol = currencySymbol,
                typeColor = typeColor
            )

            // 3. كرت قائمة التفاصيل
            OperationDetailCard {
                // الطرف المقابل (عميل / مستفيد)
                val partyLabel = if (isReceipt) "العميل" else "المستفيد"
                val defaultPartyName = if (isReceipt) "عميل نقدي" else "مصروف عام"
                OperationDetailRow(
                    icon = Icons.Default.Person,
                    label = partyLabel,
                    value = customer?.name ?: defaultPartyName,
                    valueColor = MaterialTheme.colorScheme.primary,
                    onClick = if (customer != null && onCustomerClick != null) {
                        { onCustomerClick(customer.id) }
                    } else null
                )

                // الهاتف مع أزرار الاتصال والواتساب
                if (!customer?.phone.isNullOrBlank()) {
                    OperationDetailDivider()
                    OperationDetailRow(
                        icon = Icons.Default.Phone,
                        label = "الهاتف",
                        value = customer!!.phone,
                        trailingActions = {
                            OperationDetailContactActions(phone = customer.phone)
                        }
                    )
                }

                OperationDetailDivider()
                // طريقة الدفع
                OperationDetailRow(
                    icon = Icons.Default.Payments,
                    label = "طريقة الدفع",
                    value = voucher.paymentMethod.ifBlank { "نقداً" }
                )

                // رقم السند إن وجد
                if (voucher.voucherNumber.isNotBlank()) {
                    OperationDetailDivider()
                    OperationDetailRow(
                        icon = Icons.Default.Tag,
                        label = "رقم السند",
                        value = voucher.voucherNumber
                    )
                }

                // الفئة أو البند إن وجد
                if (voucher.category.isNotBlank() && voucher.category != "عام") {
                    OperationDetailDivider()
                    OperationDetailRow(
                        icon = Icons.Default.Category,
                        label = "الفئة",
                        value = voucher.category
                    )
                }

                // الدورة المرتبطة إن وجدت
                if (voucher.sessionId != null && voucher.sessionId > 0) {
                    OperationDetailDivider()
                    OperationDetailRow(
                        icon = Icons.Default.WaterDrop,
                        label = "الدورة المرتبطة",
                        value = "دورة سقي #${voucher.sessionId}",
                        valueColor = MaterialTheme.mosarib.info,
                        onClick = if (onSessionClick != null) {
                            { onSessionClick(voucher.sessionId) }
                        } else null
                    )
                }
            }

            // 4. البيان والملاحظات إن وجدت
            OperationDetailStatement(
                statement = voucher.notes,
                typeColor = typeColor
            )
        }
    }
}
