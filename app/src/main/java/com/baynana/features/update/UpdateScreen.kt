package com.baynana.features.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baynana.core.database.AppDatabase
import com.baynana.data.local.update.ReleaseRepository
import com.baynana.domain.update.ReleaseManifest
import com.baynana.domain.update.UpdateDecision
import com.baynana.ui.components.SectionHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope

/**
 * شاشة التحديث (ح٢٠). تعرض حالة واحدة صريحة في الأعلى، ثم زرًّا واحدًا للفعل التالي.
 *
 * قواعد الشاشة، وهي قرارات لا ذوق:
 * 1. **الفحص لا يقطع عملًا:** الشاشة تُقرأ أولًا من الملفّ المحفوظ (بلا شبكة وبلا انتظار)، ثم
 *    تُفحص الشبكة في الخلفية، ولا يُمنع المستخدم من الرجوع متى شاء.
 * 2. **زرّان لا أكثر في اللحظة:** «تحقّق الآن» قبل المعرفة، ثم «نزّل التحديث» بعدها. والتنزيل
 *    لا يبدأ وحده أبدًا، ولا يثبّت شيئًا بلا شاشة النظام.
 * 3. **كل حالة لها كلمة ولون وأيقونة** (لا لون وحده): لا يعرف اللون من لا يرى.
 * 4. **«تخطّي» لا يظهر للإصدار الإجباري:** من نسخته أقدم من الحد المدعوم لا يقرّر التأجيل.
 */
@Composable
fun UpdateScreen(
    onBack: () -> Unit,
    currentVersionCode: Int,
    currentVersionName: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var state by remember { mutableStateOf<UpdateDecision.UpdateState>(UpdateDecision.UpdateState.Unknown("جارٍ الفحص")) }
    var checkedAt by remember { mutableStateOf<Long?>(null) }
    var fromCache by remember { mutableStateOf(true) }
    var checking by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(false) }
    var installedFile by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }

    val repository = remember {
        ReleaseRepository(
            context = context,
            db = AppDatabase.getDatabase(context),
            currentVersionCode = currentVersionCode,
            currentVersionName = currentVersionName
        )
    }

    fun refresh(force: Boolean) {
        scope.launch {
            checking = true
            val result = withContext(Dispatchers.IO) {
                if (force) repository.check() else repository.cachedState()
            }
            state = result.state
            checkedAt = result.checkedAt
            fromCache = result.fromCache
            checking = false
        }
    }

    // أول ما تُفتح: اعرض ما نعرفه فورًا، ثم افحص الشبكة في الخلفية.
    LaunchedEffect(Unit) {
        refresh(force = false)
        refresh(force = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
            }
            Text("التحديث", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        }

        Text(
            "نسختك الحالية: ${UpdateDecision.displayVersion(currentVersionName, currentVersionCode)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(12.dp))
        StateCard(state = state, checking = checking, checkedAt = checkedAt, fromCache = fromCache)

        Spacer(Modifier.height(12.dp))
        when (val current = state) {
            is UpdateDecision.UpdateState.Optional -> ReleaseActions(
                release = current.release,
                downloading = downloading,
                onDownload = {
                    scope.launch {
                        downloading = true
                        notice = null
                        val outcome = withContext(Dispatchers.IO) { repository.download(current.release) }
                        downloading = false
                        when (outcome) {
                            is ReleaseRepository.DownloadOutcome.Ready -> {
                                installedFile = outcome.file.absolutePath
                                notice = "نزل الملفّ وتحققت بصمته (${outcome.sizeBytes / 1024} ك.ب). اضغط «ثبّت» ليكمل النظام."
                            }
                            is ReleaseRepository.DownloadOutcome.Refused -> notice = outcome.reasonArabic
                            is ReleaseRepository.DownloadOutcome.Failed -> notice = outcome.reasonArabic
                        }
                    }
                },
                onInstall = {
                    val path = installedFile ?: return@ReleaseActions
                    val intent = repository.installerIntent(java.io.File(path))
                    if (intent == null) {
                        notice = "تعذّر فتح مُثبِّت النظام. افتح الملفّ من مدير الملفّات."
                    } else {
                        runCatching { context.startActivity(intent) }
                            .onFailure { notice = "لم يسمح النظام بالتثبيت. فعّل «تثبيت تطبيقات من مصادر أخرى» لهذا التطبيق." }
                    }
                },
                canInstall = installedFile != null,
                installNote = repository.installSummary(current.release),
                onSkip = {
                    scope.launch {
                        val skipped = withContext(Dispatchers.IO) { repository.skip(current.release.versionCode) }
                        if (skipped) {
                            notice = "تخطّينا هذا الإصدار. لن نُظهره مرة أخرى، ويبقى التحديث متاحًا من هنا وقت شئت."
                            refresh(force = false)
                        }
                    }
                }
            )

            is UpdateDecision.UpdateState.Required -> ReleaseActions(
                release = current.release,
                downloading = downloading,
                onDownload = {
                    scope.launch {
                        downloading = true
                        val outcome = withContext(Dispatchers.IO) { repository.download(current.release) }
                        downloading = false
                        when (outcome) {
                            is ReleaseRepository.DownloadOutcome.Ready -> {
                                installedFile = outcome.file.absolutePath
                                notice = "نزل الملفّ وتحققت بصمته. اضغط «ثبّت»."
                            }
                            is ReleaseRepository.DownloadOutcome.Refused -> notice = outcome.reasonArabic
                            is ReleaseRepository.DownloadOutcome.Failed -> notice = outcome.reasonArabic
                        }
                    }
                },
                onInstall = {
                    val path = installedFile ?: return@ReleaseActions
                    val intent = repository.installerIntent(java.io.File(path))
                    if (intent == null) notice = "تعذّر فتح مُثبِّت النظام."
                    else runCatching { context.startActivity(intent) }
                        .onFailure { notice = "لم يسمح النظام بالتثبيت. فعّل «تثبيت تطبيقات من مصادر أخرى» لهذا التطبيق." }
                },
                canInstall = installedFile != null,
                installNote = repository.installSummary(current.release),
                onSkip = null
            )

            else -> Unit
        }

        notice?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
        }

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { refresh(force = true) }, enabled = !checking && !downloading) {
                Text(if (checking) "يفحص…" else "تحقّق الآن")
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionHeader(
            title = "كيف نتأكد أن التحديث منّا؟",
            subtitle = "ملفّ الإصدار موقّع بمفتاح المالك، والتطبيق يتحقق بالمفتاح العام وحده. " +
                "ثم يُنزَّل الملفّ ويُحسب sha256 له، ولا يُعرض للتثبيت إلا إذا طابق البصمة الموقّعة."
        )
        Text(
            "لا ننزّل شيئًا في الخلفية، ولا نثبّت بلا شاشة النظام، ولا نطلب صلاحية تثبيت صامت. " +
                "وإن فشل التحقق، لم يتغيّر شيء في تطبيقك ولا في دفاترك.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun StateCard(
    state: UpdateDecision.UpdateState,
    checking: Boolean,
    checkedAt: Long?,
    fromCache: Boolean
) {
    val (icon, container, onContainer) = when (state) {
        is UpdateDecision.UpdateState.UpToDate ->
            Triple(Icons.Default.CheckCircle, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        is UpdateDecision.UpdateState.Optional ->
            Triple(Icons.Default.NewReleases, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        is UpdateDecision.UpdateState.Required ->
            Triple(Icons.Default.ErrorOutline, MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        is UpdateDecision.UpdateState.Unknown ->
            Triple(Icons.Default.CloudOff, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }

    // النصّ المُعلن لقارئ الشاشة: الحالة ثم سببها، فلا يُقرأ اللون وحده.
    val announced = buildString {
        append(state.titleArabic)
        when (state) {
            is UpdateDecision.UpdateState.Unknown -> append(". ").append(state.reasonArabic)
            is UpdateDecision.UpdateState.Optional -> {
                append(". الإصدار ").append(UpdateDecision.displayVersion(state.release.versionName, state.release.versionCode))
                append(". ").append(state.release.messageArabic)
            }
            is UpdateDecision.UpdateState.Required -> {
                append(". يجب تحديث التطبيق من الإصدار ")
                append(UpdateDecision.displayVersion(state.release.versionName, state.release.versionCode))
                append(" ليعمل كما ينبغي. بياناتك لا تضيع.")
            }
            is UpdateDecision.UpdateState.UpToDate -> append(". نسختك هي الأحدث.")
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = announced },
        colors = CardDefaults.cardColors(containerColor = container)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = onContainer)
                Spacer(Modifier.width(8.dp))
                Text(
                    state.titleArabic,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = onContainer
                )
                if (checking) {
                    Spacer(Modifier.width(8.dp))
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                }
            }

            when (state) {
                is UpdateDecision.UpdateState.Unknown -> Text(
                    state.reasonArabic,
                    style = MaterialTheme.typography.bodySmall,
                    color = onContainer
                )
                is UpdateDecision.UpdateState.Optional -> Text(
                    "${UpdateDecision.displayVersion(state.release.versionName, state.release.versionCode)} • ${state.release.messageArabic}",
                    style = MaterialTheme.typography.bodySmall,
                    color = onContainer
                )
                is UpdateDecision.UpdateState.Required -> Text(
                    state.release.messageArabic,
                    style = MaterialTheme.typography.bodySmall,
                    color = onContainer
                )
                is UpdateDecision.UpdateState.UpToDate -> Text(
                    "لا شيء لتفعله. وإن صدر إصدار أحدث، يُعلَم جهازك عند أول فحص.",
                    style = MaterialTheme.typography.bodySmall,
                    color = onContainer
                )
            }

            val stamp = checkedAt?.let { "آخر فحص ناجح: ${timeAgoArabic(it)}" }
                ?: "لم ينجح فحص بعد على هذا الجهاز"
            Text(
                if (fromCache) "$stamp • الحالة من آخر ملفّ محفوظ" else stamp,
                style = MaterialTheme.typography.labelSmall,
                color = onContainer
            )
        }
    }
}

@Composable
private fun ReleaseActions(
    release: ReleaseManifest.Release,
    downloading: Boolean,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    canInstall: Boolean,
    installNote: String,
    onSkip: (() -> Unit)?
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            release.messageArabic,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            "بصمة الملفّ: ${release.apkSha256.take(16)}…",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onDownload, enabled = !downloading) {
                Icon(Icons.Default.Download, contentDescription = null)
                Text(if (downloading) "  ينزّل…" else "  نزّل التحديث")
            }
            if (canInstall) {
                Button(onClick = onInstall) { Text("ثبّت") }
            }
            if (onSkip != null) {
                TextButton(onClick = onSkip, enabled = !downloading) { Text("تخطَّ هذا الإصدار") }
            }
        }
        if (canInstall) {
            Text(
                installNote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun timeAgoArabic(at: Long, now: Long = System.currentTimeMillis()): String {
    val minutes = ((now - at) / 60_000).coerceAtLeast(0)
    return when {
        minutes < 1 -> "قبل لحظات"
        minutes < 60 -> "قبل $minutes دقيقة"
        minutes < 60 * 24 -> "قبل ${minutes / 60} ساعة"
        else -> "قبل ${minutes / (60 * 24)} يوم"
    }
}
