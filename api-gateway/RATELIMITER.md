# Rate Limiting

Rate limiting controls how many requests a single user can make in a given time window. Without it, one user could flood the system with thousands of requests per second, crashing the services for everyone else.

---

## How It Works — Token Bucket Algorithm

Spring Cloud Gateway uses the **Token Bucket** algorithm backed by Redis.

```
Each user gets a "bucket" in Redis that holds tokens.

Every second, the bucket refills with N tokens (replenishRate).
Each request consumes 1 token.
If the bucket is empty → request is rejected with 429 Too Many Requests.
The bucket can hold a maximum of M tokens (burstCapacity) to allow short bursts.
```

**Example with replenishRate=10, burstCapacity=20:**

```
Second 1: User makes 20 requests at once → all allowed (burst capacity)
Second 2: Bucket refills with 10 tokens → user can make 10 requests
Second 3: User makes 15 requests → 10 allowed, 5 rejected with 429
```

---

## Strategy — Rate Limit by User ID

Every request to `/api/**` passes through `SessionAuthFilter` first, which injects the `X-User-Id` header. The rate limiter reads this header to identify who is making the request.

```
Request arrives at Gateway
        ↓
SessionAuthFilter validates session → injects X-User-Id: 3
        ↓
RequestRateLimiter reads X-User-Id: 3
        ↓
Checks Redis: "how many tokens does user 3 have left?"
        ↓
Tokens available  → forward to service
Tokens empty      → return 429 Too Many Requests
```

Each user gets their **own independent bucket** in Redis. One user hitting the limit has zero impact on other users.

---

## Implementation

### 1. Dependency (pom.xml)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
</dependency>
```

Redis reactive client is required because Spring Cloud Gateway is built on WebFlux (reactive/non-blocking). The standard Redis client won't work here.

### 2. KeyResolver Bean (RateLimiterConfig.java)

```java
@Bean
public KeyResolver userKeyResolver() {
    return exchange -> {
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        return Mono.just(userId != null ? userId : "anonymous");
    };
}
```

`KeyResolver` tells the rate limiter **what to use as the bucket key** in Redis.

- Reads the `X-User-Id` header (already injected by `SessionAuthFilter`)
- Returns it as a `Mono<String>` (reactive — returns a value asynchronously)
- Falls back to `"anonymous"` if the header is somehow missing

Each unique `X-User-Id` value gets its own token bucket in Redis:
```
Redis keys:
  request_rate_limiter.{1}.tokens   → tokens left for user 1
  request_rate_limiter.{2}.tokens   → tokens left for user 2
  request_rate_limiter.{3}.tokens   → tokens left for user 3
```

### 3. Route Configuration (application.yml)

Rate limiter is added as a filter on each protected route:

```yaml
- name: RequestRateLimiter
  args:
    redis-rate-limiter.replenishRate: 10     # tokens added per second
    redis-rate-limiter.burstCapacity: 20     # max tokens bucket can hold
    redis-rate-limiter.requestedTokens: 1    # tokens consumed per request
    key-resolver: "#{@userKeyResolver}"      # which bean to use as key
```

`"#{@userKeyResolver}"` is Spring Expression Language (SpEL) — it references the `userKeyResolver` bean from `RateLimiterConfig.java`.

---

## Rate Limits Per Route

| Route | replenishRate | burstCapacity | Reason |
|---|---|---|---|
| `GET/POST /api/vehicles/**` | 20 req/s | 40 | Browsing vehicles is frequent — higher limit |
| `GET/POST/PATCH /api/bookings/**` | 10 req/s | 20 | Booking actions are sensitive — lower limit |
| `/auth/**`, `/admin/**` | No limit (yet) | — | Will add IP-based limit later |

---

## What the Client Receives When Rate Limited

**HTTP Status:** `429 Too Many Requests`

**Response Headers:**
```
X-RateLimit-Remaining: 0       ← tokens left in the bucket
X-RateLimit-Replenish-Rate: 10 ← how fast the bucket refills
X-RateLimit-Burst-Capacity: 20 ← max bucket size
```

These headers are returned on every request (not just 429), so the client always knows how many requests it has left.

---

## Redis Storage

The token bucket state is stored in Redis — the same Redis instance already used for session storage.

```
Session storage keys:   auth:session:*          (Spring Session)
Rate limiter keys:      request_rate_limiter.*   (Spring Cloud Gateway)
```

They share Redis but use different key prefixes so there is no conflict.

---

## Full Request Flow (with rate limiting)

```
Client (Postman)
     │
     ▼
API Gateway :8080
     │
     ├─ 1. SessionAuthFilter
     │       reads SESSION_ID cookie
     │       calls auth-service /auth/validate
     │       calls auth-service /auth/me
     │       injects X-User-Id and X-User-Role headers
     │
     ├─ 2. RequestRateLimiter (per route)
     │       reads X-User-Id from header
     │       checks token bucket in Redis for this user
     │       ├─ tokens available → deduct 1 token → continue
     │       └─ no tokens → return 429 immediately (service never called)
     │
     └─ 3. Forward to downstream service
             vehicle-service  :8082
             booking-service  :8083
```

---

## What Is NOT Rate Limited (yet)

| Route | Status | Plan |
|---|---|---|
| `POST /auth/login` | Not limited | Add IP-based limit to prevent brute-force |
| `POST /auth/register` | Not limited | Add IP-based limit to prevent spam |
| `GET /auth/validate` | Not limited | Internal gateway use only |
| `/admin/**` | Not limited | Admin users trusted |

IP-based rate limiting for auth routes will be added later using a separate `KeyResolver` that reads `X-Forwarded-For` or `RemoteAddr` instead of `X-User-Id`.

---

## Files Changed

| File | Change |
|---|---|
| `api-gateway/pom.xml` | Added `spring-boot-starter-data-redis-reactive` |
| `api-gateway/src/.../config/RateLimiterConfig.java` | New file — defines `userKeyResolver` bean |
| `api-gateway/src/main/resources/application.yml` | Added Redis config + `RequestRateLimiter` filter to vehicle and booking routes |
| `docker-compose.yml` | Added `REDIS_HOST: redis` env var to api-gateway |

---

## How to Test Rate Limiting

1. Start everything: `docker-compose up --build`
2. Login in Postman to get a session
3. Send `GET /api/vehicles/available` rapidly more than 40 times in 1 second
4. After the 40th request you will receive:

```
HTTP 429 Too Many Requests
```

With headers:
```
X-RateLimit-Remaining: 0
X-RateLimit-Replenish-Rate: 20
X-RateLimit-Burst-Capacity: 40
```

5. Wait 1 second → bucket refills with 20 tokens → requests work again
