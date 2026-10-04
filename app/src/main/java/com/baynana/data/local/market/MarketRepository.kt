package com.baynana.data.local.market

import androidx.room.withTransaction
import com.baynana.core.database.AppDatabase
import com.baynana.domain.market.ListingPrivacy
import com.baynana.domain.market.ListingStatus
import com.baynana.domain.market.MarketEngine
import com.baynana.domain.market.MarketingRequestStatus
import com.baynana.domain.market.ModerationDecision
import com.baynana.domain.market.PriceMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * مستودع السوق: التخزين والانتقالات، والقرار من `MarketEngine` وحده.
 *
 * القاعدتان الحاكمتان هنا:
 * 1. **لا نشر بلا مصادقة على المراجعة الحالية** — تُفحص في [publish] بالاستدعاء إلى
 *    `MarketEngine.checkPublish`، ولا يوجد طريق ثانٍ إلى `PUBLISHED`.
 * 2. **لا هاتف مزارع في أي مخرج عام** — الشاشة العامة تُبنى من [MarketListingRow] (بلا هاتف)،
 *    والنوع العام `PublicListing` لا يملك حقل هاتف أصلًا. جدول [MarketContactRow] لا يُقرأ إلا
 *    لصاحب العرض أو لإجراء داخلي.
 */
class MarketRepository(private val db: AppDatabase) {

    private val dao get() = db.marketDao()

    // ------------------------------------------------------------------ للعرض العام

    /** عروض السوق العامة، جاهزة للشاشة والمشاركة: بلا هاتف مزارع ولا موقع دقيق. */
    fun observePublic(): Flow<List<MarketEngine.PublicListing>> =
        dao.observePublicListings().map { rows -> rows.map { it.toPublic(dao.getContact(it.id)) } }

    suspend fun publicListings(): List<MarketEngine.PublicListing> =
        dao.getPublicListings().map { it.toPublic(dao.getContact(it.id)) }

    /** أحدث مراجعة مُصادَق عليها لكل عرض — لبناء عرض عام «مصادق عليه» في تصدير أو تقرير. */
    suspend fun publicListingsWithModeration(): List<Pair<MarketEngine.PublicListing, ModerationRow?>> =
        dao.getPublicListings().map { row ->
            row.toPublic(dao.getContact(row.id)) to dao.getModeration(row.id, row.revision)
        }

    // ------------------------------------------------------------------ عند الدلال

    fun observeMyListings(brokerMemberId: String): Flow<List<BrokerListing>> =
        dao.observeListingsOfBroker(brokerMemberId).map { rows -> rows.map { it.toBrokerView() } }

    suspend fun brokerListing(id: String): BrokerListing? = dao.getListing(id)?.toBrokerView()

    /** ملخّص داخلي للدلال: ما لا يُنشر (الموقع الدقيق وهاتف المزارع) يبقى في هذه الشاشة وحدها. */
    suspend fun internalSummary(id: String): String? {
        val row = dao.getListing(id) ?: return null
        return MarketEngine.internalSummary(row.toDraft(dao.getContact(id)))
    }

    // ------------------------------------------------------------------ الكتابة

    sealed interface SaveResult {
        /** حُفظت المسودة (جديدة أو محدَّثة) بلا حاجة إلى مصادقة جديدة. */
        data class Saved(val listingId: String, val revision: Int, val status: String) : SaveResult

        /** حُفظ تعديل جوهري بعد النشر: رُفعت المراجعة وعاد العرض إلى المصادقة. */
        data class RemoderationRequired(val listingId: String, val revision: Int) : SaveResult

        data class Refused(val reason: String) : SaveResult
    }

    /**
     * يحفظ عرضًا (إنشاء أو تعديل). القرار من `MarketEngine.checkEdit`:
     * - تعديل السعر/البيان بعد النشر ⇒ مراجعة جديدة ومصادقة جديدة.
     * - عرض مبيع أو مسحوب ⇒ يُرفض بأثر معلن.
     * - المزارع يعدّل قراره ومساره وحده، لا السعر.
     */
    suspend fun save(
        draft: MarketEngine.Draft,
        actorMemberId: String,
        isFarmer: Boolean,
        changesPriceOrBody: Boolean,
        /** رقم الدلال نفسه؛ يُحفظ دائمًا محليًا، ولا يُنشر إلا إذا اختار الدلال [MarketEngine.PublicPhoneChoice.SHOWN]. */
        brokerPhoneNumber: String = "",
        now: Long = System.currentTimeMillis()
    ): SaveResult = db.withTransaction {
        val existing = dao.getListing(draft.id)
        val previousPublic = existing?.let { it.toPublic(dao.getContact(it.id)) }

        when (val check = MarketEngine.checkEdit(previousPublic, draft, actorMemberId, isFarmer, changesPriceOrBody)) {
            is MarketEngine.EditCheck.Refused -> SaveResult.Refused(check.reason)
            is MarketEngine.EditCheck.RequiresRemoderation -> {
                write(
                    draft = draft,
                    revision = check.nextRevision,
                    status = ListingStatus.PENDING_REVIEW,
                    brokerPhoneNumber = brokerPhoneNumber,
                    now = now
                )
                SaveResult.RemoderationRequired(draft.id, check.nextRevision)
            }

            MarketEngine.EditCheck.Allowed -> {
                val status = when {
                    existing == null -> ListingStatus.DRAFT
                    isFarmer && !changesPriceOrBody -> existing.status
                    existing.status == ListingStatus.REJECTED -> ListingStatus.DRAFT
                    else -> existing.status
                }
                write(
                    draft = draft,
                    revision = existing?.revision ?: 1,
                    status = status,
                    brokerPhoneNumber = brokerPhoneNumber,
                    now = now
                )
                SaveResult.Saved(draft.id, existing?.revision ?: 1, status)
            }
        }
    }

    suspend fun submitForReview(id: String): SaveResult = db.withTransaction {
        val row = dao.getListing(id) ?: return@withTransaction SaveResult.Refused("عرض غير موجود")
        if (row.status == ListingStatus.SOLD) return@withTransaction SaveResult.Refused("المحصول بِيع: لا يُعاد للمصادقة")
        if (row.status == ListingStatus.WITHDRAWN) {
            return@withTransaction SaveResult.Refused("العرض مسحوب: أنشئ عرضًا جديدًا")
        }
        // لا إرسال للمصادقة بلا طلب تسويق مقبول من المزارع — النشر بلا قبله ممنوع أصلًا.
        val request = dao.getRequestBetween(row.farmerMemberId, row.brokerMemberId)
        if (request == null || !request.isAccepted) {
            return@withTransaction SaveResult.Refused(
                "لا يُرسل عرض للمصادقة قبل قبول المزارع للتسويق"
            )
        }
        val nextStatus = if (row.status == ListingStatus.PUBLISHED || row.status == ListingStatus.RESERVED) {
            row.status
        } else {
            ListingStatus.PENDING_REVIEW
        }
        dao.upsertListing(row.copy(status = nextStatus, updatedAt = System.currentTimeMillis()))
        SaveResult.Saved(row.id, row.revision, nextStatus)
    }

    /**
     * المصادقة على مراجعة بعينها. [decidedBy] يوثّق من قرّر: `platform` لجهة المصادقة،
     * و`local-admin` لأداة الإدارة في نسخة التطوير — والفرق محفوظ ولا يُطمس.
     */
    suspend fun moderate(
        listingId: String,
        revision: Int,
        decision: String,
        note: String,
        decidedBy: String = "platform",
        now: Long = System.currentTimeMillis()
    ): SaveResult = db.withTransaction {
        val row = dao.getListing(listingId) ?: return@withTransaction SaveResult.Refused("عرض غير موجود")
        if (revision != row.revision) {
            return@withTransaction SaveResult.Refused(
                "المصادقة على مراجعة قديمة ($revision) لا تسري على المراجعة الحالية (${row.revision})"
            )
        }
        dao.upsertModeration(
            ModerationRow(
                listingId = listingId,
                revision = revision,
                decision = decision,
                decidedAt = now,
                note = note,
                decidedBy = decidedBy
            )
        )
        val nextStatus = when (decision) {
            ModerationDecision.APPROVED -> row.status
            ModerationDecision.REJECTED -> ListingStatus.REJECTED
            else -> ListingStatus.PENDING_REVIEW
        }
        if (nextStatus != row.status) dao.upsertListing(row.copy(status = nextStatus, updatedAt = now))
        SaveResult.Saved(listingId, revision, nextStatus)
    }

    /** النشر: لا يمرّ إلا بمصادقة `APPROVED` على **المراجعة الحالية**، وبفحص المحرّك الكامل. */
    suspend fun publish(id: String, now: Long = System.currentTimeMillis()): SaveResult = db.withTransaction {
        val row = dao.getListing(id) ?: return@withTransaction SaveResult.Refused("عرض غير موجود")
        val draft = row.toDraft(dao.getContact(id))
        val request = dao.getRequestBetween(row.farmerMemberId, row.brokerMemberId)?.toEngineRequest()
        val moderation = dao.getModeration(row.id, row.revision)?.toEngineModeration()

        when (val check = MarketEngine.checkPublish(draft, request, moderation, row.revision, row.status)) {
            is MarketEngine.Check.Refused -> SaveResult.Refused(check.reason)
            is MarketEngine.Check.Allowed -> {
                dao.upsertListing(
                    row.copy(
                        status = ListingStatus.PUBLISHED,
                        publishedAt = row.publishedAt ?: now,
                        updatedAt = now
                    )
                )
                SaveResult.Saved(row.id, row.revision, ListingStatus.PUBLISHED)
            }
        }
    }

    /** انتقالات الحالة بعد النشر: حجز لصلح قائم، بيع، سحب معلن. لا حذف لأي عرض. */
    suspend fun changeStatus(id: String, status: String, now: Long = System.currentTimeMillis()): SaveResult =
        db.withTransaction {
            val row = dao.getListing(id) ?: return@withTransaction SaveResult.Refused("عرض غير موجود")
            if (row.status == ListingStatus.SOLD) {
                return@withTransaction SaveResult.Refused("العرض مبيع: لا تتغيّر حالته")
            }
            if (status == ListingStatus.SOLD && row.status != ListingStatus.RESERVED) {
                return@withTransaction SaveResult.Refused("البيع يُسجَّل من عرض محجوز لصلح قائم")
            }
            dao.upsertListing(
                row.copy(
                    status = status,
                    updatedAt = now,
                    closedAt = if (status == ListingStatus.SOLD) now else row.closedAt
                )
            )
            SaveResult.Saved(id, row.revision, status)
        }

    // ------------------------------------------------------------------ طلبات التسويق

    suspend fun requestMarketing(
        id: String,
        farmerMemberId: String,
        brokerMemberId: String,
        cropTitle: String,
        note: String = "",
        now: Long = System.currentTimeMillis()
    ): SaveResult = db.withTransaction {
        if (farmerMemberId == brokerMemberId) {
            return@withTransaction SaveResult.Refused("لا يطلب الطرف التسويق من نفسه")
        }
        val existing = dao.getRequestBetween(farmerMemberId, brokerMemberId)
        if (existing != null && existing.isAccepted) {
            return@withTransaction SaveResult.Refused("الطلب مقبول أصلًا بين الطرفين نفسهما")
        }
        dao.upsertRequest(
            MarketingRequestRow(
                id = id,
                farmerMemberId = farmerMemberId,
                brokerMemberId = brokerMemberId,
                cropTitle = cropTitle.trim(),
                status = MarketingRequestStatus.REQUESTED,
                requestedAt = now,
                note = note.trim()
            )
        )
        SaveResult.Saved(id, 1, MarketingRequestStatus.REQUESTED)
    }

    /** قرار المزارع وحده على الطلب: قبول أو رفض أو سحب. */
    suspend fun decideRequest(
        requestId: String,
        farmerMemberId: String,
        decision: String,
        now: Long = System.currentTimeMillis()
    ): SaveResult = db.withTransaction {
        val row = dao.getRequest(requestId) ?: return@withTransaction SaveResult.Refused("طلب غير موجود")
        if (row.farmerMemberId != farmerMemberId) {
            return@withTransaction SaveResult.Refused("قرار الطلب للمزارع صاحب الطلب وحده")
        }
        if (decision !in listOf(
                MarketingRequestStatus.ACCEPTED,
                MarketingRequestStatus.REFUSED,
                MarketingRequestStatus.WITHDRAWN
            )
        ) {
            return@withTransaction SaveResult.Refused("قرار غير معروف: $decision")
        }
        if (row.isAccepted && decision == MarketingRequestStatus.ACCEPTED) {
            return@withTransaction SaveResult.Refused("الطلب مقبول أصلًا")
        }
        dao.upsertRequest(row.copy(status = decision))
        SaveResult.Saved(requestId, 1, decision)
    }

    fun observeRequestsOfFarmer(farmerMemberId: String) = dao.observeRequestsOfFarmer(farmerMemberId)

    suspend fun requestsOfBroker(brokerMemberId: String) = dao.getRequestsOfBroker(brokerMemberId)

    // ------------------------------------------------------------------ النصّ العام

    /** النصّ الذي يُشارك في واتساب أو يُطبع: من المحرّك نفسه، فلا صياغة ثانية. */
    suspend fun shareText(id: String): String? {
        val row = dao.getListing(id) ?: return null
        if (!row.isPublicVisible) return null
        return MarketEngine.publicText(row.toPublic(dao.getContact(id)))
    }

    // ------------------------------------------------------------------ التحويلات الداخلية

    private suspend fun write(
        draft: MarketEngine.Draft,
        revision: Int,
        status: String,
        brokerPhoneNumber: String,
        now: Long
    ) {
        val existing = dao.getListing(draft.id)
        dao.upsertListing(
            MarketListingRow(
                id = draft.id,
                revision = revision,
                status = status,
                brokerMemberId = draft.brokerMemberId,
                farmerMemberId = draft.farmerMemberId,
                farmerName = draft.farmerName.trim(),
                brokerName = draft.brokerName.trim(),
                brokerPhoneChoice = draft.brokerPhone.name,
                title = draft.title.trim(),
                cropType = draft.cropType.trim(),
                description = draft.description.trim(),
                quantityNote = draft.quantityNote.trim(),
                priceMode = draft.priceMode,
                priceMinor = if (draft.priceMode == PriceMode.FIXED) draft.priceMinor else 0L,
                currency = draft.currency,
                unit = draft.unit,
                governorate = draft.location.governorate.trim(),
                photoCount = draft.photos.size,
                createdAt = existing?.createdAt ?: draft.createdAt,
                updatedAt = now,
                publishedAt = existing?.publishedAt,
                closedAt = existing?.closedAt
            )
        )
        val previousContact = dao.getContact(draft.id)
        dao.upsertContact(
            MarketContactRow(
                listingId = draft.id,
                farmerPhone = draft.farmerPhone.trim(),
                farmerAllowsPublicPhone = draft.farmerAllowsPublicPhone,
                // الرقم يُحفظ دائمًا (ملك الدلال)، والنشر وحده يتبعه اختياره.
                brokerPhone = brokerPhoneNumber.trim().ifBlank { previousContact?.brokerPhone.orEmpty() },
                governorate = draft.location.governorate.trim(),
                district = draft.location.district.trim(),
                village = draft.location.village.trim(),
                landmark = draft.location.landmark.trim()
            )
        )
    }

    private fun MarketListingRow.toPublic(): MarketEngine.PublicListing = MarketEngine.publicListing(
        draft = toDraft(null),
        revision = revision,
        status = status
    )

    /**
     * الصفّ العام كما يراه الناس: تُملأ قناة التواصل برقم الدلال **فقط** إذا اختار الدلال نشر رقمه،
     * وإلا بقيت فارغة. وهذا هو الفرق الذي كان ساقطًا: العرض كان يُحفظ بنيّة النشر، ثم يُعاد بناؤه
     * في القراءة بلا قناة تواصل فيظهر للناس عرضًا بلا وسيلة اتصال.
     *
     * وهاتف المزارع لا يدخل هذه الدالة أبدًا: مكانه [MarketContactRow.farmerPhone] ولا يُقرأ هنا.
     */
    private fun MarketListingRow.toPublic(contact: MarketContactRow?): MarketEngine.PublicListing =
        MarketEngine.publicListingWithContact(
            draft = toDraft(contact),
            revision = revision,
            status = status,
            brokerPhone = contact?.brokerPhone.orEmpty()
        )

    private suspend fun MarketListingRow.toBrokerView(): BrokerListing = BrokerListing(
        row = this,
        contact = dao.getContact(id),
        request = dao.getRequestBetween(farmerMemberId, brokerMemberId),
        moderation = dao.getLatestModeration(id)
    )

    private fun MarketListingRow.toDraft(contact: MarketContactRow?): MarketEngine.Draft = MarketEngine.Draft(
        id = id,
        brokerMemberId = brokerMemberId,
        farmerMemberId = farmerMemberId,
        farmerName = farmerName,
        farmerPhone = contact?.farmerPhone.orEmpty(),
        farmerAllowsPublicPhone = contact?.farmerAllowsPublicPhone ?: false,
        brokerName = brokerName,
        brokerPhone = if (brokerPhoneChoice == MarketEngine.PublicPhoneChoice.SHOWN.name) {
            MarketEngine.PublicPhoneChoice.SHOWN
        } else {
            MarketEngine.PublicPhoneChoice.HIDDEN
        },
        title = title,
        cropType = cropType,
        description = description,
        quantityNote = quantityNote,
        priceMode = priceMode,
        priceMinor = priceMinor,
        currency = currency,
        unit = unit,
        location = ListingPrivacy.PrivateLocation(
            governorate = contact?.governorate ?: governorate,
            district = contact?.district.orEmpty(),
            village = contact?.village.orEmpty(),
            landmark = contact?.landmark.orEmpty()
        ),
        photos = List(photoCount) { "" },
        createdAt = createdAt
    )

    private fun MarketingRequestRow.toEngineRequest(): MarketEngine.MarketingRequest = MarketEngine.MarketingRequest(
        id = id,
        farmerMemberId = farmerMemberId,
        brokerMemberId = brokerMemberId,
        cropTitle = cropTitle,
        status = status,
        requestedAt = requestedAt,
        note = note
    )

    private fun ModerationRow.toEngineModeration(): MarketEngine.ModerationRecord = MarketEngine.ModerationRecord(
        listingId = listingId,
        revision = revision,
        decision = decision,
        decidedAt = decidedAt,
        note = note
    )
}

/**
 * ما يراه الدلال عن عرضه: العرض، وجهة الاتصال الخاصة، وطلب التسويق، وآخر مصادقة.
 * هذه الحزمة **لا تُعطى لشاشة عامة** — فيها هاتف المزارع بطبيعتها.
 */
data class BrokerListing(
    val row: MarketListingRow,
    val contact: MarketContactRow?,
    val request: MarketingRequestRow?,
    val moderation: ModerationRow?
) {
    val public: MarketEngine.PublicListing get() = MarketEngine.publicListingWithContact(
        draft = MarketEngine.Draft(
            id = row.id,
            brokerMemberId = row.brokerMemberId,
            farmerMemberId = row.farmerMemberId,
            farmerName = row.farmerName,
            farmerPhone = contact?.farmerPhone.orEmpty(),
            farmerAllowsPublicPhone = contact?.farmerAllowsPublicPhone ?: false,
            brokerName = row.brokerName,
            brokerPhone = if (row.brokerPhoneChoice == MarketEngine.PublicPhoneChoice.SHOWN.name) {
                MarketEngine.PublicPhoneChoice.SHOWN
            } else {
                MarketEngine.PublicPhoneChoice.HIDDEN
            },
            title = row.title,
            cropType = row.cropType,
            description = row.description,
            quantityNote = row.quantityNote,
            priceMode = row.priceMode,
            priceMinor = row.priceMinor,
            currency = row.currency,
            unit = row.unit,
            location = ListingPrivacy.PrivateLocation(governorate = row.governorate),
            photos = List(row.photoCount) { "" },
            createdAt = row.createdAt
        ),
        revision = row.revision,
        status = row.status,
        brokerPhone = contact?.brokerPhone.orEmpty()
    )

    val canSubmitForReview: Boolean get() = request?.isAccepted == true &&
        row.status != ListingStatus.PUBLISHED && row.status != ListingStatus.SOLD &&
        row.status != ListingStatus.WITHDRAWN

    val isApproved: Boolean get() = moderation?.decision == ModerationDecision.APPROVED &&
        moderation?.revision == row.revision

    val nextStep: String get() = when {
        request == null -> "لا طلب تسويق بعد: يحتاج المزارع أن يطلب تسويقه ثم يقبل"
        request?.isAccepted != true -> "طلب التسويق لم يُقبل من المزارع (${MarketingRequestStatus.label(request.status)})"
        row.status == ListingStatus.SOLD -> "بِيع: العرض مغلق"
        row.status == ListingStatus.WITHDRAWN -> "مسحوب بقرار صاحبه"
        row.status == ListingStatus.REJECTED -> "المصادقة رفضته: راجع البيان ثم أرسِله مرة أخرى"
        row.status == ListingStatus.PUBLISHED -> "معروض للناس"
        row.status == ListingStatus.RESERVED -> "محجوز لصلح قائم"
        isApproved -> "مصادق عليه — جاهز للنشر"
        moderation?.decision == ModerationDecision.NEEDS_EDIT ->
            "المصادقة طلبت تعديلًا: ${moderation.note.ifBlank { "راجع البيان" }}"

        else -> "بانتظار المصادقة"
    }
}
