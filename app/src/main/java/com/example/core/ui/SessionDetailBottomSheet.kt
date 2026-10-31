package com.example.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.Formatters
import com.example.features.customers.Customer
import com.example.features.sessions.WaterSession
import com.example.features.vouchers.Voucher
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal

/**
 * شاشة تفاصيل عائمة احترافية ومستجيبة لدورة السقي
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
    onSettle: (() -> Unit)? = null,
    onShareVoucher: ((Voucher) -> Unit)? = null
) {
    val isSettled = session.remainingDebt <= 0
    val isSettledByOffset = isWellOwner || session.billedToCustomerId != null || sponsorCustomer != null

    OperationDetailSheetScaffold(onDismiss = onDismiss) {
        // 1. رأس الشاشة
        val sponsorName = sponsorCustomer?.name ?: "صاحب البئر"
        OperationDetailHeader(
            icon = Icons.Default.WaterDrop,
            accentColor = PrimaryTeal,
            iconSize = 22.dp,
            title = "تفاصيل دورة السقي #${session.id}",
            subtitle = Formatters.formatDateFull(session.startTime),
            badgeText = when {
                sponsorCustomer != null -> "مقاصة على: $sponsorName 🔄"
                isSettledByOffset -> "مقاصة بئر 🔄"
                isSettled -> "مسدد بالكامل ✅"
                else -> "متبقي دين ⏳"
            },
            badgeContainerColor = when {
                sponsorCustomer != null || isSettledByOffset -> Color(0xFFE0F2FE)
                isSettled -> AccentEmerald.copy(alpha = 0.15f)
                else -> Color(0xFFE53935).copy(alpha = 0.15f)
            },
            badgeContentColor = when {
                sponsorCustomer != null || isSettledByOffset -> Color(0xFF0369A1)
                isSettled -> AccentEmerald
                else -> Color(0xFFE53935)
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 2. كرت العميل
        SessionCustomerCard(customer = customer, sponsorCustomer = sponsorCustomer)

        Spacer(modifier = Modifier.height(8.dp))

        // 3. كرت الوقت والمدة
        SessionTimeCard(session = session)

        Spacer(modifier = Modifier.height(8.dp))

        // 4. كرت الحسابات المالية وسعر الساعة المعتمد
        SessionFinanceCard(session = session, currencySymbol = currencySymbol)

        // 5. السندات والدفعات المرتبطة
        if (linkedVouchers.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            SessionLinkedVouchers(
                linkedVouchers = linkedVouchers,
                currencySymbol = currencySymbol,
                onShareVoucher = onShareVoucher
            )
        }

        // 6. البيان والملاحظات إن وجدت (بخط واضح وبارز جداً)
        if (session.notes.isNotBlank() && session.notes != "عام") {
            Spacer(modifier = Modifier.height(8.dp))
            SessionNotesCard(notes = session.notes)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 7. أزرار الإجراءات السفلية (توزيع ذكي ومستجيب 100% يمنع انكسار النصوص نهائياً)
        SessionActions(
            session = session,
            currencySymbol = currencySymbol,
            onSettle = onSettle,
            onMessage = onMessage,
            onPdf = onPdf,
            onEdit = onEdit,
            onDelete = onDelete
        )
    }
}

@Composable
private fun SessionCustomerCard(customer: Customer?, sponsorCustomer: Customer?) {
    val isWellOwnerCustomer = customer?.isWellOwner == true
    val phone = customer?.phone

    OperationDetailPartyCard(
        avatarIcon = if (isWellOwnerCustomer) Icons.Default.WaterDrop else Icons.Default.Person,
        avatarTint = if (isWellOwnerCustomer) Color(0xFF0288D1) else PrimaryTeal,
        avatarBackground = if (isWellOwnerCustomer) Color(0xFFE1F5FE) else PrimaryTeal.copy(alpha = 0.12f),
        name = customer?.name ?: "عميل غير محدد",
        nameFontSize = 14.sp,
        subtitle = {
            if (sponsorCustomer != null) {
                Text(
                    text = "على حساب: ${sponsorCustomer.name}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF0369A1),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp
                    ),
                    maxLines = 1
                )
            } else if (!phone.isNullOrBlank()) {
                OperationDetailPhoneText(phone = phone, fontSize = 11.5.sp)
            }
        },
        trailing = {
            // أزرار اتصال وواتساب سريع للعميل (أو شارة هوية إن لم يوجد رقم)
            if (!phone.isNullOrBlank()) {
                OperationDetailContactActions(phone = phone)
            } else {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = PrimaryTeal.copy(alpha = 0.08f)
                ) {
                    Text(
                        text = if (isWellOwnerCustomer) "صاحب بئر" else "حساب عميل",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = PrimaryTeal,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.5.sp
                        ),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }
        }
    )
}

@Composable
private fun SessionTimeCard(session: WaterSession) {
    OperationDetailCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "مدة السقي الصافية",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        ),
                        maxLines = 1
                    )
                }
                Text(
                    text = Formatters.formatDurationArabic(session.durationMinutes),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryTeal,
                        fontSize = 13.5.sp
                    ),
                    maxLines = 1
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "من: ${Formatters.formatTime(session.startTime)}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp
                    ),
                    maxLines = 1
                )
                Text(
                    text = "إلى: ${Formatters.formatTime(session.endTime)}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp
                    ),
                    maxLines = 1
                )
            }

            if (session.wastedMinutes > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(AccentGold.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "التوقف: ${session.wastedMinutes} دقيقة",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB45309)
                        ),
                        maxLines = 1
                    )
                    if (session.wastedReason.isNotBlank()) {
                        Text(
                            text = session.wastedReason,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFB45309)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionFinanceCard(session: WaterSession, currencySymbol: String) {
    val debtColor = if (session.remainingDebt > 0) Color(0xFFE53935) else AccentEmerald

    OperationDetailCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // سعر الساعة المعتمد البارز
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(PrimaryTeal.copy(alpha = 0.09f))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "سعر الساعة المعتمد",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PrimaryTeal,
                            fontSize = 12.5.sp
                        ),
                        maxLines = 1
                    )
                }
                Text(
                    text = "${Formatters.formatCurrency(session.pricePerHour, currencySymbol)} / س",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryTeal,
                        fontSize = 13.5.sp
                    ),
                    maxLines = 1
                )
            }

            // إجمالي المبلغ المطلوب
            SessionAmountRow(
                label = "إجمالي المبلغ المطلوب",
                labelStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.5.sp
                ),
                amount = Formatters.formatCurrency(session.totalAmount, currencySymbol),
                amountStyle = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 17.sp
                )
            )

            // تفقيط المبلغ بالعربية
            Text(
                text = Formatters.amountToArabicWords(session.totalAmount, currencySymbol),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp
                )
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

            // المدفوع
            SessionAmountRow(
                label = "المبلغ المدفوع (المسدد)",
                labelStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.5.sp
                ),
                amount = Formatters.formatCurrency(session.amountPaid, currencySymbol),
                amountStyle = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = AccentEmerald,
                    fontSize = 13.5.sp
                )
            )

            // المتبقي (الدين)
            SessionAmountRow(
                label = "المبلغ المتبقي (الدين)",
                labelStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = debtColor,
                    fontSize = 12.5.sp
                ),
                amount = if (session.remainingDebt > 0) {
                    Formatters.formatCurrency(session.remainingDebt, currencySymbol)
                } else "0 $currencySymbol (خالص)",
                amountStyle = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = debtColor,
                    fontSize = 14.5.sp
                )
            )

            if (session.discountAmount > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "مبلغ الخصم الممنوح",
                        style = MaterialTheme.typography.bodySmall.copy(color = AccentGold, fontSize = 11.5.sp),
                        maxLines = 1
                    )
                    Text(
                        text = Formatters.formatCurrency(session.discountAmount, currencySymbol),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = AccentGold,
                            fontSize = 12.sp
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * صف مبلغ مالي لا ينكسر: العنوان يتقلص بالنقاط والمبلغ يبقى كاملاً
 */
@Composable
private fun SessionAmountRow(
    label: String,
    labelStyle: TextStyle,
    amount: String,
    amountStyle: TextStyle
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = labelStyle,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = amount,
            style = amountStyle,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun SessionLinkedVouchers(
    linkedVouchers: List<Voucher>,
    currencySymbol: String,
    onShareVoucher: ((Voucher) -> Unit)?
) {
    Text(
        text = "السندات والدفعات المرتبطة (${linkedVouchers.size}):",
        style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.5.sp
        )
    )
    Spacer(modifier = Modifier.height(4.dp))
    linkedVouchers.forEach { v ->
        SessionLinkedVoucherItem(voucher = v, currencySymbol = currencySymbol, onShareVoucher = onShareVoucher)
    }
}

@Composable
private fun SessionLinkedVoucherItem(
    voucher: Voucher,
    currencySymbol: String,
    onShareVoucher: ((Voucher) -> Unit)?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = "سند #${voucher.voucherNumber.ifEmpty { voucher.id.toString() }} • ${voucher.paymentMethod}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.5.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = Formatters.formatDateTime(voucher.date),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    ),
                    maxLines = 1
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = Formatters.formatCurrency(voucher.amount, currencySymbol),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = AccentEmerald,
                        fontSize = 12.5.sp
                    ),
                    maxLines = 1
                )
                if (onShareVoucher != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { onShareVoucher(voucher) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "مشاركة",
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionNotesCard(notes: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = PrimaryTeal.copy(alpha = 0.08f)
        ),
        border = BorderStroke(1.dp, PrimaryTeal.copy(alpha = 0.25f))
    ) {
        OperationDetailNotes(
            notes = notes,
            accentColor = PrimaryTeal,
            iconSize = 20.dp,
            iconSpacing = 8.dp,
            labelFontSize = 12.sp,
            labelSpacing = 3.dp,
            notesFontSize = 13.5.sp,
            notesLineHeight = 20.sp,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
private fun SessionActions(
    session: WaterSession,
    currencySymbol: String,
    onSettle: (() -> Unit)?,
    onMessage: (() -> Unit)?,
    onPdf: (() -> Unit)?,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    OperationDetailActions {
        // إذا كان هناك متبقي دين: زر سداد رئيسي بارز وواضح جداً بعرض كامل
        if (session.remainingDebt > 0 && onSettle != null) {
            Button(
                onClick = onSettle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp)
            ) {
                Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(19.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "سداد (${Formatters.formatCurrency(session.remainingDebt, currencySymbol)})",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    maxLines = 1
                )
            }
        }

        // صف المشاركة والطباعة (زرين فقط بعرض متوازن لا ينكسر في أي شاشة)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (onMessage != null) {
                OperationDetailFilledButton(
                    text = "إرسال فاتورة",
                    icon = Icons.AutoMirrored.Filled.Send,
                    containerColor = Color(0xFF2E7D32),
                    onClick = onMessage,
                    modifier = Modifier.weight(1f)
                )
            }

            if (onPdf != null) {
                OperationDetailFilledButton(
                    text = "فاتورة PDF",
                    icon = Icons.Default.PictureAsPdf,
                    containerColor = AccentGold,
                    onClick = onPdf,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // صف التعديل والحذف
        OperationDetailEditDeleteRow(
            editLabel = "تعديل الدورة",
            deleteLabel = "حذف الدورة",
            editAccentColor = PrimaryTeal,
            onEdit = onEdit,
            onDelete = onDelete
        )
    }
}
