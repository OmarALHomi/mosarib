status: accepted
<!-- Owner request 2026-11-24. -->
# Task 070: Restore list with two tabs (Drive / الهاتف) and a clear "no internet" message

Today «استعادة نسخة» (and «حذف نسخ») opens `BackupListDialog` (features/settings/SettingsBackupDialogs.kt)
with one mixed list built by `buildBackupEntries` (SettingsBackupScreen.kt), phone and Drive copies
sorted together.

## 1. Two tabs in the list dialog
At the top of `BackupListDialog`, two tabs (segmented chips like the «يومي / أسبوعي» pair on the same
screen): «Drive» and «الهاتف». Each tab lists only its own copies (newest first), same row design, same
restore / share / delete behaviour as today. Default tab: «Drive» when a Google account is linked,
otherwise «الهاتف». Works in both restore and manage modes.

## 2. Drive tab states (instead of a silent empty list)
In this order:
- no Google account linked → «لم يتم ربط حساب Google. اربطه من «النسخ التلقائي» أعلاه.»
- no internet (`LicenseManager.isInternetAvailable(context)`, checked when the dialog opens / the tab is
  shown) → an icon (CloudOff / WifiOff) and «لا يوجد اتصال بالإنترنت — لا يمكن عرض نسخ Drive الآن.»
  with a small «إعادة المحاولة» button (re-check internet and call the existing Drive refresh).
- loading → «جارٍ تحميل نسخ Drive...»
- empty → «لا توجد نسخ على Drive»
Phone tab empty → «لا توجد نسخ على الهاتف».

## 3. Raw English error on the backup screen
The Drive status line under «إعدادات أخرى» shows a raw «NetworkError». Show Arabic instead
(«تعذر الاتصال بالإنترنت») — map it where it is displayed or recorded, using
`GoogleDriveBackupHelper.getReadableErrorMessage` if it fits.

## Allowed files
features/settings/SettingsBackupDialogs.kt, features/settings/SettingsBackupScreen.kt, and (only for
item 3) the file that records/displays that error (DriveBackupWorker.kt / DriveBackupPreferences /
GoogleDriveBackupHelper.kt). Pass extra params from SettingsScreen.kt only if needed.

## Done when
Build + tests pass; the list has Drive / الهاتف tabs; with no internet the Drive tab says so.
