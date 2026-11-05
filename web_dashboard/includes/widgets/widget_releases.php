  <div id="tab-releases" style="display: none;">
    <div class="grid-2">
      <div class="card">
        <div class="card-title">رفع نسخة APK جديدة وإنشاء رابط تحميل مباشر</div>
        <form id="apkUploadForm" onsubmit="handleApkUpload(event)">
          <div class="form-group">
            <label for="apkFile">اختيار ملف التطبيق (APK):</label>
            <input type="file" id="apkFile" accept=".apk" required style="padding: 10px;">
          </div>
          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px;">
            <div class="form-group">
              <label for="apkVersionName">اسم الإصدار (VersionName):</label>
              <input type="text" id="apkVersionName" placeholder="مثال: 1.0.1" required>
            </div>
            <div class="form-group">
              <label for="apkVersionCode">رقم الإصدار (VersionCode):</label>
              <input type="number" id="apkVersionCode" placeholder="مثال: 2" min="1" required>
            </div>
          </div>
          <div class="form-group">
            <label for="apkReleaseNotes">ملاحظات الإصدار الجديد (الميزات والإصلاحات):</label>
            <textarea id="apkReleaseNotes" rows="3" placeholder="اكتب ما تم إضافته أو إصلاحه في هذا التحديث..."></textarea>
          </div>
          <div class="form-group" style="display: flex; align-items: center; gap: 10px;">
            <input type="checkbox" id="apkForceUpdate" style="width: auto;">
            <label for="apkForceUpdate" style="margin: 0; cursor: pointer; color: var(--danger); font-weight: bold;">
              تفعيل كتحديث إجباري وقفل كافة النسخ الأقدم فوراً في فايربيس
            </label>
          </div>
          <div class="form-group" id="apkForceOptions" style="display:none; padding:12px; border:1px dashed var(--danger); border-radius:10px;">
            <label style="display:flex; align-items:center; gap:8px; font-weight:normal; cursor:pointer;">
              <input type="radio" name="apkForceMode" value="OLDER" checked style="width:auto;" onchange="refreshApkForceImpact()"> كل الإصدارات الأقدم من الإصدار الجديد
            </label>
            <label style="display:flex; align-items:center; gap:8px; font-weight:normal; cursor:pointer; margin-top:8px;">
              <input type="radio" name="apkForceMode" value="SPECIFIC" style="width:auto;" onchange="refreshApkForceImpact()"> إصدارات محددة فقط:
            </label>
            <input type="text" id="apkForceCodes" placeholder="أرقام البناء مفصولة بفاصلة، مثل: 4, 5" oninput="refreshApkForceImpact()">
            <small style="color: var(--text-muted); font-size: 11px;">«إصدارات محددة» يعمل على الإصدار 1.4.3 (بناء 8) فما بعده فقط.</small>
            <div id="apkForceImpact" style="font-size:12.5px; color:var(--danger); font-weight:700; margin-top:8px;"></div>
          </div>
          <button type="submit" class="btn" id="uploadApkBtn" style="width: auto; padding: 10px 24px; font-weight: 700;">📤 رفع التحديث</button>
          <div id="uploadProgress" style="display: none; margin-top: 14px; text-align: center;">
            <div style="font-size: 13px; color: var(--accent);">جاري رفع ملف الـ APK وتثبيته على السيرفر...</div>
          </div>
        </form>

        <hr style="border: 0; border-top: 1px solid var(--border); margin: 24px 0;">

        <div class="card-title" style="font-size: 15px;">أو ربط رابط تحميل خارجي مباشر (Google Drive / MediaFire)</div>
        <div class="form-group">
          <label for="externalApkUrl">رابط التحميل المباشر:</label>
          <input type="url" id="externalApkUrl" placeholder="https://example.com/mosarib.apk">
        </div>
        <button class="btn btn-outline" style="width: auto; padding: 10px 20px;" onclick="setExternalCloudLink()">🔗 نشر الرابط</button>
      </div>

      <div class="card">
        <div class="card-title">سجل الإصدارات وروابط التحميل المباشرة</div>
        <div id="releasesListContainer" style="max-height: 480px; overflow-y: auto;">
          <div style="text-align: center; color: var(--text-muted); padding: 24px;">جاري تحميل سجل الإصدارات...</div>
        </div>
      </div>
    </div>
  </div>

  <!-- Tab: Cloud App Policies (إعدادات وسياسات التطبيق السحابية) -->
