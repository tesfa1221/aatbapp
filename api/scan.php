<?php
include 'db.php';

header('Content-Type: application/json');

if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_FILES['images'])) {
    $files = $_FILES['images'];
    $post_fields = [];

    foreach ($files['tmp_name'] as $index => $tmpName) {
        $post_fields['images[' . $index . ']'] = new CURLFile($tmpName, 'image/jpeg', $files['name'][$index]);
    }

    // Forward to Python AI Service (set AI_SERVICE_URL in cPanel env vars)
    $ai_url = getenv('AI_SERVICE_URL') ?: 'https://aatbapp.onrender.com/analyze';
    $ch = curl_init($ai_url);
    curl_setopt($ch, CURLOPT_POST, 1);
    curl_setopt($ch, CURLOPT_POSTFIELDS, $post_fields);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);

    $result = curl_exec($ch);
    if (curl_errno($ch)) {
        echo json_encode(["status" => "Rejected", "reason" => "AI Service unreachable: " . curl_error($ch)]);
        curl_close($ch);
        exit;
    }
    curl_close($ch);

    $ai_data = json_decode($result, true);

    // Spoofing check (screen or printed paper)
    if (!empty($ai_data['spoofing_detected'])) {
        $reason = $ai_data['reason'] ?? 'Spoofing detected';
        $stmt = $pdo->prepare("INSERT INTO attendance (plate_number, status, reason) VALUES (?, 'rejected', ?)");
        $stmt->execute(['UNKNOWN', $reason]);
        echo json_encode(["status" => "Rejected", "reason" => $reason]);
        exit;
    }

    if ($ai_data && isset($ai_data['status']) && $ai_data['status'] === 'success') {
        $plate = strtoupper(trim($ai_data['plate_text'])); // Normalize
        $confidence = $ai_data['confidence'];

        // 1. Check if plate exists in the system (vehicles table)
        $checkStmt = $pdo->prepare("SELECT COUNT(*) FROM vehicles WHERE plate_number = ?");
        $checkStmt->execute([$plate]);
        $isRegistered = $checkStmt->fetchColumn() > 0;

        if ($confidence < 0.80) {
            // Low confidence rejection
            $stmt = $pdo->prepare("INSERT INTO attendance (plate_number, status, reason) VALUES (?, 'rejected', 'Low confidence')");
            $stmt->execute([$plate]);
            echo json_encode(["status" => "Rejected", "reason" => "Low confidence", "plate" => $plate]);
        } elseif (!$isRegistered) {
            // Not registered rejection
            $stmt = $pdo->prepare("INSERT INTO attendance (plate_number, status, reason) VALUES (?, 'rejected', 'Not Registered')");
            $stmt->execute([$plate]);
            echo json_encode(["status" => "Rejected", "reason" => "Plate Not Registered", "plate" => $plate]);
        } else {
            // Valid scan
            $stmt = $pdo->prepare("INSERT INTO attendance (plate_number, status) VALUES (?, 'valid')");
            $stmt->execute([$plate]);
            echo json_encode(["status" => "Valid", "plate" => $plate]);
        }
    } else {
        echo json_encode(["status" => "Rejected", "reason" => "No plate detected"]);
    }
} else {
    echo json_encode(["status" => "Error", "reason" => "Invalid request"]);
}
?>
