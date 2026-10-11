package com.example.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.PrimaryTeal
import java.util.Calendar

/**
 * نافذة حوار مخصصة وفاخرة لاختيار الوقت (بدون استخدام نوافذ النظام القديمة)
 * - دعم كامل ودقيق للغتين والعربية والوضع المظلم
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
    val isDark = isSystemInDarkTheme()

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
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF1E293B) else Color.White
            ),
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PrimaryTeal.copy(alpha = 0.15f)),
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
                            color = MaterialTheme.colorScheme.onSurface
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
                        .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9))
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
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                    Text(
                        text = String.format("%02d", selectedMinute),
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryTeal
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isPm) "مساءً" else "صباحاً",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPm) Color(0xFFF59E0B) else Color(0xFF0284C7)
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
                        activeColor = Color(0xFF0284C7),
                        modifier = Modifier.weight(1f),
                        onClick = { isPm = false }
                    )
                    AmPmToggleCard(
                        text = "مساءً (م)",
                        isSelected = isPm,
                        activeColor = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f),
                        onClick = { isPm = true }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                Spacer(modifier = Modifier.height(12.dp))

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

                Spacer(modifier = Modifier.height(12.dp))

                // أزرار سريعة للدقائق الشائعة
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(0, 15, 30, 45).forEach { min ->
                        val isSelected = selectedMinute == min
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) PrimaryTeal.copy(alpha = 0.18f)
                                    else if (isDark) Color(0xFF334155).copy(alpha = 0.5f)
                                    else Color(0xFFF1F5F9)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) PrimaryTeal else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedMinute = min }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = ":${String.format("%02d", min)}",
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) PrimaryTeal else MaterialTheme.colorScheme.onSurface
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

@Composable
private fun AmPmToggleCard(
    text: String,
    isSelected: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) activeColor.copy(alpha = 0.16f)
        else if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
        label = "ampm_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) activeColor else if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1),
        label = "ampm_border"
    )

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
            color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurface
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
    val isDark = isSystemInDarkTheme()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )

        IconButton(
            onClick = onIncrement,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
        ) {
            Icon(Icons.Default.Add, contentDescription = "زيادة", modifier = Modifier.size(18.dp))
        }

        Box(
            modifier = Modifier
                .size(width = 64.dp, height = 44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC))
                .border(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = String.format("%02d", value),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        IconButton(
            onClick = onDecrement,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
        ) {
            Icon(Icons.Default.Remove, contentDescription = "إنقاص", modifier = Modifier.size(18.dp))
        }
    }
}

/**
 * نافذة حوار مخصصة لاختيار التاريخ (يوم / شهر / سنة) بدون استخدام نوافذ النظام
 */
@Composable
fun MosaribDatePickerDialog(
    initialTimestamp: Long = System.currentTimeMillis(),
    title: String = "تحديد التاريخ",
    onDismiss: () -> Unit,
    onDateSelected: (timestamp: Long) -> Unit
) {
    val isDark = isSystemInDarkTheme()
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
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF1E293B) else Color.White
            ),
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PrimaryTeal.copy(alpha = 0.15f)),
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
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // عرض التاريخ المختار
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9))
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

                Spacer(modifier = Modifier.height(16.dp))

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
                        Text(text = "الشهر", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        IconButton(
                            onClick = { selectedMonth = if (selectedMonth >= 11) 0 else selectedMonth + 1 },
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "زيادة الشهر", modifier = Modifier.size(18.dp))
                        }
                        Box(
                            modifier = Modifier
                                .size(width = 80.dp, height = 44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC))
                                .border(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = arabicMonths.getOrElse(selectedMonth) { "" },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = { selectedMonth = if (selectedMonth <= 0) 11 else selectedMonth - 1 },
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "إنقاص الشهر", modifier = Modifier.size(18.dp))
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

                Spacer(modifier = Modifier.height(20.dp))

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
