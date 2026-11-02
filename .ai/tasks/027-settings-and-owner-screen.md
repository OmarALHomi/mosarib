status: draft

<!-- Owner request 2026-11-02. Runs AFTER 025 is merged: it touches CustomerDetailScreen.kt and the
Settings files, which 025 also changes. Claude may show the owner a mockup before it becomes
ready. -->
# Task 027: Organise the Settings screen and the well-owner page

## A. Well-owner page (`features/customers/CustomerDetailScreen.kt`, owner accounts only)
1. **Do not remove the expense action.** Task 011 replaced the owner's second big button with
   "دفع له (صرف)". Rename it to **"سند جديد"** (icon `ReceiptLong`). It opens a small choice
   (a `MosaribMenu`, or two options in a `MosaribBottomSheet`):
   - **"صرف له (دفع)"** → the existing owner payment flow (EXPENSE);
   - **"قبض منه"** → the existing receipt flow (RECEIPT) for this owner.

   The first big button stays **"شراء ساعات"**.
2. **The main balance card is too big** (owner). Make it compact:
   - one row: the label "المستحق لصاحب البئر" / "على صاحب البئر" and the amount (`headlineSmall`
     bold, state colour), with the exact amount in words under it (`bodySmall`, inline);
   - under it **one** row of 3 small stats (`labelSmall` label + `titleSmall` value): "صافي
     الساعات المشتراة"، "المدفوع له"، "الهدر";
   - remove any duplicated figures, hide the stats that are zero, and drop the big decorative
     icon or illustration if present;
   - target height ≤ 170dp at 384dp. Measure it with uiautomator and write it in the report.
3. Keep the ledger cards, the FAB and the header actions from 011 / 025.

## B. Settings screen (`features/settings/SettingsScreen.kt`, `SettingsGeneralSections.kt`, …)
Today every setting is a big card with its own inline form (price, distributor data, backup,
license, …), so the screen is very long. Reorganise it as a **grouped list**, like system
settings:
- Section headers (`labelLarge`, `primary`), then rows inside one rounded card per section.
- Row (56dp): leading icon in a 36dp tinted circle, title (`bodyLarge`), one-line subtitle with
  the current value (`bodySmall`, muted), trailing chevron or switch.
- Editing opens a `MosaribBottomSheet` with the existing form (same validation and save
  handlers), not an inline form.

Sections and rows:
1. **الحساب والاشتراك:** حالة الاشتراك (subtitle "اشتراك شهري · متبقي 12 يوماً", opens the
   existing license dialog), رمز الجهاز (copy button).
2. **التسعير والموزع:** سعر ساعة الماء ("5,000 ر.ي"), بيانات الموزع (name · phone), رمز العملة.
3. **البيانات:** النسخ الاحتياطي (last backup date), سلة المحذوفات (count), إدارة المجموعات,
   فحص العمليات المستوردة المكررة (from 008).
4. **المظهر والصوت:** الوضع الداكن (switch), القراءة الصوتية (from 025: rate, test, status).
5. **المساعدة:** دليل الاستخدام، شاركنا رأيك، عن التطبيق.
6. **أدوات المطور** (debug builds only, as today, at the very bottom).

Every existing setting and action must still be reachable. List old → new place in the report.

## C. Leftover from 026 (session detail viewer)
- In "الدفعات المرتبطة", label each line by the voucher type: "خصم · 750", "قبض · 6,000"
  (not "سداد" for everything), with the type colour.
- The count on the ledger card ("السداد (N)") and the count in the viewer must match. Use one
  shared definition.

## D. Customer page balance colour and label (owner, 2026-11-03)
On the customer page the main balance looks red whatever its kind. Seen on the device: customer
"أحمد بن حسين العولقي" with a card that shows "له: 15,339" in green has the page label "رصيد دائن
لصالح العميل (عليك)" with **15,339 in red**. Use the same convention as the customer cards,
everywhere on this page (normal customers, beneficiaries and well owners):
- **عليه** (the customer owes you): `MaterialTheme.mosarib.debt` (red). Label: "عليه (لك)".
- **له** (you owe the customer): `MaterialTheme.mosarib.credit` (green). Label: "له (عليك)".
- **مسدد**: `primary`, with the existing "مسدد" chip.
- Find out why the credit case renders red today. There are two balance branches
  (`customerWithBalance.balance` near line 704 and `receivableBalance` near line 787); check
  which one runs for each account type. Use the **same** sign rule as `CustomerCardItem` so the
  card and the page can never disagree.
- Replace the hard-coded `Color(0xFFE53935)` / `AccentEmerald` / `PrimaryTeal` in this card with
  the tokens.
- On the device, check one customer with عليه, one with له, one مسدد and one well owner, and
  list the four in the report.

## Allowed files
- `features/customers/CustomerDetailScreen.kt` (owner section, and the balance card for part D)
- `features/settings/*` (layout only, no change to ViewModel logic or backup behaviour)
- new `features/settings/SettingsRow.kt` (shared row and section composables)
- `core/ui/SessionDetailBottomSheet.kt` (part C only); the ledger card count in `CustomerDetailScreen.kt`

## Done when
- Build and tests pass. Device screenshots in light and dark themes are described in the report.

---
## Review notes (written by Claude)
_(empty)_
