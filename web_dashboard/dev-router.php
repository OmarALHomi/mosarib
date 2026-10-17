<?php
// Local Development Router for Mosarib Web Dashboard
// Allows running: php -S 127.0.0.1:8080 dev-router.php
$uri = urldecode(parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH));
$file = __DIR__ . $uri;

if ($uri !== '/' && file_exists($file) && !is_dir($file)) {
    return false; // Serve static file directly
}

if ($uri === '/admin' || $uri === '/admin/') {
    require_once __DIR__ . '/admin.php';
    exit;
}

require_once __DIR__ . '/index.php';
