package com.baynana.domain.market

import com.baynana.domain.money.Currency
import com.baynana.domain.money.Money
import com.baynana.domain.money.MoneyFormat

/**
 * محرّك السوق (ح٩) — نقيّ: يفحص العرض قبل نشره، ويصوغ **العرض العام** الذي لا يمكن أن يحمل هاتف
 * المزارع، ويحكم التعديل والمصادقة.
 *
 * القواعد المنفَّذة (الخطة §5.3):
 * 1. طلب التسويق **ليس دَينًا**، ولا يُنشر عرض قبل قبول المزارع.
 * 2. هاتف المزارع مخفي افتراضيًا ولا يظهر في أي وثيقة عامة؛ وكشفه قرار المزارع وحده لا الدلال.
 * 3. الموقع يُنقَّح إلى المحافظة وحدها: لا مديرية ولا قرية ولا علامة تدلّ على البيت.
 * 4. لا مبالغ ديون في العرض العام: النصّ الحرّ يُفحص، والسعر خانة مستقلّة بالوحدة الصغرى.
 * 5. لا نشر بلا مصادقة، وأي تعديل على السعر أو الكمية أو الوصف بعد النشر يعيد العرض إلى المصادقة
 *    برقم مراجعة جديد، فلا يُبدَّل عرض مصادَق عليه من تحت يد الناس.
 */
object MarketEngine {

    /** طلب تسويق من مزارع إلى دلال. الطلب لا يُنشئ دَينًا ولا يُنشر قبل قبول المزارع. */
    data class MarketingRequest(
        val id: String,
        val farmerMemberId: String,
        val brokerMemberId: String,
        val cropTitle: String,
        val status: String = MarketingRequestStatus.REQUESTED,
        val requestedAt: Long,
        val note: String = ""
    ) {
        val isAccepted: Boolean get() = status == MarketingRequestStatus.ACCEPTED
    }

    /** قرار مصادقة على مراجعة محدّدة من العرض. */
    data class ModerationRecord(
        val listingId: String,
        val revision: Int,
        val decision: String,
        val decidedAt: Long,
        val note: String = ""
    )

    /** مسودة العرض كما يدخلها الدلال (وفيها ما لا يُنشر: الهاتف والموقع الدقيق). */
    data class Draft(
        val id: String,
        val brokerMemberId: String,
        val farmerMemberId: String,
        val farmerName: String,
        val farmerPhone: String,
        /** قرار المزارع وحده: إن أراد أن يُنشر هاتفه. */
        val farmerAllowsPublicPhone: Boolean = false,
        val brokerName: String,
        /** هاتف الدلال هو قناة التواصل المعلنة؛ نشره قراره لأنه رقمه. */
        val brokerPhone: PublicPhoneChoice = PublicPhoneChoice.HIDDEN,
        val title: String,
        val cropType: String,
        val description: String = "",
        val quantityNote: String = "",
        val priceMode: String = PriceMode.ON_OFFER,
        val priceMinor: Long = 0L,
        val currency: String = "YER_NEW",
        val unit: String = ListingUnit.WHOLE_LOT,
        val location: ListingPrivacy.PrivateLocation,
        val photos: List<String> = emptyList(),
        val createdAt: Long
    )

    enum class PublicPhoneChoice { HIDDEN, SHOWN }

    /**
     * **العرض العام** — النوع الذي يُرسل إلى الشاشة العامة وPDF وواتساب. لا حقل فيه لهاتف المزارع،
     * ولا موقع دقيق، والسعر فيه بالوحدة الصغرى مع وحدته. الخصوصية هنا بنيوية لا اجتهادية.
     */
    data class PublicListing(
        val id: String,
        val revision: Int,
        val status: String,
        val title: String,
        val cropType: String,
        val description: String,
        val quantityNote: String,
        val priceMode: String,
        val priceMinor: Long,
        val currency: String,
        val unit: String,
        val governorate: String,
        val brokerName: String,
        /** يُملأ فقط إذا أذن الدلال بنشر رقمه؛ وهو رقم الدلال لا المزارع. */
        val brokerContact: String = "",
        val photoCount: Int = 0,
        val createdAt: Long
    )

    sealed interface Check {
        data class Allowed(val listing: PublicListing, val notes: List<String>) : Check
        data class Refused(val reason: String) : Check
    }

    private fun priceText(minor: Long, currency: String): String {
        val code = Currency.fromCode(currency) ?: return "$minor فلسًا"
        return MoneyFormat.format(Money.ofMinor(minor, code))
    }

    /**
     * يفحص العرض من أجل **النشر**. [moderation] هو قرار المصادقة على المراجعة [revision].
     */
    fun checkPublish(
        draft: Draft,
        request: MarketingRequest?,
        moderation: ModerationRecord?,
        revision: Int = 1,
        existingStatus: String? = null
    ): Check {
        // 1) لا نشر قبل قبول المزارع، والطلب نفسه ليس دَينًا.
        if (request == null) {
            return Check.Refused("لا يُنشر عرض بلا طلب تسويق من المزارع")
        }
        if (request.farmerMemberId != draft.farmerMemberId || request.brokerMemberId != draft.brokerMemberId) {
            return Check.Refused("طلب التسويق لا يطابق طرفي العرض")
        }
        if (!request.isAccepted) {
            return Check.Refused(
                "لا يُنشر العرض قبل قبول المزارع للتسويق (الحالة: ${MarketingRequestStatus.label(request.status)})"
            )
        }

        // 2) لا يُعاد نشر ما بِيع أو انتهى.
        if (existingStatus == ListingStatus.SOLD) return Check.Refused("المحصول بِيع: لا يُعاد نشره")
        if (existingStatus == ListingStatus.EXPIRED) return Check.Refused("انتهى موسم العرض: أنشئ عرضًا جديدًا")

        if (draft.title.isBlank()) return Check.Refused("عنوان العرض مطلوب")
        if (draft.title.trim().length > MarketLimits.MAX_TITLE) {
            return Check.Refused("العنوان أطول من ${MarketLimits.MAX_TITLE} حرفًا")
        }
        if (draft.cropType.isBlank()) return Check.Refused("نوع المحصول مطلوب")
        if (draft.description.trim().length > MarketLimits.MAX_DESCRIPTION) {
            return Check.Refused("الوصف أطول من ${MarketLimits.MAX_DESCRIPTION} حرفًا")
        }
        if (draft.quantityNote.trim().length > MarketLimits.MAX_QUANTITY_NOTE) {
            return Check.Refused("بيان الكمية أطول من ${MarketLimits.MAX_QUANTITY_NOTE} حرفًا")
        }
        if (draft.photos.size > MarketLimits.MAX_PHOTOS) {
            return Check.Refused("عدد الصور أكبر من ${MarketLimits.MAX_PHOTOS}")
        }
        if (draft.brokerName.isBlank()) return Check.Refused("اسم الدلال مطلوب ليُعرف من يُسأل")
        if (draft.unit !in ListingUnit.all) return Check.Refused("وحدة البيع غير معروفة")
        if (draft.location.governorate.isBlank()) {
            return Check.Refused("المحافظة مطلوبة: هي الموقع الذي يظهر للناس بعد التنقيح")
        }

        // 3) السعر: رقم معلن أو «على السوم» بلا رقم — ولا خلط.
        when (draft.priceMode) {
            PriceMode.FIXED -> {
                if (draft.priceMinor <= 0L) return Check.Refused("السعر المعلن يجب أن يكون أكبر من صفر")
                if (draft.priceMinor > Money.MAX_MINOR) return Check.Refused("السعر أكبر من الحد المسموح")
                Currency.fromCode(draft.currency) ?: return Check.Refused("عملة غير معروفة: ${draft.currency}")
            }

            PriceMode.ON_OFFER -> {
                if (draft.priceMinor != 0L) {
                    return Check.Refused("«على السوم» لا يحمل رقمًا؛ إن أردت رقمًا فاجعل السعر معلنًا")
                }
            }

            else -> return Check.Refused("طريقة السعر غير معروفة")
        }

        // 4) النصّ الحرّ: لا أرقام تواصل ولا ذكر ديون في عرض عام.
        val scan = ListingPrivacy.scanAll(draft.title, draft.description, draft.quantityNote)
        if (!scan.safe) return Check.Refused(scan.summary)

        // 5) لا نشر بلا مصادقة على المراجعة نفسها.
        if (moderation == null) return Check.Refused("العرض بانتظار المصادقة قبل النشر")
        if (moderation.listingId != draft.id || moderation.revision != revision) {
            return Check.Refused("المصادقة لا تطابق مراجعة العرض الحالية")
        }
        when (moderation.decision) {
            ModerationDecision.APPROVED -> Unit
            ModerationDecision.NEEDS_EDIT -> return Check.Refused(
                "المصادقة طلبت تعديلًا: ${moderation.note.ifBlank { "راجع الوصف" }}"
            )
            else -> return Check.Refused("العرض مرفوض: ${moderation.note.ifBlank { "راجع الدلال" }}")
        }

        val notes = mutableListOf<String>()
        notes += "هاتف المزارع لا يدخل العرض العام بنيويًا — لا حقل له في نوع العرض العام."
        if (draft.farmerAllowsPublicPhone) {
            notes += "المزارع أذن بالتواصل المباشر: يُفتح الطلب له بلا نشر رقمه، ويبقى قادرًا على سحبه."
        }
        if (draft.brokerPhone == PublicPhoneChoice.SHOWN) notes += "رقم الدلال هو قناة التواصل في العرض."
        notes += "الموقع المنشور: ${draft.location.governorate.trim()} فقط؛ المديرية والقرية للمزارع والدلال."

        return Check.Allowed(
            listing = publicListing(draft, revision, ListingStatus.PUBLISHED),
            notes = notes
        )
    }

    /**
     * يبني العرض العام. الخصوصية هنا ليست خيارًا: [PublicListing] لا يملك حقلًا لهاتف المزارع،
     * وهذا القيد النوعي هو الضمانة.
     */
    fun publicListing(draft: Draft, revision: Int, status: String): PublicListing = PublicListing(
        id = draft.id,
        revision = revision,
        status = status,
        title = draft.title.trim(),
        cropType = draft.cropType.trim(),
        description = ListingPrivacy.sanitize(draft.description),
        quantityNote = ListingPrivacy.sanitize(draft.quantityNote),
        priceMode = draft.priceMode,
        priceMinor = draft.priceMinor,
        currency = draft.currency,
        unit = draft.unit,
        governorate = ListingPrivacy.publicLocation(draft.location).governorate,
        brokerName = draft.brokerName.trim(),
        brokerContact = "",
        photoCount = draft.photos.size,
        createdAt = draft.createdAt
    )

    /**
     * نسخة تُملأ فيها قناة التواصل برقم الدلال — ولا تُقبل إلا إذا اختار الدلال نشر رقمه،
     * والرقم يأتي من الطبقة التي تملكه (لا من نوع العرض العام نفسه).
     */
    fun publicListingWithContact(draft: Draft, revision: Int, status: String, brokerPhone: String): PublicListing =
        publicListing(draft, revision, status).copy(
            brokerContact = if (draft.brokerPhone == PublicPhoneChoice.SHOWN) brokerPhone.trim() else ""
        )

    // ------------------------------------------------------------- التعديل والمصادقة

    sealed interface EditCheck {
        /** تعديل لا يحتاج مصادقة جديدة (مثل موافقة المزارع على كشف رقمه). */
        data object Allowed : EditCheck

        /** تعديل جوهري: يُرفع رقم المراجعة ويعود العرض إلى المصادقة. */
        data class RequiresRemoderation(val nextRevision: Int, val notes: List<String>) : EditCheck

        data class Refused(val reason: String) : EditCheck
    }

    /**
     * هل يجوز هذا التعديل؟ ومن يجوزه؟
     *
     * - الدلال صاحب العرض يعدّل، والمزارع يعدّل **قرار هاتفه ومساره** وحده.
     * - بعد النشر: أي تغيير في السعر أو الكمية أو الوصف يُرفع المراجعة ويعيد المصادقة.
     * - بعد البيع: لا تعديل على عرض مبيع؛ التصحيح بصلح جديد أو سحب معلن.
     */
    fun checkEdit(
        previous: PublicListing?,
        draft: Draft,
        actorMemberId: String,
        isFarmer: Boolean,
        changesPriceOrBody: Boolean
    ): EditCheck {
        if (previous == null) return EditCheck.Allowed
        if (previous.status == ListingStatus.SOLD) {
            return EditCheck.Refused("لا تعديل على عرض مبيع: التصحيح بصلح جديد أو بيان مستقل")
        }
        if (previous.status == ListingStatus.RESERVED && changesPriceOrBody) {
            return EditCheck.Refused("العرض محجوز لصلح قائم: لا يُغيَّر سعره أو بيانه")
        }

        val actorIsOwner = actorMemberId == draft.brokerMemberId
        if (!actorIsOwner && !isFarmer) {
            return EditCheck.Refused("لا تعديل من غير الدلال صاحب العرض أو المزارع نفسه")
        }
        if (!actorIsOwner && changesPriceOrBody) {
            return EditCheck.Refused(
                "المزارع يعدّل هاتفه ومساره وحده، أما السعر والبيان فبيد الدلال — والخلاف يُحلّ بسحب الطلب"
            )
        }
        if (previous.status == ListingStatus.WITHDRAWN) {
            return EditCheck.Refused("العرض مسحوب: أنشئ عرضًا جديدًا بعد موافقة المزارع")
        }
        if (changesPriceOrBody && previous.status == ListingStatus.PUBLISHED) {
            return EditCheck.RequiresRemoderation(
                nextRevision = previous.revision + 1,
                notes = listOf(
                    "تعديل بعد النشر يعيد العرض إلى المصادقة برقم مراجعة جديد (${previous.revision + 1})",
                    "المراجعة المعروضة تبقى كما صادق عليها الناس حتى تُصادَق الجديدة"
                )
            )
        }
        return EditCheck.Allowed
    }

    /** كل عرض يُنشر يحتاج مصادقة على مراجعة بعينها — نقطة سياسة واحدة تُقرأ من مكان واحد. */
    fun requiresModeration(): Boolean = true

    // ------------------------------------------------------------- النصّ

    /** النصّ العام بنسخة واحدة: يُستعمل في الشاشة والمشاركة والطباعة، فلا تنسخ جهة شكلًا آخر. */
    fun publicText(listing: PublicListing, withContact: Boolean = true): String = buildString {
        append("عرض: ").append(listing.title)
        append(" — ").append(listing.governorate)
        append("\nالمحصول: ").append(listing.cropType)
        if (listing.quantityNote.isNotBlank()) append(" • الكمية: ").append(listing.quantityNote)
        append("\nالسعر: ")
        if (listing.priceMode == PriceMode.ON_OFFER) {
            append(PriceMode.label(listing.priceMode))
        } else {
            append(priceText(listing.priceMinor, listing.currency))
            append(" (").append(ListingUnit.label(listing.unit)).append(")")
        }
        if (listing.description.isNotBlank()) append("\nالوصف: ").append(listing.description)
        if (listing.photoCount > 0) append("\nصور: ").append(listing.photoCount)
        append("\nالدلال: ").append(listing.brokerName)
        if (withContact && listing.brokerContact.isNotBlank()) append(" • ").append(listing.brokerContact)
        append("\n").append(statusLine(listing))
    }

    /** سطر حالة واحد يُقرأ في الإشعار: الحالة، والمراجعة، والثمن أو السوم. */
    fun statusLine(listing: PublicListing): String = buildString {
        append(ListingStatus.label(listing.status))
        append(" • مراجعة ").append(listing.revision)
        append(" • ")
        if (listing.priceMode == PriceMode.ON_OFFER) append("على السوم")
        else append(priceText(listing.priceMinor, listing.currency))
    }

    /** ملخّص داخلي للدلال: ما لا يُنشر (المزرعة الدقيقة) يبقى هنا. */
    fun internalSummary(draft: Draft): String = buildString {
        append("عرض ").append(draft.id)
        append(" • المزارع: ").append(draft.farmerName)
        append(" • الموقع: ").append(draft.location.governorate)
        if (draft.location.district.isNotBlank()) append(" / ").append(draft.location.district)
        if (draft.location.village.isNotBlank()) append(" / ").append(draft.location.village)
        append(" • هاتف المزارع: ").append(if (draft.farmerPhone.isBlank()) "غير مسجّل" else "محفوظ محليًا")
    }
}
