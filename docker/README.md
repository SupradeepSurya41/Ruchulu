# Ruchulu Backend — Docker Setup

## Prerequisites
- Docker Desktop (or Docker Engine + docker-compose)
- Java 17 + Maven (only if running services locally without Docker)

---

## 🚀 One-Command Start

```bash
# 1. Place this folder NEXT TO ruchulu-complete/
#    Expected layout:
#      ruchulu-complete/   ← backend code (from ZIP)
#      ruchulu-docker/     ← this folder

# 2. Copy and configure environment variables
cp .env.example .env
# Edit .env — set MAIL_PASSWORD to your Gmail App Password

# 3. Start everything
docker-compose up -d

# 4. Watch logs
docker-compose logs -f
```

---

## 🌐 Service URLs (after startup)

| Service | URL |
|---|---|
| **API Gateway** (single entry point) | http://localhost:8080 |
| User Service | http://localhost:8081/api/v1/users/ping |
| Caterer Service | http://localhost:8082/api/v1/caterers/ping |
| Booking Service | http://localhost:8083/api/v1/bookings/ping |
| Notification Service | http://localhost:8084/api/v1/notifications/ping |
| PgAdmin (DB browser) | http://localhost:5050 |

**PgAdmin login:** admin@ruchulu.com / admin123
**PgAdmin DB connection:** host=`postgres`, user=`ruchulu_admin`, pass=`ruchulu_pass`

---

## 📡 API Examples (via Gateway on port 8080)

```bash
# Register a user
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Ravi","lastName":"Kumar","email":"ravi@gmail.com",
       "phone":"9876543210","password":"Test@1234","city":"Hyderabad","role":"CUSTOMER"}'

# Search caterers
curl "http://localhost:8080/api/v1/caterers/search?city=Hyderabad"

# Top rated caterers
curl "http://localhost:8080/api/v1/caterers/top-rated/Hyderabad"
```

---

## 🛑 Stop

```bash
docker-compose down          # stop containers
docker-compose down -v       # stop + delete database volumes (full reset)
```

---

## 📦 Startup Order

Docker Compose handles this automatically via `depends_on` + healthchecks:

```
postgres ──► user-service ─────┐
         └─► caterer-service ──┤
         └─► booking-service ──┼──► api-gateway
         └─► notification-svc ─┘
redis ─────────────────────────► api-gateway
```

Services wait for postgres to be healthy before starting.
API Gateway waits for all 4 services to be healthy.

---

## 🔧 Individual service logs

```bash
docker-compose logs -f user-service
docker-compose logs -f booking-service
docker-compose logs -f api-gateway
```

## 🔄 Rebuild after code change

```bash
docker-compose up -d --build user-service
```
