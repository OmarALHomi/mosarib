#!/usr/bin/env node
/**
 * حاجز بنية قواعد Firestore (ح١٥) — يفحص شكل الملفّ والمعنى الظاهر، لا المنطق الكامل.
 *
 * مكانته بين الحاجزين: `tools/firestore_rules_test.mjs` يحكم على **السلوك** على المحاكي، وهذا
 * الملفّ يحكم على **البنية** قبل المحاكي وفي كل دقّة (حتى بلا شبكة). وكل فحص هنا وُلد من عيب
 * يمكن أن يعود: قراءة عامة (`if true`)، أو إسقاط قاعدة حذف ممنوعة، أو اختفاء حالة من مصفوفة §8.2.
 *
 * التشغيل: node tools/rules_guard.mjs
 */
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const here = path.dirname(fileURLToPath(import.meta.url));
const rulesFile = path.join(here, "..", "firestore.rules");
const matrixFile = path.join(here, "firestore_rules_test.mjs");

const raw = fs.readFileSync(rulesFile, "utf8");
const matrix = fs.readFileSync(matrixFile, "utf8");

/**
 * نصّ بلا تعليقات: الفحص على الشيفرة لا على الشرح.
 * ولا نُزيل النصوص النصّية، لأن قوائم الحقول المحمية (`'amountMinor'`…) نصوصٌ نحتاج قراءتها —
 * ولا يوجد في هذا الملفّ قوس داخل نصّ، فلا يلتبس على المتتبّع.
 */
function stripComments(text) {
  let out = "";
  let i = 0;
  while (i < text.length) {
    const two = text.slice(i, i + 2);
    if (two === "//") {
      while (i < text.length && text[i] !== "\n") i += 1;
      continue;
    }
    if (two === "/*") {
      i += 2;
      while (i < text.length && text.slice(i, i + 2) !== "*/") i += 1;
      i += 2;
      continue;
    }
    out += text[i];
    i += 1;
  }
  return out;
}

const code = stripComments(raw);

// ---------------------------------------------------------------- ١) توازن الأقواس

function assertBalanced(text, label) {
  const pairs = { ")": "(", "]": "[", "}": "{" };
  const stack = [];
  for (const ch of text) {
    if ("([{".includes(ch)) stack.push(ch);
    else if (")]}".includes(ch)) {
      assert.equal(stack.pop(), pairs[ch], `قوس غير متوازن في ${label} عند «${ch}»`);
    }
  }
  assert.equal(stack.length, 0, `أقواس غير مغلقة في ${label}: ${stack.join("")}`);
}

assertBalanced(code, "firestore.rules");
console.log("OK: أقواس ملفّ القواعد متوازنة");

// ---------------------------------------------------------------- ٢) لا قراءة عامة

assert.ok(
  !/\bif\s+true\s*;/.test(code),
  "وجدتُ قاعدة `if true`: لا قراءة عامة لأي مجموعة تحمل بيانات أشخاص (SEC-01)."
);
assert.ok(
  !/allow\s+read\s*,\s*write\s*:\s*if\s+isSignedIn\(\)\s*;/.test(code),
  "وجدتُ `allow read, write: if isSignedIn()`: صلاحية مطلقة لأي حساب مسجَّل — هذا ليس رفضًا افتراضيًا."
);
console.log("OK: لا قراءة عامة ولا صلاحية مطلقة لحساب مسجَّل");

// ---------------------------------------------------------------- ٣) الكتل المطلوبة ومعانيها

/** نصّ كتلة `match` لمسار معيّن، بتتبّع الأقواس. */
function blockBody(pathPattern) {
  // البحث ببداية النحوية لا بالنصّ المجرّد: كلمة match ثم المسار كما هو مكتوب في الملفّ، فالمسارات
  // المتشابهة (مثل `entries` داخل `musrib_entries`) لا تختلط.
  const escaped = pathPattern.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  const pattern = new RegExp(`match\\s+/${escaped}\\s*\\{`);
  const found = pattern.exec(code);
  if (!found) return null;
  const open = found.index + found[0].length - 1;
  let depth = 0;
  for (let i = open; i < code.length; i += 1) {
    if (code[i] === "{") depth += 1;
    else if (code[i] === "}") {
      depth -= 1;
      if (depth === 0) return code.slice(open + 1, i);
    }
  }
  return null;
}

const requiredBlocks = [
  {
    path: "users/{uid}",
    need: ["isMe(uid)", "isVerified", "accountStatus"],
    why: "حقول الثقة لا يكتبها العميل",
  },
  {
    path: "licenses/{uid}",
    need: ["allow write: if isAdmin();"],
    why: "الاستحقاق لا يكتبه الجهاز أبدًا",
  },
  {
    path: "subscriptions/{deviceCode}",
    need: ["isAdmin()"],
    why: "الاشتراكات لم تعد مقروءة للعامة (SEC-01)",
  },
  {
    path: "rooms/{roomId}",
    need: ["allow delete: if false;", "memberUids"],
    why: "الغرفة للأعضاء، ولا تُحذف",
  },
  {
    path: "entries/{entryId}",
    need: ["allow delete: if false;", "amountMinor", "operationId"],
    why: "المال يُصحَّح بقيد عكسي لا بتعديل ولا حذف",
  },
  {
    path: "acks/{ackUid}",
    need: ["isMe(ackUid)"],
    why: "كل عضو يكتب إقراره هو فقط",
  },
  {
    path: "market/listings/{listingId}",
    need: ["farmerPhone", "PUBLISHED", "DRAFT"],
    why: "لا هاتف مزارع، ولا نشر بلا مصادقة",
  },
  {
    path: "admin_audit/{entryId}",
    need: ["allow update, delete: if false;"],
    why: "ال سجل الإداري لا يُعدَّل ولا يُمحى",
  },
  {
    path: "reports/{reportId}",
    need: ["allow update, delete: if false;"],
    why: "البلاغ يُقرأ ولا يُعدَّل",
  },
];

for (const block of requiredBlocks) {
  const body = blockBody(block.path);
  assert.ok(body !== null, `كتلة القواعد مفقودة: match /${block.path}`);
  for (const snippet of block.need) {
    assert.ok(
      body.includes(snippet),
      `كتلة match /${block.path} فقدت «${snippet}» — والسبب: ${block.why}`
    );
  }
}
console.log(`OK: كل الكتل المطلوبة موجودة بمعانيها (${requiredBlocks.length} كتلة)`);

// ---------------------------------------------------------------- ٤) مصفوفة §8.2 كاملة

const requiredCases = ["§8.2/١", "§8.2/٢", "§8.2/٣", "§8.2/٤", "§8.2/٥", "§8.2/٦"];
for (const label of requiredCases) {
  assert.ok(
    matrix.includes(`"${label}`),
    `حالة من جدول §8.2 غائبة عن مصفوفة الاختبار: ${label}`
  );
}

const denyCount = (matrix.match(/mustDeny\(/g) || []).length;
const allowCount = (matrix.match(/mustAllow\(/g) || []).length;
assert.ok(denyCount >= 15, `مصفوفة الرفض ضعيفة: ${denyCount} حالة فقط`);
assert.ok(allowCount >= 10, `لا شواهد سماح كافية (${allowCount}): القواعد قد تصير رفضًا للكل`);
assert.ok(
  matrix.includes("process.exit(1)"),
  "المصفوفة لا تُفشل البناء عند نقص التغطية"
);
console.log(
  `OK: مصفوفة §8.2 كاملة (${requiredCases.length} حالات إلزامية، ${denyCount} رفضًا، ${allowCount} سماحًا)`
);

console.log("✅ حاجز قواعد Firestore: بنية سليمة، ولا قراءة عامة، والمصفوفة كاملة.");
