<?php
include 'db.php';
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');

try {
    $stmt = $pdo->query("SELECT id, plate_number, status, reason, scanned_at FROM attendance ORDER BY scanned_at DESC LIMIT 100");
    echo json_encode($stmt->fetchAll());
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(["error" => $e->getMessage()]);
}
?>
