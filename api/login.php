<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') { http_response_code(204); exit; }

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode(["status" => "error", "message" => "Invalid method"]);
    exit;
}

$data     = json_decode(file_get_contents('php://input'), true);
$username = trim($data['username'] ?? '');
$password = trim($data['password'] ?? '');

if (!$username || !$password) {
    echo json_encode(["status" => "error", "message" => "Username and password required"]);
    exit;
}

// ── Hardcoded demo account (always works, no DB needed) ──────────────────
if ($username === 'admin' && $password === 'admin123') {
    echo json_encode([
        "status"   => "success",
        "name"     => "Admin Controller",
        "username" => "admin",
        "role"     => "admin",
        "token"    => base64_encode("admin:" . time())
    ]);
    exit;
}

// ── DB account lookup ─────────────────────────────────────────────────────
try {
    include 'db.php';
    $stmt = $pdo->prepare("SELECT * FROM controllers WHERE username = ? LIMIT 1");
    $stmt->execute([$username]);
    $user = $stmt->fetch();
    if ($user && password_verify($password, $user['password'])) {
        echo json_encode([
            "status"   => "success",
            "name"     => $user['name'],
            "username" => $user['username'],
            "role"     => $user['role'] ?? 'controller',
            "token"    => base64_encode($user['username'] . ':' . time())
        ]);
    } else {
        echo json_encode(["status" => "error", "message" => "Invalid credentials"]);
    }
} catch (Exception $e) {
    echo json_encode(["status" => "error", "message" => "Invalid credentials"]);
}
?>
