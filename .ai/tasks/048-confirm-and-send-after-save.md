status: accepted
<!-- Owner request 2026-11-22. Both modes. Starts after 046 and 047 are merged. -->
# Task 048: Confirmation statement with read-aloud before save, send/copy after save

## 0. Finish the one-screen irrigation form (phone check after 046, 720x1600 px)
Today the «الواصل» input is cut at the bottom and «خيارات إضافية» needs a scroll. In
`AddEditSessionBottomSheet`:
- Put «سعر الساعة» and «الواصل (المدفوع الآن)» side by side in one row (each half width, each with
  its own small تفقيط line); «دفع كامل» chip stays on the الواصل label line.
- Under that row the summary card becomes one compact row of two columns: «المطلوب» | «المتبقي عليه»
  (red when > 0, green «خالص» at 0), each with its small تفقيط. Do not repeat «الواصل» in the card.
- «خيارات إضافية» stays last. Target: everything visible without scrolling on 360x800dp.
Also in `features/home/HomeCustomerList.kt`: give the list bottom content padding equal to the totals
box height so the last customer card is not hidden behind the box.

## 1. Confirmation before save (session, قبض, صرف, well-owner purchase)
In `AddEditSessionBottomSheet` (SessionDialogs.kt), `AddVoucherBottomSheet` (VouchersScreen.kt) and
`WellOwnerPurchaseBottomSheet`: pressing the save button no longer saves at once. It opens a small
confirmation sheet/dialog «تأكيد العملية» showing one plain Arabic statement built from the form
values, e.g.
- سقي: «سقي لـ أحمد يوم الأحد 22/11 من 8:10 إلى 9:10 مساءً، ساعة واحدة، المطلوب 5,000 ريال،
  الواصل 2,000، المتبقي عليه 3,000»
- قبض: «قبض من أحمد مبلغ 3,000 ريال» (+ البيان if any) ; صرف: «صرف لـ … مبلغ …»
- شراء ساعات: owner, hours, amount.
With: `SpeakButton` (core/speech) reading the same statement (amounts read in words via
`SpeechTextBuilder`), and two buttons «تأكيد الحفظ» (then calls the existing onSave) and «تعديل»
(closes the confirmation, form stays as typed). Put the statement builders in a new
`core/util/OperationStatement.kt` with unit tests.
Voice direct-save from the «فهمت» card is already a confirmation: do not add a second one there.

## 2. Send / copy after save (every entry point)
After any successful save of a session, receipt, expense or purchase — from Home, the Operations
screen, the customer page, the Sessions screen or voice direct save — show `SendMessageChoiceDialog`
(copy, WhatsApp, SMS) with the message from `MessageFormatHelper` (buildSessionBill /
buildReceiptVoucher / buildDisbursementVoucher / buildPurchaseBill).
- Do it in one place: a small app-level holder (e.g. `core/ui/PostSaveMessage.kt` with a
  `MutableStateFlow<PostSaveMessage?>`) that the view models set after the insert succeeds (they have
  the saved entity and the customer), and `MainScreen.kt` shows the dialog. Remove the existing
  per-screen post-save send dialogs (e.g. `homePurchaseTarget` in HomeScreen) so it never shows twice.
- Customer without phone: still show the dialog; copy works, WhatsApp/SMS open without a number.
- Editing an existing operation: no send dialog.

## Allowed files
features/home/HomeCustomerList.kt (padding only), features/sessions/SessionDialogs.kt, features/vouchers/VouchersScreen.kt,
features/wellowners/WellOwnerPurchaseBottomSheet.kt, core/util/OperationStatement.kt (new) + test,
core/ui/PostSaveMessage.kt (new), core/ui/SendMessageChoiceDialog.kt (only for the no-phone case),
MainScreen.kt, features/sessions/SessionsViewModel.kt, features/vouchers/VouchersViewModel.kt,
features/customers/CustomersViewModel.kt, the wellowners view model/repository if purchases are saved
there, and the screens whose old post-save dialogs are removed (HomeScreen.kt, SessionsScreen.kt,
CustomerDetailScreen.kt, VouchersScreen.kt).

## Done when
Build + tests pass; every new operation shows the statement + read-aloud before saving and the send
dialog after saving, exactly once.
