package com.example.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.CompositionLocalProvider
import com.example.core.util.FileSharingHelper
import com.example.core.util.Formatters
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.mosarib

data class CardBadge(val text: String, val color: Color)

data class CardChip(
    val text: String,
    val container: Color,
    val content: Color,
    val icon: ImageVector? = null,
    val isEndAligned: Boolean = false
)

/**
 * بطاقة موحدة لكافة أنواع العمليات (سند قبض، صرف، خصم، سقي، شراء ساعات).
 */
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
fun OperationCard(
    accent: Color,                    // type colour: border, bar, icon, amount, words
    icon: ImageVector,
    title: String,                    // customer name ("صاحب بئر: X", or category for general expense)
    badges: List<CardBadge>,          // "سند قبض", "سند صرف", "خصم", "سداد سقي", "سقي", "ساقية #2", …
    amountText: String,               // signed and formatted: "+ 60 ر.ي", "- 1,000 ر.ي", "5,000 ر.ي"
    amountInWords: String,            // Formatters.amountToArabicWords(...) — ALWAYS shown
    chips: List<CardChip> = emptyList(),   // session: duration, rate, status
    statement: String? = null,        // البيان; hidden when blank
    meta: String,                     // "4:01 م • نقداً • #EXP-2259"
    extraNote: String? = null,        // used by task 011: "الرصيد بعد العملية: X"
    onClick: () -> Unit,
    actions: (@Composable RowScope.() -> Unit)? = null, // [سداد] and expand chevron [⌄]
    speakText: (() -> String)? = null, // Task 025
    menu: @Composable () -> Unit,     // kebab button + DropdownMenu, placed in the meta row
    footer: (@Composable () -> Unit)? = null, // e.g. the expanded payment-history
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val displayWords = remember(amountText, amountInWords) {
        val ascii = FileSharingHelper.convertArabicDigitsToAscii(amountText)
        val digits = ascii.filter { it.isDigit() || it == '.' }
        val parsedAmount = digits.toDoubleOrNull()
        if (parsedAmount != null && parsedAmount > 0.0) {
            val currency = amountText.trim().split(" ").lastOrNull()?.takeIf { it.contains("ر") } ?: "ر.ي"
            Formatters.amountToArabicWordsApprox(parsedAmount, currency)
        } else {
            amountInWords
                .removePrefix("فقط ")
                .removePrefix("سالب فقط ")
                .removeSuffix(" لا غير")
                .removeSuffix(" لاغير")
                .trim()
        }
    }

    val cardDescription = remember(title, amountText, displayWords) {
        listOfNotNull(
            title,
            amountText,
            displayWords.takeIf { it.isNotBlank() }
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
            .semantics { contentDescription = cardDescription },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.4.dp, accent.copy(alpha = if (isDark) 0.45f else 0.30f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 1. Header row: Accent bar + Icon circle + 2 lines (Name/Amount, Badges/Tafqeet)
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
                        .background(accent)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Icon circle: 36dp
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Text column: Line 1 + Line 2
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // Line 1: Name (weight 1f) and Amount (intrinsic, titleMedium bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
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
                            color = accent,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    // Line 2: Badges (no shrink, no wrap) and Amount in words (weight 1f, end-aligned)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (badges.isNotEmpty()) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                badges.forEach { badge ->
                                    Surface(
                                        color = badge.color.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = badge.text,
                                            style = MaterialTheme.typography.labelSmall.tight().copy(fontWeight = FontWeight.Bold),
                                            color = badge.color,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (displayWords.isNotBlank()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = displayWords,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelMedium.tight().copy(fontWeight = FontWeight.Bold),
                                color = accent
                            )
                        }
                    }
                }
            }

            // 2. Chips row (duration, rate, status) - single row that never wraps
            if (chips.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val startChips = chips.filter { !it.isEndAligned }
                    val endChips = chips.filter { it.isEndAligned }

                    startChips.forEach { chip ->
                        Surface(
                            color = chip.container,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (chip.icon != null) {
                                    Icon(
                                        imageVector = chip.icon,
                                        contentDescription = null,
                                        tint = chip.content,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                Text(
                                    text = chip.text,
                                    style = MaterialTheme.typography.labelSmall.tight().copy(fontWeight = FontWeight.Bold),
                                    color = chip.content,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    if (endChips.isNotEmpty()) {
                        Spacer(modifier = Modifier.weight(1f))
                        endChips.forEach { chip ->
                            Surface(
                                color = chip.container,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (chip.icon != null) {
                                        Icon(
                                            imageVector = chip.icon,
                                            contentDescription = null,
                                            tint = chip.content,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                    Text(
                                        text = chip.text,
                                        style = MaterialTheme.typography.labelSmall.tight().copy(fontWeight = FontWeight.Bold),
                                        color = chip.content,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. البيان box (styled with subtle border and tinted background matching CustomerDetailScreen, 4dp vertical padding)
            if (!statement.isNullOrBlank()) {
                Surface(
                    color = accent.copy(alpha = if (isDark) 0.12f else 0.08f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, accent.copy(alpha = if (isDark) 0.35f else 0.20f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Notes,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "البيان: $statement",
                            style = MaterialTheme.typography.bodySmall.tight(12.sp).copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 4. Meta and actions row (height 32dp with LocalMinimumInteractiveComponentSize 32dp)
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 32.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = meta,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall.tight(),
                        color = MaterialTheme.mosarib.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (actions != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        actions()
                    }

                    if (speakText != null) {
                        Spacer(modifier = Modifier.width(2.dp))
                        com.example.core.speech.SpeakButton(
                            text = speakText,
                            modifier = Modifier.size(32.dp),
                            iconSize = 18.dp
                        )
                    }
                    Spacer(modifier = Modifier.width(2.dp))
                    menu()
                }
            }

            // 5. Extra note (if present)
            if (!extraNote.isNullOrBlank()) {
                Text(
                    text = extraNote,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 6. Footer (e.g. expanded payment history)
            if (footer != null) {
                footer()
            }
        }
    }
}
