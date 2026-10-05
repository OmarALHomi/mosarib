#!/usr/bin/env node
/**
 * خادم «بيننا» المرجعي — ينفّذ عقد `v1` نفسه الذي ينفّذه مسار Firebase (ح٢٢).
 *
 * **لِمَ خادم في المستودع؟** لأن الخطة تشترط: «نسخة قديمة تعمل بلا تعديل». ولا تُقاس هذه العبارة
 * بالكلام: تُقاس بأن يكون العقد مكتوبًا، والخادم ينفّذه، والعميل القديم يفهم ردّه حرفيًّا. هذا
 * الملفّ هو **مرجع العقد** الذي يُقاس عليه أي تنفيذ آخر (Firestore اليوم، أو خادم المالك غدًا).
 *
 * **العقد** (نفس مسارات عائلة `v1` في قناة التحديث):
 *
 *   POST /api/v1/changes
 *     الرأس:  Authorization: Bearer <token>
 *     الجسم:  { "deviceId": "...", "items": [ { "operationId", "entityType", "entityId",
 *               "action", "payload", "createdAt" } ] }
 *     الردّ:  { "outcomes": [ { "operationId", "status": "ACCEPTED"|"REJECTED",
 *               "retryable": bool, "reason": "..." } ] }
 *
 *   GET /api/v1/changes?cursor=<opaque>&limit=50
 *     الرأس:  Authorization: Bearer <token>
 *     الردّ:  { "changes": [ { "kind", "entityType", "entityId", "operationId",
 *               "payload", "serverTime" } ], "nextCursor": "...", "hasMore": bool }
 *
 * **ثوابت العقد** (وهي ما يحمي بيانات الناس، وكل ثابت معه سبب من عطل حقيقي):
 *  1. **ترتيب الردّ بترتيب الطلب**: العميل يعتمد على المقابلة بالمكان (`zip`)، فلا يُعاد ترتيب شيء.
 *  2. **`operationId` مفتاح منع التكرار**: إعادة إرسال العنصر نفسه لا تُنشئ تغييرًا ثانيًا، ويُقال
 *     `ACCEPTED` بصدق (قُبل سابقًا ولا أثر جديد) — لا `REJECTED` كاذبة تُقلق صاحب الدفتر.
 *  3. **حالة الخادم تقرّر الزمن**: `serverTime` من الخادم لا من الجهاز **وبالمللي ثانية كعدد**
 *     (نفس وحدة الزمن في التطبيق: `Long`)، فلا يعبث تغيير ساعة الهاتف بترتيب التغييرات ولا يُفسَّر
 *     الزمن بصيغتين مختلفتين.
 *  4. **المؤشر غير شفّاف للعميل**: نصّ يُعاد كما هو؛ لا يفترض العميل أنه رقم ولا تاريخ.
 *  5. **`hasMore` صريح**: العميل لا يستنتج نهاية الصفحة من قلّة العناصر، بل من العلم.
 *  6. **الرفض الدائم مختلف عن المؤقّت**: `retryable=false` يعني «لا تُعِد» (صلاحية أو صيغة أو نسخة
 *     غير مدعومة)، وهذا ما يجعل العميل يضع العنصر في «يحتاج تدخلًا» بدل إعادةٍ أبدية.
 *
 * **ما لا يفعله هذا الخادم (وهو مُعلن، لا منسي):** لا مصادقة حقيقية (رمز جهاز في إعداد)، ولا
 * تفويض على مستوى الغرفة، ولا دمج تعارضات (الدمج عند العميل بقواعد الروoom). وضعه في المستودع
 * **مرجعُ عقدٍ واختبار**، لا خادم إنتاج. وأي نشر حقيقي عليه التزام التصلب مع ح١٦/ح٢٣.
 */
import crypto from "node:crypto";
import fs from "node:fs";
import http from "node:http";
import https from "node:https";

export const API_PREFIX = "/api/v1";
export const MAX_ITEMS_PER_PUSH = 200;
export const MAX_LIMIT = 200;
export const DEFAULT_LIMIT = 50;

/** مبالغ نصّية بالوحدة الصغرى (ADR-04): لا يُقبل في الحمولة رقم عشري لمبلغ. */
const MONEY_FIELDS = ["amountMinor", "totalAmountMinor", "installmentMinor", "paidMinor"];

export class SyncServerStore {
  /** حالة الخادم: التغييرات بترتيب الاستلام، ومفاتيح منع التكرار. */
  constructor(options = {}) {
    this.changes = [];
    this.seenOperations = new Map(); // operationId -> index in changes
    this.stateFile = options.stateFile ?? null;
    if (this.stateFile && fs.existsSync(this.stateFile)) {
      const saved = JSON.parse(fs.readFileSync(this.stateFile, "utf8"));
      this.changes = saved.changes ?? [];
      this.changes.forEach((change, index) => this.seenOperations.set(change.operationId, index));
    }
  }

  persist() {
    if (!this.stateFile) return;
    fs.writeFileSync(this.stateFile, JSON.stringify({ changes: this.changes }), "utf8");
  }

  /**
   * تطبيق دفعة صادرة. الردّ يحمل نتيجة لكل عنصر **بنفس الترتيب**، والمكرر يُعاد له `ACCEPTED`
   * بلا تغيير جديد (منع التكرار لا عقوبة على العميل).
   */
  push(items, nowMillis) {
    const outcomes = [];
    for (const item of items) {
      const operationId = String(item?.operationId ?? "");
      const entityType = String(item?.entityType ?? "");
      const entityId = String(item?.entityId ?? "");
      const payload = String(item?.payload ?? "");

      if (!operationId || !entityType || !entityId) {
        outcomes.push({ operationId, status: "REJECTED", retryable: false, reason: "عنصر ناقص: يلزم operationId ونوع الكيان ومعرّفه" });
        continue;
      }
      if (this.seenOperations.has(operationId)) {
        outcomes.push({ operationId, status: "ACCEPTED", retryable: false, reason: "مقبول سابقًا: إعادة الإرسال لا تُنشئ أثرًا ثانيًا" });
        continue;
      }
      const payloadProblem = checkPayload(payload);
      if (payloadProblem) {
        outcomes.push({ operationId, status: "REJECTED", retryable: false, reason: payloadProblem });
        continue;
      }

      const serverTime = nowMillis();
      const change = {
        kind: String(item.action ?? "").toUpperCase() === "VOID" ? "VOID" : "UPSERT",
        entityType,
        entityId,
        operationId,
        payload,
        serverTime,
      };
      this.changes.push(change);
      this.seenOperations.set(operationId, this.changes.length - 1);
      outcomes.push({ operationId, status: "ACCEPTED", retryable: false, reason: "" });
    }
    this.persist();
    return outcomes;
  }

  /** صفحة سحب بعد المؤشر. المؤشر نصّ لا يفترضه العميل رقمًا. */
  pull(cursor, limit) {
    const start = cursor ? this.indexOfCursor(cursor) + 1 : 0;
    const safeLimit = Math.min(Math.max(Number(limit) || DEFAULT_LIMIT, 1), MAX_LIMIT);
    const slice = this.changes.slice(start, start + safeLimit);
    const hasMore = start + slice.length < this.changes.length;
    return {
      changes: slice,
      nextCursor: slice.length > 0 ? encodeCursor(start + slice.length) : cursor ?? encodeCursor(0),
      hasMore,
    };
  }

  indexOfCursor(cursor) {
    const decoded = decodeCursor(cursor);
    return Number.isInteger(decoded) ? decoded - 1 : -1; // -1 ⇒ ابدأ من الصفر عند مؤشر غير معروف
  }
}

/**
 * فحص الحمولة: مبلغ يُرسل رقمًا = رفض. لا نتسامح مع `1.5` ريال ولا مع `1500000` رقمًا، لأن قاعدة
 * المال في المشروع (ADR-04) تقول: المبلغ نصّي بالوحدة الصغرى، فلا كسور عائمة على السلك أبدًا.
 */
export function checkPayload(payload) {
  if (!payload.trim()) return "حمولة فارغة";
  let parsed;
  try {
    parsed = JSON.parse(payload);
  } catch {
    return "الحمولة ليست JSON صحيحًا";
  }
  if (parsed !== null && typeof parsed === "object") {
    for (const field of MONEY_FIELDS) {
      const value = parsed[field];
      if (value === undefined) continue;
      if (typeof value === "number") {
        return `المبلغ «${field}» يجب أن يكون نصًّا بالوحدة الصغرى (ADR-04)، لا رقمًا`;
      }
      if (typeof value === "string" && !/^-?[0-9]+$/.test(value)) {
        return `المبلغ «${field}» ليس عددًا صحيحًا بالوحدة الصغرى`;
      }
    }
  }
  return null;
}

/** مؤشر مبطّن (opaque): العميل لا يفترض شكله، والخادم حرّ في تغييره. */
export function encodeCursor(index) {
  return `c:${Buffer.from(String(index), "utf8").toString("base64url")}`;
}

export function decodeCursor(cursor) {
  const text = String(cursor ?? "");
  if (!text.startsWith("c:")) return null;
  const value = Number(Buffer.from(text.slice(2), "base64url").toString("utf8"));
  return Number.isInteger(value) && value >= 0 ? value : null;
}

/** نتيجة طلب واحدة: حالة HTTP + جسم. مفصولة عن الشبكة حتى تُختبر بلا منفذ. */
export function handleRequest(store, request, options = {}) {
  const nowMillis = options.nowMillis ?? (() => Date.now());
  const expectedToken = options.token ?? null;

  const url = new URL(request.url, "http://localhost");
  if (!url.pathname.startsWith(API_PREFIX)) {
    return { status: 404, body: { error: "مسار غير معروف" } };
  }
  if (expectedToken && request.headers?.authorization !== `Bearer ${expectedToken}`) {
    return { status: 401, body: { error: "رمز الجهاز غير صالح" } };
  }

  if (request.method === "POST" && url.pathname === `${API_PREFIX}/changes`) {
    const items = Array.isArray(request.body?.items) ? request.body.items : null;
    if (!items) return { status: 400, body: { error: "الطلب بلا items" } };
    if (items.length > MAX_ITEMS_PER_PUSH) {
      // دفعة كبيرة تُرفض بمؤقّت: على العميل أن يقسّمها لا أن يفقدها.
      return { status: 413, body: { error: `أكثر من ${MAX_ITEMS_PER_PUSH} عنصر في دفعة واحدة — قسّمها`, retryable: true } };
    }
    return { status: 200, body: { outcomes: store.push(items, nowMillis) } };
  }

  if (request.method === "GET" && url.pathname === `${API_PREFIX}/changes`) {
    return {
      status: 200,
      body: store.pull(url.searchParams.get("cursor"), url.searchParams.get("limit")),
    };
  }

  return { status: 405, body: { error: "طريقة غير مسموحة على هذا المسار" } };
}

/**
 * الخادم على المنفذ: جسم JSON بسقف حجم، ولا شيء غير ذلك.
 *
 * و`tls: {cert, key}` يُنشئ خادم https حقيقيًّا — وليس تجميلًا: التطبيق يرفض النصّ المكشوف أصلًا
 * (`usesCleartextTraffic="false"`)، فبلا TLS لا يمكن اختبار المسار الكامل على جهاز.
 */
export function createServer(options = {}) {
  const store = options.store ?? new SyncServerStore(options);
  const handler = (req, res) => {
    let raw = "";
    req.on("data", (chunk) => {
      raw += chunk;
      if (raw.length > 1024 * 1024) req.destroy();
    });
    req.on("end", () => {
      let body = null;
      if (raw.trim()) {
        try {
          body = JSON.parse(raw);
        } catch {
          res.writeHead(400, { "content-type": "application/json; charset=utf-8" });
          res.end(JSON.stringify({ error: "جسم الطلب ليس JSON" }));
          return;
        }
      }
      const result = handleRequest(store, { method: req.method, url: req.url, headers: req.headers, body }, options);
      res.writeHead(result.status, { "content-type": "application/json; charset=utf-8" });
      res.end(JSON.stringify(result.body));
    });
  };
  const server = options.tls
    ? https.createServer(options.tls, handler)
    : http.createServer(handler);
  server.syncStore = store;
  return server;
}

// ------------------------------------------------------------------ التشغيل المباشر
if (import.meta.url === `file://${process.argv[1]}`) {
  const port = Number(process.env.PORT ?? 8787);
  const token = process.env.SYNC_TOKEN ?? null;
  const stateFile = process.env.STATE_FILE ?? null;
  const tlsCert = process.env.TLS_CERT ?? null;
  const tlsKey = process.env.TLS_KEY ?? null;
  const tls = tlsCert && tlsKey ? { cert: fs.readFileSync(tlsCert), key: fs.readFileSync(tlsKey) } : null;
  const server = createServer({ token, stateFile, tls });
  server.listen(port, "0.0.0.0", () => {
    const scheme = tls ? "https" : "http";
    console.log(`خادم بيننا المرجعي يعمل على ${scheme}://0.0.0.0:${port}${token ? " (برمز جهاز)" : " (بلا رمز — للتجربة المحلية فقط)"}`);
    if (!token) console.log("تنبيه: بلا SYNC_TOKEN لا مصادقة إطلاقًا. لا تنشره على شبكة عامة بهذه الحال.");
    if (scheme === "http") {
      console.log("تنبيه: التطبيق يرفض النصّ المكشوف (usesCleartextTraffic=false). للتجربة على جهاز");
      console.log("        حقيقي ضع شهادة TLS (TLS_CERT/TLS_KEY) أو نفقًا يشغّل https.");
    }
  });
  // حالة صغيرة موثّقة للأثر: عدد التغييرات المحفوظة.
  setInterval(() => {
    console.log(`التغييرات المحفوظة: ${server.syncStore.changes.length}`);
  }, 60_000).unref();
}

/** اسم عشوائي لرمز جهاز (يُستعمل في الاختبار والتوليد الأولي). */
export function newToken() {
  return crypto.randomBytes(24).toString("base64url");
}
