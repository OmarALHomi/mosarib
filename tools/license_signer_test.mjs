#!/usr/bin/env node
/**
 * اختبار موقّع التصاريح في لوحة الأدمن (ح١٣) — واختبار التوافق بين المتصفح والتطبيق.
 *
 * لماذا هذا الاختبار موجود؟ لأن أخطر عيب في الترخيص لا يظهر في الاختبارات المحلية: أن يكون
 * الرمز **صحيحًا في المتصفح ومرفوضًا في التطبيق** (أو العكس) بسبب فرق صيغة توقيع أو ترميز حمولة.
 * فنجري هنا **تشغيلين مستقلين** على الرمز نفسه:
 *
 *   ١. الكتلة المأخوذة من اللوحة نفسها (لا نسخة ثانية تتفرّع عنها) توقّع الحمولة.
 *   ٢. مكتبة Node للتحقق (`crypto.verify` بصيغة P1363) تتحقق من التوقيع بمعزل عن كود الصفحة.
 *
 * ثم تُطبع النتيجة كـ«متجه ذهبي» يُلصق في اختبار Kotlin (`BrowserSignedLicenseTest`) ليتحقق
 * التطبيق من رمز وُقّع في المتصفح فعلًا. فالمسار كله مفحوص: لوحة → رمز → فاحص التطبيق.
 */
import assert from "node:assert/strict";
import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";
import vm from "node:vm";
import { fileURLToPath } from "node:url";

const here = path.dirname(fileURLToPath(import.meta.url));
const panelPath = path.join(here, "key_generator.html");
const html = fs.readFileSync(panelPath, "utf8");

// ---------------------------------------------------------------- ١) استخراج كتلة التوقيع من اللوحة

const blockMatch = html.match(
  /\/\/ ===== BEGIN: LICENSE SIGNING[\s\S]*?\/\/ ===== END: LICENSE SIGNING =====/
);
assert.ok(blockMatch, "كتلة LICENSE SIGNING موجودة في tools/key_generator.html");

const sandbox = {
  crypto: crypto.webcrypto,
  TextEncoder,
  TextDecoder,
  atob: (value) => Buffer.from(value, "base64").toString("binary"),
  btoa: (value) => Buffer.from(value, "binary").toString("base64"),
  console,
};
vm.createContext(sandbox);
vm.runInContext(blockMatch[0], sandbox, { filename: "license-signing-block.js" });

const {
  buildLicensePayload,
  signLicense,
  verifyLicenseToken,
  licenseDeviceCode,
  bytesFromBase64,
  base64UrlFromBytes,
} = sandbox;

// ---------------------------------------------------------------- ٢) زوج مفاتيح ومتجه ثابت

const keys = await crypto.webcrypto.subtle.generateKey(
  { name: "ECDSA", namedCurve: "P-256" },
  true,
  ["sign", "verify"]
);
const privateKeyB64 = Buffer.from(
  await crypto.webcrypto.subtle.exportKey("pkcs8", keys.privateKey)
).toString("base64");
const publicKeyB64 = Buffer.from(
  await crypto.webcrypto.subtle.exportKey("spki", keys.publicKey)
).toString("base64");

// قيم ثابتة كي يكون المتجه الذهبي قابلًا للنقل إلى اختبار Kotlin بلا تغيير.
const fields = {
  licenseId: "lic-golden-0001-abcd",
  deviceCode: "MSRB-8F42-9D1B",
  role: "MUSRIB",
  plan: "MONTHLY",
  durationDays: 30,
  issuedAt: 1770000000000,
  expiresAt: 1770000000000 + 30 * 24 * 3600 * 1000,
};

const signed = await signLicense(privateKeyB64, fields);

// ---------------------------------------------------------------- ٣) التحقق المستقل

const payloadBytes = Buffer.from(signed.payload, "utf8");
const signatureBytes = Buffer.from(signed.signature, "base64url");
assert.equal(signatureBytes.length, 64, "توقيع P-256 بصيغة P1363 = ٦٤ بايت");

const independentOk = crypto.verify(
  "sha256",
  payloadBytes,
  { key: crypto.createPublicKey({ key: Buffer.from(publicKeyB64, "base64"), format: "der", type: "spki" }), dsaEncoding: "ieee-p1363" },
  signatureBytes
);
assert.ok(independentOk, "المكتبة المستقلة تعتبر التوقيع صحيحًا");

const inPageResult = await verifyLicenseToken(publicKeyB64, signed.token);
assert.ok(inPageResult.valid, "دالة التحقق في اللوحة تقبل الرمز");
assert.equal(inPageResult.payload, signed.payload, "الحمولة المفكوكة هي الحمولة الموقّعة");

const tampered = await verifyLicenseToken(
  publicKeyB64,
  signed.token.replace(/\.([A-Za-z0-9_-]{10})/, ".AAAAAAAAAA")
);
assert.ok(!tampered.valid, "أي تغيير في التوقيع يُرفض");

// ---------------------------------------------------------------- ٤) مطابقة الصيغة التي يقرأها التطبيق

assert.equal(signed.token.split(".").length, 3, "الرمز ثلاثة أجزاء: العلامة والحمولة والتوقيع");
assert.equal(signed.token.split(".")[0], "BNNA1", "علامة الرمز BNNA1");
assert.ok(!signed.token.includes("="), "لا حشو في base64url");

const payloadFields = signed.payload.split("|");
assert.equal(payloadFields.length, 8, "الحمولة ثمانية حقول كما يقرأها LicenseToken");
assert.deepEqual(
  payloadFields,
  [
    "1",
    fields.licenseId,
    licenseDeviceCode(fields.deviceCode),
    "MUSRIB",
    "MONTHLY",
    "30",
    String(fields.issuedAt),
    String(fields.expiresAt),
  ],
  "ترتيب الحقول وقيمها كما يتوقعها الفاحص في التطبيق"
);

// كود الجهاز يُطبَّع كما يطبَّعه التطبيق (بلا فواصل وبأحرف كبيرة)
assert.equal(licenseDeviceCode("msrb-8f42-9d1b"), "MSRB8F429D1B");

// المفتاح العام يُكتب في ملفّ الأصول: تأكيد أنه base64 قياسي بلا محارف غريبة.
assert.ok(/^[A-Za-z0-9+/]+={0,2}$/.test(publicKeyB64), "المفتاح العام base64 قياسي");
assert.ok(bytesFromBase64(publicKeyB64).length > 60, "المفتاح العام DER بطول معقول");
assert.equal(
  base64UrlFromBytes(Buffer.from(signed.payload, "utf8")),
  signed.token.split(".")[1],
  "ترميز الحمولة base64url مطابق لما يبنيه التطبيق"
);

console.log("✅ موقّع التصاريح: رمز اللوحة يُتحقق منه بمكتبة مستقلة، وصيغته مطابقة لما يقرأه التطبيق.");

// ---------------------------------------------------------------- ٥) المتجه الذهبي لاختبار Kotlin

console.log("\n# انسخ ما يلي إلى app/src/test/java/com/baynana/core/license/BrowserSignedLicenseTest.kt");
console.log(`# GOLDEN_PUBLIC_KEY_BASE64 = ${publicKeyB64}`);
console.log(`# GOLDEN_TOKEN = ${signed.token}`);
console.log(`# GOLDEN_PAYLOAD = ${signed.payload}`);
