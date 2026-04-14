<?php
// Called by a cPanel cron job every 10 minutes to keep Render awake
$ch = curl_init('https://aatbapp.onrender.com/health');
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_TIMEOUT, 10);
$result = curl_exec($ch);
curl_close($ch);
echo $result ?: 'pinged';
?>
