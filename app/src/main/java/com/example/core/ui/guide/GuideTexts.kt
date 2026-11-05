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
            targetId = "session_voice_command",
            title = "سجّل العملية بصوتك",
            description = "قل العملية كاملة: مع أحمد ساعتين من 10 آلاف ومسلّم 5 آلاف."
        ),
        GuideStep(
            targetId = "session_customer",
            title = "اختيار العميل",
            description = "اختر عميلاً من القائمة أو اكتب اسماً جديداً."
        ),
        GuideStep(
            targetId = "session_mic",
            title = "الاسم بالصوت",
            description = "أو قل اسم العميل بصوتك من هذا الزر."
        ),
        GuideStep(
            targetId = "session_time_choice",
            title = "تحديد وقت السقي",
            description = "من الساعة إلى الساعة، أو عدد ساعات يدوي."
        ),
        GuideStep(
            targetId = "session_price",
            title = "سعر الساعة",
            description = "سعر الساعة يُملأ تلقائياً من القيمة العامة، أو قم بتعديله."
        ),
        GuideStep(
            targetId = "session_extra",
            title = "الوقت المهدور والخصم",
            description = "أضف وقت التوقف أو الأعطال ليُخصم من المدة، أو خصماً على المبلغ."
        ),
        GuideStep(
            targetId = "session_paid",
            title = "المبلغ المدفوع",
            description = "اكتب ما استلمته الآن، والباقي يُسجَّل ديناً على العميل."
        ),
        GuideStep(
            targetId = "session_full_paid",
            title = "مسلّم كامل",
            description = "اضغطه ليُكتب المبلغ كاملاً إذا سلّم العميل كل المبلغ."
        ),
        GuideStep(
            targetId = "session_notes",
            title = "ملاحظة",
            description = "أضف ملاحظة تظهر في كشف العميل."
        ),
        GuideStep(
            targetId = "session_save",
            title = "حفظ السقي",
            description = "احفظ دورة السقي لتوثيقها في حساب العميل."
        )
    )

    private val VOUCHER_FORM_STEPS = listOf(
        GuideStep(
            targetId = "voucher_voice_command",
            title = "السند بصوتك",
            description = "قل مثلاً: 20 ألف من حارث سعيد، أو صرف 15 ألف ديزل."
        ),
        GuideStep(
            targetId = "voucher_type",
            title = "نوع السند",
            description = "قبض: مبلغ استلمته من عميل. صرف: مصروف أو مبلغ دفعته."
        ),
        GuideStep(
            targetId = "voucher_customer",
            title = "الطرف المعني",
            description = "اختر العميل أو الطرف لربط السند بحسابه."
        ),
        GuideStep(
            targetId = "voucher_amount",
            title = "المبلغ والتفقيط",
            description = "أدخل المبلغ وستظهر قراءته كتابةً تحته لتأكيده."
        ),
        GuideStep(
            targetId = "voucher_discount",
            title = "خصم أو مسامحة",
            description = "اكتب ما سامحت به العميل ليُخصم من دينه دون أن يُحسب مقبوضاً."
        ),
        GuideStep(
            targetId = "voucher_payment_method",
            title = "طريقة الدفع",
            description = "نقداً أو حوالة أو غيرها، تظهر في السند المطبوع."
        ),
        GuideStep(
            targetId = "voucher_notes",
            title = "البيان",
            description = "اكتب سبب السند أو قله بصوتك من زر الميكروفون."
        ),
        GuideStep(
            targetId = "voucher_save",
            title = "حفظ السند",
            description = "احفظ السند ليُضاف إلى الحساب فوراً."
        )
    )

    private val PURCHASE_FORM_STEPS = listOf(
        GuideStep(
            targetId = "purchase_voice_command",
            title = "الشراء بصوتك",
            description = "قل مثلاً: شراء 5 ساعات من يوسف قاسم بـ 4 آلاف."
        ),
        GuideStep(
            targetId = "purchase_owner",
            title = "صاحب البئر",
            description = "اختر صاحب البئر الذي اشتريت منه الماء."
        ),
        GuideStep(
            targetId = "purchase_hours",
            title = "الساعات والهدر",
            description = "ساعات التشغيل، مع إمكانية خصم دقائق الهدر منها."
        ),
        GuideStep(
            targetId = "purchase_price",
            title = "سعر الساعة",
            description = "سعر شراء الساعة، ويُحسب الإجمالي تلقائياً."
        ),
        GuideStep(
            targetId = "purchase_paid",
            title = "المدفوع نقداً",
            description = "ما دفعته له الآن، والباقي يبقى من مستحقاته."
        ),
        GuideStep(
            targetId = "purchase_notes",
            title = "ملاحظة",
            description = "أضف بياناً يظهر في كشف صاحب البئر."
        ),
        GuideStep(
            targetId = "purchase_save",
            title = "حفظ الشراء",
            description = "سجّل الشراء ليُضاف إلى مستحقات صاحب البئر."
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
