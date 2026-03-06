#!/bin/bash
# ═══════════════════════════════════════════════════════════
#  Ruchulu — PostgreSQL database initialisation script
#  Creates all 4 service databases on first container start.
#  Mounted at: /docker-entrypoint-initdb.d/init-db.sh
# ═══════════════════════════════════════════════════════════

set -e

echo "🗄️  Ruchulu: Creating databases..."

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    -- User Service DB
    CREATE DATABASE ruchulu_users;
    GRANT ALL PRIVILEGES ON DATABASE ruchulu_users TO ruchulu_admin;

    -- Caterer Service DB
    CREATE DATABASE ruchulu_caterers;
    GRANT ALL PRIVILEGES ON DATABASE ruchulu_caterers TO ruchulu_admin;

    -- Booking Service DB
    CREATE DATABASE ruchulu_bookings;
    GRANT ALL PRIVILEGES ON DATABASE ruchulu_bookings TO ruchulu_admin;

    -- Notification Service DB
    CREATE DATABASE ruchulu_notifications;
    GRANT ALL PRIVILEGES ON DATABASE ruchulu_notifications TO ruchulu_admin;
EOSQL

echo "✅  Ruchulu: All databases created."
