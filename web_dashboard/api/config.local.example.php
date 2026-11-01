<?php
/**
 * Copy this file to config.local.php on the server and fill in the real values.
 * config.local.php is git-ignored: never commit it.
 */

// Firebase Console > Project settings > Service accounts > Database secrets.
// The dashboard uses it server-side only; it is never sent to the browser.
define('FIREBASE_AUTH_SECRET', '');

// Long random string used to sign admin session tokens, e.g. the output of:
//   php -r "echo bin2hex(random_bytes(32));"
define('ADMIN_TOKEN_SECRET', '');

// Only used when data/admins.json does not exist yet: creates the first super admin.
// Remove these two lines after the first login.
// define('ADMIN_BOOTSTRAP_EMAIL', 'you@example.com');
// define('ADMIN_BOOTSTRAP_PASSWORD', 'at-least-12-characters');
