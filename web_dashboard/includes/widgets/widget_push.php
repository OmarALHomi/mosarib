  <div id="tab-push" style="display: none;">
    <!-- FCM v1 Engine Status Banner -->
    <div class="card" style="margin-bottom: 20px; border: 1px solid var(--accent); background: linear-gradient(135deg, rgba(0, 131, 143, 0.15), rgba(11, 23, 29, 0.9));">
      <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px;">
        <div>
          <div style="font-weight: 800; font-size: 16px; display: flex; align-items: center; gap: 8px;">
            <span id="fcmEngineStatusBadge">⏳ جاري فحص محرك Google FCM HTTP v1...</span>
          </div>
          <div id="fcmEngineDetails" style="font-size: 12px; color: var(--text-muted); margin-top: 4px;">
            يفحص حالة ربط مفتاح الخدمة وإجمالي التوكنات المسجلة لهواتف العملاء...
          </div>
        </div>
        <div style="display: flex; gap: 8px; flex-wrap: wrap;">
          <button class="btn btn-secondary" style="font-size: 12px; padding: 6px 14px;" onclick="toggleServiceAccountModal()">
            ⚙️ إعداد / رفع مفتاح Google Service Account
          </button>
          <button class="btn btn-secondary" style="font-size: 12px; padding: 6px 14px;" onclick="testFcmConnection()">
            ⚡ اختبار الاتصال
          </button>
        </div>
      </div>

      <!-- Service Account Upload Panel (Collapsible) -->
      <div id="serviceAccountPanel" style="display: none; margin-top: 16px; padding-top: 16px; border-top: 1px solid rgba(255,255,255,0.1);">
        <h4 style="color: var(--accent); margin-bottom: 8px; font-size: 14px;">🔑 ربط مفتاح خدمة Google Service Account (FCM HTTP v1):</h4>
        <p style="font-size: 12px; color: #B2DFDB; line-height: 1.8; margin-bottom: 12px;">
          لإرسال إشعارات ستارة حقيقية ترن وتهتز في هواتف المزارعين حتى <b>لو كان التطبيق مغلقاً تماماً</b>، يتطلب Google FCM الحديث مفتاح خدمة (Private Key):
          <br>1. افتح <a href="https://console.firebase.google.com/project/mosarib/settings/serviceaccounts/adminsdk" target="_blank" style="color:var(--accent); text-decoration:underline;">Firebase Console > Service Accounts</a>.
          <br>2. اضغط على الزر الأزرق <b>"Generate new private key"</b> وحمّل ملف JSON.
          <br>3. ارفع الملف أدناه أو الصق محتواه واضغط "حفظ وتفعيل المفتاح".
        </p>

        <div style="display: flex; flex-direction: column; gap: 10px;">
          <div>
            <label style="font-size: 12px; font-weight: bold; margin-bottom: 4px; display: block;">اختيار ملف JSON من جهازك:</label>
            <input type="file" id="saFileInput" accept=".json" style="font-size: 12px; padding: 6px; width: 100%; border: 1px dashed var(--accent); border-radius: 6px; background: rgba(0,0,0,0.2);">
          </div>
          <div>
            <label style="font-size: 12px; font-weight: bold; margin-bottom: 4px; display: block;">أو لصق كود المفتاح JSON مباشرة:</label>
            <textarea id="saJsonInput" rows="4" placeholder='{ "type": "service_account", "project_id": "mosarib", "private_key": "-----BEGIN PRIVATE KEY-----...", "client_email": "..." }' style="width: 100%; font-family: monospace; font-size: 11px;"></textarea>
          </div>
          <div style="display: flex; gap: 8px;">
            <button class="btn" style="width: auto; padding: 9px 20px; font-weight: 700;" onclick="saveServiceAccountKey()">💾 حفظ المفتاح</button>
            <button class="btn btn-secondary" style="width: auto; padding: 9px 16px;" onclick="toggleServiceAccountModal()">إغلاق</button>
          </div>
          <div id="saSaveStatus" style="font-size: 12px; display: none;"></div>
        </div>
      </div>
    </div>

    <div class="grid-2">
      <div class="card">
        <div class="card-title">إرسال إشعار فوري لستارة الهاتف (FCM Push)</div>
        <div class="form-group">
          <label for="pushTarget">الجهة المستهدفة:</label>
          <select id="pushTarget">
            <option value="ALL">جميع الهواتف والمستخدمين المسجلين</option>
            <option value="TRIAL">مستخدمو النسخة التجريبية فقط</option>
            <option value="SUBSCRIBERS">المشتركون المفعّلون فقط</option>
          </select>
        </div>
        <div class="form-group">
          <label for="pushTitle">عنوان الإشعار:</label>
          <input type="text" id="pushTitle" placeholder="مثال: تنبيه هام من إدارة مُسَرِّب">
        </div>
        <div class="form-group">
          <label for="pushBody">نص الإشعار:</label>
          <textarea id="pushBody" rows="3" placeholder="اكتب نص الإشعار..."></textarea>
        </div>
        <div class="form-group">
          <label for="pushUrl">رابط يفتح عند الضغط على الإشعار (اختياري):</label>
          <input type="url" id="pushUrl" placeholder="مثال: رابط تحديث أو واتساب">
        </div>
        <button class="btn" id="btnSendPush" style="width: auto; padding: 10px 24px; font-weight: 700;" onclick="sendRealFcmPushNotification()">🚀 إرسال الإشعار</button>
        <div id="pushStatus" style="margin-top: 12px; font-size: 13px; text-align: center; display: none;"></div>
      </div>

      <div class="card">
        <div class="card-title">معلومات إشعارات FCM</div>
        <ul style="padding-right: 20px; line-height: 2; font-size: 13.5px; color: var(--text-muted);">
          <li><b>تصل والتطبيق مغلق:</b> يستقبل هاتف العميل الإشعار في الستارة العلوية عبر Google FCM مباشرة.</li>
          <li><b>تخزين سحابي:</b> يحفظ الإشعار في قاعدة البيانات ليظهر أيضاً داخل صندوق الوارد بالتطبيق.</li>
          <li><b>استهداف مرن:</b> إمكانية الإرسال لكافة المستخدمين أو لمجموعة محددة.</li>
        </ul>
      </div>
    </div>
  </div>

  <!-- Tab 2: Generator -->
