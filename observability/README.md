# Observability

Observability means being able to see **what is happening inside your system at runtime** without changing its code. It has three pillars: **Logs**, **Metrics**, and **Traces**.

---

## The Three Pillars

### 1. Logs — What happened?
Text records of events that occurred inside a service.

**Example:**
```
INFO  [booking-service,4a1b2c3d,8e9f0a1b] Booking created: id=5, vehicleId=2, customerId=3
ERROR [vehicle-service,4a1b2c3d,8e9f0a1b] Vehicle not found with id: 99
```

Notice the `traceId` and `spanId` in the brackets — these link logs from different services that belong to the same request.

**Tool used:** Slf4j (already in every service). The log pattern is updated to include traceId automatically.

---

### 2. Metrics — How is the system performing?
Numbers collected over time that show system health and performance.

**Examples of metrics collected automatically:**
- `http_server_requests_seconds` — how long each API call takes
- `http_server_requests_total` — total count of requests per endpoint
- `jvm_memory_used_bytes` — how much memory the JVM is using
- `hikaricp_connections_active` — active database connections

**Flow:**
```
Each service exposes /actuator/prometheus
        ↓
Prometheus scrapes these endpoints every 15 seconds
        ↓
Grafana reads from Prometheus and shows dashboards
```

**Tool used:** Micrometer (built into Spring Boot Actuator) + Prometheus + Grafana

---

### 3. Traces — How did a request travel across services?
A trace follows a single request as it moves through multiple services.

**Example — Create Booking request:**
```
[Gateway]        → traceId: 4a1b2c3d  spanId: 1111  (received request)
  [Booking Svc]  → traceId: 4a1b2c3d  spanId: 2222  (processed booking)
    [Vehicle Svc]→ traceId: 4a1b2c3d  spanId: 3333  (checked vehicle)
```

All three spans share the same `traceId` so Zipkin can stitch them into one timeline, showing exactly where time was spent.

**Tool used:** Micrometer Tracing + Brave + Zipkin

---

## Architecture

```
                    ┌─────────────────────────────────────┐
                    │         Your Microservices           │
                    │                                     │
                    │  api-gateway  :8080                 │
                    │  auth-service :8081   → logs with   │
                    │  vehicle-svc  :8082     traceId     │
                    │  booking-svc  :8083                 │
                    └──────┬──────────────┬───────────────┘
                           │              │
                    metrics│              │traces
                           ▼              ▼
                    ┌──────────┐   ┌──────────┐
                    │Prometheus│   │  Zipkin  │
                    │  :9090   │   │  :9411   │
                    └──────┬───┘   └──────────┘
                           │
                           ▼
                    ┌──────────┐
                    │ Grafana  │
                    │  :3000   │
                    └──────────┘
```

---

## What Was Added to Each Service

### Dependencies (pom.xml — all 4 services)
```xml
<!-- Exposes /actuator/prometheus endpoint -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>

<!-- Connects Spring Boot tracing to Brave (Zipkin's tracing library) -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>

<!-- Sends trace data to Zipkin server -->
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
```

### Configuration (application.yml — all 4 services)
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, prometheus, metrics   # expose prometheus endpoint
  metrics:
    tags:
      application: ${spring.application.name}        # tag every metric with service name
  tracing:
    sampling:
      probability: 1.0                               # trace 100% of requests
  zipkin:
    tracing:
      endpoint: http://${ZIPKIN_HOST:localhost}:9411/api/v2/spans

logging:
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"
```

---

## Accessing the Tools

| Tool | URL | Credentials |
|---|---|---|
| Prometheus | http://localhost:9090 | None |
| Grafana | http://localhost:3000 | admin / admin |
| Zipkin | http://localhost:9411 | None |

---

## How to Use Each Tool

### Prometheus — verify metrics are being collected
1. Open http://localhost:9090
2. Go to **Status → Targets** — all 4 services should show `UP`
3. In the search bar type `http_server_requests_seconds_count` and click Execute
4. You will see request counts per service, endpoint, and status code

### Grafana — view dashboards
1. Open http://localhost:3000 → login with `admin / admin`
2. Go to **Connections → Data Sources** — Prometheus is already configured
3. To import a pre-built Spring Boot dashboard:
   - Click **Dashboards → Import**
   - Enter dashboard ID: `17175` (Spring Boot Observability)
   - Select Prometheus as the data source
   - Click Import
4. You will see graphs for request rates, error rates, JVM memory, and response times per service

### Zipkin — trace a request across services
1. Open http://localhost:9411
2. Make any API call through the gateway (e.g. create a booking)
3. In Zipkin click **Run Query**
4. Click on any trace to see the full journey:
   - Which services were called
   - How long each service took
   - Where errors occurred

---

## Step-by-Step: How to See Everything in Action

Follow this exact flow after running `docker-compose up --build`.

---

### Step 1 — Start all services

```bash
docker-compose up --build
```

Wait until you see all services started in the terminal (takes 1-2 minutes). You will see logs like:
```
auth-service    | Started AuthServiceApplication in 4.2 seconds
vehicle-service | Started VehicleServiceApplication in 3.8 seconds
booking-service | Started BookingServiceApplication in 4.1 seconds
api-gateway     | Started ApiGatewayApplication in 3.5 seconds
```

---

### Step 2 — Verify Prometheus is scraping all services

1. Open **http://localhost:9090** in your browser
2. Click **Status** in the top menu → click **Targets**
3. You should see 4 targets, all with state **UP** in green:

```
api-gateway      http://api-gateway:8080/actuator/prometheus       UP
auth-service     http://auth-service:8081/actuator/prometheus      UP
vehicle-service  http://vehicle-service:8082/api/actuator/...      UP
booking-service  http://booking-service:8083/api/actuator/...      UP
```

If any shows **DOWN** — that service is not running yet. Wait a moment and refresh.

---

### Step 3 — Make some API calls in Postman

Make a few requests so there is data to see:

1. `POST /auth/login` — login as ADMIN
2. `POST /api/vehicles` — create a vehicle
3. `POST /api/bookings` — create a booking
4. `PATCH /api/bookings/1/confirm` — confirm it
5. `PATCH /api/bookings/1/complete` — complete it

These calls generate metrics and traces that will appear in Prometheus, Grafana, and Zipkin.

---

### Step 4 — See traces in Zipkin

1. Open **http://localhost:9411**
2. Click the **Run Query** button (top right)
3. You will see a list of recent traces — one per API call you made
4. Click on any trace (e.g. the booking creation one)
5. You will see a timeline like this:

```
api-gateway        ████████████████████████████████████  245ms
  booking-service    ████████████████████████████        198ms
    vehicle-service    ████████                           67ms
```

This shows:
- The request entered the gateway and took 245ms total
- Booking service used 198ms of that
- Vehicle service (called internally) used 67ms

If there was an error, the failing span is shown in **red** so you know exactly which service failed.

---

### Step 5 — Import Grafana Dashboard

1. Open **http://localhost:3000**
2. Login with username `admin` and password `admin`
3. In the left sidebar click **Dashboards** → **Import**
4. In the **Import via grafana.com** field enter: `17175`
5. Click **Load**
6. Under **Prometheus** dropdown select **Prometheus**
7. Click **Import**

You now have a full dashboard showing:

| Panel | What it shows |
|---|---|
| Request Rate | How many requests per second each service is handling |
| Error Rate | Percentage of requests returning 4xx or 5xx errors |
| Response Time (p99) | Slowest 1% of requests — useful for spotting bottlenecks |
| JVM Memory | Heap memory used by each service |
| Active DB Connections | HikariCP connection pool usage |

---

### Step 6 — Check logs with traceId

When you look at terminal logs after making an API call, you will see:

```
INFO  [booking-service,4a1b2c3d4e5f6a7b,8e9f0a1b2c3d4e5f] BookingServiceImpl - Booking created: id=1
INFO  [vehicle-service,4a1b2c3d4e5f6a7b,1a2b3c4d5e6f7a8b] VehicleServiceImpl - Status updated to BOOKED
```

Both lines share the same **traceId** (`4a1b2c3d4e5f6a7b`).

Copy that traceId and paste it into the Zipkin search box to find the exact trace for that request.

---

### Quick Reference — What to open and where

| Tool | URL | What to do |
|---|---|---|
| Prometheus | http://localhost:9090/targets | Check all 4 services are UP |
| Zipkin | http://localhost:9411 | Click Run Query → click a trace |
| Grafana | http://localhost:3000 | Login admin/admin → import dashboard 17175 |

---

## Example: Tracing a Create Booking Request

When you call `POST /api/bookings`, Zipkin shows:

```
Total: 245ms

api-gateway        ████████████████████████████████████████  245ms
  booking-service    ████████████████████████████████        198ms
    vehicle-service    ████████                               67ms
```

This tells you:
- Gateway overhead: ~47ms
- Booking service processing: ~131ms
- Vehicle service call: ~67ms

If something is slow or failing, you can pinpoint exactly which service is the problem.

---

## Files Added

```
observability/
├── prometheus.yml                              ← tells Prometheus where to scrape metrics
├── grafana/
│   └── provisioning/
│       └── datasources/
│           └── datasource.yml                 ← auto-configures Prometheus in Grafana
└── README.md                                  ← this file
```

**docker-compose.yml** — 3 new containers added:
- `zipkin` on port 9411
- `prometheus` on port 9090 (mounts `prometheus.yml`)
- `grafana` on port 3000 (mounts grafana provisioning config)

---

## Start Everything

```bash
docker-compose up --build
```

All observability tools start automatically alongside your services. No separate steps needed.
