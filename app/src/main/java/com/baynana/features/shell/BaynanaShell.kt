package com.baynana.features.shell

import com.baynana.ShellTab

/**
 * حالة التنقّل في التطبيق بنوع واحد صريح — لا «ماذا أعرض؟» من عشرة متغيّرات منطقية كما كان.
 *
 * هذا الفصل يجعل التنقّل **قابلًا للاختبار** بلا شاشة ولا جهاز: `ShellHost` دالة نقية تُجرَّب في
 * اختبارات الوحدة، فلا تعود «شاشة تُفتح صدفةً» أو «زر لا يقود إلى شيء».
 */
sealed interface BaynanaShell {
    val tab: ShellTab

    /** تبويب جذري بلا تفاصيل مفتوحة. */
    data class Tab(override val tab: ShellTab) : BaynanaShell

    /**
     * شاشة فرعية تحت تبويب آخر (غير «المزيد»): مثل «عرضي في السوق» و«طلبات التسويق».
     *
     * الفرق عن [Extra]: هذا يُبقي التبويب الأصلي محدَّدًا في الشريط السفلي، لأن المستخدم ما زال
     * داخل السوق أو داخل غرفه، لا داخل «المزيد».
     */
    data class Sub(val parent: ShellTab, val key: String, val arg: String? = null) : BaynanaShell {
        override val tab: ShellTab get() = parent
    }

    /** كشف غرفة مفتوح فوق تبويب «غرفي». */
    data class Room(val roomId: String) : BaynanaShell {
        override val tab: ShellTab get() = ShellTab.ROOMS
    }

    /**
     * شاشة فرعية تحت «المزيد» (الصلح، مزرعتي، التقارير، الإعدادات، حول).
     *
     * الشاشات القديمة تعمل وتُصبغ هنا حتى يأتي دورها في إعادة البناء (د٤–د٦)، بدل أن تكون
     * أزرارًا لا تفعل شيئًا.
     */
    data class Extra(val key: String) : BaynanaShell {
        override val tab: ShellTab get() = ShellTab.MORE
    }

    companion object {
        fun home(): BaynanaShell = Tab(ShellTab.HOME)
        fun rooms(): BaynanaShell = Tab(ShellTab.ROOMS)
        fun room(roomId: String): BaynanaShell = Room(roomId)
        fun extra(key: String): BaynanaShell = Extra(key)
        fun sub(parent: ShellTab, key: String, arg: String? = null): BaynanaShell = Sub(parent, key, arg)

        /** كشف حساب غرفة: شاشة فرعية تحت «غرفي»، ومعرّف الغرفة في الوسيط. */
        fun statement(roomId: String): BaynanaShell = Sub(ShellTab.ROOMS, KEY_STATEMENT, roomId)

        /** مفاتيح الشاشات الفرعية في مكان واحد فلا ينشأ «نصّ سحري» يتكرّر في ملفات. */
        const val KEY_STATEMENT = "statement"
    }
}

object ShellHost {
    /** الانتقال إلى تبويب: يُغلق أي تفاصيل مفتوحة (سلوك «التنقّل للأعلى» المعتاد). */
    fun navigate(tab: ShellTab): BaynanaShell = BaynanaShell.Tab(tab)

    /** زرّ الرجوع: من التفاصيل إلى قائمة غرفي، ومن أي تبويب إلى الرئيسية، ومن الرئيسية يخرج النظام. */
    fun back(shell: BaynanaShell): BaynanaShell? = when (shell) {
        is BaynanaShell.Room -> BaynanaShell.rooms()
        is BaynanaShell.Extra -> BaynanaShell.Tab(shell.tab)
        is BaynanaShell.Sub -> BaynanaShell.Tab(shell.parent)
        is BaynanaShell.Tab -> if (shell.tab == ShellTab.HOME) null else BaynanaShell.home()
    }
}
