#!/usr/bin/env node
/**
 * فحص الإصدار الموزَّع وتسجيله (ح٢١) — «لا نشر بلا توقيع، وبصمة تُقاس لا تُخمَّن».
 *
 * ثلاثة أوضاع:
 *   --check                    يفحص حال المستودع قبل البناء: رقم الإصدار لم يُوزَّع قبله، والسجلّ سليم.
 *   --verify-apk <ملفّ>        يفحص ملفًّا مبنيًّا: موقّع، وبصمة شهادته مطابقة للمسجَّل، ويطبع بصمتَيه.
 *   --stamp <ملفّ>              يُسجّل توزيعًا: يُضيف سطرًا بالبصمتين المحسوبتين من الملفّ نفسه.
 *
 * لماذا هذه الأداة موجودة؟ لأن خطأين صامتين يقعان في التوزيع المحدود:
 *   ١. **إعادة استعمال رقم إصدار** بعد أن وُزِّع: فيظنّ المالك أنه حدّث الأجهزة، وهي لا ترى شيئًا.
 *   ٢. **تغيّر مفتاح التوقيع** أو التوقيع بمفتاح غيره: فيتوقف التحديث على الأجهزة المثبَّتة،
 *      أو يصل الناس ملفًّا لا يثق به النظام.
 * والاثنان لا يُكتشفان بالنظر إلى الشاشة، فيُقاسان من الملفّ نفسه ويُسجَّلان في docs/RELEASE_LOG_AR.md.
 *
 * ملاحظة أمنية: هذه الأداة **لا توقّع شيئًا ولا تحمل مفتاحًا**. التوقيع يقع في البناء (Gradle)
 * بمفتاح المشروع من أسرار المستودع، وملفّ قناة التحديث يوقّعه المالك على جهازه (tools/release_sign.mjs).
 */
import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";
import { execFileSync } from "node:child_process";
import { fileURLToPath } from "node:url";

const here = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(here, "..");
const gradlePath = path.join(repoRoot, "app/build.gradle.kts");
const logPath = path.join(repoRoot, "docs/RELEASE_LOG_AR.md");

function fail(message) {
  console.error(`⛔ ${message}`);
  process.exit(1);
}

function gradleVersion() {
  const text = fs.readFileSync(gradlePath, "utf8");
  const code = Number((text.match(/versionCode\s*=\s*([0-9]+)/) ?? [])[1]);
  const name = (text.match(/versionName\s*=\s*"([^"]+)"/) ?? [])[1];
  if (!Number.isInteger(code) || !name) fail("تعذّر قراءة versionCode/versionName من app/build.gradle.kts");
  return { code, name };
}

/** يقرأ جدول السجلّ: صفوف البيانات وحدها (بلا الترويسة ولا صفّ «لم يوزَّع شيء بعد»). */
function readLog() {
  if (!fs.existsSync(logPath)) fail(`سجلّ الإصدارات مفقود: ${path.relative(repoRoot, logPath)}`);
  const entries = [];
  for (const line of fs.readFileSync(logPath, "utf8").split("\n")) {
    if (!line.trim().startsWith("|")) continue;
    const cells = line.split("|").map((cell) => cell.trim());
    // [ '', '#', 'versionCode', ... ] — نُبقي الصفوف التي أول خلية رقمية فيها رقمًا.
    if (cells.length < 8) continue;
    if (!/^[0-9]+$/.test(cells[2] ?? "")) continue;
    entries.push({
      index: cells[1],
      versionCode: Number(cells[2]),
      versionName: cells[3],
      apkSha256: cells[4],
      certSha256: cells[5],
      date: cells[6],
      kind: cells[7],
      note: cells[8] ?? "",
    });
  }
  return entries;
}

function checkLog() {
  const { code, name } = gradleVersion();
  const entries = readLog();
  const codes = entries.map((entry) => entry.versionCode);

  for (let i = 1; i < codes.length; i += 1) {
    if (codes[i] <= codes[i - 1]) {
      fail(`سجلّ الإصدارات غير تصاعدي: ${codes[i]} بعد ${codes[i - 1]} — رقم الإصدار يُوزَّع مرة واحدة بترتيب متزايد.`);
    }
  }
  if (new Set(codes).size !== codes.length) fail("رقم إصدار مكرّر في السجلّ — رقم واحد لتوزيع واحد.");

  const certs = new Set(entries.map((entry) => entry.certSha256).filter((value) => /^[0-9a-f]{64}$/.test(value)));
  if (certs.size > 1) {
    fail(
      "بصمة شهادة التوقيع تغيّرت بين توزيعين — تغيير المفتاح يقطع التحديث عن الأجهزة المثبَّتة. " +
        "راجع القاعدة: مفتاح واحد لا يُغيَّر بعد أول توزيع."
    );
  }

  if (codes.includes(code)) {
    fail(
      `رقم الإصدار الحالي (${code}) سُجِّل موزَّعًا في السجلّ. ارفعه قبل أي بناء يُوزَّع: ` +
        "الأجهزة التي حدّثت لن ترى إصدارًا بالرقم نفسه."
    );
  }

  const latest = codes.length > 0 ? Math.max(...codes) : 0;
  console.log(`OK: الإصدار الحالي ${name} (${code}) لم يُوزَّع بعد. أرقام موزَّعة: ${codes.length}، أعلاها ${latest}.`);
  if (certs.size === 1) console.log(`    بصمة شهادة التوقيع المعتمدة: ${[...certs][0]}`);
  if (latest >= code) {
    // لا يُمنع البناء، لكن يجب أن يُقال بصراحة: هذا بناء بلا تقدّم في الأرقام.
    console.warn(`تحذير: رقم الإصدار الحالي (${code}) ليس أكبر من أعلى رقم موزَّع (${latest}). لا يُوزَّع بهذا الرقم.`);
  }
}

function apksigner() {
  const home = process.env.ANDROID_HOME || process.env.ANDROID_SDK_ROOT || "/usr/local/lib/android/sdk";
  const dir = path.join(home, "build-tools");
  if (!fs.existsSync(dir)) return null;
  const candidates = fs
    .readdirSync(dir)
    .sort((a, b) => a.localeCompare(b, undefined, { numeric: true }))
    .map((version) => path.join(dir, version, "apksigner"));
  return candidates.reverse().find((candidate) => fs.existsSync(candidate)) ?? null;
}

function sha256OfFile(file) {
  return crypto.createHash("sha256").update(fs.readFileSync(file)).digest("hex");
}

/** بصمة شهادة التوقيع: تُقرأ من APK عبر apksigner (المصدر الموثوق لشهادة التوقيع). */
function certFingerprint(apk) {
  const signer = apksigner();
  if (!signer) {
    fail(
      "لم يُعثر على apksigner لحساب بصمة الشهادة. مرّر مسار ANDROID_HOME، أو شغّل هذا الوضع بعد البناء في CI."
    );
  }
  const output = execFileSync(signer, ["verify", "--print-certs", apk], { stdio: "pipe" }).toString();
  const match = output.match(/SHA-256 digest:\s*([0-9a-fA-F:]{64,})/);
  if (!match) fail("لم تُقرأ بصمة الشهادة من apksigner — لا تسجيل بلا بصمة.");
  const normalized = match[1].replace(/:/g, "").toLowerCase();
  if (!/^[0-9a-f]{64}$/.test(normalized)) fail(`بصمة شهادة غير متوقَّعة: ${match[1]}`);
  const debugSigned = /CN=Android Debug/.test(output);
  return { fingerprint: normalized, debugSigned, output };
}

function verifyApk(apk) {
  if (!fs.existsSync(apk)) fail(`لم يوجد الملفّ: ${apk}`);
  const { fingerprint, debugSigned, output } = certFingerprint(apk);
  const apkSha256 = sha256OfFile(apk);
  const entries = readLog();
  const expected = entries.map((entry) => entry.certSha256).find((value) => /^[0-9a-f]{64}$/.test(value));

  console.log(output.trim());
  console.log(`\nبصمة الملفّ (sha256): ${apkSha256}`);
  console.log(`بصمة شهادة التوقيع: ${fingerprint}`);
  if (debugSigned) console.log("النوع: موقّع بمفتاح التطوير (تجريبي) — لا يُوزَّع على الناس.");

  if (!debugSigned && expected && expected !== fingerprint) {
    fail(
      `شهادة هذا الملفّ (${fingerprint}) ليست الشهادة المعتمدة في السجلّ (${expected}). ` +
        "لا يُوزَّع: الأجهزة المثبَّتة لن تقبل التحديث، ومعنى ذلك تدبيران للتطبيق في السوق."
    );
  }
  if (!debugSigned && !expected) {
    console.log("ملاحظة: لا بصمة معتمدة في السجلّ بعد — أول توزيع يسجّلها.");
  }
  return { apkSha256, fingerprint, debugSigned };
}

function stampApk(apk, options) {
  const { apkSha256, fingerprint, debugSigned } = verifyApk(apk);
  if (debugSigned) fail("لا يُسجَّل توزيع لنسخة تجريبية موقّعة بمفتاح التطوير.");
  const { code, name } = gradleVersion();
  const entries = readLog();
  if (entries.some((entry) => entry.versionCode === code)) {
    fail(`رقم الإصدار ${code} مسجَّل سابقًا — ارفعه قبل التسجيل.`);
  }
  const date = options.date ?? new Date().toISOString().slice(0, 10);
  const kind = options.kind ?? "إصدار محدود";
  const note = (options.note ?? "").replace(/\|/g, "/");
  const index = entries.length + 1;

  let text = fs.readFileSync(logPath, "utf8");
  const tableStart = text.indexOf("|---|---|---|---|---|---|---|---|");
  const afterTable = text.indexOf("\n", tableStart) + 1;
  // الصفّ الأول يحلّ محلّ صفّ «لم يوزَّع شيء بعد» إن كان وحده.
  const emptyRow = "| — | — | — | — | — | — | — | لم يُوزَّع شيء بعد |\n";
  const row = `| ${index} | ${code} | ${name} | ${apkSha256} | ${fingerprint} | ${date} | ${kind} | ${note} |\n`;
  text = text.includes(emptyRow) ? text.replace(emptyRow, row) : text.slice(0, afterTable) + row + text.slice(afterTable);
  fs.writeFileSync(logPath, text, "utf8");

  console.log(`\n✅ سُجّل التوزيع رقم ${index}: الإصدار ${name} (${code}).`);
  console.log("   لا تُغيَّر بصمة الشهادة في السجلّ، ولا يُعاد استعمال رقم الإصدار أبدًا.");
}

// ------------------------------------------------------------------ التنفيذ

const args = process.argv.slice(2);
const valueOf = (flag) => {
  const index = args.indexOf(flag);
  return index >= 0 ? args[index + 1] : undefined;
};

if (args.includes("--check")) {
  checkLog();
  process.exit(0);
} else if (args.includes("--verify-apk")) {
  verifyApk(valueOf("--verify-apk"));
  process.exit(0);
} else if (args.includes("--stamp")) {
  stampApk(valueOf("--stamp"), { date: valueOf("--date"), kind: valueOf("--kind"), note: valueOf("--note") });
  process.exit(0);
} else {
  const header = fs.readFileSync(new URL(import.meta.url), "utf8");
  console.log(header.slice(header.indexOf("/**") + 3, header.indexOf("*/")).replace(/^ \*?/gm, "").trim());
  process.exit(2);
}
