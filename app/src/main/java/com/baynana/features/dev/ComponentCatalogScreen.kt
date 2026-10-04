package com.baynana.features.dev

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baynana.domain.ledger.AllocationPlan
import com.baynana.domain.ledger.EntryStatus
import com.baynana.domain.ledger.EntryType
import com.baynana.domain.ledger.LineDirection
import com.baynana.domain.ledger.MemberStatement
import com.baynana.domain.ledger.StatementDocument
import com.baynana.domain.ledger.StatementDocumentBuilder
import com.baynana.domain.ledger.PlannedAllocation
import com.baynana.domain.ledger.RoomFeedEntry
import com.baynana.domain.ledger.RoomStatus
import com.baynana.domain.ledger.SkippedLine
import com.baynana.domain.ledger.StatementLine
import com.baynana.ui.components.AckBar
import com.baynana.ui.components.AllocationPreview
import com.baynana.ui.components.AmountText
import com.baynana.ui.components.CrisisBanner
import com.baynana.ui.components.EmptyState
import com.baynana.ui.components.EntryCard
import com.baynana.ui.components.EntryStatusChip
import com.baynana.ui.components.InfoPill
import com.baynana.ui.components.NetBalanceCard
import com.baynana.ui.components.NumbersHeader
import com.baynana.ui.components.RoomRow
import com.baynana.ui.components.RoomStatusChip
import com.baynana.ui.components.SectionHeader
import com.baynana.ui.components.StatementSheet
import com.baynana.ui.components.SyncBadge
import com.baynana.ui.components.amountText
import com.baynana.ui.identity.BaynanaWordmark
import com.baynana.ui.theme.BaynanaStatus

/**
 * كاتالوج المكوّنات (د٣): شاشة عيّنات **للمراجعة لا للمستخدم**.
 *
 * الغرض عملي: بدل أن يُكتشف خلل لون أو مقاس حالة على جهاز المالك بعد النشر، تُعرض كل الحالات
 * جنبًا إلى جنب في شاشة واحدة: القيد بحالاته الستّ، حالة الغرفة، ألوان التحذير، الرصيد الدائن،
 * الفراغ، وانقطاع طويل. ومعها أرقام حقيقية من `StatementText` لا أرقام مُختلقة.
 *
 * تُفتح من «المزيد» في **نسخ التطوير فقط** (BuildConfig.DEBUG) فلا تصل إلى المستخدم النهائي.
 */
@Composable
fun ComponentCatalogScreen(onBack: () -> Unit) {
    val status = BaynanaStatus.colors

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                }
                Text(
                    "عيّنات المكوّنات (للمراجعة)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                "شاشة تطوير لا تظهر للمستخدم النهائي. كل رقم هنا يمرّ من `MoneyFormat`/`StatementText` " +
                    "مثل أي شاشة حقيقية.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item { BaynanaWordmark(markSize = 64.dp) }

        item { SectionHeader("المبالغ", "AmountText — المصدر الواحد") }
        item {
            Column {
                AmountText(minor = 12_550_000L, currencyCode = "YER_NEW")
                AmountText(minor = 0L, currencyCode = "YER_NEW", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "نصّ مشاركة: " + amountText(3_500_000L, "YER_NEW"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item { SectionHeader("ترويسة الأرقام والرصيد", "NumbersHeader + NetBalanceCard") }
        item {
            Column {
                NumbersHeader(chargedMinor = 18_000_000L, paidMinor = 12_000_000L, remainingMinor = 6_000_000L, currency = "YER_NEW")
                Spacer(Modifier.height(10.dp))
                NetBalanceCard(netMinor = 12_550_000L, currency = "YER_NEW", openDebtMinor = 6_000_000L, unappliedMinor = 500_000L)
                Spacer(Modifier.height(10.dp))
                NetBalanceCard(netMinor = -4_000_000L, currency = "YER_NEW", openDebtMinor = 9_000_000L, unappliedMinor = 0L)
            }
        }

        item { SectionHeader("شارات الحالة", "لون + رمز + كلمة — ستّ حالات قيد") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    EntryStatus.DRAFT, EntryStatus.SENT, EntryStatus.ACKNOWLEDGED,
                    EntryStatus.DISPUTED, EntryStatus.CHANGE_REQUESTED, EntryStatus.VOIDED
                ).forEach { EntryStatusChip(it) }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RoomStatusChip(RoomStatus.PENDING)
                    RoomStatusChip(RoomStatus.ACTIVE)
                    RoomStatusChip(RoomStatus.CLOSED)
                    RoomStatusChip(RoomStatus.REJECTED)
                }
                InfoPill("رصيد دائن معلّق", status.infoContainer, status.onInfoContainer)
            }
        }

        item { SectionHeader("بطاقات القيود", "EntryCard — قيد عادي، سداد، وقيد عكسي") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EntryCard(entry = sampleEntry(), currency = "YER_NEW", dateText = "2026-10-02")
                EntryCard(
                    entry = sampleEntry(
                        type = EntryType.PAYMENT,
                        direction = LineDirection.PAYMENT,
                        status = EntryStatus.ACKNOWLEDGED,
                        description = "سداد نقدي — قبض عام"
                    ),
                    currency = "YER_NEW",
                    dateText = "2026-09-25"
                )
                EntryCard(
                    entry = sampleEntry(
                        status = EntryStatus.VOIDED,
                        description = "قيد عكسي: تصحيح سقية مكرّرة",
                        isReversal = true
                    ),
                    currency = "YER_NEW",
                    dateText = "2026-09-21"
                )
            }
        }

        item { SectionHeader("شريط الإقرار", "ثلاث لمسات مع شرح ما يحدث") }
        item { AckBar(onAcknowledge = {}, onDispute = {}, onChangeRequested = {}) }

        item { SectionHeader("معاينة التخصيص", "قبل الحفظ: من أي سطر وبكم") }
        item {
            AllocationPreview(
                plan = AllocationPlan(
                    allocations = listOf(
                        PlannedAllocation("entry-1", 4_000_000L),
                        PlannedAllocation("entry-2", 1_000_000L)
                    ),
                    appliedMinor = 5_000_000L,
                    unappliedMinor = 1_500_000L,
                    skipped = listOf(SkippedLine("entry-3", "قيد معترض عليه: يُحلّ الاعتراض أولًا"))
                ),
                currency = "YER_NEW",
                lineTitleFor = { id -> if (id == "entry-1") "سقية 12 آذار" else "دَين سلعة" },
                dateTextFor = { if (it == "entry-1") "2026-03-12" else "2026-04-02" }
            )
        }

        item { SectionHeader("ورقة الكشف", "StatementSheet — هي نفس المستند الذي يُطبع ويُشارَك") }
        item {
            StatementSheet(document = sampleDocument(), onShare = {})
        }

        item { SectionHeader("صفّ الغرفة والفراغ", "RoomRow + EmptyState") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RoomRow(
                    title = "موزّع مياه الوادي",
                    subtitle = "أحمد بن ناصر • كود الربط: K7X2M",
                    netMinor = 6_000_000L,
                    currency = "YER_NEW",
                    awaiting = 2,
                    statusChip = { RoomStatusChip(RoomStatus.ACTIVE) },
                    onClick = {}
                )
                EmptyState(
                    title = "لا غرفة بعد",
                    body = "الغرفة حساب واحد بينك وبين طرف واحد: لا تختلط ديون البقالة بديون الري.",
                    actionLabel = "أنشئ غرفة",
                    onAction = {}
                )
            }
        }

        item { SectionHeader("حالة الحفظ والانقطاع", "SyncBadge + CrisisBanner") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SyncBadge("محفوظ في دفترك على هذا الجهاز")
                SyncBadge("فشل دائم في الإرسال: الخادم غير متاح (ح٢٢)", isWarning = true)
                CrisisBanner(
                    title = "لا اتصال من ٣ أيام",
                    action = "كل ما كتبته محفوظ عندك. سيُرسل تلقائيًا عند عودة الاتصال، ولا تفقد شيئًا.",
                    isDanger = false,
                    onAction = {}
                )
                CrisisBanner(
                    title = "الجرد لا يطابق: فرق ١٥٠٠٠ ر.ي",
                    action = "الترحيل يتوقف حتى يُفسَّر الفرق. راجع العميل المعلَّم في لوحة الترحيل.",
                    isDanger = true,
                    onAction = {}
                )
            }
        }

        item { Spacer(Modifier.height(30.dp)) }
    }
}

private fun sampleEntry(
    type: String = EntryType.WATER_SESSION,
    direction: LineDirection = LineDirection.CHARGE,
    status: String = EntryStatus.SENT,
    description: String = "سقية 5 ساعات — البئر الشرقي",
    isReversal: Boolean = false
) = RoomFeedEntry(
    entryId = "entry-$status-$type",
    type = type,
    description = description,
    occurredAt = 1_759_000_000_000L,
    amountMinor = 4_500_000L,
    remainingMinor = 2_500_000L,
    allocatedMinor = 2_000_000L,
    status = status,
    direction = direction,
    isReversal = isReversal,
    involvesMe = true,
    awaitingMyAck = status == EntryStatus.SENT
)

private fun sampleStatement() = MemberStatement(
    memberId = "me",
    currency = "YER_NEW",
    lines = listOf(
        // الأرقام متناسقة كي تكون العيّنة صادقة: دين 45,000 ر.ي ثم سداد 20,000 ر.ي، فيبقى 25,000.
        StatementLine(
            entryId = "entry-1",
            occurredAt = 1_759_000_000_000L,
            type = EntryType.WATER_SESSION,
            description = "سقية 6 ساعات",
            direction = LineDirection.CHARGE,
            amountMinor = 4_500_000L,
            allocatedMinor = 2_000_000L,
            remainingMinor = 2_500_000L,
            status = EntryStatus.ACKNOWLEDGED,
            runningNetMinor = -4_500_000L
        ),
        StatementLine(
            entryId = "entry-2",
            occurredAt = 1_759_100_000_000L,
            type = EntryType.PAYMENT,
            description = "سداد نقدي",
            direction = LineDirection.PAYMENT,
            amountMinor = 2_000_000L,
            allocatedMinor = 2_000_000L,
            remainingMinor = 0L,
            status = EntryStatus.ACKNOWLEDGED,
            runningNetMinor = -2_500_000L
        )
    ),
    chargedMinor = 4_500_000L,
    paidMinor = 2_000_000L,
    remainingMinor = 2_500_000L,
    unappliedMinor = 500_000L,
    netMinor = 2_500_000L
)

/** مستند جاهز للعيّنة: يُبنى من نفس المسار الذي تبني به الشاشة الحقيقية. */
private fun sampleDocument(): StatementDocument = StatementDocumentBuilder.build(
    statement = sampleStatement(),
    roomTitle = "بيت الوالد",
    subject = "كشف: أحمد",
    appVersion = "1.0",
    fileNameStamp = "2026-04-02",
    dateText = { at -> if (at == 1_759_000_000_000L) "2026-03-12" else "2026-04-02" }
)
