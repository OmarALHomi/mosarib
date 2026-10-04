package com.baynana.features.market

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baynana.data.local.ledger.LedgerHomeRepository
import com.baynana.data.local.market.BrokerListing
import com.baynana.domain.market.ListingPrivacy
import com.baynana.domain.market.ListingUnit
import com.baynana.domain.market.MarketEngine
import com.baynana.domain.market.ModerationDecision
import com.baynana.domain.market.PriceMode
import com.baynana.domain.money.Currency
import com.baynana.domain.money.Money
import com.baynana.domain.money.MoneyFormat
import com.baynana.domain.money.MoneyParser
import com.baynana.ui.components.SyncBadge
import java.util.UUID

/**
 * حوارا السوق: وصف عرض، ومصادقة.
 *
 * قاعدتان تظهران للمستخدم في الحوار نفسه لا في مستند داخلي:
 * 1. **ما تكتبه يُفحص فورًا** بروح `MarketEngine`: عنوان ووصف وكمية بلا أرقام تواصل وبلا ذكر ديون،
 *    فيرى الكاتب السبب قبل الحفظ لا بعده.
 * 2. **الموقع المنشور محافظة فقط**، وما تحته (مديرية/قرية/علامة) محلي لا يخرج.
 */

private fun pricePreview(minor: Long, currency: String): String {
    val code = Currency.fromCode(currency) ?: return "$minor فلسًا"
    return MoneyFormat.format(Money.ofMinor(minor, code))
}

@Composable
fun ListingDialog(
    existing: BrokerListing?,
    onDismiss: () -> Unit,
    onSave: (MarketEngine.Draft, Boolean, String) -> Unit
) {
    val row = existing?.row
    var title by remember { mutableStateOf(row?.title.orEmpty()) }
    var cropType by remember { mutableStateOf(row?.cropType.orEmpty()) }
    var quantityNote by remember { mutableStateOf(row?.quantityNote.orEmpty()) }
    var description by remember { mutableStateOf(row?.description.orEmpty()) }
    var priceMode by remember { mutableStateOf(row?.priceMode ?: PriceMode.ON_OFFER) }
    var priceText by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf(row?.unit ?: ListingUnit.WHOLE_LOT) }
    var governorate by remember { mutableStateOf(row?.governorate.orEmpty()) }
    var district by remember { mutableStateOf(existing?.contact?.district.orEmpty()) }
    var village by remember { mutableStateOf(existing?.contact?.village.orEmpty()) }
    var farmerName by remember { mutableStateOf(row?.farmerName.orEmpty()) }
    var farmerPhone by remember { mutableStateOf(existing?.contact?.farmerPhone.orEmpty()) }
    var brokerName by remember { mutableStateOf(row?.brokerName.orEmpty()) }
    var brokerPhone by remember { mutableStateOf(existing?.contact?.brokerPhone.orEmpty()) }
    var showBrokerPhone by remember {
        mutableStateOf(row?.brokerPhoneChoice == MarketEngine.PublicPhoneChoice.SHOWN.name)
    }
    var farmerAllowsPhone by remember { mutableStateOf(existing?.contact?.farmerAllowsPublicPhone == true) }
    var scanMessage by remember { mutableStateOf<String?>(null) }

    val scan = remember(title, description, quantityNote) {
        ListingPrivacy.scanAll(title, description, quantityNote)
    }
    val safe = scan.safe

    val parsedPrice: Long? = if (priceMode == PriceMode.ON_OFFER) {
        0L
    } else {
        val currency = Currency.fromCode(row?.currency ?: "YER_NEW")
        when (val parsed = currency?.let { MoneyParser.parse(priceText, it) }) {
            is com.baynana.domain.money.MoneyParse.Ok -> parsed.money.minor
            else -> null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "عرض محصول" else "تعديل العرض", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.heightIn(max = 430.dp).verticalScroll(rememberScrollState())) {
                Text(
                    "العرض بيان محصول، لا كشف حساب: لا مبالغ ديون ولا أرقام هواتف في العنوان والوصف.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(title, { title = it }, label = { Text("العنوان — مثال: رمان صنف ممتاز") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(cropType, { cropType = it }, label = { Text("نوع المحصول") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(quantityNote, { quantityNote = it }, label = { Text("الكمية — مثال: 40 كرتون") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(description, { description = it }, label = { Text("الوصف") }, modifier = Modifier.fillMaxWidth())

                if (!safe) {
                    Spacer(Modifier.height(8.dp))
                    SyncBadge(scan.summary, isWarning = true)
                }

                Spacer(Modifier.height(10.dp))
                Text("السعر", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceChip("على السوم", priceMode == PriceMode.ON_OFFER) { priceMode = PriceMode.ON_OFFER }
                    ChoiceChip("سعر معلن", priceMode == PriceMode.FIXED) { priceMode = PriceMode.FIXED }
                }
                if (priceMode == PriceMode.FIXED) {
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(priceText, { priceText = it }, label = { Text("المبلغ بالريال") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    if (parsedPrice != null && parsedPrice > 0L) {
                        Text(
                            "سيُحفظ: ${pricePreview(parsedPrice, row?.currency ?: "YER_NEW")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (priceText.isNotBlank()) {
                        Text("مبلغ غير مقروء — اكتبه رقمًا بلا حروف", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("وحدة البيع", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(ListingUnit.WHOLE_LOT, ListingUnit.SACK, ListingUnit.KILO, ListingUnit.CARTON).forEach { option ->
                            ChoiceChip(ListingUnit.label(option), unit == option) { unit = option }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text("الموقع", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                OutlinedTextField(governorate, { governorate = it }, label = { Text("المحافظة (هي المنشورة)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(district, { district = it }, label = { Text("المديرية") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(village, { village = it }, label = { Text("القرية") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Text(
                    "المديرية والقرية تُحفظان عندك للتنقّل، ولا تُنشران: المنشور محافظة فقط.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(10.dp))
                Text("الأطراف والهواتف", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                OutlinedTextField(farmerName, { farmerName = it }, label = { Text("اسم المزارع (كما تعرفه)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(farmerPhone, { farmerPhone = it }, label = { Text("هاتف المزارع — لا يُنشر أبدًا") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(brokerName, { brokerName = it }, label = { Text("اسم الدلال (المنشور)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(brokerPhone, { brokerPhone = it }, label = { Text("هاتف الدلال") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    ChoiceChip("يُنشر رقمي", showBrokerPhone) { showBrokerPhone = !showBrokerPhone }
                    Text(
                        if (showBrokerPhone) "سيكون رقمك قناة التواصل في العرض" else "بلا نشر رقمك: التواصل عبر الطلب",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    ChoiceChip("المزارع أذن بالتواصل المباشر", farmerAllowsPhone) { farmerAllowsPhone = !farmerAllowsPhone }
                }
                Text(
                    "حتى مع الإذن: لا يُنشر رقم المزارع؛ يُفتح باب طلب له ويبقى قادرًا على سحبه.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                enabled = safe && title.isNotBlank() && cropType.isNotBlank() && governorate.isNotBlank() &&
                    (priceMode == PriceMode.ON_OFFER || (parsedPrice ?: 0L) > 0L),
                onClick = {
                    val draft = MarketEngine.Draft(
                        id = row?.id ?: "listing-${UUID.randomUUID()}",
                        brokerMemberId = LedgerHomeRepository.MY_MEMBER_ID,
                        // الطرف المزارع يُكتب اسمه؛ ومعرّفه المحلي يُشتق من الاسم وحده حتى لا يتغيّر
                        // بتغيّر الحوار: فالعرض نفسه يبقى مرتبطًا بالمزارع نفسه بين الجلسات.
                        farmerMemberId = row?.farmerMemberId
                            ?: "farmer-" + (farmerName.trim().ifBlank { "بدون-اسم" }),
                        farmerName = farmerName.trim(),
                        farmerPhone = farmerPhone.trim(),
                        farmerAllowsPublicPhone = farmerAllowsPhone,
                        brokerName = brokerName.trim(),
                        brokerPhone = if (showBrokerPhone) MarketEngine.PublicPhoneChoice.SHOWN else MarketEngine.PublicPhoneChoice.HIDDEN,
                        title = title.trim(),
                        cropType = cropType.trim(),
                        description = description.trim(),
                        quantityNote = quantityNote.trim(),
                        priceMode = priceMode,
                        priceMinor = if (priceMode == PriceMode.FIXED) parsedPrice ?: 0L else 0L,
                        currency = row?.currency ?: "YER_NEW",
                        unit = unit,
                        location = ListingPrivacy.PrivateLocation(
                            governorate = governorate.trim(),
                            district = district.trim(),
                            village = village.trim()
                        ),
                        photos = emptyList(),
                        createdAt = row?.createdAt ?: System.currentTimeMillis()
                    )
                    val changedBody = existing == null || row == null ||
                        row.title != draft.title || row.cropType != draft.cropType ||
                        row.description != draft.description || row.quantityNote != draft.quantityNote ||
                        row.priceMode != draft.priceMode || row.priceMinor != draft.priceMinor ||
                        row.unit != draft.unit || row.governorate != draft.location.governorate
                    onSave(draft, changedBody, brokerPhone.trim())
                }
            ) { Text(if (existing == null) "احفظ في دفتري" else "احفظ التعديل") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
fun ModerateDialog(
    revision: Int,
    onDismiss: () -> Unit,
    onDecide: (String, String) -> Unit
) {
    var decision by remember { mutableStateOf(ModerationDecision.APPROVED) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("مصادقة على المراجعة $revision", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "المصادقة تسري على هذه المراجعة بعينها. أي تعديل على السعر أو البيان يرفع المراجعة، " +
                        "فلا تسري عليها هذه المصادقة.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    moderationChoices.forEach { (value, label) ->
                        ChoiceChip(label, decision == value) { decision = value }
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(note, { note = it }, label = { Text("ملاحظة (تُقرأ للدلال)") }, modifier = Modifier.fillMaxWidth())
                if (decision != ModerationDecision.APPROVED && note.isBlank()) {
                    Text(
                        "الرفض أو طلب التعديل يحتاج سببًا مكتوبًا — لا يُترك الدلال يخمّن",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = decision == ModerationDecision.APPROVED || note.isNotBlank(),
                onClick = { onDecide(decision, note) }
            ) { Text("سجّل القرار") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

/** قرارات المصادقة كما تُعرض: القيمة التقنية مع تسميتها العربية. */
private val moderationChoices = listOf(
    ModerationDecision.APPROVED to "مصادق",
    ModerationDecision.NEEDS_EDIT to "يحتاج تعديلًا",
    ModerationDecision.REJECTED to "مرفوض"
)

@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    } else {
        OutlinedButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}
