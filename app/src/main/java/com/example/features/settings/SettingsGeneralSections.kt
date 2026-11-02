package com.example.features.settings

import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Slider
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.core.speech.MosaribSpeaker
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.RadioButton
import androidx.compose.material3.HorizontalDivider
import com.example.core.ui.MosaribBottomSheet
import com.example.core.speech.SpeechPronunciation
import com.example.core.speech.SpeechStatus
import com.example.core.license.LicenseManager
import com.example.core.license.LicenseDialog
import com.example.ui.theme.mosarib
import kotlinx.coroutines.launch
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen

import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.Formatters
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryAqua

// Section 1: Water Price Setting & Historical Guarantee
@Composable
internal fun WaterPriceSection(
    defaultPriceStr: String,
    onDefaultPriceStrChange: (String) -> Unit,
    currencySymbol: String,
    onSavePrice: (Double) -> Unit
) {
    SettingsSectionCard {
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
            onValueChange = { onDefaultPriceStrChange(Formatters.formatAmountInput(it)) },
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
                onSavePrice(p)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
        ) {
            Text("حفظ السعر الجديد", fontWeight = FontWeight.Bold)
        }
    }
}

// Section 2: Distributor Profile for Invoices
@Composable
internal fun DistributorProfileSection(
    distributorName: String,
    onDistributorNameChange: (String) -> Unit,
    distributorPhone: String,
    onDistributorPhoneChange: (String) -> Unit,
    currencySymbol: String,
    onCurrencySymbolChange: (String) -> Unit,
    onSave: () -> Unit
) {
    SettingsSectionCard {
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
            onValueChange = onDistributorNameChange,
            label = { Text("اسم الموزع أو البئر") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = distributorPhone,
            onValueChange = onDistributorPhoneChange,
            label = { Text("رقم الهاتف") },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = currencySymbol,
            onValueChange = onCurrencySymbolChange,
            label = { Text("رمز العملة") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
        ) {
            Text("حفظ بيانات الموزع", fontWeight = FontWeight.Bold)
        }
    }
}

// Section 3: Theme Mode (Day / Night / System)
@Composable
internal fun ThemeModeSection(
    themeMode: String,
    onThemeModeSelected: (String) -> Unit
) {
    SettingsSectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(SecondaryAqua.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Palette, contentDescription = null, tint = SecondaryAqua, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text("مظهر التطبيق", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                Triple("LIGHT", "نهاري", Icons.Default.LightMode),
                Triple("DARK", "ليلي", Icons.Default.DarkMode),
                Triple("SYSTEM", "تلقائي", Icons.Default.Settings)
            ).forEach { (mode, label, icon) ->
                val isSelected = themeMode == mode
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onThemeModeSelected(mode) },
                    color = if (isSelected) PrimaryTeal else Color.Transparent,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 9.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    }
}

// Section 3.5: Security & Biometric Lock (البصمة والحماية)
@Composable
internal fun AppLockSection(
    isBiometricAvailable: Boolean,
    biometricEnabled: Boolean,
    onBiometricEnabledChange: (Boolean) -> Unit
) {
    SettingsSectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(PrimaryTeal.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = PrimaryTeal,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "قفل التطبيق (بصمة / رمز)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = if (isBiometricAvailable) "طلب البصمة أو رمز الهاتف عند فتح التطبيق"
                        else "فعّل بصمة أو رمز قفل في إعدادات الهاتف",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.5.sp
                        )
                    )
                }
            }

            Switch(
                checked = biometricEnabled,
                onCheckedChange = onBiometricEnabledChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = PrimaryTeal
                )
            )
        }
    }
}



@Composable
internal fun DataManagementSection(
    archivedCount: Int,
    onOpenRecycleBin: () -> Unit,
    onOpenManageGroups: () -> Unit
) {
    SettingsSectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(PrimaryTeal.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = PrimaryTeal
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text("البيانات", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Row 1: سلة المحذوفات
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onOpenRecycleBin)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (archivedCount > 0) "سلة المحذوفات ($archivedCount)" else "سلة المحذوفات",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Row 2: إدارة المجموعات
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onOpenManageGroups)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmarks,
                        contentDescription = null,
                        tint = PrimaryTeal,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "إدارة المجموعات",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

// Section 7: Voice Reading (القراءة الصوتية)
@Composable
internal fun VoiceReadingSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isSpeaking by MosaribSpeaker.isSpeaking.collectAsState()
    var isSubscribed by remember(context) { mutableStateOf(LicenseManager.isActivated(context)) }
    var showLicenseDialog by remember { mutableStateOf(false) }

    var currentRate by remember { mutableFloatStateOf(MosaribSpeaker.getSpeechRate(context)) }
    var status by remember { mutableStateOf<SpeechStatus?>(null) }
    var showVoicePickerSheet by remember { mutableStateOf(false) }
    var chosenVoiceName by remember { mutableStateOf(MosaribSpeaker.getChosenVoiceName(context)) }

    LaunchedEffect(Unit) {
        status = MosaribSpeaker.prepare(context)
    }

    val rateLabel = when {
        Math.abs(currentRate - 0.9f) < 0.02f -> "0.9× (موصى بها)"
        Math.abs(currentRate - 1.0f) < 0.02f -> "1.0× (عادية)"
        Math.abs(currentRate - 0.7f) < 0.02f -> "0.7× (بطيئة جداً)"
        Math.abs(currentRate - 0.8f) < 0.02f -> "0.8× (بطيئة)"
        Math.abs(currentRate - 1.1f) < 0.02f -> "1.1× (سريعة)"
        Math.abs(currentRate - 1.2f) < 0.02f -> "1.2× (سريعة جداً)"
        Math.abs(currentRate - 1.3f) < 0.02f -> "1.3× (أقصى سرعة)"
        else -> String.format(java.util.Locale.US, "%.2f×", currentRate)
    }

    SettingsSectionCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(PrimaryTeal.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = PrimaryTeal,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    "القراءة الصوتية",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            if (!isSubscribed) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.mosarib.textMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        "للمشتركين",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.mosarib.textMuted,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Voice Picker Row (Item 7)
        val availableVoices = remember(status) { MosaribSpeaker.getInstalledOfflineArabicVoices(context) }
        val activeVoiceIndex = availableVoices.indexOfFirst { it.name == chosenVoiceName }
        val activeVoiceLabel = if (activeVoiceIndex >= 0) {
            val v = availableVoices[activeVoiceIndex]
            val gender = when {
                v.name.contains("-male", ignoreCase = true) || v.features?.contains("male") == true -> "رجل"
                v.name.contains("-female", ignoreCase = true) || v.name.contains("-fem-", ignoreCase = true) || v.features?.contains("female") == true -> "امرأة"
                else -> null
            }
            if (gender != null) "صوت ${activeVoiceIndex + 1} ($gender)" else "صوت ${activeVoiceIndex + 1}"
        } else {
            chosenVoiceName?.substringAfterLast('-') ?: "تلقائي"
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { showVoicePickerSheet = true }
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "الصوت",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    activeVoiceLabel,
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.mosarib.textMuted)
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = null,
                tint = MaterialTheme.mosarib.textMuted
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Speech Rate Slider Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "سرعة القراءة",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                rateLabel,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = PrimaryTeal,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Slider(
            value = currentRate,
            onValueChange = { newRate ->
                val rounded = (Math.round(newRate * 20.0f) / 20.0f).coerceIn(0.7f, 1.3f)
                currentRate = rounded
                MosaribSpeaker.setSpeechRate(context, rounded)
            },
            valueRange = 0.7f..1.3f,
            steps = 11,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Test Voice & Pronunciation Buttons (Item 6.4)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    if (!isSubscribed) {
                        showLicenseDialog = true
                    } else if (isSpeaking) {
                        MosaribSpeaker.stop()
                    } else {
                        scope.launch {
                            val s = MosaribSpeaker.prepare(context)
                            status = s
                            if (s == SpeechStatus.Ready) {
                                MosaribSpeaker.speak("هذا نموذج لتجربة سرعة القراءة الصوتية في تطبيق المسرّب.")
                            }
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = if (isSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isSpeaking) "إيقاف" else "تجربة الصوت", style = MaterialTheme.typography.labelSmall)
                if (!isSubscribed) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.mosarib.textMuted,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            OutlinedButton(
                onClick = {
                    if (!isSubscribed) {
                        showLicenseDialog = true
                    } else if (isSpeaking) {
                        MosaribSpeaker.stop()
                    } else {
                        scope.launch {
                            val s = MosaribSpeaker.prepare(context)
                            status = s
                            if (s == SpeechStatus.Ready) {
                                MosaribSpeaker.speak(SpeechPronunciation.SAMPLE_PRONUNCIATION_TEXT)
                            }
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = if (isSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isSpeaking) "إيقاف" else "تجربة النطق", style = MaterialTheme.typography.labelSmall)
                if (!isSubscribed) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.mosarib.textMuted,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Status Indicator
        when (status) {
            SpeechStatus.Ready -> {
                val currentEngine by MosaribSpeaker.currentEngineLabel.collectAsState()
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.mosarib.success)
                        )
                        Text(
                            text = "جاهز (محرك قراءة النصوص متوفر)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.mosarib.success,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                    Text(
                        text = "المحرك: ${currentEngine ?: MosaribSpeaker.currentEngineDisplayName}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.mosarib.textMuted
                        ),
                        modifier = Modifier.padding(start = 18.dp)
                    )
                }
            }
            SpeechStatus.MissingVoiceData -> {
                Surface(
                    color = MaterialTheme.mosarib.warning.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.mosarib.warning)
                            )
                            Text(
                                text = "بيانات الصوت العربي غير مثبتة",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.mosarib.warning,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                        Button(
                            onClick = { MosaribSpeaker.openTtsDownload(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.mosarib.warning),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("تحميل الآن", color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            SpeechStatus.LanguageNotSupported, SpeechStatus.NoEngine -> {
                Surface(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error)
                            )
                            Text(
                                text = "المحرك لا يدعم العربية",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                        Button(
                            onClick = { MosaribSpeaker.openPlayStoreForGoogleTts(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("تثبيت محرك Google", color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            null -> {
                Text(
                    text = "جاري فحص محرك الصوت...",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }
    }

    if (showVoicePickerSheet) {
        val pickerVoices = remember { MosaribSpeaker.getInstalledOfflineArabicVoices(context) }
        MosaribBottomSheet(
            onDismissRequest = { showVoicePickerSheet = false },
            title = "اختيار الصوت",
            subtitle = "الأصوات العربية المثبتة لمحرك ${MosaribSpeaker.currentEngineDisplayName}",
            icon = Icons.AutoMirrored.Filled.VolumeUp
        ) {
            if (pickerVoices.isEmpty()) {
                Text(
                    text = "لا توجد أصوات عربية مثبتة أوفلاين لهذا المحرك.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.mosarib.textMuted),
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                pickerVoices.forEachIndexed { index, voice ->
                    val isSelected = (voice.name == chosenVoiceName)
                    val gender = when {
                        voice.name.contains("-male", ignoreCase = true) || voice.features?.contains("male") == true -> "رجل"
                        voice.name.contains("-female", ignoreCase = true) || voice.name.contains("-fem-", ignoreCase = true) || voice.features?.contains("female") == true -> "امرأة"
                        else -> null
                    }
                    val friendlyName = if (gender != null) "صوت ${index + 1} ($gender)" else "صوت ${index + 1}"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                chosenVoiceName = voice.name
                                MosaribSpeaker.setChosenVoice(context, voice.name)
                            }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                chosenVoiceName = voice.name
                                MosaribSpeaker.setChosenVoice(context, voice.name)
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = friendlyName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = voice.name,
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.mosarib.textMuted)
                            )
                        }
                        IconButton(
                            onClick = {
                                scope.launch {
                                    MosaribSpeaker.previewVoice(context, voice)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "استمع",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        showVoicePickerSheet = false
                        MosaribSpeaker.openInstallTtsData(context)
                    }
                    .padding(vertical = 10.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "تحميل أصوات أخرى",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }

    if (showLicenseDialog) {
        LicenseDialog(
            onDismiss = { showLicenseDialog = false },
            onActivated = {
                isSubscribed = LicenseManager.isActivated(context)
                showLicenseDialog = false
            },
            isMandatory = false,
            mandatoryReason = "القراءة الصوتية والكشف الصوتي متاحان للمشتركين."
        )
    }
}
