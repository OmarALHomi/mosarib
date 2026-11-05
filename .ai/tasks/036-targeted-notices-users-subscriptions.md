status: done
<!-- Owner request 2026-11-04. App + dashboard. Never add secrets to the app or the dashboard JS. Never let
the app write license/device/block/config data to Firebase. Keep database.rules.json deny-by-default;
if a rule must change, list it in the report and Claude deploys it. -->
# Task 036: Targeted notices and force update, notification links and images, user deletion, subscription dates

Facts found by Claude:
- The app already reports `versionCode` and `appVersion` (`core/license/UserDeviceTracker.kt` ~205).
- `api/fcm_sender.php` sends to all `fcm_tokens`, or filters TRIAL / SUBSCRIBERS (~220).
- `/app_broadcast` holds a single broadcast with `minRequiredVersionCode` (`core/updates/AppUpdateManager.kt`).
- `MosaribFirebaseMessagingService.kt` (~66) always opens the app with a PendingIntent and ignores links.
- `LicenseManager` keeps `activatedAt` / `expiresAt`.

## A. Target by app version (notifications and announcements)
1. **Dashboard send forms** (push notification and in-app announcement) get a "الفئة المستهدفة" block:
   - all;
   - trial / subscribers (existing);
   - **app version:** "إصدار محدد", "أقدم من", "من … إلى" by build number;
   - the list of versions in use is built from `all_users/*/versionCode`, with a user count per
     version.
   - Show "سيصل إلى {n} مستخدم" before sending.
2. **Push:** `fcm_sender.php` filters tokens by the `versionCode` of the user that owns the token.
   - Add `versionCode` next to the token when the app registers it, and keep it updated on every
     start (an allowed app write, the app's own token node only).
3. **In-app announcement:** add optional `targetMinVersionCode` / `targetMaxVersionCode` to
   `/app_broadcast`. `AppUpdateManager` shows it only when `BuildConfig.VERSION_CODE` is in range.
   Pure function plus unit tests.

## B. Force update for specific versions
1. In the release manager, "تحديث إجباري" gets a choice:
   - **"كل الإصدارات الأقدم من {min}"** (today's rule);
   - **"إصدارات محددة فقط"** (a list of build numbers, for example a buggy build 4).
2. `/app_broadcast` gets `forceVersionCodes: [4]` (optional).
   - Decision: force when `current < minRequiredVersionCode`, OR `current in forceVersionCodes`.
   - Same offline cache rule as task 031: write it every time, clear it when the broadcast is empty.
3. Update `shouldForceUpdate` and its tests: (4, min 1, [4]) → true; (5, min 1, [4]) → false;
   (3, min 4, []) → true.
4. Keep the warning text from task 033 and show how many users will be locked.

## C. Notification links and images
1. **Link:** when a push has `url` (https only):
   - tapping opens the link in the browser (`ACTION_VIEW`), not the app;
   - when `openInApp=true` or no url, open the app as today;
   - add a "فتح الرابط" action button too.
   - A `mosarib://` deep link (for example `mosarib://settings/backup`) opens that screen in the app.
     Support the main screens only and ignore unknown ones.
2. **Image:** optional `imageUrl` (https).
   - Push: `BigPictureStyle`, image downloaded with a 5 s timeout, falling back to text if it fails.
   - FCM message: also set `notification.image`.
   - In-app announcement dialog: show the image at the top (max 180 dp high, rounded), with Coil if
     the project already has it; otherwise a small loader. Do not add a heavy library.
3. **Dashboard:** image upload for notifications/announcements to `uploads/notices/`.
   - jpg / png / webp only, max 1 MB, magic-byte check, random file name.
   - Or an https URL field.
   - Preview of the notification before sending.

## D. Deleting users (think it through)
Do **not** hard-delete by default. In the dashboard users list:
1. **"تعطيل"**: sets `blocked=true` with a reason (existing block logic). Reversible.
2. **"حذف من القائمة" (soft delete)**:
   - moves the user record to `deleted_users/{id}` with `deletedAt`, `deletedBy` and the reason;
   - removes it from `all_users`, `email_index` and `fcm_tokens`.
   - Restorable from a "المحذوفون" tab for 30 days, then purged by the dashboard (server side).
3. **"حذف نهائي"**: only SUPER_ADMIN, only from the deleted tab. Confirmation requires typing the
   user's name or email.
4. **Never delete license records.** A deleted user who opens the app again is treated as a new
   registration. An active license stays valid for its device.
5. Log every delete or restore (who, when, which user) in `data/admin_audit.json`, with flock.
6. Bulk actions: select several, then disable or soft-delete them, with a count confirmation.

## E. Subscription dates
1. **Users list and user details:**
   - "بداية الاشتراك" (activatedAt), "نهاية الاشتراك" (expiresAt), "المتبقي: {n} يوم";
   - the plan name;
   - Western digits, dates as dd/MM/yyyy.
2. **New dashboard section "اشتراكات تنتهي قريباً":**
   - filters 7 / 15 / 30 days, plus "منتهية";
   - sorted by nearest end;
   - each row has the name, phone, plan, end date, days left, and buttons "تذكير" (sends a push to
     that user only) and "واتساب" (opens wa.me with a ready reminder message);
   - a count badge on the dashboard home.
3. The data comes from the existing license records (read only from the dashboard).

## Allowed files
- App:
  - `core/notifications/*`;
  - `core/updates/AppUpdateManager.kt`, `BroadcastAnnouncementDialog.kt`;
  - `core/license/UserDeviceTracker.kt` (token `versionCode` only);
  - `MainActivity` (deep-link intent handling only);
  - tests.
- Dashboard:
  - `api/fcm_sender.php`, `api/release_manager.php`, `api/public_data.php`;
  - `assets/js/admin-core.js`, `admin.php`, `assets/css/*`;
  - `uploads/notices/.htaccess` (no script execution) and `uploads/notices/index.html`;
  - `data/admin_audit.json` (new, empty `[]`).
- `database.rules.json`: only if needed, listed in the report.

## Done when
- Build, unit tests and `php -l` pass.
- On the phone:
  - a push with url opens the browser;
  - a push with image shows the picture;
  - an announcement targeted at another build does not show.
- The report has screenshots of the dashboard: version targeting, force-update choice, deleted tab,
  expiring subscriptions.
- No secrets added. Every admin write keeps the CSRF check from task 033.

## Done by Claude (owner asked), 2026-11-05
- App 1.4.3 (8): `forceVersionCodes` + cached offline; announcement `targetMin/MaxVersionCode`; `imageUrl` in announcements and pushes (`RemoteImageLoader`, BigPictureStyle); push tap opens https in the browser, `mosarib://` opens a screen (`DeepLinks`, MainActivity singleTop + onNewIntent); «فتح الرابط» action. Tests in `AppUpdateManagerTest`.
- Token `versionCode` not added: `fcm_sender.php` joins `all_users/{key}/versionCode`, so no rules change.
- Dashboard: pushes are data-only (fixes taps opening only the app); ALL no longer double-sends (topic then per-token); version targeting with server-side count (`dry_run`); image upload to `uploads/notices/`; CSRF check added to `fcm_sender.php`; force-update choice (older / specific builds) in releases, external link and announcements with a locked-users count; `api/admin_users.php` soft delete / restore / hard delete (SUPER_ADMIN) with `data/admin_audit.json`; subscription start/end/days left, expiring list 7/15/30/ended with reminder and WhatsApp, count card on the overview.
- Not tested end to end: a real push with a picture and link, the dashboard pages (they need the live server and Firebase).
