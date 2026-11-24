status: accepted
<!-- Owner OK 2026-11-24 (suggestions 1,2,3,4,6). -->
# Task 064: Simpler Settings, backup reminder, clearer locked backup

1. **Settings in simple mode** (`features/settings/SettingsScreen.kt`):
   - «رمز الجهاز» row hidden in simple mode; show the device code inside the «حالة الاشتراك» screen/
     dialog instead (copy button there). Full mode unchanged.
   - «فحص العمليات المستوردة المكررة» hidden from Settings in simple mode (it stays under «متقدم» on
     the backup page).
   - «سعر ساعة الماء» and «رمز العملة» become one row «السعر والعملة» (subtitle «5,000 ر.ي») that
     opens the existing two editors one after the other or a small sheet with both fields. Both modes.
2. «طريقة الاستخدام» subtitle shortened to «المبسّطة: الأساسيات فقط — الباقي في «خيارات إضافية»»
   so it is not cut on 360dp (or allow 2 lines).
3. **Remaining-operations circle** (header of Home/Operations/Settings, `MainScreen.kt` and wherever
   it is drawn): show it only when the user is on the trial **and** remaining ≤ 20; hide it otherwise
   (subscribed or unlimited).
4. **Backup reminder on Home** (`features/home/HomeScreen.kt` or `HomeCustomerList.kt`): if the last
   local or Drive backup is older than 7 days (or none and the app has ≥ 1 operation), a thin amber
   banner «آخر نسخة احتياطية قبل N يوماً — انسخ الآن» (or «لا توجد نسخة احتياطية بعد»); tap runs the
   same silent «انسخ الآن» as the backup page (AutoBackupEngine checkpoint + Drive if linked) and
   shows a toast; a small ✕ hides it for 3 days. Use the dates the backup page already reads.
   If backup is locked for the trial, the banner opens the subscription card instead.
5. **Locked backup page** (`features/settings/SettingsBackupScreen.kt`): when backup is disabled for
   the trial, show one friendly card «النسخ الاحتياطي متاح للمشتركين — بياناتك محفوظة على هذا
   الهاتف» with a primary button «اشترك» (opens the subscription card) instead of the greyed-out page.

## Allowed files
features/settings/SettingsScreen.kt, features/settings/SettingsBackupScreen.kt, MainScreen.kt,
features/home/HomeScreen.kt, features/home/HomeCustomerList.kt, features/vouchers/VouchersScreen.kt
(only for the header circle), core/license/LicenseDialog.kt (only to show the device code in the
status view), a small pure helper + test for "days since last backup / should show banner".

## Done when
Build + tests pass; items 1–5 visible in simple mode; full mode keeps its rows.
