#!/usr/bin/env node
/**
 * منح ادعاء الأدمن (ح١٥) — مرة واحدة عند إعداد المشروع.
 *
 * القاعدة الحاكمة: صلاحية الأدمن **ادعاء (claim) يصدره الخادم**، لا بريد مكتوب في القواعد ولا
 * حقل في وثيقة المستخدم. وقواعد Firestore تقرأ `token.admin == true` فقط. وهذا الملفّ هو الطريق
 * الوحيد إلى ذلك الادعاء، ويعمل بمفتاح خدمة يحفظه المالك خارج المستودع — ولا يُطبع ولا يُسجَّل.
 *
 * التشغيل:
 *   export GOOGLE_APPLICATION_CREDENTIALS=/path/outside/repo/service-account.json
 *   node tools/set_admin_claim.mjs admin@example.com
 *   node tools/set_admin_claim.mjs <uid> --revoke
 *
 * ملاحظات صدق:
 * - المفتاح لازم لأن هذا هو الشيء الوحيد الذي يستطيع كتابة claims. ولا يوجد طريق من المتصفح إلى
 *   هذا الادعاء: لو وُجد، لكان كل من فتح اللوحة أدمن.
 * - بعد المنح يجب أن يُحدَّث رمز المستخدم (زرّ «تحديث الصلاحية» في اللوحة) لأن الادعاء يُقرأ من
 *   الرمز الموقّع، لا من القاعدة.
 */
import path from "node:path";
import { fileURLToPath } from "node:url";

const here = path.dirname(fileURLToPath(import.meta.url));

const args = process.argv.slice(2);
const target = args.find((arg) => !arg.startsWith("--"));
const revoke = args.includes("--revoke");

if (!target) {
  console.error("الاستعمال: node tools/set_admin_claim.mjs <uid|email> [--revoke]");
  process.exit(2);
}

if (!process.env.GOOGLE_APPLICATION_CREDENTIALS && !process.env.FIREBASE_SERVICE_ACCOUNT) {
  console.error(
    [
      "لا مفتاح خدمة في البيئة.",
      "صدّر مسار مفتاح الخدمة (خارج المستودع دائمًا):",
      "  export GOOGLE_APPLICATION_CREDENTIALS=/path/outside/repo/service-account.json",
      "سبب الشرط: كتابة الادعاء تحتاج صلاحية خادم، وهي الصلاحية التي لا يجوز أن تكون في التطبيق.",
    ].join("\n")
  );
  process.exit(2);
}

let admin;
try {
  admin = await import("firebase-admin");
} catch (error) {
  console.error(
    [
      "حزمة firebase-admin غير مثبّتة.",
      `  npm install --prefix ${path.relative(process.cwd(), here)}`,
      `(${error.message})`,
    ].join("\n")
  );
  process.exit(2);
}

admin.default.initializeApp({
  credential: admin.default.credential.applicationDefault(),
});

const auth = admin.default.auth();

const user = target.includes("@")
  ? await auth.getUserByEmail(target)
  : await auth.getUser(target);

const claims = revoke ? { admin: false } : { admin: true };
await auth.setCustomUserClaims(user.uid, claims);

console.log(
  [
    `تم ${revoke ? "سحب" : "منح"} ادعاء الأدمن.`,
    `  uid: ${user.uid}`,
    `  البريد: ${user.email ?? "—"}`,
    `  الادعاء الآن: admin=${claims.admin}`,
    "الخطوة التالية: افتح لوحة الأدمن واضغط «تحديث الصلاحية» ليُقرأ الادعاء من رمز جديد.",
    revoke
      ? "تنبيه: من سُحب ادعاؤه تبقى صلاحيته سارية حتى انتهاء رمزه الحالي (ساعة واحدة)."
      : "تنبيه: هذا الادعاء يفتح كل عمليات اللوحة، فلا يُمنح إلا لحساب المالك المؤمَّن بخطوتين.",
  ].join("\n")
);
