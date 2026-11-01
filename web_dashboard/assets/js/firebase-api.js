/**
 * Mosarib Admin Portal - Firebase client
 * Every Realtime Database request goes through api/firebase_proxy.php, which checks the admin
 * session and attaches the database secret on the server. The browser never talks to Firebase
 * directly and never holds a secret, so the database rules can deny all public writes.
 */

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

function createJsonResponse(data, status = 200) {
  try {
    return new Response(JSON.stringify(data), {
      status: status,
      headers: { 'Content-Type': 'application/json' }
    });
  } catch (_) {
    return {
      ok: status >= 200 && status < 300,
      status: status,
      json: async () => data,
      text: async () => JSON.stringify(data)
    };
  }
}

async function firebaseFetch(path, options = {}) {
  const method = (options.method || 'GET').toUpperCase();
  const cleanPath = path.replace(/^\/+/, '').replace(/\?.*$/, '');
  const proxyUrl = `api/firebase_proxy.php?path=${encodeURIComponent(cleanPath)}`;
  try {
    const res = await adminApiFetch(proxyUrl, {
      ...options,
      method: method === 'GET' ? 'GET' : 'POST',
      headers: { ...(options.headers || {}), 'X-HTTP-Method-Override': method }
    });
    if (res.ok) {
      updateFirebaseStatusIndicator('CONNECTED');
    } else if (res.status === 401 || res.status === 403 || res.status === 500) {
      updateFirebaseStatusIndicator('AUTH_REQUIRED');
    } else {
      updateFirebaseStatusIndicator('ERROR');
    }
    return res;
  } catch (err) {
    updateFirebaseStatusIndicator('ERROR', err.message);
    return null;
  }
}

function promptConfigureFirebaseUrl() {
  alert(
    "🔐 الاتصال بفايربيس يتم عبر الخادم فقط.\n\n" +
    "مفتاح Database Secret يُضبط في الملف api/config.local.php على الاستضافة (FIREBASE_AUTH_SECRET) ولا يُحفظ في المتصفح.\n\n" +
    "سيتم الآن فحص الاتصال وتحديث البيانات."
  );
  loadFirebaseData();
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
  try {
    const res = await firebaseFetch(`${name}.json`);
    if (res && res.ok) {
      const data = await safeJson(res);
      const items = (data && data.data !== undefined) ? data.data : data;
      if (Array.isArray(items)) return items;
      if (items && typeof items === 'object') {
        return Object.entries(items).map(([k, v]) => (v && typeof v === 'object' ? { ...v, id: v.id || k, firebaseKey: k, _key: k } : v));
      }
    }
  } catch (_) {}

  return [];
}
