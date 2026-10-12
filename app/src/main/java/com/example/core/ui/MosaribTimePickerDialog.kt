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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
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

    Dialog(onDismissRequest = onDismiss) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
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
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // شريط العنوان العلوي
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(PrimaryTeal.copy(alpha = if (isDark) 0.22f else 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = PrimaryTeal,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // عرض الوقت المختار بخط عريض وفخم
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDark) Color(0xFF0D161B) else Color(0xFFF1F5F9))
                        .border(1.dp, if (isDark) Color(0xFF23353E) else Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                        .padding(vertical = 12.dp, horizontal = 16.dp)
                ) {
                    Text(
                        text = String.format("%02d", selectedHour12),
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryTeal
                    )
                    Text(
                        text = ":",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                    Text(
                        text = String.format("%02d", selectedMinute),
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryTeal
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = if (isPm) "مساءً (م)" else "صباحاً (ص)",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isPm) {
                            if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
                        } else {
                            if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // التبديل بين صباحاً ومساءً
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AmPmToggleCard(
                        text = "صباحاً (ص)",
                        isSelected = !isPm,
                        activeColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                        modifier = Modifier.weight(1f),
                        onClick = { isPm = false }
                    )
                    AmPmToggleCard(
                        text = "مساءً (م)",
                        isSelected = isPm,
                        activeColor = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
                        modifier = Modifier.weight(1f),
                        onClick = { isPm = true }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = if (isDark) Color(0xFF26353E) else Color(0xFFE2E8F0))
                Spacer(modifier = Modifier.height(14.dp))

                // عدادات الساعات والدقائق التفاعلية
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // الساعات
                    TimeCounterColumn(
                        label = "الساعة",
                        value = selectedHour12,
                        minValue = 1,
                        maxValue = 12,
                        onIncrement = {
                            selectedHour12 = if (selectedHour12 >= 12) 1 else selectedHour12 + 1
                        },
                        onDecrement = {
                            selectedHour12 = if (selectedHour12 <= 1) 12 else selectedHour12 - 1
                        }
                    )

                    // الدقائق
                    TimeCounterColumn(
                        label = "الدقيقة",
                        value = selectedMinute,
                        minValue = 0,
                        maxValue = 59,
                        onIncrement = {
                            selectedMinute = if (selectedMinute >= 59) 0 else selectedMinute + 1
                        },
                        onDecrement = {
                            selectedMinute = if (selectedMinute <= 0) 59 else selectedMinute - 1
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // أزرار سريعة للدقائق الشائعة
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = ":${String.format("%02d", min)}",
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = chipText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // أزرار الإجراءات (تأكيد / إلغاء)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MosaribOutlinedButton(
                        text = "إلغاء",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        height = 46.dp
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
                        height = 46.dp
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
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
            fontSize = 13.5.sp,
            color = textColor
        )
    }
}

@Composable
private fun TimeCounterColumn(
    label: String,
    value: Int,
    minValue: Int,
    maxValue: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    val isDark = isAppDark()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
        )

        IconButton(
            onClick = onIncrement,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isDark) Color(0xFF202F38) else Color(0xFFE2E8F0))
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "زيادة",
                modifier = Modifier.size(18.dp),
                tint = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
            )
        }

        var dragAccumulator by remember { mutableFloatStateOf(0f) }

        Box(
            modifier = Modifier
                .size(width = 66.dp, height = 46.dp)
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
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
            )
        }

        IconButton(
            onClick = onDecrement,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isDark) Color(0xFF202F38) else Color(0xFFE2E8F0))
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "إنقاص",
                modifier = Modifier.size(18.dp),
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
    title: String = "تحديد التاريخ",
    onDismiss: () -> Unit,
    onDateSelected: (timestamp: Long) -> Unit
) {
    val isDark = isAppDark()
    val cal = remember(initialTimestamp) {
        Calendar.getInstance().apply { timeInMillis = initialTimestamp }
    }

    var selectedYear by remember { mutableIntStateOf(cal.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(cal.get(Calendar.MONTH)) } // 0..11
    var selectedDay by remember { mutableIntStateOf(cal.get(Calendar.DAY_OF_MONTH)) }

    val arabicMonths = listOf(
        "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )

    Dialog(onDismissRequest = onDismiss) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
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
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // شريط العنوان
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(PrimaryTeal.copy(alpha = if (isDark) 0.22f else 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = PrimaryTeal,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // عرض التاريخ المختار
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isDark) Color(0xFF0D161B) else Color(0xFFF1F5F9))
                        .border(1.dp, if (isDark) Color(0xFF23353E) else Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$selectedDay ${arabicMonths.getOrElse(selectedMonth) { "" }} $selectedYear",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryTeal
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // عدادات اليوم والشهر والسنة
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // اليوم
                    TimeCounterColumn(
                        label = "اليوم",
                        value = selectedDay,
                        minValue = 1,
                        maxValue = 31,
                        onIncrement = {
                            selectedDay = if (selectedDay >= 31) 1 else selectedDay + 1
                        },
                        onDecrement = {
                            selectedDay = if (selectedDay <= 1) 31 else selectedDay - 1
                        }
                    )

                    // الشهر
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "الشهر",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                        IconButton(
                            onClick = { selectedMonth = if (selectedMonth >= 11) 0 else selectedMonth + 1 },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF202F38) else Color(0xFFE2E8F0))
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "زيادة الشهر",
                                modifier = Modifier.size(18.dp),
                                tint = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(width = 96.dp, height = 44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isDark) Color(0xFF0D161B) else Color(0xFFF8FAFC))
                                .border(1.dp, if (isDark) Color(0xFF2E404C) else Color(0xFFCBD5E1), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = arabicMonths.getOrElse(selectedMonth) { "" },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                            )
                        }
                        IconButton(
                            onClick = { selectedMonth = if (selectedMonth <= 0) 11 else selectedMonth - 1 },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF202F38) else Color(0xFFE2E8F0))
                        ) {
                            Icon(
                                Icons.Default.Remove,
                                contentDescription = "إنقاص الشهر",
                                modifier = Modifier.size(18.dp),
                                tint = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
                            )
                        }
                    }

                    // السنة
                    TimeCounterColumn(
                        label = "السنة",
                        value = selectedYear,
                        minValue = 2020,
                        maxValue = 2035,
                        onIncrement = { selectedYear++ },
                        onDecrement = { if (selectedYear > 2020) selectedYear-- }
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                // أزرار الإجراءات
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MosaribOutlinedButton(
                        text = "إلغاء",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        height = 46.dp
                    )
                    MosaribButton(
                        text = "تأكيد التاريخ",
                        onClick = {
                            val targetCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, selectedYear)
                                set(Calendar.MONTH, selectedMonth)
                                set(Calendar.DAY_OF_MONTH, selectedDay)
                            }
                            onDateSelected(targetCal.timeInMillis)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        height = 46.dp
                    )
                }
            }
        }
    }
}
}
