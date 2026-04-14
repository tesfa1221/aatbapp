<?php
include 'db.php';
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');

try {
    $totalPlates  = $pdo->query("SELECT COUNT(*) FROM vehicles")->fetchColumn();
    $todayPresent = $pdo->query("SELECT COUNT(DISTINCT plate_number) FROM attendance WHERE status='valid' AND DATE(scanned_at)=CURDATE()")->fetchColumn();
    $todayScans   = $pdo->query("SELECT COUNT(*) FROM attendance WHERE DATE(scanned_at)=CURDATE()")->fetchColumn();
    $totalAbsent  = (int)$totalPlates - (int)$todayPresent;

    echo json_encode([
        "total_plates"  => (int)$totalPlates,
        "today_present" => (int)$todayPresent,
        "today_absent"  => max(0, $totalAbsent),
        "today_scans"   => (int)$todayScans,
    ]);
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(["error" => $e->getMessage()]);
}
?>
