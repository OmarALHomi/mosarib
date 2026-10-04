#!/usr/bin/env node
/**
 * مصفوفة اختبار قواعد Firestore (ح١٥) — تُشغَّل على محاكي Firestore الحقيقي.
 *
 * الحقيقة التي تحكم هذا الملفّ: **قاعدة أمن لا تُختبَر ليست قاعدة**. ولذلك كل حالة من جدول §8.2
 * في خطة البناء موجودة هنا باسمها، مع شاهد مقابل لها: لو صار الرفض «رفضًا للكل» لسقطت شواهد
 * السماح، ولو انفتح باب لما كان لسقط حاجز الرفض. فالاختبار يسأل السؤالين معًا.
 *
 * التشغيل (يحتاج محاكيًا):
 *   tools/node_modules/.bin/firebase emulators:exec --only firestore \
 *     --project demo-baynana "node tools/firestore_rules_test.mjs"
 *
 * وفي CI خطوة مستقلّة تستدعيه، فلا يُدمج ملفّ قواعد بلا تشغيل المصفوفة عليه.
 */
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} from "@firebase/rules-unit-testing";
import {
  doc,
  getDoc,
  setDoc,
  updateDoc,
  deleteDoc,
  collection,
} from "firebase/firestore";

const here = path.dirname(fileURLToPath(import.meta.url));
const rulesPath = path.join(here, "..", "firestore.rules");
const rules = fs.readFileSync(rulesPath, "utf8");

const PROJECT_ID = "demo-baynana";
const results = [];

const testEnv = await initializeTestEnvironment({
  projectId: PROJECT_ID,
  firestore: { rules },
});

// ------------------------------------------------------------------ أدوات

/**
 * هل الرفض رفضُ صلاحية فعلًا؟
 *
 * هذا الفحص هو الفرق بين اختبار حقيقي واختبار يمرّ بالمصادفة: `assertFails` تفرح بأي رفض، ولو كان
 * `NOT_FOUND` لأن الوثيقة غير موجودة أصلًا أو `INVALID_ARGUMENT` لأن المسار خطأ. فحالة تُقاس على
 * قاعدة أمن يجب أن تُرفض بـ`PERMISSION_DENIED` صراحةً، وإلا فهي **لم تُختبر**.
 */
function isPermissionDenied(error) {
  if (!error) return false;
  const code = error.code ?? "";
  const status = error.status ?? error.statusCode ?? "";
  const message = String(error.message ?? "");
  return (
    code === "permission-denied" ||
    code === 7 ||
    status === "PERMISSION_DENIED" ||
    /permission|PERMISSION_DENIED|insufficient/i.test(message)
  );
}

/** يطبع سطرًا يلتقطه تقرير CI كتعليق (error:) وفيه اسم الحالة وسببها. */
function reportFailure(label, why, error) {
  const detail = error?.code ? `${error.code}: ${error.message}` : String(error?.message ?? error ?? "");
  console.error(`⛔ error: الحالة «${label}» ${why} — ${detail}`);
}

async function mustDeny(label, promise) {
  let error = null;
  let resolved = false;
  try {
    await promise;
    resolved = true;
  } catch (e) {
    error = e;
  }
  if (resolved) {
    reportFailure(label, "كان يجب أن تُرفض فسُمحت", null);
    throw new Error(`قاعدة مثقوبة: «${label}» سُمحت`);
  }
  if (!isPermissionDenied(error)) {
    reportFailure(label, "رُفضت لسبب غير الصلاحية (فحص بلا معنى)", error);
    throw new Error(`رفض لسبب آخر في «${label}»`);
  }
  results.push({ label, outcome: "denied" });
  // 🚫 = «رُفضت كما يجب». و⛔ محفوظة للفشل وحده، فيصل تقرير CI إلى اسم الحالة الفاشلة بلا لبس.
  console.log(`  🚫 ${label}`);
}

async function mustAllow(label, promise) {
  try {
    await assertSucceeds(promise);
  } catch (e) {
    reportFailure(label, "كان يجب أن تُسمح فرُفضت", e);
    throw e;
  }
  results.push({ label, outcome: "allowed" });
  console.log(`  ✅ ${label}`);
}

function asUser(uid, claims = {}) {
  return testEnv.authenticatedContext(uid, claims).firestore();
}

function asAnonymous() {
  return testEnv.unauthenticatedContext().firestore();
}

function asAdmin() {
  return testEnv.authenticatedContext("admin-uid", { admin: true }).firestore();
}

/** كتابة تحضيرية بلا قواعد: لبناء حالة سابقة فقط (لا نتحايل بها على أي فحص). */
async function seed(path, data) {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), path), data);
  });
}

// ------------------------------------------------------------------ التحضير

const ROOM = "rooms/room-water-1";
const ENTRY = `${ROOM}/entries/entry-1`;
const DEAL = `${ROOM}/deals/deal-1`;
const LISTING = "market_listings/listing-published-1";

await testEnv.clearFirestore();

const alice = asUser("alice");
const bob = asUser("bob");
const carol = asUser("carol"); // طرف ثالث لا علاقة له بشيء
const admin = asAdmin();
const anonymous = asAnonymous();

// حالة سابقة: غرفة بين alice وbob فقط.
await seed(ROOM, {
  memberUids: ["alice", "bob"],
  createdByUid: "alice",
  kind: "WATER",
  currency: "YER_NEW",
  status: "PENDING",
});
await seed(`${ROOM}/entries/entry-1`, {
  roomId: "room-water-1",
  createdByMemberId: "alice",
  amountMinor: 1500000,
  currency: "YER_NEW",
  type: "WATER_SESSION",
  owedByMemberId: "bob",
  owedToMemberId: "alice",
  operationId: "op-1",
  status: "SENT",
});
await seed(DEAL, {
  roomId: "room-water-1",
  partyUids: ["alice"],
  totalAmountMinor: 3000000,
  currency: "YER_NEW",
  createdByMemberId: "alice",
});
await seed(LISTING, {
  ownerUid: "alice",
  status: "PUBLISHED",
  hasPublicProjection: true,
  crop: "بن",
  district: "حرض",
});
// مسودّة حقيقية: النشر الذاتي يُختبر على مسودّة، لا على عرض منشور مسبقًا (كتابة الحالة نفسها
// لا تُعدّ تغييرًا في الفرق، فالسماح بها ليس ثقبًا ولا رفضها دليلًا).
await seed("market_listings/listing-draft-1", {
  ownerUid: "alice",
  status: "DRAFT",
  hasPublicProjection: false,
  crop: "بن",
  district: "حرض",
});
await seed("users/alice", { uid: "alice", isVerified: false, accountStatus: "ACTIVE", displayName: "أبو محمد" });
await seed("licenses/alice", { role: "MUSRIB", plan: "MONTHLY", expiresAt: 1 });
await seed("subscriptions/MSRB-8F42-9D1B", { uid: "alice", status: "ACTIVE" });
await seed("system_config/secrets", { anything: "يجب ألا يُقرأ" });

console.log("\n=== §8.2 حالات القبول الإلزامية ===");

// ١) مجهول أو طرف آخر يقرأ كشفًا ليس له
await mustDeny("§8.2/١ مجهول يقرأ غرفة فيها ديون", getDoc(doc(anonymous, ROOM)));
await mustDeny("§8.2/١ طرف ثالث يقرأ قيود غرفة ليس عضوًا فيها", getDoc(doc(carol, ENTRY)));
await mustDeny("§8.2/١ طرف ثالث يحاول قراءة ملفّ مستخدم غيره", getDoc(doc(carol, "users/alice")));

// ٢) مزارع مشارك يحاول تعديل سقية أو رصيدًا
await mustDeny(
  "§8.2/٢ عضو يحاول تعديل مبلغ قيد موجود",
  updateDoc(doc(bob, ENTRY), { amountMinor: 900 })
);
await mustDeny(
  "§8.2/٢ عضو يحاول تغيير طرفي القيد (الدائن والمدين)",
  updateDoc(doc(bob, ENTRY), { owedByMemberId: "alice", owedToMemberId: "bob" })
);
await mustDeny("§8.2/٢ عضو يحاول حذف قيد", deleteDoc(doc(bob, ENTRY)));
await mustDeny(
  "§8.2/٢ عضو يحاول تغيير معرّف العملية (فتح باب الازدواج)",
  updateDoc(doc(bob, ENTRY), { operationId: "op-2" })
);

// ٣) كتابة حقل ثقة أو اشتراك أو رفع حظر
await mustDeny(
  "§8.2/٣ مستخدم يوثّق نفسه (isVerified)",
  setDoc(doc(alice, "users/alice"), { uid: "alice", isVerified: true, accountStatus: "ACTIVE" })
);
await mustDeny(
  "§8.2/٣ مستخدم يعدّل توثيقه من حسابه القائم",
  updateDoc(doc(alice, "users/alice"), { isVerified: true })
);
await mustDeny(
  "§8.2/٣ مستخدم يرفع الحظر عن نفسه",
  updateDoc(doc(alice, "users/alice"), { blocked: false, accountStatus: "ACTIVE" })
);
await mustDeny(
  "§8.2/٣ مستخدم يكتب اشتراكه بنفسه",
  setDoc(doc(alice, "subscriptions/MSRB-8F42-9D1B"), { uid: "alice", status: "ACTIVE" })
);
await mustDeny(
  "§8.2/٣ مستخدم يكتب لنفسه استحقاقًا في licenses",
  setDoc(doc(alice, "licenses/alice"), { role: "MUSRIB", plan: "LIFETIME", expiresAt: 9 })
);
await mustDeny("§8.2/٣ مجهول يقرأ اشتراك جهاز (SEC-01: كان مقروءًا للعامة)", getDoc(doc(anonymous, "subscriptions/MSRB-8F42-9D1B")));

// ٤) بائع يحاول قبول صلح ليس له
await mustDeny(
  "§8.2/٤ عضو غرفة ليس طرفًا في الصلح يحاول تعديله",
  updateDoc(doc(bob, DEAL), { status: "ACTIVE" })
);
await mustDeny(
  "§8.2/٤ غير أدمن يحاول الكتابة في مرآة الصلح القديمة",
  setDoc(doc(bob, "deals/legacy-9"), { total_amount: 1 })
);

// ٥) عرض عام فيه هاتف مزارع أو مبلغ دين
await mustDeny(
  "§8.2/٥ إنشاء عرض فيه هاتف مزارع",
  setDoc(doc(alice, "market_listings/l1"), {
    ownerUid: "alice",
    status: "DRAFT",
    farmerPhone: "777123456",
  })
);
await mustDeny(
  "§8.2/٥ إنشاء عرض فيه مبلغ دين",
  setDoc(doc(alice, "market_listings/l2"), {
    ownerUid: "alice",
    status: "DRAFT",
    debtAmountMinor: 500000,
  })
);
await mustDeny(
  "§8.2/٥ صاحب العرض ينشر بنفسه بلا مصادقة (status=PUBLISHED)",
  setDoc(doc(alice, "market_listings/l3"), {
    ownerUid: "alice",
    status: "PUBLISHED",
    hasPublicProjection: true,
  })
);
await mustDeny(
  "§8.2/٥ صاحب العرض يعدّل حالته إلى منشور بعد إنشائه",
  updateDoc(doc(alice, "market_listings/listing-draft-1"), { status: "PUBLISHED" })
);
await mustDeny(
  "§8.2/٥ إضافة هاتف إلى عرض منشور",
  updateDoc(doc(alice, LISTING), { farmerPhone: "777123456" })
);
await mustDeny(
  "§8.2/٥ مجهول يقرأ عرضًا غير منشور",
  getDoc(doc(anonymous, "market_listings/draft-hidden"))
);

// ٦) نصّ فيه وسوم: يُخزَّن نصًّا ولا يُنفَّذ (يُعرض بأمان في اللوحة — tools/admin_console_test.mjs)
await mustAllow(
  "§8.2/٦ تخزين نصّ فيه وسم يُقبل كنصّ بيانات عادي",
  setDoc(doc(admin, "admin_audit/ev-xss"), {
    action: "USER_VERIFY",
    detail: "<img src=x onerror=alert(1)>",
  })
);

console.log("\n=== شواهد السماح (لئلا تكون القواعد رفضًا للكل) ===");

await mustAllow("أدمن يقرأ ملفّ مستخدم", getDoc(doc(admin, "users/alice")));
await mustAllow("صاحب الملفّ يقرأ ملفّه", getDoc(doc(alice, "users/alice")));
await mustAllow(
  "مستخدم يحدّث اسمه الظاهر فقط",
  setDoc(doc(alice, "users/alice"), { uid: "alice", isVerified: false, accountStatus: "ACTIVE", displayName: "أبو محمد" })
);
await mustAllow(
  "مستخدم يحدّث اسمه الظاهر في حساب قائم",
  updateDoc(doc(alice, "users/alice"), { displayName: "أبو محمد الحرضي" })
);
await mustAllow("عضو يقرأ غرفته", getDoc(doc(bob, ROOM)));
await mustAllow("عضو ينشئ قيدًا باسمه", setDoc(doc(alice, `${ROOM}/entries/entry-2`), {
  roomId: "room-water-1",
  createdByMemberId: "alice",
  amountMinor: 200000,
  currency: "YER_NEW",
  type: "GOODS_DEBT",
  owedByMemberId: "bob",
  owedToMemberId: "alice",
  operationId: "op-3",
  status: "SENT",
}));
await mustAllow(
  "عضو يحدّث حقول الحالة غير المالية في قيده",
  updateDoc(doc(alice, ENTRY), { status: "ACKNOWLEDGED" })
);
await mustAllow(
  "عضو يكتب إقراره هو",
  setDoc(doc(bob, `${ROOM}/entries/entry-1/acks/bob`), { decision: "ACKNOWLEDGED" })
);
await mustDeny(
  "عضو يحاول كتابة إقرار باسم عضو آخر",
  setDoc(doc(bob, `${ROOM}/entries/entry-1/acks/alice`), { decision: "ACKNOWLEDGED" })
);
await mustAllow(
  "طرف الصلح يعدّل حالته",
  updateDoc(doc(alice, DEAL), { status: "ACTIVE" })
);
await mustAllow("أدمن ينشئ وثيقة استحقاق", setDoc(doc(admin, "licenses/bob"), { role: "FARMER", plan: "YEARLY", expiresAt: 2 }));
await mustAllow("صاحب الاستحقاق يقرأ استحقاقه", getDoc(doc(bob, "licenses/bob")));
await mustDeny("غيره لا يقرأ استحقاقه", getDoc(doc(alice, "licenses/bob")));
await mustAllow("أدمن يحدّث الاشتراكات", setDoc(doc(admin, "subscriptions/MSRB-8F42-9D1B"), { uid: "alice", status: "SUSPENDED" }));
await mustAllow("صاحب الاشتراك يقرأ اشتراكه", getDoc(doc(alice, "subscriptions/MSRB-8F42-9D1B")));
await mustAllow("مجهول يقرأ وثيقة حدود الإطلاق وحدها", getDoc(doc(anonymous, "system_config/limits")));
await mustDeny("مجهول لا يقرأ بقية إعدادات النظام", getDoc(doc(anonymous, "system_config/secrets")));
await mustDeny("غير أدمن لا يكتب الإعدادات", setDoc(doc(alice, "system_config/limits"), { musribLimit: 1 }));
await mustAllow("مجهول يقرأ عرضًا منشورًا منزوع الرقم", getDoc(doc(anonymous, LISTING)));
await mustAllow("صاحب العرض ينشئ مسودة نظيفة", setDoc(doc(alice, "market_listings/draft-1"), {
  ownerUid: "alice",
  status: "DRAFT",
  crop: "بن",
}));
await mustAllow("صاحب العرض يعدّل مسودته", updateDoc(doc(alice, "market_listings/draft-1"), { crop: "بن يمني" }));
await mustAllow("صاحب الطلب ينشئ طلب تسويق", setDoc(doc(alice, "market_listings/draft-1/requests/r1"), {
  requesterUid: "alice",
  targetUid: "bob",
  listingId: "draft-1",
  status: "PENDING",
}));
await mustAllow("الدلال المخوَّل يقرأ الطلب", getDoc(doc(bob, "market_listings/draft-1/requests/r1")));
await mustDeny("غيرهما لا يقرأ الطلب", getDoc(doc(carol, "market_listings/draft-1/requests/r1")));
await mustAllow("مستخدم مسجَّل يبلّغ", setDoc(doc(alice, "reports/rep-1"), { reporterUid: "alice", reason: "إعلان مخالف" }));
await mustDeny("مجهول لا يبلّغ", setDoc(doc(anonymous, "reports/rep-2"), { reporterUid: "anon", reason: "x" }));
await mustDeny("المبلّغ لا يقرأ سجلّ البلاغات", getDoc(doc(alice, "reports/rep-1")));
await mustAllow("أدمن يضيف أثرًا في سجل التدقيق", setDoc(doc(admin, "admin_audit/ev-1"), { action: "SUBSCRIPTION_SUSPEND" }));
await mustDeny("أدمن لا يعدّل أثرًا في السجل", updateDoc(doc(admin, "admin_audit/ev-1"), { action: "OTHER" }));
await mustDeny("أدمن لا يحذف أثرًا في السجل", deleteDoc(doc(admin, "admin_audit/ev-1")));
await mustDeny("غير أدمن لا يقرأ سجل التدقيق", getDoc(doc(alice, "admin_audit/ev-1")));
await mustDeny("المرايا القديمة مغلقة للمستخدم", setDoc(doc(alice, "musrib_links/LINK1"), { musribName: "x" }));
await mustAllow("أدمن يقرأ المرايا القديمة", getDoc(doc(admin, "musrib_links/LINK1")));
await mustDeny("حذف غرفة ممنوع من طرف عضو", deleteDoc(doc(alice, ROOM)));
await mustDeny("مجهول لا يكتب في المجموعات", setDoc(doc(anonymous, "users/anon"), { uid: "anon" }));

await testEnv.cleanup();

// ------------------------------------------------------------------ الحصيلة

const denied = results.filter((r) => r.outcome === "denied").length;
const allowed = results.filter((r) => r.outcome === "allowed").length;

const requiredCases = results.filter((r) => r.label.startsWith("§8.2/"));
const missingCore = ["§8.2/١", "§8.2/٢", "§8.2/٣", "§8.2/٤", "§8.2/٥", "§8.2/٦"].filter(
  (prefix) => !requiredCases.some((r) => r.label.startsWith(prefix))
);
if (missingCore.length > 0) {
  console.error(`\n⛔ حالات §8.2 ناقصة من المصفوفة: ${missingCore.join(", ")}`);
  process.exit(1);
}
if (allowed === 0) {
  console.error("\n⛔ لا شاهد سماح واحد: القواعد رفضٌ للكل لا نظام صلاحيات.");
  process.exit(1);
}

console.log(
  `\n✅ مصفوفة القواعد كاملة: ${results.length} حالة (${denied} رفضًا، ${allowed} سماحًا)، ` +
    `وكل حالات §8.2 موجودة.`
);
