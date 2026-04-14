<?php
include 'db.php';
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') { http_response_code(204); exit; }

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $data = json_decode(file_get_contents('php://input'), true);
    $plate = strtoupper(trim($data['plate_number'] ?? ''));
    if (!$plate) { echo json_encode(["status"=>"error","message"=>"Plate required"]); exit; }
    try {
        $stmt = $pdo->prepare("DELETE FROM vehicles WHERE plate_number = ?");
        $stmt->execute([$plate]);
        echo json_encode(["status"=>"success","message"=>"Plate deleted"]);
    } catch (Exception $e) {
        echo json_encode(["status"=>"error","message"=>$e->getMessage()]);
    }
} else {
    echo json_encode(["status"=>"error","message"=>"Invalid method"]);
}
?>
