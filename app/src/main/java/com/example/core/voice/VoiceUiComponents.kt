package com.example.core.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.speech.MosaribSpeaker
import com.example.core.ui.DialogTone
import com.example.core.ui.MosaribBottomSheet
import com.example.core.ui.MosaribDialog
import com.example.core.ui.MosaribPrimaryButton
import com.example.core.util.Formatters
import com.example.features.customers.Customer
import com.example.ui.theme.mosarib

/**
 * Types of voice support dialogs that can be presented across screens.
 */
enum class VoiceDialogType {
    NONE,
    MISSING_ENGINE,
    OFFLINE_PACK_MISSING,
    OFFLINE_NOT_SUPPORTED,
    ONLINE_NOTICE,
    ONLINE_ONLY_NOTICE
}

/**
 * Bottom Sheet displaying voice input usage examples for all 5 operation types.
 */
@Composable
fun VoiceHintSheet(
    onDismiss: () -> Unit,
    onStartListening: () -> Unit
) {
    MosaribBottomSheet(
        onDismissRequest = onDismiss,
        title = "الإدخال الصوتي السريع",
        subtitle = "تحدث بالأمر وسيقوم التطبيق بتعبئة النموذج تلقائياً",
        icon = Icons.Default.Mic,
        footer = {
            MosaribPrimaryButton(
                text = "ابدأ التحدث الآن",
                onClick = onStartListening,
                modifier = Modifier.fillMaxWidth()
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "أمثلة للأوامر الصوتية المدعومة:",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.mosarib.textMuted
                )
            )

            VoiceExampleCard("سقي جديد", "سقي أحمد العولقي ساعتين ونص من عشرة آلاف، دفع خمسة آلاف", MaterialTheme.mosarib.infoContainer)
            VoiceExampleCard("سند قبض", "قبض من أحمد العولقي خمسطعش ألف", MaterialTheme.mosarib.successContainer)
            VoiceExampleCard("سند صرف", "صرف ديزل خمسين ألف", MaterialTheme.mosarib.debt.copy(alpha = 0.1f))
            VoiceExampleCard("شراء ساعات", "شراء من صالح ثلاث ساعات من ثمانية آلاف", MaterialTheme.mosarib.infoContainer)
            VoiceExampleCard("خصم ومسامحة", "خصم لأحمد ألفين", MaterialTheme.colorScheme.surfaceVariant)
        }
    }
}

@Composable
private fun VoiceExampleCard(typeLabel: String, exampleText: String, bgTint: Color) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgTint.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = typeLabel,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(76.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = exampleText,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Dialog shown when Google speech recognition service is not available on the device.
 */
@Composable
fun VoiceMissingDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    MosaribDialog(
        onDismissRequest = onDismiss,
        title = "محرك الصوت غير متوفر",
        message = "يتطلب الإدخال الصوتي وجود تطبيق Google للتعرف على الصوت (Google Voice Typing) مفعلاً على هاتفك.",
        icon = Icons.Default.WarningAmber,
        tone = DialogTone.Warning,
        confirmText = "تثبيت من متجر Play",
        onConfirm = {
            onDismiss()
            openGoogleAppPlayStore(context)
        },
        dismissText = "إلغاء",
        onDismiss = onDismiss
    )
}

/**
 * Dialog shown when offline Arabic speech recognition pack is not installed and device is offline.
 * Prompts user to trigger offline pack download without opening Google's red error screen.
 */
@Composable
fun VoiceOfflinePackDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    MosaribDialog(
        onDismissRequest = onDismiss,
        title = "حزمة التعرف على العربية غير محمّلة",
        message = "لإدخال البيانات بالصوت بدون إنترنت، يجب تحميل حزمة اللغة العربية. يمكنك طلب التنزيل الآن وسيبدأ التثبيت تلقائياً عند توفر الإنترنت.",
        icon = Icons.Default.WarningAmber,
        tone = DialogTone.Warning,
        confirmText = "تحميل عند توفر الإنترنت",
        onConfirm = {
            onDismiss()
            VoiceRecognitionHelper.triggerLanguagePackDownload(context)
        },
        dismissText = "حسناً",
        onDismiss = onDismiss
    )
}

/**
 * One-time notice shown when recognition is operating online via fallback and offline pack can be downloaded.
 */
@Composable
fun VoiceOnlineNoticeDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    MosaribDialog(
        onDismissRequest = onDismiss,
        title = "تنبيه الإدخال الصوتي",
        message = "التعرف على الصوت يستخدم الإنترنت الآن. لتعمل بدون إنترنت حمّل حزمة العربية.",
        icon = Icons.Default.Mic,
        tone = DialogTone.Neutral,
        confirmText = "تحميل",
        onConfirm = {
            onDismiss()
            VoiceRecognitionHelper.triggerLanguagePackDownload(context)
        },
        dismissText = "لاحقاً",
        onDismiss = onDismiss
    )
}

/**
 * Dialog shown when device is offline and Arabic offline recognition is not offered on the device.
 */
@Composable
fun VoiceOfflineNotSupportedDialog(
    onDismiss: () -> Unit
) {
    MosaribDialog(
        onDismissRequest = onDismiss,
        title = "التعرف الصوتي دون إنترنت غير متوفر",
        message = "لا يدعم محرك الصوت على هذا الجهاز التعرف على اللغة العربية بدون إنترنت. يرجى الاتصال بالإنترنت لاستخدام الإدخال الصوتي.",
        icon = Icons.Default.WarningAmber,
        tone = DialogTone.Warning,
        confirmText = "حسناً",
        onConfirm = onDismiss
    )
}

/**
 * One-time notice shown when recognition is operating online and Arabic offline download is not offered on this device.
 */
@Composable
fun VoiceOnlineOnlyNoticeDialog(
    onDismiss: () -> Unit
) {
    MosaribDialog(
        onDismissRequest = onDismiss,
        title = "تنبيه الإدخال الصوتي",
        message = "التعرف على الصوت يعمل عبر الإنترنت (محرك الصوت على هذا الجهاز لا يتيح تنزيل حزمة العربية دون إنترنت).",
        icon = Icons.Default.Mic,
        tone = DialogTone.Neutral,
        confirmText = "حسناً",
        onConfirm = onDismiss
    )
}

/**
 * Unified composable rendering whichever voice support dialog is active.
 */
@Composable
fun VoiceSupportDialogs(
    dialogType: VoiceDialogType,
    onDismiss: () -> Unit
) {
    when (dialogType) {
        VoiceDialogType.MISSING_ENGINE -> VoiceMissingDialog(onDismiss = onDismiss)
        VoiceDialogType.OFFLINE_PACK_MISSING -> VoiceOfflinePackDialog(onDismiss = onDismiss)
        VoiceDialogType.OFFLINE_NOT_SUPPORTED -> VoiceOfflineNotSupportedDialog(onDismiss = onDismiss)
        VoiceDialogType.ONLINE_NOTICE -> VoiceOnlineNoticeDialog(onDismiss = onDismiss)
        VoiceDialogType.ONLINE_ONLY_NOTICE -> VoiceOnlineOnlyNoticeDialog(onDismiss = onDismiss)
        VoiceDialogType.NONE -> {}
    }
}

private fun openGoogleAppPlayStore(context: Context) {
    try {
        val uri = Uri.parse("market://details?id=com.google.android.googlequicksearchbox")
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    } catch (_: Exception) {
        val uri = Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.googlequicksearchbox")
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    }
}

/**
 * Chooser dialog shown when customer name is ambiguous (top 3 matches + other customer),
 * or when customer is not found (shows closest matches + "إضافة عميل جديد باسم «...»").
 */
@Composable
fun VoiceCustomerChooserDialog(
    spokenQuery: String,
    candidates: List<Customer>,
    onSelectCustomer: (Customer) -> Unit,
    onChooseOther: () -> Unit,
    onAddNewCustomer: ((name: String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    MosaribDialog(
        onDismissRequest = onDismiss,
        title = "تحديد العميل",
        message = if (candidates.isNotEmpty()) {
            "وجدنا عملاء مطابقين لـ \"$spokenQuery\":"
        } else {
            "لم نجد عميلاً مطابقاً لـ \"$spokenQuery\". يمكنك إضافته الآن:"
        },
        icon = Icons.Default.Person,
        tone = DialogTone.Neutral,
        confirmText = "عميل آخر…",
        onConfirm = onChooseOther,
        dismissText = "إلغاء",
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            candidates.take(3).forEach { cust ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectCustomer(cust) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cust.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            if (cust.phone.isNotBlank()) {
                                Text(
                                    text = cust.phone,
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.mosarib.textMuted)
                                )
                            }
                        }
                    }
                }
            }

            if (spokenQuery.isNotBlank() && onAddNewCustomer != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAddNewCustomer(spokenQuery) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "إضافة عميل جديد باسم «$spokenQuery»",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Chooser dialog for well owner duration ambiguity: "سقي على حسابه / شراء ساعات منه".
 */
@Composable
fun VoiceWellOwnerActionChooserDialog(
    ownerName: String,
    onIrrigationOnAccount: () -> Unit,
    onPurchaseHours: () -> Unit,
    onDismiss: () -> Unit
) {
    MosaribDialog(
        onDismissRequest = onDismiss,
        title = "تحديد نوع العملية",
        message = "صاحب البئر «$ownerName» - اختر نوع العملية:",
        icon = Icons.Default.Person,
        tone = DialogTone.Neutral,
        confirmText = "",
        onConfirm = {},
        dismissText = "إلغاء",
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onIrrigationOnAccount() }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "سقي على حسابه",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "تسجيل جلسة ري أرضه واحتساب قيمتها في حسابه",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.mosarib.textMuted)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.mosarib.infoContainer.copy(alpha = 0.45f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPurchaseHours() }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "شراء ساعات منه",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "شراء حصة ساعات من صاحب البئر لمشروع الري",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.mosarib.textMuted)
                    )
                }
            }
        }
    }
}

/**
 * Chooser dialog for voucher direction ambiguity: "قبض منه / صرف له / خصم له".
 */
@Composable
fun VoiceVoucherTypeChooserDialog(
    spokenText: String,
    onReceipt: () -> Unit,
    onExpense: () -> Unit,
    onDiscount: () -> Unit,
    onDismiss: () -> Unit
) {
    MosaribDialog(
        onDismissRequest = onDismiss,
        title = "تحديد نوع السند",
        message = if (spokenText.isNotBlank()) "سمعت: «$spokenText»\nاختر نوع السند المطلوب:" else "اختر نوع السند المطلوب:",
        icon = Icons.Default.Mic,
        tone = DialogTone.Neutral,
        confirmText = "",
        onConfirm = {},
        dismissText = "إلغاء",
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.mosarib.successContainer.copy(alpha = 0.45f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onReceipt() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "قبض منه (سند قبض)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.mosarib.success
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.mosarib.debt.copy(alpha = 0.12f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpense() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "صرف له (سند صرف)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.mosarib.debt
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDiscount() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "خصم له (خصم ومسامحة)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * Dialog shown when speech was heard but no command could be inferred:
 * Displays "سمعت: …" and provides buttons to retry or show examples.
 */
@Composable
fun VoiceUnrecognizedDialog(
    spokenText: String,
    onReSpeak: () -> Unit,
    onShowExamples: () -> Unit,
    onDismiss: () -> Unit
) {
    MosaribDialog(
        onDismissRequest = onDismiss,
        title = "لم يتم التعرف على العملية",
        message = if (spokenText.isNotBlank()) {
            "سمعت: «$spokenText»\n\nتأكد من ذكر نوع العملية بوضوح (مثل: سقي، قبض، صرف، شراء، أو خصم)."
        } else {
            "لم يتم سماع أي كلام واضح. يرجى إعادة المحاولة."
        },
        icon = Icons.Default.WarningAmber,
        tone = DialogTone.Warning,
        confirmText = "إعادة التحدث",
        onConfirm = onReSpeak,
        dismissText = "عرض الأمثلة",
        onDismiss = onShowExamples
    )
}

/**
 * Opens system application details settings to allow manually enabling audio permissions.
 */
fun openAppSettings(context: Context) {
    try {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}

@Composable
fun VoiceSummaryBanner(
    command: VoiceCommand,
    resolvedCustomerName: String? = null,
    onReSpeak: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val summary = buildVoiceCommandSummary(command, resolvedCustomerName)
    VoiceSummaryBanner(
        summaryText = summary,
        rawSpokenText = command.rawText,
        unparsedWarnings = command.unparsed,
        onReSpeak = onReSpeak ?: {},
        modifier = modifier
    )
}

/**
 * Summary banner at the top of a voice-prefilled form:
 * "فهمت: سقي · أحمد بن حسين العولقي · ساعتان ونصف · من 10,000 · دفع 5,000"
 * Includes a speaker button (reads aloud with MosaribSpeaker) and an "إعادة" button.
 */
@Composable
fun VoiceSummaryBanner(
    summaryText: String,
    rawSpokenText: String? = null,
    unparsedWarnings: List<String> = emptyList(),
    onReSpeak: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = summaryText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    if (!rawSpokenText.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "سمعت: $rawSpokenText",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.mosarib.textMuted
                            )
                        )
                    }
                }

                // Speaker button reading aloud
                IconButton(
                    onClick = {
                        val cleanToSpeak = summaryText.removePrefix("فهمت: ")
                        MosaribSpeaker.speak(cleanToSpeak)
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "استماع",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(2.dp))

                // Re-speak button
                IconButton(
                    onClick = onReSpeak,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "إعادة التحدث",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (unparsedWarnings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.mosarib.warning,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "كلمات لم يتم التعرف عليها: ${unparsedWarnings.joinToString(", ")}",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.mosarib.warning)
                    )
                }
            }
        }
    }
}

/**
 * Formats a structured [VoiceCommand] into the standard summary text shown in the banner:
 * "فهمت: سقي · أحمد بن حسين العولقي · ساعتان ونصف · من 10,000 · دفع 5,000"
 */
fun buildVoiceCommandSummary(cmd: VoiceCommand, resolvedCustomerName: String? = null): String {
    val typeName = when (cmd.type) {
        VoiceCommandType.SESSION -> "سقي"
        VoiceCommandType.RECEIPT -> "سند قبض"
        VoiceCommandType.EXPENSE -> "سند صرف"
        VoiceCommandType.PURCHASE -> "شراء ساعات"
        VoiceCommandType.DISCOUNT -> "خصم ومسامحة"
        null -> "عملية"
    }
    val parts = mutableListOf<String>()
    parts.add(typeName)

    val cust = resolvedCustomerName?.takeIf { it.isNotBlank() } ?: cmd.customerQuery
    if (!cust.isNullOrBlank()) {
        parts.add(cust)
    }

    if (cmd.durationMinutes != null) {
        val h = cmd.durationMinutes / 60
        val m = cmd.durationMinutes % 60
        val durStr = when {
            h == 1 && m == 30 -> "ساعة ونصف"
            h == 2 && m == 30 -> "ساعتان ونصف"
            h > 2 && m == 30 -> "$h ساعات ونصف"
            h == 1 && m == 0 -> "ساعة واحدة"
            h == 2 && m == 0 -> "ساعتان"
            h > 2 && m == 0 -> "$h ساعات"
            h == 0 && m > 0 -> "$m دقيقة"
            else -> "${h}س و${m}د"
        }
        parts.add(durStr)
    }

    if (cmd.ratePerHour != null) {
        parts.add("من ${Formatters.formatNumber(cmd.ratePerHour)}")
    }

    if (cmd.amount != null) {
        parts.add("بمبلغ ${Formatters.formatNumber(cmd.amount)}")
    }

    if (cmd.paid != null) {
        parts.add("دفع ${Formatters.formatNumber(cmd.paid)}")
    }

    if (!cmd.category.isNullOrBlank()) {
        parts.add(cmd.category)
    }

    if (!cmd.notes.isNullOrBlank()) {
        parts.add(cmd.notes)
    }

    return "فهمت: ${parts.joinToString(" · ")}"
}

/**
 * Trailing mic icon button for text fields (amount, notes, customer, search bars).
 */
@Composable
fun VoiceFieldMic(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(28.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "إدخال صوتي",
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * In-app push-to-talk speech recording bottom sheet.
 * Replaces the external RecognizerIntent activity with an in-app SpeechRecognizer session:
 * - 88dp round primary mic button (compact: 72dp)
 * - Hold to talk: press to record, release to process
 * - Tap mode: single tap starts, second tap stops
 * - Live partial text display as user speaks
 * - Sound level ring responding to onRmsChanged
 * - Handles pauses with 5000ms silence length and auto-restart while held/in tap mode
 * - RECORD_AUDIO permission request on first use with Arabic rationale and settings fallback
 * - "إلغاء" button dismisses without processing
 */
@Composable
fun VoiceRecordSheet(
    title: String = "الإدخال الصوتي السريع",
    subtitle: String = "اضغط مطولاً وتكلّم، ثم أفلت",
    compact: Boolean = false,
    onDismiss: () -> Unit,
    onResult: (candidates: List<String>) -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var showRationaleDialog by remember { mutableStateOf(false) }
    var showDeniedDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            hasPermission = true
            showRationaleDialog = false
        } else {
            showRationaleDialog = false
            showDeniedDialog = true
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            showRationaleDialog = true
        }
    }

    if (showRationaleDialog) {
        MosaribDialog(
            onDismissRequest = {
                showRationaleDialog = false
                onDismiss()
            },
            title = "إذن الميكروفون",
            message = "يحتاج الإدخال الصوتي إلى الميكرفون لسماع كلامك. لا يُحفظ أي تسجيل.",
            icon = Icons.Default.Mic,
            tone = DialogTone.Neutral,
            confirmText = "متابعة",
            onConfirm = {
                showRationaleDialog = false
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            },
            dismissText = "إلغاء",
            onDismiss = {
                showRationaleDialog = false
                onDismiss()
            }
        )
        return
    }

    if (showDeniedDialog) {
        MosaribDialog(
            onDismissRequest = {
                showDeniedDialog = false
                onDismiss()
            },
            title = "إذن الميكروفون مطلوب",
            message = "تم رفض إذن الميكروفون. يرجى تفعيله من إعدادات التطبيق لتتمكن من استخدام الإدخال الصوتي.",
            icon = Icons.Default.WarningAmber,
            tone = DialogTone.Warning,
            confirmText = "فتح الإعدادات",
            onConfirm = {
                showDeniedDialog = false
                openAppSettings(context)
                onDismiss()
            },
            dismissText = "إلغاء",
            onDismiss = {
                showDeniedDialog = false
                onDismiss()
            }
        )
        return
    }

    if (!hasPermission) return

    if (!VoiceRecognitionHelper.isSpeechRecognitionAvailable(context)) {
        VoiceMissingDialog(onDismiss = onDismiss)
        return
    }

    var activeLocale by remember { mutableStateOf("ar-YE") }
    var preferOffline by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        VoiceRecognitionHelper.checkArabicSupport(context) { supportResult ->
            if (supportResult.installedOfflineLocale != null) {
                activeLocale = supportResult.installedOfflineLocale
                preferOffline = true
            } else {
                activeLocale = supportResult.bestOnlineLocale
                preferOffline = false
            }
        }
    }

    var isRecording by remember { mutableStateOf(false) }
    var isTapMode by remember { mutableStateOf(false) }
    var isHolding by remember { mutableStateOf(false) }
    var isStopping by remember { mutableStateOf(false) }
    var partialText by remember { mutableStateOf("") }
    val accumulatedParts = remember { mutableStateListOf<String>() }
    var lastCandidates by remember { mutableStateOf<List<String>>(emptyList()) }
    var rmsLevel by remember { mutableFloatStateOf(0f) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    var recognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    fun buildIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, activeLocale)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, activeLocale)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "ar")
            if (preferOffline) {
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L)
        }
    }

    fun finalizeSession() {
        isRecording = false
        isStopping = false
        isHolding = false
        isTapMode = false
        val full = accumulatedParts.joinToString(" ").trim()
        VoiceRecognitionHelper.logDebug("VoiceInput", "finalizeSession: full='$full'")
        if (full.isNotBlank()) {
            val candidates = mutableListOf<String>()
            candidates.add(full)
            for (c in lastCandidates) {
                val joined = if (accumulatedParts.size > 1) {
                    (accumulatedParts.dropLast(1) + c).joinToString(" ").trim()
                } else c.trim()
                if (joined.isNotBlank() && !candidates.contains(joined)) {
                    candidates.add(joined)
                }
            }
            onResult(candidates)
        } else {
            errorMessage = "لم يتم سماع أي كلام، يرجى إعادة المحاولة"
        }
    }

    fun restartSession() {
        if (!isHolding && !isTapMode) return
        try {
            recognizer?.cancel()
            recognizer?.startListening(buildIntent())
            isRecording = true
        } catch (e: Throwable) {
            VoiceRecognitionHelper.logDebug("VoiceInput", "restartSession error: ${e.message}")
        }
    }

    fun startSession(isHold: Boolean) {
        errorMessage = null
        isHolding = isHold
        isTapMode = !isHold
        isStopping = false
        accumulatedParts.clear()
        lastCandidates = emptyList()
        partialText = ""
        try {
            recognizer?.cancel()
            recognizer?.startListening(buildIntent())
            isRecording = true
        } catch (e: Throwable) {
            VoiceRecognitionHelper.logDebug("VoiceInput", "startSession error: ${e.message}")
            errorMessage = "تعذر بدء الميكروفون"
            isRecording = false
        }
    }

    fun stopSession() {
        VoiceRecognitionHelper.logDebug("VoiceInput", "stopSession, isRecording=$isRecording, parts=${accumulatedParts.size}")
        isHolding = false
        isTapMode = false
        isStopping = true
        try {
            recognizer?.stopListening()
        } catch (_: Throwable) {
            finalizeSession()
        }
        mainHandler.postDelayed({
            if (isStopping) {
                finalizeSession()
            }
        }, 1200)
    }

    DisposableEffect(context) {
        try {
            val sr = SpeechRecognizer.createSpeechRecognizer(context)
            recognizer = sr
            sr.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isRecording = true
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {
                    rmsLevel = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                }
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) {
                    VoiceRecognitionHelper.logDebug("VoiceInput", "onError code=$error, isHolding=$isHolding, isTapMode=$isTapMode, isStopping=$isStopping")
                    if (isHolding || isTapMode) {
                        if (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT || error == SpeechRecognizer.ERROR_NO_MATCH) {
                            restartSession()
                            return
                        }
                    }
                    if (isStopping) {
                        finalizeSession()
                        return
                    }
                    if (error != SpeechRecognizer.ERROR_CLIENT) {
                        errorMessage = VoiceRecognitionHelper.getErrorMessage(error)
                        isRecording = false
                    }
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                    val best = matches.firstOrNull()?.trim().orEmpty()
                    VoiceRecognitionHelper.logDebug("VoiceInput", "onResults best='$best', matches=$matches, isHolding=$isHolding, isTapMode=$isTapMode")
                    if (best.isNotBlank()) {
                        accumulatedParts.add(best)
                        lastCandidates = matches
                    }
                    partialText = ""
                    if (isHolding || isTapMode) {
                        restartSession()
                    } else {
                        finalizeSession()
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val p = matches?.firstOrNull()?.trim().orEmpty()
                    if (p.isNotBlank()) {
                        partialText = p
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        } catch (e: Throwable) {
            VoiceRecognitionHelper.logDebug("VoiceInput", "createSpeechRecognizer error: ${e.message}")
        }

        onDispose {
            mainHandler.removeCallbacksAndMessages(null)
            try {
                recognizer?.cancel()
                recognizer?.destroy()
            } catch (_: Throwable) {}
        }
    }

    val animatedRms by animateFloatAsState(
        targetValue = if (isRecording) rmsLevel else 0f,
        animationSpec = tween(durationMillis = 80),
        label = "rmsRing"
    )

    val liveText = remember(accumulatedParts.toList(), partialText) {
        val acc = accumulatedParts.joinToString(" ").trim()
        when {
            acc.isNotBlank() && partialText.isNotBlank() -> "$acc $partialText"
            acc.isNotBlank() -> acc
            partialText.isNotBlank() -> partialText
            else -> ""
        }
    }

    MosaribBottomSheet(
        onDismissRequest = {
            try {
                recognizer?.cancel()
            } catch (_: Throwable) {}
            onDismiss()
        },
        title = title,
        subtitle = subtitle,
        icon = Icons.Default.Mic,
        footer = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        try {
                            recognizer?.cancel()
                        } catch (_: Throwable) {}
                        onDismiss()
                    }
                ) {
                    Text(
                        text = "إلغاء",
                        style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.mosarib.textMuted)
                    )
                }

                if (isTapMode && isRecording) {
                    MosaribPrimaryButton(
                        text = "إنهاء واعتماد",
                        onClick = { stopSession() }
                    )
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = if (compact) 8.dp else 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Live recognized text box
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (compact) 60.dp else 84.dp)
                    .padding(horizontal = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (liveText.isNotBlank()) {
                        Text(
                            text = liveText,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            textAlign = TextAlign.Center
                        )
                    } else if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.mosarib.warning),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            text = if (isRecording) "تحدث الآن، نحن نستمع..." else "اضغط مطولاً أو انقر للبدء...",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.mosarib.textMuted),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (compact) 16.dp else 24.dp))

            // Sound level ring & 88dp Mic button
            val btnSize = if (compact) 72.dp else 88.dp
            val outerBoxSize = if (compact) 108.dp else 132.dp

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(outerBoxSize)
            ) {
                // Outer ring
                Box(
                    modifier = Modifier
                        .size(btnSize + (animatedRms * (if (compact) 28 else 40)).dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f + (animatedRms * 0.35f)))
                )
                // Mid ring
                Box(
                    modifier = Modifier
                        .size(btnSize + (animatedRms * (if (compact) 12 else 18)).dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f + (animatedRms * 0.25f)))
                )
                // Main mic button
                Box(
                    modifier = Modifier
                        .size(btnSize)
                        .clip(CircleShape)
                        .background(
                            if (isRecording) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                        )
                        .pointerInput(isRecording, isTapMode) {
                            detectTapGestures(
                                onPress = {
                                    if (isTapMode) {
                                        stopSession()
                                        return@detectTapGestures
                                    }
                                    startSession(isHold = true)
                                    val pressStart = System.currentTimeMillis()
                                    tryAwaitRelease()
                                    val elapsed = System.currentTimeMillis() - pressStart
                                    if (elapsed < 350) {
                                        isHolding = false
                                        isTapMode = true
                                    } else {
                                        stopSession()
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "ميكروفون",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(if (compact) 32.dp else 40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (compact) 12.dp else 18.dp))

            // Dynamic hints below mic
            when {
                isRecording && isHolding -> {
                    Text(
                        text = "تكلّم الآن... أفلت الزر عند الانتهاء",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                isRecording && isTapMode -> {
                    Text(
                        text = "تكلّم الآن... اضغط الزر للإنهاء",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                else -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "اضغط مطولاً وتكلّم، ثم أفلت",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "أو اضغط للبدء ثم اضغط للإنهاء",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.mosarib.textMuted)
                        )
                    }
                }
            }
        }
    }
}
