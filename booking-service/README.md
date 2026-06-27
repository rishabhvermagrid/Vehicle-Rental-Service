# Booking Service

Manages vehicle bookings in the Vehicle Rental System. Handles booking creation, retrieval, cancellation, and completion. Communicates with `vehicle-service` internally to verify availability and update vehicle status.

---

## Tech Stack

- Java 21
- Spring Boot 3.2.5
- Spring Security 6 (stateless, header-based)
- Spring Data JPA + MySQL
- WebClient (Spring WebFlux) for internal HTTP calls
- Lombok

---

## Port

`8083` — accessed through API Gateway at `localhost:8080`

---

## Architecture

```
Client (Postman)
     |
     ▼
API Gateway :8080       ← validates SESSION_ID cookie, injects X-User-Id + X-User-Role
     |
     ▼
Booking Service :8083   ← enforces role rules via Spring Security
     |
     ▼
Vehicle Service :8082   ← internal call with ADMIN identity to check/update vehicle
```

Authentication is **not** handled here. The API Gateway validates the session with `auth-service` and injects two headers before forwarding:

| Header | Example |
|---|---|
| `X-User-Id` | `3` |
| `X-User-Role` | `CUSTOMER` |

`HeaderAuthenticationFilter` reads these headers and sets the Spring Security context.

---

## Role-Based Access

| Endpoint | CUSTOMER | STAFF | ADMIN |
|---|---|---|---|
| `POST /api/bookings` | Own only | Any customer | Any customer |
| `GET /api/bookings` | No | Yes | Yes |
| `GET /api/bookings/my` | Yes | No | No |
| `GET /api/bookings/{id}` | Own only | Yes | Yes |
| `PATCH /api/bookings/{id}/cancel` | Own only | Yes | Yes |
| `PATCH /api/bookings/{id}/complete` | No | Yes | Yes |

---

## API Endpoints

All paths are prefixed with `/api/bookings` and routed through the gateway at `localhost:8080`.

### Create Booking

```
POST /api/bookings
```

**Request Body:**
```json
{
  "vehicleId": 1,
  "customerId": 5,
  "startDate": "2026-07-01",
  "endDate": "2026-07-05"
}
```

- `customerId` is **ignored** for CUSTOMER role — the gateway-injected `X-User-Id` is used instead.
- `customerId` is **required** for STAFF/ADMIN booking on behalf of a customer.
- `totalDays` and `totalAmount` are calculated server-side.

**Response `201`:**
```json
{
  "success": true,
  "message": "Booking created successfully.",
  "data": {
    "id": 1,
    "vehicleId": 1,
    "customerId": 5,
    "startDate": "2026-07-01",
    "endDate": "2026-07-05",
    "totalDays": 4,
    "totalAmount": 200.00,
    "status": "PENDING",
    "createdAt": "2026-06-28T10:00:00",
    "updatedAt": "2026-06-28T10:00:00"
  },
  "timestamp": "2026-06-28T10:00:00"
}
```

---

### Get All Bookings (ADMIN / STAFF)

```
GET /api/bookings
```

Returns all bookings in the system.

---

### Get My Bookings (CUSTOMER)

```
GET /api/bookings/my
```

Returns bookings belonging to the logged-in customer (uses `X-User-Id`).

---

### Get Booking by ID

```
GET /api/bookings/{id}
```

CUSTOMER can only fetch their own booking — returns `403` otherwise.

---

### Cancel Booking

```
PATCH /api/bookings/{id}/cancel
```

- CUSTOMER can only cancel their own bookings.
- Only `PENDING` or `CONFIRMED` bookings can be cancelled.
- Releases the vehicle back to `AVAILABLE` status in vehicle-service.

---

### Complete Booking (ADMIN / STAFF)

```
PATCH /api/bookings/{id}/complete
```

- Only `CONFIRMED` bookings can be completed.
- Releases the vehicle back to `AVAILABLE` status in vehicle-service.

---

## Booking Status Flow

```
PENDING → CONFIRMED → COMPLETED
    |          |
    └──────────┴──→ CANCELLED
```

| Status | Description |
|---|---|
| `PENDING` | Booking created, vehicle marked RENTED |
| `CONFIRMED` | Confirmed by staff/admin |
| `CANCELLED` | Cancelled; vehicle released back to AVAILABLE |
| `COMPLETED` | Rental period ended; vehicle released back to AVAILABLE |

---

## Business Rules

- `endDate` must be after `startDate`
- Vehicle must have status `AVAILABLE` at time of booking
- Vehicle cannot have an existing `PENDING` or `CONFIRMED` booking
- `totalDays = endDate - startDate` (in days)
- `totalAmount = totalDays × vehicle.pricePerDay`

---

## Internal Service Communication

Booking service calls vehicle-service using **ADMIN identity** regardless of the actual user's role:

```
X-User-Id:   0
X-User-Role: ADMIN
```

This allows it to read any vehicle and update vehicle status without being blocked by vehicle-service's role rules.

**Calls made:**
- `GET /api/vehicles/{id}` — fetch vehicle details and price
- `PATCH /api/vehicles/{id}/status` — update status to `RENTED` or `AVAILABLE`

---

## Error Responses

All errors follow the same structure:

```json
{
  "success": false,
  "message": "Error description here.",
  "data": null,
  "timestamp": "2026-06-28T10:00:00"
}
```

| Scenario | HTTP Status |
|---|---|
| Booking not found | `404` |
| Vehicle not found | `404` |
| Vehicle not available | `409` |
| Vehicle service down | `503` |
| Invalid dates or status transition | `400` |
| Accessing another user's booking | `403` |
| Unauthenticated (no session) | `401` |
| Validation errors | `400` with field-level map |

---

## Configuration

`src/main/resources/application.yml`

| Property | Env Var | Default |
|---|---|---|
| Server port | — | `8083` |
| MySQL host | `MYSQL_HOST` | `localhost` |
| MySQL port | `MYSQL_PORT` | `3306` |
| MySQL database | `MYSQL_DATABASE` | `booking_db` |
| MySQL user | `MYSQL_USER` | `root` |
| MySQL password | `MYSQL_PASSWORD` | `root` |
| Vehicle service URL | `VEHICLE_SERVICE_HOST` | `localhost` |

---

## Running Locally (via Docker Compose)

From the project root:

```bash
# Start all services
docker-compose up --build

# Stop all services
docker-compose down

# Wipe all data and restart fresh
docker-compose down -v && docker-compose up --build
```

The booking service depends on `booking-mysql` being healthy before it starts.

---

## Project Structure

```
booking-service/
├── src/main/java/com/rental/booking/
│   ├── BookingServiceApplication.java
│   ├── client/
│   │   └── VehicleClient.java          # WebClient calls to vehicle-service
│   ├── config/
│   │   ├── HeaderAuthenticationFilter.java
│   │   ├── SecurityConfig.java
│   │   └── WebClientConfig.java
│   ├── controller/
│   │   └── BookingController.java
│   ├── dto/
│   │   ├── request/CreateBookingRequest.java
│   │   └── response/
│   │       ├── ApiResponse.java
│   │       ├── BookingResponse.java
│   │       └── VehicleResponse.java
│   ├── entity/
│   │   └── Booking.java
│   ├── enums/
│   │   └── BookingStatus.java
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java
│   │   ├── AccessDeniedException.java
│   │   ├── BookingNotFoundException.java
│   │   ├── InvalidBookingException.java
│   │   ├── VehicleNotAvailableException.java
│   │   ├── VehicleNotFoundException.java
│   │   └── VehicleServiceUnavailableException.java
│   ├── repository/
│   │   └── BookingRepository.java
│   └── service/
│       ├── BookingService.java
│       └── impl/BookingServiceImpl.java
├── Dockerfile
└── README.md
```
