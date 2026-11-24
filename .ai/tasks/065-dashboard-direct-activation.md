status: accepted
<!-- Owner OK 2026-11-24. Web dashboard only. Owner uploads changed files by hand (never hosting_deploy). -->
# Task 065: Dashboard — activate / extend / revoke straight from the lists

Files: `web_dashboard/assets/js/admin-core.js`, the widget PHP files that render the users and requests
tabs (`web_dashboard/includes/widgets/widget_*.php`), `web_dashboard/assets/css/admin.css` if needed.
Today: the requests row has «تفعيل» (`quickActivateRequest` ~1600 → `generateAndSaveKey` ~2116, writes
`licenses/{key}` and `devices/{MSRB-xxxx-xxxx}`); the users table (`renderUsersTable` ~1756, row
buttons ~1883–1917) has no activation button. The app already activates itself from
`devices/{deviceCode}` on its sync (LicenseManager.syncRemoteSubscriptionStatus).

## 1. Users row actions
- Not subscribed → «تفعيل»: small modal (plan شهري / سنوي, name prefilled from the user row) →
  reuse `generateAndSaveKey` / `quickActivateRequest` (no second copy of the key logic). The device
  code must be the dashed form `MSRB-xxxx-xxxx` the app reads (rebuild it from the stored key if the
  row holds a cleaned key). After success: toast + offer «إرسال واتساب» (wa.me link with the
  existing message template if one exists).
- Subscribed → «تمديد» (plan شهري/سنوي): new `expiresAt` = max(now, current expiresAt) + 30/365
  days, written to **both** `licenses/{key}` and `devices/{code}` (same key, not a new one).
- Subscribed → «إلغاء التفعيل» with a confirm dialog: `isRevoked = true` on both nodes.
- Update the row and KPI counters without a full reload.
## 2. Requests row
«تفعيل» becomes «تفعيل وإرسال واتساب»: same activation, then opens the WhatsApp message to the
requester's phone with their key and plan. Keep a plain «تفعيل» in the ⋮ or as secondary.
## 3. Safety
All writes keep going through `api/firebase_proxy.php` with the CSRF header (existing helpers in
`firebase-api.js`). No new Firebase paths, no secrets in JS. Validate plan and device code format
before writing; show errors from the proxy.
## Checks
`node --check` on edited JS; `php -l` on edited PHP. (No live Firebase locally.)
## Report
List every changed file (owner uploads exactly these).
