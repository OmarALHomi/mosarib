package com.example.features.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WaterDrop
import com.example.core.util.BackupManager
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ui.LuxuryToastNotification
import com.example.core.util.Formatters
import com.example.features.pumps.PumpSource
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryAqua

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val config by viewModel.appConfig.collectAsStateWithLifecycle()
    val toast by viewModel.toast.collectAsStateWithLifecycle()

    var distributorName by remember(config.distributorName) { mutableStateOf(config.distributorName) }
    var distributorPhone by remember(config.distributorPhone) { mutableStateOf(config.distributorPhone) }
    var defaultPriceStr by remember(config.defaultPricePerHour) { mutableStateOf(config.defaultPricePerHour.toString()) }
    var currencySymbol by remember(config.currencySymbol) { mutableStateOf(config.currencySymbol) }

    // File Picker Launcher for JSON Database Restore
    val restoreFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.restoreBackup(it) }
    }

    val savedBackups by viewModel.savedBackups.collectAsStateWithLifecycle()
    var backupToRestore by remember { mutableStateOf<BackupManager.BackupFileInfo?>(null) }
    var backupToDelete by remember { mutableStateOf<BackupManager.BackupFileInfo?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {



        // Section 1: Water Price Setting & Historical Guarantee
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .shadow(2.dp, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AccentGold.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AttachMoney, contentDescription = null, tint = AccentGold)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("سعر ساعة الماء", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = defaultPriceStr,
                        onValueChange = { defaultPriceStr = Formatters.formatAmountInput(it) },
                        label = { Text("سعر الساعة ($currencySymbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("default_price_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    val parsedDefPrice = Formatters.parseAmountInput(defaultPriceStr)
                    if (parsedDefPrice > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = Formatters.amountToArabicWords(parsedDefPrice, currencySymbol),
                            style = MaterialTheme.typography.bodySmall.copy(color = PrimaryTeal, fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Notice about historical pricing preservation
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PrimaryTeal.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "يطبق على الجلسات الجديدة فقط.",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val p = if (parsedDefPrice > 0) parsedDefPrice else 5000.0
                            viewModel.updateDefaultPricePerHour(p)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                    ) {
                        Text("حفظ السعر الجديد", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section 2: Distributor Profile for Invoices
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .shadow(2.dp, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PrimaryTeal.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryTeal)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("بيانات الموزع", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = distributorName,
                        onValueChange = { distributorName = it },
                        label = { Text("اسم الموزع أو البئر") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = distributorPhone,
                        onValueChange = { distributorPhone = it },
                        label = { Text("رقم الهاتف") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = currencySymbol,
                        onValueChange = {
                            currencySymbol = it
                            viewModel.updateCurrencySymbol(it)
                        },
                        label = { Text("رمز العملة") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            viewModel.updateDistributorInfo(distributorName, distributorPhone)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                    ) {
                        Text("حفظ بيانات الموزع", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section 3: Theme Mode (Day / Night / System)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .shadow(2.dp, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SecondaryAqua.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Palette, contentDescription = null, tint = SecondaryAqua)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("مظهر التطبيق", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = config.themeMode == "LIGHT",
                                onClick = { viewModel.updateThemeMode("LIGHT") },
                                colors = RadioButtonDefaults.colors(selectedColor = PrimaryTeal)
                            )
                            Icon(Icons.Default.LightMode, contentDescription = null, tint = AccentGold, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("نهاري")
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = config.themeMode == "DARK",
                                onClick = { viewModel.updateThemeMode("DARK") },
                                colors = RadioButtonDefaults.colors(selectedColor = PrimaryTeal)
                            )
                            Icon(Icons.Default.DarkMode, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ليلي")
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = config.themeMode == "SYSTEM",
                                onClick = { viewModel.updateThemeMode("SYSTEM") },
                                colors = RadioButtonDefaults.colors(selectedColor = PrimaryTeal)
                            )
                            Text("تلقائي")
                        }
                    }
                }
            }
        }

        // Section 4: Real Database Backup & Storage (Phone + Google Drive)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .shadow(1.5.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE0F2F1)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "النسخ الاحتياطي والأمان",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0F172A)
                                )
                            )
                        }

                        // زر إنشاء نسخة جديدة وحفظها
                        Button(
                            onClick = { viewModel.backupToPhoneStorage() },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("نسخة جديدة", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "يتم حفظ نسخ البيانات محلياً وبإمكانك استعادتها أو مشاركتها عبر Google Drive وواتساب في أي وقت للحفاظ على حساباتك بأمان.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B))
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // خيارات الاستيراد والمشاركة
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.createBackupAndShare() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryTeal)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("مشاركة للدرايف", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { restoreFileLauncher.launch(arrayOf("application/json", "*/*")) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp), tint = AccentGold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("استيراد ملف", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // استعراض النسخ الاحتياطية المحفوظة
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "استعراض النسخ المحفوظة (${savedBackups.size})",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        )
                        IconButton(
                            onClick = { viewModel.loadBackups() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = "تحديث", modifier = Modifier.size(16.dp), tint = PrimaryTeal)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (savedBackups.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                .padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "لا توجد نسخ احتياطية محفوظة بعد. اضغط على 'نسخة جديدة' بالأعلى لإنشاء نسخة احتياطية فورية.",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B)),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            savedBackups.forEach { backup ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFF8FAFC))
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFE0F2F1)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Backup, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(16.dp))
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = backup.formattedDate,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF0F172A)
                                                )
                                            )
                                            Text(
                                                text = "${backup.sizeText}  •  ${backup.name}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color(0xFF64748B),
                                                    fontSize = 10.sp
                                                ),
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    // الأزرار: استعادة، مشاركة، حذف
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // استعادة
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(PrimaryTeal.copy(alpha = 0.12f))
                                                .clickable { backupToRestore = backup }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("استعادة", style = MaterialTheme.typography.labelSmall.copy(color = PrimaryTeal, fontWeight = FontWeight.Bold))
                                        }

                                        // مشاركة
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFFE8F5E9))
                                                .clickable { viewModel.shareExistingBackup(backup.file) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = "مشاركة", tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                                        }

                                        // حذف
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFFFFEBEE))
                                                .clickable { backupToDelete = backup },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFE53935), modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialogs for Restore and Delete
    val toRestore = backupToRestore
    if (toRestore != null) {
        AlertDialog(
            onDismissRequest = { backupToRestore = null },
            title = { Text("تأكيد استعادة النسخة الاحتياطية", fontWeight = FontWeight.Bold) },
            text = {
                Text("هل أنت متأكد من استعادة هذه النسخة بتاريخ ${toRestore.formattedDate}؟ سيتم تحديث وتثبيت البيانات الموجودة في النسخة.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreBackupFromFile(toRestore.file)
                        backupToRestore = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                ) {
                    Text("استعادة الآن", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { backupToRestore = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    val toDelete = backupToDelete
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { backupToDelete = null },
            title = { Text("تأكيد حذف النسخة", fontWeight = FontWeight.Bold) },
            text = {
                Text("هل تريد حذف ملف النسخة الاحتياطية (${toDelete.name}) نهائياً؟")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBackupFile(toDelete.file)
                        backupToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("حذف", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { backupToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Floating Toast Notification
    LuxuryToastNotification(
        toast = toast,
        onDismiss = { viewModel.dismissToast() },
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 10.dp)
    )
}
}
