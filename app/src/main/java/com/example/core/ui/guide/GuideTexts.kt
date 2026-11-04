package com.example.core.ui.guide

data class GuideStep(
    val targetId: String,
    val title: String,
    val description: String,
    val spokenText: String = "$title. $description"
)

object GuideTexts {

    private val HOME_STEPS = listOf(
        GuideStep(
            targetId = "home_session_new",
            title = "سقي جديد",
            description = "سجّل دورة الري: العميل، المدة، والسعر."
        ),
        GuideStep(
            targetId = "home_voice",
            title = "إدخال صوتي",
            description = "قل العملية بصوتك، مثل: مع أحمد ساعتين من 10 آلاف."
        ),
        GuideStep(
            targetId = "home_vouchers",
            title = "قبض وصرف",
            description = "سندات القبض من العملاء، والصرف للمصروفات."
        ),
        GuideStep(
            targetId = "home_last_sessions",
            title = "آخر دورات السقي",
            description = "اضغط أي دورة لتعديلها أو مشاركتها."
        ),
        GuideStep(
            targetId = "home_comprehensive_report",
            title = "التقرير الكامل",
            description = "كشف شامل PDF لكل الحسابات."
        )
    )

    private val SESSION_FORM_STEPS = listOf(
        GuideStep(
            targetId = "session_customer",
            title = "اختيار العميل",
            description = "اختر عميلاً أو اكتب اسماً جديداً."
        ),
        GuideStep(
            targetId = "session_mic",
            title = "الميكروفون الصوتي",
            description = "أو قل الاسم بالصوت مباشرة."
        ),
        GuideStep(
            targetId = "session_time_choice",
            title = "تحديد وقت السقي",
            description = "من الساعة إلى الساعة، أو عدد ساعات يدوي."
        ),
        GuideStep(
            targetId = "session_price",
            title = "سعر الساعة",
            description = "سعر الساعة يُملأ من سعر العميل تلقائياً."
        ),
        GuideStep(
            targetId = "session_save",
            title = "حفظ السقي",
            description = "احفظ دورة السقي لتوثيقها في حساب العميل."
        )
    )

    private val VOUCHER_FORM_STEPS = listOf(
        GuideStep(
            targetId = "voucher_type",
            title = "نوع السند",
            description = "قبض لتحصيل نقد من عميل، أو صرف للمصروفات."
        ),
        GuideStep(
            targetId = "voucher_customer",
            title = "الطرف المعني",
            description = "اختر العميل أو الطرف لربط السند بحسابه."
        ),
        GuideStep(
            targetId = "voucher_amount",
            title = "المبلغ والتفقيط",
            description = "أدخل المبلغ وستظهر قراءته كتابةً لتأكيده."
        ),
        GuideStep(
            targetId = "voucher_save",
            title = "حفظ السند",
            description = "احفظ السند لخصمه أو إضافته للحساب فوراً."
        )
    )

    private val PURCHASE_FORM_STEPS = listOf(
        GuideStep(
            targetId = "purchase_owner",
            title = "صاحب البئر",
            description = "اختر صاحب البئر الذي تم الشراء منه."
        ),
        GuideStep(
            targetId = "purchase_hours",
            title = "الساعات والهدر",
            description = "ساعات التشغيل مع إمكانية خصم دقائق الهدر."
        ),
        GuideStep(
            targetId = "purchase_price",
            title = "سعر الساعة",
            description = "سعر شراء الساعة وحساب الإجمالي تلقائياً."
        ),
        GuideStep(
            targetId = "purchase_save",
            title = "حفظ الشراء",
            description = "تسجيل الشراء وإضافته لمستحقات صاحب البئر."
        )
    )

    private val NAV_STEPS = listOf(
        GuideStep(
            targetId = "nav_vouchers",
            title = "العمليات",
            description = "سجل السندات وحركات الصندوق واليومية."
        ),
        GuideStep(
            targetId = "nav_customers",
            title = "العملاء",
            description = "دليل المزارعين، حساباتهم، وكشوفات الـ PDF."
        ),
        GuideStep(
            targetId = "nav_reports",
            title = "التقارير",
            description = "التقرير الشامل، الأرباح، والديون والمستحقات."
        ),
        GuideStep(
            targetId = "nav_settings",
            title = "الإعدادات",
            description = "النسخ الاحتياطي، المساعد الصوتي، والتخصيص."
        )
    )

    const val HINT_SESSION_WELL_OWNER = "session_well_owner_hint"
    const val HINT_SESSION_WELL_OWNER_TEXT = "فعّله إذا كانت القيمة تُخصم من مستحقات صاحب البئر."

    fun getTour(tourId: String): List<GuideStep> {
        return when (tourId) {
            GuidePreferences.TOUR_HOME -> HOME_STEPS
            GuidePreferences.TOUR_SESSION_FORM -> SESSION_FORM_STEPS
            GuidePreferences.TOUR_VOUCHER_FORM -> VOUCHER_FORM_STEPS
            GuidePreferences.TOUR_PURCHASE_FORM -> PURCHASE_FORM_STEPS
            GuidePreferences.TOUR_NAV -> NAV_STEPS
            else -> emptyList()
        }
    }
}
