<!-- Royal Security Authentication Modal / Screen -->
<div id="authScreenOverlay" style="position: fixed; inset: 0; z-index: 999999; background: radial-gradient(circle at center, #132A32 0%, #070E12 100%); display: flex; align-items: center; justify-content: center; padding: 20px;">
  <div style="background: rgba(18, 35, 43, 0.95); backdrop-filter: blur(16px); -webkit-backdrop-filter: blur(16px); border: 1px solid rgba(0, 188, 212, 0.35); border-radius: 20px; box-shadow: 0 20px 60px rgba(0,0,0,0.8), 0 0 40px rgba(0, 188, 212, 0.15); width: 100%; max-width: 460px; padding: 36px 30px; text-align: center; color: #E0F2F1;">
    <div style="display: flex; justify-content: center; margin-bottom: 16px;">
      <div style="width: 52px; height: 52px; border-radius: 14px; background: linear-gradient(135deg, #00838F, #004D40); display: flex; align-items: center; justify-content: center; color: #80DEEA; border: 1px solid rgba(0, 188, 212, 0.3);">
        <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z"></path>
        </svg>
      </div>
    </div>
    <h2 style="font-size: 22px; font-weight: 800; color: #FFF; margin: 0 0 6px 0;">بوابة الدخول المحمية • مُسَرِّب</h2>
    <p style="font-size: 13px; color: #80CBC4; margin: 0 0 24px 0; line-height: 1.6;">لوحة الإدارة مشفرة بحماية أمنية (Bcrypt + Rate-Limit). لا يُسمح بالدخول إلا لمسؤولي النظام المعتمدين.</p>
    
    <div id="authErrorBanner" style="display: none; background: rgba(229, 57, 53, 0.15); border: 1px solid #E53935; color: #FFCDD2; font-size: 12.5px; border-radius: 10px; padding: 10px; margin-bottom: 18px; text-align: right; line-height: 1.5;"></div>

    <form id="secureLoginForm" onsubmit="handleSecureLogin(event)" style="text-align: right;">
      <input type="hidden" id="loginCsrfToken" value="">
      
      <div class="form-group" style="margin-bottom: 16px;">
        <label for="adminEmailInput" style="font-size: 13px; color: #B2DFDB; font-weight: 700; margin-bottom: 6px; display: block;">البريد الإلكتروني المعتمد:</label>
        <div style="position: relative;">
          <input type="email" id="adminEmailInput" placeholder="أدخل بريدك الإلكتروني المعتمد" required style="width: 100%; padding: 12px 14px; background: rgba(9, 18, 23, 0.85); border: 1px solid rgba(0, 150, 166, 0.4); border-radius: 10px; color: #FFF; font-size: 14px; font-family: 'Cairo';">
        </div>
      </div>

      <div class="form-group" style="margin-bottom: 22px;">
        <label for="adminPasswordInput" style="font-size: 13px; color: #B2DFDB; font-weight: 700; margin-bottom: 6px; display: block;">كلمة المرور المشفرة:</label>
        <div style="position: relative;">
          <input type="password" id="adminPasswordInput" placeholder="••••••••••••" required style="width: 100%; padding: 12px 14px; background: rgba(9, 18, 23, 0.85); border: 1px solid rgba(0, 150, 166, 0.4); border-radius: 10px; color: #FFF; font-size: 14px; font-family: 'Cairo';">
        </div>
      </div>

      <button type="submit" id="loginSubmitBtn" class="btn" style="width: 100%; padding: 14px; font-size: 14.5px; font-weight: 800; border-radius: 12px; background: linear-gradient(135deg, #00897B, #004D40); box-shadow: 0 6px 20px rgba(0, 137, 123, 0.4); border: 1px solid #4DB6AC; display: flex; align-items: center; justify-content: center; gap: 8px;">
        <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
          <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
        </svg>
        <span>التحقق والدخول الآمن</span>
      </button>
    </form>

    <div style="margin-top: 24px; padding-top: 18px; border-top: 1px solid rgba(255,255,255,0.08); font-size: 11.5px; color: #80CBC4; display: flex; justify-content: space-between; align-items: center;">
      <span>تشفير Argon2id / Bcrypt</span>
      <span>حظر التخمين 15 دقيقة</span>
      <span>م. عمر الحومي</span>
    </div>
  </div>
</div>
