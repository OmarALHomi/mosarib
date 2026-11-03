package com.example.features.settings

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WarningAmber
import com.example.core.voice.VoiceUnparsedLog
import com.example.core.voice.VoiceUnparsedPhrasesSheet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.speech.MosaribSpeaker
import com.example.core.speech.SpeechPronunciation
import com.example.core.speech.SpeechStatus
import com.example.core.ui.MosaribBottomSheet
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.mosarib
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsVoiceScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isSpeaking by MosaribSpeaker.isSpeaking.collectAsState()
    var isConnectedReading by remember { mutableStateOf(MosaribSpeaker.isConnectedReadingEnabled(context)) }

    var currentRate by remember { mutableFloatStateOf(MosaribSpeaker.getSpeechRate(context)) }
    var status by remember { mutableStateOf<SpeechStatus?>(null) }
    var showVoicePickerSheet by remember { mutableStateOf(false) }
    var chosenVoiceName by remember { mutableStateOf(MosaribSpeaker.getChosenVoiceName(context)) }
    var showUnparsedPhrasesSheet by remember { mutableStateOf(false) }
    var unparsedEntriesCount by remember { mutableStateOf(VoiceUnparsedLog.getEntries(context).size) }

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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "القراءة الصوتية",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp)
        ) {
            // -----------------------------------------------------------------
            // 1. إعدادات الصوت والنطق
            // -----------------------------------------------------------------
            item {
                SettingsSectionHeader(title = "إعدادات الصوت والنطق")
                SettingsCard {
                    // اختيار الصوت
                    SettingsRow(
                        title = "الصوت المعتمد",
                        subtitle = activeVoiceLabel,
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        iconTint = MaterialTheme.colorScheme.primary,
                        onClick = { showVoicePickerSheet = true }
                    )

                    SettingsRowDivider()

                    // سرعة القراءة
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "سرعة القراءة",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                rateLabel,
                                style = MaterialTheme.typography.bodyMedium.copy(
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
                    }

                    SettingsRowDivider()

                    // قراءة متصلة
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "قراءة متصلة",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "وصل الكلمات داخل الجملة والوقف عند علامات الترقيم",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.mosarib.textMuted)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Switch(
                            checked = isConnectedReading,
                            onCheckedChange = { checked ->
                                isConnectedReading = checked
                                MosaribSpeaker.setConnectedReadingEnabled(context, checked)
                            }
                        )
                    }
                }
            }

            // -----------------------------------------------------------------
            // 2. اختبار الصوت
            // -----------------------------------------------------------------
            item {
                Spacer(modifier = Modifier.height(14.dp))
                SettingsSectionHeader(title = "اختبار الصوت")
                SettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (isSpeaking) {
                                    MosaribSpeaker.stop()
                                } else {
                                    scope.launch {
                                        val s = MosaribSpeaker.prepare(context)
                                        status = s
                                        if (s == SpeechStatus.Ready) {
                                            MosaribSpeaker.speak(MosaribSpeaker.DEFAULT_PREVIEW_TEXT)
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isSpeaking) "إيقاف" else "تجربة الصوت",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                if (isSpeaking) {
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
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isSpeaking) "إيقاف" else "تجربة النطق",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            // -----------------------------------------------------------------
            // 3. حالة محرك الصوت
            // -----------------------------------------------------------------
            item {
                Spacer(modifier = Modifier.height(14.dp))
                SettingsSectionHeader(title = "حالة محرك الصوت")
                SettingsCard {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        when (status) {
                            SpeechStatus.Ready -> {
                                val currentEngine by MosaribSpeaker.currentEngineLabel.collectAsState()
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = MaterialTheme.mosarib.success,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                    }
                                    Text(
                                        text = "المحرك: ${currentEngine ?: MosaribSpeaker.currentEngineDisplayName}",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = MaterialTheme.mosarib.textMuted
                                        ),
                                        modifier = Modifier.padding(start = 18.dp)
                                    )
                                }
                            }
                            SpeechStatus.MissingVoiceData -> {
                                Surface(
                                    color = MaterialTheme.mosarib.warning.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
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
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
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
                }
            }
            // -----------------------------------------------------------------
            // 4. جمل لم تُفهم (لتطوير القواعد)
            // -----------------------------------------------------------------
            item {
                Spacer(modifier = Modifier.height(14.dp))
                SettingsSectionHeader(title = "سجل التعرف الصوتي")
                SettingsCard {
                    SettingsRow(
                        title = "جمل لم تُفهم ($unparsedEntriesCount)",
                        subtitle = "عرض الجمل غير المفهومة لنسخها أو مشاركتها وتطوير القواعد",
                        icon = Icons.Default.WarningAmber,
                        iconTint = MaterialTheme.mosarib.warning,
                        onClick = { showUnparsedPhrasesSheet = true }
                    )
                }
            }
        }
    }

    // Voice Picker Bottom Sheet (single-value editor)
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



    if (showUnparsedPhrasesSheet) {
        VoiceUnparsedPhrasesSheet(
            onDismiss = {
                showUnparsedPhrasesSheet = false
                unparsedEntriesCount = VoiceUnparsedLog.getEntries(context).size
            }
        )
    }
}