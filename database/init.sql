-- MariaDB initialization script for ping system
-- Run this script as root user to set up the database

-- Create database
CREATE DATABASE IF NOT EXISTS ping_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Create dedicated user for the ping system
CREATE USER IF NOT EXISTS 'ping_user'@'localhost' IDENTIFIED BY 'ping_secure_password_2024';

-- Grant limited privileges to ping_user
GRANT SELECT, INSERT ON ping_db.* TO 'ping_user'@'localhost';
FLUSH PRIVILEGES;

-- Use the ping database
USE ping_db;

-- Create pings table
CREATE TABLE IF NOT EXISTS pings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    device_id VARCHAR(255) NOT NULL,
    timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'received',
    INDEX idx_device_id (device_id),
    INDEX idx_timestamp (timestamp)
);

-- Insert a test ping to verify setup
INSERT INTO pings (device_id, status) VALUES ('test_device_setup', 'setup_test');

-- Display table structure
DESCRIBE pings;

-- Show the test record
SELECT * FROM pings WHERE device_id = 'test_device_setup';