-- Run this in phpMyAdmin or MySQL to set up the database

CREATE DATABASE IF NOT EXISTS aatb_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE aatb_db;

-- Registered vehicles/plates
CREATE TABLE IF NOT EXISTS vehicles (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    plate_number VARCHAR(20) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Attendance log
CREATE TABLE IF NOT EXISTS attendance (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    plate_number VARCHAR(20) NOT NULL,
    status       ENUM('valid','rejected') NOT NULL,
    reason       VARCHAR(100) DEFAULT NULL,
    scanned_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Controllers (app users)
CREATE TABLE IF NOT EXISTS controllers (
    id       INT AUTO_INCREMENT PRIMARY KEY,
    name     VARCHAR(100) NOT NULL,
    username VARCHAR(50)  NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role     ENUM('admin','controller') DEFAULT 'controller',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Default admin account: admin / admin123
INSERT IGNORE INTO controllers (name, username, password, role)
VALUES ('Admin Controller', 'admin', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'admin');
