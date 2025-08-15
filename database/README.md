# Database Setup Instructions

## Prerequisites
- MariaDB or MySQL server installed
- Root access to the database server

## Setup Steps

1. **Run the initialization script:**
   ```bash
   sudo mysql -u root -p < init.sql
   ```

2. **Verify the setup:**
   ```bash
   mysql -u ping_user -p -D ping_db
   # Password: ping_secure_password_2024
   ```

3. **Test the connection:**
   ```sql
   SHOW TABLES;
   SELECT * FROM pings;
   ```

## Security Notes
- Change the default password in `init.sql` before running in production
- The `ping_user` has minimal privileges (SELECT, INSERT only)
- Consider using environment variables for credentials in production

## Schema Details
- **Database:** `ping_db`
- **Table:** `pings`
- **Columns:**
  - `id`: Auto-incrementing primary key
  - `device_id`: Unique identifier for the watch device
  - `timestamp`: When the ping was received (auto-generated)
  - `status`: Status of the ping (default: 'received')