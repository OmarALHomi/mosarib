status: accepted
<!-- Owner OK 2026-11-24. App side of direct activation. -->
# Task 066: App re-checks its subscription when it comes back to the foreground

Today `LicenseManager.syncRemoteSubscriptionStatus` (reads `devices/{deviceCode}`) runs only once per
launch from `core/sync/AppSyncEngine.kt`. After the admin activates/extends/revokes from the dashboard
the user must restart the app.
1. Run the same sync when the app returns to the foreground (`ProcessLifecycleOwner` ON_START or the
   activity's onResume), throttled: at most once every 10 minutes, and only with network. No
   WorkManager, no polling loop.
2. When the result changes from not-activated to activated (or the expiry moves later), show a
   success toast/snackbar «تم تفعيل اشتراكك 🎉» (or «تم تمديد اشتراكك حتى <date>»), and refresh the
   UI state that depends on activation (`settingsViewModel.refreshActivationStatus()` or equivalent).
   Revocation keeps today's behaviour.
3. Do not change how validity is decided (isRevoked, expiresAt, device code, signature). Put the
   throttle decision in a pure function with a unit test.
## Allowed files
core/sync/AppSyncEngine.kt, core/license/LicenseManager.kt (only to return/expose the change result,
no validation change), MainActivity.kt, MainScreen.kt (only for the toast hook if needed), new test.
