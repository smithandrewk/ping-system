# Persistent Ping System for Wear OS

A comprehensive system that enables Wear OS smartwatches to send periodic "pings" to a remote server for device monitoring and activity tracking.

## System Overview

### Components
- **Backend Server**: Flask-based API running on Linux
- **Database**: MariaDB for storing ping data
- **Client App**: Kotlin-based Android Wear OS app with persistent background service

### Features
- ✅ Persistent foreground service that survives app kills and reboots
- ✅ Periodic ping sending every 15 minutes (configurable)
- ✅ Automatic retry with exponential backoff on network failures
- ✅ Auto-start on device boot/reboot
- ✅ Battery optimization handling
- ✅ Minimal UI for service control and status monitoring
- ✅ Secure database operations with input validation
- ✅ Comprehensive logging for debugging

## Quick Start

### 1. Database Setup
```bash
cd database/
sudo mysql -u root -p < init.sql
```

### 2. Server Setup
```bash
cd server/
pip install -r requirements.txt
cp .env.example .env
# Edit .env with your database credentials
python app.py
```

### 3. Wear OS App
1. Open `ping/` directory in Android Studio
2. Update server URL in `ApiClient.kt`
3. Build and install on Wear OS device/emulator
4. Grant battery optimization exemption when prompted

## Architecture

```
Watch App --> HTTP POST /ping --> Flask Server --> MariaDB
     ^                                                 |
     |                                                 v
Boot Receiver --> PingService --> WorkManager --> PingWorker
```

### Data Flow
1. **Boot/App Start**: BootReceiver starts PingService
2. **Service Initialization**: PingService creates foreground notification and schedules WorkManager
3. **Periodic Execution**: WorkManager triggers PingWorker every 15 minutes
4. **Ping Sending**: PingWorker sends HTTP POST with device_id to server
5. **Server Processing**: Flask validates input and stores in database with timestamp
6. **Retry Logic**: Failed pings retry up to 3 times with exponential backoff

## Configuration

### Server Configuration (server/.env)
```env
DB_HOST=localhost
DB_USER=ping_user
DB_PASSWORD=your_secure_password
DB_NAME=ping_db
SERVER_HOST=0.0.0.0
SERVER_PORT=5000
```

### Watch App Configuration
- **Server URL**: Update `BASE_URL` in `ApiClient.kt`
- **Ping Interval**: Modify `PING_INTERVAL_MINUTES` in `PingService.kt`
- **Retry Settings**: Adjust `MAX_RETRY_ATTEMPTS` in `PingWorker.kt`

## Testing

### 1. Server Testing
```bash
# Test health endpoint
curl http://localhost:5000/health

# Test ping endpoint
curl -X POST http://localhost:5000/ping \
  -H "Content-Type: application/json" \
  -d '{"device_id": "test_device"}'

# Verify database
mysql -u ping_user -p -D ping_db -e "SELECT * FROM pings;"
```

### 2. Watch App Testing
1. Install app on Wear OS emulator
2. Start service via UI
3. Monitor logs: `adb logcat | grep -E "(PingService|PingWorker|BootReceiver)"`
4. Test reboot: Cold boot emulator and verify service auto-starts
5. Test network failure: Disable network, observe retry behavior

### 3. End-to-End Testing
1. Start server and verify health check
2. Install and start watch app
3. Monitor database for incoming pings
4. Test offline scenarios and recovery
5. Verify boot auto-start functionality

## Security Considerations

- Database user has minimal privileges (SELECT, INSERT only)
- Input validation prevents SQL injection
- No sensitive data transmitted or stored
- HTTPS recommended for production deployment
- Environment variables for credential management

## Production Deployment

### Server Deployment
```bash
cd server/
sudo ./setup.sh
sudo systemctl start ping-server
sudo systemctl enable ping-server
```

### Database Security
- Change default passwords before production
- Use strong passwords and proper network security
- Regular backups and monitoring

### Watch App Distribution
- Sign APK for production distribution
- Configure proper server endpoints
- Test thoroughly on target devices

## Troubleshooting

### Common Issues
1. **Service not starting**: Check battery optimization settings
2. **Pings not reaching server**: Verify network connectivity and server URL
3. **Database connection errors**: Check credentials and database status
4. **Auto-start not working**: Verify RECEIVE_BOOT_COMPLETED permission

### Debug Commands
```bash
# Check server logs
sudo journalctl -u ping-server -f

# Watch app logs
adb logcat | grep -E "(PingService|PingWorker)"

# Database status
mysql -u ping_user -p -D ping_db -e "SELECT COUNT(*) as total_pings FROM pings;"
```

## Performance

- **Server**: Handles 100+ pings/minute
- **Database**: Indexed for efficient queries
- **Watch**: Minimal battery impact with optimized scheduling
- **Network**: Lightweight JSON payloads (~50 bytes per ping)

## Future Enhancements

- Web dashboard for ping visualization
- Multi-device support with querying capabilities
- Authentication with API keys
- Advanced analytics and alerting
- Real-time ping status monitoring