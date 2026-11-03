status: ready
<!-- Owner request 2026-11-03. Runs after 027 in worktree D:\Kotlin\mosarib-t023 (no file overlap
with 030). License LOGIC must not change: styling, texts and leaving the app only. -->
# Task 031: About page, the mandatory subscription screen, update and announcement notices

## 0. CRITICAL: force update locks users who already updated (do this first)
`core/updates/AppUpdateManager.kt` line ~46:
```kotlin
val isForceUpdate = obj.optBoolean("isForceUpdate", false) || (BuildConfig.VERSION_CODE < minVersion)
```
When the dashboard publishes a release with "تحديث إجباري" (`web_dashboard/api/release_manager.php`),
it writes `isForceUpdate: true` and `minRequiredVersionCode = <new code>` to `/app_broadcast`. Because
of the `||`, users who **already installed** the new version still see `ForceUpdateScreen` and can
never use the app.
- The rule must be: **force only when `BuildConfig.VERSION_CODE < minRequiredVersionCode`.** The
  `isForceUpdate` flag alone never blocks; it only marks that the broadcast is an update. When the
  flag is true and the app is already at or above the minimum, show nothing for that broadcast
  (no announcement either).
- **Offline:** cache the last `minRequiredVersionCode` that was read (SharedPreferences, same
  prefs file). At start, if there is no internet, use the cached value, so an old version cannot
  skip a force update by turning the internet off. Never write it to Firebase.
- Unit tests for a pure decision function `shouldForceUpdate(currentCode, minCode, flag)`:
  - (4, 5, true) → true;
  - (5, 5, true) → false;
  - (6, 5, true) → false;
  - (4, 1, true) → false;
  - (4, 5, false) → true.

## A. About page (`features/about/AboutScreen.kt`)
1. **Version from the build, never typed by hand:**
   - today the page shows a fixed "الإصدار 1.0.0";
   - show `BuildConfig.VERSION_NAME` and the build number `BuildConfig.VERSION_CODE`:
     "الإصدار 1.4.0 (بناء 5)";
   - make the copyright year dynamic (current year).
2. **Rewrite the page, shorter and clearer**, in this order:
   - **App card:** icon, "المُسَرِّب", the one-line description "إدارة توزيع مياه الآبار والري،
     بدون إنترنت", the version, and the license chip (مفعّلة / تجريبية · N عملية متبقية). Keep
     the existing activate / renew button and the device code with a copy button.
   - **What the app does:** 5 short rows with icons, each one line:
     - السقي بالدقيقة والحساب الآلي؛
     - السندات وكشوف الحساب PDF وواتساب؛
     - القراءة الصوتية للمبالغ والعمليات؛
     - الإدخال الصوتي (only if task 030 is merged; otherwise leave it out)؛
     - النسخ الاحتياطي المحلي وGoogle Drive، ويعمل بدون إنترنت.

     Replace the current long paragraph.
   - **Developer card:** "م. عمر الحومي · فريق تُبّع سوفت", one line of role, location. Contact
     buttons: واتساب، اتصال، إيميل.
   - **Links:** موقع التطبيق **mosarib.tubbasoft.com** (new), الموقع الشخصي, تُبّع سوفت.
   - **Footer:** "© {year} م. عمر الحومي · تُبّع سوفت".
   - Keep every existing link, number and email (take them from `LicenseManager` constants and
     the existing code, not new hard-coded copies). Tokens and typography only.

## B. Mandatory subscription screen (`core/license/LicenseDialog.kt` with `isMandatory = true`)
1. **The user must be able to leave the app** (owner). Today back does nothing. A
   `BackHandler` that calls `finishAffinity()` is in the parent composition, but the sheet window
   gets the back press (the same cause as in task 026).
   - In mandatory mode, handle back **inside the sheet content** and call
     `(context as? Activity)?.finishAffinity()`.
   - Add a visible text button **"خروج من التطبيق"** at the bottom, which does the same.
   - The sheet still cannot be dismissed into the app (keep the exact `isMandatory`
     `confirmValueChange` rule from 023). Leaving closes the app; it does not open it.
2. **Styling only:** a clear title and the reason line (from the existing `mandatoryReason`), the
   plans, the key field, then the contact buttons. Change no activation, validation or plan
   logic.

## C. Update and announcement notices (`core/updates/*`, `MainScreen.kt` call sites only)
1. **`ForceUpdateScreen`:**
   - the same visual language as the app;
   - remove the emojis from the titles and buttons ("تحديث إجباري ملزم 🚀", "… الآن 📥");
   - show "إصدارك: {VERSION_NAME}" and "المطلوب: {required}". Today the required value can show a
     version code as if it were a name; show the required **version name** if the broadcast has
     it, otherwise "بناء {code}";
   - buttons: "تحميل التحديث" (primary), "تواصل مع المطور" (secondary), and **"خروج من التطبيق"**;
   - the system back button also leaves the app.
2. **`BroadcastAnnouncementDialog`** (announcements and soft update):
   - use `MosaribDialog` (or the shared sheet), with the tone by type:
     - Info for news;
     - Success for "تحديث متوفر";
     - Warning for maintenance;
   - title, message (scrollable when long), the optional "ملاحظات التحديث" as a short bullet
     list, and the buttons "تحديث" / "لاحقاً" or "حسناً";
   - "لاحقاً" must not show the same announcement again in the same day (use the existing
     dismissed-id storage if there is one; otherwise store the id in `app_settings`).
3. Review the other app-level messages shown from `MainScreen.kt`:
   - device limit;
   - blocked device;
   - "شاركنا رأيك" prompt;
   - license expiry soon.

   Make them use the shared dialog/sheet components, with no emojis in titles and buttons and
   with Arabic text that says what happened and what to do. List each message before and after
   in the report.

## D. The date line in text messages (owner, 2026-11-03)
Every WhatsApp/SMS text from `core/util/MessageFormatHelper.kt` has "📅 التاريخ: …". The 📅 emoji
is a fixed picture that most phones draw as **"17"** (July 17), whatever the date, so it looks
wrong next to the real date.
- Replace the emoji with the **day of the operation in keycap digits**, then the weekday and the
  date:
  - one digit: "5️⃣ الأربعاء · 05/11/2026";
  - two digits: "2️⃣7️⃣ الثلاثاء · 27/10/2026".
  - Build the keycaps from the day number (`digit + "️⃣"`).
- **Which date:**
  - sessions: the irrigation start day;
  - vouchers: the voucher date;
  - purchases: the purchase date;
  - statements: the statement date ("حتى").
  - Never the current time, except for the "today" summary (line ~282), which is today.
- Western digits, weekday in Arabic.
- One helper `MessageFormatHelper.dateLine(millis, label = "التاريخ")` used by every message.
  Unit tests: days 1, 9, 10, 17, 31, and the weekday name.

## Allowed files
- `features/about/AboutScreen.kt`
- `core/license/LicenseDialog.kt` (styling, the back handling and the exit button only)
- `core/updates/ForceUpdateScreen.kt`, `core/updates/BroadcastAnnouncementDialog.kt`,
  `core/updates/AppUpdateManager.kt` (only for the dismissed-id storage, if needed)
- `MainScreen.kt` (the notice call sites only)
- `core/util/MessageFormatHelper.kt` (part D only)
- tests

## Done when
- Build and tests pass.
- On the device:
  - the About page shows the real version;
  - the mandatory screen: back and "خروج من التطبيق" close the app, and nothing opens the app
    without activation (test with a debug flag or a test build state; do not touch real license
    data);
  - the force-update screen and one announcement are described in the report, in light and dark.

---
## Review notes (written by Claude)
_(empty)_
