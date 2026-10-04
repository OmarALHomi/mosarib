package com.baynana.data.local.market

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.baynana.core.database.AppDatabase
import com.baynana.domain.market.ListingPrivacy
import com.baynana.domain.market.ListingStatus
import com.baynana.domain.market.MarketEngine
import com.baynana.domain.market.MarketingRequestStatus
import com.baynana.domain.market.ModerationDecision
import com.baynana.domain.market.PriceMode
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * بوابة ح٩ (٢/٢) على قاعدة حقيقية: تخزين العروض والطلبات والمصادقات، وأن **لا هاتف مزارع يخرج**
 * من أي مسار عام. الفحص هنا ليس على الشاشة بل على ما تقرأه الشاشة فعلًا من القاعدة.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MarketRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: MarketRepository

    private val broker = "me"
    private val farmer = "farmer-أحمد"
    private val now = 1_700_000_000_000L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repository = MarketRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun draft(
        id: String = "listing-1",
        title: String = "رمان صنف ممتاز",
        farmerPhone: String = "777123456",
        governorate: String = "صنعاء",
        description: String = "محصول هذا الموسم"
    ) = MarketEngine.Draft(
        id = id,
        brokerMemberId = broker,
        farmerMemberId = farmer,
        farmerName = "أحمد بن ناصر",
        farmerPhone = farmerPhone,
        brokerName = "الدلال سالم",
        brokerPhone = MarketEngine.PublicPhoneChoice.SHOWN,
        title = title,
        cropType = "رمان",
        description = description,
        quantityNote = "40 كرتون",
        priceMode = PriceMode.FIXED,
        priceMinor = 3_500_000L, // 35,000 ر.ي
        currency = "YER_NEW",
        unit = "SACK",
        location = ListingPrivacy.PrivateLocation(
            governorate = governorate,
            district = "بني حشيش",
            village = "الحصن"
        ),
        createdAt = now
    )

    private suspend fun acceptMarketing() {
        repository.requestMarketing("req-1", farmer, broker, "رمان", now = now)
        repository.decideRequest("req-1", farmer, MarketingRequestStatus.ACCEPTED)
    }

    // ------------------------------------------------------------------ ١) لا نشر بلا قبول المزارع

    @Test
    fun `لا_يُنشر_عرض_قبل_قبول_المزارع`() = runBlocking {
        repository.save(draft(), actorMemberId = broker, isFarmer = false, changesPriceOrBody = true, brokerPhoneNumber = "733000000", now = now)
        // بلا طلب تسويق مقبول: الإرسال للمصادقة نفسه مرفوض
        val submitted = repository.submitForReview("listing-1")
        assertTrue(submitted is MarketRepository.SaveResult.Refused)
        assertTrue((submitted as MarketRepository.SaveResult.Refused).reason.contains("قبول المزارع"))

        repository.moderate("listing-1", 1, ModerationDecision.APPROVED, "", now = now)
        val published = repository.publish("listing-1")
        assertTrue(published is MarketRepository.SaveResult.Refused)
        assertEquals(0, db.marketDao().publicCount())
    }

    // ------------------------------------------------------------------ ٢) لا نشر بلا مصادقة مطابقة للمراجعة

    @Test
    fun `لا_يُنشر_بلا_مصادقة_على_المراجعة_نفسها`() = runBlocking {
        acceptMarketing()
        repository.save(draft(), broker, false, true, "733000000", now)
        repository.submitForReview("listing-1")

        // مصادقة على مراجعة لا وجود لها (2) لا تسري على المراجعة الحالية (1)
        val stale = repository.moderate("listing-1", 2, ModerationDecision.APPROVED, "", now = now)
        assertTrue(stale is MarketRepository.SaveResult.Refused)

        val published = repository.publish("listing-1")
        assertTrue("النشر بلا مصادقة يجب أن يُرفض", published is MarketRepository.SaveResult.Refused)

        repository.moderate("listing-1", 1, ModerationDecision.APPROVED, "", now = now)
        val ok = repository.publish("listing-1")
        assertTrue(ok is MarketRepository.SaveResult.Saved)
        assertEquals(1, db.marketDao().publicCount())
    }

    @Test
    fun `طلب_التعديل_يمنع_النشر_حتى_يُصلَح`() = runBlocking {
        acceptMarketing()
        repository.save(draft(), broker, false, true, "733000000", now)
        repository.submitForReview("listing-1")
        repository.moderate("listing-1", 1, ModerationDecision.NEEDS_EDIT, "اختصر الوصف", now = now)

        val refused = repository.publish("listing-1")
        assertTrue(refused is MarketRepository.SaveResult.Refused)
        assertTrue((refused as MarketRepository.SaveResult.Refused).reason.contains("اختصر الوصف"))
        assertEquals(0, db.marketDao().publicCount())
    }

    // ------------------------------------------------------------------ ٣) الخصوصية بنيوية

    @Test
    fun `لا_هاتف_مزارع_ولا_موقع_دقيق_في_أي_مخرج_عام`() = runBlocking {
        acceptMarketing()
        repository.save(draft(), broker, false, true, "733000000", now)
        repository.submitForReview("listing-1")
        repository.moderate("listing-1", 1, ModerationDecision.APPROVED, "", now = now)
        repository.publish("listing-1")

        val public = repository.publicListings().single()
        val text = MarketEngine.publicText(public)
        val shareText = repository.shareText("listing-1")

        listOf(text, shareText.orEmpty(), MarketEngine.statusLine(public)).forEach { surface ->
            assertFalse("رقم المزارع تسرّب إلى: $surface", surface.contains("777123456"))
            assertFalse("القرية تسرّبت", surface.contains("الحصن"))
            assertFalse("المديرية تسرّبت", surface.contains("بني حشيش"))
        }
        assertEquals("صنعاء", public.governorate)
        assertTrue("رقم الدلال مصرّح بنشره فيظهر", public.brokerContact.contains("733000000"))

        // والصفّ المخزَّن نفسه لا يحمل هاتفًا: الهاتف في جدول منفصل بقصد
        val row = db.marketDao().getListing("listing-1")!!
        assertFalse(row.toString().contains("777123456"))
        assertEquals("777123456", db.marketDao().getContact("listing-1")!!.farmerPhone)
    }

    @Test
    fun `النصّ_الحرّ_يرفض_رقم_تواصل_أو_ذكر_دين`() = runBlocking {
        acceptMarketing()
        val withPhone = repository.save(
            draft(description = "للتواصل 777 123 456"),
            broker, false, true, "733000000", now
        )
        // المحرّك وحده هو من يمنع النشر؛ والحفظ لا يمنع (المسودة محلية)، لكن الإرسال/النشر يفشل:
        assertTrue(withPhone is MarketRepository.SaveResult.Saved)

        repository.submitForReview("listing-1")
        repository.moderate("listing-1", 1, ModerationDecision.APPROVED, "", now = now)
        val refused = repository.publish("listing-1")
        assertTrue(refused is MarketRepository.SaveResult.Refused)
        assertEquals(0, db.marketDao().publicCount())
    }

    // ------------------------------------------------------------------ ٤) التعديل بعد النشر

    @Test
    fun `تعديل_السعر_بعد_النشر_يرفع_المراجعة_ويعيد_المصادقة`() = runBlocking {
        acceptMarketing()
        repository.save(draft(), broker, false, true, "733000000", now)
        repository.submitForReview("listing-1")
        repository.moderate("listing-1", 1, ModerationDecision.APPROVED, "", now = now)
        repository.publish("listing-1")

        val changed = repository.save(
            draft(title = "رمان صنف أول", description = "محصول هذا الموسم").copy(priceMinor = 4_000_000L),
            broker, false, changesPriceOrBody = true, brokerPhoneNumber = "733000000", now = now + 1
        )
        assertTrue(changed is MarketRepository.SaveResult.RemoderationRequired)
        assertEquals(2, (changed as MarketRepository.SaveResult.RemoderationRequired).revision)

        val row = db.marketDao().getListing("listing-1")!!
        assertEquals(2, row.revision)
        assertEquals(ListingStatus.PENDING_REVIEW, row.status)
        // المصادقة القديمة (مراجعة 1) لا تسري على المراجعة 2
        assertNull(db.marketDao().getModeration("listing-1", 2))
        assertTrue(repository.publish("listing-1") is MarketRepository.SaveResult.Refused)

        repository.moderate("listing-1", 2, ModerationDecision.APPROVED, "", now = now + 2)
        assertTrue(repository.publish("listing-1") is MarketRepository.SaveResult.Saved)
    }

    // ------------------------------------------------------------------ ٥) البيع والسحب

    @Test
    fun `البيع_من_محجوز_فقط_و_لا_تعديل_على_مبيع`() = runBlocking {
        acceptMarketing()
        repository.save(draft(), broker, false, true, "733000000", now)
        repository.submitForReview("listing-1")
        repository.moderate("listing-1", 1, ModerationDecision.APPROVED, "", now = now)
        repository.publish("listing-1")

        // بيع مباشر بلا حجز: مرفوض (البيع يُسجَّل من صلح قائم)
        assertTrue(repository.changeStatus("listing-1", ListingStatus.SOLD) is MarketRepository.SaveResult.Refused)

        repository.changeStatus("listing-1", ListingStatus.RESERVED)
        assertTrue(repository.changeStatus("listing-1", ListingStatus.SOLD) is MarketRepository.SaveResult.Saved)

        val edited = repository.save(draft(title = "شيء آخر"), broker, false, true, "733000000", now = now + 5)
        assertTrue(edited is MarketRepository.SaveResult.Refused)
        assertTrue((edited as MarketRepository.SaveResult.Refused).reason.contains("مبيع"))
    }

    @Test
    fun `العرض_المسحوب_لا_يُعاد_نشره_بل_يُنشأ_جديد`() = runBlocking {
        acceptMarketing()
        repository.save(draft(), broker, false, true, "733000000", now)
        repository.submitForReview("listing-1")
        repository.moderate("listing-1", 1, ModerationDecision.APPROVED, "", now = now)
        repository.publish("listing-1")
        repository.changeStatus("listing-1", ListingStatus.WITHDRAWN)

        assertEquals(0, db.marketDao().publicCount())
        val again = repository.submitForReview("listing-1")
        assertTrue(again is MarketRepository.SaveResult.Refused)
    }

    // ------------------------------------------------------------------ ٦) طلبات التسويق

    @Test
    fun `الطلب_ليس_دينًا_ولا_يُنشئ_قيدًا_في_أي_غرفة`() = runBlocking {
        repository.requestMarketing("req-1", farmer, broker, "رمان", now = now)
        db.marketDao().getRequest("req-1")!!.let { request ->
            assertEquals(MarketingRequestStatus.REQUESTED, request.status)
            assertFalse(request.isAccepted)
        }
        // لا غرفة ولا قيد أُنشئ لأجل الطلب
        assertEquals(0, db.ledgerDao().getAllRooms().size)
    }

    @Test
    fun `قرار_الطلب_للمزارع_وحده`() = runBlocking {
        repository.requestMarketing("req-1", farmer, broker, "رمان", now = now)
        val intruder = repository.decideRequest("req-1", "farmer-آخر", MarketingRequestStatus.ACCEPTED)
        assertTrue(intruder is MarketRepository.SaveResult.Refused)
        assertTrue((intruder as MarketRepository.SaveResult.Refused).reason.contains("للمزارع"))

        val owner = repository.decideRequest("req-1", farmer, MarketingRequestStatus.ACCEPTED)
        assertTrue(owner is MarketRepository.SaveResult.Saved)
        assertTrue(db.marketDao().getRequest("req-1")!!.isAccepted)
    }

    @Test
    fun `قبول_مكرر_أو_طلب_من_النفس_يُرفضان`() = runBlocking {
        repository.requestMarketing("req-1", farmer, broker, "رمان", now = now)
        repository.decideRequest("req-1", farmer, MarketingRequestStatus.ACCEPTED)
        assertTrue(
            repository.decideRequest("req-1", farmer, MarketingRequestStatus.ACCEPTED) is
                MarketRepository.SaveResult.Refused
        )
        assertTrue(
            repository.requestMarketing("req-2", broker, broker, "رمان") is MarketRepository.SaveResult.Refused
        )
    }

    // ------------------------------------------------------------------ ٧) مراجعة المراجعات

    @Test
    fun `عرض_بلا_مصادقة_لا_يظهر_للناس_ولو_حُفظ`() = runBlocking {
        acceptMarketing()
        repository.save(draft(), broker, false, true, "733000000", now)
        assertEquals(0, db.marketDao().publicCount())
        assertTrue(repository.publicListings().isEmpty())
        assertNotNull(db.marketDao().getListing("listing-1"))
    }

    @Test
    fun `عدّادُ_العروض_العامة_يحسب_المعروض_والمحجوز_فقط`() = runBlocking {
        acceptMarketing()
        repeat(2) { index ->
            val id = "listing-$index"
            repository.save(draft(id = id, title = "عرض $index"), broker, false, true, "733000000", now)
            repository.submitForReview(id)
            repository.moderate(id, 1, ModerationDecision.APPROVED, "", now = now)
            repository.publish(id)
        }
        assertEquals(2, db.marketDao().publicCount())
        repository.changeStatus("listing-0", ListingStatus.RESERVED)
        assertEquals(2, db.marketDao().publicCount(), )
        repository.changeStatus("listing-0", ListingStatus.SOLD)
        assertEquals(1, db.marketDao().publicCount())
    }
}
