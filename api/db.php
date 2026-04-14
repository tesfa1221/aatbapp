<?php
$host = 'localhost';
$db   = 'aatb_db';
$user = 'root';
$pass = '';
$charset = 'utf8mb4';

$dsn = "mysql:host=$host;dbname=$db;charset=$charset";
try {
     $pdo = new PDO($dsn, $user, $pass, [
         PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
         PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
         PDO::ATTR_EMULATE_PREPARES   => false,
     ]);
} catch (\PDOException $e) {
     die(json_encode(["status" => "error", "reason" => "DB Connection failed: " . $e->getMessage()]));
}
?>
