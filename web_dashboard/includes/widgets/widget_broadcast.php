  <div id="tab-broadcast" style="display: none;">
    <!-- Active Broadcast Preview Card -->
    <div class="card" id="currentBroadcastCard" style="margin-bottom: 20px; border: 1px solid var(--accent); display: none;">
      <div class="card-title" style="color: var(--accent); margin-bottom: 8px;">الإعلان أو التحديث النشط حالياً على جميع الهواتف</div>
      <div id="currentBroadcastContent" style="font-size: 13.5px; line-height: 1.8; color: var(--text);"></div>
      <div style="display: flex; gap: 10px; margin-top: 12px;">
        <button class="btn btn-outline" style="width: auto; padding: 6px 14px; font-size: 12.5px; color: var(--danger); border-color: var(--danger);" onclick="cancelBroadcast()">إلغاء وتصفير هذا الإعلان فوراً</button>
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
          <label for="bcMinVersion">الحد الأدنى لرقم الإصدار المسموح به (VersionCode):</label>
          <input type="number" id="bcMinVersion" value="2" min="1">
          <small style="color: var(--text-muted); font-size: 11px;">أي هاتف لديه VersionCode أقل من هذا الرقم سيُقفل عليه التطبيق فوراً ويطالبه بالتحديث.</small>
        </div>
        <div class="form-group">
          <label for="bcMessage">نص الإشعار / رسالة التحديث:</label>
          <textarea id="bcMessage" rows="3" placeholder="اكتب التفاصيل والميزات الجديدة أو التوجيهات..."></textarea>
        </div>
        <div class="form-group">
          <label for="bcUrl">رابط التحديث أو التحميل (اختياري):</label>
          <input type="url" id="bcUrl" placeholder="مثال: https://mosarib.com/download أو رابط واتساب">
        </div>
        <div class="form-group">
          <label for="bcBtnText">نص الزر:</label>
          <input type="text" id="bcBtnText" value="تحديث التطبيق الآن">
        </div>
        <button class="btn" onclick="publishBroadcast()">نشر الإشعار / قفل النسخ القديمة لجميع الهواتف</button>
        <button class="btn btn-outline" style="margin-top: 10px;" onclick="cancelBroadcast()">إلغاء وتصفير الإعلان الحالي</button>
      </div>

      <div class="card">
        <div class="card-title">كيف يعمل التحديث الإجباري والإعلانات؟</div>
        <ul style="padding-right: 20px; line-height: 2.2; font-size: 14px; color: #B2DFDB;">
          <li><b>الإعلان العام (INFO/WARNING):</b> يظهر كنافذة أنيقة للمستخدمين لمرة واحدة فقط عند فتح التطبيق، ولا يمنعهم من العمل.</li>
          <li><b>التحديث الإجباري (Force Update):</b> إذا اخترت "تحديث إجباري" وحددت VersionCode أعلى من النسخ القديمة، فإن أي شخص يفتح النسخة القديمة <b>ستُقفل الشاشة في وجهه تماماً</b> ولا يمكنه الدخول أو فتح الحسابات حتى يحمّل التحديث الجديد عبر الرابط المحدد!</li>
          <li><b>تحكم فوري:</b> يتم تفعيل القفل فوراً لحظة اتصال هاتف العميل بالإنترنت.</li>
        </ul>
      </div>
    </div>
  </div>

  <!-- Tab: Push Notifications -->
