  let allUsers = [];
  let allFeedbacks = [];
  let allRequests = [];
  let pendingRequestId = null;
  let adminPaymentMethods = [];
  const apiFetch = (url, opts) => (typeof adminApiFetch === 'function') ? adminApiFetch(url, opts) : fetch(url, { credentials: 'same-origin', ...opts });

  function toggleUpdateInputs() {
    const isUpdate = document.getElementById("bcType").value === "UPDATE";
    document.getElementById("versionGroup").style.display = isUpdate ? "block" : "none";
    if (isUpdate) {
      document.getElementById("bcBtnText").value = "تحميل التحديث الجديد 📥";
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

  function switchTab(tabId) {
    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    ['analytics', 'requests', 'users', 'releases', 'policies', 'push', 'broadcast', 'generator', 'feedbacks', 'admins', 'publicPortal'].forEach(id => {
      const el = document.getElementById(`tab-${id}`);
      if (el) el.style.display = 'none';
    });

    const targetEl = document.getElementById(`tab-${tabId}`);
    if (targetEl) targetEl.style.display = 'block';
    const activeBtn = document.getElementById(`tabBtn${tabId.charAt(0).toUpperCase() + tabId.slice(1)}`);
    if (activeBtn) activeBtn.classList.add('active');

    if (tabId === 'releases') {
      loadReleasesList();
    } else if (tabId === 'analytics') {
      renderAllRoyalCharts();
    } else if (tabId === 'admins') {
      loadAdminsList();
    } else if (tabId === 'publicPortal') {
      loadPortalSettingsForAdmin();
    } else if (tabId === 'push') {
      loadFcmEngineStatus();
    }
  }

  async function loadPortalSettingsForAdmin() {
    try {
      const res = await apiFetch('api/public_data.php');
      const json = (await safeJson(res)) || {};
      if (json.success && json.data) {
        const ps = json.data.portal_settings || {};
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
          document.getElementById("pShowPlansSection").checked = ps.show_plans_section !== false;
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
          <b style="font-size: 13.5px; color: #FFF;">${item.title}</b>
          <div style="font-size: 11px; color: var(--text-muted);">${item.category} • ${new Date(item.published_at).toLocaleDateString('ar-EG')}</div>
        </div>
        <button class="btn-sm" style="background: rgba(229,57,53,0.2); color: #EF5350; border: 1px solid #EF5350;" onclick="deletePortalNews('${item.id}')">حذف 🗑️</button>
      </div>
    `).join('');
  }

  async function handleSavePortalSettings(e) {
    if (e) e.preventDefault();
    const btn = document.getElementById("savePortalSettingsBtn");
    if (btn) btn.disabled = true;

    const payload = {
      action: 'update_settings',
      active_region_name: document.getElementById("pActiveRegionName").value.trim(),
      avg_hourly_price: parseFloat(document.getElementById("pAvgPrice").value) || 0,
      min_hourly_price: parseFloat(document.getElementById("pMinPrice").value) || 0,
      max_hourly_price: parseFloat(document.getElementById("pMaxPrice").value) || 0,
      currency_symbol: document.getElementById("pCurrency").value.trim(),
      water_status: document.getElementById("pWaterStatus").value,
      rates_note: document.getElementById("pRatesNote").value.trim(),
      headline_alert: document.getElementById("pHeadlineAlert").value.trim(),

      hero_badge: document.getElementById("pHeroBadge").value.trim(),
      hero_title: document.getElementById("pHeroTitle").value.trim(),
      hero_subtitle: document.getElementById("pHeroSubtitle").value.trim(),
      contact_whatsapp: document.getElementById("pContactWhatsapp").value.trim(),
      contact_phone: document.getElementById("pContactPhone").value.trim(),
      developer_name: document.getElementById("pDeveloperName").value.trim(),
      developer_company: document.getElementById("pDeveloperCompany").value.trim(),
      developer_email: document.getElementById("pDeveloperEmail").value.trim(),

      show_top_ticker: document.getElementById("pShowTopTicker").checked,
      show_rates_section: document.getElementById("pShowRatesSection").checked,
      show_features_section: document.getElementById("pShowFeaturesSection").checked,
      show_plans_section: document.getElementById("pShowPlansSection").checked,
      show_developer_section: document.getElementById("pShowDeveloperSection").checked,
      show_download_cta: document.getElementById("pShowDownloadCta").checked,
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
      alert("✅ تم حفظ ونشر إعدادات ومحتوى البوابة بنجاح في سحابة فايربيس!");
    } else {
      alert(`⚠️ تم تسجيل التعديل محلياً، ولكن حدث خطأ في سحابة فايربيس: (${fbError || 'تعذر الاتصال'}). تأكد من مفتاح السحابة.`);
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
        alert("✅ " + (data.message || 'تم النشر بنجاح'));
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
      if (data.success) {
        loadPortalSettingsForAdmin();
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
        statusDiv.innerText = "✅ " + data.message;
        loadFcmEngineStatus();
        setTimeout(() => { toggleServiceAccountModal(); }, 2500);
      } else {
        statusDiv.style.color = "#EF4444";
        statusDiv.innerText = "❌ " + (data.message || "فشل التحقق من المفتاح");
      }
    } catch (err) {
      statusDiv.style.color = "#EF4444";
      statusDiv.innerText = "❌ حدث خطأ في الاتصال بالسيرفر";
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
        statusDiv.innerText = "✅ " + data.message;
      } else {
        statusDiv.style.color = "#EF4444";
        statusDiv.innerText = "❌ " + (data.message || "الاتصال غير جاهز");
      }
    } catch (_) {
      statusDiv.style.color = "#EF4444";
      statusDiv.innerText = "❌ تعذر الاتصال بالخادم";
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
          target: target
        })
      });

      const resData = (await safeJson(res)) || {};
      if (resData.success) {
        statusDiv.style.color = "var(--success)";
        statusDiv.innerHTML = `✅ ${resData.message}`;
        loadFcmEngineStatus();
      } else {
        throw new Error(resData.message || 'حدث خطأ في الإرسال');
      }
    } catch (e) {
      statusDiv.style.color = "#EF4444";
      statusDiv.innerText = "❌ " + e.message;
      // Fallback directly to client-side RTDB broadcast
      sendCustomPushNotification();
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
        statusDiv.innerText = "✅ تم حفظ ونشر أسعار وخصومات الاشتراكات في فايربيس والموقع والتطبيق بنجاح!";
      }
    } else {
      if (statusDiv) {
        statusDiv.style.color = "#EF4444";
        statusDiv.innerText = `❌ خطأ في الحفظ السحابي: ${fbError || 'تعذر الاتصال'}. يرجى التحقق من مفتاح السحابة وقواعد الأمان.`;
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
    formData.append('is_force_update', document.getElementById("apkForceUpdate").checked ? '1' : '0');

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
        alert("✅ " + (data.message || 'تم رفع ملف التحديث بنجاح') + "\n\nرابط التحميل المباشر:\n" + (data.data ? data.data.download_url : ''));
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
    const isForce = confirm("هل تريد تفعيل هذا الرابط كتحديث إجباري وقفل النسخ القديمة فوراً؟");
    const notes = prompt("ملاحظات ورسالة التحديث للمستخدمين:", "تحديث جديد متاح لتطبيق مُسَرِّب");

    const formData = new FormData();
    formData.append('action', 'set_cloud_link');
    formData.append('custom_url', url);
    formData.append('version_code', vCode);
    formData.append('is_force_update', isForce ? '1' : '0');
    formData.append('release_notes', notes || '');

    try {
      const res = await apiFetch('api/release_manager.php', { method: 'POST', body: formData });
      const data = (await safeJson(res)) || {};
      if (data.success) {
        alert("✅ تم ربط وبث الرابط لجميع المستخدمين في فايربيس بنجاح!");
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
        const sizeMb = (r.file_size / (1024 * 1024)).toFixed(1);
        const dateStr = new Date(r.uploaded_at).toLocaleString('ar-EG');
        return `
          <div style="background: var(--input-bg); border: 1px solid var(--border); border-radius: 12px; padding: 14px; margin-bottom: 12px;">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px;">
              <b style="font-size: 15px; color: #FFF;">نسخة: v${r.version_name} (Code: ${r.version_code})</b>
              <span class="badge ${r.is_force_update ? 'badge-blocked' : 'badge-active'}">${r.is_force_update ? 'تحديث إجباري' : 'إصدار اختياري'}</span>
            </div>
            <div style="font-size: 12px; color: var(--text-muted); margin-bottom: 8px;">
              الحجم: ${sizeMb} MB • تاريخ الرفع: ${dateStr}
            </div>
            <div style="font-size: 13px; line-height: 1.6; margin-bottom: 10px; color: var(--text);">
              ${r.release_notes || 'بدون ملاحظات'}
            </div>
            <div style="display: flex; gap: 8px;">
              <input type="text" value="${r.download_url}" readonly style="font-size: 12px; padding: 6px 10px; flex: 1;">
              <button class="btn-sm btn-action" onclick="navigator.clipboard.writeText('${r.download_url}').then(()=>alert('تم نسخ رابط التحميل المباشر: ${r.download_url}'))">📋 نسخ الرابط</button>
              <button class="btn-sm btn-action" onclick="window.open('${r.download_url}', '_blank')">📥 تنزيل</button>
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
      statusDiv.innerHTML = `✅ تم نشر الإشعار والبث بنجاح إلى قاعدة البيانات ومتاح لجميع الأجهزة فور فتح التطبيق! (الأجهزة المسجلة بالتوكن: <b>${tokens.length}</b>)`;
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
    if (!confirm("هل تريد إلغاء الإعلان والتحديث الإجباري الحالي؟")) return;
    try {
      await firebaseFetch('app_broadcast.json', { method: "DELETE" });
      alert("تم إلغاء الإعلان بنجاح.");
      loadFirebaseData();
    } catch (e) {
      alert("خطأ: " + e.message);
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
      alert(`✅ تم حفظ السياسات بنجاح في السحابة!\n• الحد المجاني: ${freeLimit} عملية\n• النسخ للمجانيين: ${backupAllowed ? 'مسموح' : 'معطل'}\n• نسخ Drive: ${driveBackupAllowed ? 'مسموح' : 'معطل'}\nستطبق الهواتف هذه السياسات تلقائياً عند فتح التطبيق.`);
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
        <span style="font-weight:bold; font-size:15px; color:${isUpdate ? 'var(--danger)' : 'var(--accent)'};">${currentBroadcast.title}</span>
        <span class="badge ${isUpdate ? 'badge-blocked' : 'badge-active'}">${isUpdate ? 'تحديث إجباري وقفل' : 'إعلان توجيهي'}</span>
      </div>
      <div>${currentBroadcast.message || 'بدون تفاصيل'}</div>
      <div style="font-size:12px; color:var(--text-muted); margin-top:6px;">
        ${currentBroadcast.minRequiredVersionCode ? `الحد الأدنى للإصدار المسموح: <b>vCode ${currentBroadcast.minRequiredVersionCode}</b> • ` : ''}
        تاريخ النشر: ${pubDate}
      </div>
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
      try {
        const licRes = await firebaseFetch('licenses.json');
        if (licRes && licRes.ok) {
          const licData = await safeJson(licRes);
          const licObj = (licData && licData.data !== undefined) ? licData.data : licData;
          if (licObj && typeof licObj === 'object') {
            Object.values(licObj).forEach(lic => {
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
      const dateStr = r.timestamp ? new Date(r.timestamp).toLocaleString('ar-EG') : 'الآن';
      const cleanPhone = (r.phone || '').replace(/[^0-9]/g, '');
      const phoneLink = cleanPhone ? `<a href="https://wa.me/${cleanPhone}" target="_blank" style="color: var(--success); font-weight: bold;">${r.phone} 💬</a>` : '-';
      const planBadge = r.plan === 'YEARLY' ? `<span class="badge badge-yearly">سنوي 80$ ⭐</span>` : `<span class="badge badge-monthly">شهري 10$</span>`;
      const isCompleted = r.status === 'COMPLETED';
      const statusBadge = isCompleted ? `<span class="badge badge-active">تم التفعيل ✅</span>` : `<span class="badge badge-trial">جديد ⏳</span>`;
      const devCode = r.deviceCode || '-';
      const isWeb = r.requestType === 'WEB_DIRECT';
      const sourceBadge = isWeb 
        ? `<span class="badge" style="background:rgba(2, 132, 199, 0.15); color:#0284C7; border:1px solid rgba(2, 132, 199, 0.3);">الموقع 🌐</span>` 
        : `<span class="badge badge-active">التطبيق 📱</span>`;
      
      const regionText = r.region || 'غير محدد';
      const receiptText = r.receiptNumber || r.transferNumber || '-';
      const reqId = r.id || r.requestId || '';

      return `
        <tr>
          <td><small>${dateStr}</small></td>
          <td><b>${r.subscriberName || 'مشترك جديد'}</b></td>
          <td>${phoneLink}</td>
          <td>${planBadge}</td>
          <td>
            <div style="font-weight: 700; font-size: 13px; color: #FFF;">${regionText}</div>
            <div style="color: var(--accent); font-weight: 800; font-size: 11.5px; margin-top: 3px;">
              سند: <span style="background: rgba(0, 180, 216, 0.15); padding: 2px 6px; border-radius: 4px; border: 1px dashed var(--accent);">${receiptText}</span>
            </div>
            ${r.paymentMethod ? `<div style="color: #6EE7B7; font-weight: 700; font-size: 11px; margin-top: 3px;">عبر: <b>${r.paymentMethod}</b></div>` : ''}
          </td>
          <td><code style="color: var(--accent); font-weight: bold; font-size: 13px;">${devCode}</code></td>
          <td>${sourceBadge}</td>
          <td>${statusBadge}</td>
          <td>
            <div style="display: flex; gap: 6px; align-items: center;">
              ${!isCompleted 
                ? `<button class="btn-sm btn-unblock" onclick="quickActivateRequest('${devCode}', '${(r.subscriberName || '').replace(/'/g, "\\'")}', '${r.plan || 'YEARLY'}', '${reqId}')" title="تفعيل الاشتراك وتوليد الرمز">⚡ تفعيل</button>`
                : `<span style="font-size: 11.5px; color: var(--success); font-weight: bold; padding: 2px 6px;">منفّذ ✅</span>`
              }
              ${cleanPhone ? `<button class="btn-sm btn-outline" onclick="notifySubscriberWhatsApp('${cleanPhone}', '${(r.subscriberName || '').replace(/'/g, "\\'")}', '${receiptText}')" title="تأكيد استلام السند عبر واتساب">💬</button>` : ''}
              <button class="btn-sm" style="background: rgba(239, 68, 68, 0.15); color: #EF4444; border: 1px solid rgba(239, 68, 68, 0.35); padding: 4px 8px; font-size: 12px;" onclick="deleteSubscriptionRequest('${reqId}')" title="حذف هذا الطلب">🗑️ حذف</button>
            </div>
          </td>
        </tr>
      `;
    }).join('');
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
          <button type="button" class="btn-sm btn-delete" onclick="deleteAdminPaymentMethod(${idx})">حذف 🗑️</button>
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

    alert("✅ تمت إضافة الحساب إلى القائمة. اضغط على زر 'حفظ ونشر الحسابات في فايربيس' لاعتماده ونشره في السحابة والموقع والتطبيق.");
  }

  function deleteAdminPaymentMethod(idx) {
    if (!confirm("هل أنت متأكد من حذف هذا الحساب؟")) return;
    adminPaymentMethods.splice(idx, 1);
    renderAdminPaymentMethods();
  }

  async function saveAllPaymentMethods() {
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
      alert("✅ تم حفظ ونشر حسابات الدفع والمحافظ بنجاح في سحابة فايربيس!\nتظهر للعملاء في التطبيق والموقع فوراً.");
    } else {
      alert(`⚠️ حدث خطأ أثناء الحفظ في فايربيس: (${fbError || 'تعذر الاتصال'}). يرجى التأكد من مفتاح السحابة.`);
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
    if (!confirm("هل أنت متأكد من حذف هذا الطلب نهائياً من قاعدة البيانات؟")) return;

    try {
      await firebaseFetch(`subscription_requests/${reqId}.json`, { method: "DELETE" });

      try {
        await apiFetch('api/public_data.php', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            action: 'delete_subscription_request',
            request_id: reqId,
            csrf_token: currentCsrfToken
          })
        });
      } catch (_) {}

      allRequests = allRequests.filter(r => (r.id || r.requestId) !== reqId);
      updateStats();
      filterRequests();
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
      for (const r of completed) {
        const id = r.id || r.requestId;
        if (id) {
          await firebaseFetch(`subscription_requests/${id}.json`, { method: "DELETE" }).catch(() => {});
        }
      }

      try {
        await apiFetch('api/public_data.php', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            action: 'clear_completed_requests',
            csrf_token: currentCsrfToken
          })
        });
      } catch (_) {}

      allRequests = allRequests.filter(r => r.status !== 'COMPLETED');
      updateStats();
      filterRequests();
      alert("✅ تم حذف كافة الطلبات المنفذة بنجاح.");
    } catch (err) {
      alert("خطأ أثناء الحذف: " + err.message);
    }
  }

  function setUserFilter(filter) {
    currentUserFilter = filter;
    document.querySelectorAll('.pill-filter-btn').forEach(b => b.classList.remove('active'));
    const btn = document.getElementById(`filter${filter.charAt(0) + filter.slice(1).toLowerCase().replace(/_([a-z])/g, (_, l) => l.toUpperCase())}`);
    if (btn) btn.classList.add('active');
    filterUsers();
  }

  function renderUsersTable(users) {
    const tbody = document.getElementById("usersTableBody");
    if (!users || users.length === 0) {
      tbody.innerHTML = `<tr><td colspan="8" style="text-align: center; color: var(--text-muted);">لا يوجد مستخدمون مسجلون بعد (سيظهر كل من يفتح التطبيق هنا تلقائياً)</td></tr>`;
      return;
    }

    const globalLimit = appConfig.freeOperationsLimit || 200;

    // Counters for pill filters
    let countActive = 0, countTrial = 0, countLocked = 0, countBackupLocked = 0, countBlocked = 0;
    users.forEach(u => {
      const isBlocked = u.isBlocked === true;
      const isAct = u.isActivated === true && !isBlocked;
      const devKey = u.cleanDeviceKey || (u.deviceCode ? u.deviceCode.replace(/-/g, "") : "");
      const override = allOverrides[devKey] || {};
      const limit = override.customOperationsLimit || globalLimit;
      const ops = u.operationsCount || 0;
      const isLocked = !isAct && (ops >= limit || u.isLocked);
      const isBkLocked = override.isBackupLocked === true || (!isAct && appConfig.isBackupAllowedForTrial === false);

      if (isBlocked) countBlocked++;
      else if (isAct) countActive++;
      else countTrial++;

      if (isLocked) countLocked++;
      if (isBkLocked) countBackupLocked++;
    });

    if (document.getElementById("countFilterAll")) document.getElementById("countFilterAll").innerText = users.length;
    if (document.getElementById("countFilterActive")) document.getElementById("countFilterActive").innerText = countActive;
    if (document.getElementById("countFilterTrial")) document.getElementById("countFilterTrial").innerText = countTrial;
    if (document.getElementById("countFilterLocked")) document.getElementById("countFilterLocked").innerText = countLocked;
    if (document.getElementById("countFilterBackupLocked")) document.getElementById("countFilterBackupLocked").innerText = countBackupLocked;
    if (document.getElementById("countFilterBlocked")) document.getElementById("countFilterBlocked").innerText = countBlocked;

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

      if (currentUserFilter === 'ACTIVE') return isAct;
      if (currentUserFilter === 'TRIAL') return !isAct && !isBlocked;
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
      tbody.innerHTML = `<tr><td colspan="8" style="text-align: center; color: var(--text-muted); padding: 20px;">لا توجد نتائج مطابقة للفلتر المختار</td></tr>`;
      return;
    }

    tbody.innerHTML = filteredList.map(u => {
      const devKey = u.cleanDeviceKey || (u.deviceCode ? u.deviceCode.replace(/-/g, "") : "");
      const override = allOverrides[devKey] || {};
      const limit = override.customOperationsLimit || globalLimit;
      const ops = u.operationsCount || 0;
      const isBlocked = u.isBlocked === true;
      const isAct = u.isActivated === true && !isBlocked;
      const isOverLimit = !isAct && ops >= limit;
      const isDeviceBkLocked = override.isBackupLocked === true;

      // Status Badge
      let statusBadge;
      if (isBlocked) statusBadge = `<span class="badge badge-blocked">⛔ محظور</span>`;
      else if (isAct) statusBadge = `<span class="badge badge-active">✅ مشترك مفعّل</span>`;
      else if (isOverLimit) statusBadge = `<span class="badge badge-blocked">🔒 تجاوز الحد (مقفل)</span>`;
      else statusBadge = `<span class="badge badge-trial">⏳ تجريبي مجاني</span>`;

      // Plan Badge
      const planBadge = u.plan === "YEARLY" 
        ? `<span class="badge badge-yearly">سنوي ⭐</span>` 
        : (u.plan === "MONTHLY" ? `<span class="badge badge-monthly">شهري</span>` : `<span class="badge" style="background:#334155;color:#94a3b8;">تجربة</span>`);

      // Operations Progress Bar
      let pct = Math.min(100, Math.round((ops / limit) * 100));
      let barColor = isAct ? "var(--success)" : (ops >= limit ? "var(--danger)" : (pct >= 75 ? "var(--accent)" : "var(--primary-light)"));
      const customLimitNote = override.customOperationsLimit ? `<br><small style="color:var(--accent); font-weight:bold;">(حد خاص: ${override.customOperationsLimit})</small>` : '';
      const opsHtml = isAct 
        ? `<div><b>${ops}</b> عملية<br><small style="color:var(--success);">غير محدود (مشترك)</small></div>`
        : `
          <div>
            <b>${ops}</b> / ${limit} عملية (${pct}%)${customLimitNote}
            <div class="ops-bar-container">
              <div class="ops-bar-fill" style="width: ${pct}%; background: ${barColor};"></div>
            </div>
          </div>
        `;

      // Backup Status Badge
      let bkBadge;
      if (isDeviceBkLocked) bkBadge = `<span class="badge badge-blocked" title="تم قفل النسخ لهذا الجهاز بالاسم">🚫 مقفول للجهاز</span>`;
      else if (!isAct && appConfig.isBackupAllowedForTrial === false) bkBadge = `<span class="badge badge-trial" title="معطل عالمياً للمجانيين">⚠️ معطل للكل</span>`;
      else bkBadge = `<span class="badge badge-active">✅ متاح</span>`;

      const lastSeen = u.lastActiveAt ? new Date(u.lastActiveAt).toLocaleString('ar-EG') : 'غير محدد';
      const userName = u.subscriberName || u.googleDisplayName || 'مستخدم مسرّب';
      const userPhone = u.phone ? `<br><small style="color:#4ADE80; font-weight:600;">📞 ${u.phone}</small>` : '';
      const googleInfo = u.googleEmail 
        ? `<div><b>${userName}</b>${userPhone}<br><small style="color:#38BDF8;">${u.googleEmail}</small></div>`
        : `<div><b>${userName}</b>${userPhone}<br><span style="color:#64748B; font-size:11px;">بدون بريد جوجل</span></div>`;

      // Action Buttons
      const blockBtn = isBlocked
        ? `<button class="btn-sm btn-unblock" onclick="toggleBlockUser('${devKey}', '${u.googleEmail || ''}', false)">فك الحظر</button>`
        : `<button class="btn-sm btn-block" onclick="toggleBlockUser('${devKey}', '${u.googleEmail || ''}', true)">حظر ⛔</button>`;

      const bkLockBtn = isDeviceBkLocked
        ? `<button class="btn-sm btn-unblock" onclick="toggleDeviceBackupLock('${devKey}', false)">فتح النسخ 🔓</button>`
        : `<button class="btn-sm btn-outline" style="padding:4px 8px; font-size:11px;" onclick="toggleDeviceBackupLock('${devKey}', true)">قفل النسخ 🔒</button>`;

      const customLimitBtn = `<button class="btn-sm btn-outline" style="padding:4px 8px; font-size:11px;" onclick="promptSetCustomLimit('${devKey}', ${limit})">تعديل الحد ⚙️</button>`;
      const warnBtn = `<button class="btn-sm btn-outline" style="padding:4px 8px; font-size:11px;" onclick="promptSendDeviceWarning('${devKey}')">رسالة ⚠️</button>`;

      const warningBadge = override.specialWarningMessage ? `<div style="margin-top:4px;"><small style="color:var(--accent); font-size:10px;">رسالة موجهة: ${override.specialWarningMessage}</small></div>` : '';

      const priceTag = (u.pricePerHour || u.hourlyRate) 
        ? `<b style="color:var(--gold); font-size:13px;">${Number(u.pricePerHour || u.hourlyRate).toLocaleString('ar-EG')}</b> <small style="color:var(--text-muted);">ريال/س</small>`
        : `<span style="color:var(--text-muted); font-size:11.5px;">افتراضي (5000)</span>`;

      return `
        <tr onclick="openUserDetailModal('${devKey}')" title="انقر لعرض الملف الشامل، الرسائل الموجهة، وإرسال إشعار">
          <td>${googleInfo}</td>
          <td><code>${u.deviceCode || devKey}</code><br><small style="color:var(--text-muted);">${u.deviceModel || '-'}</small></td>
          <td>${priceTag}</td>
          <td>${statusBadge}${warningBadge}</td>
          <td>${planBadge}</td>
          <td>${opsHtml}</td>
          <td>${bkBadge}</td>
          <td><small>${lastSeen}</small></td>
          <td>
            <div class="btn-action-group" onclick="event.stopPropagation()">
              <button class="btn-sm btn-outline" style="padding:4px 8px; font-size:11px; background: rgba(0, 131, 143, 0.12); color: var(--primary);" onclick="event.stopPropagation(); openUserDetailModal('${devKey}')">تفاصيل 👤</button>
              ${blockBtn}
              ${bkLockBtn}
              ${customLimitBtn}
              ${warnBtn}
            </div>
          </td>
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

  async function generateAndSaveKey() {
    const devCode = document.getElementById("deviceCode").value.trim().toUpperCase();
    const subName = document.getElementById("subscriberName").value.trim();
    const plan = document.getElementById("planSelect").value;
    const prefix = plan === "MONTHLY" ? "M" : "Y";
    const durationDays = plan === "MONTHLY" ? 30 : 365;

    if (!devCode) {
      alert("يرجى كتابة كود جهاز العميل أولاً");
      return;
    }

    const cleanCode = devCode.replace(/-/g, "").trim();
    const hash = await sha256(cleanCode + SECRET_SALT + prefix);
    const part1 = hash.substring(0, 4);
    const part2 = hash.substring(4, 8);
    const key = `ACTV-${prefix}-${part1}-${part2}`;
    const cleanKey = key.replace(/-/g, "");

    const now = Date.now();
    const expiresAt = now + (durationDays * 24 * 3600 * 1000);

    const payload = {
      key: cleanKey,
      displayKey: key,
      deviceCode: devCode,
      subscriberName: subName || "مشترك",
      plan: plan,
      activatedAt: now,
      expiresAt: expiresAt,
      isRevoked: false,
      lastCheckedAt: now
    };

    let pushSuccess = false;
    let pushError = null;

    try {
      // 1. Save License to RTDB
      const res = await firebaseFetch(`licenses/${cleanKey}.json`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
      });
      if (res.ok) {
        pushSuccess = true;
      } else {
        pushError = `HTTP ${res.status} (${res.statusText || 'رفض الخادم'})`;
      }

      // 2. Also save to devices for fast device lookup
      await firebaseFetch(`devices/${devCode}.json`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
      }).catch(() => {});

      // 3. Mark subscription request as completed if applicable
      if (pendingRequestId) {
        await firebaseFetch(`subscription_requests/${pendingRequestId}/status.json`, {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify("COMPLETED")
        }).catch(() => {});

        await firebaseFetch(`subscription_requests/${pendingRequestId}/activationKey.json`, {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(key)
        }).catch(() => {});

        pendingRequestId = null;
      }
    } catch (e) {
      pushError = e.message;
    }

    document.getElementById("keyText").innerText = key;
    document.getElementById("keyBox").style.display = "block";

    if (pushSuccess) {
      alert(`✅ تم توليد وتفعيل الرمز بنجاح: ${key}\nتم رفعه وحفظه في سحابة فايربيس بنجاح وربطه بالجهاز (${devCode}).`);
    } else {
      alert(`⚠️ تم توليد الرمز محلياً: ${key}\nولكن حدث خطأ أثناء رفعه إلى فايربيس: (${pushError || 'تعذر الاتصال'}).\nالرابط الحالي: ${FIREBASE_BASE_URL}\nيمكنك النقر على زر سحابة فايربيس بأعلى الصفحة للتأكد من الرابط وقواعد الأمان.`);
    }

    loadFirebaseData();
  }

  function copyKey() {
    const key = document.getElementById("keyText").innerText;
    navigator.clipboard.writeText(key).then(() => alert("تم نسخ الكود بنجاح: " + key));
  }

  function sendToWhatsApp() {
    const key = document.getElementById("keyText").innerText;
    const name = document.getElementById("subscriberName").value.trim() || "عزيزنا المشترك";
    const plan = document.getElementById("planSelect").value === "MONTHLY" ? "اشتراك شهري (30 يوماً)" : "اشتراك سنوي (365 يوماً)";
    const text = `السلام عليكم ورحمة الله أخي ${name}،\nتم تفعيل اشتراكك في تطبيق المُسَرِّب (${plan}).\n\nرمز التفعيل لجهازك هو:\n${key}\n\nيرجى نسخه ولصقه في نافذة التفعيل بالتطبيق أثناء الاتصال بالإنترنت.`;
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
      const dateStr = new Date(f.timestamp || Date.now()).toLocaleString('ar-EG');
      const stars = "★".repeat(f.rating || 5) + "☆".repeat(5 - (f.rating || 5));
      const phoneLink = f.phone ? `<a href="https://wa.me/${f.phone.replace(/[^0-9]/g, '')}" target="_blank" style="color:var(--success); font-weight:bold;">${f.phone}</a>` : '-';

      return `
        <tr>
          <td><small>${dateStr}</small></td>
          <td><b>${f.subscriberName || 'مستخدم'}</b></td>
          <td>${phoneLink}</td>
          <td><span class="rating-stars">${stars}</span></td>
          <td><span class="badge badge-monthly">${f.category || 'اقتراح'}</span></td>
          <td style="max-width: 320px; line-height: 1.6;">${f.message}</td>
          <td><small>${f.deviceModel || f.deviceCode || '-'}</small></td>
          <td>
            ${f.phone ? `<button class="btn-sm btn-unblock" onclick="window.open('https://wa.me/${f.phone.replace(/[^0-9]/g, '')}', '_blank')">واتساب 💬</button>` : '-'}
          </td>
        </tr>
      `;
    }).join('');
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
    activeModalUserKey = cleanDeviceKey;
    const overlay = document.getElementById("userModalOverlay");
    if (!overlay) return;

    // Find user
    const u = allUsers.find(item => {
      const devKey = item.cleanDeviceKey || (item.deviceCode ? item.deviceCode.replace(/-/g, "") : "");
      return devKey === cleanDeviceKey;
    }) || { cleanDeviceKey: cleanDeviceKey, deviceCode: cleanDeviceKey };

    const override = allOverrides[cleanDeviceKey] || {};
    const limit = override.customOperationsLimit || globalLimit;
    const ops = u.operationsCount || 0;
    const isBlocked = u.isBlocked === true;
    const isAct = u.isActivated === true && !isBlocked;
    const isDeviceBkLocked = override.isBackupLocked === true;
    const userName = u.subscriberName || u.googleDisplayName || 'مستخدم مسرّب';

    // Set header
    document.getElementById("modalUserName").innerText = userName;
    document.getElementById("modalUserAvatar").innerText = isBlocked ? '⛔' : (isAct ? '⭐' : '👤');
    
    let statusBadge = isBlocked
      ? `<span class="badge badge-blocked">⛔ محظور</span>`
      : (isAct ? `<span class="badge badge-active">✅ مشترك مفعّل</span>` : `<span class="badge badge-trial">⏳ تجريبي</span>`);
    document.getElementById("modalUserStatusBadge").innerHTML = statusBadge;

    // Populate Info Pills
    document.getElementById("modalDeviceCode").innerHTML = `<code>${u.deviceCode || cleanDeviceKey}</code>`;
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
    document.getElementById("modalUserPlan").innerText = u.plan === "YEARLY" ? "سنوي ⭐" : (u.plan === "MONTHLY" ? "شهري" : "تجربة مجانية");
    document.getElementById("modalUserLastActive").innerText = u.lastActiveAt ? new Date(u.lastActiveAt).toLocaleString('ar-EG') : (u.lastSeen ? new Date(u.lastSeen).toLocaleString('ar-EG') : 'غير محدد');
    document.getElementById("modalUserOps").innerText = isAct ? `${ops} عملية (غير محدود - مشترك)` : `${ops} / ${limit} عملية`;
    document.getElementById("modalUserBackupStatus").innerHTML = isDeviceBkLocked 
      ? `<span style="color:var(--danger); font-weight:bold;">🚫 مقفول للجهاز</span>`
      : `<span style="color:var(--success); font-weight:bold;">✅ متاح</span>`;

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
    blockBtn.innerText = isBlocked ? "فك الحظر 🔓" : "حظر المستخدم ⛔";
    blockBtn.onclick = async () => {
      await toggleBlockUser(cleanDeviceKey, u.googleEmail || '', !isBlocked);
      openUserDetailModal(cleanDeviceKey);
    };

    const bkBtn = document.getElementById("modalBtnToggleBackup");
    bkBtn.innerText = isDeviceBkLocked ? "فتح النسخ 🔓" : "قفل النسخ 🔒";
    bkBtn.onclick = async () => {
      await toggleDeviceBackupLock(cleanDeviceKey, !isDeviceBkLocked);
      openUserDetailModal(cleanDeviceKey);
    };

    const limitBtn = document.getElementById("modalBtnSetLimit");
    limitBtn.onclick = async () => {
      await promptSetCustomLimit(cleanDeviceKey, limit);
      openUserDetailModal(cleanDeviceKey);
    };

    // Load targeted messages
    loadUserMessagesList(cleanDeviceKey, override);

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

  function switchUserModalTab(tabName) {
    ['profile', 'messages', 'push'].forEach(t => {
      const el = document.getElementById(`modalUserTab-${t}`);
      const btn = document.getElementById(`modalTabBtn${t.charAt(0).toUpperCase() + t.slice(1)}`);
      if (el) el.style.display = (t === tabName) ? "block" : "none";
      if (btn) btn.classList.toggle("active", t === tabName);
    });
  }

  async function loadUserMessagesList(cleanDeviceKey, override) {
    const tbody = document.getElementById("modalUserMessagesList");
    const countBadge = document.getElementById("modalMessagesCount");
    if (!tbody) return;

    let messages = [];

    try {
      const res = await firebaseFetch(`device_overrides/${cleanDeviceKey}/messages.json`);
      if (res && res.ok) {
        const data = await safeJson(res);
        const obj = (data && data.data !== undefined) ? data.data : data;
        if (obj && typeof obj === 'object') {
          Object.entries(obj).forEach(([mId, mVal]) => {
            if (mVal && typeof mVal === 'object') {
              messages.push({ id: mId, ...mVal });
            }
          });
        }
      }
    } catch (_) {}

    const legacyMsg = override.specialWarningMessage;
    if (messages.length === 0 && legacyMsg) {
      messages.push({
        id: 'legacy_warning',
        title: 'رسالة إدارية موجهة',
        text: legacyMsg,
        type: 'warning',
        displays: '0',
        active: true,
        createdAt: override.updatedAt || Date.now()
      });
    }

    if (countBadge) countBadge.innerText = messages.length;

    if (messages.length === 0) {
      tbody.innerHTML = `<tr><td colspan="6" style="text-align: center; color: var(--text-muted); padding: 18px;">لا توجد أي رسائل موجهة لهذا الجهاز حالياً</td></tr>`;
      return;
    }

    messages.sort((a, b) => (b.createdAt || 0) - (a.createdAt || 0));

    tbody.innerHTML = messages.map(m => {
      const typeIcons = { warning: '⚠️ تحذيري', info: 'ℹ️ إخباري', important: '📢 هام' };
      const typeBadge = typeIcons[m.type] || 'تنبيه';
      const dispText = m.displays == '1' ? 'مرة واحدة' : (m.displays == '3' ? '3 مرات' : 'دائم');
      const activeBadge = m.active !== false
        ? `<span class="badge badge-active">نشطة ومفعلة</span>`
        : `<span class="badge badge-blocked">معطلة</span>`;
      const dateStr = m.createdAt ? new Date(m.createdAt).toLocaleDateString('ar-EG') : 'الآن';

      const toggleAction = m.active !== false
        ? `<button class="btn-sm btn-outline" style="padding:3px 8px; font-size:10.5px;" onclick="toggleUserMessageState('${cleanDeviceKey}', '${m.id}', false)">تعطيل</button>`
        : `<button class="btn-sm btn-unblock" style="padding:3px 8px; font-size:10.5px;" onclick="toggleUserMessageState('${cleanDeviceKey}', '${m.id}', true)">تفعيل</button>`;

      const deleteBtn = `<button class="btn-sm btn-block" style="padding:3px 8px; font-size:10.5px;" onclick="deleteUserMessage('${cleanDeviceKey}', '${m.id}')">حذف</button>`;

      return `
        <tr>
          <td><small style="color:var(--accent);">${typeBadge}</small><br><b>${m.title || 'بدون عنوان'}</b></td>
          <td style="max-width: 260px; font-size: 12px; line-height: 1.4;">${m.text || ''}</td>
          <td><small>${dispText}</small></td>
          <td>${activeBadge}</td>
          <td><small>${dateStr}</small></td>
          <td>
            <div style="display:flex; gap:4px;">
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
    const title = (document.getElementById("newMsgTitle")?.value || "").trim();
    const text = (document.getElementById("newMsgText")?.value || "").trim();
    const type = document.getElementById("newMsgType")?.value || "warning";
    const displays = document.getElementById("newMsgDisplays")?.value || "0";

    if (!text) {
      return alert("يرجى كتابة نص الرسالة أولاً");
    }

    const msgId = "msg_" + Date.now();
    const msgPayload = {
      title: title || "تنبيه من الإدارة",
      text: text,
      type: type,
      displays: displays,
      active: true,
      createdAt: Date.now()
    };

    try {
      await firebaseFetch(`device_overrides/${activeModalUserKey}/messages/${msgId}.json`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(msgPayload)
      });

      await firebaseFetch(`device_overrides/${activeModalUserKey}/specialWarningMessage.json`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(text)
      });

      document.getElementById("newMsgTitle").value = "";
      document.getElementById("newMsgText").value = "";

      alert("تم حفظ الرسالة الموجهة وستظهر للمستخدم في هاتفه بنجاح!");
      loadFirebaseData();
      loadUserMessagesList(activeModalUserKey, allOverrides[activeModalUserKey] || {});
    } catch (e) {
      alert("حدث خطأ أثناء حفظ الرسالة: " + e.message);
    }
  }

  async function toggleUserMessageState(cleanDeviceKey, msgId, newState) {
    try {
      await firebaseFetch(`device_overrides/${cleanDeviceKey}/messages/${msgId}/active.json`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(newState)
      });

      if (!newState) {
        await firebaseFetch(`device_overrides/${cleanDeviceKey}/specialWarningMessage.json`, { method: "DELETE" });
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
      await firebaseFetch(`device_overrides/${cleanDeviceKey}/messages/${msgId}.json`, { method: "DELETE" });
      await firebaseFetch(`device_overrides/${cleanDeviceKey}/specialWarningMessage.json`, { method: "DELETE" });
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
      const devKey = item.cleanDeviceKey || (item.deviceCode ? item.deviceCode.replace(/-/g, "") : "");
      return devKey === activeModalUserKey;
    });
    const deviceCode = u?.deviceCode || activeModalUserKey;

    sendBtn.disabled = true;
    sendBtn.innerText = "جاري إرسال الإشعار...";
    statusEl.style.display = "block";
    statusEl.style.color = "var(--accent)";
    statusEl.innerText = "جاري التوصيل لمحرك Google FCM...";

    try {
      const res = await fetch('api/fcm_sender.php', {
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

      const data = await res.json();
      if (data && data.success) {
        statusEl.style.color = "var(--success)";
        statusEl.innerText = `✅ ${data.message || 'تم إرسال الإشعار بنجاح!'}`;
        alert("تم إرسال الإشعار لستارة هاتف المستخدم بنجاح!");
      } else {
        statusEl.style.color = "var(--danger)";
        statusEl.innerText = `❌ ${data.message || 'تعذر الإرسال'}`;
      }
    } catch (e) {
      statusEl.style.color = "var(--danger)";
      statusEl.innerText = `❌ خطأ في الاتصال: ${e.message}`;
    } finally {
      sendBtn.disabled = false;
      sendBtn.innerText = "🚀 إرسال الإشعار فوراً للهاتف";
    }
  }

  // Initial load
  initTheme();
  checkServerSession();
