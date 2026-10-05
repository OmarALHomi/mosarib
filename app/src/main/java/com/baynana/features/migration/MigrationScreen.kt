package com.baynana.features.migration

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baynana.core.database.AppDatabase
import com.baynana.core.sync.SyncConfig
import com.baynana.core.util.FileSharingHelper
import com.baynana.data.local.migration.MigrationRepository
import com.baynana.data.local.sync.SyncTransportProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * «نقل الدفتر» (ح٢٣): تصدير الملفّ ومشاركته، واستيراد ملفّ وصل من الجهاز الآخر، ثم **حكم الجرد**.
 *
 * ثلاث قواعد في الشاشة، وكلّها لحماية بيانات العائلة:
 * 1. **لا شيء يعمل وحده**: لا تصدير دوري ولا استيراد خلفي؛ كل فعل بضغطة صريحة.
 * 2. **الملفّ يُكتب ثم يُشارك**: يُكتب في `cacheDir/reports` (المسار المصرَّح به في FileProvider) ثم
 *    يُشارك عبر واتساب أو أي تطبيق — بلا حساب ولا خادم.
 * 3. **لا «تمّ» بلا جرد**: بعد الاستيراد تُعرض نتيجة المقارنة صريحة: «الجرد مطابق» أو قائمة الفروق
 *    بالعربية. فإن نقص قيد واحد، يظهر باسمه ولا يُقال إن الترحيل نجح.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MigrationScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { MigrationRepository(AppDatabase.getDatabase(context)) }
    val channelReady = remember { SyncConfig.isConfigured(context) }

    var busy by remember { mutableStateOf("") }
    var headline by remember { mutableStateOf("") }
    var differences by remember { mutableStateOf<List<String>>(emptyList()) }
    var notes by remember { mutableStateOf<List<String>>(emptyList()) }
    var isClean by remember { mutableStateOf<Boolean?>(null) }
    var localLine by remember { mutableStateOf("") }

    // اختيار ملفّ الترحيل من الجهاز (لا إذن تخزين: منتقي النظام هو من يمنح الملفّ بعينه).
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = "جاري قراءة الملفّ…"
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                }.getOrNull()
            }
            if (text.isNullOrBlank()) {
                busy = ""
                headline = "لم يُقرأ الملفّ: تأكّد أنه ملفّ ترحيل «بيننا» غير معدَّل"
                isClean = null
                return@launch
            }
            val transport = SyncTransportProvider.transport()
            if (transport == null) {
                busy = ""
                headline = "لا قناة مزامنة مضبوطة على هذا الجهاز: الاستيراد يمرّ من الخادم نفسه ولا يعمل بدونه"
                isClean = null
                return@launch
            }
            val report = runCatching { repository.import(text, transport) }.getOrNull()
            busy = ""
            if (report == null) {
                headline = "تعذّر الاستيراد: الملفّ تالف أو مبتور"
                isClean = null
                return@launch
            }
            isClean = report.isClean
            headline = report.summaryText()
            differences = report.diff.differences
            notes = report.diff.notes
            localLine = report.localInventoryLine
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("نقل الدفتر", fontWeight = FontWeight.Bold)
                        Text(
                            "ملفّ واحد، وجرد يُثبت أن كل رقم وصل",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("ما تفعله هذه الشاشة", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "تُصدّر دفترك في ملفّ واحد تشاركه بنفسك (واتساب أو أي تطبيق)، أو تستورد ملفًّا " +
                            "وصل من الجهاز الآخر. وبعد الاستيراد تُقارَن أرقام الملفّ بأرقام دفترك " +
                            "وترى الفرق — إن وُجد — واحدًا واحدًا.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "لا شيء يُرسل من تلقاء نفسه، والملفّ لا يُرسل إلى أي جهة إلا أنت.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = {
                    busy = "جاري تجهيز الملفّ…"
                    scope.launch {
                        val export = withContext(Dispatchers.IO) { repository.export() }
                        val file = withContext(Dispatchers.IO) { writeMigrationFile(context, export) }
                        busy = ""
                        localLine = repository.inventoryLine(export.inventory)
                        headline = "الملفّ جاهز: ${export.events} حركة — ${repository.inventoryLine(export.inventory)}"
                        FileSharingHelper.shareTextFile(context, file, title = "ملفّ ترحيل دفتر بيننا")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = busy.isBlank()
            ) {
                Text("تصدير ملفّ الترحيل ومشاركته")
            }

            OutlinedButton(
                onClick = { pickFile.launch(arrayOf("application/json", "text/plain", "application/octet-stream", "*/*")) },
                modifier = Modifier.fillMaxWidth(),
                enabled = busy.isBlank()
            ) {
                Text(if (channelReady) "استيراد ملفّ ترحيل" else "استيراد ملفّ (يحتاج قناة مزامنة مضبوطة)")
            }

            if (busy.isNotBlank()) {
                Text(busy, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (headline.isNotBlank()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            when (isClean) {
                                true -> "الجرد مطابق ✓"
                                false -> "الجرد فيه فرق — لم يُكتمل النقل"
                                null -> "النتيجة"
                            },
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(headline, style = MaterialTheme.typography.bodySmall)
                        if (localLine.isNotBlank()) {
                            Text("دفترك الآن: $localLine", style = MaterialTheme.typography.labelSmall)
                        }
                        notes.forEach { note ->
                            Text("• $note", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        differences.forEach { line ->
                            Text("• $line", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

/** يكتب الملفّ في `cacheDir/reports` — المسار المصرَّح به في FileProvider وحده. */
private fun writeMigrationFile(context: Context, export: MigrationRepository.Export): File {
    val directory = File(context.cacheDir, "reports").apply { mkdirs() }
    val file = File(directory, export.fileName)
    file.writeText(export.text, Charsets.UTF_8)
    return file
}
