#!/usr/bin/env node
/**
 * مولّد «المتجه الذهبي» لقناة الإصدار (ح٢٠) وكاتب ملفّ الاختبار Kotlin.
 *
 * الاستعمال:
 *   node tools/release_manifest.mjs --write     # يكتب ReleaseSignatureVectors.kt
 *   node tools/release_manifest.mjs --check      # يتحقق أن الملفّ المكتوب هو الناتج الحالي
 *   node tools/release_manifest.mjs --print      # يطبع الملفّ الناتج للعين
 *
 * لِمَ «--check» موجود؟ لأن المتجهات إن قُدت يدويًا فستنفصل عن الأداة بعد أول تعديل، فيصير الاختبار
 * يقيس شيئًا لم تعُد الأداة تنتجه. فـCI يستدعي `--check`: أي فرق بين ما تنتجه الأداة وما هو مثبَّت
 * يُفشل الفحص.
 *
 * والمتجهات نفسها تُعيد إنتاجها بمفتاح اختبار معلن (انظر release_common.mjs): لا سرّ في المستودع،
 * والنتيجة ثابتة يُعاد توليدها في أي وقت.
 */
import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import {
  EXAMPLE_RELEASE,
  REQUIRED_RELEASE,
  RELEASE_PREFIX,
  VECTOR_TEST_PUBLIC_KEY_BASE64,
  buildPayload,
  buildToken,
  bytesFromBase64Url,
  signPayload,
} from "./release_common.mjs";

const here = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(here, "..");
const outputPath = path.join(
  repoRoot,
  "app/src/test/java/com/baynana/domain/update/ReleaseSignatureVectors.kt"
);

/** متجه مشوّه من متجه سليم: يُقلب حرف في الحمولة بعد الترميز (اختبار التوقيع لا البيانات). */
function tamperPayload(token) {
  const parts = token.split(".");
  const encoded = parts[1];
  const first = encoded[0];
  const replacement = first === "A" ? "B" : "A";
  parts[1] = replacement + encoded.slice(1);
  return parts.join(".");
}

/** متجه بتوقيع مقلوب بايت واحد: كل الحقول سليمة، والتوقيع وحده ليس من المالك. */
function corruptSignature(token) {
  const parts = token.split(".");
  const signature = parts[2];
  const flipped = (signature[0] === "A" ? "B" : "A") + signature.slice(1);
  parts[2] = flipped;
  return parts.join(".");
}

const optional = buildToken(EXAMPLE_RELEASE);
const required = buildToken(REQUIRED_RELEASE);

const kotlin = `package com.baynana.domain.update

/**
 * متجهات ذهبية لقناة الإصدار (ح٢٠) — **مولَّدة آليًّا، لا تُحرَّر بيد**.
 *
 * المصدر: \`node tools/release_manifest.mjs --write\` (والتحقق منها: \`--check\` في CI).
 * والمفتاح هنا **مفتاح اختبار معلن** وظيفته توليد متجهات ثابتة؛ لا يوقّع أي ملفّ إصدار حقيقي.
 * والغاية: أن يُوقَّع الرمز بأداة المالك (Node) ويُتحقق منه في التطبيق (Kotlin) على البايتات
 * نفسها — فالرحلة كاملة مفحوصة، لا «يعمل عندي».
 */
object ReleaseSignatureVectors {

    const val TEST_PUBLIC_KEY_BASE64 =
        "${VECTOR_TEST_PUBLIC_KEY_BASE64}"

    /** إصدار اختياري: أحدث من ٣، ومدعوم للجميع. */
    const val OPTIONAL_TOKEN =
        "${optional.token}"

    const val OPTIONAL_PAYLOAD =
        "${optional.payload}"

    /** إصدار إجباري: من هو على ٤ أو أقل يلزمه التحديث. */
    const val REQUIRED_TOKEN =
        "${required.token}"

    /** نفس الحمولة بحرف مقلوب في ترميزها: التوقيع يجب أن يسقط. */
    const val TAMPERED_PAYLOAD_TOKEN =
        "${tamperPayload(optional.token)}"

    /** الحمولة الصحيحة بتوقيع مقلوب: يجب أن يسقط. */
    const val CORRUPT_SIGNATURE_TOKEN =
        "${corruptSignature(optional.token)}"

    /** ملفّ بلا علامة «بيننا»: لا يُقبل ولو كان JSON سليمًا. */
    const val PLAIN_JSON =
        "{\\"versionCode\\":99,\\"versionName\\":\\"9.9\\",\\"apkUrl\\":\\"https://attacker.example/app.apk\\"}"
}
`;

const mode = process.argv[2] ?? "--print";
if (mode === "--write") {
  fs.mkdirSync(path.dirname(outputPath), { recursive: true });
  fs.writeFileSync(outputPath, kotlin, "utf8");
  console.log(`كُتب: ${path.relative(repoRoot, outputPath)}`);
} else if (mode !== "--check") {
  process.stdout.write(kotlin);
}

/**
 * فحص المتجهات المثبَّتة. المقارنة **ليست** تساوي البايتات: توقيع ECDSA فيه عشوائية مشروعة
 * (نقطة ارتكاز مختلفة كل مرة)، وكل توقيع صحيح مقبول. فالفحص يتحقق مما يهمّ فعلًا:
 *   - الحمولة المثبَّتة هي نفسها التي تبنيها الأداة الآن (ترتيب الحقول ومحتواها)،
 *   - توقيع كل متجه سليم يتحقق بالمفتاح العام نفسه بمكتبة مستقلة،
 *   - والمتجهان المشوَّهان لا يتحققان (وإلا لسقطت قيمة الاختبار نفسه).
 */
function readVectors() {
  if (!fs.existsSync(outputPath)) return null;
  const text = fs.readFileSync(outputPath, "utf8");
  const constant = (name) => {
    const match = text.match(new RegExp(`${name}\\s*=\\s*\\n?\\s*"([^"]+)"`));
    return match ? match[1] : null;
  };
  return {
    text,
    publicKey: constant("TEST_PUBLIC_KEY_BASE64"),
    optionalToken: constant("OPTIONAL_TOKEN"),
    optionalPayload: constant("OPTIONAL_PAYLOAD"),
    requiredToken: constant("REQUIRED_TOKEN"),
    tamperedToken: constant("TAMPERED_PAYLOAD_TOKEN"),
    corruptToken: constant("CORRUPT_SIGNATURE_TOKEN"),
  };
}

function verifyWith(publicKeyBase64, payload, signatureBase64Url) {
  const key = crypto.createPublicKey({ key: Buffer.from(publicKeyBase64, "base64"), format: "der", type: "spki" });
  return crypto.verify(
    "sha256",
    Buffer.from(payload, "utf8"),
    { key, dsaEncoding: "der" },
    bytesFromBase64Url(signatureBase64Url)
  );
}

function payloadOfToken(token) {
  const parts = String(token).split(".");
  if (parts.length !== 3 || parts[0] !== RELEASE_PREFIX) return null;
  return Buffer.from(parts[1].replace(/-/g, "+").replace(/_/g, "/"), "base64").toString("utf8");
}

function checkVectors() {
  const vectors = readVectors();
  if (!vectors) return { ok: false, why: "ملفّ المتجهات غير موجود" };
  if (vectors.publicKey !== VECTOR_TEST_PUBLIC_KEY_BASE64) {
    return { ok: false, why: "المفتاح العام في المتجهات ليس مفتاح الاختبار المعلن" };
  }
  const failures = [];

  const optionalPayload = buildPayload(EXAMPLE_RELEASE);
  if (vectors.optionalPayload !== optionalPayload) failures.push("حمولة الإصدار الاختياري لا تساوي ما تبنيه الأداة");
  if (payloadOfToken(vectors.optionalToken) !== optionalPayload) failures.push("حمولة رمز الإصدار الاختياري مختلفة");
  if (!verifyWith(vectors.publicKey, optionalPayload, vectors.optionalToken.split(".")[2])) {
    failures.push("توقيع الإصدار الاختياري لا يتحقق بالمفتاح العام");
  }

  const requiredPayload = buildPayload(REQUIRED_RELEASE);
  if (payloadOfToken(vectors.requiredToken) !== requiredPayload) failures.push("حمولة الإصدار الإجباري مختلفة");
  if (!verifyWith(vectors.publicKey, requiredPayload, vectors.requiredToken.split(".")[2])) {
    failures.push("توقيع الإصدار الإجباري لا يتحقق بالمفتاح العام");
  }

  if (verifyWith(vectors.publicKey, payloadOfToken(vectors.tamperedToken), vectors.tamperedToken.split(".")[2])) {
    failures.push("المتجه المشوَّه في حمولته يمرّ — والاختبار يفقد قيمته");
  }
  if (verifyWith(vectors.publicKey, optionalPayload, vectors.corruptToken.split(".")[2])) {
    failures.push("المتجه المشوَّه في توقيعه يمرّ — والاختبار يفقد قيمته");
  }
  // والحمولة المشوَّهة ليست هي الحمولة الصحيحة (وإلا لما كان التشويه تشويهًا).
  if (payloadOfToken(vectors.tamperedToken) === optionalPayload) failures.push("التشويه لم يغيّر الحمولة أصلًا");

  return failures.length === 0 ? { ok: true } : { ok: false, why: failures.join("؛ ") };
}

if (mode === "--check") {
  const result = checkVectors();
  if (!result.ok) {
    console.error(`⛔ متجهات قناة الإصدار غير سليمة: ${result.why}`);
    console.error("   أعِد التوليد: node tools/release_manifest.mjs --write");
    process.exit(1);
  }
  console.log("OK: متجهات قناة الإصدار مطابقة لما تنتجه الأداة، وتواقيعها تتحقق بمكتبة مستقلة.");
  process.exit(0);
}
