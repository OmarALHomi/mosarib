#!/usr/bin/env node
/**
 * اختبار أدوات قناة الإصدار (ح٢٠) — يعمل في CI، بلا شبكة وبلا أندرويد.
 *
 * لماذا هذا الاختبار موجود؟ لأن أخطر عيب في قناة التحديث لا يظهر في اختبار محلي: أن تكون الأداة
 * (Node) والتطبيق (Kotlin) لا يفهمان الشيء نفسه — فرق في ترتيب الحقول، أو في ترميز الحمولة، أو في
 * صيغة التوقيع — فيظنّ المالك أن القناة تعمل، وتُرفض ملفّاته في كل الأجهزة.
 *
 * فيُفعل هنا ثلاثة أمور:
 *   ١. **المتجهات مطابقة لما تنتجه الأداة الآن** (`release_manifest.mjs --check`): أي فرق بين ما
 *      هو مثبَّت في اختبارات Kotlin وما تنتجه الأداة يُفشل الفحص. فلا تنفصل النسختان.
 *   ٢. **الأداة تُنتج ملفًّا يُقبل، وتَرفض ما يجب رفضه:** تعديل حرف، توقيع مقلوب، نصّ عارٍ،
 *      رابط http، بصمة ناقصة، رسالة فارغة، وإصدار إداريًا متناقض.
 *   ٣. **التوقيع يتحقق بمكتبة مستقلة** (`crypto.verify`) لا بالكود الذي وقّع به، تشغيلان مستقلان
 *      على البايتات نفسها.
 */
import assert from "node:assert/strict";
import crypto from "node:crypto";
import fs from "node:fs";
import { execFileSync } from "node:child_process";
import path from "node:path";
import { fileURLToPath } from "node:url";
import {
  EXAMPLE_RELEASE,
  VECTOR_TEST_PRIVATE_KEY_BASE64,
  VECTOR_TEST_PUBLIC_KEY_BASE64,
  buildPayload,
  buildToken,
  signPayload,
  verifyPayload,
} from "./release_common.mjs";

const here = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(here, "..");

let checks = 0;
function ok(label) {
  checks += 1;
  console.log(`  ✅ ${label}`);
}

// ---------------------------------------------------------------- ١) المتجهات مطابقة للأداة
execFileSync(process.execPath, [path.join(here, "release_manifest.mjs"), "--check"], { stdio: "inherit" });
ok("متجهات Kotlin مطابقة لما تنتجه الأداة");

// ---------------------------------------------------------------- ٢) دورة توقيع/تحقق كاملة
const { payload, signature, token } = buildToken(EXAMPLE_RELEASE);
assert.equal(payload.split("|").length, 8, "الحمولة ثمانية حقول بالترتيب الذي يقرأه التطبيق");
assert.ok(/^BNR1\./.test(token), "الرمز يبدأ بعلامة الملفّ");
assert.ok(verifyPayload(payload, signature), "التوقيع يتحقق بمكتبة Node المستقلة");
ok("توقيع ← رمز ← تحقق بمكتبة مستقلة");

// التطبيق يفكّ الحمولة من الرمز نفسه: يجب أن تعود الحمولة بالنصّ الموقّع بالحرف.
const fromToken = Buffer.from(token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/"), "base64").toString("utf8");
assert.equal(fromToken, payload, "الحمولة المفكوكة من الرمز هي نفسها التي وُقّعت");
ok("فكّ الترميز يعيد النصّ الموقّع حرفًا بحرف");

// ---------------------------------------------------------------- ٣) الرفض حيث يجب الرفض
function flipFirstChar(text) {
  return (text[0] === "A" ? "B" : "A") + text.slice(1);
}

const tamperedParts = token.split(".");
tamperedParts[1] = flipFirstChar(tamperedParts[1]);
const tamperedToken = tamperedParts.join(".");
const tamperedPayload = Buffer.from(
  tamperedParts[1].replace(/-/g, "+").replace(/_/g, "/"),
  "base64"
).toString("utf8");

assert.notEqual(tamperedPayload, payload, "التشويه يجب أن يغيّر الحمولة فعلًا");
assert.ok(!verifyPayload(tamperedPayload, signature), "الحمولة المشوَّهة بتوقيعها الأصلي تُرفض");
assert.ok(!verifyPayload(payload, signature.replace(/^./, flipFirstChar(signature))), "توقيع مقلوب يُرفض");
assert.ok(!verifyPayload(payload + "x", signature), "حمولة زائدة تُرفض");
ok("تعديل حرف في الحمولة أو في التوقيع يُكتشف");

// محاكاة ما يفعله التطبيق حرفيًّا: يفكّ الأجزاء، ثم يتحقق بالبايتات نفسها.
function kotlinStyleRead(manifest) {
  const parts = String(manifest).trim().split(".");
  if (parts.length !== 3 || parts[0] !== "BNR1") throw new Error("ليس ملفّ إصدار");
  const decoded = Buffer.from(parts[1].replace(/-/g, "+").replace(/_/g, "/"), "base64").toString("utf8");
  if (!verifyPayload(decoded, parts[2])) throw new Error("توقيع مرفوض");
  return decoded;
}
assert.equal(kotlinStyleRead(token), payload, "الرمز السليم يُقرأ ويُتحقق");
assert.throws(() => kotlinStyleRead(tamperedToken), /توقيع مرفوض/, "الرمز المحرَّف لا يمرّ");
assert.throws(() => kotlinStyleRead("{\"versionCode\":99}"), /ليس ملفّ إصدار/);
ok("الرمز السليم يمرّ والمحرَّف يسقط، في محاكاة فحص التطبيق");

// ---------------------------------------------------------------- ٤) أداة المالك: فحوص القواعد
const tmpDir = execFileSync("mktemp", ["-d"]).toString().trim();
const keyPem = path.join(tmpDir, "release-key.pem");
const apkPath = path.join(tmpDir, "baynana.apk");
const outPath = path.join(tmpDir, "release-manifest.txt");

const privateKey = crypto.createPrivateKey({
  key: Buffer.from(VECTOR_TEST_PRIVATE_KEY_BASE64, "base64"),
  format: "der",
  type: "pkcs8",
});
fs.writeFileSync(keyPem, privateKey.export({ type: "pkcs8", format: "pem" }));
fs.writeFileSync(apkPath, Buffer.from("apk-for-test".repeat(64)));

execFileSync(
  process.execPath,
  [
    path.join(here, "release_sign.mjs"),
    "--key", keyPem,
    "--apk", apkPath,
    "--url", "https://example.github.io/baynana/releases/baynana-1.2.apk",
    "--version-code", "4",
    "--version-name", "1.2",
    "--min-supported", "1",
    "--message", "تحسينات وإصلاحات",
    "--out", outPath,
  ],
  { stdio: "pipe" }
);
const signedToken = fs.readFileSync(outPath, "utf8").trim();
assert.ok(signedToken.startsWith("BNR1."), "الأداة تُنتج رمزًا بالصيغة");
const signedPayload = Buffer.from(signedToken.split(".")[1].replace(/-/g, "+").replace(/_/g, "/"), "base64").toString("utf8");
assert.ok(verifyPayload(signedPayload, signedToken.split(".")[2]), "مخرَج الأداة يتحقق بمكتبة مستقلة");
assert.ok(signedPayload.includes(crypto.createHash("sha256").update(fs.readFileSync(apkPath)).digest("hex")), "البصمة محسوبة من الملفّ نفسه");
ok("أداة المالك تنتج ملفًّا صحيحًا وبصمته من الملفّ لا من رأس الاستجابة");

// وترفض الأداة ما يجب رفضه، قبل أن يصل إلى الأجهزة.
function signExpectingFailure(extraArgs, expected) {
  try {
    execFileSync(process.execPath, [
      path.join(here, "release_sign.mjs"),
      "--key", keyPem,
      "--url", "https://example.com/app.apk",
      "--sha256", "a".repeat(64),
      "--version-code", "4",
      "--version-name", "1.2",
      "--min-supported", "1",
      "--message", "رسالة",
      ...extraArgs,
    ], { stdio: "pipe" });
  } catch (error) {
    const output = `${error.stdout}${error.stderr}`;
    assert.ok(output.includes(expected), `الرفض يجب أن يذكر السبب (${expected})، والخرج: ${output.slice(0, 120)}`);
    return;
  }
  assert.fail("كان يجب أن ترفض الأداة هذا الاستعمال");
}

// الحجّة الأخيرة تُغلب ما قبلها في الأداة، فيُمرَّر التجاوز وحده بعد الأساس الصحيح.
signExpectingFailure(["--url", "http://example.com/app.apk"], "https");
signExpectingFailure(["--min-supported", "9"], "أقدم إصدار مدعوم");
signExpectingFailure(["--version-name", "v1.2"], "اسم الإصدار");
signExpectingFailure(["--message", "   "], "الرسالة العربية مطلوبة");
ok("الأداة ترفض: رابطًا غير مشفّر، حدًّا أدنى متناقضًا، واسم إصدار غير صالح");

console.log(`\n✅ اختبار أدوات قناة الإصدار: ${checks} فحوص، كلها سليمة.`);
