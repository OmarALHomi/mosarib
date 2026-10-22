<!-- User Deep Profile & Targeted Messages Modal (2026 Edition) -->
<div id="userModalOverlay" class="modal-overlay" onclick="closeUserDetailModal(event)">
  <div class="user-modal" onclick="event.stopPropagation()">
    <!-- Modal Header -->
    <div class="modal-header">
      <h2>
        <span id="modalUserAvatar" style="font-size: 22px;">👤</span>
        <span id="modalUserName">تفاصيل المستخدم</span>
        <span id="modalUserStatusBadge"></span>
      </h2>
      <button class="modal-close-btn" onclick="closeUserDetailModal()" title="إغلاق">&times;</button>
    </div>

    <!-- Modal Sub-Tabs -->
    <div style="padding: 14px 24px 0 24px; background: rgba(0, 131, 143, 0.02);">
      <div class="modal-subtabs">
        <button class="modal-subtab-btn active" id="modalTabBtnProfile" onclick="switchUserModalTab('profile')">
          📋 بيانات الهاتف والاشتراك
        </button>
        <button class="modal-subtab-btn" id="modalTabBtnMessages" onclick="switchUserModalTab('messages')">
          💬 التنبيهات والرسائل الموجهة (<span id="modalMessagesCount">0</span>)
        </button>
        <button class="modal-subtab-btn" id="modalTabBtnPush" onclick="switchUserModalTab('push')">
          🚀 إرسال إشعار فوري (Push)
        </button>
      </div>
    </div>

    <!-- Modal Body -->
    <div class="modal-body">
      <!-- 1. Profile & Device Tab -->
      <div id="modalUserTab-profile">
        <!-- Quick Action Bar -->
        <div style="display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 18px; padding-bottom: 14px; border-bottom: 1px solid var(--card-border);">
          <button class="btn btn-outline" id="modalBtnWhatsApp" style="width: auto; padding: 7px 14px; font-size: 12px; display: inline-flex; align-items: center; gap: 6px;">
            <span>🟢</span> مراسلة عبر واتساب
          </button>
          <button class="btn btn-outline" id="modalBtnToggleBlock" style="width: auto; padding: 7px 14px; font-size: 12px;">
            ⛔ حظر / فك الحظر
          </button>
          <button class="btn btn-outline" id="modalBtnToggleBackup" style="width: auto; padding: 7px 14px; font-size: 12px;">
            🔒 قفل / فتح النسخ
          </button>
          <button class="btn btn-outline" id="modalBtnSetLimit" style="width: auto; padding: 7px 14px; font-size: 12px;">
            ⚙️ تعديل الحد المخصص
          </button>
        </div>

        <!-- Info Grid -->
        <div class="info-pill-grid">
          <div class="info-pill">
            <label>كود الجهاز الفريد (Device Code):</label>
            <div class="val" id="modalDeviceCode">-</div>
          </div>
          <div class="info-pill">
            <label>موديل ونوع الهاتف:</label>
            <div class="val" id="modalDeviceModel">-</div>
          </div>
          <div class="info-pill">
            <label>إصدار نظام Android:</label>
            <div class="val" id="modalAndroidVersion">-</div>
          </div>
          <div class="info-pill">
            <label>رقم الهاتف المسجل:</label>
            <div class="val" id="modalUserPhone">-</div>
          </div>
          <div class="info-pill">
            <label>حساب Google المسجل:</label>
            <div class="val" id="modalUserEmail">-</div>
          </div>
          <div class="info-pill">
            <label>سعر الساعة للمسرب (ريال/س):</label>
            <div class="val" id="modalUserPrice" style="color: var(--accent);">-</div>
          </div>
          <div class="info-pill">
            <label>نوع الباقة والاشتراك:</label>
            <div class="val" id="modalUserPlan">-</div>
          </div>
          <div class="info-pill">
            <label>تاريخ آخر نشاط / مزامنة:</label>
            <div class="val" id="modalUserLastActive">-</div>
          </div>
          <div class="info-pill">
            <label>استهلاك العمليات والحد الأقصى:</label>
            <div class="val" id="modalUserOps">-</div>
          </div>
          <div class="info-pill">
            <label>حالة النسخ الاحتياطي للجهاز:</label>
            <div class="val" id="modalUserBackupStatus">-</div>
          </div>
        </div>
      </div>

      <!-- 2. Targeted Messages Manager Tab -->
      <div id="modalUserTab-messages" style="display: none;">
        <!-- Add / Edit Message Accordion / Box -->
        <div class="card" id="userMsgFormCard" style="margin-bottom: 18px; padding: 16px; border: 1px dashed var(--primary); background: rgba(0, 131, 143, 0.03); transition: all 0.25s ease;">
          <input type="hidden" id="editingMsgId" value="">
          <div id="userMsgFormTitle" style="font-size: 13.5px; font-weight: 800; color: var(--primary); margin-bottom: 10px; display: flex; align-items: center; justify-content: space-between;">
            <div style="display: flex; align-items: center; gap: 6px;">
              <span id="userMsgFormIcon">➕</span> <span id="userMsgFormHeading">إضافة رسالة أو تنبيه موجه لهذا الجهاز</span>
            </div>
            <button id="btnCancelEditMsg" type="button" class="btn-sm btn-outline" style="display:none; font-size:11.5px; padding:3px 10px; color:var(--text-muted); border-color:var(--border);" onclick="cancelEditUserMessage()">إلغاء التعديل ✖</button>
          </div>
          <div style="display: flex; flex-direction: column; gap: 10px;">
            <div style="display: grid; grid-template-columns: 2fr 1fr; gap: 10px;">
              <input type="text" id="newMsgTitle" placeholder="عنوان التنبيه (مثال: تنبيه هام من الإدارة)" style="font-size: 13px;">
              <select id="newMsgType" style="font-size: 13px;">
                <option value="warning">⚠️ تحذيري (Warning)</option>
                <option value="info">ℹ️ إخباري (Info)</option>
                <option value="important">📢 إشعار هام (Critical)</option>
              </select>
            </div>
            <textarea id="newMsgText" rows="3" placeholder="اكتب نص الرسالة التي ستظهر للمستخدم في التطبيق..." style="font-size: 13px;"></textarea>
            <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 10px;">
              <div style="display: flex; align-items: center; gap: 8px;">
                <label style="font-size: 12px; margin: 0; font-weight: bold;">مرات العرض:</label>
                <select id="newMsgDisplays" style="width: auto; padding: 5px 10px; font-size: 12px;">
                  <option value="1">مرة واحدة فقط (يختفي بعد الفتح)</option>
                  <option value="3">3 مرات</option>
                  <option value="0" selected>دائم (حتى يقوم الإدمن بإلغائه)</option>
                </select>
              </div>
              <button class="btn" id="btnSaveUserMsg" style="width: auto; padding: 8px 18px; font-size: 13px;" onclick="saveNewUserMessage()">
                💾 حفظ وإرسال للجهاز
              </button>
            </div>
          </div>
        </div>

        <!-- Messages Table -->
        <div style="font-size: 14px; font-weight: 800; color: var(--text-title); margin-bottom: 10px;">
          سجل التنبيهات الموجهة لهذا الجهاز:
        </div>
        <div class="table-container" style="max-height: 250px;">
          <table>
            <thead>
              <tr>
                <th>النوع والعنوان</th>
                <th>نص الرسالة</th>
                <th>مرات العرض</th>
                <th>الحالة</th>
                <th>تاريخ الإرسال</th>
                <th>إجراءات</th>
              </tr>
            </thead>
            <tbody id="modalUserMessagesList">
              <tr><td colspan="6" style="text-align: center; color: var(--text-muted);">لا توجد رسائل موجهة لهذا المستخدم حالياً</td></tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 3. Direct FCM Push Tab -->
      <div id="modalUserTab-push" style="display: none;">
        <div class="card" style="padding: 16px; border: 1px solid var(--card-border);">
          <div style="font-size: 14px; font-weight: 800; color: var(--text-title); margin-bottom: 6px;">
            🚀 إرسال إشعار فوري لستارة هاتف هذا المستخدم (Google FCM)
          </div>
          <p style="font-size: 12px; color: var(--text-muted); margin-bottom: 14px; line-height: 1.6;">
            هذا الإشعار يظهر مباشرة في ستارة إشعارات هاتف هذا الجهاز ويهتز ويرن حتى لو كان التطبيق مغلقاً كلياً.
          </p>

          <div class="form-group">
            <label for="userPushTitle">عنوان الإشعار:</label>
            <input type="text" id="userPushTitle" placeholder="مثال: مرحباً بك في مُسَرِّب">
          </div>
          <div class="form-group">
            <label for="userPushBody">نص الإشعار:</label>
            <textarea id="userPushBody" rows="3" placeholder="اكتب نص الإشعار الذي سيظهر في شريط هاتف المستخدم..."></textarea>
          </div>
          <div class="form-group">
            <label for="userPushUrl">رابط يفتح عند النقر (اختياري):</label>
            <input type="url" id="userPushUrl" placeholder="مثال: https://mosarib.com أو رابط واتساب">
          </div>
          <button class="btn" id="btnSendDirectPush" style="width: auto; padding: 9px 24px; font-weight: 700;" onclick="sendDirectUserPushNotification()">
            🚀 إرسال الإشعار فوراً للهاتف
          </button>
          <div id="userPushStatus" style="margin-top: 10px; font-size: 12.5px; display: none;"></div>
        </div>
      </div>
    </div>
  </div>
</div>
