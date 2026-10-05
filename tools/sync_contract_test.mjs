#!/usr/bin/env node
/**
 * اختبار العقد بين **الخادم المرجعي** (ح٢٢) و**فكّ الترميز في التطبيق**.
 *
 * لِمَ هذا الاختبار؟ لأن عبارة «نسخة قديمة تعمل بلا تعديل» لا تُقاس بالكلام: تُقاس بأن يُشغَّل
 * الخادم الحقيقي، ويُبنى الطلب بالعقد، ويُقارَن ردّه حرفيًّا بما يفكّه التطبيق. فلو تغيّر الخادم
 * (شكل حقل، أو نوع زمن، أو ترتيب الردّ) يسقط الفحص قبل أن يسقط في يد مستخدم.
 *
 * وثلاثة أمور تُفعل هنا:
 *   ١. الخادم المرجعي يُشغَّل **داخل العملية** (بلا شبكة) على مخزن نصّي، وتُستدعى مسارات العقد.
 *   ٢. الردود الحقيقية تُثبَّت في «متجهات ذهبية» يقرؤها اختبار Kotlin (تُولَّد بأمر `--write`).
 *   ٣. تُفحص ثوابت العقد نصًّا: منع التكرار، رفض المبلغ الرقمي، فصاحة الخطأ، ترتيب الردّ،
 *      عدم كشف الغرف لغير المعنيّ (لا يقرأ إلا ما بعد مؤشره)، والمؤشر غير الشفّاف.
 */
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import {
  API_PREFIX,
  RELEASE_PATH,
  SyncServerStore,
  checkPayload,
  decodeCursor,
  encodeCursor,
  handleRequest,
} from "../server/baynana-sync-server.mjs";

const here = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(here, "..");
const vectorsPath = path.join(repoRoot, "app/src/test/java/com/baynana/domain/sync/SyncWireVectors.kt");

const FIXED_NOW = 1_767_225_600_000; // زمن ثابت: متجهات قابلة لإعادة التوليد
const TOKEN = "dev-token-for-contract-test";
const nowMillis = () => FIXED_NOW;

function post(store, items) {
  return handleRequest(
    store,
    { method: "POST", url: `${API_PREFIX}/changes`, headers: { authorization: `Bearer ${TOKEN}` }, body: { deviceId: "device-a", items } },
    { token: TOKEN, nowMillis }
  );
}

function get(store, cursor, limit = 50) {
  const query = new URLSearchParams();
  if (cursor) query.set("cursor", cursor);
  if (limit) query.set("limit", String(limit));
  return handleRequest(
    store,
    { method: "GET", url: `${API_PREFIX}/changes?${query}`, headers: { authorization: `Bearer ${TOKEN}` } },
    { token: TOKEN, nowMillis }
  );
}

// ------------------------------------------------------------------ ١) دفعة فيها مكرر ومرفوض
const store = new SyncServerStore();

const entryPayload = JSON.stringify({
  id: "entry-1",
  roomId: "room-water-1",
  operationId: "op-entry-1",
  type: "WATER_SESSION",
  owedByMemberId: "counterpart-room-water-1",
  owedToMemberId: "me",
  amountMinor: "1500000",
  currency: "YER_NEW",
  occurredAt: FIXED_NOW - 4000,
});

const pushItems = [
  { operationId: "op-entry-1", entityType: "entry", entityId: "entry-1", action: "UPSERT", payload: entryPayload, createdAt: FIXED_NOW - 4000 },
  { operationId: "op-entry-1", entityType: "entry", entityId: "entry-1", action: "UPSERT", payload: entryPayload, createdAt: FIXED_NOW - 4000 },
  { operationId: "op-bad-money", entityType: "entry", entityId: "entry-9", action: "UPSERT", payload: JSON.stringify({ id: "entry-9", amountMinor: 1500000 }), createdAt: FIXED_NOW },
];

const pushResponse = post(store, pushItems);
assert.equal(pushResponse.status, 200);
const outcomes = pushResponse.body.outcomes;
assert.equal(outcomes.length, pushItems.length, "نتيجة لكل عنصر بنفس ترتيب الطلب");
assert.equal(outcomes[0].status, "ACCEPTED");
assert.equal(outcomes[1].status, "ACCEPTED", "إعادة الإرسال مقبولة بلا أثر ثانٍ");
assert.equal(store.changes.length, 1, "المكرر لم يُنشئ تغييرًا ثانيًا");
assert.equal(outcomes[2].status, "REJECTED");
assert.equal(outcomes[2].retryable, false, "رفض المبلغ الرقمي دائم لا مؤقّت");
assert.ok(outcomes[2].reason.includes("الوحدة الصغرى"), "سبب الرفض يذكر القاعدة بلغة مفهومة");
console.log("  ✅ منع التكرار: إعادة الإرسال لا تُنشئ أثرًا ثانيًا، والمبلغ الرقمي مرفوض بسببه");
console.log("  ✅ ترتيب النتائج مطابق لترتيب الطلب");

// ------------------------------------------------------------------ ٢) السحب والمؤشر
const firstPage = get(store, null, 1);
assert.equal(firstPage.status, 200);
assert.equal(firstPage.body.changes.length, 1);
assert.equal(firstPage.body.hasMore, false);

const page2 = post(store, [
  { operationId: "op-entry-2", entityType: "entry", entityId: "entry-2", action: "UPSERT", payload: JSON.stringify({ id: "entry-2", amountMinor: "250000" }), createdAt: FIXED_NOW },
  { operationId: "op-entry-3", entityType: "entry", entityId: "entry-3", action: "UPSERT", payload: JSON.stringify({ id: "entry-3", amountMinor: "300000" }), createdAt: FIXED_NOW },
]);
assert.equal(page2.body.outcomes.filter((outcome) => outcome.status === "ACCEPTED").length, 2);

const limitPage = get(store, null, 2);
assert.equal(limitPage.body.changes.length, 2);
assert.equal(limitPage.body.hasMore, true, "hasMore صريح لأن هناك المزيد");
const nextPage = get(store, limitPage.body.nextCursor, 2);
assert.ok(nextPage.body.changes.length >= 1);
assert.notEqual(nextPage.body.changes[0].operationId, limitPage.body.changes[0].operationId, "المؤشر يمنع تكرار ما قُرئ");
assert.ok(decodeCursor(limitPage.body.nextCursor) !== null, "المؤشر مقروء عند الخادم");
assert.ok(limitPage.body.nextCursor.startsWith("c:"), "شكل المؤشر غير شفّاف للعميل");
console.log("  ✅ المؤشر غير شفّاف ويعمل، وhasMore صريح، بلا تكرار عناصر");

// ------------------------------------------------------------------ ٣) الأمن الأساسي
const unauthorized = handleRequest(
  store,
  { method: "GET", url: `${API_PREFIX}/changes`, headers: { authorization: "Bearer wrong" } },
  { token: TOKEN, nowMillis }
);
assert.equal(unauthorized.status, 401);
assert.ok(unauthorized.body.error.includes("رمز"), "رسالة عربية لا stack trace");
const unknownPath = handleRequest(store, { method: "GET", url: "/admin/rooms", headers: { authorization: `Bearer ${TOKEN}` } }, { token: TOKEN, nowMillis });
assert.equal(unknownPath.status, 404);
console.log("  ✅ بلا رمز صالح: 401، ومسار غير معروف: 404 — ولا شيء غيرهما");

// ---------------------------------------------------------- ٣ب) قناة الإصدار على الخادم نفسه
// الخادم الخاص ينفّذ العقدين: المزامنة والتحميل. وهنا يُثبَّت أمران يخطئ فيهما من ينشر الخادم:
//   ١) مسار الإصدار **عامّ**: يعمل بلا رمز مزامنة (جهاز جديد لم يُضبط له رمز بعد).
//   ٢) النصّ يُخدَم **كما هو**: التوقيع يُتحقَّق من البايتات، فأي إعادة ترميز JSON تفسده.
const releaseWithoutFile = handleRequest(
  new SyncServerStore(),
  { method: "GET", url: RELEASE_PATH, headers: {} },
  { token: TOKEN, nowMillis }
);
assert.equal(releaseWithoutFile.status, 404, "بلا ملفّ إصدار: 404 لا نجاح كاذب");
assert.ok(releaseWithoutFile.body.error.includes("ملفّ إصدار"), "ورسالة عربية مفهومة");

const releaseSignature = "eyJwYXlsb2FkIjoiMS4yIn0.c2lnbmF0dXJl";
const releaseWithFile = handleRequest(
  new SyncServerStore(),
  { method: "GET", url: RELEASE_PATH, headers: {} },
  { token: TOKEN, nowMillis, releaseText: releaseSignature }
);
assert.equal(releaseWithFile.status, 200, "الملفّ يُخدَم على المسار الثابت");
assert.equal(releaseWithFile.raw, releaseSignature, "النصّ كما هو بلا إعادة ترميز");
assert.equal(releaseWithFile.body, undefined, "لا يُعاد ترميز الملفّ كـJSON");
console.log("  ✅ قناة الإصدار على الخادم الخاص: عامّة بلا رمز، ونصّها يُخدَم كما هو (أو 404 عربية)");

// ------------------------------------------------------------------ ٤) فحوص الحمولة مباشرة
assert.ok(checkPayload(JSON.stringify({ amountMinor: "1500000" })) === null);
assert.ok(checkPayload(JSON.stringify({ amountMinor: 1.5 })) !== null, "الكسر العشري مرفوض");
assert.ok(checkPayload(JSON.stringify({ amountMinor: "1.5" })) !== null, "النصّ غير الصحيح مرفوض");
assert.ok(checkPayload("{ ليس JSON") !== null);
console.log("  ✅ قاعدة المال على السلك: نصّ صحيح بالوحدة الصغرى، ولا كسور عشرية");

// ------------------------------------------------------------------ ٥) المتجهات الذهبية لـKotlin
const kotlin = `package com.baynana.domain.sync

/**
 * متجهات ذهبية لعقد المزامنة \`v1\` (ح٢٢) — **مولَّدة آليًّا من الخادم المرجعي، لا تُحرَّر بيد**.
 *
 * المصدر: \`node tools/sync_contract_test.mjs --write\`، والتحقق منها في CI بالأمر نفسه بلا
 * \`--write\`. فالمتجهات ليست وصفًا للعقد بل **ردّ الخادم الحقيقي**، واختبار Kotlin يفكّها بالدوال
 * التي يستعملها التطبيق في الإنتاج.
 */
object SyncWireVectors {

    /** الطلب كما يبنيه التطبيق (نفس الحقول ونفس الترتيب). */
    const val PUSH_REQUEST = ${JSON.stringify(
      JSON.stringify({
        deviceId: "device-a",
        items: pushItems.slice(0, 1).map((item) => ({
          operationId: item.operationId,
          entityType: item.entityType,
          entityId: item.entityId,
          action: item.action,
          payload: item.payload,
          createdAt: item.createdAt,
        })),
      })
    )}

    /** ردّ الخادم على دفعة فيها: مقبول، ثم نفس العنصر (مكرر)، ثم عنصر بمبلغ رقمي. */
    const val PUSH_RESPONSE = ${JSON.stringify(JSON.stringify(pushResponse.body))}

    /** صفحة سحب محدودة (٢ من ٣): تُثبت أن "hasMore" صريح وأن المؤشر يعمل. */
    const val PULL_PAGE_LIMITED = ${JSON.stringify(JSON.stringify(limitPage.body))}

    /** صفحة سحب بعد المؤشر: ما تبقّى. */
    const val PULL_PAGE_AFTER_CURSOR = ${JSON.stringify(JSON.stringify(nextPage.body))}

    /** صفحة فارغة: الخادم لا يعرف المؤشر (مؤشر قديم أو من خادم آخر). */
    const val PULL_EMPTY = ${JSON.stringify(JSON.stringify({ changes: [], nextCursor: encodeCursor(0), hasMore: false }))}

    /** ردّ مشوّه: التطبيق لا يُعلن نجاحًا ولا يُفسد شيئًا — كل العناصر تُعاد لاحقًا. */
    const val PUSH_RESPONSE_GARBAGE = "<html>502 Bad Gateway</html>"

    /** ردّ ناقص النتائج: عنصر واحد بينما الطلب عنصران ⇒ الثاني لا يُعلن نجاحه. */
    const val PUSH_RESPONSE_SHORT = ${JSON.stringify(
      JSON.stringify({ outcomes: [{ operationId: "op-entry-1", status: "ACCEPTED", retryable: false, reason: "" }] })
    )}
}
`;

const mode = process.argv[2] ?? "--check";
if (mode === "--write") {
  fs.mkdirSync(path.dirname(vectorsPath), { recursive: true });
  fs.writeFileSync(vectorsPath, kotlin, "utf8");
  console.log(`كُتب: ${path.relative(repoRoot, vectorsPath)}`);
} else {
  const current = fs.existsSync(vectorsPath) ? fs.readFileSync(vectorsPath, "utf8") : "";
  if (current !== kotlin) {
    // المقارنة على **المحتوى المولَّد** لا على التواقيع (لا عشوائية هنا: الخادم حتمي بزمن ثابت).
    console.error("⛔ متجهات عقد المزامنة غير مطابقة لمخرَج الخادم المرجعي.");
    console.error("   أعِد التوليد: node tools/sync_contract_test.mjs --write");
    process.exit(1);
  }
  console.log("OK: متجهات عقد المزامنة هي ردّ الخادم المرجعي نفسه، بلا تغيير.");
}

console.log("\n✅ عقد المزامنة v1: الخادم المرجعي والتطبيق يتكلّمان اللغة نفسها.");
