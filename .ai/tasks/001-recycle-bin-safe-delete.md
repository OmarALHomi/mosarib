status: ready

# Task 001: Make permanent delete in the recycle bin safe

## Background
The recycle bin (archived customers) can permanently delete one customer
(`permanentlyDeleteCustomer`) or all archived customers (`emptyRecycleBin`). Both currently run a
bare `DELETE FROM customers`, but the related tables have different foreign-key rules:

| Table | Rule on `customerId` | Effect today |
|---|---|---|
| `WaterSession` | `CASCADE` | all the customer's sessions vanish silently from reports and totals |
| `Voucher` | `SET_NULL` | receipts/payments stay with no owner and can skew report totals |
| `WellOwnerPurchase` | `RESTRICT` | delete throws `SQLiteConstraintException`; nothing catches it, so the app crashes, and one such account makes "empty recycle bin" fail entirely |

## Required behaviour
1. Permanently deleting a customer removes the customer **and** all their sessions, vouchers
   and well-owner purchases, atomically, in one Room `@Transaction`. Either everything is
   deleted or nothing is.
2. `emptyRecycleBin` does the same for every archived customer, in one transaction.
3. Before the delete runs, the confirmation dialog tells the user how many records will be
   removed, e.g. "سيتم حذف الحساب نهائياً مع 12 جلسة و 5 سندات و 2 مشتريات. لا يمكن التراجع."
   For "empty recycle bin" show the totals across all archived customers.
4. Any database error is caught in the ViewModel and shown with the existing toast
   (`showToast(..., ToastType.ERROR)`); the app must not crash.
5. Only archived customers can be permanently deleted: the DAO queries must include
   `AND isArchived = 1` so an active customer can never be removed by mistake.

## Allowed files
- `app/src/main/java/com/example/features/customers/CustomerDao.kt`
- `app/src/main/java/com/example/features/customers/CustomerRepository.kt`
- `app/src/main/java/com/example/features/customers/CustomersViewModel.kt`
- `app/src/main/java/com/example/features/customers/RecycleBinDialog.kt`
- `app/src/main/java/com/example/features/sessions/WaterSessionDao.kt` (only if you need a delete/count query)
- `app/src/main/java/com/example/features/vouchers/VoucherDao.kt` (same)
- `app/src/main/java/com/example/features/wellowners/WellOwnerPurchaseDao.kt` (same)
- A new unit test file under `app/src/test/java/com/example/` is welcome.

## Out of scope
- Do not change the foreign-key rules or the database version (no schema change is needed).
- Do not change the archive/restore behaviour or the recycle bin's look beyond the new
  confirmation text.

## Done when
- Build and tests pass.
- The report lists the new DAO queries and shows the exact confirmation texts used.

---
## Review notes (written by Claude)
_(empty)_
