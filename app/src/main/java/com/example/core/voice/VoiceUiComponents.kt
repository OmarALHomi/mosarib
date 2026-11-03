package com.example.core.voice

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
 * Chooser dialog shown when customer name is ambiguous (top 3 matches + other customer).
 */
@Composable
fun VoiceCustomerChooserDialog(
    spokenQuery: String,
    candidates: List<Customer>,
    onSelectCustomer: (Customer) -> Unit,
    onChooseOther: () -> Unit,
    onDismiss: () -> Unit
) {
    MosaribDialog(
        onDismissRequest = onDismiss,
        title = "تحديد العميل",
        message = "وجدنا أكثر من عميل مطابق لـ \"$spokenQuery\":",
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
        }
    }
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
