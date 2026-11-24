status: accepted
<!-- Owner request 2026-11-22: simplify backup and periodic backup settings. -->
# Task 054: Backup page — one status card, one switch, one restore list

File: `features/settings/SettingsBackupScreen.kt` (+ `SettingsBackupDialogs.kt` only to reuse dialogs).
Today: sections «إجراءات النسخ السريع» (نسخة جديدة / استيراد ملف / مشاركة), «النسخ الاحتياطي المحلي»,
«النسخ التلقائي إلى Google Drive» (frequency, wifi, last error), «سحابة Google Drive (الملفات
المحفوظة)», and many restore/delete dialogs. Too many choices for a simple user.

## Target (both modes), top to bottom
1. **Status card**: «آخر نسخة على الهاتف: <relative date>» and «آخر نسخة على Drive: <date / غير
   مفعّل>», with a green check when today, amber when older than 7 days. One big button
   «انسخ الآن» (local copy, and Drive too if linked — use the existing actions).
2. **النسخ التلقائي**: one row with a switch. When on: chips «يومي / أسبوعي» (map to the existing
   frequency values), and the Google account row («مرتبط بـ name@…» or «ربط بحساب Google»).
   «على الواي فاي فقط» and the last error go under a small «إعدادات أخرى» expander.
3. **استعادة**: one button «استعادة نسخة» opening one list of local and Drive copies together,
   newest first; each row: source icon (phone / Drive), date, size. Tap → the existing restore
   confirmation. Keep the existing restore code paths.
4. **متقدم** (collapsed): استيراد ملف، مشاركة ملف النسخة، تنظيف النسخ القديمة، فحص العمليات المكررة،
   حذف نسخ — each opening the dialogs that exist today.
Keep every backup/restore/Drive function, safety prompt and the rule "one Drive copy per day"
unchanged; this is layout and wording.

## Allowed files
features/settings/SettingsBackupScreen.kt, features/settings/SettingsBackupDialogs.kt.

## Done when
Build + tests pass; the page shows the four blocks above; every old action is still reachable.
