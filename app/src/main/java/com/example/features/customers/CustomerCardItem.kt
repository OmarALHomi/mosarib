package com.example.features.customers
import androidx.lifecycle.lifecycleScope

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import com.example.core.license.LicenseDialog
import com.example.core.license.LicenseManager
import com.example.core.speech.SpeechStatus
import com.example.core.speech.TtsMissingDataDialog
import com.example.core.speech.VoiceStatementExporter
import com.example.core.ui.MosaribLoadingDialog
import com.example.core.ui.MosaribDialog
import com.example.core.ui.DialogTone
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.CompositionLocalProvider
import com.example.core.ui.MosaribMenu
import com.example.core.speech.*
import com.example.core.ui.MosaribMenuDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import com.example.core.ui.isAppDark
import com.example.core.util.Formatters
import com.example.core.ui.pressScale

import com.example.ui.theme.AvatarPalette

import com.example.ui.theme.mosarib

@Suppress("DEPRECATION")
private val TightPlatformTextStyle = androidx.compose.ui.text.PlatformTextStyle(
    includeFontPadding = false
)

private val TightLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both
)

@Suppress("DEPRECATION")
private fun TextStyle.tight(overrideSize: TextUnit? = null): TextStyle {
    val size = if (overrideSize != null && overrideSize.isSpecified) overrideSize else this.fontSize
    val effectiveSize = if (size.isSpecified) size else 14.sp
    return this.copy(
        fontSize = effectiveSize,
        lineHeight = (effectiveSize.value * 1.25f).sp,
        lineHeightStyle = TightLineHeightStyle,
        platformStyle = TightPlatformTextStyle
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerCardItem(
    item: CustomerWithBalance,
    currencySymbol: String,
    onClick: () -> Unit,
    onCallClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onPdfStatementClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    onVoiceStatementClick: (() -> Unit)? = null
) {
    val customer = item.customer
    val isDark = isAppDark()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isSubscribed by remember(context) { mutableStateOf(LicenseManager.isActivated(context)) }
    var showSpeechLicenseDialog by remember { mutableStateOf(false) }
    var missingSpeechStatus by remember { mutableStateOf<SpeechStatus?>(null) }
    var isGeneratingVoiceStatement by remember { mutableStateOf(false) }
    var exportError by remember { mutableStateOf<String?>(null) }
    var speechError by remember { mutableStateOf<SpeechError?>(null) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        MosaribSpeaker.errors.collect { err ->
            speechError = err
        }
    }

    val activity = context as? androidx.activity.ComponentActivity
    val exportScope = activity?.lifecycleScope ?: scope
    var menuExpanded by remember(customer.id) { mutableStateOf(false) }

    val avatarColor = AvatarPalette.forId(customer.id, isDark)
    val initials = remember(customer.name) {
        val words = customer.name.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        when {
            words.isEmpty() -> ""
            words.size == 1 -> words[0].take(1)
            else -> {
                val firstLetter = words.first().take(1)
                val lastWord = words.last()
                val cleanLast = if (lastWord.startsWith("ال") && lastWord.length > 2) {
                    lastWord.removePrefix("ال")
                } else {
                    lastWord
                }
                val lastLetter = cleanLast.take(1)
                "$firstLetter$lastLetter"
            }
        }
    }
    val (stateColor, stateLabel, balanceAmount) = if (customer.isWellOwner) {
        val net = item.payableBalance - item.receivableBalance
        when {
            net > 0.001 -> Triple(MaterialTheme.mosarib.credit, "له", net)
            net < -0.001 -> Triple(MaterialTheme.mosarib.debt, "عليه", -net)
            else -> Triple(MaterialTheme.mosarib.settled, "مسدد", 0.0)
        }
    } else {
        when {
            item.receivableBalance > 0.001 -> Triple(MaterialTheme.mosarib.debt, "عليه", item.receivableBalance)
            item.receivableBalance < -0.001 -> Triple(MaterialTheme.mosarib.credit, "له", -item.receivableBalance)
            else -> Triple(MaterialTheme.mosarib.settled, "مسدد", 0.0)
        }
    }

    val isZeroBalance = stateLabel == "مسدد" || balanceAmount <= 0.001
    val amountText = if (isZeroBalance) "مسدد" else "$stateLabel: ${Formatters.formatCurrency(balanceAmount, currencySymbol)}"
    val amountInWords = if (isZeroBalance) "" else Formatters.amountToArabicWordsApprox(balanceAmount, currencySymbol)

    val cleanWords = remember(amountInWords) {
        amountInWords
            .removePrefix("فقط ")
            .removePrefix("سالب فقط ")
            .removeSuffix(" لا غير")
            .removeSuffix(" لاغير")
            .trim()
    }

    val cardDescription = remember(customer.name, amountText, cleanWords) {
        listOfNotNull(
            customer.name,
            amountText,
            cleanWords.takeIf { it.isNotBlank() }
        ).joinToString("، ")
    }

    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .pressScale(targetScale = 0.98f, interactionSource = interactionSource)
            .shadow(
                elevation = if (isDark) 0.dp else 1.dp,
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onClick
            )
            .semantics { contentDescription = cardDescription }
            .testTag("customer_card_${customer.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.4.dp, stateColor.copy(alpha = if (isDark) 0.45f else 0.30f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Header: Accent bar + Avatar + 2 lines of text (Name/Balance, Chips/Tafqeet)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Accent bar: 4dp x 28dp
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(stateColor)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Avatar: 44dp circle
                Box(
                    modifier = Modifier.size(44.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(avatarColor.background),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = avatarColor.onBackground
                        )
                    }

                    if (customer.isWellOwner) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(MaterialTheme.mosarib.info),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = "صاحب بئر",
                                tint = MaterialTheme.mosarib.onInfo,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Text column: Line 1 + Line 2
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // Line 1: Name (weight 1f) and Balance (intrinsic, titleMedium bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = customer.name,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium.tight().copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = amountText,
                            style = MaterialTheme.typography.titleMedium.tight().copy(fontWeight = FontWeight.Bold),
                            color = stateColor,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    // Line 2: Chips (no shrink, no wrap) and Amount in words (weight 1f, end-aligned)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (customer.customGroup.isNotBlank()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = customer.customGroup,
                                        style = MaterialTheme.typography.labelSmall.tight(),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${item.totalOperationsCount} حركة",
                                    style = MaterialTheme.typography.labelSmall.tight(),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }

                            if (customer.isWellOwner) {
                                Surface(
                                    color = MaterialTheme.mosarib.info.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "صاحب بئر",
                                        style = MaterialTheme.typography.labelSmall.tight().copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.mosarib.info,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        if (cleanWords.isNotBlank()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = cleanWords,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelMedium.tight().copy(fontWeight = FontWeight.Bold),
                                color = stateColor
                            )
                        }
                    }
                }
            }

            // Bottom row: Phone + WhatsApp + PDF + Call + Kebab menu (height 32dp with LocalMinimumInteractiveComponentSize 32dp)
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 32.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                Text(
                    text = customer.phone,
                    style = MaterialTheme.typography.labelSmall.tight(),
                    color = MaterialTheme.mosarib.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                SpeakButton(
                    text = {
                        SpeechTextBuilder.buildBalanceSentence(
                            customerName = customer.name,
                            balance = item.balance,
                            currency = currencySymbol,
                            isWellOwner = customer.isWellOwner,
                            payableBalance = item.payableBalance,
                            receivableBalance = item.receivableBalance
                        )
                    },
                    modifier = Modifier.size(32.dp),
                    iconSize = 18.dp
                )

                if (customer.phone.isNotBlank()) {
                    IconButton(
                        onClick = onWhatsAppClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "إرسال كشف عبر واتساب",
                            tint = MaterialTheme.mosarib.success,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onPdfStatementClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "كشف حساب PDF",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (customer.phone.isNotBlank()) {
                    IconButton(
                        onClick = onCallClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "اتصال",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier.size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "خيارات",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    MosaribMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        MosaribMenuItem(
                            text = "إرسال كشف عبر واتساب",
                            icon = Icons.AutoMirrored.Filled.Send,
                            tint = MaterialTheme.mosarib.success,
                            onClick = {
                                menuExpanded = false
                                onWhatsAppClick()
                            }
                        )
                        MosaribMenuItem(
                            text = "إرسال كشف صوتي",
                            icon = Icons.AutoMirrored.Filled.VolumeUp,
                            tint = MaterialTheme.colorScheme.primary,
                            trailingIcon = if (isSubscribed) null else Icons.Default.Lock,
                            onClick = {
                                menuExpanded = false
                                if (!isSubscribed) {
                                    showSpeechLicenseDialog = true
                                } else {
                                    if (onVoiceStatementClick != null) {
                                        onVoiceStatementClick()
                                    } else {
                                        isGeneratingVoiceStatement = true
                                        exportScope.launch {
                                            try {
                                                val res = VoiceStatementExporter.generateAndShareVoiceStatement(
                                                    context = context,
                                                    customer = customer,
                                                    balance = item.balance,
                                                    isWellOwner = customer.isWellOwner,
                                                    payableBalance = item.payableBalance,
                                                    receivableBalance = item.receivableBalance,
                                                    currencySymbol = currencySymbol,
                                                    onStatus = { st ->
                                                        isGeneratingVoiceStatement = false
                                                        missingSpeechStatus = st
                                                    }
                                                )
                                                if (res.isFailure) {
                                                    val ex = res.exceptionOrNull()
                                                    exportError = ex?.message ?: "تعذّر تجهيز الكشف الصوتي ومشاركته."
                                                }
                                            } catch (e: Exception) {
                                                exportError = e.message ?: "تعذّر تجهيز الكشف الصوتي."
                                            } finally {
                                                isGeneratingVoiceStatement = false
                                            }
                                        }
                                    }
                                }
                            }
                        )
                        MosaribMenuItem(
                            text = "كشف حساب PDF",
                            icon = Icons.Default.PictureAsPdf,
                            tint = MaterialTheme.colorScheme.tertiary,
                            onClick = {
                                menuExpanded = false
                                onPdfStatementClick()
                            }
                        )
                        MosaribMenuItem(
                            text = "عرض كشف الحساب",
                            icon = Icons.Default.Person,
                            tint = MaterialTheme.colorScheme.primary,
                            onClick = {
                                menuExpanded = false
                                onClick()
                            }
                        )
                        if (customer.phone.isNotEmpty()) {
                            MosaribMenuItem(
                                text = "اتصال هاتفي (${customer.phone})",
                                icon = Icons.Default.Call,
                                tint = MaterialTheme.colorScheme.primary,
                                onClick = {
                                    menuExpanded = false
                                    onCallClick()
                                }
                            )
                        }
                        MosaribMenuItem(
                            text = if (customer.isWellOwner) "تعديل حساب البئر" else "تعديل العميل",
                            icon = Icons.Default.Edit,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = {
                                menuExpanded = false
                                onEditClick()
                            }
                        )
                        MosaribMenuDivider()
                        MosaribMenuItem(
                            text = "أرشفة الحساب",
                            icon = Icons.Default.Delete,
                            destructive = true,
                            onClick = {
                                menuExpanded = false
                                onDeleteClick()
                            }
                        )
                    }
                }
                }
            }
        }
    }

    if (showSpeechLicenseDialog) {
        LicenseDialog(
            onDismiss = { showSpeechLicenseDialog = false },
            onActivated = {
                isSubscribed = LicenseManager.isActivated(context)
                showSpeechLicenseDialog = false
            },
            isMandatory = false,
            mandatoryReason = "القراءة الصوتية والكشف الصوتي متاحان للمشتركين."
        )
    }

    missingSpeechStatus?.let { status ->
        TtsMissingDataDialog(
            status = status,
            onDismiss = { missingSpeechStatus = null }
        )
    }

    if (isGeneratingVoiceStatement) {
        MosaribLoadingDialog(
            message = "جاري تجهيز الكشف الصوتي…"
        )
    }

    speechError?.let { err ->
        SpeechErrorDialog(
            error = err,
            onDismiss = { speechError = null }
        )
    }

    exportError?.let { msg ->
        MosaribDialog(
            onDismissRequest = { exportError = null },
            title = "تعذّر تجهيز الكشف الصوتي",
            message = if (msg.contains("TTS", ignoreCase = true) || msg.contains("voice", ignoreCase = true) || msg.contains("synth", ignoreCase = true)) {
                "تعذّر تحويل نص الكشف إلى صوت. يرجى التأكد من تحميل حزمة الصوت العربي وتوفر مساحة كافية."
            } else {
                "تعذّر تجهيز ومشاركة الكشف الصوتي: $msg"
            },
            tone = DialogTone.Danger,
            confirmText = "تحميل الصوت العربي",
            dismissText = "إلغاء",
            onConfirm = {
                exportError = null
                MosaribSpeaker.openInstallTtsData(context)
            },
            onDismiss = { exportError = null }
        )
    }
}
