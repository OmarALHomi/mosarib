<?php
/**
 * Mosarib Royal Enterprise Authentication & Security Engine
 * Features:
 * - Argon2id / Bcrypt password hashing
 * - Secure Server-side PHP Sessions with HttpOnly + SameSite cookies
 * - Rate Limiting & Brute-force lockout (5 failed attempts = 15-minute freeze)
 * - Strict CSRF token validation
 * - Multi-admin management (Name, Email, Role, CreatedAt, LastLogin)
 * - Initial super-admin bootstrap (Eng. Omar AL-Homi)
 */

if (session_status() === PHP_SESSION_NONE) {
    // Configure secure session parameters
    ini_set('session.cookie_httponly', 1);
    ini_set('session.use_only_cookies', 1);
    ini_set('session.cookie_samesite', 'Strict');
    $isHttps = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off')
        || (!empty($_SERVER['HTTP_X_FORWARDED_PROTO']) && $_SERVER['HTTP_X_FORWARDED_PROTO'] === 'https')
        || (!empty($_SERVER['HTTP_CF_VISITOR']) && strpos($_SERVER['HTTP_CF_VISITOR'], 'https') !== false)
        || (isset($_SERVER['SERVER_PORT']) && $_SERVER['SERVER_PORT'] == 443);
    if ($isHttps) {
        ini_set('session.cookie_secure', 1);
    }
    session_start();
}

require_once __DIR__ . '/config.php';

$adminsFile = __DIR__ . '/../data/admins.json';
$rateLimitFile = __DIR__ . '/../data/login_attempts.json';

// Initialize admins storage if not exists. The first super admin comes from ADMIN_BOOTSTRAP_EMAIL /
// ADMIN_BOOTSTRAP_PASSWORD in config.local.php; there is no built-in default password.
if ((!file_exists($adminsFile) || filesize($adminsFile) === 0)
    && defined('ADMIN_BOOTSTRAP_EMAIL') && defined('ADMIN_BOOTSTRAP_PASSWORD')
    && strlen(ADMIN_BOOTSTRAP_PASSWORD) >= 12) {
    $defaultAdmin = [
        'id' => 'ADMIN_' . bin2hex(random_bytes(6)),
        'name' => 'المهندس عمر الحومي',
        'email' => strtolower(ADMIN_BOOTSTRAP_EMAIL),
        'password_hash' => password_hash(ADMIN_BOOTSTRAP_PASSWORD, PASSWORD_DEFAULT),
        'role' => 'SUPER_ADMIN',
        'is_active' => true,
        'created_at' => time() * 1000,
        'last_login' => null,
        'created_by' => 'SYSTEM'
    ];
    file_put_contents($adminsFile, json_encode([$defaultAdmin], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT));
}

// Generate or retrieve CSRF token
function getCsrfToken() {
    if (empty($_SESSION['csrf_token'])) {
        $_SESSION['csrf_token'] = bin2hex(random_bytes(32));
    }
    return $_SESSION['csrf_token'];
}

function verifyCsrfToken($token) {
    return !empty($_SESSION['csrf_token']) && !empty($token) && hash_equals($_SESSION['csrf_token'], $token);
}

// Rate Limiting helper to block brute force (throttles per IP and per email with flock)
function checkRateLimit($ip, $email = '') {
    global $rateLimitFile;
    if (!file_exists($rateLimitFile)) return true;
    $fp = @fopen($rateLimitFile, 'c+');
    if (!$fp) return true;
    if (!flock($fp, LOCK_SH)) {
        fclose($fp);
        return true;
    }
    $content = stream_get_contents($fp);
    flock($fp, LOCK_UN);
    fclose($fp);

    $attempts = json_decode($content, true) ?: [];
    $keys = ['ip_' . $ip];
    if (!empty($email)) {
        $keys[] = 'email_' . strtolower(trim($email));
    }

    $now = time();
    foreach ($keys as $k) {
        if (isset($attempts[$k])) {
            $data = $attempts[$k];
            if ($data['count'] >= 5 && ($now - $data['last_attempt']) < 900) {
                $remaining = 15 - round(($now - $data['last_attempt']) / 60);
                jsonResponse(false, "تم حظر محاولات الدخول مؤقتاً لحماية أمن اللوحة. يرجى الانتظار {$remaining} دقيقة.", null, 429);
            }
        }
    }
    return true;
}

function recordFailedAttempt($ip, $email = '') {
    global $rateLimitFile;
    $fp = @fopen($rateLimitFile, 'c+');
    if (!$fp) return;
    if (!flock($fp, LOCK_EX)) {
        fclose($fp);
        return;
    }
    $content = stream_get_contents($fp);
    $attempts = json_decode($content, true) ?: [];
    $now = time();

    $keys = ['ip_' . $ip];
    if (!empty($email)) {
        $keys[] = 'email_' . strtolower(trim($email));
    }

    foreach ($keys as $k) {
        if (!isset($attempts[$k]) || ($now - $attempts[$k]['last_attempt']) >= 900) {
            $attempts[$k] = ['count' => 1, 'last_attempt' => $now];
        } else {
            $attempts[$k]['count']++;
            $attempts[$k]['last_attempt'] = $now;
        }
    }

    ftruncate($fp, 0);
    rewind($fp);
    fwrite($fp, json_encode($attempts, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT));
    fflush($fp);
    flock($fp, LOCK_UN);
    fclose($fp);
}

function clearRateLimit($ip, $email = '') {
    global $rateLimitFile;
    if (!file_exists($rateLimitFile)) return;
    $fp = @fopen($rateLimitFile, 'c+');
    if (!$fp) return;
    if (!flock($fp, LOCK_EX)) {
        fclose($fp);
        return;
    }
    $content = stream_get_contents($fp);
    $attempts = json_decode($content, true) ?: [];
    unset($attempts['ip_' . $ip]);
    if (!empty($email)) {
        unset($attempts['email_' . strtolower(trim($email))]);
    }
    ftruncate($fp, 0);
    rewind($fp);
    fwrite($fp, json_encode($attempts, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT));
    fflush($fp);
    flock($fp, LOCK_UN);
    fclose($fp);
}

// Check if currently authenticated via HttpOnly PHP session
function requireAuth() {
    $now = time();
    $maxSessionAge = 12 * 3600;
    $maxIdleAge = 2 * 3600;
    if (!empty($_SESSION['admin_logged_in']) && $_SESSION['admin_logged_in'] === true && !empty($_SESSION['admin_user'])) {
        $loginTime = intval($_SESSION['login_time'] ?? 0);
        $lastAct = intval($_SESSION['last_activity'] ?? $loginTime);
        if (($loginTime > 0 && ($now - $loginTime) > $maxSessionAge) || ($lastAct > 0 && ($now - $lastAct) > $maxIdleAge)) {
            $_SESSION = [];
            @session_destroy();
            jsonResponse(false, 'انتهت صلاحية الجلسة، يرجى تسجيل الدخول مجدداً', null, 401);
        }
        $_SESSION['last_activity'] = $now;
        return;
    }
    jsonResponse(false, 'غير مصرح: يجب تسجيل الدخول بحساب مسؤول معتمد أولاً', null, 401);
}

// Route API actions
$action = $_GET['action'] ?? $_POST['action'] ?? '';

if ($action === 'get_csrf') {
    jsonResponse(true, 'CSRF Token ready', ['csrf_token' => getCsrfToken()]);
}

if ($action === 'check_session') {
    if (!empty($_SESSION['admin_logged_in']) && $_SESSION['admin_logged_in'] === true) {
        jsonResponse(true, 'الجلسة نشطة ومصرحة', [
            'admin' => $_SESSION['admin_user'],
            'csrf_token' => getCsrfToken()
        ]);
    } else {
        jsonResponse(false, 'لا توجد جلسة نشطة', ['csrf_token' => getCsrfToken()], 401);
    }
}

// Handle Login
if ($action === 'login') {
    $ip = $_SERVER['REMOTE_ADDR'] ?? '0.0.0.0';
    $input = json_decode(file_get_contents('php://input'), true) ?: $_POST;
    $email = strtolower(trim($input['email'] ?? ''));
    $password = trim($input['password'] ?? '');
    $csrf = $input['csrf_token'] ?? '';

    // Rate limit check per IP and email
    checkRateLimit($ip, $email);

    // Strict CSRF verification on login
    if (!verifyCsrfToken($csrf)) {
        jsonResponse(false, 'رمز أمان الجلسة غير صالح (CSRF)', null, 403);
    }

    if (empty($email) || empty($password)) {
        jsonResponse(false, 'يرجى إدخال البريد الإلكتروني وكلمة المرور');
    }

    $admins = file_exists($adminsFile) ? (json_decode(file_get_contents($adminsFile), true) ?: []) : [];
    $foundAdmin = null;
    $adminIndex = -1;

    foreach ($admins as $idx => $admin) {
        if (strtolower($admin['email']) === $email) {
            $foundAdmin = $admin;
            $adminIndex = $idx;
            break;
        }
    }

    if (!$foundAdmin || !password_verify($password, $foundAdmin['password_hash'])) {
        recordFailedAttempt($ip, $email);
        jsonResponse(false, 'البريد الإلكتروني أو كلمة المرور غير صحيحة!', null, 401);
    }

    if (!$foundAdmin['is_active']) {
        jsonResponse(false, 'تم إيقاف صلاحيات هذا الحساب الإداري. تواصل مع المسؤول الرئيسي.', null, 403);
    }

    // Success: Regenerate session to prevent fixation
    session_regenerate_id(true);
    clearRateLimit($ip, $email);

    // Update last login
    $admins[$adminIndex]['last_login'] = time() * 1000;
    file_put_contents($adminsFile, json_encode($admins, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT), LOCK_EX);

    $userData = [
        'id' => $foundAdmin['id'],
        'name' => $foundAdmin['name'],
        'email' => $foundAdmin['email'],
        'role' => $foundAdmin['role']
    ];

    $_SESSION['admin_logged_in'] = true;
    $_SESSION['admin_user'] = $userData;
    $_SESSION['login_time'] = time();
    $_SESSION['last_activity'] = time();

    jsonResponse(true, 'تم تسجيل الدخول بنجاح وترقية الجلسة لأعلى درجات الأمان', [
        'admin' => $userData,
        'csrf_token' => getCsrfToken()
    ]);
}

// Handle Logout
if ($action === 'logout') {
    $_SESSION = [];
    if (ini_get("session.use_cookies")) {
        $params = session_get_cookie_params();
        setcookie(session_name(), '', time() - 42000,
            $params["path"], $params["domain"],
            $params["secure"], $params["httponly"]
        );
    }
    session_destroy();
    jsonResponse(true, 'تم تسجيل الخروج وتأمين الجلسة بنجاح');
}

// Create New Admin (Restricted to logged-in SUPER_ADMIN)
if ($action === 'create_admin') {
    requireAuth();
    if ($_SESSION['admin_user']['role'] !== 'SUPER_ADMIN') {
        jsonResponse(false, 'عذراً، إضافة مسؤولين جدد مقتصرة حصراً على المسؤول الرئيسي (Super Admin)', null, 403);
    }

    $input = json_decode(file_get_contents('php://input'), true) ?: $_POST;
    $csrf = $input['csrf_token'] ?? '';
    if (!verifyCsrfToken($csrf)) {
        jsonResponse(false, 'رمز الحماية ضد التلاعب غير صالح', null, 403);
    }

    $name = trim($input['name'] ?? '');
    $email = strtolower(trim($input['email'] ?? ''));
    $password = trim($input['password'] ?? '');
    $role = trim($input['role'] ?? 'ADMIN');

    if (empty($name) || empty($email) || empty($password)) {
        jsonResponse(false, 'يرجى تزويد الاسم والبريد الإلكتروني وكلمة المرور بالكامل');
    }

    if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
        jsonResponse(false, 'صيغة البريد الإلكتروني غير صحيحة');
    }

    if (strlen($password) < 8) {
        jsonResponse(false, 'كلمة المرور يجب أن لا تقل عن 8 أحرف وأرقام معقدة لحماية الحساب');
    }

    $admins = file_exists($adminsFile) ? (json_decode(file_get_contents($adminsFile), true) ?: []) : [];
    foreach ($admins as $a) {
        if (strtolower($a['email']) === $email) {
            jsonResponse(false, 'هذا البريد الإلكتروني مسجل مسبقاً لمسؤول آخر');
        }
    }

    $newAdmin = [
        'id' => 'ADMIN_' . bin2hex(random_bytes(6)),
        'name' => $name,
        'email' => $email,
        'password_hash' => password_hash($password, PASSWORD_DEFAULT),
        'role' => in_array($role, ['SUPER_ADMIN', 'ADMIN', 'MODERATOR']) ? $role : 'ADMIN',
        'is_active' => true,
        'created_at' => time() * 1000,
        'last_login' => null,
        'created_by' => $_SESSION['admin_user']['name']
    ];

    $admins[] = $newAdmin;
    file_put_contents($adminsFile, json_encode($admins, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT), LOCK_EX);

    jsonResponse(true, "تم إنشاء حساب المسؤول ({$name}) بنجاح وتأمينه بأعلى معايير التشفير", [
        'id' => $newAdmin['id'],
        'name' => $newAdmin['name'],
        'email' => $newAdmin['email'],
        'role' => $newAdmin['role']
    ]);
}

// List Admins
if ($action === 'list_admins') {
    requireAuth();
    $admins = file_exists($adminsFile) ? (json_decode(file_get_contents($adminsFile), true) ?: []) : [];
    $safeAdmins = array_map(function($a) {
        return [
            'id' => $a['id'],
            'name' => $a['name'],
            'email' => $a['email'],
            'role' => $a['role'],
            'is_active' => $a['is_active'],
            'created_at' => $a['created_at'],
            'last_login' => $a['last_login'],
            'created_by' => $a['created_by'] ?? 'SYSTEM'
        ];
    }, $admins);

    jsonResponse(true, 'تم جلب قائمة مسؤولي النظام بنجاح', $safeAdmins);
}

// Delete Admin (Restricted to logged-in SUPER_ADMIN via CSRF-protected POST)
if ($action === 'delete_admin') {
    requireAuth();
    if (($_SESSION['admin_user']['role'] ?? '') !== 'SUPER_ADMIN') {
        jsonResponse(false, 'عذراً، حذف حسابات المسؤولين مقتصر على المسؤول الرئيسي فقط (Super Admin)', null, 403);
    }
    if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
        jsonResponse(false, 'طريقة غير مدعومة', null, 405);
    }

    $input = json_decode(file_get_contents('php://input'), true) ?: $_POST;
    $csrfHeaders = function_exists('getallheaders') ? getallheaders() : [];
    $csrf = $_SERVER['HTTP_X_CSRF_TOKEN'] ?? ($csrfHeaders['X-CSRF-Token'] ?? ($csrfHeaders['x-csrf-token'] ?? ($input['csrf_token'] ?? '')));
    if (!verifyCsrfToken($csrf)) {
        jsonResponse(false, 'رمز الحماية ضد التلاعب غير صالح', null, 403);
    }

    $targetId = trim((string)($input['id'] ?? ''));
    if ($targetId === '') {
        jsonResponse(false, 'معرف المسؤول مطلوب');
    }
    if ($targetId === ($_SESSION['admin_user']['id'] ?? '')) {
        jsonResponse(false, 'لا يمكنك حذف حسابك الذي تستخدمه حالياً');
    }

    $admins = file_exists($adminsFile) ? (json_decode(file_get_contents($adminsFile), true) ?: []) : [];
    $activeSupers = 0;
    $targetAdmin = null;
    foreach ($admins as $a) {
        if (($a['role'] ?? '') === 'SUPER_ADMIN' && ($a['is_active'] ?? true) !== false) {
            $activeSupers++;
        }
        if (($a['id'] ?? '') === $targetId) {
            $targetAdmin = $a;
        }
    }
    if ($targetAdmin === null) {
        jsonResponse(false, 'حساب المسؤول غير موجود');
    }
    if (($targetAdmin['role'] ?? '') === 'SUPER_ADMIN' && $activeSupers <= 1) {
        jsonResponse(false, 'لا يمكن حذف المسؤول الرئيسي الوحيد في النظام');
    }

    $remaining = array_values(array_filter($admins, function($a) use ($targetId) {
        return ($a['id'] ?? '') !== $targetId;
    }));
    file_put_contents($adminsFile, json_encode($remaining, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT), LOCK_EX);
    jsonResponse(true, 'تم حذف حساب المسؤول بنجاح');
}
