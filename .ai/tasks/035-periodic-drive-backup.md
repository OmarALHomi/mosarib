status: ready
<!-- Owner request 2026-11-04: periodic automatic backup to Google Drive. -->
# Task 035: Real periodic automatic backup to Google Drive

## Today (found by Claude)
- `core/util/AutoBackupEngine.performDailyAutoBackup` runs **only when the app starts**
  (`AppSyncEngine.performConsolidatedStartupSync`), once a day. It makes a local copy (keeps 7)
  and uploads to Drive only if a Google account is signed in **and the internet is on at that
  moment**.
- So when the app is not opened, or there is no internet at start, nothing reaches Drive. Nothing
  retries later, and there is no WorkManager in the project.

## What to build
1. **WorkManager** (`androidx.work:work-runtime-ktx`, latest stable): a unique periodic work
   `mosarib_drive_backup`.
   - Constraints: network connected, battery not low.
   - Exponential backoff on failure.
   - It runs the same backup (local file plus Drive upload) even when the app is closed.
   - Skip when nothing changed since the last successful Drive backup: compare the DB modification
     time or a change counter.
2. **Setting in the backup page** (`SettingsBackupScreen`), "النسخ التلقائي إلى Google Drive":
   - frequency: يومياً (default) / كل 3 أيام / أسبوعياً / إيقاف;
   - "على الواي فاي فقط" switch (off by default);
   - status line: "آخر نسخة إلى Drive: {date time}" or "لم تتم أي نسخة بعد", plus the last error
     in plain Arabic ("لا يوجد إنترنت"، "الحساب غير مربوط"، "مساحة Drive ممتلئة");
   - the button "انسخ الآن" enqueues a one-time work and shows progress.
3. **Startup:** keep the local daily copy at start. If the last Drive backup is older than the chosen
   period, enqueue a one-time Drive work. Never block startup.
4. **Drive retention:** keep the last 10 backups in the app-data folder and delete older ones made by
   the app only.
5. If the Google account is not linked, the periodic work does nothing. The backup page shows
   "اربط حساب Google لتفعيل النسخ إلى Drive" with the existing sign-in button.
6. A notification only on repeated failure (3 in a row): "تعذّر النسخ إلى Google Drive منذ {n}
   أيام". No notification on success.
7. Western digits in all dates.

## Allowed files
- `core/util/AutoBackupEngine.kt`, `core/util/GoogleDriveBackupHelper.kt`
- `core/sync/AppSyncEngine.kt` (startup enqueue only)
- new `core/backup/DriveBackupWorker.kt`
- `features/settings/SettingsBackupScreen.kt`, `SettingsBackupSection.kt`,
  `SettingsBackupUiState.kt`
- `app/build.gradle.kts` and `gradle/libs.versions.toml` (WorkManager only)
- `MosaribApplication` / `MainActivity` (only if WorkManager needs initialization)
- tests: the decision "is a Drive backup due" as a pure function (period, last success, changed?)

## Done when
- Build and tests pass.
- On the phone:
  - a forced run (`adb shell cmd jobscheduler run -f omarAlhomi.mosarib.com <id>` or a debug button)
    uploads with the app closed;
  - the status line updates;
  - with no internet the work waits and runs when the internet returns.
- Never touch real user data beyond making the backup. No restore is triggered.
