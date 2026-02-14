# GatewayaaS - Configurable API Gateway

A production-ready API Gateway built with **Netty** and **raw JDBC** to demonstrate understanding of:
- Non-blocking I/O and event-driven architecture
- HTTP protocol and request forwarding
- Round-robin load balancing
- Automated health checking
- Multi-tenant isolation

## Features

- ✅ User authentication (JWT-based)
- ✅ Multi-tenant gateway management
- ✅ Dynamic route configuration
- ✅ Round-robin load balancing
- ✅ Automated health checks
- ✅ Request/response proxying
- ✅ Asynchronous request logging
- ✅ Background worker services

## Architecture
```
┌─────────────────────────────────────────────────────────────┐
│                     Netty Server                            │
│  ┌────────────────┐         ┌──────────────────┐            │
│  │  Boss EventLoop│────────▶│  Worker EventLoop│           │
│  └────────────────┘         └──────────────────┘            │
│                                      │                      │
│                           ┌─────────────────────┐           │
│                           │  Channel Pipeline   │           │
│                           │  - HttpServerCodec  │           │
│                           │  - RouterHandler    │           │
│                           └─────────────────────┘           │
└─────────────────────────────────────────────────────────────┘
                              │
                    ┌─────────┴─────────┐
                    │                   │
         ┌──────────▼────────┐   ┌─────▼────────┐
         │  ProxyService     │   │  Services    │
         │  (forwarding)     │   │  - Auth      │
         │                   │   │  - Gateway   │
         └──────────┬────────┘   │  - Route     │
                    │            └──────────────┘
         ┌──────────▼────────────────┐
         │  Background Services      │
         │  - Health Checker         │
         │  - Log Flusher            │
         └───────────────────────────┘
```

## Tech Stack

- **Java 17**
- **Netty 4.1** - Async I/O framework
- **PostgreSQL** - Database
- **HikariCP** - Connection pooling
- **JJWT** - JWT authentication
- **Gson** - JSON processing
- **Flyway** - Database migrations

## Quick Start

### Prerequisites
- Java 17+
- Docker & Docker Compose (or PostgreSQL 16+)
- Maven 3.9+

### Run with Docker Compose
```bash
# Build and start all services
docker-compose up --build

# Server will be available at http://localhost:8080
```

### Run Locally
```bash
# 1. Start PostgreSQL
docker run -d \
  -e POSTGRES_DB=gatewaydb \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:16-alpine

# 2. Build application
mvn clean package

# 3. Run application
java -jar target/gatewayaas-1.0.0.jar
```

## API Usage

### 1. Register User
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'
```

### 2. Login
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'

# Response:
# {
#   "access_token": "eyJhbGc...",
#   "token_type": "Bearer",
#   "expires_in": "3600"
# }
```

### 3. Create Gateway
```bash
curl -X POST http://localhost:8080/api/v1/gateways \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Production Gateway",
    "description": "Main production traffic"
  }'

# Save the gateway ID from response
```

### 4. Create Route
```bash
curl -X POST http://localhost:8080/api/v1/gateways/GATEWAY_ID/routes \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "path": "/api/users",
    "method": "GET",
    "timeout_ms": 30000
  }'

# Save the route ID from response
```

### 5. Add Backend Targets
```bash
# Add first target
curl -X POST http://localhost:8080/api/v1/routes/ROUTE_ID/targets \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "target_url": "http://backend1.example.com:8080"
  }'

# Add second target
curl -X POST http://localhost:8080/api/v1/routes/ROUTE_ID/targets \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "target_url": "http://backend2.example.com:8080"
  }'
```

### 6. Test Proxy (Load Balancing)
```bash
# Make multiple requests - they will round-robin across targets
curl http://localhost:8080/gateway/GATEWAY_ID/api/users
curl http://localhost:8080/gateway/GATEWAY_ID/api/users
curl http://localhost:8080/gateway/GATEWAY_ID/api/users
```

### 7. View Request Logs
```bash
curl http://localhost:8080/api/v1/gateways/GATEWAY_ID/logs?limit=50 \
  -H "Authorization: Bearer YOUR_TOKEN"

# Filter by status code
curl http://localhost:8080/api/v1/gateways/GATEWAY_ID/logs?status=500 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## Configuration

Edit `src/main/resources/application.properties`:
```properties
# Server
server.port=8080

# Database
db.url=jdbc:postgresql://localhost:5432/gatewaydb
db.username=postgres
db.password=postgres

# JWT
jwt.secret=your-secret-key
jwt.expiration.ms=3600000

# Health Checks
healthcheck.interval.seconds=30
healthcheck.unhealthy.threshold=3

# Logging
log.batch.size=100
log.flush.interval.seconds=5
```

## Testing
```bash
# Run unit tests
mvn test
```

## Project Structure
```
src/main/java/com/gateway/
├── Main.java                    # Entry point
├── config/                      # Configuration
├── database/                    # Connection pool, transactions
├── domain/                      # Entity models
├── netty/                       # Netty server & handlers
│   ├── NettyServer.java
│   ├── ServerInitializer.java
│   ├── RouterHandler.java
│   └── handlers/                # Request handlers
├── repository/                  # Database access (JDBC)
├── service/                     # Business logic
│   ├── ProxyService.java        # Core forwarding
│   ├── LoadBalancerService.java
│   ├── HealthCheckService.java
│   └── LogService.java
└── util/                        # JWT, JSON, passwords
```

## Interview Talking Points

**Why Netty over Spring Boot?**
- Understand async I/O, event loops, and non-blocking architecture
- Learn how frameworks work internally
- Better equipped to debug production issues

**How does request forwarding work?**
1. Extract gateway ID and path from URL
2. Query database for matching route
3. Get healthy targets for route
4. Load balancer selects target (round-robin)
5. Forward request with timeout
6. Stream response back to client
7. Log request asynchronously

**How do you handle target failures?**
- Health check worker pings `/health` every 30s
- Track consecutive failures in database
- Mark unhealthy after 3 failures
- Exclude from load balancing
- Auto-recover after 2 consecutive successes

**Thread model?**
- Boss EventLoop: 1 thread accepts connections
- Worker EventLoop: CPU cores × 2 threads handle I/O
- Each channel assigned to one EventLoop (thread affinity)
- Background services use separate ScheduledExecutorService