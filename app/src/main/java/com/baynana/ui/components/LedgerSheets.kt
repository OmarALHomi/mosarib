package com.baynana.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baynana.domain.ledger.AllocationPlan
import com.baynana.domain.ledger.LineDirection
import com.baynana.domain.ledger.MemberStatement
import com.baynana.domain.ledger.StatementLine
import com.baynana.domain.ledger.StatementText
import com.baynana.ui.theme.BaynanaStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * مكوّنات الدفتر المتبقّية من مكتبة التصميم (د٣): شريط الإقرار، معاينة التخصيص، ورقة الكشف،
 * ولافتة الحالة الحرجة.
 *
 * القاعدة الجامعة: **كل نصّ مالي يُبنى في `StatementText`**، وكل رقم حالة يُقرأ من `BaynanaStatus`.
 * فلا صياغة ثانية هنا، ولا لون بلا معنى.
 */

/** نصوص ثابتة لشريط الإقرار: الأزرار الثلاثة بكلماتها ومساراتها. */
object AckLabels {
    const val ACKNOWLEDGE = "أقرّ"
    const val DISPUTE = "أعترض"
    const val CHANGE = "طلب تعديل"
    const val ACKNOWLEDGE_HINT = "الإقرار يثبّت القيد كما هو، ولا يمسحه ولا يجمّده"
    const val DISPUTE_HINT = "الاعتراض يبقي القيد ظاهرًا للطرفين مع سببك المكتوب"
    const val CHANGE_HINT = "طلب التعديل يفتح نقاشًا، والقيد يبقى معلّقًا حتى يُحلّ"
}

/**
 * شريط الإقرار: ثلاث لمسات واضحة، وتحتها سطر يقول ماذا يحدث فعلًا.
 *
 * لماذا سطر الشرح تحت كل زر: أكثر ما يخيف المستخدم في تطبيق حسابات هو ظنّ أن «أعترض» يمحو
 * أو أن «أقرّ» يقفل الباب. فالكلمة تُشرح في مكانها، لا في صفحة مساعدة.
 */
@Composable
fun AckBar(
    onAcknowledge: () -> Unit,
    onDispute: () -> Unit,
    onChangeRequested: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AckAction(
                label = AckLabels.ACKNOWLEDGE,
                icon = Icons.Filled.Check,
                container = BaynanaStatus.colors.acknowledgedContainer,
                content = BaynanaStatus.colors.onAcknowledgedContainer,
                enabled = enabled,
                onClick = onAcknowledge
            )
            AckAction(
                label = AckLabels.DISPUTE,
                icon = Icons.Filled.ReportProblem,
                container = BaynanaStatus.colors.dangerContainer,
                content = BaynanaStatus.colors.onDangerContainer,
                enabled = enabled,
                onClick = onDispute
            )
            AckAction(
                label = AckLabels.CHANGE,
                icon = Icons.Filled.Rule,
                container = BaynanaStatus.colors.waitingContainer,
                content = BaynanaStatus.colors.onWaitingContainer,
                enabled = enabled,
                onClick = onChangeRequested
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "أقرّ: يثبّت. أعترض: يبقى مع سببك. طلب تعديل: يفتح التصحيح. لا شيء منها يمحو قيدًا.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AckAction(
    label: String,
    icon: ImageVector,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .background(
                color = if (enabled) container else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) content else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        TextButton(onClick = onClick, enabled = enabled) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = content, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * معاينة التخصيص: ما سيُخصم من أي سطر وبكم، وما سيُعلَّق كرصيد دائن — **قبل** التنفيذ.
 *
 * لماذا هذا المكوّن إلزامي: التزام «قبض عام» لا يقفل دَينًا بعينه، فالمستخدم يجب أن يرى نتيجة
 * اختياره قبل أن يضغط. الشاشة تترجم `AllocationPlan` من المحرّك، ولا تحسب شيئًا بنفسها.
 */
@Composable
fun AllocationPreview(
    plan: AllocationPlan,
    currency: String,
    lineTitleFor: (String) -> String,
    dateTextFor: (String) -> String,
    modifier: Modifier = Modifier
) {
    val colors = BaynanaStatus.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.infoContainer, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Info, contentDescription = null, tint = colors.onInfoContainer, modifier = Modifier.width(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                "قبل الحفظ: هذا ما سيحدث",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = colors.onInfoContainer
            )
        }
        Spacer(Modifier.height(6.dp))

        if (plan.isEmpty && plan.unappliedMinor == 0L) {
            Text(
                "لا سطر سيُخصم عليه: لا دَين مقابل في هذه الغرفة والعملة.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onInfoContainer
            )
        }

        plan.allocations.forEach { allocation ->
            Text(
                text = "• ${lineTitleFor(allocation.debtEntryId)} — ${amountText(allocation.amountMinor, currency)}" +
                    dateTextFor(allocation.debtEntryId).takeIf { it.isNotBlank() }?.let { " ($it)" }.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onInfoContainer
            )
        }

        if (plan.appliedMinor > 0L) {
            Spacer(Modifier.height(4.dp))
            Text(
                "المسدَّد الآن: ${amountText(plan.appliedMinor, currency)}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = colors.onInfoContainer
            )
        }

        if (plan.unappliedMinor > 0L) {
            Spacer(Modifier.height(4.dp))
            Text(
                "يبقى رصيدًا دائنًا في هذه الغرفة: ${amountText(plan.unappliedMinor, currency)} " +
                    "— لا يُنقل لطرف ثالث ولا يُحوَّل لعملة أخرى.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onInfoContainer
            )
        }

        plan.skipped.forEach { skipped ->
            Text(
                "• لم يُخصَّص من ${skipped.entryId}: ${skipped.reason}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * ورقة الكشف: العرض البصري لكشف عضو، ونصّه من `StatementText` نفسه.
 *
 * الفائدة الحقيقية: ما تراه على الشاشة هو **حرفيًا** ما يخرج في المشاركة/الطباعة، لأن كليهما يقرأ
 * من الدالة نفسها. فلا يحدث أن يكون رصيد الشاشة مخالفًا لرصيد الورق.
 */
@Composable
fun StatementSheet(
    statement: MemberStatement,
    title: String,
    subtitle: String? = null,
    onShare: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    dateFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                subtitle?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            onShare?.let { action ->
                TextButton(onClick = action) { Text("شارك النصّ", style = MaterialTheme.typography.labelMedium) }
            }
        }

        Spacer(Modifier.height(10.dp))
        NumbersHeader(
            chargedMinor = statement.chargedMinor,
            paidMinor = statement.paidMinor,
            remainingMinor = statement.remainingMinor,
            currency = statement.currency
        )
        Spacer(Modifier.height(8.dp))
        Text(
            // النصّ المخزَّن للمشاركة: نقتبس أول سطر منه ليُعرف أن الشاشة والورق نصّ واحد
            text = StatementText.summary(statement),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))

        statement.lines.forEach { line ->
            StatementRow(line = line, currency = statement.currency, dateFormat = dateFormat)
        }

        if (statement.unappliedMinor > 0L) {
            Spacer(Modifier.height(8.dp))
            Text(
                "رصيد دائن معلّق: ${amountText(statement.unappliedMinor, statement.currency)} — لم يُخصَّص على دَين بعد",
                style = MaterialTheme.typography.bodySmall,
                color = BaynanaStatus.colors.onInfoContainer
            )
        }
    }
}

@Composable
private fun StatementRow(line: StatementLine, currency: String, dateFormat: SimpleDateFormat) {
    val isCharge = line.direction == LineDirection.CHARGE
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                line.description.ifBlank { line.type },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2
            )
            Text(
                "${dateFormat.format(Date(line.occurredAt))} • المتبقي ${amountText(line.remainingMinor, currency)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = (if (isCharge) "" else "− ") + amountText(line.amountMinor, currency),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (isCharge) MaterialTheme.colorScheme.onSurface else BaynanaStatus.colors.acknowledged
            )
            // الرصيد الجاري بعد هذا السطر: «كم عليّ حتى هنا؟» — رقم واحد يجيب بلا جمع ذهني.
            Text(
                text = when {
                    line.runningNetMinor == 0L -> "مصفّى حتى هنا"
                    line.runningNetMinor < 0L -> "عليك ${amountText(line.runningNetMinor, currency)}"
                    else -> "لك ${amountText(line.runningNetMinor, currency)}"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * لافتة الحالة الحرجة: انقطاع طويل، أو فرق في الجرد، أو فشل دائم يحتاج تدخّلًا.
 *
 * قاعدتها: **لا تُخيف بلا سبب، ولا تطمئن بلا حقيقة** — سطر واحد يقول ما جرى، وسطر يقول ما العمل.
 */
@Composable
fun CrisisBanner(
    title: String,
    action: String,
    isDanger: Boolean = false,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = BaynanaStatus.colors
    val container = if (isDanger) colors.dangerContainer else colors.waitingContainer
    val content = if (isDanger) colors.onDangerContainer else colors.onWaitingContainer
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(container, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isDanger) Icons.Filled.ReportProblem else Icons.Filled.Info,
                contentDescription = null,
                tint = content,
                modifier = Modifier.width(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = content)
        }
        Spacer(Modifier.height(4.dp))
        Text(action, style = MaterialTheme.typography.bodySmall, color = content)
        onAction?.let { handler ->
            TextButton(onClick = handler) {
                Text("الحل", style = MaterialTheme.typography.labelMedium, color = content, fontWeight = FontWeight.Bold)
            }
        }
    }
}
