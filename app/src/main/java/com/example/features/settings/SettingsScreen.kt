package com.example.features.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.WaterDrop
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
import com.example.core.ui.LuxuryBannerCard
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
    val pumps by viewModel.pumps.collectAsStateWithLifecycle()
    val toast by viewModel.toast.collectAsStateWithLifecycle()

    var distributorName by remember(config.distributorName) { mutableStateOf(config.distributorName) }
    var distributorPhone by remember(config.distributorPhone) { mutableStateOf(config.distributorPhone) }
    var defaultPriceStr by remember(config.defaultPricePerHour) { mutableStateOf(config.defaultPricePerHour.toString()) }
    var currencySymbol by remember(config.currencySymbol) { mutableStateOf(config.currencySymbol) }

    var pumpToEdit by remember { mutableStateOf<PumpSource?>(null) }
    var showAddPumpSheet by remember { mutableStateOf(false) }

    // File Picker Launcher for JSON Database Restore
    val restoreFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.restoreBackup(it) }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            LuxuryToastNotification(toast = toast, onDismiss = { viewModel.dismissToast() })
        }

        // Header Banner
        item {
            LuxuryBannerCard(
                title = "إعدادات التطبيق والمضخات",
                subtitle = "تسعير ساعة الماء، بيانات الفواتير، المظهر، والنسخ الاحتياطي السحابي",
                modifier = Modifier.padding(16.dp)
            )
        }

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
                        Text("تسعير ساعة الماء العام", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = defaultPriceStr,
                        onValueChange = { defaultPriceStr = Formatters.formatAmountInput(it) },
                        label = { Text("السعر الافتراضي لكل ساعة ري ($currencySymbol)") },
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
                            text = "ملاحظة أمان: أي تعديل في السعر هنا يطبق فقط على الجلسات الجديدة القادمة، ولا يؤثر إطلاقاً على الجلسات والمبيعات السابقة.",
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
                        Text("بيانات الموزع / المسرب (للفواتير وPDF)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = distributorName,
                        onValueChange = { distributorName = it },
                        label = { Text("اسم الموزع أو البئر (يظهر في رأس الفواتير)") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = distributorPhone,
                        onValueChange = { distributorPhone = it },
                        label = { Text("رقم هاتف الموزع للتواصل") },
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
                        label = { Text("رمز العملة (مثل: ر.ي، ر.س، ج.م، $)") },
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
                        Text("مظهر التطبيق (الثيم النهاري والليلي)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
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
                            Text("نهاري (فاتح)")
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = config.themeMode == "DARK",
                                onClick = { viewModel.updateThemeMode("DARK") },
                                colors = RadioButtonDefaults.colors(selectedColor = PrimaryTeal)
                            )
                            Icon(Icons.Default.DarkMode, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ليلي فخم")
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

        // Section 4: Pump Sources Management
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
                                    .background(PrimaryTeal.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.WaterDrop, contentDescription = null, tint = PrimaryTeal)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("مضخات وآبار المياه (${pumps.size})", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }

                        OutlinedButton(
                            onClick = { showAddPumpSheet = true },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إضافة مضخة")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    pumps.forEach { pump ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(pump.name, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "${pump.powerType}  •  سعر الساعة: ${Formatters.formatCurrency(pump.defaultPricePerHour, config.currencySymbol)}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }

                                Row {
                                    IconButton(onClick = { pumpToEdit = pump }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Edit, contentDescription = "تعديل", modifier = Modifier.size(18.dp))
                                    }
                                    if (pumps.size > 1) {
                                        IconButton(onClick = { viewModel.deletePump(pump) }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFE53935), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 5: Backup, Restore & Google Drive
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
                                .background(AccentEmerald.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = AccentEmerald)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("النسخ الاحتياطي ومزامنة Google Drive", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "يمكنك تصدير نسخة احتياطية كاملة من قاعدة البيانات وحفظها على Google Drive أو إرسالها إلى واتساب أو وحدة التخزين، واستعادتها في أي وقت بنقرة واحدة.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.createBackupAndShare() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                        ) {
                            Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("نسخ احتياطي / Drive", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                restoreFileLauncher.launch(arrayOf("application/json", "*/*"))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("استعادة بيانات", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Pump Sheet
    if (showAddPumpSheet || pumpToEdit != null) {
        AddEditPumpBottomSheet(
            initialPump = pumpToEdit,
            currencySymbol = config.currencySymbol,
            onDismiss = {
                showAddPumpSheet = false
                pumpToEdit = null
            },
            onSave = { id, name, loc, price, power, notes ->
                viewModel.savePump(id, name, loc, price, power, notes)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPumpBottomSheet(
    initialPump: PumpSource? = null,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (id: Long, name: String, location: String, price: Double, powerType: String, notes: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember { mutableStateOf(initialPump?.name ?: "") }
    var location by remember { mutableStateOf(initialPump?.locationOrWellNumber ?: "") }
    var priceStr by remember { mutableStateOf(initialPump?.defaultPricePerHour?.toString() ?: "5000") }
    var powerType by remember { mutableStateOf(initialPump?.powerType ?: "ديزل") }
    var notes by remember { mutableStateOf(initialPump?.notes ?: "") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Text(
                text = if (initialPump == null) "إضافة مضخة / بئر ماء جديدة" else "تعديل بيانات المضخة",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("اسم المضخة / البئر *") },
                placeholder = { Text("مضخة بئر الوادي") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("الموقع أو رقم البئر") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = priceStr,
                onValueChange = { priceStr = Formatters.formatAmountInput(it) },
                label = { Text("سعر ساعة الماء الافتراضي لهذه المضخة ($currencySymbol)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            val parsedPumpPrice = Formatters.parseAmountInput(priceStr)
            if (parsedPumpPrice > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = Formatters.amountToArabicWords(parsedPumpPrice, currencySymbol),
                    style = MaterialTheme.typography.bodySmall.copy(color = PrimaryTeal, fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = powerType,
                onValueChange = { powerType = it },
                label = { Text("نوع الطاقة (ديزل، كهرباء، طاقة شمسية)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("ملاحظات") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val price = if (parsedPumpPrice > 0) parsedPumpPrice else 5000.0
                    if (name.isNotBlank()) {
                        onSave(initialPump?.id ?: 0L, name, location, price, powerType, notes)
                        onDismiss()
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
            ) {
                Text("حفظ المضخة", fontWeight = FontWeight.Bold)
            }
        }
    }
}
