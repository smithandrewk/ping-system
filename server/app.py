#!/usr/bin/env python3
"""
Flask server for receiving pings from Wear OS devices.
Stores ping data in MariaDB database.
"""

import os
import logging
from datetime import datetime
from flask import Flask, request, jsonify, render_template
import pymysql
from dotenv import load_dotenv

load_dotenv()

app = Flask(__name__)

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

DB_CONFIG = {
    'host': os.getenv('DB_HOST', 'localhost'),
    'user': os.getenv('DB_USER', 'ping_user'),
    'password': os.getenv('DB_PASSWORD', 'ping_secure_password_2024'),
    'database': os.getenv('DB_NAME', 'ping_db'),
    'port': int(os.getenv('DB_PORT', 3306)),
    'charset': 'utf8mb4',
    'autocommit': True
}

def get_db_connection():
    """Create and return a database connection."""
    try:
        connection = pymysql.connect(**DB_CONFIG)
        return connection
    except Exception as e:
        logger.error(f"Database connection failed: {e}")
        return None

def validate_ping_data(data):
    """Validate incoming ping data."""
    if not data:
        return False, "No JSON data provided"
    
    if 'device_id' not in data:
        return False, "Missing required field: device_id"
    
    device_id = data['device_id']
    if not device_id or not isinstance(device_id, str):
        return False, "device_id must be a non-empty string"
    
    if len(device_id) > 255:
        return False, "device_id too long (max 255 characters)"
    
    return True, "Valid"

def insert_ping(device_id, status='received'):
    """Insert a ping record into the database."""
    connection = get_db_connection()
    if not connection:
        return False, "Database connection failed"
    
    try:
        with connection.cursor() as cursor:
            sql = "INSERT INTO pings (device_id, timestamp, status) VALUES (%s, %s, %s)"
            timestamp = datetime.now()
            cursor.execute(sql, (device_id, timestamp, status))
            logger.info(f"Ping recorded: device_id={device_id}, timestamp={timestamp}")
            return True, "Ping recorded successfully"
    except Exception as e:
        logger.error(f"Failed to insert ping: {e}")
        return False, f"Database error: {str(e)}"
    finally:
        connection.close()

@app.route('/ping', methods=['POST'])
def receive_ping():
    """Handle incoming ping requests from Wear OS devices."""
    try:
        data = request.get_json()
        
        is_valid, message = validate_ping_data(data)
        if not is_valid:
            logger.warning(f"Invalid ping data: {message}")
            return jsonify({'error': message}), 400
        
        device_id = data['device_id']
        success, message = insert_ping(device_id)
        
        if success:
            return jsonify({'message': 'Ping received'}), 200
        else:
            return jsonify({'error': message}), 500
            
    except Exception as e:
        logger.error(f"Unexpected error in /ping endpoint: {e}")
        return jsonify({'error': 'Internal server error'}), 500

@app.route('/health', methods=['GET'])
def health_check():
    """Health check endpoint."""
    connection = get_db_connection()
    if connection:
        connection.close()
        return jsonify({'status': 'healthy', 'database': 'connected'}), 200
    else:
        return jsonify({'status': 'unhealthy', 'database': 'disconnected'}), 503

@app.errorhandler(404)
def not_found(error):
    return jsonify({'error': 'Endpoint not found'}), 404

@app.errorhandler(405)
def method_not_allowed(error):
    return jsonify({'error': 'Method not allowed'}), 405

def get_device_status():
    """Get all devices with their latest ping info and status."""
    connection = get_db_connection()
    if not connection:
        return []
    
    try:
        with connection.cursor() as cursor:
            # Get latest ping for each device with status calculation
            sql = """
            SELECT 
                device_id,
                MAX(timestamp) as last_ping,
                COUNT(*) as total_pings,
                CASE 
                    WHEN MAX(timestamp) > DATE_SUB(NOW(), INTERVAL 30 MINUTE) THEN 'online'
                    WHEN MAX(timestamp) > DATE_SUB(NOW(), INTERVAL 2 HOUR) THEN 'warning'
                    ELSE 'offline'
                END as status
            FROM pings 
            GROUP BY device_id 
            ORDER BY MAX(timestamp) DESC
            """
            cursor.execute(sql)
            devices = cursor.fetchall()
            
            # Convert to list of dicts and calculate time ago
            device_list = []
            for device in devices:
                device_dict = {
                    'device_id': device[0],
                    'last_ping': device[1],
                    'total_pings': device[2],
                    'status': device[3],
                    'time_ago': calculate_time_ago(device[1])
                }
                device_list.append(device_dict)
            
            return device_list
            
    except Exception as e:
        logger.error(f"Failed to get device status: {e}")
        return []
    finally:
        connection.close()

def calculate_time_ago(timestamp):
    """Calculate human-readable time difference."""
    if not timestamp:
        return "Never"
    
    now = datetime.now()
    diff = now - timestamp
    
    if diff.days > 0:
        return f"{diff.days} day{'s' if diff.days > 1 else ''} ago"
    elif diff.seconds > 3600:
        hours = diff.seconds // 3600
        return f"{hours} hour{'s' if hours > 1 else ''} ago"
    elif diff.seconds > 60:
        minutes = diff.seconds // 60
        return f"{minutes} minute{'s' if minutes > 1 else ''} ago"
    else:
        return "Just now"

@app.route('/')
@app.route('/dashboard')
def dashboard():
    """Dashboard showing all devices and their status."""
    devices = get_device_status()
    total_devices = len(devices)
    online_devices = len([d for d in devices if d['status'] == 'online'])
    
    stats = {
        'total_devices': total_devices,
        'online_devices': online_devices,
        'offline_devices': total_devices - online_devices
    }
    
    return render_template('dashboard.html', devices=devices, stats=stats)

if __name__ == '__main__':
    host = os.getenv('SERVER_HOST', '0.0.0.0')
    port = int(os.getenv('SERVER_PORT', 5000))
    debug = os.getenv('FLASK_DEBUG', 'False').lower() in ['true', '1', 'yes']
    
    logger.info(f"Starting ping server on {host}:{port}")
    app.run(host=host, port=port, debug=debug)