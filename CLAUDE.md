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

## AI Agent Handoff & Continuity

This section ensures future AI agents can quickly understand project status and continue work seamlessly.

### Current Project Status
- **GitHub Project**: https://github.com/users/smithandrewk/projects/5
- **Repository**: https://github.com/smithandrewk/ping-system
- **Active Sprint**: Sprint 1 - Foundation & Quick Wins
- **Current Phase**: Phase 1 (Issues #1-32) - Foundation & Observability

### Quick Context Commands for New Agents

```bash
# Check current sprint and project status
gh project view 5 --owner smithandrewk

# See active milestone and issues
gh issue list --milestone "Sprint 1 - Foundation & Quick Wins"
gh issue list --state open --sort updated | head -10

# Check recent development activity
git log --oneline -10
git status

# Verify system health
cd server/ && python test_server.py
curl http://localhost:5000/health

# Check what's currently being worked on
gh issue list --state open --assignee @me
git branch -a | grep -E "(feature|issue)"
```

### Standard Agent Onboarding Protocol

When starting with a new AI agent, begin with this prompt:

```
Please analyze the current state of this project:

1. Read CLAUDE.md for project context
2. Check GitHub project status: gh project view 5 --owner smithandrewk  
3. Review current milestone: gh issue list --milestone "Sprint 1 - Foundation & Quick Wins"
4. Check what's in progress: gh issue list --state open --assignee @me
5. Review recent commits: git log --oneline -10

Based on this analysis, tell me:
- What was the previous agent working on?
- What's the current status of that work?
- What should we do next?
```

### Issue-Driven Development Workflow

All work should map to GitHub issues. Future agents should:

1. **Check current milestone** for sprint context
2. **Review issue status** - look for "in-progress" or recently updated issues
3. **Read issue comments** for implementation notes and progress
4. **Check for feature branches** related to active issues
5. **Update issue comments** with progress as work continues

### Progress Documentation Standards

**Update issues with progress comments:**
```bash
# Example: When making progress on an issue
gh issue comment [ISSUE_NUMBER] --body "✅ [Completed work]
🔄 [Currently working on]
⏭️ [Next steps]"
```

**Branch naming convention:**
```bash
git checkout -b feature/issue-[NUMBER]-[brief-description]
# Example: feature/issue-1-basic-dashboard
```

**Commit message format:**
```bash
git commit -m "[Component]: [Brief description] for Issue #[NUMBER]

- [Specific change 1]
- [Specific change 2]
- [Specific change 3]

[Closes #NUMBER if complete]"
```

### Sprint Planning Overview

#### Sprint 1: Foundation & Quick Wins (Issues #1, #5, #7, #9)
**Goal**: Get immediate value and system observability
- **#1**: Basic web dashboard ⚡ Quick Win
- **#5**: Server metrics endpoint ⚡ Quick Win  
- **#7**: Database cleanup job ⚡ Quick Win
- **#9**: Device registration with metadata

#### Sprint 2: Device Management (Issues #2, #3, #10)
**Goal**: Polish dashboard and add device-specific features
- **#3**: Device health status indicators
- **#10**: Device nickname/labeling  
- **#2**: Ping frequency visualization

#### Sprint 3: Sensor Foundation (Issues #17, #33)
**Goal**: Transition from heartbeat to smart data collection
- **#17**: Charging state detection (Android)
- **#33**: Wear detection using sensor patterns

### Emergency Handoff Commands

If you need to quickly onboard an agent mid-task:

```bash
# Generate quick status report
echo "## Project Status $(date)" > HANDOFF.md
echo "### Active Issues" >> HANDOFF.md  
gh issue list --state open --sort updated | head -5 >> HANDOFF.md
echo "### Recent Changes" >> HANDOFF.md
git log --oneline -5 >> HANDOFF.md
echo "### Current Branch" >> HANDOFF.md
git branch --show-current >> HANDOFF.md
cat HANDOFF.md
```

### Development Environment Quick Start

```bash
# Server setup
cd server/
pip install -r requirements.txt
cp .env.example .env  # Edit with database credentials
python app.py  # Starts development server

# Test system health
python test_server.py
curl http://localhost:5000/health

# Android development
cd ping/
./gradlew build
```

### Key Principles for AI Agents

1. **Issues are the source of truth** - All work maps to GitHub issues with clear acceptance criteria
2. **CLAUDE.md provides context** - Always read this file first for project understanding
3. **GitHub Project shows priorities** - Use milestones and project board to guide work
4. **Test before deploy** - Always run `test_server.py` and verify functionality
5. **Document progress** - Update issue comments and commit messages with detailed progress
6. **Follow the roadmap** - Stick to planned sprint goals unless explicitly redirected

### Troubleshooting for New Agents

**If confused about current state:**
- Check the most recently updated issues: `gh issue list --state open --sort updated`
- Look at recent commits: `git log --oneline -10`
- Review project board: https://github.com/users/smithandrewk/projects/5

**If system isn't working:**
- Run health check: `curl http://localhost:5000/health`
- Check database connection: `mysql -u ping_user -p -D ping_db`
- Verify dependencies: `cd server/ && pip install -r requirements.txt`

**If unsure what to work on:**
- Check current milestone: `gh issue list --milestone "Sprint 1 - Foundation & Quick Wins"`
- Look for issues marked with "quick-win" label for immediate impact
- Start with Issue #1 (Dashboard) if nothing is clearly in progress

This handoff system ensures any AI agent can quickly become productive and continue building toward the advanced ML platform vision.