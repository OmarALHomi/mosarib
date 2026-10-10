package com.example.features.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.AppTab
import com.example.core.ui.LuxuryToastNotification
import com.example.core.util.Formatters
import com.example.features.customers.AddEditCustomerBottomSheet
import com.example.features.customers.Customer
import com.example.features.customers.CustomersViewModel
import com.example.features.sessions.AddEditSessionBottomSheet
import com.example.features.sessions.SessionsViewModel
import com.example.features.sessions.WaterSessionWithCustomer
import com.example.features.settings.SettingsViewModel
import com.example.features.vouchers.AddVoucherBottomSheet
import com.example.features.vouchers.VoucherType
import com.example.features.vouchers.VouchersViewModel
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryAqua
import java.util.Calendar

/**
 * زر صغير نابض بالحياة لتغيير الثيم مع هالة ضوئية تفاعلية
 */
@Composable
fun LivingThemeToggleButton(
    isDarkTheme: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_theme")
    val haloScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_scale"
    )
    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_alpha"
    )

    val rotation by animateFloatAsState(
        targetValue = if (isDarkTheme) 360f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "rotation"
    )

    Box(
        modifier = modifier.size(46.dp),
        contentAlignment = Alignment.Center
    ) {
        // Living breathing halo ring
        Box(
            modifier = Modifier
                .size(40.dp)
                .scale(haloScale)
                .clip(CircleShape)
                .background(
                    if (isDarkTheme) AccentGold.copy(alpha = haloAlpha)
                    else PrimaryTeal.copy(alpha = haloAlpha)
                )
        )

        // Main button pill
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(
                    if (isDarkTheme) Color(0xFF1E293B)
                    else Color(0xFFE0F2F1)
                )
                .clickable { onToggle() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = "تبديل المظهر",
                tint = if (isDarkTheme) AccentGold else PrimaryTeal,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer(rotationZ = rotation)
            )
        }
    }
}

/**
 * بطاقة إجراء سريع مدمجة وأنيقة
 */
@Composable
fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(1.5.dp, RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp
                    ),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * رسم بياني بالأعمدة لتوزيع ساعات الري الأسبوعية (Weekly Irrigation Hours Bar Chart)
 */
@Composable
fun WeeklyIrrigationBarChartCard(
    sessions: List<WaterSessionWithCustomer>,
    modifier: Modifier = Modifier
) {
    // حساب ساعات الري لآخر 7 أيام
    val daysStats = remember(sessions) {
        val now = Calendar.getInstance()
        (6 downTo 0).map { daysAgo ->
            val cal = Calendar.getInstance().apply {
                timeInMillis = now.timeInMillis
                add(Calendar.DAY_OF_YEAR, -daysAgo)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = cal.timeInMillis
            val endOfDay = startOfDay + 86400000L - 1L

            val dayMinutes = sessions.filter {
                it.session.startTime in startOfDay..endOfDay
            }.sumOf { it.session.durationMinutes }

            val dayName = when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SATURDAY -> "السبت"
                Calendar.SUNDAY -> "الأحد"
                Calendar.MONDAY -> "الاثنين"
                Calendar.TUESDAY -> "الثلاثاء"
                Calendar.WEDNESDAY -> "الأربعاء"
                Calendar.THURSDAY -> "الخميس"
                Calendar.FRIDAY -> "الجمعة"
                else -> ""
            }

            DayWaterStat(
                dayName = dayName,
                dayDateText = "${cal.get(Calendar.DAY_OF_MONTH)}/${cal.get(Calendar.MONTH) + 1}",
                totalMinutes = dayMinutes,
                isToday = daysAgo == 0
            )
        }
    }

    val maxMinutes = (daysStats.maxOfOrNull { it.totalMinutes } ?: 0).coerceAtLeast(60)
    val totalWeekMinutes = daysStats.sumOf { it.totalMinutes }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .shadow(1.5.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "إجمالي السقي: ${Formatters.formatDurationArabic(totalWeekMinutes)}",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = PrimaryTeal,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.5.sp
                    )
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "آخر 7 أيام",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bars Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                daysStats.forEach { stat ->
                    val fraction = (stat.totalMinutes.toFloat() / maxMinutes.toFloat()).coerceIn(0.04f, 1f)
                    val hoursText = if (stat.totalMinutes == 0) "0" else String.format(java.util.Locale.US, "%.1f", stat.totalMinutes / 60.0)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "$hoursText س",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = if (stat.isToday) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (stat.isToday) AccentEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // Animated Bar with safe max height
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height((65 * fraction).dp.coerceAtLeast(6.dp))
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    if (stat.isToday) Brush.verticalGradient(listOf(AccentEmerald, PrimaryTeal))
                                    else if (stat.totalMinutes > 0) Brush.verticalGradient(listOf(PrimaryTeal.copy(alpha = 0.85f), SecondaryAqua))
                                    else Brush.verticalGradient(listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1)))
                                )
                        )

                        Spacer(modifier = Modifier.height(5.dp))

                        Text(
                            text = stat.dayName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.5.sp,
                                fontWeight = if (stat.isToday) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (stat.isToday) PrimaryTeal else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            maxLines = 1
                        )

                        if (stat.isToday) {
                            Surface(
                                color = AccentEmerald.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = "اليوم",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentEmerald
                                    ),
                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.height(13.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * مؤشر تحصيل الديون والتدفق المالي (Debt Recovery & Cash Flow Progress Card)
 */
@Composable
fun FinancialRecoveryGaugeCard(
    homeStats: com.example.features.sessions.HomeDashboardStats,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val totalTurnover = homeStats.totalCollectedCash + homeStats.totalOutstandingDebt
    val recoveryRatio = if (totalTurnover > 0) (homeStats.totalCollectedCash / totalTurnover).toFloat().coerceIn(0f, 1f) else 1f
    val percentInt = (recoveryRatio * 100).toInt()

    val totalFlow = (homeStats.totalCollectedCash + homeStats.totalExpenses).coerceAtLeast(1.0)
    val receiptsRatio = (homeStats.totalCollectedCash / totalFlow).toFloat().coerceIn(0.02f, 0.98f)
    val netProfit = homeStats.totalCollectedCash - homeStats.totalExpenses

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .shadow(1.5.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // 1. مؤشر تحصيل الديون
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "مؤشر تحصيل الديون",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp
                        )
                    )
                    Text(
                        text = "نسبة الدفعات المستلمة من إجمالي الديون والمستحقات",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (percentInt >= 70) Color(0xFFE8F5E9) else Color(0xFFFFF3E0))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$percentInt% محصل",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (percentInt >= 70) Color(0xFF2E7D32) else Color(0xFFE65100),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Debt Recovery Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(11.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(recoveryRatio)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(AccentEmerald, PrimaryTeal)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2 Stat Columns: Collected vs Debts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "المقبوض كاش: ${Formatters.formatCurrency(homeStats.totalCollectedCash, currencySymbol)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = AccentEmerald,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp
                        )
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "الديون القائمة: ${Formatters.formatCurrency(homeStats.totalOutstandingDebt, currencySymbol)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (homeStats.totalOutstandingDebt > 0) Color(0xFFD32F2F) else AccentEmerald,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                thickness = 0.8.dp
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 2. بروجرس التدفق المالي: المقبوضات والمصروفات
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "التدفق المالي (المقبوضات والمصروفات)",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.5.sp
                        )
                    )
                    Text(
                        text = "مقارنة السيولة المحصلة بالنفقات التشغيلية",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    )
                }

                val profitBadgeText = if (netProfit >= 0) "+ " + Formatters.formatCurrency(netProfit, currencySymbol)
                else "- " + Formatters.formatCurrency(kotlin.math.abs(netProfit), currencySymbol)

                Surface(
                    color = if (netProfit >= 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "الصافي: $profitBadgeText",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (netProfit >= 0) Color(0xFF2E7D32) else Color(0xFFD32F2F),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.5.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Two-tone Cash Flow Progress Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(11.dp)
                    .clip(RoundedCornerShape(6.dp))
            ) {
                // Receipts share (Green/Teal)
                Box(
                    modifier = Modifier
                        .weight(receiptsRatio.coerceAtLeast(0.01f))
                        .fillMaxHeight()
                        .background(AccentEmerald)
                )
                // Expenses share (Red)
                Box(
                    modifier = Modifier
                        .weight((1f - receiptsRatio).coerceAtLeast(0.01f))
                        .fillMaxHeight()
                        .background(Color(0xFFE53935))
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(AccentEmerald))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "مقبوض: ${Formatters.formatCurrency(homeStats.totalCollectedCash, currencySymbol)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.5.sp
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFFE53935)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "مصروف: ${Formatters.formatCurrency(homeStats.totalExpenses, currencySymbol)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.5.sp
                        )
                    )
                }
            }
        }
    }
}

/**
 * بطاقة معاينة مدمجة وسريعة لأحدث عمليات السقي
 */
@Composable
fun RecentSessionMiniCard(
    sessionWithCustomer: WaterSessionWithCustomer,
    currencySymbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = sessionWithCustomer.session
    val c = sessionWithCustomer.customer

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .shadow(1.dp, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(PrimaryTeal.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = null,
                        tint = PrimaryTeal,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = c?.name ?: "عميل غير محدد",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp
                        )
                    )
                    val startTimeStr = Formatters.formatTime(s.startTime)
                    val endTimeStr = Formatters.formatTime(s.endTime)
                    val dateStr = Formatters.formatDate(s.startTime)
                    Text(
                        text = "من $startTimeStr إلى $endTimeStr  •  $dateStr (${Formatters.formatDurationArabic(s.durationMinutes)})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Formatters.formatCurrency(s.totalAmount, currencySymbol),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryTeal,
                        fontSize = 13.sp
                    )
                )
                Text(
                    text = if (s.remainingDebt > 0) "متبقي آجل" else "مسدد بالكامل",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (s.remainingDebt > 0) Color(0xFFC62828) else Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.5.sp
                    )
                )
            }
        }
    }
}
