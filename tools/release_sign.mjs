#!/usr/bin/env node
/**
 * أداة المالك لتوقيع ملفّ الإصدار (ح٢٠). **تُشغَّل على جهاز المالك وحده.**
 *
 * المفتاح الخاص لا يدخل المستودع ولا التطبيق: يُقرأ من ملفّ خارجي يمرّره المالك (أو من متغيّر
 * بيئة)، ويُستعمل هنا ثم يُنسى. والتوقيع يقع على **نصّ الحمولة نفسه** الذي يبنيه التطبيق بالترتيب
 * نفسه، فلو اختلف حرف واحد لَرُفض الملفّ في الأجهزة.
 *
 * الاستعمال:
 *   node tools/release_sign.mjs \
 *     --key ~/keys/release-key.pem \
 *     --url https://example.github.io/baynana/releases/baynana-1.2.apk \
 *     --sha256 <بصمة الملفّ المبنية> \
 *     --version-code 4 --version-name 1.2 --min-supported 1 \
 *     --message "إصلاح حساب الباقي عند السداد الجزئي" \
 *     --out release-manifest.txt
 *
 * ثم يُنشر محتوى الملفّ على: <العنوان الأساسي>/api/v1/app-release
 *
 * **التحقق قبل التسليم:** الأداة تتحقق من الرمز بمفتاحها العام بعد توقيعه، وتقارن بصمة الملفّ
 * إن مُرّر `--apk`: فلا يُنشر ملفّ إصدار يشير إلى بصمة مخالفة لما بُني فعلًا.
 */
import crypto from "node:crypto";
import fs from "node:fs";
import { buildPayload, buildToken, base64UrlFromBytes } from "./release_common.mjs";

function parseArgs(argv) {
  const args = {};
  for (let i = 0; i < argv.length; i += 2) {
    const key = argv[i];
    if (!key?.startsWith("--")) continue;
    args[key.slice(2)] = argv[i + 1];
  }
  return args;
}

function usage(message) {
  console.error(`⛔ ${message}\n`);
  console.error("راجع رأس الملفّ لأمثلة الاستعمال (node tools/release_sign.mjs --help).");
  process.exit(2);
}

const args = parseArgs(process.argv.slice(2));
if (process.argv.includes("--help") || process.argv.length < 3) {
  // الاستعمال المعروض هو رأس الملفّ نفسه: نسخة واحدة لا تتفرّع.
  const header = fs.readFileSync(new URL(import.meta.url), "utf8");
  console.log(header.slice(header.indexOf("/**") + 3, header.indexOf("*/")).replace(/^ \*?/gm, "").trim());
  process.exit(0);
}

const privateKeyPath = args.key;
if (!privateKeyPath) usage("مرّر --key بمسار ملفّ المفتاح الخاص (خارج المستودع).");
if (!fs.existsSync(privateKeyPath)) usage(`لم يوجد ملفّ المفتاح: ${privateKeyPath}`);

const privateKeyPem = fs.readFileSync(privateKeyPath, "utf8");
const privateKey = crypto.createPrivateKey(privateKeyPem);

// البصمة: إما تُحسب من ملفّ الـAPK نفسه، أو تُمرَّر صريحة. ولا تُترك فارغة.
let sha256 = args.sha256?.toLowerCase()?.replace(/[^0-9a-f]/g, "");
if (args.apk) {
  const digest = crypto.createHash("sha256").update(fs.readFileSync(args.apk)).digest("hex");
  if (sha256 && sha256 !== digest) usage("البصمة الممرَّرة لا تطابق بصمة ملفّ الـAPK — لا ننشر ملفًّا مضلِّلًا.");
  sha256 = digest;
}
if (!sha256 || sha256.length !== 64) usage("بصمة sha256 مطلوبة (٦٤ حرفًا hex)، أو مرّر --apk لحسابها.");

const versionCode = Number(args["version-code"]);
const versionName = String(args["version-name"] ?? "");
const minSupported = Number(args["min-supported"] ?? 1);
const url = String(args.url ?? "");
const message = String(args.message ?? "");
const publishedAt = args["published-at"] ? Number(args["published-at"]) : Date.now();

const release = {
  versionCode,
  versionName,
  minSupportedVersionCode: minSupported,
  apkUrl: url,
  apkSha256: sha256,
  publishedAt,
  messageArabic: message,
};

// فحص مسبق على القواعد التي سيقرؤها التطبيق، فلا نُنتج ملفًّا نعرف أنه سيُرفض.
const payload = buildPayload(release);
const payloadBytes = Buffer.from(payload, "utf8");
const signature = base64UrlFromBytes(
  crypto.sign("sha256", payloadBytes, { key: privateKey, dsaEncoding: "der" })
);
const token = `BNR1.${base64UrlFromBytes(payloadBytes)}.${signature}`;

const publicKeyDer = crypto.createPublicKey(privateKey).export({ type: "spki", format: "der" });
const publicKeyBase64 = publicKeyDer.toString("base64");
const verified = crypto.verify(
  "sha256",
  payloadBytes,
  { key: crypto.createPublicKey({ key: publicKeyDer, format: "der", type: "spki" }), dsaEncoding: "der" },
  Buffer.from(signature.replace(/-/g, "+").replace(/_/g, "/") + "=".repeat((4 - (signature.length % 4)) % 4), "base64")
);

if (!verified) usage("التوقيع لم يتحقق من مفتاحه العام — ملفّ غير صالح، ولم يُكتب شيء.");

// فحوص القواعد نفسها التي في التطبيق، لتُقرأ رسالة عربية واحدة واضحة قبل النشر.
const problems = [];
if (!Number.isInteger(versionCode) || versionCode <= 0) problems.push("رقم الإصدار يجب أن يكون عددًا صحيحًا موجبًا.");
if (!/^[0-9]+\.[0-9]+(\.[0-9]+)?$/.test(versionName)) problems.push("اسم الإصدار بصيغة «رقم.رقم» أو «رقم.رقم.رقم».");
if (!Number.isInteger(minSupported) || minSupported < 1 || minSupported > versionCode) {
  problems.push("أقدم إصدار مدعوم لا يمكن أن يكون أكبر من رقم الإصدار.");
}
if (!url.startsWith("https://")) problems.push("الرابط يجب أن يكون https.");
if (!message.trim()) problems.push("الرسالة العربية مطلوبة: المستخدم يستحق أن يعرف ما تغيّر.");
if (message.includes("\n")) problems.push("الرسالة سطر واحد بلا أسطر جديدة.");
if (problems.length > 0) usage(`الملفّ سيُرفض في الأجهزة:\n  - ${problems.join("\n  - ")}`);

const outPath = args.out;
if (outPath) fs.writeFileSync(outPath, token + "\n", "utf8");

console.log("✅ وُقّع ملفّ الإصدار وتحقّق من توقيعه.");
console.log(`   الإصدار: ${versionName} (${versionCode}) — أقدم مدعوم: ${minSupported}`);
console.log(`   المفتاح العام (يُلصق في app/src/main/assets/release_public_key.txt):`);
console.log(`   ${publicKeyBase64}`);
if (outPath) console.log(`   الملفّ: ${outPath} — انشره على: <العنوان الأساسي>/api/v1/app-release`);
else console.log(`\n${token}`);
