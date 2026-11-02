package com.example.core.speech

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.license.LicenseDialog
import com.example.core.license.LicenseManager
import com.example.core.ui.DialogTone
import com.example.core.ui.MosaribDialog
import com.example.ui.theme.mosarib
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Shared button for reading text aloud via Arabic TTS.
 * Supports configurable size (32dp or 40dp), loading spinner during prepare(),
 * instance-specific utterance tracking (turns to Stop on that card only),
 * lock badge for free users, and automatic stop on composition disposal.
 */
@Composable
fun SpeakButton(
    text: () -> String,
    modifier: Modifier = Modifier,
    iconSize: Dp = 20.dp,
    onRequireLicense: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isSpeaking by MosaribSpeaker.isSpeaking.collectAsStateWithLifecycle()
    val activeUtteranceId by MosaribSpeaker.activeUtteranceId.collectAsStateWithLifecycle()
    val myUtteranceId = remember { UUID.randomUUID().toString() }
    val isMySpeaking = isSpeaking && (activeUtteranceId == myUtteranceId)
    var isSubscribed by remember(context) { mutableStateOf(LicenseManager.isActivated(context)) }
    var isLoading by remember { mutableStateOf(false) }

    var showLicenseDialog by remember { mutableStateOf(false) }
    var missingStatus by remember { mutableStateOf<SpeechStatus?>(null) }
    var speechError by remember { mutableStateOf<SpeechError?>(null) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        MosaribSpeaker.errors.collect { err ->
            speechError = err
        }
    }

    DisposableEffect(myUtteranceId) {
        onDispose {
            if (activeUtteranceId == myUtteranceId) {
                MosaribSpeaker.stop()
            }
        }
    }

    val buttonSize = if (iconSize <= 18.dp) 32.dp else 40.dp

    Box(
        modifier = modifier.size(buttonSize),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = {
                if (isMySpeaking) {
                    MosaribSpeaker.stop()
                } else {
                    if (!isSubscribed) {
                        if (onRequireLicense != null) {
                            onRequireLicense()
                        } else {
                            showLicenseDialog = true
                        }
                    } else {
                        scope.launch {
                            isLoading = true
                            try {
                                val status = MosaribSpeaker.prepare(context)
                                if (status == SpeechStatus.Ready) {
                                    MosaribSpeaker.speak(text(), utteranceId = myUtteranceId)
                                } else {
                                    missingStatus = status
                                }
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                }
            },
            modifier = Modifier
                .size(buttonSize)
                .semantics {
                    contentDescription = if (isMySpeaking) "إيقاف القراءة" else "استمع"
                }
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(iconSize),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    imageVector = if (isMySpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = if (isMySpeaking) "إيقاف القراءة" else "استمع",
                    tint = if (isMySpeaking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(iconSize)
                )
            }
        }

        // Small lock badge for non-subscribers
        if (!isSubscribed) {
            val badgeSize = if (iconSize <= 18.dp) 12.dp else 14.dp
            val badgeIconSize = if (iconSize <= 18.dp) 8.dp else 10.dp
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-1).dp, y = (-1).dp)
                    .size(badgeSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.mosarib.warning,
                    modifier = Modifier.size(badgeIconSize)
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

    missingStatus?.let { status ->
        TtsMissingDataDialog(
            status = status,
            onDismiss = { missingStatus = null }
        )
    }

    speechError?.let { err ->
        SpeechErrorDialog(
            error = err,
            onDismiss = { speechError = null }
        )
    }
}

/**
 * Dialog alerting user when Arabic voice data is missing or no TTS engine is installed.
 */
@Composable
fun TtsMissingDataDialog(
    status: SpeechStatus,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    when (status) {
        SpeechStatus.MissingVoiceData, SpeechStatus.LanguageNotSupported -> {
            MosaribDialog(
                onDismissRequest = onDismiss,
                title = "الصوت العربي غير محمّل",
                message = "لتفعيل القراءة الصوتية، حمّل حزمة اللغة العربية من إعدادات تحويل النص إلى كلام في جوالك (مرة واحدة، تعمل بعدها بدون إنترنت).",
                tone = DialogTone.Info,
                confirmText = "تحميل الآن",
                dismissText = "لاحقاً",
                onConfirm = {
                    onDismiss()
                    MosaribSpeaker.openInstallTtsData(context)
                },
                onDismiss = onDismiss
            )
        }
        SpeechStatus.NoEngine -> {
            MosaribDialog(
                onDismissRequest = onDismiss,
                title = "الصوت العربي غير محمّل",
                message = "لا يوجد محرك نطق على جوالك. ثبّت «خدمات التعرف على الكلام من Google» من المتجر ثم أعد المحاولة.",
                tone = DialogTone.Info,
                confirmText = "تثبيت الآن",
                dismissText = "لاحقاً",
                onConfirm = {
                    onDismiss()
                    MosaribSpeaker.openGoogleTtsPlayStore(context)
                },
                onDismiss = onDismiss
            )
        }
        SpeechStatus.Ready -> {
            // No dialog needed
        }
    }
}

/**
 * Dialog alerting the user when speech playback fails.
 */
@Composable
fun SpeechErrorDialog(
    error: SpeechError,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    MosaribDialog(
        onDismissRequest = onDismiss,
        title = "تعذّر تشغيل الصوت",
        message = "تعذّر نطق العبارة الصوتية على هذا الجهاز. لتفعيل القراءة دون إنترنت، تأكد من تحميل حزمة الصوت العربي من إعدادات تحويل النص إلى كلام.",
        tone = DialogTone.Info,
        confirmText = "تحميل الصوت العربي",
        dismissText = "حسناً",
        onConfirm = {
            onDismiss()
            MosaribSpeaker.openInstallTtsData(context)
        },
        onDismiss = onDismiss
    )
}

/**
 * Menu item with support for small trailing icon (e.g. lock badge for non-subscribers).
 */
@Composable
fun MosaribMenuItem(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    destructive: Boolean = false,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    trailing: String? = null
) {
    DropdownMenuItem(
        text = {
            androidx.compose.foundation.layout.Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (trailingIcon != null) {
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(8.dp))
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = null,
                        tint = MaterialTheme.mosarib.warning,
                        modifier = Modifier.size(14.dp)
                    )
                } else if (trailing != null) {
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = trailing,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.mosarib.textMuted
                    )
                }
            }
        },
        leadingIcon = icon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = if (destructive) MaterialTheme.colorScheme.error else tint,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        onClick = onClick
    )
}
