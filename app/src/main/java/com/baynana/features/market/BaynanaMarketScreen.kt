package com.baynana.features.market

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baynana.data.local.market.BrokerListing
import com.baynana.data.local.market.MarketingRequestRow
import com.baynana.domain.market.ListingStatus
import com.baynana.domain.market.ListingUnit
import com.baynana.domain.market.MarketEngine
import com.baynana.domain.market.MarketingRequestStatus
import com.baynana.domain.market.ModerationDecision
import com.baynana.domain.market.PriceMode
import com.baynana.ui.components.EmptyState
import com.baynana.ui.components.InfoPill
import com.baynana.ui.components.SectionHeader
import com.baynana.ui.components.SyncBadge
import com.baynana.ui.components.amountText
import com.baynana.ui.theme.BaynanaStatus
import kotlinx.coroutines.launch

/**
 * «السوق» الجديد: عرض محصول، لا دفتر ديون.
 *
 * ما يقوله هذا الملف بصراحة للمستخدم (وهو ما يحميه فعلًا):
 * - **لا يظهر رقم هاتفك لأحد**: النوع العام لا يملك حقل هاتف أصلًا، والموقع المنشور محافظة فقط.
 * - **لا يُنشر عرض قبل قبول المزارع**، ولا بعد النشر بلا مصادقة على المراجعة نفسها.
 * - **العرض ليس دَينًا**: لا يُنشئ قيدًا في أي غرفة، ولا يمسّ كشوف الحساب.
 */
@Composable
fun BaynanaMarketScreen(
    viewModel: BaynanaMarketViewModel,
    onBack: (() -> Unit)? = null
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showNewListing by remember { mutableStateOf(false) }
    var moderateTarget by remember { mutableStateOf<Pair<String, Int>?>(null) }
    var editTarget by remember { mutableStateOf<BrokerListing?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(top = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "السوق",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = { showNewListing = true }) { Text("عرض محصول") }
                }
                Text(
                    "عروض المحاصيل: بلا أرقام هواتف، وبلا موقع دقيق، وبلا ديون. " +
                        "التواصل عبر الدلال صاحب العرض.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        state.message?.let { message ->
            item {
                SyncBadge(message, isWarning = state.isWarning)
            }
        }

        item {
            SectionHeader(
                title = "عروض ظاهرة (${state.publicListings.size})",
                subtitle = "هذه العروض تُشارك بضغطة: النصّ نفسه الذي تراه هو الذي يُرسل"
            )
        }

        if (state.publicListings.isEmpty() && !state.loading) {
            item {
                EmptyState(
                    title = "لا عرض معروضًا بعد",
                    body = "المزارع يطلب التسويق، والدلال يوصّف المحصول، والمصادقة تتأكد، وحينها فقط " +
                        "يظهر العرض. لا نُعرض ما لم يُراجَع، ولا نُخفي أن السوق اليوم على هذا الجهاز " +
                        "ويُشارَك بواتساب أو ملف.",
                    actionLabel = "أنشئ عرضًا",
                    onAction = { showNewListing = true }
                )
            }
        }

        items(state.publicListings, key = { it.id }) { listing ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            listing.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        InfoPill(
                            text = ListingStatus.label(listing.status),
                            container = if (listing.status == ListingStatus.RESERVED) {
                                BaynanaStatus.colors.waitingContainer
                            } else {
                                BaynanaStatus.colors.acknowledgedContainer
                            },
                            onContainer = if (listing.status == ListingStatus.RESERVED) {
                                BaynanaStatus.colors.onWaitingContainer
                            } else {
                                BaynanaStatus.colors.onAcknowledgedContainer
                            }
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = buildString {
                            append(listing.cropType)
                            if (listing.quantityNote.isNotBlank()) append(" • ").append(listing.quantityNote)
                            append(" • ").append(listing.governorate)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (listing.priceMode == PriceMode.ON_OFFER) {
                            "على السوم — التفاوض مباشر مع الدلال"
                        } else {
                            "السعر: ${amountText(listing.priceMinor, listing.currency)} (${ListingUnit.label(listing.unit)})"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (listing.description.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(listing.description, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "الدلال: ${listing.brokerName} • ${listing.governorate} • ${MarketEngine.statusLine(listing)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            val text = MarketEngine.publicText(listing)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(Intent.createChooser(intent, "إرسال العرض"))
                        }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                            Text("شارك العرض", style = MaterialTheme.typography.labelMedium)
                        }
                        Text(
                            text = "لا رقم هاتف مزارع في هذه البطاقة",
                            style = MaterialTheme.typography.labelSmall,
                            color = BaynanaStatus.colors.onAcknowledgedContainer,
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .padding(start = 4.dp)
                        )
                    }
                }
            }
        }

        // ------------------------------------------------------- ما يملكه هذا الجهاز

        if (state.myListings.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "عروضي (${state.myListings.size})",
                    subtitle = "ما تراه هنا هو كل شيء: الهاتف والموقع الدقيق لا يخرجان من هذا الجهاز"
                )
            }
            items(state.myListings, key = { it.row.id }) { listing ->
                BrokerListingCard(
                    listing = listing,
                    onEdit = { editTarget = listing },
                    onSubmit = { viewModel.submitForReview(listing.row.id) },
                    onModerate = { moderateTarget = listing.row.id to listing.row.revision },
                    onPublish = { viewModel.publish(listing.row.id) },
                    onShare = {
                        scope.launch {
                            val text = viewModel.shareText(listing.row.id)
                            if (text != null) {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }
                                context.startActivity(Intent.createChooser(intent, "إرسال العرض"))
                            }
                        }
                    },
                    onWithdraw = { viewModel.changeStatus(listing.row.id, ListingStatus.WITHDRAWN) },
                    onMarkSold = { viewModel.changeStatus(listing.row.id, ListingStatus.SOLD) }
                )
            }
        }

        if (state.myRequests.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "طلبات التسويق (${state.myRequests.size})",
                    subtitle = "الطلب ليس دَينًا: قبولك هو ما يسمح بنشر عرضك، ويمكنك سحبه"
                )
            }
            items(state.myRequests, key = { it.id }) { request ->
                RequestCard(request = request, decidedByMe = true) { decision ->
                    viewModel.decideRequest(request.id, decision)
                }
            }
        }

        if (state.brokerRequests.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "طلبات وصلتني كدلال (${state.brokerRequests.size})",
                    subtitle = "لأعرف لماذا لا يُنشر عرض ما: القرار للمزارع وحده"
                )
            }
            items(state.brokerRequests, key = { it.id }) { request ->
                RequestCard(request = request, decidedByMe = false) { }
            }
        }

        item {
            Spacer(Modifier.height(6.dp))
            SyncBadge(
                "السوق اليوم على جهازك: العرض يُنشر في دفترك ويُشارك بواتساب أو ملف تبادل، " +
                    "ولا يوجد خادم عام بعد (ح٢٢)."
            )
            Spacer(Modifier.height(20.dp))
        }
    }

    if (showNewListing) {
        ListingDialog(
            existing = null,
            onDismiss = { showNewListing = false },
            onSave = { draft, changes, phone ->
                viewModel.saveListing(draft, changes, phone)
                showNewListing = false
            }
        )
    }

    editTarget?.let { listing ->
        ListingDialog(
            existing = listing,
            onDismiss = { editTarget = null },
            onSave = { draft, changes, phone ->
                viewModel.saveListing(draft, changes, phone)
                editTarget = null
            }
        )
    }

    moderateTarget?.let { (id, revision) ->
        ModerateDialog(
            revision = revision,
            onDismiss = { moderateTarget = null },
            onDecide = { decision, note ->
                viewModel.moderateLocally(id, revision, decision, note)
                moderateTarget = null
            }
        )
    }
}

/** بطاقة الدلال: فيها ما لا يُنشر — عمدًا، لأن هذه الشاشة له وحده. */
@Composable
private fun BrokerListingCard(
    listing: BrokerListing,
    onEdit: () -> Unit,
    onSubmit: () -> Unit,
    onModerate: () -> Unit,
    onPublish: () -> Unit,
    onShare: () -> Unit,
    onWithdraw: () -> Unit,
    onMarkSold: () -> Unit
) {
    val colors = BaynanaStatus.colors
    val row = listing.row
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(row.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                InfoPill(
                    text = "${ListingStatus.label(row.status)} • مراجعة ${row.revision}",
                    container = MaterialTheme.colorScheme.surfaceVariant,
                    onContainer = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(listing.nextStep, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text(
                text = buildString {
                    append("المزارع: ${row.farmerName.ifBlank { "غير مسمّى" }}")
                    append(" • ").append(row.cropType)
                    if (row.quantityNote.isNotBlank()) append(" • ").append(row.quantityNote)
                    append(" • ").append(row.governorate)
                    if (row.priceMode == PriceMode.FIXED) {
                        append(" • ").append(amountText(row.priceMinor, row.currency))
                        append(" ").append(ListingUnit.label(row.unit))
                    } else {
                        append(" • على السوم")
                    }
                },
                style = MaterialTheme.typography.bodySmall
            )
            listing.contact?.let { contact ->
                if (contact.farmerPhone.isNotBlank() || contact.district.isNotBlank() || contact.village.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = buildString {
                            append("خاص (لا يُنشر): ")
                            if (contact.farmerPhone.isNotBlank()) append("الهاتف محفوظ محليًا")
                            if (contact.district.isNotBlank()) append(" • ").append(contact.district)
                            if (contact.village.isNotBlank()) append(" • ").append(contact.village)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onInfoContainer
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEdit, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                    Text("عدّل", style = MaterialTheme.typography.labelMedium)
                }
                if (listing.canSubmitForReview) {
                    OutlinedButton(onClick = onSubmit, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                        Text("أرسل للمصادقة", style = MaterialTheme.typography.labelMedium)
                    }
                }
                OutlinedButton(onClick = onModerate, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                    Text("مصادقة محلية", style = MaterialTheme.typography.labelMedium)
                }
                if (row.isPublicVisible) {
                    Button(onClick = onShare, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                        Text("شارك", style = MaterialTheme.typography.labelMedium)
                    }
                }
                if (listing.isApproved && !row.isPublicVisible) {
                    Button(onClick = onPublish, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                        Text("انشر", style = MaterialTheme.typography.labelMedium)
                    }
                }
                if (row.status == ListingStatus.RESERVED) {
                    OutlinedButton(onClick = onMarkSold, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                        Text("تم البيع", style = MaterialTheme.typography.labelMedium)
                    }
                }
                if (row.isPublicVisible) {
                    TextButton(onClick = onWithdraw) { Text("اسحب", style = MaterialTheme.typography.labelMedium) }
                }
            }
            if (listing.isApproved && !row.isPublicVisible) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "مصادق على هذه المراجعة — «انشر» هو ما يجعله ظاهرًا للناس",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onAcknowledgedContainer
                )
            }
        }
    }
}

@Composable
private fun RequestCard(
    request: MarketingRequestRow,
    decidedByMe: Boolean,
    onDecide: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    request.cropTitle.ifBlank { "طلب تسويق" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                InfoPill(
                    text = MarketingRequestStatus.label(request.status),
                    container = if (request.isAccepted) {
                        BaynanaStatus.colors.acknowledgedContainer
                    } else {
                        BaynanaStatus.colors.waitingContainer
                    },
                    onContainer = if (request.isAccepted) {
                        BaynanaStatus.colors.onAcknowledgedContainer
                    } else {
                        BaynanaStatus.colors.onWaitingContainer
                    }
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "طلب تسويق ليس دَينًا ولا يُنشئ قيدًا؛ وهو الشرط الذي يسمح بنشر عرضك.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (decidedByMe && !request.isAccepted) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onDecide(MarketingRequestStatus.ACCEPTED) }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                        Text("أقبل", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(onClick = { onDecide(MarketingRequestStatus.REFUSED) }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                        Text("أرفض", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            if (decidedByMe && request.isAccepted) {
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = { onDecide(MarketingRequestStatus.WITHDRAWN) }) {
                    Text("اسحب القبول", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
