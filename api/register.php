<?php
include 'db.php';
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') { http_response_code(204); exit; }

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $data = json_decode(file_get_contents('php://input'), true);
    $plate = strtoupper(trim($data['plate_number'] ?? $data['plate'] ?? ''));

    if (!$plate) {
        echo json_encode(["status" => "error", "message" => "Plate number missing"]);
        exit;
    }
    try {
        $stmt = $pdo->prepare("INSERT INTO vehicles (plate_number) VALUES (?) ON DUPLICATE KEY UPDATE plate_number = plate_number");
        $stmt->execute([$plate]);
        echo json_encode(["status" => "success", "message" => "Plate registered", "plate" => $plate]);
    } catch (Exception $e) {
        echo json_encode(["status" => "error", "message" => $e->getMessage()]);
    }
} else {
    echo json_encode(["status" => "error", "message" => "Invalid method"]);
}
?>
