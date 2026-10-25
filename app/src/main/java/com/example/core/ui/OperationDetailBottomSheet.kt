package com.example.core.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.Formatters
import com.example.features.customers.Customer
import com.example.features.sessions.WaterSession
import com.example.features.vouchers.Voucher
import com.example.features.vouchers.VoucherType
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal

/**
 * شاشة تفاصيل عائمة احترافية ومستجيبة لدورة السقي
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailBottomSheet(
    session: WaterSession,
    customer: Customer?,
    currencySymbol: String,
    linkedVouchers: List<Voucher> = emptyList(),
    isWellOwner: Boolean = false,
    onDismiss: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onPdf: (() -> Unit)? = null,
    onMessage: (() -> Unit)? = null,
    onSettle: (() -> Unit)? = null,
    onShareVoucher: ((Voucher) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val isSettled = session.remainingDebt <= 0
    val isSettledByOffset = isWellOwner || session.billedToCustomerId != null

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. رأس الشاشة
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
                                .background(PrimaryTeal.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = null,
                                tint = PrimaryTeal,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "تفاصيل دورة السقي #${session.id}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 16.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = Formatters.formatDateFull(session.startTime),
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
                        color = when {
                            isSettledByOffset -> Color(0xFFE0F2FE)
                            isSettled -> AccentEmerald.copy(alpha = 0.15f)
                            else -> Color(0xFFE53935).copy(alpha = 0.15f)
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = when {
                                isSettledByOffset -> "مقاصة بئر 🔄"
                                isSettled -> "مسدد بالكامل ✅"
                                else -> "متبقي دين ⏳"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isSettledByOffset -> Color(0xFF0369A1)
                                    isSettled -> AccentEmerald
                                    else -> Color(0xFFE53935)
                                },
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. كرت العميل
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
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
                                    .background(if (customer?.isWellOwner == true) Color(0xFFE1F5FE) else PrimaryTeal.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (customer?.isWellOwner == true) Icons.Default.WaterDrop else Icons.Default.Person,
                                    contentDescription = null,
                                    tint = if (customer?.isWellOwner == true) Color(0xFF0288D1) else PrimaryTeal,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(
                                    text = customer?.name ?: "عميل غير محدد",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!customer?.phone.isNullOrBlank()) {
                                    Text(
                                        text = customer!!.phone,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.5.sp
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        // أزرار اتصال وواتساب سريع للعميل (أو شارة هوية إن لم يوجد رقم)
                        if (!customer?.phone.isNullOrBlank()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilledTonalIconButton(
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer!!.phone}"))
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
                                            val cleanPhone = customer!!.phone.replace("+", "").replace(" ", "").trim()
                                            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone")
                                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                        } catch (_: Exception) {}
                                    },
                                    modifier = Modifier.size(34.dp),
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = Color(0xFF2E7D32).copy(alpha = 0.15f),
                                        contentColor = Color(0xFF2E7D32)
                                    )
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "واتساب", modifier = Modifier.size(16.dp))
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = PrimaryTeal.copy(alpha = 0.08f)
                            ) {
                                Text(
                                    text = if (customer?.isWellOwner == true) "صاحب بئر" else "حساب عميل",
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
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 3. كرت الوقت والمدة
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
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

                Spacer(modifier = Modifier.height(8.dp))

                // 4. كرت الحسابات المالية وسعر الساعة المعتمد
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "إجمالي المبلغ المطلوب",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.5.sp
                                ),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = Formatters.formatCurrency(session.totalAmount, currencySymbol),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 17.sp
                                ),
                                maxLines = 1,
                                softWrap = false
                            )
                        }

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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "المبلغ المدفوع (المسدد)",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.5.sp
                                ),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = Formatters.formatCurrency(session.amountPaid, currencySymbol),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = AccentEmerald,
                                    fontSize = 13.5.sp
                                ),
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        // المتبقي (الدين)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "المبلغ المتبقي (الدين)",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (session.remainingDebt > 0) Color(0xFFE53935) else AccentEmerald,
                                    fontSize = 12.5.sp
                                ),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (session.remainingDebt > 0) {
                                    Formatters.formatCurrency(session.remainingDebt, currencySymbol)
                                } else "0 $currencySymbol (خالص)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (session.remainingDebt > 0) Color(0xFFE53935) else AccentEmerald,
                                    fontSize = 14.5.sp
                                ),
                                maxLines = 1,
                                softWrap = false
                            )
                        }

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

                // 5. السندات والدفعات المرتبطة
                if (linkedVouchers.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
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
                                        text = "سند #${v.voucherNumber.ifEmpty { v.id.toString() }} • ${v.paymentMethod}",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 11.5.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = Formatters.formatDateTime(v.date),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp
                                        ),
                                        maxLines = 1
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = Formatters.formatCurrency(v.amount, currencySymbol),
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
                                            onClick = { onShareVoucher(v) },
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
                }

                // 6. البيان والملاحظات إن وجدت (بخط واضح وبارز جداً)
                if (session.notes.isNotBlank() && session.notes != "عام") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = PrimaryTeal.copy(alpha = 0.08f)
                        ),
                        border = BorderStroke(1.dp, PrimaryTeal.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Notes,
                                contentDescription = null,
                                tint = PrimaryTeal,
                                modifier = Modifier.size(20.dp).padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "البيان / الملاحظات:",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryTeal,
                                        fontSize = 12.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = session.notes,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 13.5.sp,
                                        lineHeight = 20.sp
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 7. أزرار الإجراءات السفلية (توزيع ذكي ومستجيب 100% يمنع انكسار النصوص نهائياً)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                                text = "تسجيل سداد (${Formatters.formatCurrency(session.remainingDebt, currencySymbol)})",
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
                            Button(
                                onClick = onMessage,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إرسال فاتورة", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, maxLines = 1)
                            }
                        }

                        if (onPdf != null) {
                            Button(
                                onClick = onPdf,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("فاتورة PDF", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, maxLines = 1)
                            }
                        }
                    }

                    // صف التعديل والحذف
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
                                    border = BorderStroke(1.dp, PrimaryTeal.copy(alpha = 0.5f)),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryTeal)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("تعديل الدورة", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }

                            if (onDelete != null) {
                                OutlinedButton(
                                    onClick = onDelete,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE53935)),
                                    border = BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.5f)),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFE53935))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("حذف الدورة", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, color = Color(0xFFE53935))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * شاشة تفاصيل عائمة احترافية ومستجيبة للسند (قبض / صرف / خصم)
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val isReceipt = voucher.type == VoucherType.RECEIPT
    val isDiscount = voucher.type == VoucherType.DISCOUNT
    val badgeColor = when {
        isReceipt -> AccentEmerald
        isDiscount -> AccentGold
        else -> Color(0xFFE53935)
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. رأس السند
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
                                .background(badgeColor.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when {
                                    isReceipt -> Icons.Default.ArrowDownward
                                    isDiscount -> Icons.Default.Discount
                                    else -> Icons.Default.ArrowUpward
                                },
                                contentDescription = null,
                                tint = badgeColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = when {
                                    isReceipt -> "سند قبض #${voucher.voucherNumber.ifEmpty { voucher.id.toString() }}"
                                    isDiscount -> "سند خصم وتسوية #${voucher.voucherNumber.ifEmpty { voucher.id.toString() }}"
                                    else -> "سند صرف #${voucher.voucherNumber.ifEmpty { voucher.id.toString() }}"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 16.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = Formatters.formatDateTime(voucher.date),
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

                    Surface(
                        color = badgeColor.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = when {
                                isReceipt -> "قبض من عميل"
                                isDiscount -> "خصم ومسامحة"
                                else -> "صرف ونفقات"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = badgeColor,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. كرت المبلغ الفخم والتفقيط
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = badgeColor.copy(alpha = 0.08f)
                    ),
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "مبلغ السند",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = Formatters.formatCurrency(voucher.amount, currencySymbol),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = badgeColor,
                                fontSize = 26.sp
                            ),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = Formatters.amountToArabicWords(voucher.amount, currencySymbol),
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

                Spacer(modifier = Modifier.height(8.dp))

                // 3. كرت الطرف المقابل (العميل أو المستفيد)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
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
                                    .background(PrimaryTeal.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(19.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(
                                    text = customer?.name ?: if (isReceipt) "عميل نقدي" else "مصروف عام",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 13.5.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!customer?.phone.isNullOrBlank()) {
                                    Text(
                                        text = customer!!.phone,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        if (!customer?.phone.isNullOrBlank()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilledTonalIconButton(
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer!!.phone}"))
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
                                            val cleanPhone = customer!!.phone.replace("+", "").replace(" ", "").trim()
                                            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone")
                                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                        } catch (_: Exception) {}
                                    },
                                    modifier = Modifier.size(34.dp),
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = Color(0xFF2E7D32).copy(alpha = 0.15f),
                                        contentColor = Color(0xFF2E7D32)
                                    )
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "واتساب", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 4. كرت تفاصيل الدفع والبيان
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "التصنيف / البند",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.5.sp
                                    )
                                )
                                Text(
                                    text = voucher.category,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 12.5.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (voucher.sessionId != null && voucher.sessionId > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "سداد لدورة سقي",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.5.sp
                                    )
                                )
                                Text(
                                    text = "سقية #${voucher.sessionId}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryTeal,
                                        fontSize = 12.5.sp
                                    )
                                )
                            }
                        }

                        if (voucher.notes.isNotBlank() && voucher.notes != "عام") {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Notes,
                                    contentDescription = null,
                                    tint = PrimaryTeal,
                                    modifier = Modifier.size(18.dp).padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "البيان / الملاحظات:",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryTeal,
                                            fontSize = 11.5.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = voucher.notes,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 13.5.sp,
                                            lineHeight = 19.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 5. أزرار الإجراءات السفلية (مستجيبة 100% وموزعة على صفين متوازنين)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // صف المشاركة والطباعة (زرين فقط في الصف)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (onShare != null) {
                            Button(
                                onClick = onShare,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("مشاركة السند", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, maxLines = 1)
                            }
                        }

                        if (onPdf != null) {
                            Button(
                                onClick = onPdf,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("طباعة PDF", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, maxLines = 1)
                            }
                        }
                    }

                    // صف التعديل والحذف
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
                                    border = BorderStroke(1.dp, PrimaryTeal.copy(alpha = 0.5f)),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryTeal)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("تعديل السند", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }

                            if (onDelete != null) {
                                OutlinedButton(
                                    onClick = onDelete,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE53935)),
                                    border = BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.5f)),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFE53935))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("حذف السند", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, color = Color(0xFFE53935))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
