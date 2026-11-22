package com.example.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.Formatters
import com.example.features.customers.Customer
import com.example.features.sessions.WaterSession
import com.example.features.vouchers.Voucher
import com.example.features.vouchers.VoucherType
import com.example.ui.theme.mosarib

/**
 * حساب إجمالي عدد الدفعات المرتبطة بدورة السقي (السداد النقدي الفوري + السندات اللاحقة)
 */
fun computeSessionPaymentsCount(session: WaterSession, linkedVouchers: List<Voucher>): Int {
    val vouchersTotal = linkedVouchers.sumOf { it.amount }
    val initialPaid = (session.amountPaid - vouchersTotal).coerceAtLeast(0.0)
    return (if (initialPaid > 0.01) 1 else 0) + linkedVouchers.size
}

/**
 * شاشة تفاصيل عائمة موحدة لدورة السقي متناسقة مع لغة بطاقات العمليات (OperationCard)
 */
@Composable
fun SessionDetailBottomSheet(
    session: WaterSession,
    customer: Customer?,
    currencySymbol: String,
    linkedVouchers: List<Voucher> = emptyList(),
    isWellOwner: Boolean = false,
    sponsorCustomer: Customer? = null,
    onDismiss: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onPdf: (() -> Unit)? = null,
    onMessage: (() -> Unit)? = null,
    onShareVoucher: ((Voucher) -> Unit)? = null,
    onCustomerClick: ((Long) -> Unit)? = null
) {
    val typeColor = MaterialTheme.mosarib.info
    val isSettled = session.remainingDebt <= 0
    val isSettledByOffset = isWellOwner || session.billedToCustomerId != null || sponsorCustomer != null
    val sponsorName = sponsorCustomer?.name ?: "صاحب البئر"

    val (badgeText, badgeBg, badgeFg) = when {
        sponsorCustomer != null -> Triple("مقاصة على: $sponsorName", typeColor.copy(alpha = 0.12f), typeColor)
        isSettledByOffset -> Triple("مقاصة بئر", typeColor.copy(alpha = 0.12f), typeColor)
        isSettled -> Triple("مسدد بالكامل", MaterialTheme.mosarib.success.copy(alpha = 0.12f), MaterialTheme.mosarib.success)
        else -> Triple("باقي ${Formatters.formatCurrency(session.remainingDebt, currencySymbol)}", MaterialTheme.mosarib.debt.copy(alpha = 0.12f), MaterialTheme.mosarib.debt)
    }

    val subtitleText = "${Formatters.formatDateFull(session.startTime)} · ${Formatters.formatTime(session.startTime)}"

    MosaribBottomSheet(
        onDismissRequest = onDismiss,
        title = "دورة سقي #${session.id}",
        subtitle = subtitleText,
        icon = Icons.Default.WaterDrop,
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
                onPdf = if (session.remainingDebt > 0) onPdf else null,
                onDelete = onDelete,
                deleteLabel = "حذف الدورة"
            )
        },
        footer = {
                if (onMessage != null) {
                    MosaribPrimaryButton(
                        text = "إرسال",
                        onClick = onMessage,
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
            // 2. كتلة المبلغ الكبير وتفقيطه وعمودي المدفوع والباقي
            OperationDetailAmountBlock(
                amount = session.totalAmount,
                currencySymbol = currencySymbol,
                typeColor = typeColor,
                extraContent = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "المدفوع",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = Formatters.formatCurrency(session.amountPaid, currencySymbol),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.mosarib.success
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(24.dp)
                                .background(typeColor.copy(alpha = 0.25f))
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "الباقي",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (session.remainingDebt > 0) {
                                    Formatters.formatCurrency(session.remainingDebt, currencySymbol)
                                } else "0 $currencySymbol (خالص)",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (session.remainingDebt > 0) MaterialTheme.mosarib.debt else MaterialTheme.mosarib.success
                                )
                            )
                        }
                    }
                }
            )

            // 3. كرت قائمة التفاصيل الأساسية
            OperationDetailCard {
                // العميل
                OperationDetailRow(
                    icon = Icons.Default.Person,
                    label = "العميل",
                    value = customer?.name ?: "عميل غير محدد",
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
                // المدة الصافية
                OperationDetailRow(
                    icon = Icons.Default.Schedule,
                    label = "المدة الصافية",
                    value = Formatters.formatDurationArabic(session.durationMinutes)
                )

                OperationDetailDivider()
                // من – إلى
                OperationDetailRow(
                    icon = Icons.Default.AccessTime,
                    label = "من – إلى",
                    value = "${Formatters.formatTime(session.startTime)} – ${Formatters.formatTime(session.endTime)}"
                )

                OperationDetailDivider()
                // سعر الساعة
                OperationDetailRow(
                    icon = Icons.Default.Speed,
                    label = "سعر الساعة المعتمد",
                    value = "${Formatters.formatCurrency(session.pricePerHour, currencySymbol)} / س"
                )

                // المضخة (إن وجدت)
                if (session.pumpName.isNotBlank() && session.pumpName != "عام") {
                    OperationDetailDivider()
                    OperationDetailRow(
                        icon = Icons.Default.WaterDrop,
                        label = "المضخة",
                        value = session.pumpName
                    )
                }

                // الهدر (إن وجد)
                if (session.wastedMinutes > 0) {
                    OperationDetailDivider()
                    val wasteText = buildString {
                        append("${session.wastedMinutes} دقيقة")
                        if (session.wastedReason.isNotBlank()) {
                            append(" (${session.wastedReason})")
                        }
                    }
                    OperationDetailRow(
                        icon = Icons.Default.Warning,
                        label = "الوقت المهدور",
                        value = wasteText,
                        valueColor = MaterialTheme.mosarib.warning
                    )
                }

                // الخصم (إن وجد)
                if (session.discountAmount > 0) {
                    OperationDetailDivider()
                    OperationDetailRow(
                        icon = Icons.Default.Discount,
                        label = "الخصم الممنوح",
                        value = Formatters.formatCurrency(session.discountAmount, currencySymbol),
                        valueColor = MaterialTheme.mosarib.discount
                    )
                }

                // محمّل على (إن وجد)
                if (sponsorCustomer != null || session.billedToCustomerId != null) {
                    OperationDetailDivider()
                    OperationDetailRow(
                        icon = Icons.Default.AccountBalance,
                        label = "محمّل على",
                        value = sponsorCustomer?.name ?: "صاحب البئر",
                        valueColor = MaterialTheme.mosarib.info
                    )
                }
            }

            // 4. البيان (الجهة الملاحظات إن وجدت)
            OperationDetailStatement(
                statement = session.notes,
                typeColor = typeColor
            )

            // 5. السندات والدفعات المرتبطة
            val totalPaymentCount = computeSessionPaymentsCount(session, linkedVouchers)
            if (totalPaymentCount > 0) {
                SessionLinkedPaymentsSection(
                    session = session,
                    linkedVouchers = linkedVouchers,
                    currencySymbol = currencySymbol,
                    onShareVoucher = onShareVoucher
                )
            }
        }
    }
}

/**
 * قائمة مصغرة وقابلة للطي للدفعات المرتبطة بالجلسة (سداد · 4,558 · 2/11)
 */
@Composable
private fun SessionLinkedPaymentsSection(
    session: WaterSession,
    linkedVouchers: List<Voucher>,
    currencySymbol: String,
    onShareVoucher: ((Voucher) -> Unit)?
) {
    var expanded by remember { mutableStateOf(true) }
    val vouchersTotal = linkedVouchers.sumOf { it.amount }
    val initialPaid = (session.amountPaid - vouchersTotal).coerceAtLeast(0.0)
    val hasInitialPaid = initialPaid > 0.01
    val totalCount = computeSessionPaymentsCount(session, linkedVouchers)

    OperationDetailCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Payments,
                    contentDescription = null,
                    tint = MaterialTheme.mosarib.success,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "الدفعات المرتبطة ($totalCount)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (hasInitialPaid) {
                    OperationDetailDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "سداد نقدي · ${Formatters.formatCurrency(initialPaid, currencySymbol)} · ${Formatters.formatDateShort(session.startTime)}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.mosarib.credit
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                linkedVouchers.forEach { v ->
                    OperationDetailDivider()
                    val (labelPrefix, labelColor) = when (v.type) {
                        VoucherType.DISCOUNT -> Pair("خصم", MaterialTheme.mosarib.discount)
                        VoucherType.RECEIPT -> Pair("قبض", MaterialTheme.mosarib.credit)
                        VoucherType.EXPENSE -> Pair("صرف", MaterialTheme.mosarib.debt)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$labelPrefix · ${Formatters.formatCurrency(v.amount, currencySymbol)} · ${Formatters.formatDateShort(v.date)}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = labelColor
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (onShareVoucher != null) {
                            IconButton(
                                onClick = { onShareVoucher(v) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "مشاركة السند",
                                    tint = labelColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
