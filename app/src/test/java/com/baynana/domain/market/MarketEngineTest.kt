package com.baynana.domain.market

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * بوابة ح٩: **العرض العام بلا هاتف ولا مبالغ ديون**، ولا نشر قبل قبول المزارع، ولا نشر بلا مصادقة،
 * وأي تعديل جوهري بعد النشر يرفع المراجعة ويعيد المصادقة.
 */
class MarketEngineTest {

    private val at = 1_700_000_000_000L

    private fun request(
        status: String = MarketingRequestStatus.ACCEPTED,
        farmer: String = "m-farmer",
        broker: String = "m-broker"
    ) = MarketEngine.MarketingRequest(
        id = "req-1", farmerMemberId = farmer, brokerMemberId = broker,
        cropTitle = "قمح", status = status, requestedAt = at
    )

    private fun draft(
        id: String = "listing-1",
        title: String = "قمح بلدي نظيف",
        description: String = "محصول هذا الموسم، معبّأ في أكياس",
        quantityNote: String = "40 كيسًا",
        priceMode: String = PriceMode.FIXED,
        priceMinor: Long = 3_500_000L,
        unit: String = ListingUnit.SACK,
        farmerPhone: String = "777123456",
        allowsPublicPhone: Boolean = false,
        brokerPhone: MarketEngine.PublicPhoneChoice = MarketEngine.PublicPhoneChoice.HIDDEN,
        photos: List<String> = emptyList(),
        location: ListingPrivacy.PrivateLocation = ListingPrivacy.PrivateLocation(
            governorate = "صنعاء", district = "بني حشيش", village = "الجراف", landmark = "بجانب المسجد"
        )
    ) = MarketEngine.Draft(
        id = id,
        brokerMemberId = "m-broker",
        farmerMemberId = "m-farmer",
        farmerName = "أحمد المزارع",
        farmerPhone = farmerPhone,
        farmerAllowsPublicPhone = allowsPublicPhone,
        brokerName = "علي الدلال",
        brokerPhone = brokerPhone,
        title = title,
        cropType = "حبوب",
        description = description,
        quantityNote = quantityNote,
        priceMode = priceMode,
        priceMinor = priceMinor,
        currency = "YER_NEW",
        unit = unit,
        location = location,
        photos = photos,
        createdAt = at
    )

    private fun approved(revision: Int = 1, decision: String = ModerationDecision.APPROVED) =
        MarketEngine.ModerationRecord(
            listingId = "listing-1", revision = revision, decision = decision,
            decidedAt = at + 1_000, note = "بيان واضح"
        )

    private fun allowed(
        draft: MarketEngine.Draft = draft(),
        request: MarketEngine.MarketingRequest? = request(),
        moderation: MarketEngine.ModerationRecord? = approved(),
        revision: Int = 1
    ): MarketEngine.PublicListing {
        val result = MarketEngine.checkPublish(draft, request, moderation, revision)
        assertTrue("كان المتوقع قبولًا لكن جاء: $result", result is MarketEngine.Check.Allowed)
        return (result as MarketEngine.Check.Allowed).listing
    }

    private fun refused(
        draft: MarketEngine.Draft = draft(),
        request: MarketEngine.MarketingRequest? = request(),
        moderation: MarketEngine.ModerationRecord? = approved(),
        revision: Int = 1,
        existingStatus: String? = null
    ): String {
        val result = MarketEngine.checkPublish(draft, request, moderation, revision, existingStatus)
        assertTrue("كان المتوقع رفضًا لكن جاء: $result", result is MarketEngine.Check.Refused)
        return (result as MarketEngine.Check.Refused).reason
    }

    // ------------------------------------------------------------- الخصوصية البنيوية

    @Test
    fun `the farmer phone cannot appear in the public listing because the type has no place for it`() {
        val listing = allowed()
        assertFalse(
            "لا حقل هاتف في نوع العرض العام",
            MarketEngine.PublicListing::class.java.declaredFields.any { it.name.contains("farmer", ignoreCase = true) }
        )
        assertFalse("ولا رقم المزارع في النصّ العام", MarketEngine.publicText(listing).contains("777123456"))
        assertFalse("ولا حتى في القناة المعلنة", listing.brokerContact.contains("777123456"))
    }

    @Test
    fun `a phone written inside the description is caught and blocks publication`() {
        val reason = refused(draft(description = "للاستفسار 777 123 456 أو بعد العصر"))
        assertTrue(reason, reason.contains("رقم يشبه الهاتف"))
        assertTrue("الرسالة تشرح أين يُكتب السعر", reason.contains("والسعر في خانه لا في الوصف"))
    }

    @Test
    fun `arabic-indic digits and country codes are caught too`() {
        assertFalse(ListingPrivacy.scan("اتصل ٧٧٧١٢٣٤٥٦").safe)
        assertFalse(ListingPrivacy.scan("واتس +967-777-123-456").safe)
        assertFalse(ListingPrivacy.scan("رقمي 00967777123456").safe)
        assertTrue("نصّ نظيف يمرّ", ListingPrivacy.scan("قمح نظيف جاهز للتسليم").safe)
    }

    @Test
    fun `quantity and price inside a sentence are not merged into a fake phone number`() {
        // «20 كيس» و«35000» مقطعان يفصلهما حرف، فلا يصلحان لتكوين رقم من سبعة أرقام.
        assertTrue(ListingPrivacy.scan("20 كيس من محصول هذا العام").safe)
        assertTrue(ListingPrivacy.scan("المحصول جاهز والتسليم بعد أسبوع").safe)
    }

    @Test
    fun `sanitize masks the digits instead of throwing the whole description away`() {
        val cleaned = ListingPrivacy.sanitize("للاستفسار 777 123 456 بعد العصر")
        assertFalse(cleaned.contains("777"))
        assertTrue(cleaned.contains("بعد العصر"))
        assertTrue(cleaned.contains("•"))
    }

    @Test
    fun `a public listing never mentions debts or account balances`() {
        assertTrue(refused(draft(description = "الباقي عليه من العام الماضي 40 ألف")).contains("ذكر دَين"))
        assertTrue(refused(draft(description = "كشف حساب قديم وديون متأخرة")).contains("ذكر دَين"))
        // والوصف نفسه لا يُنشر إن كان فيه ما يخصّ الحساب.
        assertTrue(ListingPrivacy.scan("سدد باقي الدين قبل الحصاد").findings.any { it.contains("دَين") })
    }

    @Test
    fun `the published location is the governorate only`() {
        val listing = allowed()
        assertEquals("صنعاء", listing.governorate)
        val text = MarketEngine.publicText(listing)
        assertFalse("لا مديرية في العرض العام", text.contains("بني حشيش"))
        assertFalse("ولا قرية", text.contains("الجراف"))
        assertFalse("ولا علامة تدلّ على البيت", text.contains("بجانب المسجد"))
    }

    @Test
    fun `the private summary is the only place the precise farm appears, and it names no phone`() {
        val summary = MarketEngine.internalSummary(draft())
        assertTrue(summary.contains("بني حشيش"))
        assertTrue(summary.contains("الجراف"))
        assertTrue("حتى الملخّص الداخلي لا يطبع الرقم", summary.contains("محفوظ محليًا"))
        assertFalse(summary.contains("777123456"))
    }

    // ------------------------------------------------------------- شروط النشر

    @Test
    fun `no listing is published before the farmer accepts the marketing request`() {
        val reason = refused(request = request(status = MarketingRequestStatus.REQUESTED))
        assertTrue(reason, reason.contains("قبل قبول المزارع"))
        assertTrue(refused(request = request(status = MarketingRequestStatus.REFUSED)).contains("قبل قبول المزارع"))
    }

    @Test
    fun `a request that belongs to another farmer or another broker is refused`() {
        assertTrue(refused(request = request(farmer = "m-other")).contains("لا يطابق طرفي العرض"))
        assertTrue(refused(request = request(broker = "m-other")).contains("لا يطابق طرفي العرض"))
    }

    @Test
    fun `a missing request is refused, because the marketing request is not a debt and not a listing`() {
        assertTrue(refused(request = null).contains("بلا طلب تسويق"))
    }

    @Test
    fun `publication without moderation is refused`() {
        assertTrue(refused(moderation = null).contains("بانتظار المصادقة"))
    }

    @Test
    fun `moderation must match the exact revision being published`() {
        assertTrue(refused(moderation = approved(revision = 2), revision = 1).contains("لا تطابق مراجعة العرض"))
    }

    @Test
    fun `a rejected or edit-requested moderation stops publication with its reason`() {
        val rejected = refused(moderation = approved(decision = ModerationDecision.REJECTED))
        assertTrue(rejected, rejected.contains("العرض مرفوض"))
        val needsEdit = refused(moderation = approved(decision = ModerationDecision.NEEDS_EDIT))
        assertTrue(needsEdit, needsEdit.contains("طلبت تعديلًا"))
    }

    @Test
    fun `a sold or expired crop is not republished`() {
        assertTrue(refused(existingStatus = ListingStatus.SOLD).contains("بِيع"))
        assertTrue(refused(existingStatus = ListingStatus.EXPIRED).contains("انتهى موسم"))
    }

    @Test
    fun `an empty title or an oversized description is refused before anything else`() {
        assertTrue(refused(draft(title = "   ")).contains("عنوان العرض"))
        assertTrue(refused(draft(description = "ط".repeat(MarketLimits.MAX_DESCRIPTION + 1))).contains("أطول من"))
        assertTrue(refused(draft(quantityNote = "ك".repeat(MarketLimits.MAX_QUANTITY_NOTE + 1))).contains("أطول من"))
    }

    @Test
    fun `too many photos are refused`() {
        val many = (1..MarketLimits.MAX_PHOTOS + 1).map { "photo-$it.jpg" }
        assertTrue(refused(draft(photos = many)).contains("عدد الصور"))
    }

    @Test
    fun `an unknown unit or a missing governorate is refused`() {
        assertTrue(refused(draft(unit = "TIN")).contains("وحدة البيع"))
        assertTrue(refused(draft(location = ListingPrivacy.PrivateLocation())).contains("المحافظة"))
    }

    // ------------------------------------------------------------- السعر

    @Test
    fun `a fixed price must be a real amount in minor units`() {
        assertTrue(refused(draft(priceMinor = 0L)).contains("السعر المعلن"))
        assertTrue(refused(draft(priceMinor = -5L)).contains("السعر المعلن"))
        assertTrue(refused(draft(priceMinor = 5_000_000L, priceMode = PriceMode.FIXED).copy(currency = "XYZ")).contains("عملة غير معروفة"))
    }

    @Test
    fun `on offer means no number at all`() {
        assertTrue(refused(draft(priceMode = PriceMode.ON_OFFER, priceMinor = 100L)).contains("على السوم"))
        val listing = allowed(draft(priceMode = PriceMode.ON_OFFER, priceMinor = 0L))
        assertTrue(MarketEngine.publicText(listing).contains("على السوم"))
    }

    @Test
    fun `an unknown price mode is refused`() {
        assertTrue(refused(draft(priceMode = "MAYBE")).contains("طريقة السعر"))
    }

    // ------------------------------------------------------------- التعديل

    @Test
    fun `the broker may edit a published listing, but the edit returns it to moderation`() {
        val listing = allowed()
        val edit = MarketEngine.checkEdit(
            previous = listing,
            draft = draft(priceMinor = 3_800_000L),
            actorMemberId = "m-broker",
            isFarmer = false,
            changesPriceOrBody = true
        )
        assertTrue(edit is MarketEngine.EditCheck.RequiresRemoderation)
        assertEquals(2, (edit as MarketEngine.EditCheck.RequiresRemoderation).nextRevision)
        assertTrue(edit.notes.any { it.contains("تبقى كما صادق عليها الناس") })
    }

    @Test
    fun `the farmer may change his own phone decision without moderation, but not the price`() {
        val listing = allowed()
        val hisDecision = MarketEngine.checkEdit(
            previous = listing,
            draft = draft(allowsPublicPhone = true),
            actorMemberId = "m-farmer",
            isFarmer = true,
            changesPriceOrBody = false
        )
        assertEquals(MarketEngine.EditCheck.Allowed, hisDecision)

        val priceChange = MarketEngine.checkEdit(
            previous = listing,
            draft = draft(priceMinor = 1L),
            actorMemberId = "m-farmer",
            isFarmer = true,
            changesPriceOrBody = true
        )
        assertTrue((priceChange as MarketEngine.EditCheck.Refused).reason.contains("السعر والبيان فبيد الدلال"))
    }

    @Test
    fun `a stranger cannot edit anything`() {
        val listing = allowed()
        val edit = MarketEngine.checkEdit(listing, draft(), "m-stranger", isFarmer = false, changesPriceOrBody = true)
        assertTrue((edit as MarketEngine.EditCheck.Refused).reason.contains("لا تعديل من غير الدلال"))
    }

    @Test
    fun `a sold listing is never edited and a reserved one keeps its price`() {
        val sold = allowed().copy(status = ListingStatus.SOLD)
        assertTrue(
            (MarketEngine.checkEdit(sold, draft(), "m-broker", false, true) as MarketEngine.EditCheck.Refused)
                .reason.contains("عرض مبيع")
        )
        val reserved = allowed().copy(status = ListingStatus.RESERVED)
        assertTrue(
            (MarketEngine.checkEdit(reserved, draft(), "m-broker", false, true) as MarketEngine.EditCheck.Refused)
                .reason.contains("محجوز لصلح قائم")
        )
        assertEquals(
            "والمزارع يعدّل قراره في عرض محجوز",
            MarketEngine.EditCheck.Allowed,
            MarketEngine.checkEdit(reserved, draft(allowsPublicPhone = true), "m-farmer", true, false)
        )
    }

    // ------------------------------------------------------------- النصّ

    @Test
    fun `the public text is one text with the same numbers as the listing`() {
        val listing = allowed()
        val text = MarketEngine.publicText(listing)
        assertTrue(text, text.contains("عرض: قمح بلدي نظيف — صنعاء"))
        assertTrue(text, text.contains("المحصول: حبوب • الكمية: 40 كيسًا"))
        assertTrue(text, text.contains("السعر: 35,000 ر.ي (بالكيس)"))
        assertTrue(text, text.contains("الدلال: علي الدلال"))
        assertTrue(text, text.contains("معروض • مراجعة 1 • 35,000 ر.ي"))
    }

    @Test
    fun `the broker contact appears only when he chooses to publish his own number`() {
        val hidden = allowed(draft(brokerPhone = MarketEngine.PublicPhoneChoice.HIDDEN))
        assertEquals("", hidden.brokerContact)
        val shown = MarketEngine.publicListingWithContact(
            draft(brokerPhone = MarketEngine.PublicPhoneChoice.SHOWN),
            revision = 1,
            status = ListingStatus.PUBLISHED,
            brokerPhone = "777999000"
        )
        assertEquals("777999000", shown.brokerContact)
        assertTrue(MarketEngine.publicText(shown).contains("777999000"))
        assertFalse("ورقم المزارع لا يظهر معه", MarketEngine.publicText(shown).contains("777123456"))
    }

    @Test
    fun `an on-offer listing prints no amount anywhere`() {
        val listing = allowed(draft(priceMode = PriceMode.ON_OFFER, priceMinor = 0L))
        val text = MarketEngine.publicText(listing)
        assertTrue(text.contains("على السوم"))
        assertFalse(text.contains("ر.ي"))
    }

    @Test
    fun `the status line reads as one sentence and carries the revision`() {
        val listing = allowed()
        assertEquals("معروض • مراجعة 1 • 35,000 ر.ي", MarketEngine.statusLine(listing))
        assertEquals(
            "محجوز • مراجعة 2 • على السوم",
            MarketEngine.statusLine(listing.copy(status = ListingStatus.RESERVED, revision = 2, priceMode = PriceMode.ON_OFFER))
        )
    }

    @Test
    fun `the farmer's consent is reported as a direct channel, never as a published number`() {
        val result = MarketEngine.checkPublish(
            draft(allowsPublicPhone = true),
            request(),
            approved(),
            revision = 1
        )
        val notes = (result as MarketEngine.Check.Allowed).notes
        assertTrue(notes.any { it.contains("يُفتح الطلب له بلا نشر رقمه") })
        assertTrue(notes.any { it.contains("لا حقل له في نوع العرض العام") })
    }
}
