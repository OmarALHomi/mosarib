# Review 027, round 2 (Claude, 2026-11-03 05:05, device check)

Build and tests pass.

## Works on the device ✓
- **Settings list:**
  - sections الحساب والاشتراك / التسعير والموزع / البيانات / المظهر والصوت / المساعدة;
  - rows with tinted icons and current values;
  - the subscription subtitle is computed ("متبقي 12 يوماً" comes from `remDays`).
- **Backup and voice open as full pages,** and the system back button returns to the list.
- **Well-owner page:**
  - compact balance card (~145dp), green, with the words;
  - "شراء ساعات" + "سند جديد", which opens "صرف له (دفع)" / "قبض منه".
- **Customer balance:** "له (عليك)" 15,339 is now green (it was red before).
- **Session viewer:** "خصم · 750" with its own colour; one shared payment count
  (`computeSessionPaymentsCount`) for the ledger card and the viewer.

## Required
1. **Backup page: two texts drawn on top of each other** (local section). The row "النسخ
   الاحتياطي اليومي التلقائي" (icon, title, switch, "مفعّل: حفظ يومي تلقائي ونقاط أمان") and the
   block "أحدث النسخ المحلية المحفوظة: / لا توجد نسخ محلية محفوظة بعد…" share the same place.
   - Put them one under the other: first the auto-backup row, then the list header.
   - **The empty message must not show when backups exist.** Two backups were listed under it
     ("نسخة اليوم التلقائية", "نسخة الأمس التلقائية").
   - Check this with no backups, with only auto backups, and with saved backups.
2. **Double header on the sub-pages.** The main bar "الإعدادات" (with the star) stays at the top,
   and a second bar "← النسخ الاحتياطي والاستعادة" sits under it. A sub-page must have **one**
   top bar: the page title with the back arrow, replacing the main bar while the sub-page is
   open. Do the same on the voice page.
3. **Western digits everywhere** (owner rule from task 012).
   - The backup dates show Eastern Arabic digits: "آخر نسخة: ٢٠٢٦/١١/٠٣ - ١:٠٢ ص" in the list
     row, and "٢٠٢٦/١١/٠٣ ١:٠٢ ص · 516.4 KB" inside the backup page. The text comes from the
     backup item's `formattedDate`.
   - Format these dates with the app's `Formatters` (Western digits), or make that formatter
     use `Locale.US` digits. You may touch the file that builds `formattedDate` for this item
     only.
   - Search the settings pages for any other Eastern Arabic digits (٠-٩) and fix them.
4. Keep the settings list's scroll position when you come back from a sub-page (from the round-1
   note). Check it on the device.

## Note
During this check, the task 030 build was installed over the 027 build at 05:04, and the app
closed in the middle of the test. Before installing on the shared phone, check that no other
task is being tested on it.
