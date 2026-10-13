package com.example.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import android.widget.Toast
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.PrimaryTeal
import java.util.Calendar

/**
 * نافذة حوار مخصصة وفاخرة لاختيار الوقت (بدون استخدام نوافذ النظام القديمة)
 * - دعم كامل ودقيق وتلقائي للوضعين الفاتح والمظلم (Dark & Light Theme)
 * - اختيار الساعات (1..12)، الدقائق (00..59)، والصباح/المساء
 * - أزرار سريعة للرفع والخفض واختيار الدقائق الشائعة (00, 15, 30, 45)
 */
@Composable
fun MosaribTimePickerDialog(
    initialHour24: Int,
    initialMinute: Int,
    title: String = "تحديد الوقت",
    onDismiss: () -> Unit,
    onTimeSelected: (hour24: Int, minute: Int) -> Unit
) {
    val isDark = isAppDark()

    // تحويل الوقت الأولي إلى نظام 12 ساعة
    val initIsPm = initialHour24 >= 12
    val initHour12 = when {
        initialHour24 == 0 -> 12
        initialHour24 > 12 -> initialHour24 - 12
        else -> initialHour24
    }

    var selectedHour12 by remember { mutableIntStateOf(initHour12) }
    var selectedMinute by remember { mutableIntStateOf(initialMinute.coerceIn(0, 59)) }
    var isPm by remember { mutableStateOf(initIsPm) }

    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenWidthDp < 360

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Card(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth(if (isSmallScreen) 0.96f else 0.90f)
                    .padding(horizontal = if (isSmallScreen) 4.dp else 8.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF141F25) else Color.White
                ),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF2A3C45) else Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(if (isSmallScreen) 12.dp else 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // شريط العنوان العلوي
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isSmallScreen) 32.dp else 38.dp)
                                .clip(CircleShape)
                                .background(PrimaryTeal.copy(alpha = if (isDark) 0.22f else 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = PrimaryTeal,
                                modifier = Modifier.size(if (isSmallScreen) 16.dp else 20.dp)
                            )
                        }
                        Text(
                            text = title,
                            style = (if (isSmallScreen) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium).copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(if (isSmallScreen) 12.dp else 18.dp))

                    // عرض الوقت المختار بخط عريض وفخم (صباح/مساء على اليمين، ثم الساعة، ثم الدقائق على اليسار)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) Color(0xFF0D161B) else Color(0xFFF1F5F9))
                            .border(1.dp, if (isDark) Color(0xFF23353E) else Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                            .padding(vertical = if (isSmallScreen) 8.dp else 12.dp, horizontal = if (isSmallScreen) 8.dp else 16.dp)
                    ) {
                        Text(
                            text = if (isPm) "مساءً (م)" else "صباحاً (ص)",
                            fontSize = if (isSmallScreen) 14.sp else 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isPm) {
                                if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
                            } else {
                                if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                            }
                        )
                        Spacer(modifier = Modifier.width(if (isSmallScreen) 8.dp else 14.dp))
                        Text(
                            text = String.format("%02d", selectedHour12),
                            fontSize = if (isSmallScreen) 28.sp else 38.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PrimaryTeal
                        )
                        Text(
                            text = ":",
                            fontSize = if (isSmallScreen) 24.sp else 34.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                            modifier = Modifier.padding(horizontal = if (isSmallScreen) 3.dp else 6.dp)
                        )
                        Text(
                            text = String.format("%02d", selectedMinute),
                            fontSize = if (isSmallScreen) 28.sp else 38.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PrimaryTeal
                        )
                    }

                    Spacer(modifier = Modifier.height(if (isSmallScreen) 12.dp else 18.dp))

                    // عناصر اختيار الوقت التفاعلية مرتبة من اليمين للشمال:
                    // (1) اختيار صباح أو مساء على اليمين، (2) الساعة في الوسط، (3) الدقائق على اليسار
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(if (isSmallScreen) 6.dp else 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. اختيار صباح أو مساء (على اليمين في RTL)
                        Column(
                            modifier = Modifier.weight(1.1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(if (isSmallScreen) 4.dp else 6.dp)
                        ) {
                            Text(
                                text = "الفترة",
                                fontSize = if (isSmallScreen) 11.sp else 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                            AmPmToggleCard(
                                text = "صباحاً (ص)",
                                isSelected = !isPm,
                                activeColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                                modifier = Modifier.fillMaxWidth().height(if (isSmallScreen) 44.dp else 56.dp),
                                isSmall = isSmallScreen,
                                onClick = { isPm = false }
                            )
                            AmPmToggleCard(
                                text = "مساءً (م)",
                                isSelected = isPm,
                                activeColor = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
                                modifier = Modifier.fillMaxWidth().height(if (isSmallScreen) 44.dp else 56.dp),
                                isSmall = isSmallScreen,
                                onClick = { isPm = true }
                            )
                        }

                        // 2. الساعة (في الوسط في RTL)
                        TimeCounterColumn(
                            modifier = Modifier.weight(1f),
                            label = "الساعة",
                            value = selectedHour12,
                            minValue = 1,
                            maxValue = 12,
                            isSmall = isSmallScreen,
                            onIncrement = {
                                selectedHour12 = if (selectedHour12 >= 12) 1 else selectedHour12 + 1
                            },
                            onDecrement = {
                                selectedHour12 = if (selectedHour12 <= 1) 12 else selectedHour12 - 1
                            }
                        )

                        // 3. الدقيقة (على اليسار في RTL)
                        TimeCounterColumn(
                            modifier = Modifier.weight(1f),
                            label = "الدقيقة",
                            value = selectedMinute,
                            minValue = 0,
                            maxValue = 59,
                            isSmall = isSmallScreen,
                            onIncrement = {
                                selectedMinute = if (selectedMinute >= 59) 0 else selectedMinute + 1
                            },
                            onDecrement = {
                                selectedMinute = if (selectedMinute <= 0) 59 else selectedMinute - 1
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(if (isSmallScreen) 10.dp else 14.dp))

                    // أزرار سريعة للدقائق الشائعة
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(if (isSmallScreen) 4.dp else 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(0, 15, 30, 45).forEach { min ->
                            val isSelected = selectedMinute == min
                            val chipBg = when {
                                isSelected -> PrimaryTeal
                                isDark -> Color(0xFF1B2830)
                                else -> Color(0xFFF1F5F9)
                            }
                            val chipText = when {
                                isSelected -> Color.White
                                isDark -> Color(0xFFCBD5E1)
                                else -> Color(0xFF334155)
                            }
                            val chipBorder = when {
                                isSelected -> PrimaryTeal
                                isDark -> Color(0xFF2E404C)
                                else -> Color(0xFFCBD5E1)
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(chipBg)
                                    .border(1.dp, chipBorder, RoundedCornerShape(8.dp))
                                    .clickable { selectedMinute = min }
                                    .padding(vertical = if (isSmallScreen) 5.dp else 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = ":${String.format("%02d", min)}",
                                    fontSize = if (isSmallScreen) 11.5.sp else 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = chipText
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(if (isSmallScreen) 14.dp else 20.dp))

                    // أزرار الإجراءات (تأكيد / إلغاء)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(if (isSmallScreen) 6.dp else 10.dp)
                    ) {
                        MosaribOutlinedButton(
                            text = "إلغاء",
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            height = if (isSmallScreen) 42.dp else 46.dp
                        )
                        MosaribButton(
                            text = "تأكيد الوقت",
                            onClick = {
                                val hour24 = when {
                                    isPm && selectedHour12 == 12 -> 12
                                    isPm -> selectedHour12 + 12
                                    !isPm && selectedHour12 == 12 -> 0
                                    else -> selectedHour12
                                }
                                onTimeSelected(hour24, selectedMinute)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            height = if (isSmallScreen) 42.dp else 46.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AmPmToggleCard(
    text: String,
    isSelected: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
    isSmall: Boolean = false,
    onClick: () -> Unit
) {
    val isDark = isAppDark()
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) activeColor.copy(alpha = if (isDark) 0.22f else 0.12f)
        else if (isDark) Color(0xFF141F25) else Color(0xFFF8FAFC),
        label = "ampm_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) activeColor
        else if (isDark) Color(0xFF2A3C45) else Color(0xFFCBD5E1),
        label = "ampm_border"
    )
    val textColor = if (isSelected) activeColor
    else if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = if (isSmall) 6.dp else 10.dp, horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
            fontSize = if (isSmall) 11.5.sp else 13.5.sp,
            color = textColor,
            maxLines = 1
        )
    }
}

@Composable
private fun TimeCounterColumn(
    label: String,
    value: Int,
    minValue: Int,
    maxValue: Int,
    modifier: Modifier = Modifier,
    isSmall: Boolean = false,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    val isDark = isAppDark()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (isSmall) 3.dp else 4.dp)
    ) {
        Text(
            text = label,
            fontSize = if (isSmall) 11.sp else 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
        )

        IconButton(
            onClick = onIncrement,
            modifier = Modifier
                .size(if (isSmall) 32.dp else 36.dp)
                .clip(CircleShape)
                .background(if (isDark) Color(0xFF202F38) else Color(0xFFE2E8F0))
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "زيادة",
                modifier = Modifier.size(if (isSmall) 16.dp else 18.dp),
                tint = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
            )
        }

        var dragAccumulator by remember { mutableFloatStateOf(0f) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isSmall) 42.dp else 46.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isDark) Color(0xFF0D161B) else Color(0xFFF8FAFC))
                .border(1.dp, if (isDark) Color(0xFF2E404C) else Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            dragAccumulator += dragAmount
                            val threshold = 22f
                            while (dragAccumulator <= -threshold) {
                                onIncrement()
                                dragAccumulator += threshold
                            }
                            while (dragAccumulator >= threshold) {
                                onDecrement()
                                dragAccumulator -= threshold
                            }
                        },
                        onDragEnd = { dragAccumulator = 0f },
                        onDragCancel = { dragAccumulator = 0f }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = String.format("%02d", value),
                fontSize = if (isSmall) 19.sp else 22.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
            )
        }

        IconButton(
            onClick = onDecrement,
            modifier = Modifier
                .size(if (isSmall) 32.dp else 36.dp)
                .clip(CircleShape)
                .background(if (isDark) Color(0xFF202F38) else Color(0xFFE2E8F0))
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "إنقاص",
                modifier = Modifier.size(if (isSmall) 16.dp else 18.dp),
                tint = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
            )
        }
    }
}

/**
 * نافذة حوار مخصصة لاختيار التاريخ (يوم / شهر / سنة) بدون استخدام نوافذ النظام
 * - تدعم الوضعين الفاتح والمظلم بتباين عالٍ وألوان منسجمة
 */
@Composable
fun MosaribDatePickerDialog(
    initialTimestamp: Long = System.currentTimeMillis(),
    minTimestamp: Long? = null,
    title: String = "تحديد التاريخ",
    onDismiss: () -> Unit,
    onDateSelected: (timestamp: Long) -> Unit
) {
    val context = LocalContext.current
    val isDark = isAppDark()
    val effectiveInitial = if (minTimestamp != null && initialTimestamp < minTimestamp) minTimestamp else initialTimestamp
    val cal = remember(effectiveInitial) {
        Calendar.getInstance().apply { timeInMillis = effectiveInitial }
    }

    var selectedYear by remember { mutableIntStateOf(cal.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(cal.get(Calendar.MONTH)) } // 0..11
    var selectedDay by remember { mutableIntStateOf(cal.get(Calendar.DAY_OF_MONTH)) }

    val arabicMonths = listOf(
        "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )

    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenWidthDp < 360

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Card(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth(if (isSmallScreen) 0.96f else 0.90f)
                    .padding(horizontal = if (isSmallScreen) 4.dp else 8.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF141F25) else Color.White
                ),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF2A3C45) else Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(if (isSmallScreen) 12.dp else 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // شريط العنوان
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isSmallScreen) 32.dp else 38.dp)
                                .clip(CircleShape)
                                .background(PrimaryTeal.copy(alpha = if (isDark) 0.22f else 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = PrimaryTeal,
                                modifier = Modifier.size(if (isSmallScreen) 16.dp else 20.dp)
                            )
                        }
                        Text(
                            text = title,
                            style = (if (isSmallScreen) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium).copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(if (isSmallScreen) 12.dp else 18.dp))

                    // عرض التاريخ المختار
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) Color(0xFF0D161B) else Color(0xFFF1F5F9))
                            .border(1.dp, if (isDark) Color(0xFF23353E) else Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                            .padding(vertical = if (isSmallScreen) 8.dp else 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$selectedDay ${arabicMonths.getOrElse(selectedMonth) { "" }} $selectedYear",
                            fontSize = if (isSmallScreen) 17.sp else 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryTeal
                        )
                    }

                    Spacer(modifier = Modifier.height(if (isSmallScreen) 12.dp else 18.dp))

                    // عدادات اليوم والشهر والسنة
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(if (isSmallScreen) 6.dp else 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // اليوم
                        TimeCounterColumn(
                            modifier = Modifier.weight(1f),
                            label = "اليوم",
                            value = selectedDay,
                            minValue = 1,
                            maxValue = 31,
                            isSmall = isSmallScreen,
                            onIncrement = {
                                selectedDay = if (selectedDay >= 31) 1 else selectedDay + 1
                            },
                            onDecrement = {
                                selectedDay = if (selectedDay <= 1) 31 else selectedDay - 1
                            }
                        )

                        // الشهر
                        Column(
                            modifier = Modifier.weight(1.35f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(if (isSmallScreen) 3.dp else 4.dp)
                        ) {
                            Text(
                                text = "الشهر",
                                fontSize = if (isSmallScreen) 11.sp else 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                            IconButton(
                                onClick = { selectedMonth = if (selectedMonth >= 11) 0 else selectedMonth + 1 },
                                modifier = Modifier
                                    .size(if (isSmallScreen) 32.dp else 36.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0xFF202F38) else Color(0xFFE2E8F0))
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "زيادة الشهر",
                                    modifier = Modifier.size(if (isSmallScreen) 16.dp else 18.dp),
                                    tint = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(if (isSmallScreen) 42.dp else 46.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isDark) Color(0xFF0D161B) else Color(0xFFF8FAFC))
                                    .border(1.dp, if (isDark) Color(0xFF2E404C) else Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = arabicMonths.getOrElse(selectedMonth) { "" },
                                    fontSize = if (isSmallScreen) 12.sp else 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A),
                                    maxLines = 1
                                )
                            }
                            IconButton(
                                onClick = { selectedMonth = if (selectedMonth <= 0) 11 else selectedMonth - 1 },
                                modifier = Modifier
                                    .size(if (isSmallScreen) 32.dp else 36.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0xFF202F38) else Color(0xFFE2E8F0))
                            ) {
                                Icon(
                                    Icons.Default.Remove,
                                    contentDescription = "إنقاص الشهر",
                                    modifier = Modifier.size(if (isSmallScreen) 16.dp else 18.dp),
                                    tint = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
                                )
                            }
                        }

                        // السنة
                        TimeCounterColumn(
                            modifier = Modifier.weight(1.1f),
                            label = "السنة",
                            value = selectedYear,
                            minValue = 2020,
                            maxValue = 2035,
                            isSmall = isSmallScreen,
                            onIncrement = { selectedYear++ },
                            onDecrement = { if (selectedYear > 2020) selectedYear-- }
                        )
                    }

                    Spacer(modifier = Modifier.height(if (isSmallScreen) 16.dp else 22.dp))

                    // أزرار الإجراءات
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(if (isSmallScreen) 6.dp else 10.dp)
                    ) {
                        MosaribOutlinedButton(
                            text = "إلغاء",
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            height = if (isSmallScreen) 42.dp else 46.dp
                        )
                        MosaribButton(
                            text = "تأكيد التاريخ",
                            onClick = {
                                val targetCal = Calendar.getInstance().apply {
                                    set(selectedYear, selectedMonth, selectedDay, 0, 0, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                if (minTimestamp != null) {
                                    val minDayCal = Calendar.getInstance().apply {
                                        timeInMillis = minTimestamp
                                        set(get(Calendar.YEAR), get(Calendar.MONTH), get(Calendar.DAY_OF_MONTH), 0, 0, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    if (targetCal.before(minDayCal)) {
                                        Toast.makeText(context, "لا يمكن اختيار تاريخ انتهاء قبل تاريخ البدء", Toast.LENGTH_SHORT).show()
                                        return@MosaribButton
                                    }
                                }
                                onDateSelected(targetCal.timeInMillis)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            height = if (isSmallScreen) 42.dp else 46.dp
                        )
                    }
                }
            }
        }
    }
}
