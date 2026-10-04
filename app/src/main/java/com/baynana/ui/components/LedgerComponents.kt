package com.baynana.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.baynana.domain.ledger.EntryStatus
import com.baynana.domain.ledger.LineDirection
import com.baynana.domain.money.Currency
import com.baynana.domain.money.Money
import com.baynana.domain.money.MoneyFormat
import com.baynana.domain.ledger.RoomFeedEntry
import com.baynana.ui.theme.BaynanaStatus

/**
 * مكوّنات الدفتر: بطاقة قيد، صفّ غرفة، صافي الرصيد، حالة الفراغ، وشريط المزامنة.
 * كل مبلغ في هذه المكوّنات يمرّ من [MoneyFormat] — لا حساب في الواجهة (ADR-04).
 */

/** مبلغ من الوحدة الصغرى نصًّا. المصدر الوحيد لعرض المال في الواجهة. **دالة نقية** لا
 *  @Composable، لأنها تُستدعى داخل معالِجات الأحداث (onClick) كذلك. */
fun amountText(minor: Long, currencyCode: String): String {
    val currency = Currency.fromCode(currencyCode) ?: return "$minor فلسًا"
    return MoneyFormat.format(Money.ofMinor(kotlin.math.abs(minor), currency))
}

@Composable
fun AmountText(
    minor: Long,
    currencyCode: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Text(text = amountText(minor, currencyCode), modifier = modifier, style = style, color = color)
}

/** ترويسة أرقام: «المبلغ … • المسدَّد … • الباقي …» — من `StatementText` عبر الشاشة. */
@Composable
fun NumbersHeader(
    chargedMinor: Long,
    paidMinor: Long,
    remainingMinor: Long,
    currency: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        HeaderCell("المبلغ", chargedMinor, currency)
        HeaderCell("المسدَّد", paidMinor, currency)
        // سداد زائد: «لك» لا «الباقي» — الكلمة تحمل الاتجاه، فلا يُعرض سالب ولا يُفهم موجبًا خطأً.
        if (remainingMinor < 0L) {
            HeaderCell("لك", -remainingMinor, currency, emphasize = true)
        } else {
            HeaderCell("الباقي", remainingMinor, currency, emphasize = true)
        }
    }
}

@Composable
private fun HeaderCell(label: String, minor: Long, currency: String, emphasize: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        Text(
            text = amountText(minor, currency),
            style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Medium,
            color = if (emphasize) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * بطاقة قيد. القاعدة: المبلغ أولًا (هو ما يبحث عنه العين)، ثم البيان، ثم الحالة، ثم التاريخ.
 * والقيد العكسي يظهر بخطّ منقّط وسطر «قيد عكسي» لأنه ليس حدثًا جديدًا بل تصحيحًا.
 */
@Composable
fun EntryCard(
    entry: RoomFeedEntry,
    currency: String,
    dateText: String,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = BaynanaStatus.colors
    val isCharge = entry.direction == LineDirection.CHARGE
    val amountColor = when {
        entry.status == EntryStatus.VOIDED -> MaterialTheme.colorScheme.onSurfaceVariant
        isCharge -> MaterialTheme.colorScheme.onSurface
        else -> colors.acknowledged
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (entry.isReversal) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = amountText(entry.amountMinor, currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = amountColor
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isCharge) "عليّ" else "لي",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (entry.isReversal) "قيد عكسي — ${entry.description}" else entry.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusLook.EntryChip(entry.status)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            trailing?.let { it() }
        }
    }
}

/** صفّ غرفة في قائمة «غرفي»: من هي، وكم لي/عليّ، وماذا ينتظر. */
@Composable
fun RoomRow(
    title: String,
    subtitle: String,
    netMinor: Long,
    currency: String,
    awaiting: Int,
    statusChip: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    statusChip()
                    if (awaiting > 0) {
                        Spacer(Modifier.width(8.dp))
                        InfoPill(
                            text = "ينتظر إقرارك: $awaiting",
                            container = BaynanaStatus.colors.waitingContainer,
                            onContainer = BaynanaStatus.colors.onWaitingContainer
                        )
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                if (netMinor == 0L) {
                    Text(
                        "مصفّى",
                        style = MaterialTheme.typography.titleSmall,
                        color = BaynanaStatus.colors.acknowledged
                    )
                } else {
                    Text(
                        text = amountText(netMinor, currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (netMinor > 0) BaynanaStatus.colors.acknowledged else MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = if (netMinor > 0) "لي" else "عليّ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** صافي الرصيد بخط كبير: أول ما تراه العين في الرئيسية. */
@Composable
fun NetBalanceCard(
    netMinor: Long,
    currency: String,
    openDebtMinor: Long,
    unappliedMinor: Long,
    modifier: Modifier = Modifier
) {
    val positive = netMinor >= 0
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = if (positive) "لك عند الناس" else "عليك للناس",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = amountText(netMinor, currency),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoPill(
                    text = "ديون مفتوحة: ${amountText(openDebtMinor, currency)}",
                    container = MaterialTheme.colorScheme.surface,
                    onContainer = MaterialTheme.colorScheme.onSurface
                )
                if (unappliedMinor > 0L) {
                    InfoPill(
                        text = "رصيد دائن معلّق: ${amountText(unappliedMinor, currency)}",
                        container = BaynanaStatus.colors.infoContainer,
                        onContainer = BaynanaStatus.colors.onInfoContainer
                    )
                }
            }
        }
    }
}

/** حالة فراغ مصمّمة: جملة تشرح ما العمل + إجراء واحد. */
@Composable
fun EmptyState(
    title: String,
    body: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50)),
            contentAlignment = Alignment.Center
        ) {
            Text("✎", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(14.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/** شريط حالة الحفظ/المزامنة: صادق دائمًا ولا يدّعي نجاحًا سحابيًا. */
@Composable
fun SyncBadge(text: String, isWarning: Boolean = false, modifier: Modifier = Modifier) {
    val colors = BaynanaStatus.colors
    val container = if (isWarning) colors.waitingContainer else colors.infoContainer
    val onContainer = if (isWarning) colors.onWaitingContainer else colors.onInfoContainer
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(container, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = onContainer)
    }
}

/** ترويسة قسم: عنوان صغير وسطر توضيح. */
@Composable
fun SectionHeader(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
