# Auth Service

A production-ready session-based authentication microservice built with Spring Boot 3, Java 21, MySQL, and Redis.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.3.5 |
| Language | Java 21 |
| Security | Spring Security 6 |
| Session Store | Spring Session + Redis |
| Database | MySQL 8 + Spring Data JPA |
| Password Hashing | BCrypt |
| Build Tool | Maven |
| Containerization | Docker + Docker Compose |

---

## How It Works

- Users are stored in **MySQL** with BCrypt-hashed passwords.
- On login, a session is created and stored in **Redis** with a 1-hour TTL.
- The session ID is sent to the client as an **HttpOnly cookie** named `SESSION_ID`.
- Every subsequent request carries the cookie automatically — no tokens, no Authorization headers.
- On logout, the session is invalidated in Redis and the cookie is expired on the client.

```
Client                   Auth Service              Redis                MySQL
  |                           |                      |                    |
  |-- POST /auth/login ------->|                      |                    |
  |                           |-- loadUser ---------->|                   --->
  |                           |-- saveSession ------->|                    |
  |<-- 200 + SESSION_ID cookie-|                      |                    |
  |                           |                      |                    |
  |-- GET /auth/me (+ cookie)->|                      |                    |
  |                           |-- loadSession ------->|                    |
  |<-- 200 user data ----------|                      |                    |
```

---

## Roles

The system supports three roles for a vehicle rental context:

| Role | Description | Access |
|---|---|---|
| `CUSTOMER` | Default role on registration | Own profile, session management |
| `STAFF` | Rental staff members | `/staff/**` + all CUSTOMER endpoints |
| `ADMIN` | System administrator | `/admin/**` + all endpoints |

Every new registration defaults to `CUSTOMER`. An admin can promote any user via `PUT /admin/users/{id}/role`.

---

## Project Structure

```
src/main/java/org/rishabh/authservice/
├── AuthServiceApplication.java
├── controller/
│   ├── AuthController.java          # Auth endpoints (register, login, logout…)
│   └── AdminController.java         # Admin-only endpoints (users, role assignment)
├── service/
│   ├── AuthService.java             # Interface
│   ├── AdminService.java            # Interface
│   └── impl/
│       ├── AuthServiceImpl.java     # Business logic
│       ├── AdminServiceImpl.java    # Admin business logic
│       └── CustomUserDetailsService.java  # Spring Security user loader
├── repository/
│   └── UserRepository.java          # JPA repository
├── entity/
│   └── User.java                    # users table (roles: CUSTOMER, STAFF, ADMIN)
├── dto/
│   ├── request/
│   │   ├── RegisterRequest.java
│   │   ├── LoginRequest.java
│   │   ├── ChangePasswordRequest.java
│   │   └── AssignRoleRequest.java   # Admin role assignment
│   └── response/
│       ├── ApiResponse.java         # Generic response wrapper
│       └── UserResponse.java
├── config/
│   ├── SecurityConfig.java          # Spring Security + role-based rules
│   └── SessionConfig.java           # SESSION_ID cookie configuration
└── exception/
    ├── GlobalExceptionHandler.java  # Centralized error handling
    ├── UserAlreadyExistsException.java
    ├── UserNotFoundException.java
    └── InvalidCredentialsException.java
```

---

## API Endpoints

### Auth (Public + Authenticated)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/auth/register` | Public | Create a new account (defaults to CUSTOMER) |
| `POST` | `/auth/login` | Public | Login and receive session cookie |
| `POST` | `/auth/logout` | Authenticated | Invalidate session |
| `GET` | `/auth/me` | Authenticated | Get current user profile |
| `GET` | `/auth/validate` | Public | Check if session is active |
| `PUT` | `/auth/change-password` | Authenticated | Change password (invalidates session) |

### Admin (ADMIN role only)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/admin/users` | ADMIN | List all users with their roles |
| `PUT` | `/admin/users/{id}/role` | ADMIN | Assign CUSTOMER / STAFF / ADMIN to a user |

All responses follow this structure:

```json
{
  "success": true,
  "message": "...",
  "data": { },
  "timestamp": "2026-06-27T10:00:00"
}
```

---

## Running with Docker (Recommended)

Requires: Docker + Docker Compose

```bash
docker-compose up --build
```

This starts three containers:

| Container | Image | Port |
|---|---|---|
| `auth-service` | Built from Dockerfile | `8081` |
| `auth-mysql` | mysql:8.0 | `3307` (host) → `3306` (container) |
| `auth-redis` | redis:7-alpine | Internal only |

The service starts only after MySQL and Redis pass their health checks.

**Stop everything:**
```bash
docker-compose down
```

**Stop and wipe all data (volumes):**
```bash
docker-compose down -v
```

---

## Running Locally

Requires: Java 21, Maven, MySQL on port 3306, Redis on port 6379.

```bash
./mvnw spring-boot:run
```

Make sure `src/main/resources/application.yml` has the correct DB and Redis credentials for your local setup.

---

## Configuration

Key settings in `application.yml`:

| Property | Default | Description |
|---|---|---|
| `server.port` | `8081` | Application port |
| `spring.datasource.url` | `localhost:3306/auth_db` | MySQL connection |
| `spring.data.redis.host` | `localhost` | Redis host (overridden to `redis` in Docker) |
| `spring.session.timeout` | `3600s` | Session TTL (1 hour) |

In Docker Compose, these are overridden via environment variables:
```
SPRING_DATASOURCE_URL
SPRING_DATA_REDIS_HOST
SPRING_DATA_REDIS_PORT
```

---

## Testing with Postman

**1. Register**
```
POST http://localhost:8081/auth/register
Content-Type: application/json

{
  "username": "rishabh",
  "email": "rishabh@example.com",
  "password": "Secret@123"
}
```

Password rules: min 8 chars, must include uppercase, lowercase, digit, and a special character (`@$!%*?&`).

**2. Login**
```
POST http://localhost:8081/auth/login
Content-Type: application/json

{
  "email": "rishabh@example.com",
  "password": "Secret@123"
}
```

Postman automatically stores the `SESSION_ID` cookie. All subsequent requests send it automatically.

**3. Get current user**
```
GET http://localhost:8081/auth/me
```

**4. Validate session**
```
GET http://localhost:8081/auth/validate
```

**5. Change password**
```
PUT http://localhost:8081/auth/change-password
Content-Type: application/json

{
  "currentPassword": "Secret@123",
  "newPassword": "NewPass@456",
  "confirmNewPassword": "NewPass@456"
}
```

Session is invalidated after a password change — you must login again.

**6. Logout**
```
POST http://localhost:8081/auth/logout
```

**7. List all users (ADMIN only)**
```
GET http://localhost:8081/admin/users
```

**8. Assign a role (ADMIN only)**
```
PUT http://localhost:8081/admin/users/5/role
Content-Type: application/json

{
  "role": "STAFF"
}
```

Valid values for `role`: `CUSTOMER`, `STAFF`, `ADMIN`.

---

## Security Notes

- Passwords are hashed with **BCrypt** — plain-text passwords are never stored.
- Sessions are stored server-side in Redis — the client only holds an opaque session ID.
- The `SESSION_ID` cookie is **HttpOnly** (not accessible via JavaScript) and **SameSite=Lax**.
- Set `serializer.setUseSecureCookie(true)` in `SessionConfig.java` for HTTPS in production.
- CSRF protection is disabled (suitable for API clients). Enable it if this API is consumed by a browser-based SPA.
