# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## System Architecture

This is a persistent ping system for Wear OS devices with three main components:

### Backend (server/)
- **Flask API** (`app.py`) with single `/ping` endpoint that receives device pings
- **MariaDB database** stores pings with device_id, timestamp, and status
- **PyMySQL** for database connections with validation and error handling
- Configuration via environment variables in `.env` file

### Android Wear OS App (ping/)
- **PingService**: Foreground service that runs persistently, schedules WorkManager tasks
- **PingWorker**: WorkManager worker that executes HTTP POST requests every 15 minutes with exponential backoff retry
- **BootReceiver**: BroadcastReceiver that auto-starts service on device boot
- **ApiClient**: Retrofit HTTP client with retry logic for network failures
- **MainActivity**: Simple UI for service control and status monitoring

### Database (database/)
- MariaDB schema with `pings` table (id, device_id, timestamp, status)
- Dedicated `ping_user` with minimal SELECT/INSERT privileges

## Key Data Flow
1. BootReceiver → PingService → WorkManager schedules PingWorker
2. PingWorker → ApiClient → HTTP POST /ping → Flask validates → Database INSERT
3. Network failures trigger exponential backoff retry (max 3 attempts)

## Development Commands

### Server Development
```bash
cd server/
pip install -r requirements.txt
cp .env.example .env  # Edit database credentials
python app.py  # Development server
python test_server.py  # API testing
```

### Android Development
```bash
cd ping/
./gradlew build  # Build Wear OS app
./gradlew assembleDebug  # Debug APK
./gradlew assembleRelease  # Release APK
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Database Operations
```bash
cd database/
sudo mysql -u root -p < init.sql  # Initialize schema
mysql -u ping_user -p -D ping_db  # Connect to database
```

### Testing Commands
```bash
# Server API testing
curl http://localhost:5000/health
curl -X POST http://localhost:5000/ping -H "Content-Type: application/json" -d '{"device_id": "test"}'

# Android logging
adb logcat | grep -E "(PingService|PingWorker|BootReceiver)"

# Database verification
mysql -u ping_user -p -D ping_db -e "SELECT * FROM pings ORDER BY timestamp DESC LIMIT 10;"
```

## Configuration Points

### Critical Configuration Files
- `server/app.py`: Database connection, Flask routes, validation logic
- `ping/app/src/main/java/com/delta/ping/network/ApiClient.kt`: Server URL (`BASE_URL`)
- `ping/app/src/main/java/com/delta/ping/service/PingService.kt`: Ping interval (`PING_INTERVAL_MINUTES`)
- `ping/app/src/main/java/com/delta/ping/worker/PingWorker.kt`: Retry logic (`MAX_RETRY_ATTEMPTS`)

### Environment Configuration
- Server uses `.env` file for database credentials and Flask settings
- Android app hardcodes server URL in `ApiClient.kt` - update for different environments

## Service Architecture Details

### Android Service Lifecycle
- **PingService** runs as foreground service with persistent notification
- Uses `WorkManager` with network constraints for reliable background execution
- **START_STICKY** ensures service restarts if killed by system
- Battery optimization exemption required for reliable operation

### Retry and Resilience
- PingWorker implements exponential backoff: 1s, 2s, 4s delays
- Network constraints prevent execution without connectivity
- Failed pings logged but don't prevent future attempts
- Service auto-restarts on device reboot via BootReceiver

### Database Design
- Single `pings` table with indexed device_id and timestamp columns
- Minimal user privileges for security (ping_user can only SELECT/INSERT)
- Auto-generated timestamps for ping reception
- Status field allows future expansion (currently defaults to 'received')

## Development Workflow

When modifying the system:
1. **Server changes**: Test with `python test_server.py` before deployment
2. **Android changes**: Test foreground service persistence and boot auto-start
3. **Database changes**: Update both `init.sql` and server validation logic
4. **Configuration changes**: Update both client `ApiClient.kt` and server `.env`

## Deployment

Production deployment uses systemd service for server auto-start:
```bash
cd server/
sudo ./setup.sh  # Creates ping-server user and systemd service
sudo systemctl start ping-server
```

Server runs as non-root `ping-server` user with virtual environment at `/opt/ping-server/venv`.