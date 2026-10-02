package com.baynana.features.sessions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baynana.core.ui.EmptyStateView
import com.baynana.core.ui.LuxuryToastNotification
import com.baynana.core.ui.SendMessageChoiceDialog
import com.baynana.core.ui.StatBoxCard
import com.baynana.core.util.Formatters
import com.baynana.features.customers.Customer
import com.baynana.features.vouchers.Voucher
import com.baynana.ui.theme.AccentEmerald
import com.baynana.ui.theme.AccentGold
import com.baynana.ui.theme.PrimaryTeal
import com.baynana.ui.theme.SecondaryAqua

@Composable
fun SessionCardItem(
    sessionWithCustomer: WaterSessionWithCustomer,
    currencySymbol: String,
    linkedVouchers: List<Voucher> = emptyList(),
    onCustomerClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onPdfClick: () -> Unit,
    onMessageClick: () -> Unit,
    onSettleClick: (() -> Unit)? = null,
    onShareVoucher: ((Voucher) -> Unit)? = null
) {
    val session = sessionWithCustomer.session
    val customer = sessionWithCustomer.customer
    var menuExpanded by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .shadow(1.5.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // 1. الصف العلوي: معلومات العميل على اليمين (في RTL) و 3 أزرار دائرية/مربعة ناعمة على اليسار
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // العميل والمزرعة
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onCustomerClick() }
                        .weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(PrimaryTeal.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = null,
                            tint = PrimaryTeal,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = customer?.name ?: "عميل غير محدد",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = 1
                        )
                        Text(
                            text = if (!customer?.farmName.isNullOrBlank()) customer?.farmName!! else "جلسة ري",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // الأزرار الثلاثة على اليسار (في RTL: إرسال الرسالة، ثم PDF، ثم خيارات إضافية)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // زر إرسال الفاتورة (واتساب / SMS)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { onMessageClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "إرسال الفاتورة",
                            tint = PrimaryTeal,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // زر فاتورة PDF
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { onPdfClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "فاتورة PDF",
                            tint = PrimaryTeal,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // زر القائمة (المزيد)
                    Box {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable { menuExpanded = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "خيارات",
                                tint = PrimaryTeal,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("إرسال الفاتورة (واتساب / SMS)") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = PrimaryTeal) },
                                onClick = {
                                    menuExpanded = false
                                    onMessageClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("تعديل") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onEditClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("حذف", color = Color(0xFFE53935)) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFE53935)) },
                                onClick = {
                                    menuExpanded = false
                                    onDeleteClick()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. الصف الأوسط: كبسولة كاملة للمدة وسعر الساعة
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // المدة مع أيقونة الساعة (في RTL: أيقونة الساعة أولاً على اليمين ثم النص)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = PrimaryTeal,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = Formatters.formatDurationArabic(session.durationMinutes),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                // سعر الساعة بالعربي (في RTL: على اليسار)
                Text(
                    text = "@ ${Formatters.formatNumber(session.pricePerHour)} $currencySymbol ساعة",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. الجزء السفلي: مقسم لعمودين مع فاصل رأسي
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // العمود الأيمن (في RTL): الإجمالي والمسدد والمتبقي (3 كبسولات عربية)
                Column(
                    modifier = Modifier.weight(1.05f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "الإجمالي",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.padding(bottom = 1.dp)
                    )

                    // كبسولة الإجمالي
                    SessionMetricPill(
                        label = "الإجمالي",
                        value = "${Formatters.formatNumber(session.totalAmount)} $currencySymbol",
                        bgColor = AccentEmerald.copy(alpha = 0.15f),
                        textColor = AccentEmerald
                    )

                    // كبسولة المدفوع
                    SessionMetricPill(
                        label = "المدفوع",
                        value = "${Formatters.formatNumber(session.amountPaid)} $currencySymbol",
                        bgColor = AccentEmerald.copy(alpha = 0.15f),
                        textColor = AccentEmerald
                    )

                    // كبسولة المتبقي
                    if (session.remainingDebt > 0) {
                        SessionMetricPill(
                            label = "المتبقي",
                            value = "${Formatters.formatNumber(session.remainingDebt)} $currencySymbol",
                            bgColor = Color(0xFFE53935).copy(alpha = 0.15f),
                            textColor = Color(0xFFC62828)
                        )
                    } else {
                        SessionMetricPill(
                            label = "المتبقي",
                            value = "0 $currencySymbol (خالص)",
                            bgColor = AccentEmerald.copy(alpha = 0.15f),
                            textColor = AccentEmerald
                        )
                    }
                }

                // فاصل رأسي رفيع
                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                )

                // العمود الأيسر (في RTL): وقت السقي من-إلى والتاريخ + المبلغ المتبقي/الإجمالي بالعريض والمحمر + كتابة المبلغ بالعربي
                Column(
                    modifier = Modifier.weight(0.95f),
                    verticalArrangement = Arrangement.Center
                ) {
                    val startTimeStr = Formatters.formatTime(session.startTime)
                    val endTimeStr = Formatters.formatTime(session.endTime)
                    val dateStr = Formatters.formatDate(session.startTime)

                    Text(
                        text = "فترة السقي والتاريخ",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = "من $startTimeStr إلى $endTimeStr",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = PrimaryTeal,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp
                        ),
                        maxLines = 1
                    )
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.5.sp
                        ),
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    val highlightAmount = if (session.remainingDebt > 0) session.remainingDebt else session.totalAmount
                    val highlightColor = if (session.remainingDebt > 0) Color(0xFFB71C1C) else PrimaryTeal

                    Text(
                        text = "${Formatters.formatNumber(highlightAmount)} $currencySymbol",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = highlightColor,
                            fontSize = 18.sp
                        )
                    )

                    Text(
                        text = Formatters.amountToArabicWords(highlightAmount, currencySymbol),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            lineHeight = 13.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 2
                    )
                }
            }

            if (session.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "ملاحظة: ${session.notes}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // شريط السداد وخاصية توسيع الكرت لعرض تواريخ السداد
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = PrimaryTeal,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (session.remainingDebt == 0.0) "خالص ومسدد بالكامل (عرض التفاصيل)" else "سجل السداد وسندات القبض",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (session.remainingDebt == 0.0) AccentEmerald else PrimaryTeal
                        )
                    )
                }

                if (session.remainingDebt > 0 && onSettleClick != null) {
                    Button(
                        onClick = onSettleClick,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("سداد", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // الدفعة المسددة مقدماً
                    val upfront = Math.max(0.0, session.amountPaid - linkedVouchers.sumOf { it.amount })
                    if (upfront > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(6.dp).clip(CircleShape).background(AccentEmerald)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("دفعة مقدمة (فور التسجيل):", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            }
                            Text(
                                "${Formatters.formatCurrency(upfront, currencySymbol)} • ${Formatters.formatDate(session.startTime)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, color = AccentEmerald)
                            )
                        }
                    }

                    // سندات القبض المرتبطة
                    if (linkedVouchers.isNotEmpty()) {
                        linkedVouchers.forEach { v ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier.size(6.dp).clip(CircleShape).background(PrimaryTeal)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("سند قبض (${v.voucherNumber}):", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        "${Formatters.formatCurrency(v.amount, currencySymbol)} • ${Formatters.formatDate(v.date)}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, color = PrimaryTeal)
                                    )
                                    if (onShareVoucher != null) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(PrimaryTeal.copy(alpha = 0.15f))
                                                .clickable { onShareVoucher(v) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.Send,
                                                contentDescription = "مشاركة السند",
                                                tint = PrimaryTeal,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (session.amountPaid == 0.0) {
                        Text(
                            text = "لم يتم تسجيل أي سداد لهذه الجلسة حتى الآن (مؤخر بالكامل)",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionMetricPill(
    label: String,
    value: String,
    bgColor: Color,
    textColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = textColor,
                fontSize = 11.sp
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = textColor,
                fontSize = 11.sp
            )
        )
    }
}
