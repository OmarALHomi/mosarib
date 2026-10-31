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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.Formatters
import com.example.features.customers.Customer
import com.example.features.vouchers.Voucher
import com.example.features.vouchers.VoucherType
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal

/**
 * شاشة تفاصيل عائمة احترافية ومستجيبة للسند (قبض / صرف / خصم)
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
    onShare: (() -> Unit)? = null
) {
    val isReceipt = voucher.type == VoucherType.RECEIPT
    val isDiscount = voucher.type == VoucherType.DISCOUNT
    val badgeColor = when {
        isReceipt -> AccentEmerald
        isDiscount -> AccentGold
        else -> Color(0xFFE53935)
    }

    OperationDetailSheetScaffold(onDismiss = onDismiss) {
        // 1. رأس السند
        OperationDetailHeader(
            icon = when {
                isReceipt -> Icons.Default.ArrowDownward
                isDiscount -> Icons.Default.Discount
                else -> Icons.Default.ArrowUpward
            },
            accentColor = badgeColor,
            iconSize = 20.dp,
            title = when {
                isReceipt -> "سند قبض #${voucher.voucherNumber.ifEmpty { voucher.id.toString() }}"
                isDiscount -> "سند خصم وتسوية #${voucher.voucherNumber.ifEmpty { voucher.id.toString() }}"
                else -> "سند صرف #${voucher.voucherNumber.ifEmpty { voucher.id.toString() }}"
            },
            subtitle = Formatters.formatDateTime(voucher.date),
            badgeText = when {
                isReceipt -> "قبض من عميل"
                isDiscount -> "خصم ومسامحة"
                else -> "صرف ونفقات"
            },
            badgeContainerColor = badgeColor.copy(alpha = 0.14f),
            badgeContentColor = badgeColor
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 2. كرت المبلغ الفخم والتفقيط
        OperationDetailAmountCard(
            label = "مبلغ السند",
            amount = voucher.amount,
            currencySymbol = currencySymbol,
            accentColor = badgeColor
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 3. كرت الطرف المقابل (العميل أو المستفيد)
        val phone = customer?.phone
        OperationDetailPartyCard(
            avatarIcon = Icons.Default.Person,
            avatarTint = PrimaryTeal,
            avatarBackground = PrimaryTeal.copy(alpha = 0.12f),
            name = customer?.name ?: if (isReceipt) "عميل نقدي" else "مصروف عام",
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

        // 4. كرت تفاصيل الدفع والبيان
        VoucherPaymentDetailsCard(voucher = voucher)

        Spacer(modifier = Modifier.height(14.dp))

        // 5. أزرار الإجراءات السفلية (مستجيبة 100% وموزعة على صفين متوازنين)
        OperationDetailActions {
            // صف المشاركة والطباعة (زرين فقط في الصف)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onShare != null) {
                    OperationDetailFilledButton(
                        text = "مشاركة السند",
                        icon = Icons.AutoMirrored.Filled.Send,
                        containerColor = Color(0xFF2E7D32),
                        onClick = onShare,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (onPdf != null) {
                    OperationDetailFilledButton(
                        text = "طباعة PDF",
                        icon = Icons.Default.PictureAsPdf,
                        containerColor = AccentGold,
                        onClick = onPdf,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // صف التعديل والحذف
            OperationDetailEditDeleteRow(
                editLabel = "تعديل السند",
                deleteLabel = "حذف السند",
                editAccentColor = PrimaryTeal,
                onEdit = onEdit,
                onDelete = onDelete
            )
        }
    }
}

@Composable
private fun VoucherPaymentDetailsCard(voucher: Voucher) {
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
                Text(
                    text = "طريقة الدفع",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.5.sp
                    )
                )
                Surface(
                    color = PrimaryTeal.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = voucher.paymentMethod.ifBlank { "نقداً" },
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PrimaryTeal,
                            fontSize = 11.5.sp
                        ),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                    )
                }
            }

            if (voucher.category.isNotBlank() && voucher.category != "عام") {
                OperationDetailInfoRow(
                    label = "التصنيف / البند",
                    value = voucher.category,
                    valueMaxLines = 1,
                    valueOverflow = TextOverflow.Ellipsis
                )
            }

            if (voucher.sessionId != null && voucher.sessionId > 0) {
                OperationDetailInfoRow(
                    label = "سداد لدورة سقي",
                    value = "سقية #${voucher.sessionId}",
                    valueColor = PrimaryTeal
                )
            }

            if (voucher.notes.isNotBlank() && voucher.notes != "عام") {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                Spacer(modifier = Modifier.height(2.dp))
                OperationDetailNotes(
                    notes = voucher.notes,
                    accentColor = PrimaryTeal,
                    iconSize = 18.dp,
                    iconSpacing = 6.dp,
                    labelFontSize = 11.5.sp,
                    labelSpacing = 2.dp,
                    notesFontSize = 13.5.sp,
                    notesLineHeight = 19.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
