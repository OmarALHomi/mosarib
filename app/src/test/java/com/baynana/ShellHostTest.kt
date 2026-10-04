package com.baynana

import com.baynana.features.shell.BaynanaShell
import com.baynana.features.shell.ShellHost
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * تنقّل الهيكل الجديد: منطق نقي يُجرَّب بلا شاشة ولا جهاز.
 *
 * السبب: الشكل القديم كان يُظهر شاشات من عشرة أعلام منطقية، فكان يسهل «فتح شاشة صدفةً» أو بقاء
 * المستخدم في مكان لا مخرج منه. هنا نتأكد أن كل انتقال له نهاية معروفة، وأن زرّ الرجوع لا يُغلق
 * التطبيق إلا من الرئيسية.
 */
class ShellHostTest {

    @Test
    fun `five_tabs_with_approved_arabic_titles`() {
        val titles = ShellTab.entries.map { it.title }
        assertEquals(listOf("الرئيسية", "غرفي", "الحركات", "السوق", "المزيد"), titles)
    }

    @Test
    fun `home_is_the_default_shell`() {
        assertEquals(ShellTab.HOME, BaynanaShell.home().tab)
    }

    @Test
    fun `navigate_closes_open_detail`() {
        val open = BaynanaShell.room("room-1")
        val next = ShellHost.navigate(ShellTab.MOVEMENTS)
        assertEquals(ShellTab.MOVEMENTS, next.tab)
        assertTrue("يجب ألا يبقى كشف غرفة مفتوحًا", next is BaynanaShell.Tab)
        assertEquals(ShellTab.ROOMS, open.tab)
    }

    @Test
    fun `back_from_room_detail_goes_to_rooms_tab`() {
        val back = ShellHost.back(BaynanaShell.room("room-9"))
        assertEquals(ShellTab.ROOMS, back?.tab)
        assertTrue(back is BaynanaShell.Tab)
    }

    @Test
    fun `back_from_any_non_home_tab_goes_home`() {
        ShellTab.entries.filter { it != ShellTab.HOME }.forEach { tab ->
            assertEquals(
                "الرجوع من «${tab.title}» يجب أن يقود للرئيسية",
                ShellTab.HOME,
                ShellHost.back(BaynanaShell.Tab(tab))?.tab
            )
        }
    }

    @Test
    fun `back_from_home_leaves_the_app`() {
        assertNull(ShellHost.back(BaynanaShell.home()))
    }

    @Test
    fun `extra_screen_belongs_to_more_tab`() {
        val shell = BaynanaShell.extra("deals")
        assertSame(ShellTab.MORE, shell.tab)
        assertEquals(ShellTab.MORE, ShellHost.back(shell)?.tab)
    }

    @Test
    fun `room_detail_belongs_to_rooms_tab`() {
        assertSame(ShellTab.ROOMS, BaynanaShell.room("room-2").tab)
    }
}
