package com.example.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MosaribTheme

@Preview(name = "Sheet with Long List", showBackground = true)
@Composable
fun PreviewSheetWithLongList() {
    MosaribTheme {
        MosaribBottomSheetLazy(
            onDismissRequest = {},
            title = "سجل العمليات والتقارير",
            subtitle = "عرض 40 عملية سابقة",
            icon = Icons.Default.List,
            footer = {
                MosaribPrimaryButton(
                    text = "تصدير التقرير",
                    onClick = {},
                    modifier = Modifier.weight(1f)
                )
                MosaribSecondaryButton(
                    text = "إغلاق",
                    onClick = {},
                    modifier = Modifier.weight(1f)
                )
            }
        ) {
            items(40) { index ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "عملية رقم #${index + 1}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${(index + 1) * 2500} ر.ي",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Form Sheet with Unsaved Changes", showBackground = true)
@Composable
fun PreviewSheetWithUnsavedChanges() {
    MosaribTheme {
        var name by remember { mutableStateOf("محمد القاسمي") }
        var notes by remember { mutableStateOf("تم تعديل بعض البيانات...") }

        MosaribBottomSheet(
            onDismissRequest = {},
            title = "تعديل بيانات الحساب",
            subtitle = "المزارع: محمد القاسمي",
            icon = Icons.Default.Edit,
            hasUnsavedChanges = true,
            footer = {
                MosaribPrimaryButton(
                    text = "حفظ التغييرات",
                    onClick = {},
                    modifier = Modifier.weight(1f)
                )
                MosaribSecondaryButton(
                    text = "إلغاء",
                    onClick = {},
                    modifier = Modifier.weight(1f)
                )
            }
        ) {
            Text(
                text = "يوجد تغييرات غير محفوظة في هذا النموذج، إغلاق الشاشة سيطلب تأكيد التجاهل أولاً.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("الاسم") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("ملاحظات") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(name = "Dialog Tone - Neutral", showBackground = true)
@Composable
fun PreviewDialogNeutral() {
    MosaribTheme {
        MosaribDialog(
            onDismissRequest = {},
            title = "تأكيد الإجراء",
            message = "هل تريد اعتماد هذه العملية ونقلها إلى الحساب النهائي؟",
            icon = Icons.Default.WaterDrop,
            tone = DialogTone.Neutral,
            confirmText = "اعتماد",
            onConfirm = {},
            dismissText = "إلغاء",
            onDismiss = {}
        )
    }
}

@Preview(name = "Dialog Tone - Info", showBackground = true)
@Composable
fun PreviewDialogInfo() {
    MosaribTheme {
        MosaribDialog(
            onDismissRequest = {},
            title = "معلومات التحديث",
            message = "يتوفر إصدار جديد يتضمن إصلاحات مهمة للأمان وتحديثات لقاعدة البيانات.",
            icon = Icons.Default.Info,
            tone = DialogTone.Info,
            confirmText = "تحديث الآن",
            onConfirm = {},
            dismissText = "لاحقاً",
            onDismiss = {}
        )
    }
}

@Preview(name = "Dialog Tone - Success", showBackground = true)
@Composable
fun PreviewDialogSuccess() {
    MosaribTheme {
        MosaribDialog(
            onDismissRequest = {},
            title = "تمت العملية بنجاح",
            message = "تم حفظ السند وترحيل المبلغ إلى كشف حساب العميل بأمان.",
            icon = Icons.Default.Check,
            tone = DialogTone.Success,
            confirmText = "تم",
            onConfirm = {},
            dismissText = null
        )
    }
}

@Preview(name = "Dialog Tone - Warning", showBackground = true)
@Composable
fun PreviewDialogWarning() {
    MosaribTheme {
        MosaribDialog(
            onDismissRequest = {},
            title = "تنبيه تجاوز الرصيد",
            message = "رصيد العميل الحالي تجاوز الحد الائتماني المسموح به، هل تريد المتابعة؟",
            icon = Icons.Default.Warning,
            tone = DialogTone.Warning,
            confirmText = "متابعة",
            onConfirm = {},
            dismissText = "تراجع",
            onDismiss = {}
        )
    }
}

@Preview(name = "Dialog Tone - Danger", showBackground = true)
@Composable
fun PreviewDialogDanger() {
    MosaribTheme {
        MosaribDialog(
            onDismissRequest = {},
            title = "حذف العملية نهائياً؟",
            message = "سيتم نقل العملية إلى سلة المحذوفات ولن تظهر في كشوفات الحساب الحالية.",
            icon = Icons.Default.Delete,
            tone = DialogTone.Danger,
            confirmText = "حذف",
            onConfirm = {},
            dismissText = "إلغاء",
            onDismiss = {}
        )
    }
}

@Preview(name = "Menu with Destructive Item", showBackground = true)
@Composable
fun PreviewMenuWithDestructiveItem() {
    MosaribTheme {
        Surface(
            modifier = Modifier.padding(24.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Box {
                MosaribMenu(
                    expanded = true,
                    onDismissRequest = {}
                ) {
                    MosaribMenuItem(
                        text = "تعديل البيانات",
                        icon = Icons.Default.Edit,
                        onClick = {}
                    )
                    MosaribMenuItem(
                        text = "مشاركة كشف الحساب",
                        icon = Icons.Default.Share,
                        trailing = "PDF",
                        onClick = {}
                    )
                    MosaribMenuDivider()
                    MosaribMenuItem(
                        text = "حذف الحساب",
                        icon = Icons.Default.Delete,
                        destructive = true,
                        onClick = {}
                    )
                }
            }
        }
    }
}
