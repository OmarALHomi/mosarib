  // Task 036: newest license per device (read only) and the users selected for bulk actions
  let subscriptionByDevice = {};
  let selectedUserKeys = new Set();
  let allUsers = [];
  let allFeedbacks = [];
  let allRequests = [];
  let pendingRequestId = null;
  let adminPaymentMethods = [];
  const apiFetch = (url, opts) => (typeof adminApiFetch === 'function') ? adminApiFetch(url, opts) : fetch(url, { credentials: 'same-origin', ...opts });

  function escapeHtml(str) {
    if (str === null || str === undefined) return '';
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  let adminTestimonialsData = [];
  function ensurePortalStatsAndTestimonialsCard() {
    if (document.getElementById('cardPortalStatsAndTestimonials')) return;
    const form = document.getElementById('portalSettingsForm');
    if (!form) return;
    const grid = form.querySelector('.grid-2');
    if (!grid) return;

    const card = document.createElement('div');
    card.id = 'cardPortalStatsAndTestimonials';
    card.className = 'card';
    card.style.cssText = 'border: 1px solid rgba(0, 188, 212, 0.45); margin-top: 16px; grid-column: 1 / -1;';
    card.innerHTML = `
      <div class="card-title">إحصائيات المنظومة وآراء العملاء في الواجهة العامة</div>
      
      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 16px;">
        <div class="form-group">
          <label for="pStatsWells">عدد الآبار المدارة (اتركه فارغاً لإخفاء شريط الإحصائيات):</label>
          <input type="text" id="pStatsWells" placeholder="مثال: 1240">
        </div>
        <div class="form-group">
          <label for="pStatsVouchers">عدد سندات القبض (اتركه فارغاً لإخفاء شريط الإحصائيات):</label>
          <input type="text" id="pStatsVouchers" placeholder="مثال: 38000">
        </div>
      </div>

      <hr style="border: 0; border-top: 1px solid var(--border); margin: 16px 0;">

      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
        <label style="font-weight: 700; color: var(--text-title); margin: 0;">قائمة آراء وتجارب العملاء (تختفي من الواجهة عند تفريغها):</label>
        <button type="button" class="btn-sm btn-outline" id="btnAddTestimonialBtn" style="padding: 6px 14px;">إضافة رأي جديد</button>
      </div>
      <div id="adminTestimonialsContainer" style="display: flex; flex-direction: column; gap: 10px;"></div>
    `;

    const visCard = form.querySelector(".card[style*='rgba(0, 200, 83']");
    if (visCard) {
      form.insertBefore(card, visCard);
    } else {
      form.appendChild(card);
    }

    const addBtn = document.getElementById('btnAddTestimonialBtn');
    if (addBtn) {
      addBtn.addEventListener('click', () => {
        adminTestimonialsData.push({ name: '', place: '', quote: '' });
        renderAdminTestimonials(adminTestimonialsData);
      });
    }
  }

  function renderAdminTestimonials(list) {
    adminTestimonialsData = Array.isArray(list) ? [...list] : [];
    const container = document.getElementById('adminTestimonialsContainer');
    if (!container) return;
    if (adminTestimonialsData.length === 0) {
      container.innerHTML = '<div style="text-align: center; color: var(--text-muted); padding: 12px; font-size: 13px;">لا توجد آراء عملاء حالياً (القسم مخفي في الواجهة العامة).</div>';
      return;
    }
    container.innerHTML = adminTestimonialsData.map((t, idx) => `
      <div style="background: var(--input-bg); border: 1px solid var(--border); border-radius: 8px; padding: 12px; display: grid; grid-template-columns: 1fr 1fr 2fr auto; gap: 8px; align-items: center;">
        <input type="text" class="t-name" placeholder="الاسم" value="${escapeHtml(t.name || '')}" style="font-size: 13px; padding: 6px 10px;">
        <input type="text" class="t-place" placeholder="المنطقة / الصفة" value="${escapeHtml(t.place || '')}" style="font-size: 13px; padding: 6px 10px;">
        <input type="text" class="t-quote" placeholder="نص الرأي / التجربة" value="${escapeHtml(t.quote || '')}" style="font-size: 13px; padding: 6px 10px;">
        <button type="button" class="btn-sm btn-del-testimonial" data-idx="${idx}" style="background: rgba(239, 68, 68, 0.15); color: #EF4444; border: 1px solid rgba(239, 68, 68, 0.35); padding: 6px 10px;">حذف</button>
      </div>
    `).join('');

    container.querySelectorAll('.btn-del-testimonial').forEach(btn => {
      btn.addEventListener('click', function() {
        const i = parseInt(this.getAttribute('data-idx'), 10);
        adminTestimonialsData.splice(i, 1);
        renderAdminTestimonials(adminTestimonialsData);
      });
    });
  }

  function getAdminTestimonialsList() {
    const container = document.getElementById('adminTestimonialsContainer');
    if (!container) return adminTestimonialsData;
    const rows = container.querySelectorAll("div[style*='grid-template-columns']");
    const result = [];
    rows.forEach(r => {
      const name = r.querySelector('.t-name')?.value.trim() || '';
      const place = r.querySelector('.t-place')?.value.trim() || '';
      const quote = r.querySelector('.t-quote')?.value.trim() || '';
      if (name || quote) {
        result.push({ name, place, quote });
      }
    });
    return result;
  }

  function toggleUpdateInputs() {
    const isUpdate = document.getElementById("bcType").value === "UPDATE";
    document.getElementById("versionGroup").style.display = isUpdate ? "block" : "none";
    if (isUpdate) {
      document.getElementById("bcBtnText").value = "تحميل التحديث الجديد ";
    }
  }

  let allOverrides = {};
  let appConfig = {
    freeOperationsLimit: 200,
    isBackupAllowedForTrial: true,
    isCloudBackupAllowed: true,
    isExcelExportAllowedForTrial: true,
    globalAnnouncementText: ""
  };
  let currentBroadcast = null;
  let currentUserFilter = 'ALL';
  let currentRequestFilter = 'PENDING';

  // Navigation: 5 sections. Every old tab id still works; tabs that are not Home/Requests/Users/Subs live under Settings.
  const ALL_TABS = ['analytics', 'requests', 'users', 'subs', 'releases', 'policies', 'push', 'broadcast', 'generator', 'feedbacks', 'admins', 'publicPortal'];
  const TAB_SECTION = { analytics: 'home', requests: 'requests', users: 'users', subs: 'subs' };

  function switchTab(tabId) {
    if (!ALL_TABS.includes(tabId)) tabId = 'analytics';
    try {
      sessionStorage.setItem('mosarib_active_tab', tabId);
      localStorage.setItem('mosarib_admin_tab', tabId);
      if (window.history && window.history.replaceState) {
        window.history.replaceState(null, null, `#${tabId}`);
      }
    } catch (_) {}

    const section = TAB_SECTION[tabId] || 'settings';
    document.querySelectorAll('[data-section]').forEach(b => {
      const on = b.dataset.section === section;
      b.classList.toggle('active', on);
      if (on) b.setAttribute('aria-current', 'page'); else b.removeAttribute('aria-current');
    });
    document.querySelectorAll('#settingsSubtabs [data-tab]').forEach(b => b.classList.toggle('active', b.dataset.tab === (tabId === 'broadcast' ? 'push' : tabId)));
    const sub = document.getElementById('settingsSubtabs');
    if (sub) sub.style.display = section === 'settings' ? 'flex' : 'none';
    const homeTop = document.getElementById('homeTop');
    if (homeTop) homeTop.style.display = section === 'home' ? 'block' : 'none';

    ALL_TABS.forEach(id => {
      const el = document.getElementById(`tab-${id}`);
      if (el) el.style.display = 'none';
    });
    // Notifications and in-app broadcast share one sub-tab
    const shown = (tabId === 'push' || tabId === 'broadcast') ? ['push', 'broadcast'] : [tabId];
    shown.forEach(id => {
      const el = document.getElementById(`tab-${id}`);
      if (el) el.style.display = 'block';
    });
    closeRowMenu();

    if (tabId === 'releases') {
      loadReleasesList();
    } else if (tabId === 'analytics') {
      renderAllRoyalCharts();
    } else if (tabId === 'admins') {
      loadAdminsList();
    } else if (tabId === 'publicPortal') {
      loadPortalSettingsForAdmin();
    } else if (tabId === 'push' || tabId === 'broadcast') {
      loadFcmEngineStatus();
    } else if (tabId === 'subs') {
      renderSubscriptionsList();
    }
  }

  async function loadPortalSettingsForAdmin() {
    try {
      const res = await apiFetch('api/public_data.php');
      const json = (await safeJson(res)) || {};
      if (json.success && json.data) {
        const ps = json.data.portal_settings || {};
        ensurePortalStatsAndTestimonialsCard();
        if (document.getElementById("pStatsWells")) document.getElementById("pStatsWells").value = ps.stats_wells || '';
        if (document.getElementById("pStatsVouchers")) document.getElementById("pStatsVouchers").value = ps.stats_vouchers || '';
        renderAdminTestimonials(ps.testimonials || []);

        if (document.getElementById("pActiveRegionName")) {
          document.getElementById("pActiveRegionName").value = ps.active_region_name || 'الجمهورية اليمنية (صنعاء، مأرب، الجوف، عمران)';
          document.getElementById("pAvgPrice").value = ps.avg_hourly_price || 7500;
          document.getElementById("pMinPrice").value = ps.min_hourly_price || 5500;
          document.getElementById("pMaxPrice").value = ps.max_hourly_price || 9500;
          document.getElementById("pCurrency").value = ps.currency_symbol || 'ريال';
          document.getElementById("pWaterStatus").value = ps.water_status || 'GOOD';
          document.getElementById("pRatesNote").value = ps.rates_note || '';
          document.getElementById("pHeadlineAlert").value = ps.headline_alert || '';

          document.getElementById("pHeroBadge").value = ps.hero_badge || '';
          document.getElementById("pHeroTitle").value = ps.hero_title || '';
          document.getElementById("pHeroSubtitle").value = ps.hero_subtitle || '';
          document.getElementById("pContactWhatsapp").value = ps.contact_whatsapp || '967773712030';
          document.getElementById("pContactPhone").value = ps.contact_phone || '773712030';
          document.getElementById("pDeveloperName").value = ps.developer_name || 'المهندس عمر الحومي';
          document.getElementById("pDeveloperCompany").value = ps.developer_company || 'تُبع سوفت للحلول البرمجية الذكية (TubbaSoft)';
          document.getElementById("pDeveloperEmail").value = ps.developer_email || 'omar@tubbasoft.com';

          document.getElementById("pShowTopTicker").checked = ps.show_top_ticker !== false;
          document.getElementById("pShowRatesSection").checked = ps.show_rates_section !== false;
          document.getElementById("pShowFeaturesSection").checked = ps.show_features_section !== false;
          if (document.getElementById("pShowHowSection")) document.getElementById("pShowHowSection").checked = ps.show_how_section !== false;
          if (document.getElementById("pShowTestimonialsSection")) document.getElementById("pShowTestimonialsSection").checked = ps.show_testimonials_section !== false;
          document.getElementById("pShowPlansSection").checked = ps.show_plans_section !== false;
          if (document.getElementById("pShowNewsSection")) document.getElementById("pShowNewsSection").checked = ps.show_news_section !== false;
          if (document.getElementById("pShowFaqSection")) document.getElementById("pShowFaqSection").checked = ps.show_faq_section !== false;
          document.getElementById("pShowDeveloperSection").checked = ps.show_developer_section !== false;
          document.getElementById("pShowDownloadCta").checked = ps.show_download_cta !== false;
        }

        const pricing = json.data.subscription_pricing || {};
        const ym = pricing.yearly || {};
        const mm = pricing.monthly || {};
        if (document.getElementById("pricingYearlyPrice")) {
          document.getElementById("pricingYearlyPrice").value = ym.price || '80';
          document.getElementById("pricingYearlyOriginal").value = ym.original_price || '120';
          document.getElementById("pricingYearlyDiscount").value = ym.discount_text || 'وفر 33% (خصم 40$)';
          document.getElementById("pricingYearlyCurrency").value = ym.currency || '$';

          document.getElementById("pricingMonthlyPrice").value = mm.price || '10';
          document.getElementById("pricingMonthlyOriginal").value = mm.original_price || '';
          document.getElementById("pricingMonthlyDiscount").value = mm.discount_text || '';
          document.getElementById("pricingMonthlyCurrency").value = mm.currency || '$';
        }

        const an = json.data.analytics;
        if (an) {
          const totVis = an.total_visitors || 0;
          const todVis = an.today_visitors || 0;
          const totDl = an.total_downloads || 0;
          const todDl = an.today_downloads || 0;

          const elTotVis = document.getElementById("statTotalVisitors");
          const elTodVis = document.getElementById("statTodayVisitorsBadge");
          const elTotDl = document.getElementById("statTotalDownloads");
          const elTodDl = document.getElementById("statTodayDownloadsBadge");
          if (elTotVis) elTotVis.innerText = totVis.toLocaleString('ar-EG');
          if (elTodVis) elTodVis.innerText = `اليوم: ${todVis.toLocaleString('ar-EG')}`;
          if (elTotDl) elTotDl.innerText = totDl.toLocaleString('ar-EG');
          if (elTodDl) elTodDl.innerText = `اليوم: ${todDl.toLocaleString('ar-EG')}`;

          const pTotVis = document.getElementById("portalKpiTotalVisitors");
          const pTodVis = document.getElementById("portalKpiTodayVisitors");
          const pTotDl = document.getElementById("portalKpiTotalDownloads");
          const pTodDl = document.getElementById("portalKpiTodayDownloads");
          if (pTotVis) pTotVis.innerText = totVis.toLocaleString('ar-EG');
          if (pTodVis) pTodVis.innerText = `زوار اليوم: ${todVis.toLocaleString('ar-EG')} زائر فريد`;
          if (pTotDl) pTotDl.innerText = totDl.toLocaleString('ar-EG');
          if (pTodDl) pTodDl.innerText = `تنزيلات اليوم: ${todDl.toLocaleString('ar-EG')} تحميل مباشر`;
        }

        renderAdminPortalNews(json.data.news || []);

        adminPaymentMethods = json.data.payment_methods || [];
        renderAdminPaymentMethods();
      }
    } catch (e) {
      console.error(e);
    }
  }

  function renderAdminPortalNews(news) {
    const cont = document.getElementById("adminPortalNewsList");
    if (!cont) return;
    if (news.length === 0) {
      cont.innerHTML = `<div style="text-align: center; color: var(--text-muted); padding: 14px;">لا توجد أخبار منشورة.</div>`;
      return;
    }

    cont.innerHTML = news.map(item => `
      <div style="background: var(--input-bg); border: 1px solid var(--border); border-radius: 10px; padding: 10px 14px; margin-bottom: 8px; display: flex; justify-content: space-between; align-items: center;">
        <div>
          <b style="font-size: 13.5px; color: #FFF;">${escapeHtml(item.title)}</b>
          <div style="font-size: 11px; color: var(--text-muted);">${escapeHtml(item.category)} • ${new Date(item.published_at).toLocaleDateString('en-US')}</div>
        </div>
        <button class="btn-sm btn-del-news" style="background: rgba(229,57,53,0.2); color: #EF5350; border: 1px solid #EF5350;" data-id="${escapeHtml(item.id)}">حذف</button>
      </div>
    `).join('');

    cont.querySelectorAll('.btn-del-news').forEach(btn => {
      btn.addEventListener('click', function() {
        deletePortalNews(this.getAttribute('data-id'));
      });
    });
  }

  async function handleSavePortalSettings(e) {
    if (e) e.preventDefault();
    const btn = document.getElementById("savePortalSettingsBtn");
    if (btn) btn.disabled = true;

    const payload = {
      action: 'update_settings',
      active_region_name: document.getElementById("pActiveRegionName") ? document.getElementById("pActiveRegionName").value.trim() : '',
      avg_hourly_price: document.getElementById("pAvgPrice") ? (parseFloat(document.getElementById("pAvgPrice").value) || 0) : 0,
      min_hourly_price: document.getElementById("pMinPrice") ? (parseFloat(document.getElementById("pMinPrice").value) || 0) : 0,
      max_hourly_price: document.getElementById("pMaxPrice") ? (parseFloat(document.getElementById("pMaxPrice").value) || 0) : 0,
      currency_symbol: document.getElementById("pCurrency") ? document.getElementById("pCurrency").value.trim() : 'ريال',
      water_status: document.getElementById("pWaterStatus") ? document.getElementById("pWaterStatus").value : 'GOOD',
      rates_note: document.getElementById("pRatesNote") ? document.getElementById("pRatesNote").value.trim() : '',
      headline_alert: document.getElementById("pHeadlineAlert") ? document.getElementById("pHeadlineAlert").value.trim() : '',

      hero_badge: document.getElementById("pHeroBadge") ? document.getElementById("pHeroBadge").value.trim() : '',
      hero_title: document.getElementById("pHeroTitle") ? document.getElementById("pHeroTitle").value.trim() : '',
      hero_subtitle: document.getElementById("pHeroSubtitle") ? document.getElementById("pHeroSubtitle").value.trim() : '',
      contact_whatsapp: document.getElementById("pContactWhatsapp") ? document.getElementById("pContactWhatsapp").value.trim() : '',
      contact_phone: document.getElementById("pContactPhone") ? document.getElementById("pContactPhone").value.trim() : '',
      developer_name: document.getElementById("pDeveloperName") ? document.getElementById("pDeveloperName").value.trim() : '',
      developer_company: document.getElementById("pDeveloperCompany") ? document.getElementById("pDeveloperCompany").value.trim() : '',
      developer_email: document.getElementById("pDeveloperEmail") ? document.getElementById("pDeveloperEmail").value.trim() : '',

      show_top_ticker: document.getElementById("pShowTopTicker") ? document.getElementById("pShowTopTicker").checked : true,
      show_rates_section: document.getElementById("pShowRatesSection") ? document.getElementById("pShowRatesSection").checked : true,
      show_features_section: document.getElementById("pShowFeaturesSection") ? document.getElementById("pShowFeaturesSection").checked : true,
      show_how_section: document.getElementById("pShowHowSection") ? document.getElementById("pShowHowSection").checked : true,
      show_testimonials_section: document.getElementById("pShowTestimonialsSection") ? document.getElementById("pShowTestimonialsSection").checked : true,
      show_plans_section: document.getElementById("pShowPlansSection") ? document.getElementById("pShowPlansSection").checked : true,
      show_news_section: document.getElementById("pShowNewsSection") ? document.getElementById("pShowNewsSection").checked : true,
      show_faq_section: document.getElementById("pShowFaqSection") ? document.getElementById("pShowFaqSection").checked : true,
      show_developer_section: document.getElementById("pShowDeveloperSection") ? document.getElementById("pShowDeveloperSection").checked : true,
      show_download_cta: document.getElementById("pShowDownloadCta") ? document.getElementById("pShowDownloadCta").checked : true,
      stats_wells: document.getElementById("pStatsWells") ? document.getElementById("pStatsWells").value.trim() : '',
      stats_vouchers: document.getElementById("pStatsVouchers") ? document.getElementById("pStatsVouchers").value.trim() : '',
      testimonials: getAdminTestimonialsList(),
      csrf_token: currentCsrfToken
    };

    let fbSuccess = false;
    let fbError = null;

    // 1. Direct Save to Firebase Realtime Database
    try {
      const fbRes = await firebaseFetch('portal_settings.json', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
      if (fbRes && fbRes.ok) fbSuccess = true;
      else fbError = fbRes ? `HTTP ${fbRes.status}` : 'تعذر الاتصال بفايربيس';
    } catch (err) {
      fbError = err.message;
    }

    // 2. Also notify local API in background
    try {
      await apiFetch('api/public_data.php', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
    } catch (_) {}

    if (fbSuccess) {
      alert("تم حفظ ونشر إعدادات ومحتوى البوابة بنجاح في سحابة فايربيس!");
    } else {
      alert(`تم تسجيل التعديل محلياً، ولكن حدث خطأ في سحابة فايربيس: (${fbError || 'تعذر الاتصال'}). تأكد من مفتاح السحابة.`);
    }

    if (btn) btn.disabled = false;
  }

  async function handleCreatePortalNews(e) {
    e.preventDefault();
    const title = document.getElementById("newsTitleInput").value.trim();
    const cat = document.getElementById("newsCategoryInput").value;
    const summary = document.getElementById("newsSummaryInput").value.trim();

    try {
      const res = await apiFetch('api/public_data.php', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          action: 'add_news',
          title: title,
          category: cat,
          summary: summary,
          csrf_token: currentCsrfToken
        })
      });
      const data = (await safeJson(res)) || {};
      if (data.success) {
        alert("" + (data.message || 'تم النشر بنجاح'));
        document.getElementById("portalNewsForm").reset();
        loadPortalSettingsForAdmin();
      } else {
        alert("خطأ: " + (data.message || 'تعذر النشر'));
      }
    } catch (err) {
      alert("فشل النشر: " + err.message);
    }
  }

  async function deletePortalNews(id) {
    if (!confirm("هل أنت متأكد من حذف هذا الخبر من واجهة الموقع العام؟")) return;
    try {
      const res = await apiFetch('api/public_data.php', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          action: 'delete_news',
          id: id,
          csrf_token: currentCsrfToken
        })
      });
      const data = (await safeJson(res)) || {};
      if (data && data.success) {
        alert("تم حذف الخبر بنجاح.");
        loadPortalSettingsForAdmin();
      } else {
        alert("تعذر حذف الخبر: " + (data.message || 'خطأ غير معروف'));
      }
    } catch (err) {
      alert("خطأ: " + err.message);
    }
  }

  async function loadFcmEngineStatus() {
    const badge = document.getElementById("fcmEngineStatusBadge");
    const details = document.getElementById("fcmEngineDetails");
    if (!badge) return;
    try {
      const res = await apiFetch('api/fcm_sender.php');
      const data = (await safeJson(res)) || {};
      if (data.success && data.data) {
        const d = data.data;
        if (d.is_fcm_v1_configured) {
          badge.innerHTML = '🟢 <span style="color:#10B981;">محرك Google FCM HTTP v1 نشط ومفعل</span>';
          details.innerHTML = `المشروع: <b>${d.firebase_project}</b> • الحساب: <b>${d.service_account ? d.service_account.client_email : ''}</b> • الأجهزة المسجلة الجاهزة للاستقبال: <b>${d.registered_tokens_count} جهاز</b> (تصل والتطبيق مغلق ✅)`;
        } else {
          badge.innerHTML = '🟡 <span style="color:var(--accent);">محرك Google FCM HTTP v1 بحاجة لربط مفتاح الخدمة</span>';
          details.innerHTML = `الأجهزة المسجلة في السحابة: <b>${d.registered_tokens_count} جهاز</b>. ليرن هاتف العميل والتطبيق مغلق تماماً، اضغط زر "إعداد / رفع مفتاح Google Service Account" أعلاه.`;
        }
      }
    } catch (_) {}
  }

  function toggleServiceAccountModal() {
    const panel = document.getElementById("serviceAccountPanel");
    if (panel) {
      panel.style.display = (panel.style.display === 'none') ? 'block' : 'none';
    }
  }

  async function saveServiceAccountKey() {
    const fileInput = document.getElementById("saFileInput");
    const jsonInput = document.getElementById("saJsonInput").value.trim();
    const statusDiv = document.getElementById("saSaveStatus");
    statusDiv.style.display = "block";
    statusDiv.style.color = "var(--accent)";
    statusDiv.innerText = "جاري التحقق من المفتاح واختبار اتصاله بـ Google FCM... ⏳";

    let payload = {};
    if (fileInput.files && fileInput.files.length > 0) {
      const file = fileInput.files[0];
      const text = await file.text();
      payload = { action: 'save_service_account', service_account_json: text };
    } else if (jsonInput) {
      payload = { action: 'save_service_account', service_account_json: jsonInput };
    } else {
      statusDiv.style.color = "#EF4444";
      statusDiv.innerText = "يرجى اختيار ملف JSON أو لصق محتواه أولاً";
      return;
    }

    try {
      const res = await apiFetch('api/fcm_sender.php', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
      const data = (await safeJson(res)) || {};
      if (data.success) {
        statusDiv.style.color = "#10B981";
        statusDiv.innerText = "" + data.message;
        loadFcmEngineStatus();
        setTimeout(() => { toggleServiceAccountModal(); }, 2500);
      } else {
        statusDiv.style.color = "#EF4444";
        statusDiv.innerText = "" + (data.message || "فشل التحقق من المفتاح");
      }
    } catch (err) {
      statusDiv.style.color = "#EF4444";
      statusDiv.innerText = "حدث خطأ في الاتصال بالسيرفر";
    }
  }

  async function testFcmConnection() {
    const statusDiv = document.getElementById("pushStatus");
    statusDiv.style.display = "block";
    statusDiv.style.color = "var(--accent)";
    statusDiv.innerText = "جاري فحص الاتصال بـ Google FCM... ⏳";
    try {
      const res = await apiFetch('api/fcm_sender.php', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ action: 'test_connection' })
      });
      const data = (await safeJson(res)) || {};
      if (data.success) {
        statusDiv.style.color = "#10B981";
        statusDiv.innerText = "" + data.message;
      } else {
        statusDiv.style.color = "#EF4444";
        statusDiv.innerText = "" + (data.message || "الاتصال غير جاهز");
      }
    } catch (_) {
      statusDiv.style.color = "#EF4444";
      statusDiv.innerText = "تعذر الاتصال بالخادم";
    }
  }

  async function sendRealFcmPushNotification() {
    const title = document.getElementById("pushTitle").value.trim();
    const body = document.getElementById("pushBody").value.trim();
    const url = document.getElementById("pushUrl").value.trim();
    const target = document.getElementById("pushTarget").value;
    const statusDiv = document.getElementById("pushStatus");
    const btn = document.getElementById("btnSendPush");

    if (!title || !body) {
      alert("يرجى كتابة عنوان الإشعار ونصه أولاً");
      return;
    }
    const audienceText = document.getElementById("pushAudience")?.innerText || "";
    if (!confirm(`إرسال الإشعار الآن؟\n${audienceText}`)) return;

    statusDiv.style.display = "block";
    statusDiv.style.color = "var(--accent)";
    statusDiv.innerText = "جاري إرسال الإشعار السحابي الفوري عبر Google FCM للأجهزة... ⏳";
    if (btn) btn.disabled = true;

    try {
      const res = await apiFetch('api/fcm_sender.php', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          action: 'send',
          title: title,
          body: body,
          action_url: url,
          target: target,
          image_url: (document.getElementById("pushImageUrl")?.value || "").trim(),
          open_in_app: !!document.getElementById("pushOpenInApp")?.checked,
          version_mode: document.getElementById("pushVersionMode")?.value || "",
          version_from: document.getElementById("pushVersionFrom")?.value || "",
          version_to: document.getElementById("pushVersionTo")?.value || ""
        })
      });

      const resData = (await safeJson(res)) || {};
      if (resData.success) {
        statusDiv.style.color = "var(--success)";
        statusDiv.innerHTML = `${resData.message}`;
        loadFcmEngineStatus();
      } else {
        throw new Error(resData.message || 'حدث خطأ في الإرسال');
      }
    } catch (e) {
      statusDiv.style.color = "#EF4444";
      statusDiv.innerText = "" + e.message;
      // Fallback directly to client-side RTDB broadcast (only for "all users": it cannot filter)
      if (target === "ALL") sendCustomPushNotification();
    } finally {
      if (btn) btn.disabled = false;
    }
  }

  async function handleSaveSubscriptionPricing(e) {
    if (e) e.preventDefault();
    const btn = document.getElementById("btnSavePricing");
    const statusDiv = document.getElementById("pricingSaveStatus");
    if (btn) btn.disabled = true;
    if (statusDiv) {
      statusDiv.style.display = "block";
      statusDiv.style.color = "var(--accent)";
      statusDiv.innerText = "جاري حفظ ونشر أسعار وخصومات الاشتراكات في فايربيس... ⏳";
    }

    const payload = {
      action: 'save_subscription_pricing',
      yearly_price: document.getElementById("pricingYearlyPrice").value.trim(),
      yearly_original_price: document.getElementById("pricingYearlyOriginal").value.trim(),
      yearly_discount_text: document.getElementById("pricingYearlyDiscount").value.trim(),
      yearly_currency: document.getElementById("pricingYearlyCurrency").value.trim() || '$',

      monthly_price: document.getElementById("pricingMonthlyPrice").value.trim(),
      monthly_original_price: document.getElementById("pricingMonthlyOriginal").value.trim(),
      monthly_discount_text: document.getElementById("pricingMonthlyDiscount").value.trim(),
      monthly_currency: document.getElementById("pricingMonthlyCurrency").value.trim() || '$'
    };

    const firebasePayload = {
      monthly: {
        price: payload.monthly_price,
        original_price: payload.monthly_original_price,
        discount_text: payload.monthly_discount_text,
        currency: payload.monthly_currency
      },
      yearly: {
        price: payload.yearly_price,
        original_price: payload.yearly_original_price,
        discount_text: payload.yearly_discount_text,
        currency: payload.yearly_currency
      },
      updated_at: Date.now()
    };

    let fbSuccess = false;
    let fbError = null;

    // 1. Direct Save to Firebase Realtime Database
    try {
      const fbRes = await firebaseFetch('subscription_pricing.json', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(firebasePayload)
      });
      if (fbRes && fbRes.ok) {
        fbSuccess = true;
      } else {
        fbError = fbRes ? `HTTP ${fbRes.status}` : 'تعذر الاتصال بفايربيس';
      }
    } catch (err) {
      fbError = err.message;
    }

    // 2. Also notify local API in background
    try {
      await apiFetch('api/public_data.php', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
    } catch (_) {}

    if (fbSuccess) {
      if (statusDiv) {
        statusDiv.style.color = "#10B981";
        statusDiv.innerText = "تم حفظ ونشر أسعار وخصومات الاشتراكات في فايربيس والموقع والتطبيق بنجاح!";
      }
    } else {
      if (statusDiv) {
        statusDiv.style.color = "#EF4444";
        statusDiv.innerText = `خطأ في الحفظ السحابي: ${fbError || 'تعذر الاتصال'}. يرجى التحقق من مفتاح السحابة وقواعد الأمان.`;
      }
    }

    if (btn) btn.disabled = false;
  }

  async function handleApkUpload(e) {
    e.preventDefault();
    const fileInput = document.getElementById("apkFile");
    if (!fileInput.files || fileInput.files.length === 0) {
      alert("يرجى اختيار ملف APK أولاً");
      return;
    }

    const formData = new FormData();
    formData.append('action', 'upload');
    formData.append('apk_file', fileInput.files[0]);
    formData.append('version_name', document.getElementById("apkVersionName").value.trim());
    formData.append('version_code', document.getElementById("apkVersionCode").value);
    formData.append('release_notes', document.getElementById("apkReleaseNotes").value.trim());
    const isForce = document.getElementById("apkForceUpdate").checked;
    formData.append('is_force_update', isForce ? '1' : '0');
    if (isForce) {
      const mode = (document.querySelector('input[name="apkForceMode"]:checked') || {}).value || 'OLDER';
      const codes = parseVersionCodes(document.getElementById("apkForceCodes")?.value || "");
      if (mode === 'SPECIFIC' && !codes.length) return alert("اكتب أرقام الإصدارات المطلوب إجبارها، مثل: 4, 5");
      const newCode = parseInt(document.getElementById("apkVersionCode").value) || 1;
      const locked = countForceLocked(mode, newCode, codes);
      if (!confirm(`سيتم قفل التطبيق على ${locked} مستخدم حتى يحدّثوا.\n\nتنبيه: لا تفعّله إلا بعد أن يحدّث أغلب المستخدمين؛ يمنع الإصدارات الأقدم من العمل.\n\nمتابعة؟`)) return;
      formData.append('force_mode', mode);
      formData.append('force_codes', codes.join(','));
    }
    formData.append('csrf_token', currentCsrfToken);

    const progressDiv = document.getElementById("uploadProgress");
    const btn = document.getElementById("uploadApkBtn");
    btn.disabled = true;
    progressDiv.style.display = "block";

    try {
      const res = await apiFetch('api/release_manager.php', {
        method: 'POST',
        body: formData
      });
      const data = (await safeJson(res)) || {};
      if (data.success) {
        alert("" + (data.message || 'تم رفع ملف التحديث بنجاح') + "\n\nرابط التحميل المباشر:\n" + (data.data ? data.data.download_url : ''));
        document.getElementById("apkUploadForm").reset();
        loadReleasesList();
        loadFirebaseData();
      } else {
        alert("فشل الرفع: " + (data.message || 'تعذر رفع الملف'));
      }
    } catch (err) {
      alert("خطأ أثناء الرفع: " + err.message);
    } finally {
      btn.disabled = false;
      progressDiv.style.display = "none";
    }
  }

  async function setExternalCloudLink() {
    const url = document.getElementById("externalApkUrl").value.trim();
    if (!url) return alert("يرجى إدخال رابط التحميل المباشر");

    const vCode = prompt("رقم الإصدار (VersionCode) الذي يتضمنه هذا الرابط (مثال: 2):", "2");
    if (!vCode) return;
    const vName = prompt("اسم الإصدار (Version Name) مثل 1.4.0:", "1.4.0");
    let isForce = confirm("هل تريد تفعيل هذا الرابط كتحديث إجباري وقفل النسخ القديمة فوراً؟\n\nتحذير: لا تفعّله إلا بعد أن يحدّث أغلب المستخدمين؛ يمنع الإصدارات الأقدم من العمل.");
    let forceMode = 'OLDER', forceCodes = [];
    if (isForce) {
      const raw = prompt("اترك الخانة فارغة لقفل كل الإصدارات الأقدم،\nأو اكتب أرقام إصدارات محددة فقط (مثل: 4, 5):", "");
      if (raw === null) return;
      forceCodes = parseVersionCodes(raw);
      if (forceCodes.length) forceMode = 'SPECIFIC';
      const locked = countForceLocked(forceMode, parseInt(vCode) || 1, forceCodes);
      if (!confirm(`سيتم قفل التطبيق على ${locked} مستخدم حتى يحدّثوا. متابعة؟`)) return;
    }
    const notes = prompt("ملاحظات ورسالة التحديث للمستخدمين:", "تحديث جديد متاح لتطبيق مُسَرِّب");

    const formData = new FormData();
    formData.append('action', 'set_cloud_link');
    formData.append('custom_url', url);
    formData.append('version_code', vCode);
    formData.append('version_name', vName || ('1.' + vCode + '.0'));
    formData.append('is_force_update', isForce ? '1' : '0');
    formData.append('force_mode', forceMode);
    formData.append('force_codes', forceCodes.join(','));
    formData.append('release_notes', notes || '');
    formData.append('csrf_token', currentCsrfToken);

    try {
      const res = await apiFetch('api/release_manager.php', { method: 'POST', body: formData });
      const data = (await safeJson(res)) || {};
      if (data.success) {
        alert("تم ربط وبث الرابط لجميع المستخدمين في فايربيس بنجاح!");
        loadFirebaseData();
      } else {
        alert("فشل: " + (data.message || 'تعذر حفظ الرابط'));
      }
    } catch (err) {
      alert("خطأ: " + err.message);
    }
  }

  async function loadReleasesList() {
    const container = document.getElementById("releasesListContainer");
    if (!container) return;
    try {
      const res = await apiFetch('api/release_manager.php');
      const data = (await safeJson(res)) || {};
      const releases = (data.data && data.data.releases) ? data.data.releases : [];

      if (releases.length === 0) {
        container.innerHTML = `<div style="text-align: center; color: var(--text-muted); padding: 24px;">لا توجد نسخ APK مرفوعة على السيرفر بعد. يمكنك رفع أول نسخة عبر النموذج على اليمين.</div>`;
        return;
      }

      container.innerHTML = releases.map(r => {
        const sizeStr = (r.is_external || !r.file_size) ? 'رابط خارجي مباشر 🔗' : `${(r.file_size / (1024 * 1024)).toFixed(1)} MB`;
        const dateStr = new Date(r.uploaded_at || r.created_at || Date.now()).toLocaleString('ar-EG');
        return `
          <div style="background: var(--input-bg); border: 1px solid var(--border); border-radius: 12px; padding: 14px; margin-bottom: 12px;">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px;">
              <b style="font-size: 15px; color: #FFF;">نسخة: v${r.version_name} (Code: ${r.version_code})</b>
              <span class="badge ${r.is_force_update ? 'badge-blocked' : 'badge-active'}">${r.is_force_update ? 'تحديث إجباري' : 'إصدار اختياري'}</span>
            </div>
            <div style="font-size: 12px; color: var(--text-muted); margin-bottom: 8px;">
              الحجم: ${sizeStr} • تاريخ النشر: ${dateStr}
            </div>
            <div style="font-size: 13px; line-height: 1.6; margin-bottom: 10px; color: var(--text);">
              ${r.release_notes || 'بدون ملاحظات'}
            </div>
            <div style="display: flex; gap: 8px;">
              <input type="text" value="${r.download_url}" readonly style="font-size: 12px; padding: 6px 10px; flex: 1;">
              <button class="btn-sm btn-action" onclick="navigator.clipboard.writeText('${r.download_url}').then(()=>alert('تم نسخ رابط التحميل المباشر: ${r.download_url}'))"> نسخ الرابط</button>
              <button class="btn-sm btn-action" onclick="window.open('${r.download_url}', '_blank')"> تنزيل</button>
            </div>
          </div>
        `;
      }).join('');
    } catch (_) {
      container.innerHTML = `<div style="text-align: center; color: var(--text-muted); padding: 24px;">سجل الإصدارات سيكون متاحاً بمجرد رفع الموقع إلى السيرفر (PHP Backend).</div>`;
    }
  }

  async function sendCustomPushNotification() {
    const title = document.getElementById("pushTitle").value.trim();
    const body = document.getElementById("pushBody").value.trim();
    const url = document.getElementById("pushUrl").value.trim();
    const target = document.getElementById("pushTarget").value;
    const statusDiv = document.getElementById("pushStatus");

    if (!title || !body) {
      alert("يرجى كتابة عنوان الإشعار ونصه أولاً");
      return;
    }

    statusDiv.style.display = "block";
    statusDiv.style.color = "var(--accent)";
    statusDiv.innerText = "جاري جلب أجهزة المستخدمين وإرسال الإشعار...";

    try {
      let tokens = [];
      try {
        const tokenRes = await firebaseFetch('fcm_tokens.json');
        if (tokenRes && tokenRes.ok) {
          const tokenData = await safeJson(tokenRes);
          const tokenObj = (tokenData && tokenData.data !== undefined) ? tokenData.data : tokenData;
          if (tokenObj && !tokenObj.error) {
            tokens = Object.values(tokenObj);
          }
        }
      } catch (_) {}

      const bcRes = await firebaseFetch('app_broadcast.json', {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          id: "PUSH_" + Date.now(),
          title: title,
          message: body,
          actionUrl: url,
          actionText: "فتح الإشعار",
          type: "INFO",
          publishedAt: Date.now()
        })
      });

      if (!bcRes.ok) {
        throw new Error(`خطأ من Firebase (${bcRes.status}): تأكد من إضافة قواعد app_broadcast`);
      }

      statusDiv.style.color = "var(--success)";
      statusDiv.innerHTML = `تم نشر الإشعار والبث بنجاح إلى قاعدة البيانات ومتاح لجميع الأجهزة فور فتح التطبيق! (الأجهزة المسجلة بالتوكن: <b>${tokens.length}</b>)`;
      loadFirebaseData();
    } catch (e) {
      statusDiv.style.color = "var(--danger)";
      statusDiv.innerText = "فشل الإرسال: " + e.message;
    }
  }

  async function publishBroadcast() {
    const title = document.getElementById("bcTitle").value.trim();
    const type = document.getElementById("bcType").value;
    const msg = document.getElementById("bcMessage").value.trim();
    const url = document.getElementById("bcUrl").value.trim();
    const btnText = document.getElementById("bcBtnText").value.trim();
    const minVer = parseInt(document.getElementById("bcMinVersion").value) || 1;

    if (!title) {
      alert("يرجى كتابة عنوان الإعلان أو التحديث أولاً");
      return;
    }

    const payload = {
      id: "BC_" + Date.now(),
      title: title,
      type: type,
      message: msg,
      actionUrl: url,
      actionText: btnText || "عرض التفاصيل",
      minRequiredVersionCode: minVer,
      isForceUpdate: type === "UPDATE",
      publishedAt: Date.now()
    };
    const imageUrl = (document.getElementById("bcImageUrl")?.value || "").trim();
    if (imageUrl) {
      if (!imageUrl.startsWith("https://")) return alert("رابط الصورة يجب أن يبدأ بـ https://");
      payload.imageUrl = imageUrl;
    }

    if (type === "UPDATE") {
      const force = readBroadcastForceAudience();
      if (force.mode === "SPECIFIC") {
        if (!force.codes.length) return alert("اكتب أرقام الإصدارات المطلوب إجبارها، مثل: 4, 5");
        payload.minRequiredVersionCode = 1;
        payload.forceVersionCodes = force.codes;
      }
      const locked = countForceLocked(force.mode, minVer, force.codes);
      if (!confirm(`سيتم قفل التطبيق على ${locked} مستخدم حتى يحدّثوا.\n\nتنبيه: لا تفعّله إلا بعد أن يحدّث أغلب المستخدمين؛ يمنع الإصدارات الأقدم من العمل.\n\nمتابعة؟`)) return;
    } else {
      const range = readVersionRange("bcAudience");
      if (range === false) return alert("حدد رقم الإصدار المستهدف بشكل صحيح");
      if (range) {
        if (range.min > 0) payload.targetMinVersionCode = range.min;
        if (range.max > 0) payload.targetMaxVersionCode = range.max;
      }
      const reach = range ? countUsersInRange(range.min, range.max) : allUsers.length;
      if (!confirm(`سيصل الإعلان إلى ${reach} مستخدم. متابعة؟`)) return;
    }

    try {
      const res = await firebaseFetch('app_broadcast.json', {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
      });
      if (!res.ok) {
        throw new Error(`رفض السيرفر النشر (${res.status}): يرجى التأكد من قواعد app_broadcast`);
      }
      alert(type === "UPDATE" ? "تم تفعيل التحديث الإجباري وقفل النسخ القديمة بنجاح!" : "تم نشر الإشعار لجميع المستخدمين بنجاح!");
      loadFirebaseData();
    } catch (e) {
      alert("فشل النشر: " + e.message);
    }
  }

  async function cancelBroadcast() {
    if (!confirm("هل تريد إلغاء وحذف الإعلان الحالي؟ سيُلغى التحديث الإجباري أيضاً.")) return;
    try {
      // 1. Immediately clear from memory and update UI so user sees it gone without delay
      currentBroadcast = null;
      renderActiveBroadcast();

      // 2. Clear inputs in broadcast form
      const titleInput = document.getElementById("bcTitle");
      if (titleInput) titleInput.value = "";
      const msgInput = document.getElementById("bcMessage");
      if (msgInput) msgInput.value = "";
      const urlInput = document.getElementById("bcUrl");
      if (urlInput) urlInput.value = "";

      // 3. Delete from Firebase via PUT null and DELETE methods (works regardless of proxy/WAF)
      try {
        await firebaseFetch('app_broadcast.json', {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(null)
        });
      } catch (_) {}

      try {
        await firebaseFetch('app_broadcast.json', { method: "DELETE" });
      } catch (_) {}

      // 4. Also call server-side endpoints to ensure removal directly on server
      try {
        await apiFetch('api/fcm_sender.php', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ action: 'cancel_broadcast', csrf_token: currentCsrfToken })
        });
      } catch (_) {}

      try {
        await apiFetch('api/public_data.php', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ action: 'cancel_broadcast', csrf_token: currentCsrfToken })
        });
      } catch (_) {}

      alert("تم حذف وإلغاء الإعلان والتحديث من جميع الهواتف والسحابة بنجاح!");
      loadFirebaseData();
    } catch (e) {
      alert("خطأ أثناء إلغاء الإعلان: " + e.message);
      loadFirebaseData();
    }
  }

  async function saveAppPolicies() {
    const freeLimit = parseInt(document.getElementById("policyFreeLimit").value) || 200;
    const backupAllowed = document.getElementById("policyBackupAllowed").checked;
    const driveBackupAllowed = document.getElementById("policyDriveBackupAllowed").checked;
    const excelExportAllowed = document.getElementById("policyExcelExportAllowed").checked;
    const note = document.getElementById("policyGlobalNote").value.trim();

    const payload = {
      freeOperationsLimit: freeLimit,
      isBackupAllowedForTrial: backupAllowed,
      isCloudBackupAllowed: driveBackupAllowed,
      isExcelExportAllowedForTrial: excelExportAllowed,
      globalAnnouncementText: note,
      updatedAt: Date.now()
    };

    try {
      const res = await firebaseFetch('app_config.json', {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
      });
      if (!res.ok) throw new Error("رفض السيرفر الحفظ (" + res.status + ")");
      appConfig = payload;
      alert(`تم حفظ السياسات بنجاح في السحابة!\n• الحد المجاني: ${freeLimit} عملية\n• النسخ للمجانيين: ${backupAllowed ? 'مسموح' : 'معطل'}\n• نسخ Drive: ${driveBackupAllowed ? 'مسموح' : 'معطل'}\nستطبق الهواتف هذه السياسات تلقائياً عند فتح التطبيق.`);
      loadFirebaseData();
    } catch (e) {
      alert("فشل حفظ السياسات: " + e.message);
    }
  }

  function renderAppPolicies() {
    if (document.getElementById("policyFreeLimit")) {
      document.getElementById("policyFreeLimit").value = appConfig.freeOperationsLimit || 200;
      document.getElementById("policyBackupAllowed").checked = appConfig.isBackupAllowedForTrial !== false;
      document.getElementById("policyDriveBackupAllowed").checked = appConfig.isCloudBackupAllowed !== false;
      document.getElementById("policyExcelExportAllowed").checked = appConfig.isExcelExportAllowedForTrial !== false;
      document.getElementById("policyGlobalNote").value = appConfig.globalAnnouncementText || "";

      const updatedStr = appConfig.updatedAt ? new Date(appConfig.updatedAt).toLocaleString('ar-EG') : 'القيم الافتراضية';
      document.getElementById("policyCloudStatus").innerHTML = `
        حد العمليات: <b>${appConfig.freeOperationsLimit || 200}</b> • النسخ المجاني: <b>${appConfig.isBackupAllowedForTrial !== false ? 'متاح ✅' : 'معطل ❌'}</b><br>
        <small style="color:var(--text-muted);">آخر تحديث للسياسات: ${updatedStr}</small>
      `;
    }
  }

  function renderActiveBroadcast() {
    const card = document.getElementById("currentBroadcastCard");
    const content = document.getElementById("currentBroadcastContent");
    if (!card || !content) return;

    if (!currentBroadcast || !currentBroadcast.title) {
      card.style.display = "none";
      return;
    }

    const isUpdate = currentBroadcast.type === 'UPDATE' || currentBroadcast.isForceUpdate;
    const pubDate = currentBroadcast.publishedAt ? new Date(currentBroadcast.publishedAt).toLocaleString('ar-EG') : '';
    content.innerHTML = `
      <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:6px;">
        <span style="font-weight:bold; font-size:15px; color:${isUpdate ? 'var(--danger)' : 'var(--accent)'};">${escapeHtml(currentBroadcast.title)}</span>
        <span class="badge ${isUpdate ? 'badge-blocked' : 'badge-active'}">${isUpdate ? 'تحديث إجباري وقفل' : 'إعلان توجيهي'}</span>
      </div>
      <div>${escapeHtml(currentBroadcast.message || 'بدون تفاصيل')}</div>
      <div style="font-size:12px; color:var(--text-muted); margin-top:6px;">
        ${currentBroadcast.minRequiredVersionCode ? `الحد الأدنى للإصدار المسموح: <b>vCode ${escapeHtml(currentBroadcast.minRequiredVersionCode)}</b> • ` : ''}
        ${Array.isArray(currentBroadcast.forceVersionCodes) && currentBroadcast.forceVersionCodes.length ? `إجبار الإصدارات: <b>${escapeHtml(currentBroadcast.forceVersionCodes.join(', '))}</b> • ` : ''}
        ${(currentBroadcast.targetMinVersionCode || currentBroadcast.targetMaxVersionCode) ? `مستهدف للإصدارات: <b>${escapeHtml(currentBroadcast.targetMinVersionCode || '…')} – ${escapeHtml(currentBroadcast.targetMaxVersionCode || '…')}</b> • ` : ''}
        تاريخ النشر: ${pubDate}
      </div>
      ${currentBroadcast.imageUrl && String(currentBroadcast.imageUrl).startsWith('https://') ? `<img src="${escapeHtml(currentBroadcast.imageUrl)}" alt="" style="margin-top:8px; max-width:100%; max-height:140px; border-radius:10px;">` : ''}
    `;
    card.style.display = "block";
  }

  async function loadFirebaseData() {
    try {
      const [reqs, users, fbs] = await Promise.all([
        fetchCollection('subscription_requests'),
        fetchCollection('all_users'),
        fetchCollection('feedbacks')
      ]);

      // Cross-reference with licenses to ensure isActivated is 100% accurate even if user node lagged
      let activeLicenseMap = {};
      subscriptionByDevice = {};
      try {
        const licRes = await firebaseFetch('licenses.json');
        if (licRes && licRes.ok) {
          const licData = await safeJson(licRes);
          const licObj = (licData && licData.data !== undefined) ? licData.data : licData;
          if (licObj && typeof licObj === 'object') {
            Object.values(licObj).forEach(lic => {
              rememberSubscription(lic && lic.deviceCode, lic);
              if (lic && lic.deviceCode && !lic.isRevoked && (lic.expiresAt > Date.now())) {
                const cleanCode = lic.deviceCode.replace(/-/g, "").trim();
                activeLicenseMap[cleanCode] = lic;
              }
            });
          }
        }
      } catch (_) {}

      // Cross-reference with devices node as well
      try {
        const devRes = await firebaseFetch('devices.json');
        if (devRes && devRes.ok) {
          const devData = await safeJson(devRes);
          const devObj = (devData && devData.data !== undefined) ? devData.data : devData;
          if (devObj && typeof devObj === 'object') {
            Object.entries(devObj).forEach(([dCode, dVal]) => {
              rememberSubscription(dCode, dVal);
              if (dVal && !dVal.isRevoked && (dVal.expiresAt > Date.now())) {
                const cleanCode = dCode.replace(/-/g, "").trim();
                activeLicenseMap[cleanCode] = dVal;
              }
            });
          }
        }
      } catch (_) {}

      allRequests = reqs || [];
      allUsers = (users || []).map(u => {
        const devKey = u.cleanDeviceKey || (u.deviceCode ? u.deviceCode.replace(/-/g, "").trim() : "");
        const verifiedLic = activeLicenseMap[devKey];
        if (verifiedLic) {
          return {
            ...u,
            isActivated: true,
            plan: verifiedLic.plan || u.plan || 'MONTHLY',
            subscriberName: u.subscriberName || verifiedLic.subscriberName
          };
        }
        return u;
      });
      allFeedbacks = fbs || [];

      // Fetch app_config, device_overrides, app_broadcast
      try {
        const cfgRes = await firebaseFetch('app_config.json');
        if (cfgRes && cfgRes.ok) {
          const cfgData = await safeJson(cfgRes);
          const cfgObj = (cfgData && cfgData.data !== undefined) ? cfgData.data : cfgData;
          if (cfgObj && typeof cfgObj === 'object') appConfig = { ...appConfig, ...cfgObj };
        }
      } catch (_) {}

      try {
        const overRes = await firebaseFetch('device_overrides.json');
        if (overRes && overRes.ok) {
          const overData = await safeJson(overRes);
          const overObj = (overData && overData.data !== undefined) ? overData.data : overData;
          if (overObj && typeof overObj === 'object') allOverrides = overObj;
        }
      } catch (_) {}

      try {
        const bcRes = await firebaseFetch('app_broadcast.json');
        if (bcRes && bcRes.ok) {
          const bcData = await safeJson(bcRes);
          currentBroadcast = (bcData && bcData.data !== undefined) ? bcData.data : bcData;
        } else {
          currentBroadcast = null;
        }
      } catch (_) {
        currentBroadcast = null;
      }
    } catch (e) {
      console.error("Error loading data from Firebase:", e);
    } finally {
      // ALWAYS render tables and stats even if Firebase is unreachable or empty, preventing UI hang
      updateStats();
      loadPortalSettingsForAdmin();
      renderAppPolicies();
      renderActiveBroadcast();
      filterRequests();
      renderUsersTable(allUsers);
      renderFeedbacksTable(allFeedbacks);
      if (typeof refreshTask036Panels === 'function') refreshTask036Panels();
    }
  }

  // Chart Instances Registry
  let chartInstances = {};

  function destroyChart(id) {
    if (chartInstances[id]) {
      chartInstances[id].destroy();
      delete chartInstances[id];
    }
  }

  function renderAllRoyalCharts() {
    if (typeof Chart === 'undefined') return;

    // 1. Calculate Real KPIs
    let totalOps = 0;
    let onlineCount = 0;
    const now = Date.now();
    const oneDayAgo = now - 24 * 60 * 60 * 1000;

    let versionsMap = {};
    let activeSubs = 0;
    let trialCount = 0;
    let blockedCount = 0;

    // Operations buckets for trial users
    let bucket0to50 = 0;
    let bucket51to100 = 0;
    let bucket101to150 = 0;
    let bucket151to200 = 0;
    let bucketOverLimit = 0;

    allUsers.forEach(u => {
      const ops = u.operationsCount || 0;
      totalOps += ops;

      const lastActive = u.lastActiveAt || u.lastSeen || 0;
      if (lastActive >= oneDayAgo) {
        onlineCount++;
      }

      if (u.isBlocked) {
        blockedCount++;
      } else if (u.isActivated) {
        activeSubs++;
      } else {
        trialCount++;
        if (ops <= 50) bucket0to50++;
        else if (ops <= 100) bucket51to100++;
        else if (ops <= 150) bucket101to150++;
        else if (ops <= 200) bucket151to200++;
        else bucketOverLimit++;
      }

      const vName = u.appVersion || 'v1.0.0';
      versionsMap[vName] = (versionsMap[vName] || 0) + 1;
    });

    const kpiOpsEl = document.getElementById("kpiTotalOperations");
    if (kpiOpsEl) kpiOpsEl.innerText = totalOps.toLocaleString('ar-EG');

    const conversionRate = allUsers.length > 0 ? ((activeSubs / allUsers.length) * 100).toFixed(1) : 0;
    const kpiConvEl = document.getElementById("kpiConversionRate");
    if (kpiConvEl) kpiConvEl.innerText = `${conversionRate}%`;

    const kpiOnlineEl = document.getElementById("kpiOnlineUsers");
    if (kpiOnlineEl) kpiOnlineEl.innerText = onlineCount;

    const latestVer = Object.keys(versionsMap).sort().pop() || 'v1.0.0';
    const kpiVerEl = document.getElementById("kpiLatestVersion");
    if (kpiVerEl) kpiVerEl.innerText = latestVer;

    // Chart Font & Theme Colors
    const isDark = document.documentElement.getAttribute('data-theme') !== 'light';
    const textColor = isDark ? '#B2DFDB' : '#37474F';
    const gridColor = isDark ? 'rgba(255, 255, 255, 0.08)' : 'rgba(0, 0, 0, 0.06)';

    // 1. Subscription Distribution Donut Chart
    destroyChart('chartSubscriptionDistribution');
    const ctxSub = document.getElementById('chartSubscriptionDistribution');
    if (ctxSub) {
      chartInstances['chartSubscriptionDistribution'] = new Chart(ctxSub, {
        type: 'doughnut',
        data: {
          labels: ['مشتركون مفعّلون ⭐', 'نسخ تجريبية ⏳', 'محظورون ⛔'],
          datasets: [{
            data: [activeSubs, trialCount, blockedCount],
            backgroundColor: ['#00897B', '#FFB300', '#E53935'],
            borderColor: isDark ? '#12232B' : '#FFFFFF',
            borderWidth: 3,
            hoverOffset: 8
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: {
              position: 'bottom',
              labels: { color: textColor, font: { family: 'Cairo', size: 12 } }
            }
          },
          cutout: '68%'
        }
      });
    }

    // 2. Operations Bucket Bar Chart (Trial Consumption)
    destroyChart('chartOperationsBucket');
    const ctxBucket = document.getElementById('chartOperationsBucket');
    if (ctxBucket) {
      chartInstances['chartOperationsBucket'] = new Chart(ctxBucket, {
        type: 'bar',
        data: {
          labels: ['0-50 عملية', '51-100 عملية', '101-150 عملية', '151-200 عملية', 'تجاوز الحد (+200)'],
          datasets: [{
            label: 'عدد المستخدمين التجريبيين',
            data: [bucket0to50, bucket51to100, bucket101to150, bucket151to200, bucketOverLimit],
            backgroundColor: ['#4DB6AC', '#26A69A', '#FFA726', '#FF7043', '#EF5350'],
            borderRadius: 8
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: { display: false }
          },
          scales: {
            x: {
              ticks: { color: textColor, font: { family: 'Cairo', size: 11 } },
              grid: { display: false }
            },
            y: {
              ticks: { color: textColor, font: { family: 'Cairo' }, stepSize: 1 },
              grid: { color: gridColor }
            }
          }
        }
      });
    }

    // 3. User Registration Timeline (Last 7 Days)
    destroyChart('chartUsersTimeline');
    const ctxTimeline = document.getElementById('chartUsersTimeline');
    if (ctxTimeline) {
      const days = [];
      const dayCounts = [];
      for (let i = 6; i >= 0; i--) {
        const d = new Date(now - i * 24 * 60 * 60 * 1000);
        const dayStr = d.toLocaleDateString('ar-EG', { weekday: 'short', day: 'numeric', month: 'numeric' });
        days.push(dayStr);

        const startOfDay = new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();
        const endOfDay = startOfDay + 24 * 60 * 60 * 1000;
        const count = allUsers.filter(u => u.firstSeen && u.firstSeen >= startOfDay && u.firstSeen < endOfDay).length;
        dayCounts.push(count);
      }

      chartInstances['chartUsersTimeline'] = new Chart(ctxTimeline, {
        type: 'line',
        data: {
          labels: days,
          datasets: [{
            label: 'المستخدمون الجدد',
            data: dayCounts,
            fill: true,
            borderColor: '#4CAF50',
            backgroundColor: 'rgba(76, 175, 80, 0.15)',
            tension: 0.4,
            pointBackgroundColor: '#81C784',
            pointRadius: 5,
            pointHoverRadius: 7
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: { display: false }
          },
          scales: {
            x: {
              ticks: { color: textColor, font: { family: 'Cairo', size: 11 } },
              grid: { display: false }
            },
            y: {
              ticks: { color: textColor, font: { family: 'Cairo' }, stepSize: 1 },
              grid: { color: gridColor }
            }
          }
        }
      });
    }

    // 4. App Versions Polar / Bar
    destroyChart('chartAppVersions');
    const ctxVer = document.getElementById('chartAppVersions');
    if (ctxVer) {
      const vLabels = Object.keys(versionsMap);
      const vValues = Object.values(versionsMap);

      chartInstances['chartAppVersions'] = new Chart(ctxVer, {
        type: 'bar',
        data: {
          labels: vLabels.length > 0 ? vLabels : ['v1.0.0'],
          datasets: [{
            label: 'عدد الهواتف',
            data: vValues.length > 0 ? vValues : [allUsers.length],
            backgroundColor: ['#AB47BC', '#7E57C2', '#5C6BC0', '#42A5F5', '#26C6DA'],
            borderRadius: 8
          }]
        },
        options: {
          indexAxis: 'y',
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: { display: false }
          },
          scales: {
            x: {
              ticks: { color: textColor, font: { family: 'Cairo' }, stepSize: 1 },
              grid: { color: gridColor }
            },
            y: {
              ticks: { color: textColor, font: { family: 'Cairo', size: 12 } },
              grid: { display: false }
            }
          }
        }
      });
    }
  }

  function updateStats() {
    document.getElementById("statTotalUsers").innerText = allUsers.length;
    const activeLic = allUsers.filter(u => u.isActivated && !u.isBlocked).length;
    const trial = allUsers.filter(u => !u.isActivated && !u.isBlocked).length;
    const blocked = allUsers.filter(u => u.isBlocked).length;
    const pendingReqs = allRequests.filter(r => r.status !== 'COMPLETED').length;

    document.getElementById("statActiveLicenses").innerText = activeLic;
    document.getElementById("statTrialUsers").innerText = trial;
    document.getElementById("statBlockedUsers").innerText = blocked;
    document.getElementById("statPendingRequests").innerText = pendingReqs;
    const expEl = document.getElementById("statExpiringSoon");
    if (expEl && typeof expiringSubscriptions === 'function') expEl.innerText = expiringSubscriptions(7).length;

    // Home KPI cards (task 067), computed from the lists already loaded
    const setKpi = (id, n) => { const el = document.getElementById(id); if (el) el.innerText = n; };
    setKpi('kpiHomePending', pendingReqs);
    setKpi('kpiHomeActive', activeLic);
    setKpi('kpiHomeExpiring', typeof expiringSubscriptions === 'function' ? expiringSubscriptions(7).length : 0);
    setKpi('kpiHomeToday', allUsers.filter(isActiveToday).length);

    const badge = document.getElementById("badgeReqCount");
    if (badge) {
      if (pendingReqs > 0) {
        badge.innerText = pendingReqs;
        badge.style.display = "inline-block";
      } else {
        badge.style.display = "none";
      }
    }

    // Update charts if active tab
    const analyticsTab = document.getElementById("tab-analytics");
    if (analyticsTab && analyticsTab.style.display !== 'none') {
      renderAllRoyalCharts();
    }
  }

  function renderRequestsTable(requests) {
    const tbody = document.getElementById("requestsTableBody");
    if (!requests || requests.length === 0) {
      const msg = currentRequestFilter === 'PENDING' 
        ? "لا توجد طلبات جديدة معلقة حالياً ✅" 
        : (currentRequestFilter === 'COMPLETED' ? "لا توجد طلبات منفذة" : "لا توجد طلبات اشتراك مسجلة");
      tbody.innerHTML = `<tr><td colspan="9" style="text-align: center; color: var(--text-muted); padding: 24px;">${msg}</td></tr>`;
      return;
    }

    requests.sort((a,b) => (b.timestamp || 0) - (a.timestamp || 0));

    tbody.innerHTML = requests.map(r => {
      const dateStr = r.timestamp ? new Date(r.timestamp).toLocaleString('en-US') : 'الآن';
      const cleanPhone = (r.phone || '').replace(/[^0-9]/g, '');
      const phoneLink = cleanPhone ? `<a href="https://wa.me/${cleanPhone}" target="_blank" rel="noopener" style="color: var(--success); font-weight: bold;">${escapeHtml(r.phone)}</a>` : '-';
      const planBadge = r.plan === 'YEARLY' ? `<span class="badge badge-yearly">سنوي 80$</span>` : `<span class="badge badge-monthly">شهري 10$</span>`;
      const isCompleted = r.status === 'COMPLETED';
      const statusBadge = isCompleted ? `<span class="badge badge-active">تم التفعيل</span>` : `<span class="badge badge-trial">جديد</span>`;
      const devCode = r.deviceCode || '-';
      const isWeb = r.requestType === 'WEB_DIRECT';
      const sourceBadge = isWeb 
        ? `<span class="badge" style="background:rgba(2, 132, 199, 0.15); color:#0284C7; border:1px solid rgba(2, 132, 199, 0.3);">الموقع</span>` 
        : `<span class="badge badge-active">التطبيق</span>`;
      
      const regionText = r.region || 'غير محدد';
      const receiptText = r.receiptNumber || r.receipt_number || r.receipt || 'قيد التحويل';
      const reqId = r.id || r.requestId || r.firebaseKey || r._key || '';

      return `
        <tr>
          <td><small>${dateStr}</small></td>
          <td><b>${escapeHtml(r.subscriberName || 'مشترك جديد')}</b></td>
          <td>${phoneLink}</td>
          <td>${planBadge}</td>
          <td>
            <div style="font-weight: 700; font-size: 13px; color: #FFF;">${escapeHtml(regionText)}</div>
            <div style="color: var(--accent); font-weight: 800; font-size: 11.5px; margin-top: 3px;">
              سند: <span style="background: rgba(0, 180, 216, 0.15); padding: 2px 6px; border-radius: 4px; border: 1px dashed var(--accent);">${escapeHtml(receiptText)}</span>
            </div>
            ${r.paymentMethod ? `<div style="color: #6EE7B7; font-weight: 700; font-size: 11px; margin-top: 3px;">عبر: <b>${escapeHtml(r.paymentMethod)}</b></div>` : ''}
          </td>
          <td><code style="color: var(--accent); font-weight: bold; font-size: 13px;">${escapeHtml(devCode)}</code></td>
          <td>${sourceBadge}</td>
          <td>${statusBadge}</td>
          <td>
            <div style="display: flex; gap: 6px; align-items: center;">
              ${!isCompleted
                ? (cleanPhone
                    ? `<button class="btn-sm btn-unblock btn-act-req" data-wa="1" data-phone="${cleanPhone}" data-code="${escapeHtml(devCode)}" data-name="${escapeHtml(r.subscriberName || '')}" data-plan="${escapeHtml(r.plan || 'YEARLY')}" data-id="${escapeHtml(reqId)}" title="تفعيل الاشتراك وإرسال الرمز عبر واتساب">تفعيل وإرسال واتساب</button><button class="btn-sm btn-outline btn-act-req" data-code="${escapeHtml(devCode)}" data-name="${escapeHtml(r.subscriberName || '')}" data-plan="${escapeHtml(r.plan || 'YEARLY')}" data-id="${escapeHtml(reqId)}" title="تفعيل بدون إرسال">تفعيل فقط</button>`
                    : `<button class="btn-sm btn-unblock btn-act-req" data-code="${escapeHtml(devCode)}" data-name="${escapeHtml(r.subscriberName || '')}" data-plan="${escapeHtml(r.plan || 'YEARLY')}" data-id="${escapeHtml(reqId)}" title="تفعيل الاشتراك وتوليد الرمز">تفعيل</button>`)
                :`<span style="font-size: 11.5px; color: var(--success); font-weight: bold; padding: 2px 6px;">منفّذ</span>`
              }
              ${cleanPhone ? `<button class="btn-sm btn-outline btn-wa-notify" data-phone="${cleanPhone}" data-name="${escapeHtml(r.subscriberName || '')}" data-receipt="${escapeHtml(receiptText)}" title="تأكيد استلام السند عبر واتساب">واتساب</button>` : ''}
              <button class="btn-sm btn-del-req" style="background: rgba(239, 68, 68, 0.15); color: #EF4444; border: 1px solid rgba(239, 68, 68, 0.35); padding: 4px 8px; font-size: 12px;" data-id="${escapeHtml(reqId)}" title="حذف هذا الطلب">حذف</button>
            </div>
          </td>
        </tr>
      `;
    }).join('');

    tbody.querySelectorAll('.btn-act-req').forEach(btn => {
      btn.addEventListener('click', function() {
        const a = n => this.getAttribute(n);
        if (a('data-wa')) activateRequestAndSend(a('data-code'), a('data-name'), a('data-plan'), a('data-id'), a('data-phone'));
        else quickActivateRequest(a('data-code'), a('data-name'), a('data-plan'), a('data-id'));
      });
    });

    tbody.querySelectorAll('.btn-wa-notify').forEach(btn => {
      btn.addEventListener('click', function() {
        notifySubscriberWhatsApp(this.getAttribute('data-phone'), this.getAttribute('data-name'), this.getAttribute('data-receipt'));
      });
    });

    tbody.querySelectorAll('.btn-del-req').forEach(btn => {
      btn.addEventListener('click', function() {
        deleteSubscriptionRequest(this.getAttribute('data-id'));
      });
    });
  }

  function notifySubscriberWhatsApp(phone, name, receipt) {
    const text = `السلام عليكم ورحمة الله أخي ${name}،\nتم استلام سند التحويل (${receipt}) ومطابقة الإيداع بنجاح في منظومة مُسَرِّب.\nجاري الآن إصدار وتأكيد كود التفعيل الخاص بجهازك.`;
    window.open(`https://wa.me/${phone}?text=${encodeURIComponent(text)}`, '_blank');
  }

  function renderAdminPaymentMethods() {
    const cont = document.getElementById("adminPaymentMethodsList");
    if (!cont) return;
    if (adminPaymentMethods.length === 0) {
      cont.innerHTML = `<div style="text-align: center; color: var(--text-muted); padding: 14px; grid-column: 1 / -1;">لا توجد حسابات مضافة بعد. يمكنك إضافة أول حساب من النموذج أدناه.</div>`;
      return;
    }

    cont.innerHTML = adminPaymentMethods.map((pm, idx) => `
      <div style="background: var(--input-bg); border: 1px solid var(--border); border-radius: 12px; padding: 14px; position: relative;">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
          <b style="font-size: 14px; color: #FFF;">${pm.bank_name || 'بدون اسم'}</b>
          <span class="badge ${pm.account_currency === 'USD' ? 'badge-yearly' : 'badge-active'}">${pm.account_currency || 'YER'}</span>
        </div>
        <div style="font-family: monospace; font-size: 15px; font-weight: 800; color: var(--accent); margin-bottom: 6px; letter-spacing: 0.5px;">
          ${pm.account_number}
        </div>
        <div style="font-size: 12px; color: var(--text-muted); margin-bottom: 4px;">
          باسم: <b>${pm.account_holder || '-'}</b>
        </div>
        ${pm.notes ? `<div style="font-size: 11px; color: #80CBC4; margin-bottom: 8px;">${pm.notes}</div>` : ''}
        <div style="display: flex; justify-content: flex-end; margin-top: 8px;">
          <button type="button" class="btn-sm btn-delete" onclick="deleteAdminPaymentMethod(${idx})">حذف </button>
        </div>
      </div>
    `).join('');
  }

  function addNewPaymentMethod() {
    const bank = document.getElementById("newPayBankName").value.trim();
    const curr = document.getElementById("newPayCurrency").value;
    const num = document.getElementById("newPayAccountNumber").value.trim();
    const holder = document.getElementById("newPayAccountHolder").value.trim();
    const logo = document.getElementById("newPayLogoUrl").value.trim();
    const notes = document.getElementById("newPayNotes").value.trim();

    if (!bank || !num) {
      return alert("يرجى إدخال اسم البنك/المحفظة ورقم الحساب");
    }

    adminPaymentMethods.push({
      id: "PAY_" + Date.now(),
      bank_name: bank,
      account_currency: curr,
      account_number: num,
      account_holder: holder || "عمر الحومي",
      logo_url: logo,
      notes: notes,
      is_active: true
    });

    renderAdminPaymentMethods();

    // Clear inputs
    document.getElementById("newPayBankName").value = "";
    document.getElementById("newPayAccountNumber").value = "";
    document.getElementById("newPayLogoUrl").value = "";
    document.getElementById("newPayNotes").value = "";

    alert("تمت إضافة الحساب إلى القائمة. اضغط على زر 'حفظ ونشر الحسابات في فايربيس' لاعتماده ونشره في السحابة والموقع والتطبيق.");
  }

  function deleteAdminPaymentMethod(idx) {
    if (!confirm("هل أنت متأكد من حذف هذا الحساب؟")) return;
    adminPaymentMethods.splice(idx, 1);
    renderAdminPaymentMethods();
  }

  async function saveAllPaymentMethods() {
    if (!confirm("هل أنت متأكد من حفظ وتحديث حسابات وطرق الدفع واستبدال البيانات السابقة؟")) return;
    let fbSuccess = false;
    let fbError = null;

    // 1. Direct Sync to Firebase RTDB
    try {
      const fbRes = await firebaseFetch('payment_methods.json', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(adminPaymentMethods)
      });
      if (fbRes && fbRes.ok) fbSuccess = true;
      else fbError = fbRes ? `HTTP ${fbRes.status}` : 'تعذر الاتصال بفايربيس';
    } catch (err) {
      fbError = err.message;
    }

    // 2. Also notify local API in background
    try {
      await apiFetch('api/public_data.php', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          action: 'save_payment_methods',
          payment_methods: adminPaymentMethods,
          csrf_token: currentCsrfToken
        })
      });
    } catch (_) {}

    if (fbSuccess) {
      alert("تم حفظ ونشر حسابات الدفع والمحافظ بنجاح في سحابة فايربيس!\nتظهر للعملاء في التطبيق والموقع فوراً.");
    } else {
      alert(`حدث خطأ أثناء الحفظ في فايربيس: (${fbError || 'تعذر الاتصال'}). يرجى التأكد من مفتاح السحابة.`);
    }
  }

  function quickActivateRequest(code, name, plan, reqId) {
    switchTab('generator');
    document.getElementById('deviceCode').value = code;
    document.getElementById('subscriberName').value = name;
    document.getElementById('planSelect').value = plan || 'MONTHLY';
    pendingRequestId = reqId || null;
    generateAndSaveKey();
  }

  async function activateRequestAndSend(code, name, plan, reqId, phone) {
    const r = await activateDevice(code, name, plan, reqId);
    if (!r.ok) return alert(r.error);
    openWhatsAppMessage(phone, activationMessage(name, plan, r.key), `تم التفعيل: ${r.key}`);
    loadFirebaseData();
  }

  function setRequestFilter(filter) {
    currentRequestFilter = filter;
    document.querySelectorAll('#reqFilterPending, #reqFilterCompleted, #reqFilterAll').forEach(b => b.classList.remove('active'));
    if (filter === 'PENDING') document.getElementById('reqFilterPending')?.classList.add('active');
    else if (filter === 'COMPLETED') document.getElementById('reqFilterCompleted')?.classList.add('active');
    else if (filter === 'ALL') document.getElementById('reqFilterAll')?.classList.add('active');
    filterRequests();
  }

  function filterRequests() {
    const q = (document.getElementById("requestSearch")?.value || "").trim().toLowerCase();

    // Update Request Count Badges
    const countPending = allRequests.filter(r => r.status !== 'COMPLETED').length;
    const countCompleted = allRequests.filter(r => r.status === 'COMPLETED').length;
    const countAll = allRequests.length;

    if (document.getElementById("countReqPending")) document.getElementById("countReqPending").innerText = countPending;
    if (document.getElementById("countReqCompleted")) document.getElementById("countReqCompleted").innerText = countCompleted;
    if (document.getElementById("countReqAll")) document.getElementById("countReqAll").innerText = countAll;

    let filtered = allRequests.filter(r => {
      const isCompleted = r.status === 'COMPLETED';
      if (currentRequestFilter === 'PENDING') return !isCompleted;
      if (currentRequestFilter === 'COMPLETED') return isCompleted;
      return true; // ALL
    });

    if (q) {
      filtered = filtered.filter(r => 
        (r.subscriberName && r.subscriberName.toLowerCase().includes(q)) ||
        (r.phone && r.phone.includes(q)) ||
        (r.deviceCode && r.deviceCode.toLowerCase().includes(q)) ||
        (r.receiptNumber && r.receiptNumber.toLowerCase().includes(q)) ||
        (r.region && r.region.toLowerCase().includes(q))
      );
    }

    renderRequestsTable(filtered);
  }

  async function deleteSubscriptionRequest(reqId) {
    if (!reqId) return alert("معرف الطلب غير متوفر لحذفه");
    if (!confirm("هل أنت متأكد من حذف هذا الطلب نهائياً من قاعدة البيانات والسحابة؟")) return;

    const r = allRequests.find(item => item.id === reqId || item.requestId === reqId || item.firebaseKey === reqId || item._key === reqId);
    const keysToDelete = Array.from(new Set([
      reqId,
      r?.firebaseKey,
      r?._key,
      r?.id,
      r?.requestId
    ].filter(k => k && typeof k === 'string' && k.trim().length > 0)));

    let deleteSuccess = false;

    try {
      // 1. Direct delete from Firebase RTDB (via SDK or REST)
      for (const k of keysToDelete) {
        const res = await firebaseFetch(`subscription_requests/${k}.json`, { method: "DELETE" });
        if (res && (res.ok || res.status === 200)) {
          deleteSuccess = true;
        }
      }

      // 2. Also call server-side public_data.php proxy which uses cURL directly on the server to ensure 100% deletion
      try {
        const apiRes = await apiFetch('api/public_data.php', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            action: 'delete_subscription_request',
            request_id: reqId,
            keys: keysToDelete,
            csrf_token: currentCsrfToken
          })
        });
        const apiData = (await safeJson(apiRes)) || {};
        if (apiData && apiData.success) {
          deleteSuccess = true;
        }
      } catch (_) {}

      // 3. Remove from local memory
      allRequests = allRequests.filter(item => {
        const itemId = item.id || item.requestId;
        const itemKey = item.firebaseKey || item._key;
        return !keysToDelete.includes(itemId) && !keysToDelete.includes(itemKey) && itemId !== reqId;
      });

      updateStats();
      filterRequests();

      alert("تم حذف طلب الاشتراك نهائياً من السحابة وقاعدة البيانات بنجاح.");
    } catch (err) {
      alert("خطأ أثناء الحذف: " + err.message);
    }
  }

  async function deleteCompletedRequests() {
    const completed = allRequests.filter(r => r.status === 'COMPLETED');
    if (completed.length === 0) {
      alert("لا توجد طلبات منفذة حالياً لحذفها.");
      return;
    }

    if (!confirm(`هل أنت متأكد من حذف كافة الطلبات المنفذة (${completed.length} طلب) نهائياً من السحابة؟`)) return;

    try {
      const allKeys = [];
      for (const r of completed) {
        const keys = [r.firebaseKey, r._key, r.id, r.requestId].filter(k => k && typeof k === 'string' && k.trim().length > 0);
        keys.forEach(k => allKeys.push(k));
        for (const k of keys) {
          await firebaseFetch(`subscription_requests/${k}.json`, { method: "DELETE" }).catch(() => {});
        }
      }

      try {
        await apiFetch('api/public_data.php', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            action: 'clear_completed_requests',
            keys: Array.from(new Set(allKeys)),
            csrf_token: currentCsrfToken
          })
        });
      } catch (_) {}

      allRequests = allRequests.filter(r => r.status !== 'COMPLETED');
      updateStats();
      filterRequests();
      alert("تم حذف كافة الطلبات المنفذة نهائياً من السحابة بنجاح.");
    } catch (err) {
      alert("خطأ أثناء الحذف: " + err.message);
    }
  }

  function setUserFilter(filter) {
    currentUserFilter = filter;
    document.querySelectorAll('#userFilterChips .pill-filter-btn').forEach(b => b.classList.remove('active'));
    const btn = document.getElementById(`filter${filter.charAt(0) + filter.slice(1).toLowerCase().replace(/_([a-z])/g, (_, l) => l.toUpperCase())}`);
    if (btn) btn.classList.add('active');
    filterUsers();
  }

  function renderUsersTable(users) {
    const tbody = document.getElementById("usersTableBody");
    if (!users || users.length === 0) {
      tbody.innerHTML = emptyRow(7, 'لا يوجد مستخدمون مسجلون بعد', 'سيظهر هنا كل من يفتح التطبيق تلقائياً');
      return;
    }

    const globalLimit = appConfig.freeOperationsLimit || 200;

    // Counters for pill filters
    let countActive = 0, countTrial = 0, countLocked = 0, countBackupLocked = 0, countBlocked = 0, countExpired = 0, countToday = 0;
    users.forEach(u => {
      const isBlocked = u.isBlocked === true;
      const isAct = u.isActivated === true && !isBlocked;
      const devKey = u.cleanDeviceKey || (u.deviceCode ? u.deviceCode.replace(/-/g, "") : "");
      const override = allOverrides[devKey] || {};
      const limit = override.customOperationsLimit || globalLimit;
      const ops = u.operationsCount || 0;
      const isLocked = !isAct && (ops >= limit || u.isLocked);
      const isBkLocked = override.isBackupLocked === true || (!isAct && appConfig.isBackupAllowedForTrial === false);

      const st = userStatus(u, devKey).kind;
      if (st === 'blocked') countBlocked++;
      else if (st === 'active') countActive++;
      else if (st === 'expired') countExpired++;
      else countTrial++;
      if (isActiveToday(u)) countToday++;

      if (isLocked) countLocked++;
      if (isBkLocked) countBackupLocked++;
    });

    if (document.getElementById("countFilterAll")) document.getElementById("countFilterAll").innerText = users.length;
    if (document.getElementById("countFilterActive")) document.getElementById("countFilterActive").innerText = countActive;
    if (document.getElementById("countFilterTrial")) document.getElementById("countFilterTrial").innerText = countTrial;
    if (document.getElementById("countFilterLocked")) document.getElementById("countFilterLocked").innerText = countLocked;
    if (document.getElementById("countFilterBackupLocked")) document.getElementById("countFilterBackupLocked").innerText = countBackupLocked;
    if (document.getElementById("countFilterBlocked")) document.getElementById("countFilterBlocked").innerText = countBlocked;
    if (document.getElementById("countFilterExpired")) document.getElementById("countFilterExpired").innerText = countExpired;
    if (document.getElementById("countFilterToday")) document.getElementById("countFilterToday").innerText = countToday;

    // Apply filter
    let filteredList = users.filter(u => {
      const isBlocked = u.isBlocked === true;
      const isAct = u.isActivated === true && !isBlocked;
      const devKey = u.cleanDeviceKey || (u.deviceCode ? u.deviceCode.replace(/-/g, "") : "");
      const override = allOverrides[devKey] || {};
      const limit = override.customOperationsLimit || globalLimit;
      const ops = u.operationsCount || 0;
      const isLocked = !isAct && (ops >= limit || u.isLocked);
      const isBkLocked = override.isBackupLocked === true || (!isAct && appConfig.isBackupAllowedForTrial === false);

      const stKind = userStatus(u, devKey).kind;
      if (currentUserFilter === 'ACTIVE') return stKind === 'active';
      if (currentUserFilter === 'TRIAL') return stKind === 'trial';
      if (currentUserFilter === 'EXPIRED') return stKind === 'expired';
      if (currentUserFilter === 'TODAY') return isActiveToday(u);
      if (currentUserFilter === 'LOCKED') return isLocked;
      if (currentUserFilter === 'BACKUP_LOCKED') return isBkLocked;
      if (currentUserFilter === 'BLOCKED') return isBlocked;
      return true;
    });

    // Search query
    const q = (document.getElementById("userSearch")?.value || "").trim().toLowerCase();
    if (q) {
      filteredList = filteredList.filter(u => 
        (u.googleEmail && u.googleEmail.toLowerCase().includes(q)) ||
        (u.googleDisplayName && u.googleDisplayName.toLowerCase().includes(q)) ||
        (u.subscriberName && u.subscriberName.toLowerCase().includes(q)) ||
        (u.phone && u.phone.includes(q)) ||
        (u.deviceCode && u.deviceCode.toLowerCase().includes(q)) ||
        (u.deviceModel && u.deviceModel.toLowerCase().includes(q))
      );
    }

    filteredList.sort((a,b) => (b.lastActiveAt || 0) - (a.lastActiveAt || 0));

    if (filteredList.length === 0) {
      tbody.innerHTML = emptyRow(7, 'لا توجد نتائج مطابقة', 'غيّر الفلتر أو عبارة البحث');
      return;
    }

    tbody.innerHTML = filteredList.map(u => {
      const devKey = u.cleanDeviceKey || (u.deviceCode ? u.deviceCode.replace(/-/g, "").trim() : "") || (u.id ? String(u.id).replace(/-/g, "").trim() : "") || (u.firebaseKey ? String(u.firebaseKey).replace(/-/g, "").trim() : "");
      const override = allOverrides[devKey] || {};
      const limit = override.customOperationsLimit || globalLimit;
      const ops = u.operationsCount || 0;
      const isBlocked = u.isBlocked === true;
      const isAct = u.isActivated === true && !isBlocked;
      const isOverLimit = !isAct && ops >= limit;
      const isDeviceBkLocked = override.isBackupLocked === true;

      const st = userStatus(u, devKey);
      const statusBadge = `<span class="chip chip-${st.kind}">${escapeHtml(st.label)}</span>`;

      // Remaining free operations
      const remaining = Math.max(0, limit - ops);
      const pct = Math.min(100, Math.round((ops / limit) * 100));
      const barColor = ops >= limit ? "var(--danger)" : (pct >= 75 ? "var(--accent)" : "var(--primary-light)");
      const customLimitNote = override.customOperationsLimit ? `<small class="cell-sub" style="color:var(--accent-text);">حد خاص: ${override.customOperationsLimit}</small>` : '';
      const opsHtml = isAct
        ? `<div><b>غير محدود</b><small class="cell-sub">${ops} عملية منفّذة</small></div>`
        : `<div><b style="color:${remaining === 0 ? 'var(--danger-text)' : 'inherit'};">${remaining === 0 ? 'مقفل' : remaining}</b> <small class="cell-sub" style="display:inline;">من ${limit}</small>
            <div class="ops-bar-container"><div class="ops-bar-fill" style="width: ${pct}%; background: ${barColor};"></div></div>${customLimitNote}</div>`;

      const lastSeen = relativeTime(u.lastActiveAt);
      const lastSeenFull = u.lastActiveAt ? new Date(u.lastActiveAt).toLocaleString('en-US') : '';
      const userName = u.subscriberName || u.googleDisplayName || 'مستخدم مسرّب';
      const userPhone = u.phone ? `<small class="cell-sub" style="color:var(--success-text);">${escapeHtml(u.phone)}</small>` : '';
      const userMail = u.googleEmail ? `<small class="cell-sub">${escapeHtml(u.googleEmail)}</small>` : '';
      const googleInfo = `<div><b>${escapeHtml(userName)}</b>${userPhone}${userMail}</div>`;
      const bkNote = isDeviceBkLocked ? `<small class="cell-sub" style="color:var(--danger-text);" title="تم قفل النسخ لهذا الجهاز بالاسم">النسخ مقفول</small>` : '';
      const priceNote = (u.pricePerHour || u.hourlyRate) ? `<small class="cell-sub">${Number(u.pricePerHour || u.hourlyRate).toLocaleString('ar-EG')} ريال/س</small>` : '';
      const warningBadge = override.specialWarningMessage ? `<small class="cell-sub" style="color:var(--accent-text);">رسالة موجهة: ${escapeHtml(override.specialWarningMessage)}</small>` : '';

      // One menu: the existing row actions plus activate / extend / revoke (task 065)
      const mi = (label, handler, danger) => `<button type="button" class="menu-item${danger ? ' danger' : ''}" role="menuitem" onclick="${handler}">${label}</button>`;
      const subItems = isAct
        ? mi('تمديد الاشتراك', `openExtendModal('${devKey}')`) + mi('إلغاء التفعيل', `revokeSubscription('${devKey}')`, true)
        : mi('تفعيل اشتراك', `openActivateModal('${devKey}')`);
      const menuHtml = `
        <div class="kebab" onclick="event.stopPropagation()">
          <button type="button" class="kebab-btn" aria-label="إجراءات المستخدم" aria-haspopup="menu" onclick="toggleRowMenu(this)">&#8942;</button>
          <div class="kebab-menu" role="menu">
            ${mi('تفاصيل المستخدم', `openUserDetailModal('${devKey}')`)}
            ${subItems}
            <div class="menu-sep"></div>
            ${isBlocked ? mi('فك الحظر', `toggleBlockUser('${devKey}', '${u.googleEmail || ''}', false)`) : mi('حظر', `toggleBlockUser('${devKey}', '${u.googleEmail || ''}', true)`, true)}
            ${isDeviceBkLocked ? mi('فتح النسخ الاحتياطي', `toggleDeviceBackupLock('${devKey}', false)`) : mi('قفل النسخ الاحتياطي', `toggleDeviceBackupLock('${devKey}', true)`)}
            ${mi('تعديل حد العمليات', `promptSetCustomLimit('${devKey}', ${limit})`)}
            ${mi('إرسال رسالة للمستخدم', `promptSendDeviceWarning('${devKey}')`)}
            <div class="menu-sep"></div>
            ${mi('حذف من القائمة', `softDeleteUsers(['${devKey}'])`, true)}
          </div>
        </div>`;

      return `
        <tr onclick="openUserDetailModal('${devKey}')" title="انقر لعرض الملف الشامل">
          <td class="cell-select" onclick="event.stopPropagation()"><input type="checkbox" class="user-select" style="width:auto;" aria-label="تحديد" value="${escapeHtml(devKey)}" ${selectedUserKeys.has(devKey) ? 'checked' : ''} onchange="toggleUserSelection('${devKey}', this.checked)"></td>
          <td data-label="المستخدم">${googleInfo}</td>
          <td data-label="الجهاز"><code>${escapeHtml(u.deviceCode || devKey)}</code><small class="cell-sub">${escapeHtml(u.deviceModel || '-')}</small>${priceNote}${bkNote}</td>
          <td data-label="الحالة">${statusBadge}${warningBadge}</td>
          <td data-label="العمليات المتبقية">${opsHtml}</td>
          <td data-label="آخر ظهور"><small title="${escapeHtml(lastSeenFull)}">${lastSeen}</small></td>
          <td class="col-actions">${menuHtml}</td>
        </tr>
      `;
    }).join('');
  }

  async function toggleDeviceBackupLock(cleanDeviceKey, lock) {
    const text = lock ? "قفل ميزة النسخ الاحتياطي لهذا الجهاز تحديداً؟" : "إعادة السماح بالنسخ الاحتياطي لهذا الجهاز؟";
    if (!confirm(text)) return;
    try {
      await firebaseFetch(`device_overrides/${cleanDeviceKey}/isBackupLocked.json`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(lock)
      });
      await firebaseFetch(`all_users/${cleanDeviceKey}/isBackupLocked.json`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(lock)
      });
      alert(lock ? "تم قفل النسخ الاحتياطي للجهاز بنجاح." : "تم فك قفل النسخ الاحتياطي بنجاح.");
      loadFirebaseData();
    } catch (e) {
      alert("خطأ: " + e.message);
    }
  }

  async function promptSetCustomLimit(cleanDeviceKey, currentLimit) {
    const res = prompt(`أدخل حد العمليات المخصص لهذا الجهاز (الافتراضي الحالي: ${currentLimit}):\nاكتب 0 لإلغاء الحد المخصص والعودة للحد العام.`, currentLimit);
    if (res === null) return;
    const num = parseInt(res);
    if (isNaN(num) || num < 0) return alert("يرجى إدخال رقم صحيح");

    try {
      if (num === 0) {
        await firebaseFetch(`device_overrides/${cleanDeviceKey}/customOperationsLimit.json`, { method: "DELETE" });
        await firebaseFetch(`all_users/${cleanDeviceKey}/customOperationsLimit.json`, { method: "DELETE" });
        alert("تم إلغاء الحد المخصص والعودة للحد العام.");
      } else {
        await firebaseFetch(`device_overrides/${cleanDeviceKey}/customOperationsLimit.json`, {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(num)
        });
        await firebaseFetch(`all_users/${cleanDeviceKey}/customOperationsLimit.json`, {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(num)
        });
        alert(`تم ضبط حد العمليات لهذا الجهاز إلى ${num} عملية بنجاح!`);
      }
      loadFirebaseData();
    } catch (e) {
      alert("خطأ: " + e.message);
    }
  }

  async function promptSendDeviceWarning(cleanDeviceKey) {
    const currentMsg = allOverrides[cleanDeviceKey]?.specialWarningMessage || "";
    const msg = prompt("اكتب رسالة التنبيه التي ستظهر للمستخدم على شاشته فور فتح التطبيق:\n(اترك الحقل فارغاً لحذف الرسالة الحالية)", currentMsg);
    if (msg === null) return;

    try {
      if (msg.trim().length === 0) {
        await firebaseFetch(`device_overrides/${cleanDeviceKey}/specialWarningMessage.json`, { method: "DELETE" });
        alert("تم حذف رسالة التنبيه الموجهة.");
      } else {
        await firebaseFetch(`device_overrides/${cleanDeviceKey}/specialWarningMessage.json`, {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(msg.trim())
        });
        alert("تم حفظ رسالة التنبيه وستظهر للمستخدم في هاتفه!");
      }
      loadFirebaseData();
    } catch (e) {
      alert("خطأ: " + e.message);
    }
  }

  async function toggleBlockUser(cleanDeviceKey, email, block) {
    const actionText = block ? "إيقاف وحظر التطبيق تماماً على هذا الجهاز أو المستخدم؟" : "إلغاء الحظر والسماح للمستخدم بالعمل؟";
    let reason = "تم إيقاف التطبيق على هذا الجهاز لمخالفة شروط الاستخدام أو محاولة التحايل.";
    if (block) {
      const inputReason = prompt("اكتب سبب الإيقاف الذي سيظهر للمستخدم على شاشته:", reason);
      if (inputReason === null) return;
      if (inputReason.trim()) reason = inputReason.trim();
    } else {
      if (!confirm(`هل أنت متأكد من ${actionText}`)) return;
    }

    try {
      await firebaseFetch(`all_users/${cleanDeviceKey}/isBlocked.json`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(block)
      });
      await firebaseFetch(`all_users/${cleanDeviceKey}/blockReason.json`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(reason)
      });
      await firebaseFetch(`device_overrides/${cleanDeviceKey}/isBlocked.json`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(block)
      });
      await firebaseFetch(`device_overrides/${cleanDeviceKey}/blockReason.json`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(reason)
      });

      // Revoke or un-revoke active licenses for this device in /licenses
      try {
        const licRes = await firebaseFetch('licenses.json');
        if (licRes && licRes.ok) {
          const licData = await safeJson(licRes);
          const licObj = (licData && licData.data !== undefined) ? licData.data : licData;
          if (licObj && typeof licObj === 'object') {
            for (const [lKey, lic] of Object.entries(licObj)) {
              if (lic && lic.deviceCode) {
                const licClean = lic.deviceCode.replace(/-/g, "").trim();
                if (licClean === cleanDeviceKey) {
                  await firebaseFetch(`licenses/${lKey}/isRevoked.json`, {
                    method: "PUT",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(block ? true : false)
                  });
                }
              }
            }
          }
        }
      } catch (_) {}

      if (email && email.includes("@")) {
        const safeEmailKey = email.replace(/\./g, "_").replace(/@/g, "_at_");
        if (block) {
          await firebaseFetch(`blocked_emails/${safeEmailKey}.json`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ email: email, blockedAt: Date.now(), reason: reason })
          });
        } else {
          await firebaseFetch(`blocked_emails/${safeEmailKey}.json`, { method: "DELETE" });
        }
      }

      alert(block ? "تم إيقاف وحظر التطبيق على المستخدم بنجاح." : "تم فك الحظر بنجاح.");
      loadFirebaseData();
    } catch (e) {
      alert("حدث خطأ أثناء تعديل الحالة: " + e.message);
    }
  }

  function filterUsers() {
    const showDeleted = currentUserFilter === 'DELETED';
    const panel = document.getElementById('deletedUsersPanel');
    const table = document.getElementById('usersTableContainer');
    if (panel) panel.style.display = showDeleted ? 'block' : 'none';
    if (table) table.style.display = showDeleted ? 'none' : '';
    if (showDeleted) {
      loadDeletedUsers();
      return;
    }
    renderUsersTable(allUsers);
  }

  function exportUsersCSV() {
    if (allUsers.length === 0) return alert("لا توجد بيانات لتصديرها");
    let csv = "\uFEFFالاسم بجوجل,إيميل جوجل,كود الجهاز,الموديل,النظام,الحالة,الباقة,العمليات,آخر ظهور\n";
    allUsers.forEach(u => {
      const lastSeen = u.lastActiveAt ? new Date(u.lastActiveAt).toLocaleString('ar-EG') : '';
      const status = u.isBlocked ? "موقوف" : (u.isActivated ? "مشترك" : "تجريبي");
      csv += `"${u.googleDisplayName || ''}","${u.googleEmail || ''}","${u.deviceCode || ''}","${u.deviceModel || ''}","${u.androidVersion || ''}","${status}","${u.plan || 'TRIAL'}","${u.operationsCount || 0}","${lastSeen}"\n`;
    });
    downloadCSV(csv, "mosarib_all_users.csv");
  }

  function randomKeyPart(length) {
    const alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    const bytes = new Uint8Array(length);
    crypto.getRandomValues(bytes);
    return Array.from(bytes, b => alphabet[b % alphabet.length]).join("");
  }

  async function generateUniqueActivationKey(prefix) {
    for (let attempt = 0; attempt < 5; attempt++) {
      const key = `ACTV-${prefix}-${randomKeyPart(4)}-${randomKeyPart(4)}-${randomKeyPart(4)}`;
      const res = await firebaseFetch(`licenses/${key.replace(/-/g, "")}.json`);
      if (!res || !res.ok) return null;
      if ((await safeJson(res)) === null) return key;
    }
    return null;
  }

  /** Dashed device code the app reads (MSRB-XXXX-XXXX) from a dashed or cleaned code, or null if invalid. */
  function toDashedDeviceCode(code) {
    const m = /^MSRB([A-Z0-9]{4})([A-Z0-9]{4})$/.exec(String(code || '').replace(/-/g, '').trim().toUpperCase());
    return m ? `MSRB-${m[1]}-${m[2]}` : null;
  }

  /** The one place that issues a key: writes licenses/{key} and devices/{code}, completes the request. */
  async function activateDevice(devCodeRaw, subName, plan, reqId) {
    const devCode = toDashedDeviceCode(devCodeRaw);
    if (!devCode) return { ok: false, error: 'كود الجهاز غير صالح (الصيغة MSRB-XXXX-XXXX)' };
    if (plan !== 'MONTHLY' && plan !== 'YEARLY') return { ok: false, error: 'الباقة غير صالحة' };
    const prefix = plan === 'MONTHLY' ? 'M' : 'Y';
    const durationDays = plan === 'MONTHLY' ? 30 : 365;

    // Random key: it is not derived from the device code, so it cannot be computed outside the
    // dashboard. The app only accepts a key whose record exists here and is bound to its device.
    const key = await generateUniqueActivationKey(prefix);
    if (!key) return { ok: false, error: 'تعذر توليد رمز فريد، تحقق من الاتصال بالخادم ثم أعد المحاولة' };
    const cleanKey = key.replace(/-/g, '');

    const now = Date.now();
    const payload = {
      key: cleanKey,
      displayKey: key,
      deviceCode: devCode,
      subscriberName: subName || 'مشترك',
      plan: plan,
      activatedAt: now,
      expiresAt: now + durationDays * DAY_MS,
      isRevoked: false,
      lastCheckedAt: now
    };
    const put = (path, body) => firebaseFetch(path, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    });

    let error = null;
    try {
      const res = await put(`licenses/${cleanKey}.json`, payload);
      if (!res || !res.ok) error = `HTTP ${res ? res.status : 'تعذر الاتصال'} (${(res && res.statusText) || 'رفض الخادم'})`;
      else {
        const dev = await put(`devices/${devCode}.json`, payload);
        if (!dev || !dev.ok) error = 'حُفظ الترخيص لكن فشل تحديث سجل الجهاز';
      }
      if (!error && reqId) {
        await put(`subscription_requests/${reqId}/status.json`, 'COMPLETED').catch(() => {});
        await put(`subscription_requests/${reqId}/activationKey.json`, key).catch(() => {});
      }
    } catch (e) {
      error = e.message;
    }
    return error ? { ok: false, key, devCode, error } : { ok: true, key, devCode, plan, expiresAt: payload.expiresAt };
  }

  async function generateAndSaveKey() {
    const devCode = document.getElementById("deviceCode").value.trim().toUpperCase();
    const subName = document.getElementById("subscriberName").value.trim();
    const plan = document.getElementById("planSelect").value;

    if (!devCode) {
      alert("يرجى كتابة كود جهاز العميل أولاً");
      return;
    }

    const reqId = pendingRequestId;
    pendingRequestId = null;
    const r = await activateDevice(devCode, subName, plan, reqId);

    if (r.key) {
      document.getElementById("keyText").innerText = r.key;
      document.getElementById("keyBox").style.display = "block";
    }

    if (r.ok) {
      alert(`تم توليد وتفعيل الرمز بنجاح: ${r.key}\nتم رفعه وحفظه في سحابة فايربيس بنجاح وربطه بالجهاز (${r.devCode}).`);
    } else if (r.key) {
      alert(`تم توليد الرمز محلياً: ${r.key}\nولكن حدث خطأ أثناء رفعه إلى فايربيس: (${r.error || 'تعذر الاتصال'}).\nيمكنك النقر على زر سحابة فايربيس بأعلى الصفحة للتأكد من الرابط وقواعد الأمان.`);
    } else {
      alert(r.error);
    }

    loadFirebaseData();
  }

  // ===== Task 065: activate / extend / revoke from the lists =====

  function activationMessage(name, plan, key) {
    const planText = plan === "MONTHLY" ? "اشتراك شهري (30 يوماً)" : "اشتراك سنوي (365 يوماً)";
    return `السلام عليكم ورحمة الله أخي ${name || "عزيزنا المشترك"}،\nتم تفعيل اشتراكك في تطبيق المُسَرِّب (${planText}).\n\nرمز التفعيل لجهازك هو:\n${key}\n\nيرجى نسخه ولصقه في نافذة التفعيل بالتطبيق أثناء الاتصال بالإنترنت.`;
  }

  function showToast(msg, actionLabel, onAction) {
    const t = document.createElement('div');
    t.style.cssText = 'position:fixed; bottom:24px; left:50%; transform:translateX(-50%); z-index:10001; background:var(--card-bg); color:var(--text); border:1px solid var(--card-border); box-shadow:var(--card-shadow); border-radius:12px; padding:12px 16px; display:flex; gap:12px; align-items:center; max-width:90vw;';
    t.innerHTML = `<span>${escapeHtml(msg)}</span>`;
    if (actionLabel) {
      const b = document.createElement('button');
      b.className = 'btn-sm btn-unblock';
      b.textContent = actionLabel;
      b.onclick = () => { onAction(); t.remove(); };
      t.appendChild(b);
    }
    document.body.appendChild(t);
    setTimeout(() => t.remove(), actionLabel ? 15000 : 4000);
  }

  /** Opens WhatsApp at once; if the browser blocks the popup, offers a button (a real click is allowed). */
  function openWhatsAppMessage(phone, text, doneMsg) {
    const num = whatsappNumber(phone);
    if (!num) return showToast(doneMsg);
    const url = `https://wa.me/${num}?text=${encodeURIComponent(text)}`;
    if (!window.open(url, '_blank')) showToast(doneMsg, 'إرسال واتساب', () => window.open(url, '_blank'));
  }

  /** Plan picker modal. onConfirm(plan, name) returns a promise; the dialog closes when it finishes. */
  function showPlanModal(title, name, withName, confirmLabel, onConfirm) {
    const ov = document.createElement('div');
    ov.style.cssText = 'position:fixed; inset:0; z-index:10000; background:rgba(0,0,0,0.6); display:flex; align-items:center; justify-content:center; padding:16px;';
    ov.innerHTML = `<div style="background:var(--card-bg); color:var(--text); border:1px solid var(--card-border); border-radius:16px; padding:20px; width:100%; max-width:360px; box-shadow:var(--card-shadow);">
      <h3 style="margin:0 0 14px;">${escapeHtml(title)}</h3>
      ${withName ? `<label style="font-size:12px; color:var(--text-muted);">اسم المشترك</label><input id="planModalName" type="text" value="${escapeHtml(name || '')}" style="margin-bottom:10px;">` : ''}
      <label style="font-size:12px; color:var(--text-muted);">الباقة</label>
      <select id="planModalPlan" style="margin-bottom:16px;"><option value="MONTHLY">شهري (30 يوماً)</option><option value="YEARLY">سنوي (365 يوماً)</option></select>
      <div style="display:flex; gap:8px; justify-content:flex-end;">
        <button type="button" class="btn-sm btn-outline" id="planModalCancel">إلغاء</button>
        <button type="button" class="btn-sm btn-unblock" id="planModalOk">${escapeHtml(confirmLabel)}</button>
      </div></div>`;
    document.body.appendChild(ov);
    ov.querySelector('#planModalCancel').onclick = () => ov.remove();
    const ok = ov.querySelector('#planModalOk');
    ok.onclick = async () => {
      ok.disabled = true;
      const nm = ov.querySelector('#planModalName');
      try { await onConfirm(ov.querySelector('#planModalPlan').value, nm ? nm.value.trim() : ''); }
      finally { ov.remove(); }
    };
  }

  /** Reflects a change in the local lists, then redraws the users table and counters (no reload). */
  function applyLocalSubscription(devKey, patch) {
    const k = String(devKey).toUpperCase();
    subscriptionByDevice[k] = { ...(subscriptionByDevice[k] || { activatedAt: Date.now() }), ...patch };
    const u = userByKey(k);
    if (u) {
      if (patch.isRevoked === true) u.isActivated = false;
      else { u.isActivated = true; if (patch.plan) u.plan = patch.plan; }
    }
    renderUsersTable(allUsers);
    updateStats();
    if (typeof refreshTask036Panels === 'function') refreshTask036Panels();
  }

  function openActivateModal(devKey) {
    const u = userByKey(devKey);
    if (!u) return alert('تعذر العثور على المستخدم');
    const code = toDashedDeviceCode(u.deviceCode || devKey);
    if (!code) return alert('كود الجهاز غير صالح (الصيغة MSRB-XXXX-XXXX)');
    const name = u.subscriberName || u.googleDisplayName || '';
    showPlanModal('تفعيل اشتراك', name, true, 'تفعيل', async (plan, nm) => {
      const r = await activateDevice(code, nm, plan, null);
      if (!r.ok) return alert(r.error);
      applyLocalSubscription(devKey, { key: r.key.replace(/-/g, ''), plan, activatedAt: Date.now(), expiresAt: r.expiresAt, isRevoked: false });
      if (u.phone) openWhatsAppMessage(u.phone, activationMessage(nm, plan, r.key), `تم التفعيل: ${r.key}`);
      else showToast(`تم التفعيل: ${r.key}`);
    });
  }

  function openExtendModal(devKey) {
    const sub = subscriptionByDevice[String(devKey).toUpperCase()];
    const code = toDashedDeviceCode(devKey);
    if (!sub || !code) return alert('تعذر العثور على بيانات الاشتراك لهذا الجهاز');
    showPlanModal('تمديد الاشتراك', '', false, 'تمديد', async (plan) => {
      const expiresAt = Math.max(Date.now(), sub.expiresAt) + (plan === 'MONTHLY' ? 30 : 365) * DAY_MS;
      const r = await writeSubscriptionPatch(sub.key, code, { expiresAt, plan, lastCheckedAt: Date.now() });
      if (!r.ok) return alert(r.error);
      applyLocalSubscription(devKey, { expiresAt, plan });
      showToast(`تم التمديد حتى ${formatDateDMY(expiresAt)}`);
    });
  }

  async function revokeSubscription(devKey) {
    const sub = subscriptionByDevice[String(devKey).toUpperCase()];
    const code = toDashedDeviceCode(devKey);
    if (!sub || !code) return alert('تعذر العثور على بيانات الاشتراك لهذا الجهاز');
    if (!confirm('إلغاء تفعيل اشتراك هذا الجهاز؟ سيعود إلى النسخة التجريبية.')) return;
    const r = await writeSubscriptionPatch(sub.key, code, { isRevoked: true, lastCheckedAt: Date.now() });
    if (!r.ok) return alert(r.error);
    applyLocalSubscription(devKey, { isRevoked: true });
    showToast('تم إلغاء التفعيل');
  }

  /** PATCHes the same fields on licenses/{key} (when known) and devices/{code}. */
  async function writeSubscriptionPatch(cleanKey, devCode, fields) {
    const paths = [`devices/${devCode}.json`];
    if (cleanKey) paths.unshift(`licenses/${cleanKey}.json`);
    for (const path of paths) {
      const res = await firebaseFetch(path, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(fields)
      });
      if (!res || !res.ok) return { ok: false, error: `فشل الحفظ في ${path.split('/')[0]}: HTTP ${res ? res.status : 'تعذر الاتصال'}` };
    }
    return { ok: true };
  }

  function copyKey() {
    const key = document.getElementById("keyText").innerText;
    navigator.clipboard.writeText(key).then(() => alert("تم نسخ الكود بنجاح: " + key));
  }

  function sendToWhatsApp() {
    const key = document.getElementById("keyText").innerText;
    const text = activationMessage(document.getElementById("subscriberName").value.trim(), document.getElementById("planSelect").value, key);
    window.open(`https://wa.me/?text=${encodeURIComponent(text)}`, '_blank');
  }

  function renderFeedbacksTable(feedbacks) {
    const tbody = document.getElementById("feedbacksTableBody");
    if (!feedbacks || feedbacks.length === 0) {
      tbody.innerHTML = `<tr><td colspan="8" style="text-align: center; color: var(--text-muted);">لا توجد رسائل ملاحظات واردة بعد</td></tr>`;
      return;
    }

    feedbacks.sort((a,b) => (b.timestamp || 0) - (a.timestamp || 0));

    tbody.innerHTML = feedbacks.map(f => {
      const dateStr = new Date(f.timestamp || Date.now()).toLocaleString('en-US');
      const stars = "★".repeat(f.rating || 5) + "☆".repeat(5 - (f.rating || 5));
      const cleanPhone = (f.phone || '').replace(/[^0-9]/g, '');
      const phoneLink = cleanPhone ? `<a href="https://wa.me/${cleanPhone}" target="_blank" rel="noopener" style="color:var(--success); font-weight:bold;">${escapeHtml(f.phone)}</a>` : '-';

      return `
        <tr>
          <td><small>${dateStr}</small></td>
          <td><b>${escapeHtml(f.subscriberName || 'مستخدم')}</b></td>
          <td>${phoneLink}</td>
          <td><span class="rating-stars">${stars}</span></td>
          <td><span class="badge badge-monthly">${escapeHtml(f.category || 'اقتراح')}</span></td>
          <td style="max-width: 320px; line-height: 1.6;">${escapeHtml(f.message)}</td>
          <td><small>${escapeHtml(f.deviceModel || f.deviceCode || '-')}</small></td>
          <td>
            ${cleanPhone ? `<button class="btn-sm btn-unblock btn-fb-wa" data-phone="${cleanPhone}">واتساب</button>` : '-'}
          </td>
        </tr>
      `;
    }).join('');

    tbody.querySelectorAll('.btn-fb-wa').forEach(btn => {
      btn.addEventListener('click', function() {
        window.open('https://wa.me/' + this.getAttribute('data-phone'), '_blank', 'noopener');
      });
    });
  }

  function filterFeedbacks() {
    const q = document.getElementById("feedbackSearch").value.trim().toLowerCase();
    const filtered = allFeedbacks.filter(f => 
      (f.subscriberName && f.subscriberName.toLowerCase().includes(q)) ||
      (f.message && f.message.toLowerCase().includes(q)) ||
      (f.phone && f.phone.includes(q))
    );
    renderFeedbacksTable(filtered);
  }

  function exportRequestsCSV() {
    if (allRequests.length === 0) return alert("لا توجد طلبات لتصديرها");
    let csv = "\uFEFFالتاريخ,الاسم,الهاتف,الباقة,المنطقة,رقم السند,طريقة الدفع,كود الجهاز,المصدر,الحالة\n";
    allRequests.forEach(r => {
      const dateStr = r.timestamp ? new Date(r.timestamp).toLocaleString('ar-EG') : '';
      csv += `"${dateStr}","${r.subscriberName || ''}","${r.phone || ''}","${r.plan || ''}","${r.region || ''}","${r.receiptNumber || ''}","${r.paymentMethod || ''}","${r.deviceCode || ''}","${r.requestType || ''}","${r.status || ''}"\n`;
    });
    downloadCSV(csv, "mosarib_subscription_requests.csv");
  }

  function exportFeedbacksCSV() {
    if (allFeedbacks.length === 0) return alert("لا توجد رسائل لتصديرها");
    let csv = "\uFEFFالتاريخ,الاسم,الهاتف,التقييم,النوع,الرسالة,الجهاز\n";
    allFeedbacks.forEach(f => {
      const dateStr = new Date(f.timestamp || Date.now()).toLocaleString('ar-EG');
      csv += `"${dateStr}","${f.subscriberName || ''}","${f.phone || ''}","${f.rating || 5}","${f.category || ''}","${(f.message || '').replace(/"/g, '""')}","${f.deviceModel || ''}"\n`;
    });
    downloadCSV(csv, "mosarib_feedbacks.csv");
  }

  function downloadCSV(csvContent, fileName) {
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement("a");
    link.href = URL.createObjectURL(blob);
    link.setAttribute("download", fileName);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }


  // --- User Detail Modal & Targeted Messages Manager ---
  let activeModalUserKey = null;

  async function openUserDetailModal(cleanDeviceKey) {
    const cleanKey = String(cleanDeviceKey || '').replace(/-/g, "").trim();
    activeModalUserKey = cleanKey;
    const overlay = document.getElementById("userModalOverlay");
    if (!overlay) return;

    // Find user robustly across all potential identifiers
    const u = allUsers.find(item => {
      const devKey = item.cleanDeviceKey || (item.deviceCode ? item.deviceCode.replace(/-/g, "").trim() : "") || item.id || item.firebaseKey || item._key || "";
      const devCodeClean = (item.deviceCode || '').replace(/-/g, "").trim();
      return devKey === cleanKey || devCodeClean === cleanKey || item.id === cleanKey || item.firebaseKey === cleanKey || item._key === cleanKey;
    }) || { cleanDeviceKey: cleanKey, deviceCode: cleanKey };

    const override = allOverrides[cleanKey] || {};
    const globalOpsLimit = (appConfig && appConfig.freeOperationsLimit) ? appConfig.freeOperationsLimit : 200;
    const limit = override.customOperationsLimit || globalOpsLimit;
    const ops = u.operationsCount || 0;
    const isBlocked = u.isBlocked === true;
    const isAct = u.isActivated === true && !isBlocked;
    const isDeviceBkLocked = override.isBackupLocked === true;
    const userName = u.subscriberName || u.googleDisplayName || 'مستخدم مسرّب';

    // Set header
    document.getElementById("modalUserName").innerText = userName;
    document.getElementById("modalUserAvatar").innerText = isBlocked ? '⛔' : (isAct ? '⭐' : '👤');
    
    let statusBadge = isBlocked
      ? `<span class="badge badge-blocked">محظور</span>`
      : (isAct ? `<span class="badge badge-active">مشترك مفعّل</span>` : `<span class="badge badge-trial">⏳ تجريبي مجاني</span>`);
    document.getElementById("modalUserStatusBadge").innerHTML = statusBadge;

    // Populate Info Pills
    document.getElementById("modalDeviceCode").innerHTML = `<code>${u.deviceCode || cleanKey}</code>`;
    document.getElementById("modalDeviceModel").innerText = u.deviceModel || 'غير محدد';
    document.getElementById("modalAndroidVersion").innerText = u.androidVersion || 'Android';
    document.getElementById("modalUserPhone").innerHTML = u.phone 
      ? `<b style="color:#4ADE80;">${u.phone}</b>` 
      : '<span style="color:var(--text-muted);">غير مسجل</span>';
    document.getElementById("modalUserEmail").innerHTML = u.googleEmail 
      ? `<span style="color:#38BDF8;">${u.googleEmail}</span>` 
      : '<span style="color:var(--text-muted);">بدون بريد Google</span>';
    
    const price = (u.pricePerHour || u.hourlyRate);
    document.getElementById("modalUserPrice").innerText = price ? `${Number(price).toLocaleString('ar-EG')} ريال/س` : 'الافتراضي (5000 ريال/س)';
    document.getElementById("modalUserPlan").innerHTML = escapeHtml(u.plan === "YEARLY" ? "سنوي ⭐" : (u.plan === "MONTHLY" ? "شهري" : "تجربة مجانية")) + subscriptionHtml(cleanKey);
    document.getElementById("modalUserLastActive").innerText = u.lastActiveAt ? new Date(u.lastActiveAt).toLocaleString('ar-EG') : (u.lastSeen ? new Date(u.lastSeen).toLocaleString('ar-EG') : 'غير محدد');
    document.getElementById("modalUserOps").innerText = isAct ? `${ops} عملية (غير محدود - مشترك)` : `${ops} / ${limit} عملية`;
    document.getElementById("modalUserBackupStatus").innerHTML = isDeviceBkLocked 
      ? `<span style="color:var(--danger); font-weight:bold;">🚫 مقفول للجهاز</span>`
      : `<span style="color:var(--success); font-weight:bold;">متاح</span>`;

    // Action buttons in modal
    const waBtn = document.getElementById("modalBtnWhatsApp");
    if (u.phone && u.phone.trim().length > 5) {
      waBtn.style.display = "inline-flex";
      const cleanPhone = u.phone.replace(/[^0-9]/g, '');
      const waLink = cleanPhone.startsWith('967') ? cleanPhone : '967' + cleanPhone.replace(/^0+/, '');
      waBtn.onclick = () => window.open(`https://wa.me/${waLink}`, '_blank');
    } else {
      waBtn.style.display = "none";
    }

    const blockBtn = document.getElementById("modalBtnToggleBlock");
    blockBtn.innerText = isBlocked ? "فك الحظر " : "حظر المستخدم ⛔";
    blockBtn.onclick = async () => {
      await toggleBlockUser(cleanKey, u.googleEmail || '', !isBlocked);
      openUserDetailModal(cleanKey);
    };

    const bkBtn = document.getElementById("modalBtnToggleBackup");
    bkBtn.innerText = isDeviceBkLocked ? "فتح النسخ " : "قفل النسخ ";
    bkBtn.onclick = async () => {
      await toggleDeviceBackupLock(cleanKey, !isDeviceBkLocked);
      openUserDetailModal(cleanKey);
    };

    const limitBtn = document.getElementById("modalBtnSetLimit");
    limitBtn.onclick = async () => {
      await promptSetCustomLimit(cleanKey, limit);
      openUserDetailModal(cleanKey);
    };

    // Load targeted messages
    loadUserMessagesList(cleanKey, override);

    // Reset Push Form
    document.getElementById("userPushTitle").value = "";
    document.getElementById("userPushBody").value = "";
    document.getElementById("userPushStatus").style.display = "none";

    switchUserModalTab('profile');
    overlay.classList.add("active");
  }

  function closeUserDetailModal(e) {
    if (e && e.target !== e.currentTarget && !e.target.classList.contains("modal-close-btn")) return;
    const overlay = document.getElementById("userModalOverlay");
    if (overlay) overlay.classList.remove("active");
    activeModalUserKey = null;
  }

  // Keyboard shortcut: Close user modal with Escape key
  document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape' || e.key === 'Esc') {
      closeUserDetailModal();
    }
  });

  function switchUserModalTab(tabName) {
    ['profile', 'messages', 'push'].forEach(t => {
      const el = document.getElementById(`modalUserTab-${t}`);
      const btn = document.getElementById(`modalTabBtn${t.charAt(0).toUpperCase() + t.slice(1)}`);
      if (el) el.style.display = (t === tabName) ? "block" : "none";
      if (btn) btn.classList.toggle("active", t === tabName);
    });
  }

  let currentUserLoadedMessages = {};

  function escapeHtml(str) {
    if (str === null || str === undefined) return '';
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  function startEditUserMessage(cleanDeviceKey, msgId) {
    const m = currentUserLoadedMessages[msgId];
    if (!m) return;
    const editIdEl = document.getElementById("editingMsgId");
    if (editIdEl) editIdEl.value = msgId;

    const titleEl = document.getElementById("newMsgTitle");
    if (titleEl) titleEl.value = m.title || '';
    const textEl = document.getElementById("newMsgText");
    if (textEl) textEl.value = m.text || '';
    const typeEl = document.getElementById("newMsgType");
    if (typeEl) typeEl.value = m.type || 'warning';
    const dispEl = document.getElementById("newMsgDisplays");
    if (dispEl) dispEl.value = (m.displays !== undefined && m.displays !== null) ? String(m.displays) : '0';

    const headingEl = document.getElementById("userMsgFormHeading");
    if (headingEl) headingEl.innerText = "تعديل نص وعنوان التنبيه الموجه لهذا الجهاز:";
    const iconEl = document.getElementById("userMsgFormIcon");
    if (iconEl) iconEl.innerText = "";
    const cardEl = document.getElementById("userMsgFormCard");
    if (cardEl) {
      cardEl.style.borderColor = "var(--accent)";
      cardEl.style.background = "rgba(217, 119, 6, 0.06)";
    }
    const saveBtn = document.getElementById("btnSaveUserMsg");
    if (saveBtn) {
      saveBtn.innerText = "💾 حفظ التعديل وتحديث الجهاز";
      saveBtn.className = "btn btn-secondary";
    }
    const cancelBtn = document.getElementById("btnCancelEditMsg");
    if (cancelBtn) cancelBtn.style.display = "inline-flex";

    if (textEl) {
      textEl.focus();
      textEl.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
  }

  function cancelEditUserMessage() {
    const editIdEl = document.getElementById("editingMsgId");
    if (editIdEl) editIdEl.value = "";
    const titleEl = document.getElementById("newMsgTitle");
    if (titleEl) titleEl.value = "";
    const textEl = document.getElementById("newMsgText");
    if (textEl) textEl.value = "";
    const typeEl = document.getElementById("newMsgType");
    if (typeEl) typeEl.value = "warning";
    const dispEl = document.getElementById("newMsgDisplays");
    if (dispEl) dispEl.value = "0";

    const headingEl = document.getElementById("userMsgFormHeading");
    if (headingEl) headingEl.innerText = "إضافة رسالة أو تنبيه موجه لهذا الجهاز";
    const iconEl = document.getElementById("userMsgFormIcon");
    if (iconEl) iconEl.innerText = "➕";
    const cardEl = document.getElementById("userMsgFormCard");
    if (cardEl) {
      cardEl.style.borderColor = "var(--primary)";
      cardEl.style.background = "rgba(0, 131, 143, 0.03)";
    }
    const saveBtn = document.getElementById("btnSaveUserMsg");
    if (saveBtn) {
      saveBtn.innerText = "💾 حفظ وإرسال للجهاز";
      saveBtn.className = "btn";
    }
    const cancelBtn = document.getElementById("btnCancelEditMsg");
    if (cancelBtn) cancelBtn.style.display = "none";
  }

  async function loadUserMessagesList(cleanDeviceKey, override = {}) {
    const tbody = document.getElementById("modalUserMessagesList");
    const countBadge = document.getElementById("modalMessagesCount");
    if (!tbody) return;

    cancelEditUserMessage();
    currentUserLoadedMessages = {};
    let messages = [];

    try {
      const res = await firebaseFetch(`device_overrides/${cleanDeviceKey}/messages.json`);
      if (res && res.ok) {
        const data = await safeJson(res);
        const obj = (data && data.data !== undefined) ? data.data : data;
        if (obj && typeof obj === 'object') {
          Object.entries(obj).forEach(([mId, mVal]) => {
            if (mVal !== null && mVal !== undefined) {
              let item = {};
              if (typeof mVal === 'string') {
                item = {
                  id: mId,
                  title: 'تنبيه موجه',
                  text: mVal,
                  type: 'warning',
                  displays: '0',
                  active: true,
                  createdAt: Date.now()
                };
              } else if (typeof mVal === 'object') {
                item = {
                  id: mId,
                  title: mVal.title || mVal.subject || mVal.header || mVal.name || '',
                  text: mVal.text || mVal.message || mVal.body || mVal.content || mVal.msg || mVal.details || '',
                  type: mVal.type || (mVal.isWarning ? 'warning' : 'info'),
                  displays: (mVal.displays !== undefined && mVal.displays !== null) ? String(mVal.displays) : '0',
                  active: (mVal.active !== false && mVal.active !== 'false' && mVal.active !== 0 && mVal.active !== '0'),
                  createdAt: mVal.createdAt || mVal.timestamp || mVal.publishedAt || mVal.date || null,
                  ...mVal
                };
              }

              // Fallback title if missing
              if (!item.title) {
                item.title = item.type === 'warning' ? 'تنبيه تحذيري' : (item.type === 'important' ? 'إشعار هام' : 'رسالة إدارية');
              }
              // Fallback text if missing: check specialWarningMessage on device override
              if (!item.text && override && override.specialWarningMessage) {
                item.text = override.specialWarningMessage;
              }

              currentUserLoadedMessages[mId] = item;
              messages.push(item);
            }
          });
        }
      }
    } catch (_) {}

    // Check for specialWarningMessage and ensure it is always listed
    const legacyMsg = override ? override.specialWarningMessage : '';
    if (legacyMsg && typeof legacyMsg === 'string' && legacyMsg.trim().length > 0) {
      const alreadyInList = messages.some(m => (m.text || '').trim() === legacyMsg.trim());
      if (!alreadyInList) {
        const legItem = {
          id: 'legacy_warning',
          title: 'تنبيه شاشة البداية الموجه',
          text: legacyMsg.trim(),
          type: 'warning',
          displays: '0',
          active: true,
          createdAt: override.updatedAt || Date.now()
        };
        currentUserLoadedMessages['legacy_warning'] = legItem;
        messages.push(legItem);
      }
    }

    if (countBadge) countBadge.innerText = messages.length;

    if (messages.length === 0) {
      tbody.innerHTML = `<tr><td colspan="6" style="text-align: center; color: var(--text-muted); padding: 18px;">لا توجد أي رسائل موجهة لهذا الجهاز حالياً</td></tr>`;
      return;
    }

    messages.sort((a, b) => (b.createdAt || 0) - (a.createdAt || 0));

    tbody.innerHTML = messages.map(m => {
      const typeIcons = { warning: 'تحذيري', info: 'ℹ️ إخباري', important: '📢 هام' };
      const typeBadge = typeIcons[m.type] || 'ℹ️ تنبيه';
      const dispText = m.displays == '1' ? 'مرة واحدة' : (m.displays == '3' ? '3 مرات' : 'دائم');
      const activeBadge = m.active !== false
        ? `<span class="badge badge-active">نشطة ومفعلة</span>`
        : `<span class="badge badge-blocked">معطلة</span>`;
      const dateStr = m.createdAt ? new Date(m.createdAt).toLocaleDateString('ar-EG') : 'الآن';

      const toggleAction = m.active !== false
        ? `<button class="btn-sm btn-outline" style="padding:3px 8px; font-size:10.5px;" onclick="toggleUserMessageState('${cleanDeviceKey}', '${m.id}', false)">تعطيل</button>`
        : `<button class="btn-sm btn-unblock" style="padding:3px 8px; font-size:10.5px;" onclick="toggleUserMessageState('${cleanDeviceKey}', '${m.id}', true)">تفعيل</button>`;

      const editBtn = `<button class="btn-sm btn-outline" style="padding:3px 8px; font-size:10.5px; color:var(--primary); border-color:var(--primary);" onclick="startEditUserMessage('${cleanDeviceKey}', '${m.id}')" title="تعديل نص وعنوان التنبيه">تعديل </button>`;

      const deleteBtn = `<button class="btn-sm btn-block" style="padding:3px 8px; font-size:10.5px;" onclick="deleteUserMessage('${cleanDeviceKey}', '${m.id}')" title="حذف هذا التنبيه نهائياً">حذف </button>`;

      const safeText = m.text ? escapeHtml(m.text) : '<span style="color:var(--text-muted); font-style:italic;">(لا يوجد نص)</span>';
      const safeTitle = escapeHtml(m.title || 'تنبيه موجه');

      return `
        <tr>
          <td><small style="color:var(--accent); font-weight:700;">${typeBadge}</small><br><b>${safeTitle}</b></td>
          <td style="max-width: 260px; font-size: 12.5px; line-height: 1.5; color:var(--text-title);">${safeText}</td>
          <td><small>${dispText}</small></td>
          <td>${activeBadge}</td>
          <td><small>${dateStr}</small></td>
          <td>
            <div style="display:flex; gap:4px; flex-wrap:wrap;">
              ${editBtn}
              ${toggleAction}
              ${deleteBtn}
            </div>
          </td>
        </tr>
      `;
    }).join('');
  }

  async function saveNewUserMessage() {
    if (!activeModalUserKey) return;
    const editingId = (document.getElementById("editingMsgId")?.value || "").trim();
    const title = (document.getElementById("newMsgTitle")?.value || "").trim();
    const text = (document.getElementById("newMsgText")?.value || "").trim();
    const type = document.getElementById("newMsgType")?.value || "warning";
    const displays = document.getElementById("newMsgDisplays")?.value || "0";

    if (!text) {
      return alert("يرجى كتابة نص الرسالة أو التنبيه أولاً");
    }

    const isEdit = Boolean(editingId);
    const msgId = (isEdit && editingId !== 'legacy_warning') ? editingId : ("msg_" + Date.now());
    const existingObj = isEdit ? (currentUserLoadedMessages[editingId] || {}) : {};

    const msgPayload = {
      title: title || (type === 'warning' ? 'تنبيه تحذيري' : 'تنبيه من الإدارة'),
      text: text,
      type: type,
      displays: displays,
      active: existingObj.active !== undefined ? existingObj.active : true,
      createdAt: existingObj.createdAt || Date.now(),
      updatedAt: Date.now()
    };

    try {
      // 1. Save in device_overrides messages node
      await firebaseFetch(`device_overrides/${activeModalUserKey}/messages/${msgId}.json`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(msgPayload)
      });

      // 2. Also keep specialWarningMessage updated for immediate splash screen appearance
      await firebaseFetch(`device_overrides/${activeModalUserKey}/specialWarningMessage.json`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(text)
      });

      await firebaseFetch(`all_users/${activeModalUserKey}/specialWarningMessage.json`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(text)
      });

      // 3. Update local caches
      if (!allOverrides[activeModalUserKey]) allOverrides[activeModalUserKey] = {};
      if (!allOverrides[activeModalUserKey].messages) allOverrides[activeModalUserKey].messages = {};
      allOverrides[activeModalUserKey].messages[msgId] = msgPayload;
      allOverrides[activeModalUserKey].specialWarningMessage = text;

      cancelEditUserMessage();

      alert(isEdit ? "تم تعديل وحفظ بيانات التنبيه وتحديثها على الهاتف بنجاح!" : "تم حفظ وإرسال التنبيه الموجه بنجاح! سيظهر للمستخدم في هاتفه فوراً.");
      loadUserMessagesList(activeModalUserKey, allOverrides[activeModalUserKey] || {});
      loadFirebaseData();
    } catch (e) {
      alert("حدث خطأ أثناء حفظ الرسالة: " + e.message);
    }
  }

  async function toggleUserMessageState(cleanDeviceKey, msgId, newState) {
    try {
      if (msgId !== 'legacy_warning') {
        await firebaseFetch(`device_overrides/${cleanDeviceKey}/messages/${msgId}/active.json`, {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(newState)
        });
      }

      if (!newState) {
        await firebaseFetch(`device_overrides/${cleanDeviceKey}/specialWarningMessage.json`, { method: "DELETE" }).catch(() => {});
        await firebaseFetch(`all_users/${cleanDeviceKey}/specialWarningMessage.json`, { method: "DELETE" }).catch(() => {});
        if (allOverrides[cleanDeviceKey]) delete allOverrides[cleanDeviceKey].specialWarningMessage;
      } else {
        const m = currentUserLoadedMessages[msgId];
        if (m && m.text) {
          await firebaseFetch(`device_overrides/${cleanDeviceKey}/specialWarningMessage.json`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(m.text)
          }).catch(() => {});
          if (allOverrides[cleanDeviceKey]) allOverrides[cleanDeviceKey].specialWarningMessage = m.text;
        }
      }

      if (allOverrides[cleanDeviceKey] && allOverrides[cleanDeviceKey].messages && allOverrides[cleanDeviceKey].messages[msgId]) {
        allOverrides[cleanDeviceKey].messages[msgId].active = newState;
      }

      loadFirebaseData();
      loadUserMessagesList(cleanDeviceKey, allOverrides[cleanDeviceKey] || {});
    } catch (e) {
      alert("خطأ: " + e.message);
    }
  }

  async function deleteUserMessage(cleanDeviceKey, msgId) {
    if (!confirm("هل أنت متأكد من حذف هذه الرسالة نهائياً؟")) return;
    try {
      if (msgId !== 'legacy_warning') {
        await firebaseFetch(`device_overrides/${cleanDeviceKey}/messages/${msgId}.json`, {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(null)
        }).catch(() => {});
        await firebaseFetch(`device_overrides/${cleanDeviceKey}/messages/${msgId}.json`, { method: "DELETE" }).catch(() => {});
      }
      await firebaseFetch(`device_overrides/${cleanDeviceKey}/specialWarningMessage.json`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(null)
      }).catch(() => {});
      await firebaseFetch(`device_overrides/${cleanDeviceKey}/specialWarningMessage.json`, { method: "DELETE" }).catch(() => {});
      await firebaseFetch(`all_users/${cleanDeviceKey}/specialWarningMessage.json`, { method: "DELETE" }).catch(() => {});
      
      if (allOverrides[cleanDeviceKey]) {
        delete allOverrides[cleanDeviceKey].specialWarningMessage;
        if (allOverrides[cleanDeviceKey].messages && allOverrides[cleanDeviceKey].messages[msgId]) {
          delete allOverrides[cleanDeviceKey].messages[msgId];
        }
      }

      const editIdEl = document.getElementById("editingMsgId");
      if (editIdEl && editIdEl.value === msgId) {
        cancelEditUserMessage();
      }

      alert("تم حذف الرسالة والتنبيه نهائياً بنجاح.");
      loadFirebaseData();
      loadUserMessagesList(cleanDeviceKey, allOverrides[cleanDeviceKey] || {});
    } catch (e) {
      alert("خطأ: " + e.message);
    }
  }

  async function sendDirectUserPushNotification() {
    if (!activeModalUserKey) return;
    const title = (document.getElementById("userPushTitle")?.value || "").trim();
    const body = (document.getElementById("userPushBody")?.value || "").trim();
    const url = (document.getElementById("userPushUrl")?.value || "").trim();
    const statusEl = document.getElementById("userPushStatus");
    const sendBtn = document.getElementById("btnSendDirectPush");

    if (!title || !body) {
      return alert("يرجى ملء عنوان ونص الإشعار");
    }

    const u = allUsers.find(item => {
      const devKey = item.cleanDeviceKey || (item.deviceCode ? item.deviceCode.replace(/-/g, "").trim() : "") || item.id || item.firebaseKey || "";
      return devKey === activeModalUserKey || item.id === activeModalUserKey || item.firebaseKey === activeModalUserKey;
    });
    const deviceCode = u?.deviceCode || activeModalUserKey;

    sendBtn.disabled = true;
    sendBtn.innerText = "جاري إرسال الإشعار...";
    statusEl.style.display = "block";
    statusEl.style.color = "var(--accent)";
    statusEl.innerText = "جاري التوصيل لمحرك Google FCM وستارة الهاتف... ⏳";

    try {
      const res = await apiFetch('api/fcm_sender.php', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          action: 'send_push',
          title: title,
          body: body,
          action_url: url,
          target: 'DEVICE',
          target_device_code: deviceCode
        })
      });

      const data = (await safeJson(res)) || {};
      if (data && data.success) {
        statusEl.style.color = "var(--success)";
        statusEl.innerText = `${data.message || 'تم إرسال الإشعار بنجاح!'}`;
        alert("" + (data.message || "تم إرسال الإشعار لستارة هاتف المستخدم بنجاح!"));
        
        loadFirebaseData();
        loadUserMessagesList(activeModalUserKey, allOverrides[activeModalUserKey] || {});
      } else {
        statusEl.style.color = "var(--danger)";
        statusEl.innerText = `${data.message || 'تعذر الإرسال'}`;
        alert("تعذر إرسال الإشعار: " + (data.message || 'خطأ غير معروف'));
      }
    } catch (e) {
      statusEl.style.color = "var(--danger)";
      statusEl.innerText = `خطأ في الاتصال: ${e.message}`;
      alert("خطأ في الاتصال بالسيرفر: " + e.message);
    } finally {
      sendBtn.disabled = false;
      sendBtn.innerText = "إرسال الإشعار فوراً للهاتف";
    }
  }

  // Initial load
  initTheme();
  checkServerSession();
  try {
    const savedTab = (location.hash ? location.hash.replace('#', '') : '') || localStorage.getItem('mosarib_admin_tab') || sessionStorage.getItem('mosarib_active_tab');
    switchTab(ALL_TABS.includes(savedTab) ? savedTab : 'analytics');
  } catch (_) {}

  // Force Update warning and default unchecked setup (B2)
  const forceEl = document.getElementById("apkForceUpdate");
  if (forceEl) {
    forceEl.checked = false;
    let warnDiv = document.getElementById("apkForceWarnLabel");
    if (!warnDiv) {
      warnDiv = document.createElement("div");
      warnDiv.id = "apkForceWarnLabel";
      warnDiv.style.cssText = "font-size: 12px; color: var(--danger); margin-top: 4px; display: none; font-weight: 600;";
      warnDiv.textContent = "لا تفعّله إلا بعد أن يحدّث أغلب المستخدمين؛ يمنع الإصدارات الأقدم من العمل";
      forceEl.parentElement.parentElement.appendChild(warnDiv);
    }
    forceEl.addEventListener("change", function() {
      warnDiv.style.display = this.checked ? "block" : "none";
      const opts = document.getElementById("apkForceOptions");
      if (opts) opts.style.display = this.checked ? "block" : "none";
      refreshApkForceImpact();
      if (this.checked) {
        alert("تنبيه: لا تفعّله إلا بعد أن يحدّث أغلب المستخدمين؛ يمنع الإصدارات الأقدم من العمل");
      }
    });
  }


  // ===== Task 036: audience by app build, notification pictures, force update for specific builds =====

  function parseVersionCodes(raw) {
    const seen = new Set();
    String(raw || '').split(/[\s,،]+/).forEach(p => {
      const n = parseInt(p, 10);
      if (n > 0) seen.add(n);
    });
    return Array.from(seen);
  }

  function userVersionCode(u) {
    const n = parseInt(u && u.versionCode, 10);
    return n > 0 ? n : 0;
  }

  /** {versionCode: users} from all_users (users without a build number are counted as 0). */
  function versionUsageMap() {
    const map = {};
    (allUsers || []).forEach(u => {
      const c = userVersionCode(u);
      map[c] = (map[c] || 0) + 1;
    });
    return map;
  }

  function versionUsageHtml() {
    const map = versionUsageMap();
    const codes = Object.keys(map).map(Number).sort((a, b) => b - a);
    if (!codes.length) return '';
    const names = {};
    (allUsers || []).forEach(u => { const c = userVersionCode(u); if (c && u.appVersion) names[c] = u.appVersion; });
    return 'الإصدارات المستخدمة الآن: ' + codes.map(c =>
      c ? `<b>${c}</b>${names[c] ? ' (' + escapeHtml(names[c]) + ')' : ''}: ${map[c]}` : `غير معروف: ${map[c]}`
    ).join(' • ');
  }

  function countUsersInRange(min, max) {
    return (allUsers || []).filter(u => {
      const c = userVersionCode(u);
      if (!c) return false;
      if (min > 0 && c < min) return false;
      if (max > 0 && c > max) return false;
      return true;
    }).length;
  }

  /** Reads EQ / LT / RANGE inputs with the given prefix. null = everyone, false = invalid. */
  function readVersionRange(prefix) {
    const mode = document.getElementById(prefix + 'Mode')?.value || 'ALL';
    const from = parseInt(document.getElementById(prefix + 'From')?.value, 10) || 0;
    const to = parseInt(document.getElementById(prefix + 'To')?.value, 10) || 0;
    if (mode === 'ALL') return null;
    if (mode === 'EQ') return from > 0 ? { min: from, max: from } : false;
    if (mode === 'LT') return from > 1 ? { min: 0, max: from - 1 } : false;
    if (mode === 'RANGE') return (from > 0 && to >= from) ? { min: from, max: to } : false;
    return null;
  }

  function countForceLocked(mode, minCode, codes) {
    return (allUsers || []).filter(u => {
      const c = userVersionCode(u);
      if (!c) return false;
      return mode === 'SPECIFIC' ? codes.includes(c) : c < minCode;
    }).length;
  }

  function readBroadcastForceAudience() {
    const mode = (document.querySelector('input[name="bcForceMode"]:checked') || {}).value || 'OLDER';
    return { mode, codes: parseVersionCodes(document.getElementById('bcForceCodes')?.value || '') };
  }

  function refreshBroadcastAudience() {
    const isUpdate = document.getElementById('bcType')?.value === 'UPDATE';
    const group = document.getElementById('bcAudienceGroup');
    if (group) group.style.display = isUpdate ? 'none' : 'block';
    const usage = document.getElementById('bcVersionUsage');
    if (usage) usage.innerHTML = versionUsageHtml();

    const countEl = document.getElementById('bcAudienceCount');
    if (countEl && !isUpdate) {
      const range = readVersionRange('bcAudience');
      countEl.innerText = range === false ? 'حدد رقم الإصدار بشكل صحيح'
        : `سيصل إلى ${range ? countUsersInRange(range.min, range.max) : (allUsers || []).length} مستخدم`;
    }
    const impact = document.getElementById('bcForceImpact');
    if (impact && isUpdate) {
      const f = readBroadcastForceAudience();
      const minVer = parseInt(document.getElementById('bcMinVersion')?.value, 10) || 1;
      impact.innerText = `سيتم قفل التطبيق على ${countForceLocked(f.mode, minVer, f.codes)} مستخدم حتى يحدّثوا`;
    }
  }

  function refreshApkForceImpact() {
    const el = document.getElementById('apkForceImpact');
    if (!el) return;
    const mode = (document.querySelector('input[name="apkForceMode"]:checked') || {}).value || 'OLDER';
    const codes = parseVersionCodes(document.getElementById('apkForceCodes')?.value || '');
    const newCode = parseInt(document.getElementById('apkVersionCode')?.value, 10) || 1;
    el.innerHTML = `سيتم قفل التطبيق على ${countForceLocked(mode, newCode, codes)} مستخدم حتى يحدّثوا<br><span style="font-weight:normal; color:var(--text-muted);">${versionUsageHtml()}</span>`;
  }

  // ---- Push: audience preview (server counts the real tokens) and notification preview
  let pushAudienceTimer = null;
  function refreshPushAudience() {
    const target = document.getElementById('pushTarget')?.value || 'ALL';
    const block = document.getElementById('pushVersionBlock');
    if (block) block.style.display = target === 'VERSION' ? 'block' : 'none';
    const usage = document.getElementById('pushVersionUsage');
    if (usage) usage.innerHTML = versionUsageHtml();
    const out = document.getElementById('pushAudience');
    if (!out) return;
    if (target === 'VERSION' && readVersionRange('pushVersion') === false) {
      out.innerText = 'حدد رقم الإصدار بشكل صحيح';
      return;
    }
    clearTimeout(pushAudienceTimer);
    pushAudienceTimer = setTimeout(async () => {
      try {
        const res = await apiFetch('api/fcm_sender.php', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            action: 'send', dry_run: true, target,
            version_mode: document.getElementById('pushVersionMode')?.value || '',
            version_from: document.getElementById('pushVersionFrom')?.value || '',
            version_to: document.getElementById('pushVersionTo')?.value || ''
          })
        });
        const data = (await safeJson(res)) || {};
        out.innerText = data.success ? `سيصل إلى ${data.data.audience} جهاز` : (data.message || '');
      } catch (_) {
        out.innerText = '';
      }
    }, 350);
  }

  function renderPushPreview() {
    const box = document.getElementById('pushPreview');
    if (!box) return;
    const title = document.getElementById('pushTitle')?.value.trim() || '';
    const body = document.getElementById('pushBody')?.value.trim() || '';
    const url = document.getElementById('pushUrl')?.value.trim() || '';
    const img = document.getElementById('pushImageUrl')?.value.trim() || '';
    if (!title && !body) { box.innerHTML = ''; return; }
    box.innerHTML = `
      <div style="border:1px solid var(--border); border-radius:14px; padding:10px 12px; background:var(--surface-alt);">
        <div style="font-size:11px; color:var(--text-muted); margin-bottom:4px;">معاينة الإشعار</div>
        <div style="font-weight:800;">${escapeHtml(title)}</div>
        <div style="font-size:13px; color:var(--text-body); margin-top:2px;">${escapeHtml(body)}</div>
        ${img.startsWith('https://') ? `<img src="${escapeHtml(img)}" alt="" style="margin-top:8px; width:100%; max-height:150px; object-fit:cover; border-radius:10px;">` : ''}
        ${url ? `<div style="margin-top:6px; font-size:12px; color:var(--primary); font-weight:700;">${url.toLowerCase().startsWith('mosarib://') ? 'يفتح شاشة في التطبيق' : 'يفتح الرابط في المتصفح'} • زر «فتح الرابط»</div>` : ''}
      </div>`;
  }

  // ---- Pictures for notifications / announcements
  function previewNoticeImage(prefix) {
    const url = (document.getElementById(prefix + 'ImageUrl')?.value || '').trim();
    const img = document.getElementById(prefix + 'ImagePreview');
    if (img) {
      if (url.startsWith('https://')) { img.src = url; img.style.display = 'block'; }
      else { img.removeAttribute('src'); img.style.display = 'none'; }
    }
    if (prefix === 'push') renderPushPreview();
  }

  async function uploadNoticeImage(prefix) {
    const input = document.getElementById(prefix + 'ImageFile');
    const file = input && input.files && input.files[0];
    if (!file) return;
    if (file.size > 1024 * 1024) { alert('حجم الصورة يجب ألا يتجاوز 1 ميجابايت'); input.value = ''; return; }
    const fd = new FormData();
    fd.append('action', 'upload_image');
    fd.append('image_file', file);
    fd.append('csrf_token', currentCsrfToken);
    try {
      const res = await apiFetch('api/fcm_sender.php', { method: 'POST', body: fd });
      const data = (await safeJson(res)) || {};
      if (!data.success) throw new Error(data.message || 'تعذر رفع الصورة');
      document.getElementById(prefix + 'ImageUrl').value = data.data.url;
      previewNoticeImage(prefix);
    } catch (e) {
      alert('فشل رفع الصورة: ' + e.message);
    } finally {
      input.value = '';
    }
  }

  (function initTask036Inputs() {
    const target = document.getElementById('pushTarget');
    if (target) target.addEventListener('change', refreshPushAudience);
    ['pushTitle', 'pushBody'].forEach(id => document.getElementById(id)?.addEventListener('input', renderPushPreview));
    document.getElementById('bcType')?.addEventListener('change', refreshBroadcastAudience);
    document.getElementById('apkVersionCode')?.addEventListener('input', refreshApkForceImpact);
  })();

  function refreshTask036Panels() {
    refreshBroadcastAudience();
    refreshApkForceImpact();
    refreshPushAudience();
  }


  // ===== Task 036: subscription dates, expiring subscriptions, user removal =====

  let expiringDays = 7;
  const DAY_MS = 24 * 3600 * 1000;

  /** Keeps the newest license per device (by expiresAt). Read only: licenses are never changed here. */
  function rememberSubscription(deviceCode, lic) {
    if (!deviceCode || !lic || typeof lic !== 'object') return;
    const key = String(deviceCode).replace(/-/g, '').trim().toUpperCase();
    const expiresAt = Number(lic.expiresAt) || 0;
    if (!key || !expiresAt) return;
    const prev = subscriptionByDevice[key];
    if (!prev || expiresAt > prev.expiresAt) {
      subscriptionByDevice[key] = {
        activatedAt: Number(lic.activatedAt) || 0,
        expiresAt: expiresAt,
        plan: lic.plan || '',
        isRevoked: lic.isRevoked === true,
        key: String(lic.key || '').replace(/-/g, '')
      };
    }
  }

  function formatDateDMY(ms) {
    if (!ms) return '-';
    const d = new Date(ms);
    const dd = String(d.getDate()).padStart(2, '0');
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    return `${dd}/${mm}/${d.getFullYear()}`;
  }

  function daysLeft(expiresAt) {
    return Math.ceil((expiresAt - Date.now()) / DAY_MS);
  }

  function planLabel(plan) {
    if (plan === 'YEARLY') return 'سنوي';
    if (plan === 'MONTHLY') return 'شهري';
    return plan || '-';
  }


  // ===== Task 067: presentation helpers (status chip, relative time, row menu, subscriptions list) =====

  function relativeTime(ms) {
    if (!ms) return 'غير محدد';
    const m = Math.floor((Date.now() - ms) / 60000);
    if (m < 1) return 'الآن';
    if (m < 60) return `منذ ${m} دقيقة`;
    const h = Math.floor(m / 60);
    if (h < 24) return `منذ ${h} ساعة`;
    const d = Math.floor(h / 24);
    return d < 30 ? `منذ ${d} يوم` : formatDateDMY(ms);
  }

  function isActiveToday(u) {
    const t = new Date();
    t.setHours(0, 0, 0, 0);
    return (u.lastActiveAt || 0) >= t.getTime();
  }

  /** One status per user: blocked / active (subscribed until date) / expired / trial. */
  function userStatus(u, devKey) {
    const sub = subscriptionByDevice[String(devKey || '').toUpperCase()];
    if (u.isBlocked === true) return { kind: 'blocked', label: 'محظور' };
    if (u.isActivated === true) return { kind: 'active', label: sub ? `مشترك حتى ${formatDateDMY(sub.expiresAt)}` : 'مشترك' };
    if (sub) return { kind: 'expired', label: sub.isRevoked ? 'ملغى' : 'منتهٍ' };
    return { kind: 'trial', label: 'تجريبي' };
  }

  function emptyRow(cols, title, hint) {
    return `<tr class="empty-row"><td colspan="${cols}"><div class="empty-state"><div class="empty-title">${title}</div><div class="empty-hint">${hint || ''}</div></div></td></tr>`;
  }

  // Row actions menu (fixed position, so the scrolling table never clips it)
  let openMenuEl = null;
  function closeRowMenu() {
    if (!openMenuEl) return;
    openMenuEl.classList.remove('open');
    openMenuEl.style.top = '';
    openMenuEl.style.left = '';
    openMenuEl = null;
  }
  function toggleRowMenu(btn) {
    const menu = btn.nextElementSibling;
    const wasOpen = openMenuEl === menu;
    closeRowMenu();
    if (wasOpen || !menu) return;
    menu.classList.add('open');
    const r = btn.getBoundingClientRect();
    const w = menu.offsetWidth, h = menu.offsetHeight;
    const left = Math.max(8, Math.min(r.left, window.innerWidth - w - 8));
    let top = r.bottom + 4;
    if (top + h > window.innerHeight - 8) top = Math.max(8, r.top - h - 4);
    menu.style.top = top + 'px';
    menu.style.left = left + 'px';
    openMenuEl = menu;
  }
  document.addEventListener('click', e => {
    if (openMenuEl && !e.target.closest('.kebab-btn') && (!openMenuEl.contains(e.target) || e.target.closest('.menu-item'))) closeRowMenu();
  });
  window.addEventListener('scroll', closeRowMenu, true);
  window.addEventListener('resize', closeRowMenu);

  // Subscriptions section: read-only, one row per device (newest licence, from subscriptionByDevice)
  let subsFilter = 'ALL';

  function subStatus(sub) {
    const left = daysLeft(sub.expiresAt);
    if (sub.isRevoked) return { kind: 'revoked', label: 'ملغى', left };
    if (sub.expiresAt <= Date.now()) return { kind: 'expired', label: 'منتهٍ', left };
    if (left <= 7) return { kind: 'soon', label: 'ينتهي قريباً', left };
    return { kind: 'active', label: 'فعّال', left };
  }

  function setSubsFilter(f) {
    subsFilter = f;
    document.querySelectorAll('#subsFilterChips .pill-filter-btn').forEach(b => b.classList.toggle('active', b.dataset.subs === f));
    renderSubscriptionsList();
  }

  function renderSubscriptionsList() {
    const body = document.getElementById('subsTableBody');
    if (!body) return;
    const all = Object.entries(subscriptionByDevice)
      .map(([key, sub]) => ({ key, sub, user: userByKey(key), st: subStatus(sub) }));
    const count = k => all.filter(x => x.st.kind === k).length;
    const setCount = (id, n) => { const el = document.getElementById(id); if (el) el.innerText = n; };
    setCount('countSubsAll', all.length);
    setCount('countSubsSoon', count('soon'));
    setCount('countSubsExpired', count('expired'));
    setCount('countSubsRevoked', count('revoked'));

    const q = (document.getElementById('subsSearch')?.value || '').trim().toLowerCase();
    let list = all.filter(x => subsFilter === 'ALL' || x.st.kind === subsFilter);
    if (q) {
      list = list.filter(x => {
        const name = ((x.user && (x.user.subscriberName || x.user.googleDisplayName)) || '').toLowerCase();
        return name.includes(q) || x.key.toLowerCase().includes(q);
      });
    }
    // nearest end first for upcoming, most recent first for ended ones
    list.sort((a, b) => a.sub.expiresAt - b.sub.expiresAt);
    if (!list.length) {
      body.innerHTML = emptyRow(6, 'لا توجد اشتراكات مطابقة', 'غيّر الفلتر أو عبارة البحث');
      return;
    }
    body.innerHTML = list.map(({ key, sub, user, st }) => {
      const name = (user && (user.subscriberName || user.googleDisplayName)) || 'مشترك';
      const phone = (user && user.phone) || '';
      const leftText = st.kind === 'revoked' ? '' : (st.left < 0 ? `منذ ${Math.abs(st.left)} يوم` : `بعد ${st.left} يوم`);
      const chipKind = st.kind === 'soon' ? 'trial' : (st.kind === 'revoked' ? 'blocked' : (st.kind === 'expired' ? 'expired' : 'active'));
      return `<tr>
        <td data-label="المشترك"><div><b>${escapeHtml(name)}</b>${phone ? `<small class="cell-sub">${escapeHtml(phone)}</small>` : ''}</div></td>
        <td data-label="كود الجهاز"><code>${escapeHtml(key)}</code></td>
        <td data-label="الباقة">${escapeHtml(planLabel(sub.plan))}</td>
        <td data-label="ينتهي في"><div>${formatDateDMY(sub.expiresAt)}${leftText ? `<small class="cell-sub">${leftText}</small>` : ''}</div></td>
        <td data-label="الحالة"><span class="chip chip-${chipKind}">${st.kind === 'soon' ? `ينتهي خلال ${Math.max(st.left, 0)} يوم` : st.label}</span></td>
        <td class="col-actions"><button type="button" class="btn-sm btn-unblock" onclick="openExtendModal('${escapeHtml(key)}')">تمديد</button></td>
      </tr>`;
    }).join('');
  }

  function subscriptionHtml(devKey) {
    const sub = subscriptionByDevice[String(devKey || '').toUpperCase()];
    if (!sub) return '';
    const left = daysLeft(sub.expiresAt);
    const color = sub.isRevoked || left < 0 ? 'var(--danger)' : (left <= 7 ? 'var(--accent)' : 'var(--success)');
    const leftText = sub.isRevoked ? 'ملغى' : (left < 0 ? `منتهي منذ ${Math.abs(left)} يوم` : `المتبقي: ${left} يوم`);
    return `<div style="font-size:11px; line-height:1.7; margin-top:4px; color:var(--text-muted);">
      بداية الاشتراك: <b>${formatDateDMY(sub.activatedAt)}</b><br>
      نهاية الاشتراك: <b>${formatDateDMY(sub.expiresAt)}</b><br>
      <b style="color:${color};">${leftText}</b></div>`;
  }

  function userByKey(devKey) {
    const k = String(devKey || '').toUpperCase();
    return (allUsers || []).find(u => (u.cleanDeviceKey || String(u.deviceCode || '').replace(/-/g, '')).toUpperCase() === k);
  }

  /** days > 0: ends within that many days; days < 0: already ended. Sorted by nearest end. */
  function expiringSubscriptions(days) {
    const now = Date.now();
    return Object.entries(subscriptionByDevice)
      .filter(([, s]) => !s.isRevoked && (days < 0 ? s.expiresAt <= now : (s.expiresAt > now && s.expiresAt - now <= days * DAY_MS)))
      .map(([key, s]) => ({ key, sub: s, user: userByKey(key) }))
      .sort((a, b) => days < 0 ? b.sub.expiresAt - a.sub.expiresAt : a.sub.expiresAt - b.sub.expiresAt);
  }

  function setExpiringFilter(days) {
    expiringDays = days;
    document.querySelectorAll('.exp-pill').forEach(b => b.classList.toggle('active', Number(b.dataset.days) === days));
    renderExpiringSubscriptions();
  }

  function whatsappNumber(phone) {
    let p = String(phone || '').replace(/[^0-9]/g, '');
    if (p.startsWith('00')) p = p.slice(2);
    if (p.startsWith('0')) p = p.slice(1);
    if (p.length === 9) p = '967' + p;
    return p;
  }

  function renderExpiringSubscriptions() {
    const body = document.getElementById('expiringTableBody');
    const badge = document.getElementById('expiringCountBadge');
    const list = expiringSubscriptions(expiringDays);
    if (badge) badge.innerText = expiringSubscriptions(7).length;
    if (!body) return;
    if (!list.length) {
      body.innerHTML = `<tr><td colspan="6" style="text-align:center; color:var(--text-muted);">لا توجد اشتراكات ${expiringDays < 0 ? 'منتهية' : 'تنتهي خلال ' + expiringDays + ' يوماً'}</td></tr>`;
      return;
    }
    body.innerHTML = list.map(({ key, sub, user }) => {
      const name = (user && (user.subscriberName || user.googleDisplayName)) || 'مشترك';
      const phone = (user && user.phone) || '';
      const left = daysLeft(sub.expiresAt);
      const msg = left < 0
        ? `مرحباً ${name}، انتهى اشتراكك في تطبيق المُسَرِّب بتاريخ ${formatDateDMY(sub.expiresAt)}. للتجديد تواصل معنا.`
        : `مرحباً ${name}، اشتراكك في تطبيق المُسَرِّب ينتهي بتاريخ ${formatDateDMY(sub.expiresAt)} (بعد ${left} يوم). للتجديد تواصل معنا.`;
      const wa = phone ? `<a class="btn-sm btn-outline" style="padding:4px 8px; font-size:11px;" target="_blank" rel="noopener" href="https://wa.me/${whatsappNumber(phone)}?text=${encodeURIComponent(msg)}">واتساب</a>` : '';
      return `<tr>
        <td><b>${escapeHtml(name)}</b><br><code style="font-size:11px;">${escapeHtml(key)}</code></td>
        <td>${escapeHtml(phone || '-')}</td>
        <td>${escapeHtml(planLabel(sub.plan))}</td>
        <td>${formatDateDMY(sub.expiresAt)}</td>
        <td><b style="color:${left < 0 ? 'var(--danger)' : (left <= 7 ? 'var(--accent)' : 'var(--success)')};">${left < 0 ? 'منتهي' : left + ' يوم'}</b></td>
        <td><div class="btn-action-group">
          <button class="btn-sm btn-outline" style="padding:4px 8px; font-size:11px;" onclick="sendRenewalReminder('${escapeHtml(key)}', ${left})">تذكير 🔔</button>
          ${wa}
        </div></td>
      </tr>`;
    }).join('');
  }

  async function sendRenewalReminder(devKey, left) {
    const title = 'تذكير بتجديد الاشتراك';
    const body = left < 0 ? 'انتهى اشتراكك في المُسَرِّب. جدّده للاستمرار دون توقف.' : `اشتراكك في المُسَرِّب ينتهي بعد ${left} يوم. جدّده للاستمرار دون توقف.`;
    if (!confirm(`إرسال إشعار تذكير لهذا المشترك فقط؟\n\n${body}`)) return;
    try {
      const res = await apiFetch('api/fcm_sender.php', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ action: 'send', target: 'DEVICE', target_device_code: devKey, title, body })
      });
      const data = (await safeJson(res)) || {};
      alert(data.success ? 'تم إرسال التذكير' : ('تعذر الإرسال: ' + (data.message || '')));
    } catch (e) {
      alert('تعذر الإرسال: ' + e.message);
    }
  }

  // ---- selection and bulk actions
  function updateBulkBar() {
    const bar = document.getElementById('usersBulkBar');
    const count = document.getElementById('usersSelectedCount');
    if (count) count.innerText = selectedUserKeys.size;
    if (bar) bar.style.display = selectedUserKeys.size ? 'flex' : 'none';
  }

  function toggleUserSelection(devKey, checked) {
    if (checked) selectedUserKeys.add(devKey); else selectedUserKeys.delete(devKey);
    updateBulkBar();
  }

  function toggleSelectAllUsers(checked) {
    document.querySelectorAll('#usersTableBody .user-select').forEach(cb => {
      cb.checked = checked;
      toggleUserSelection(cb.value, checked);
    });
  }

  function clearUserSelection() {
    selectedUserKeys.clear();
    const all = document.getElementById('usersSelectAll');
    if (all) all.checked = false;
    document.querySelectorAll('#usersTableBody .user-select').forEach(cb => { cb.checked = false; });
    updateBulkBar();
  }

  async function bulkDisableUsers() {
    const keys = Array.from(selectedUserKeys);
    if (!keys.length) return;
    if (!confirm(`تعطيل (حظر) ${keys.length} مستخدم؟ يمكن فك الحظر لاحقاً.`)) return;
    for (const key of keys) {
      const u = userByKey(key);
      // toggleBlockUser asks for a reason and confirms each time; run it per user
      await toggleBlockUser(key, (u && u.googleEmail) || '', true);
    }
    clearUserSelection();
  }

  async function bulkSoftDeleteUsers() {
    const keys = Array.from(selectedUserKeys);
    if (keys.length) await softDeleteUsers(keys);
  }

  async function softDeleteUsers(keys) {
    const reason = prompt(`حذف ${keys.length} مستخدم من القائمة؟\nيمكن استرجاعهم من «المحذوفون» خلال 30 يوماً، والترخيص لا يُحذف.\n\nسبب الحذف (اختياري):`, '');
    if (reason === null) return;
    try {
      const res = await apiFetch('api/admin_users.php', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ action: 'soft_delete', keys, reason })
      });
      const data = (await safeJson(res)) || {};
      alert(data.message || (data.success ? 'تم' : 'تعذر الحذف'));
      clearUserSelection();
      loadFirebaseData();
      refreshDeletedCount();
    } catch (e) {
      alert('تعذر الحذف: ' + e.message);
    }
  }

  // ---- deleted users list
  async function fetchDeletedUsers() {
    const res = await apiFetch('api/admin_users.php', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ action: 'list_deleted' })
    });
    const data = (await safeJson(res)) || {};
    if (!data.success) throw new Error(data.message || 'تعذر جلب المحذوفين');
    return data.data;
  }

  async function refreshDeletedCount() {
    try {
      const d = await fetchDeletedUsers();
      const el = document.getElementById('countFilterDeleted');
      if (el) el.innerText = d.users.length;
    } catch (_) {}
  }

  async function loadDeletedUsers() {
    const body = document.getElementById('deletedUsersBody');
    if (!body) return;
    try {
      const d = await fetchDeletedUsers();
      const el = document.getElementById('countFilterDeleted');
      if (el) el.innerText = d.users.length;
      if (!d.users.length) {
        body.innerHTML = `<tr><td colspan="6" style="text-align:center; color:var(--text-muted);">لا يوجد مستخدمون محذوفون</td></tr>`;
        return;
      }
      body.innerHTML = d.users.map(u => `<tr>
        <td><b>${escapeHtml(u.name || 'مستخدم')}</b>${u.email ? `<br><small style="color:#38BDF8;">${escapeHtml(u.email)}</small>` : ''}${u.phone ? `<br><small>${escapeHtml(u.phone)}</small>` : ''}</td>
        <td><code>${escapeHtml(u.key)}</code></td>
        <td>${formatDateDMY(u.deletedAt)}</td>
        <td><small>${escapeHtml(u.deletedBy || '')}${u.reason ? '<br>' + escapeHtml(u.reason) : ''}</small></td>
        <td>${formatDateDMY(u.purgeAt)}</td>
        <td><div class="btn-action-group">
          <button class="btn-sm btn-unblock" onclick="restoreDeletedUser('${escapeHtml(u.key)}')">استرجاع ↩️</button>
          ${d.can_hard_delete ? `<button class="btn-sm btn-block" onclick="hardDeleteUser('${escapeHtml(u.key)}')">حذف نهائي</button>` : ''}
        </div></td>
      </tr>`).join('');
    } catch (e) {
      body.innerHTML = `<tr><td colspan="6" style="text-align:center; color:var(--danger);">${escapeHtml(e.message)}</td></tr>`;
    }
  }

  async function restoreDeletedUser(key) {
    if (!confirm('استرجاع هذا المستخدم إلى القائمة؟')) return;
    const res = await apiFetch('api/admin_users.php', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ action: 'restore', key })
    });
    const data = (await safeJson(res)) || {};
    alert(data.message || '');
    loadDeletedUsers();
    loadFirebaseData();
  }

  async function hardDeleteUser(key) {
    const typed = prompt('الحذف النهائي لا يمكن التراجع عنه (الترخيص لا يُحذف).\nاكتب اسم المستخدم أو بريده أو كود جهازه للتأكيد:', '');
    if (!typed) return;
    const res = await apiFetch('api/admin_users.php', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ action: 'hard_delete', key, confirm: typed })
    });
    const data = (await safeJson(res)) || {};
    alert(data.message || '');
    loadDeletedUsers();
  }

  (function initTask036Users() {
    const prevRefresh = typeof refreshTask036Panels === 'function' ? refreshTask036Panels : null;
    refreshTask036Panels = function () {
      if (prevRefresh) prevRefresh();
      renderExpiringSubscriptions();
      renderSubscriptionsList();
      updateStats();
      updateBulkBar();
      refreshDeletedCount();
    };
  })();
