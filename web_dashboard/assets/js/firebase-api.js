/**
 * Mosarib Admin Portal - Direct Firebase REST Client
 * Communicates directly with Firebase Realtime Database without requiring server-side proxies.
 * Solves all CORS and session save issues on InfinityFree and shared hosting.
 */

const FIRESTORE_BASE_URL = "https://firestore.googleapis.com/v1/projects/mosarib/databases/(default)/documents";
const DEFAULT_RTDB_URL = "https://mosarib-default-rtdb.asia-southeast1.firebasedatabase.app";

let storedUrl = localStorage.getItem("mosarib_rtdb_url");
if (!storedUrl || storedUrl.includes("firebaseio.com")) {
  storedUrl = DEFAULT_RTDB_URL;
  localStorage.setItem("mosarib_rtdb_url", storedUrl);
}
let RTDB_BASE_URL = storedUrl;
let FIREBASE_BASE_URL = RTDB_BASE_URL;
let adminSecretKey = localStorage.getItem("mosarib_admin_secret_key") || "";
const SECRET_SALT = "mosarib_secure_license_secret_key_alhomi_2026_water_app";

function getFirebaseUrl(path) {
  const cleanPath = path.replace(/^\/+/, '');
  const sep = cleanPath.includes("?") ? "&" : "?";
  return adminSecretKey ? `${FIREBASE_BASE_URL}/${cleanPath}${sep}auth=${encodeURIComponent(adminSecretKey)}` : `${FIREBASE_BASE_URL}/${cleanPath}`;
}

function updateFirebaseStatusIndicator(state, detail = '') {
  const el = document.getElementById("firebaseStatus");
  if (!el) return;
  if (state === 'CONNECTED') {
    el.className = "status-badge status-active";
    el.style.borderColor = "rgba(0, 230, 118, 0.4)";
    el.style.background = "rgba(0, 230, 118, 0.12)";
    el.innerHTML = `<div class="status-dot" style="background:#00E676; box-shadow:0 0 8px #00E676;"></div> متصل بفايربيس`;
    el.title = detail || "تم التحقق والاتصال بنجاح بسحابة فايربيس";
  } else if (state === 'AUTH_REQUIRED') {
    el.className = "status-badge";
    el.style.borderColor = "rgba(255, 179, 0, 0.5)";
    el.style.background = "rgba(255, 179, 0, 0.15)";
    el.innerHTML = `<div class="status-dot" style="background:#FFB300; box-shadow:0 0 8px #FFB300;"></div> مفتاح الصلاحية مطلوب ⚠️`;
    el.title = "قاعدة البيانات تتطلب مفتاح Database Secret أو تعديل القواعد في Firebase Console. انقر هنا للضبط.";
  } else {
    el.className = "status-badge";
    el.style.borderColor = "rgba(239, 83, 80, 0.4)";
    el.style.background = "rgba(239, 83, 80, 0.12)";
    el.innerHTML = `<div class="status-dot" style="background:#EF5350; box-shadow:0 0 8px #EF5350;"></div> تعذر الاتصال بفايربيس ❌`;
    el.title = detail || "تعذر الوصول لقاعدة البيانات. انقر هنا لفحص الرابط.";
  }
}

async function firebaseFetch(path, options = {}) {
  const directUrl = getFirebaseUrl(path);
  try {
    const res = await fetch(directUrl, options);
    if (res && res.ok) {
      updateFirebaseStatusIndicator('CONNECTED');
      return res;
    }
    if (res && res.status === 401) {
      updateFirebaseStatusIndicator('AUTH_REQUIRED');
    }
    // Direct fetch was not ok (e.g. 401 unauthorized or server error), try PHP proxy fallback if available
    try {
      const proxyUrl = `api/firebase_proxy.php?path=${encodeURIComponent(path)}${adminSecretKey ? `&auth=${encodeURIComponent(adminSecretKey)}` : ''}`;
      const proxyRes = await fetch(proxyUrl, options);
      if (proxyRes && proxyRes.ok) {
        updateFirebaseStatusIndicator('CONNECTED');
        return proxyRes;
      }
    } catch (_) {}
    return res;
  } catch (err) {
    // Browser direct fetch failed with network/CORS error, fallback to server-side PHP proxy
    try {
      const proxyUrl = `api/firebase_proxy.php?path=${encodeURIComponent(path)}${adminSecretKey ? `&auth=${encodeURIComponent(adminSecretKey)}` : ''}`;
      const proxyRes = await fetch(proxyUrl, options);
      if (proxyRes && proxyRes.ok) {
        updateFirebaseStatusIndicator('CONNECTED');
        return proxyRes;
      }
      return proxyRes;
    } catch (_) {}
    updateFirebaseStatusIndicator('ERROR');
    return null;
  }
}

function promptConfigureFirebaseUrl() {
  const choice = prompt(
    "⚙️ إعدادات ربط قاعدة بيانات فايربيس (Firebase Settings):\n\n" +
    "1. تعديل رابط Realtime Database\n" +
    "2. إدخال/تعديل مفتاح الأمان (Database Secret)\n" +
    "3. فحص الاتصال وتحديث البيانات الآن\n" +
    "4. إعادة ضبط الرابط الافتراضي الرسمي\n\n" +
    "الرابط الحالي:\n" + FIREBASE_BASE_URL + "\n\n" +
    "المفتاح السري الحالي:\n" + (adminSecretKey ? "•••••••••••••• (محدد ومحفوظ)" : "غير محدد (اتصال مفتوح القواعد)") + "\n\n" +
    "أدخل رقم الخيار (1 أو 2 أو 3 أو 4):",
    "3"
  );

  if (choice === "1") {
    const newUrl = prompt("أدخل رابط Firebase Realtime Database الجديد:", FIREBASE_BASE_URL);
    if (newUrl && newUrl.trim()) {
      let clean = newUrl.trim().replace(/\/+$/, '');
      if (!clean.startsWith("http://") && !clean.startsWith("https://")) clean = "https://" + clean;
      localStorage.setItem("mosarib_rtdb_url", clean);
      FIREBASE_BASE_URL = clean;
      RTDB_BASE_URL = clean;
      alert("✅ تم حفظ الرابط بنجاح: " + clean);
      loadFirebaseData();
    }
  } else if (choice === "2") {
    const newSecret = prompt(
      "أدخل مفتاح Database Secret لمشروع فايربيس (من إعدادات Firebase Console > Service Accounts > Database Secrets):",
      adminSecretKey
    );
    if (newSecret !== null) {
      adminSecretKey = newSecret.trim();
      localStorage.setItem("mosarib_admin_secret_key", adminSecretKey);
      alert(adminSecretKey ? "✅ تم حفظ مفتاح الأمان بنجاح وسيتم إرفاقه بجميع الطلبات السحابية!" : "تم إلغاء مفتاح الأمان.");
      loadFirebaseData();
    }
  } else if (choice === "3") {
    loadFirebaseData();
  } else if (choice === "4") {
    localStorage.setItem("mosarib_rtdb_url", DEFAULT_RTDB_URL);
    FIREBASE_BASE_URL = DEFAULT_RTDB_URL;
    RTDB_BASE_URL = DEFAULT_RTDB_URL;
    alert("✅ تم استرجاع الرابط الافتراضي: " + DEFAULT_RTDB_URL);
    loadFirebaseData();
  }
}

function parseFirestoreDoc(doc) {
  if (!doc || !doc.fields) return {};
  const res = {};
  for (const [key, val] of Object.entries(doc.fields)) {
    if (val.stringValue !== undefined) res[key] = val.stringValue;
    else if (val.integerValue !== undefined) res[key] = parseInt(val.integerValue, 10);
    else if (val.doubleValue !== undefined) res[key] = parseFloat(val.doubleValue);
    else if (val.booleanValue !== undefined) res[key] = val.booleanValue;
    else if (val.timestampValue !== undefined) res[key] = val.timestampValue;
    else res[key] = val;
  }
  return res;
}

async function fetchCollection(name) {
  // 1. First Attempt: Realtime Database with auth and proxy fallback
  try {
    const res = await firebaseFetch(`${name}.json`);
    if (res && res.ok) {
      const data = await safeJson(res);
      const items = (data && data.data !== undefined) ? data.data : data;
      if (Array.isArray(items)) return items;
      if (items && typeof items === 'object') return Object.values(items);
    }
  } catch (_) {}

  // 2. Second Attempt: Cloud Firestore REST API
  try {
    const res = await fetch(`${FIRESTORE_BASE_URL}/${name}?pageSize=300`);
    if (res && res.ok) {
      const data = await safeJson(res);
      if (data && data.documents && Array.isArray(data.documents)) {
        return data.documents.map(parseFirestoreDoc);
      }
    }
  } catch (_) {}

  return [];
}

async function sha256(message) {
  const msgBuffer = new TextEncoder().encode(message);
  const hashBuffer = await crypto.subtle.digest('SHA-256', msgBuffer);
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
}
