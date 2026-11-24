  <div id="tab-broadcast" style="display: none;">
    <!-- Active Broadcast Preview Card -->
    <div class="card" id="currentBroadcastCard" style="margin-bottom: 20px; border: 1px solid var(--accent); display: none;">
      <div class="card-title" style="color: var(--accent); margin-bottom: 8px;">الإعلان أو التحديث النشط حالياً على جميع الهواتف</div>
      <div id="currentBroadcastContent" style="font-size: 13.5px; line-height: 1.8; color: var(--text);"></div>
      <div style="display: flex; gap: 10px; margin-top: 12px;">
        <button class="btn btn-outline" style="width: auto; padding: 6px 14px; font-size: 12.5px; color: var(--danger); border-color: var(--danger);" onclick="cancelBroadcast()">❌ إلغاء الإعلان</button>
      </div>
    </div>

    <div class="grid-2">
      <div class="card">
        <div class="card-title">إرسال إعلان عام أو تفعيل التحديث الإجباري</div>
        <div class="form-group">
          <label for="bcTitle">عنوان الإعلان / التنبيه:</label>
          <input type="text" id="bcTitle" placeholder="مثال: تحديث إجباري هام للإصدار v1.1.0">
        </div>
        <div class="form-group">
          <label for="bcType">نوع الإشعار:</label>
          <select id="bcType" onchange="toggleUpdateInputs()">
            <option value="INFO">رسالة توجيهية / إعلان عام</option>
            <option value="WARNING">تنبيه فني هام</option>
            <option value="UPDATE">تحديث إجباري وقفل النسخ القديمة</option>
          </select>
        </div>
        <div class="form-group" id="versionGroup" style="display: none;">
          <label style="display:flex; align-items:center; gap:8px; font-weight:normal; cursor:pointer;">
            <input type="radio" name="bcForceMode" value="OLDER" checked style="width:auto;" onchange="refreshBroadcastAudience()"> كل الإصدارات الأقدم من:
          </label>
          <input type="number" id="bcMinVersion" value="2" min="1" oninput="refreshBroadcastAudience()">
          <small style="color: var(--text-muted); font-size: 11px;">أي هاتف لديه VersionCode أقل من هذا الرقم سيُقفل عليه التطبيق فوراً ويطالبه بالتحديث.</small>
          <label style="display:flex; align-items:center; gap:8px; font-weight:normal; cursor:pointer; margin-top:10px;">
            <input type="radio" name="bcForceMode" value="SPECIFIC" style="width:auto;" onchange="refreshBroadcastAudience()"> إصدارات محددة فقط (مثلاً نسخة فيها خلل):
          </label>
          <input type="text" id="bcForceCodes" placeholder="أرقام البناء مفصولة بفاصلة، مثل: 4, 5" oninput="refreshBroadcastAudience()">
          <small style="color: var(--text-muted); font-size: 11px;">يعمل خيار «إصدارات محددة» على الإصدار 1.4.3 (بناء 8) فما بعده فقط.</small>
          <div id="bcForceImpact" style="font-size:12.5px; color:var(--danger); font-weight:700; margin-top:8px;"></div>
        </div>
        <div class="form-group" id="bcAudienceGroup">
          <label for="bcAudienceMode">الفئة المستهدفة للإعلان:</label>
          <select id="bcAudienceMode" onchange="refreshBroadcastAudience()">
            <option value="ALL">كل الإصدارات</option>
            <option value="EQ">إصدار محدد</option>
            <option value="LT">أقدم من</option>
            <option value="RANGE">من … إلى</option>
          </select>
          <div style="display:flex; gap:8px; margin-top:8px;">
            <input type="number" id="bcAudienceFrom" min="1" placeholder="رقم البناء" oninput="refreshBroadcastAudience()">
            <input type="number" id="bcAudienceTo" min="1" placeholder="إلى (للمدى فقط)" oninput="refreshBroadcastAudience()">
          </div>
          <div id="bcAudienceCount" style="font-size:12.5px; color:var(--accent); font-weight:700; margin-top:8px;"></div>
          <small style="color: var(--text-muted); font-size: 11px;">الاستهداف حسب الإصدار يعمل على الإصدار 1.4.3 (بناء 8) فما بعده؛ الإصدارات الأقدم ترى الإعلان دائماً.</small>
        </div>
        <div id="bcVersionUsage" style="font-size:12px; color:var(--text-muted); margin:-6px 0 14px; line-height:1.8;"></div>
        <div class="form-group">
          <label for="bcMessage">نص الإشعار / رسالة التحديث:</label>
          <textarea id="bcMessage" rows="3" placeholder="اكتب التفاصيل والميزات الجديدة أو التوجيهات..."></textarea>
        </div>
        <div class="form-group">
          <label for="bcUrl">رابط التحديث أو التحميل (اختياري):</label>
          <input type="text" id="bcUrl" placeholder="مثال: https://mosarib.com/download أو mosarib://settings">
        </div>
        <div class="form-group">
          <label for="bcImageUrl">صورة مع الإعلان (اختياري):</label>
          <div style="display:flex; gap:8px; flex-wrap:wrap; align-items:center;">
            <input type="url" id="bcImageUrl" placeholder="https://... أو ارفع صورة" style="flex:1; min-width:200px;" oninput="previewNoticeImage('bc')">
            <input type="file" id="bcImageFile" accept="image/jpeg,image/png,image/webp" style="display:none;" onchange="uploadNoticeImage('bc')">
            <button type="button" class="btn btn-outline" style="width:auto; padding:8px 14px;" onclick="document.getElementById('bcImageFile').click()">🖼️ رفع صورة</button>
          </div>
          <small style="color: var(--text-muted); font-size: 11px;">JPG أو PNG أو WEBP، حتى 1 ميجابايت.</small>
          <img id="bcImagePreview" alt="" style="display:none; margin-top:8px; max-width:100%; max-height:160px; border-radius:12px; object-fit:cover;">
        </div>

        <div class="form-group">
          <label for="bcBtnText">نص الزر:</label>
          <input type="text" id="bcBtnText" value="تحديث التطبيق الآن">
        </div>
        <div style="display: flex; gap: 10px; flex-wrap: wrap;">
          <button class="btn" style="width: auto; padding: 10px 24px; font-weight: 700;" onclick="publishBroadcast()">📢 نشر</button>
          <button class="btn btn-outline" style="width: auto; padding: 10px 18px;" onclick="cancelBroadcast()">❌ إلغاء</button>
        </div>
      </div>

      <div class="card">
        <div class="card-title">معلومات التحديث والإعلانات</div>
        <ul style="padding-right: 20px; line-height: 2; font-size: 13.5px; color: var(--text-muted);">
          <li><b>الإعلان العام (INFO/WARNING):</b> يظهر كنافذة للمستخدمين لمرة واحدة عند فتح التطبيق دون إيقاف العمل.</li>
          <li><b>التحديث الإجباري (Force Update):</b> يقفل التطبيق على الإصدارات الأقدم ويلزم المستخدم بالتحديث عبر الرابط المحدد.</li>
          <li><b>تطبيق فوري:</b> تسري التغييرات بمجرد اتصال الهاتف بالإنترنت.</li>
        </ul>
      </div>
    </div>
  </div>

  <!-- Tab: Push Notifications -->
