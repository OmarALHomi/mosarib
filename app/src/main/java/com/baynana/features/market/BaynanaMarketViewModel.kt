package com.baynana.features.market

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.baynana.core.database.AppDatabase
import com.baynana.data.local.ledger.LedgerHomeRepository
import com.baynana.data.local.market.BrokerListing
import com.baynana.data.local.market.MarketingRequestRow
import com.baynana.data.local.market.MarketRepository
import com.baynana.domain.market.ListingPrivacy
import com.baynana.domain.market.MarketEngine
import com.baynana.domain.market.MarketingRequestStatus
import com.baynana.domain.market.ModerationDecision
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * حالة السوق: ما يُعرض للناس، وما يملكه هذا الجهاز من عروض وطلبات.
 *
 * ملاحظة صدق متعمّدة: السوق اليوم **محلي**. العرض يُنشر في دفترك، وتظهره الشاشة العامة لجهازك،
 * وينتقل إلى الناس عبر المشاركة (واتساب) أو ملف التبادل. ولا يوجد خادم بعد (ح٢٢)، فلا يُوعد
 * المستخدم برؤية الناس لعرضه في «السوق العام» حتى يوجد ما يحقق ذلك.
 */
class BaynanaMarketViewModel(application: Application) : AndroidViewModel(application) {

    data class UiState(
        val loading: Boolean = true,
        /** ما يراه الناس: معروض أو محجوز فقط، وبلا هاتف مزارع بنيويًا. */
        val publicListings: List<MarketEngine.PublicListing> = emptyList(),
        val myListings: List<BrokerListing> = emptyList(),
        val myRequests: List<MarketingRequestRow> = emptyList(),
        /** طلبات وصلتني كدلال: يقرّرها المزارع، وأنا أراها لأعرف لماذا لا يُنشر عرضي. */
        val brokerRequests: List<MarketingRequestRow> = emptyList(),
        val message: String? = null,
        val isWarning: Boolean = false
    )

    private val database: AppDatabase = AppDatabase.getDatabase(application)
    private val repository = MarketRepository(database)
    private val myMemberId = LedgerHomeRepository.MY_MEMBER_ID

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val publicListings = withContext(Dispatchers.IO) { repository.publicListings() }
            _state.value = _state.value.copy(loading = false, publicListings = publicListings)
            collectPrivate()
        }
    }

    private fun collectPrivate() {
        viewModelScope.launch {
            repository.observeMyListings(myMemberId).collect { listings ->
                _state.value = _state.value.copy(myListings = listings)
            }
        }
        viewModelScope.launch {
            repository.observeRequestsOfFarmer(myMemberId).collect { requests ->
                _state.value = _state.value.copy(myRequests = requests)
            }
        }
        viewModelScope.launch {
            val brokerRequests = withContext(Dispatchers.IO) { repository.requestsOfBroker(myMemberId) }
            _state.value = _state.value.copy(brokerRequests = brokerRequests)
        }
    }

    // ------------------------------------------------------------------ الكتابة

    /** حفظ عرض: الإنشاء والتعديل من نفس الباب، والقرار من المحرّك. */
    fun saveListing(draft: MarketEngine.Draft, changesPriceOrBody: Boolean, brokerPhone: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.save(
                    draft = draft,
                    actorMemberId = myMemberId,
                    isFarmer = false,
                    changesPriceOrBody = changesPriceOrBody,
                    brokerPhoneNumber = brokerPhone
                )
            }
            report(result)
            refresh()
        }
    }

    fun submitForReview(id: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { repository.submitForReview(id) }
            report(result)
            refresh()
        }
    }

    fun publish(id: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { repository.publish(id) }
            report(result)
            refresh()
        }
    }

    fun changeStatus(id: String, status: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { repository.changeStatus(id, status) }
            report(result)
            refresh()
        }
    }

    /** المصادقة المحلية: لأداة الإدارة في نسخة التطوير فقط (والوسم يوثّق من قرّر). */
    fun moderateLocally(id: String, revision: Int, decision: String, note: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.moderate(id, revision, decision, note, decidedBy = "local-admin")
            }
            report(result, successNote = "سُجِّلت مصادقة محلية على المراجعة $revision")
            refresh()
        }
    }

    fun requestMarketing(brokerMemberId: String, cropTitle: String, note: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.requestMarketing(
                    id = "req-${java.util.UUID.randomUUID()}",
                    farmerMemberId = myMemberId,
                    brokerMemberId = brokerMemberId,
                    cropTitle = cropTitle,
                    note = note
                )
            }
            report(result, successNote = "أُرسل الطلب للدلال: لن يُنشر عرضك قبل قبولك أنت")
            refresh()
        }
    }

    fun decideRequest(requestId: String, decision: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.decideRequest(requestId, myMemberId, decision)
            }
            report(result, successNote = "قرارك مسجَّل: ${MarketingRequestStatus.label(decision)}")
            refresh()
        }
    }

    suspend fun shareText(id: String): String? = withContext(Dispatchers.IO) { repository.shareText(id) }

    /** النصّ العام لعرض مملوك لي: من المحرّك نفسه، فلا صياغة ثانية في الشاشة. */
    fun publicTextOf(listing: BrokerListing): String =
        MarketEngine.publicText(listing.public)

    private fun report(result: MarketRepository.SaveResult, successNote: String? = null) {
        val (text, warning) = when (result) {
            is MarketRepository.SaveResult.Refused -> result.reason to true
            is MarketRepository.SaveResult.RemoderationRequired ->
                "تعديل بعد النشر: رُفعت المراجعة إلى ${result.revision}، والعرض عاد للمصادقة" to false

            is MarketRepository.SaveResult.Saved -> (successNote ?: when (result.status) {
                "DRAFT" -> "حُفظ كمسودة في دفترك"
                "PENDING_REVIEW" -> "أُرسل للمصادقة: لا يُنشر حتى تُصادَق هذه المراجعة"
                "PUBLISHED" -> "نُشر العرض في دفترك، ويمكنك مشاركته بضغطة"
                else -> "حُفظ (الحالة: ${result.status})"
            }) to false
        }
        _state.value = _state.value.copy(message = text, isWarning = warning)
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }

    companion object {
        /** مكان فارغ جاهز للنماذج: المحافظة تُكتب، والتفصيل يبقى محليًا. */
        fun emptyLocation(): ListingPrivacy.PrivateLocation = ListingPrivacy.PrivateLocation()

        const val MODERATION_LOCAL = "local-admin"

        /** الأدوار المتوقعة في المصادقة — تُعرض في الشاشة فتُعرف الحالة بلا تخمين. */
        val moderationChoices = listOf(
            ModerationDecision.APPROVED,
            ModerationDecision.NEEDS_EDIT,
            ModerationDecision.REJECTED
        )
    }
}
