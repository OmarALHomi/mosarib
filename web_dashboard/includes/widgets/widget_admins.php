  <div id="tab-admins" style="display: none;">
    <div class="grid-2">
      <div class="card" style="border: 1px solid rgba(0, 188, 212, 0.4);">
        <div class="card-title">إضافة مسؤول نظام جديد</div>
        <p style="font-size: 12.5px; color: var(--text-muted); margin-bottom: 16px;">
          لن يتمكن أي شخص من فتح الموقع أو التحكم بالتطبيق إلا إذا كان مسجلاً بالاسم والبريد وكلمة المرور المشفرة.
        </p>
        <form id="newAdminForm" onsubmit="handleCreateAdmin(event)">
          <div class="form-group">
            <label for="newAdminName">اسم المسؤول بالكامل:</label>
            <input type="text" id="newAdminName" placeholder="مثال: المهندس أحمد" required>
          </div>
          <div class="form-group">
            <label for="newAdminEmail">البريد الإلكتروني المعتمد:</label>
            <input type="email" id="newAdminEmail" placeholder="ahmed@example.com" required>
          </div>
          <div class="form-group">
            <label for="newAdminPassword">كلمة المرور (8 خانات على الأقل):</label>
            <input type="password" id="newAdminPassword" placeholder="••••••••••••" minlength="8" required>
          </div>
          <div class="form-group">
            <label for="newAdminRole">الصلاحية والرتبة الإدارية:</label>
            <select id="newAdminRole">
              <option value="ADMIN">مسؤول إدارة وتراخيص (Admin)</option>
              <option value="SUPER_ADMIN">مسؤول رئيسي بصلاحيات مطلقة (Super Admin)</option>
              <option value="MODERATOR">مشرف متابعة وقراءة فقط (Moderator)</option>
            </select>
          </div>
          <button type="submit" class="btn" id="createAdminBtn" style="width: auto; padding: 10px 24px; font-weight: 700;">
            ➕ إضافة مسؤول
          </button>
        </form>
      </div>

      <div class="card" style="border: 1px solid rgba(0, 150, 166, 0.3);">
        <div class="card-title">قائمة المسؤولين المعتمدين</div>
        <div id="adminsListContainer" style="max-height: 480px; overflow-y: auto;">
          <div style="text-align: center; color: var(--text-muted); padding: 24px;">جاري تحميل مسؤولي النظام...</div>
        </div>
      </div>
    </div>
  </div>

  <!-- Tab: Public Portal Management (التحكم بأسعار المياه وأخبار وزوار الموقع العام) -->
