# Deployment Guide

This guide walks through deploying the persistent ping system in a production environment.

## Prerequisites

- Linux server (Ubuntu 20.04+ recommended)
- MariaDB/MySQL installed
- Python 3.8+
- Android Studio for Wear OS app compilation
- Wear OS device or emulator for testing

## Step 1: Database Setup

### Install MariaDB
```bash
sudo apt update
sudo apt install mariadb-server mariadb-client
sudo mysql_secure_installation
```

### Initialize Database
```bash
cd database/
# Edit init.sql to change the default password
sudo mysql -u root -p < init.sql
```

### Verify Database Setup
```bash
mysql -u ping_user -p -D ping_db
# Password: your_secure_password
mysql> SHOW TABLES;
mysql> DESCRIBE pings;
mysql> EXIT;
```

## Step 2: Server Deployment

### Production Setup
```bash
cd server/
sudo ./setup.sh
```

### Manual Setup (Alternative)
```bash
# Create system user
sudo useradd --system --create-home --home-dir /opt/ping-server ping-server

# Setup directories
sudo mkdir -p /opt/ping-server
sudo cp * /opt/ping-server/
sudo chown -R ping-server:ping-server /opt/ping-server

# Install dependencies
sudo -u ping-server python3 -m venv /opt/ping-server/venv
sudo -u ping-server /opt/ping-server/venv/bin/pip install -r /opt/ping-server/requirements.txt

# Configure environment
sudo cp /opt/ping-server/.env.example /opt/ping-server/.env
sudo nano /opt/ping-server/.env  # Edit with your settings
sudo chown ping-server:ping-server /opt/ping-server/.env
sudo chmod 600 /opt/ping-server/.env
```

### Configure Systemd Service
```bash
sudo cp ping-server.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable ping-server
sudo systemctl start ping-server
```

### Verify Server
```bash
sudo systemctl status ping-server
curl http://localhost:5000/health
python3 test_server.py
```

## Step 3: Network Configuration

### Firewall Setup
```bash
# Allow HTTP traffic
sudo ufw allow 5000/tcp
# Or for HTTPS (recommended)
sudo ufw allow 443/tcp
```

### NGINX Reverse Proxy (Recommended)
```nginx
server {
    listen 80;
    server_name your-domain.com;
    
    location / {
        proxy_pass http://127.0.0.1:5000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### SSL Certificate (Production)
```bash
sudo apt install certbot python3-certbot-nginx
sudo certbot --nginx -d your-domain.com
```

## Step 4: Wear OS App Configuration

### Update Server Configuration
Edit `ping/app/src/main/java/com/delta/ping/network/ApiClient.kt`:
```kotlin
private const val BASE_URL = "https://your-domain.com/"
// or
private const val BASE_URL = "http://your-server-ip:5000/"
```

### Build Release APK
```bash
cd ping/
./gradlew assembleRelease
```

### Install on Device
```bash
adb install app/build/outputs/apk/release/app-release.apk
```

## Step 5: Testing & Validation

### Server Tests
```bash
cd server/
python3 test_server.py
```

### Watch App Tests
1. Install app on Wear OS device
2. Start ping service
3. Monitor logs: `adb logcat | grep PingService`
4. Check database for incoming pings
5. Test device reboot functionality

### Database Verification
```sql
-- Check ping count
SELECT COUNT(*) as total_pings FROM pings;

-- Check recent pings
SELECT * FROM pings ORDER BY timestamp DESC LIMIT 10;

-- Check unique devices
SELECT device_id, COUNT(*) as ping_count 
FROM pings 
GROUP BY device_id 
ORDER BY ping_count DESC;
```

## Step 6: Monitoring & Maintenance

### Log Monitoring
```bash
# Server logs
sudo journalctl -u ping-server -f

# System logs
tail -f /var/log/syslog | grep ping-server
```

### Database Maintenance
```sql
-- Clean old pings (older than 30 days)
DELETE FROM pings WHERE timestamp < DATE_SUB(NOW(), INTERVAL 30 DAY);

-- Optimize table
OPTIMIZE TABLE pings;
```

### Backup Strategy
```bash
# Database backup
mysqldump -u ping_user -p ping_db > ping_backup_$(date +%Y%m%d).sql

# Automated backup script (add to crontab)
0 2 * * * /usr/bin/mysqldump -u ping_user -p'password' ping_db > /backup/ping_$(date +\%Y\%m\%d).sql
```

## Security Checklist

- [ ] Changed default database passwords
- [ ] Enabled firewall with minimal open ports
- [ ] Configured HTTPS with valid certificates
- [ ] Database user has minimal privileges
- [ ] Server runs as non-root user
- [ ] Regular security updates applied
- [ ] Log monitoring in place
- [ ] Backup strategy implemented

## Performance Tuning

### Database Optimization
```sql
-- Add indexes for better performance
CREATE INDEX idx_device_timestamp ON pings(device_id, timestamp);

-- Monitor query performance
SHOW PROCESSLIST;
```

### Server Optimization
```bash
# Increase worker processes in systemd service
sudo systemctl edit ping-server
```

Add:
```ini
[Service]
ExecStart=
ExecStart=/opt/ping-server/venv/bin/gunicorn --bind 0.0.0.0:5000 --workers 4 app:app
```

## Troubleshooting

### Common Issues

**Service won't start:**
```bash
sudo systemctl status ping-server
sudo journalctl -u ping-server -n 50
```

**Database connection issues:**
```bash
mysql -u ping_user -p -h localhost ping_db
# Test connectivity and credentials
```

**High CPU/Memory usage:**
```bash
top -p $(pgrep -f ping-server)
htop
```

**Network connectivity issues:**
```bash
netstat -tlnp | grep 5000
telnet localhost 5000
```

### Health Monitoring

Create a monitoring script:
```bash
#!/bin/bash
# health_check.sh
curl -f http://localhost:5000/health || systemctl restart ping-server
```

Add to crontab:
```bash
*/5 * * * * /path/to/health_check.sh
```

## Production Checklist

- [ ] Database initialized and secured
- [ ] Server deployed and running
- [ ] Systemd service enabled
- [ ] Firewall configured
- [ ] SSL certificates installed
- [ ] Monitoring in place
- [ ] Backups configured
- [ ] Watch app built and tested
- [ ] End-to-end testing completed
- [ ] Documentation updated with production URLs