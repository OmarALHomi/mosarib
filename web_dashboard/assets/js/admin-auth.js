/**
 * Mosarib Admin Portal - Authentication, Session & Security Module
 * Developed for Eng. Omar AL-Homi (TubbaSoft 2026)
 */

async function safeJson(res) {
  if (!res) return null;
  try {
    if (typeof res.text !== 'function') return res;
    const text = await res.text();
    if (!text || !text.trim()) return null;
    return JSON.parse(text);
  } catch (_) {
    return null;
  }
}

function adminApiFetch(url, options = {}) {
  const opts = { ...options };
  opts.credentials = 'same-origin';
  const headers = { ...(opts.headers || {}) };
  if (currentCsrfToken && !headers['X-CSRF-Token']) {
    headers['X-CSRF-Token'] = currentCsrfToken;
  }
  opts.headers = headers;
  return fetch(url, opts);
}

let currentAdminSession = null;
let currentCsrfToken = "";
let currentAdminsList = [];

async function checkServerSession() {
  try {
    const res = await adminApiFetch('api/auth.php?action=check_session');
    const data = (await safeJson(res)) || {};
    if (data.success && data.data && data.data.admin) {
      currentAdminSession = data.data.admin;
      currentCsrfToken = data.data.csrf_token || "";
      sessionStorage.setItem("mosarib_admin_session", JSON.stringify(currentAdminSession));
      unlockDashboardView(currentAdminSession);
      return true;
    } else {
      currentCsrfToken = (data.data && data.data.csrf_token) ? data.data.csrf_token : "";
      lockDashboardView();
      return false;
    }
  } catch (_) {
    fetchCsrfToken();
    lockDashboardView();
    return false;
  }
}

async function fetchCsrfToken() {
  try {
    const res = await fetch('api/auth.php?action=get_csrf', { credentials: 'same-origin' });
    const data = (await safeJson(res)) || {};
    if (data.data && data.data.csrf_token) {
      currentCsrfToken = data.data.csrf_token;
      const el = document.getElementById("loginCsrfToken");
      if (el) el.value = currentCsrfToken;
    }
  } catch (_) {}
}

function lockDashboardView() {
  const overlay = document.getElementById("authScreenOverlay");
  const mainCont = document.getElementById("mainDashboardContainer");
  if (overlay) overlay.style.display = "flex";
  if (mainCont) {
    mainCont.style.filter = "blur(10px)";
    mainCont.style.pointerEvents = "none";
  }
  const tokenEl = document.getElementById("loginCsrfToken");
  if (tokenEl && currentCsrfToken) tokenEl.value = currentCsrfToken;
}

function unlockDashboardView(admin) {
  const overlay = document.getElementById("authScreenOverlay");
  const mainCont = document.getElementById("mainDashboardContainer");
  if (overlay) overlay.style.display = "none";
  if (mainCont) {
    mainCont.style.filter = "none";
    mainCont.style.pointerEvents = "auto";
  }

  const nameEl = document.getElementById("currentAdminName");
  const roleEl = document.getElementById("currentAdminRole");
  if (nameEl) nameEl.innerText = admin.name || admin.email;
  if (roleEl) roleEl.innerText = admin.role || "ADMIN";

  // Auto-load data upon successful secure login
  if (typeof loadFirebaseData === 'function') {
    loadFirebaseData();
  }

  // Restore saved active tab (from URL hash or sessionStorage)
  try {
    const savedTab = (location.hash ? location.hash.replace('#', '') : '') || sessionStorage.getItem('mosarib_active_tab');
    if (savedTab && typeof switchTab === 'function' && ['analytics', 'requests', 'users', 'releases', 'policies', 'push', 'broadcast', 'generator', 'feedbacks', 'admins', 'publicPortal'].includes(savedTab)) {
      switchTab(savedTab);
    }
  } catch (_) {}
}

async function handleSecureLogin(e) {
  e.preventDefault();
  const email = document.getElementById("adminEmailInput").value.trim();
  const password = document.getElementById("adminPasswordInput").value;
  const errorBanner = document.getElementById("authErrorBanner");
  const btn = document.getElementById("loginSubmitBtn");

  errorBanner.style.display = "none";
  btn.disabled = true;
  btn.innerText = "جاري التحقق والتشفير...";

  try {
    const res = await fetch('api/auth.php?action=login', {
      method: 'POST',
      credentials: 'same-origin',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        email: email,
        password: password,
        csrf_token: currentCsrfToken
      })
    });

    const data = (await safeJson(res)) || {};
    if (data.success) {
      currentAdminSession = data.data.admin;
      currentCsrfToken = data.data.csrf_token;
      sessionStorage.setItem("mosarib_admin_session", JSON.stringify(currentAdminSession));
      unlockDashboardView(currentAdminSession);
    } else {
      errorBanner.innerText = `خطأ: ${data.message || 'بيانات الدخول غير صحيحة'}`;
      errorBanner.style.display = "block";
    }
  } catch (err) {
    errorBanner.innerText = `تعذر الاتصال بخادم المصادقة: ${err.message}`;
    errorBanner.style.display = "block";
  } finally {
    btn.disabled = false;
    btn.innerHTML = `<svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path></svg> <span>التحقق والدخول الآمن</span>`;
  }
}

async function handleSecureLogout() {
  if (!confirm("هل أنت متأكد من رغبتك في تسجيل الخروج من لوحة الإدارة؟")) return;
  try {
    await fetch('api/auth.php?action=logout', { credentials: 'same-origin' });
  } catch (_) {}
  sessionStorage.removeItem("mosarib_admin_session");
  sessionStorage.removeItem("mosarib_admin_unlocked");
  currentAdminSession = null;
  location.reload();
}

async function handleCreateAdmin(e) {
  e.preventDefault();
  const name = document.getElementById("newAdminName").value.trim();
  const email = document.getElementById("newAdminEmail").value.trim();
  const password = document.getElementById("newAdminPassword").value;
  const role = document.getElementById("newAdminRole").value;

  try {
    const res = await adminApiFetch('api/auth.php?action=create_admin', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: name,
        email: email,
        password: password,
        role: role,
        csrf_token: currentCsrfToken
      })
    });
    const data = (await safeJson(res)) || {};
    if (data.success) {
      alert(data.message);
      const formEl = document.getElementById("newAdminForm");
      if (formEl) formEl.reset();
      loadAdminsList();
    } else {
      alert("خطأ: " + (data.message || 'تعذر إضافة المسؤول'));
    }
  } catch (err) {
    alert("فشل الإرسال: " + err.message);
  }
}

async function loadAdminsList() {
  const container = document.getElementById("adminsListContainer");
  if (!container) return;
  container.innerHTML = `<div style="text-align: center; color: var(--text-muted); padding: 20px;">جاري تحميل المسؤولين...</div>`;

  try {
    const res = await adminApiFetch('api/auth.php?action=list_admins');
    const data = (await safeJson(res)) || {};
    if (!data.success || !data.data) {
      container.innerHTML = `<div style="text-align: center; color: #EF5350; padding: 20px;">${data.message || 'تعذر التحميل'}</div>`;
      return;
    }

    const admins = data.data;
    currentAdminsList = admins;
    const isSuperAdmin = currentAdminSession && currentAdminSession.role === 'SUPER_ADMIN';

    const esc = (s) => String(s ?? '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;').replace(/'/g,'&#039;');
    container.innerHTML = admins.map(a => {
      const lastLoginStr = a.last_login ? new Date(a.last_login).toLocaleString('en-US') : 'لم يدخل بعد';
      const safeName = esc(a.name || '');
      const safeEmail = esc(a.email || '');
      const safeCreatedBy = esc(a.created_by || '');
      const safeRole = esc(a.role || '');
      return `
        <div style="background: var(--input-bg); border: 1px solid var(--table-border); border-radius: 12px; padding: 14px 18px; margin-bottom: 10px;">
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px;">
            <strong style="color: var(--text-title); font-size: 14px;">${safeName}</strong>
            <span class="badge ${a.role === 'SUPER_ADMIN' ? 'badge-yearly' : 'badge-trial'}">${safeRole}</span>
          </div>
          <div style="font-size: 12px; color: var(--accent-text, #80CBC4); margin-bottom: 6px;">${safeEmail}</div>
          <div style="display:flex; justify-content:space-between; align-items:center;">
            <div style="font-size: 11px; color: var(--text-muted);">آخر دخول: ${lastLoginStr} • أضيف بواسطة: ${safeCreatedBy}</div>
            ${isSuperAdmin && (!currentAdminSession || currentAdminSession.id !== a.id) ? `
              <button type="button" class="btn-sm btn-delete-admin" data-id="${esc(a.id)}" data-name="${safeName}" data-role="${safeRole}" style="background:rgba(239,68,68,0.15); color:#EF4444; border:1px solid rgba(239,68,68,0.35); font-size:11px; padding:3px 8px;">حذف</button>
            ` : ''}
          </div>
        </div>
      `;
    }).join('');

    // Attach click handlers with F1 protections
    container.querySelectorAll('.btn-delete-admin').forEach(btn => {
      btn.addEventListener('click', async function(e) {
        e.preventDefault();
        const aId = this.getAttribute('data-id');
        const aName = this.getAttribute('data-name');
        const aRole = this.getAttribute('data-role');

        const activeSuperAdmins = currentAdminsList.filter(x => x.role === 'SUPER_ADMIN' && x.is_active !== false).length;
        if (aRole === 'SUPER_ADMIN' && activeSuperAdmins <= 1) {
          alert('لا يمكن حذف أو تعطيل المسؤول الرئيسي الوحيد في النظام.');
          return;
        }

        if (!confirm(`هل أنت متأكد من حذف حساب المسؤول "${aName}" نهائياً؟`)) {
          return;
        }

        try {
          const delRes = await adminApiFetch('api/auth.php?action=delete_admin', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ id: aId, csrf_token: currentCsrfToken })
          });
          const delData = (await safeJson(delRes)) || {};
          if (delData.success) {
            alert(delData.message || 'تم حذف المسؤول');
            loadAdminsList();
          } else {
            alert('خطأ: ' + (delData.message || 'تعذر حذف المسؤول'));
          }
        } catch (err) {
          alert('فشل الحذف: ' + err.message);
        }
      });
    });
  } catch (e) {
    container.innerHTML = `<div style="text-align: center; color: #EF5350; padding: 20px;">تعذر تحميل قائمة المسؤولين</div>`;
  }
}

// --- Theme Management ---
function initTheme() {
  try {
    const saved = localStorage.getItem('mosarib_admin_theme') || 'dark';
    applyTheme(saved);
  } catch(e) {
    applyTheme('dark');
  }
}

function applyTheme(theme) {
  const icon = document.getElementById('themeIcon');
  const label = document.getElementById('themeLabel');
  if (theme === 'light') {
    document.documentElement.setAttribute('data-theme', 'light');
    if (label) label.textContent = 'الوضع الليلي';
  } else {
    document.documentElement.setAttribute('data-theme', 'dark');
    if (label) label.textContent = 'الوضع الفاتح';
  }
  try {
    localStorage.setItem('mosarib_admin_theme', theme);
  } catch(e) {}

  if (typeof renderAllRoyalCharts === 'function') {
    try { renderAllRoyalCharts(); } catch(_) {}
  }
}

function toggleTheme() {
  const isLight = document.documentElement.getAttribute('data-theme') === 'light';
  applyTheme(isLight ? 'dark' : 'light');
}
