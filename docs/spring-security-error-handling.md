# Spring Security Error Handling in Microservices

> **Service affected:** `enrollment-service`
> **Phase:** 4 — Enrollment Service implementation
> **Date:** 2026-03-27

---

## The Problem

After completing the enrollment service, three test cases returned **`401 Unauthorized`** instead of the expected HTTP status:

| Test Case | Expected Response | Actual Response |
|-----------|------------------|-----------------|
| Enroll in the same course again | `409 Conflict` | `401 Unauthorized` |
| Enroll in an unpublished course | `400 Bad Request` | `401 Unauthorized` |
| Enroll with Course Service down | `503 Service Unavailable` | `401 Unauthorized` |

All three requests were made **with a valid JWT token**. Authentication was succeeding — so `401` made no logical sense from the business perspective.

---

## Root Cause

The application has **two separate layers** that handle errors, and they do not share control:

```
HTTP Request
    │
    ▼
┌─────────────────────────────┐
│   Spring Security Filters   │  ← Layer 1: handles auth/authz errors
│   (JwtAuthenticationFilter) │
└─────────────┬───────────────┘
              │  (only if security passes)
              ▼
┌─────────────────────────────┐
│   DispatcherServlet         │  ← Layer 2: handles application errors
│   → Controller              │
│   → Service                 │
│   → GlobalExceptionHandler  │
└─────────────────────────────┘
```

`@RestControllerAdvice` / `GlobalExceptionHandler` **only operates inside Layer 2**. It never sees exceptions that originate in Layer 1.

The original `SecurityConfig` used:

```java
// BEFORE — raw status with no body
.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
.accessDeniedHandler((req, res, ex) ->
        res.sendError(HttpStatus.FORBIDDEN.value(), ...))
```

`HttpStatusEntryPoint` short-circuits the entire request pipeline and writes a **raw empty HTTP response** the moment the security layer decides the request is not authenticated. This happened because the `JwtAuthenticationFilter` (from `jwt-common`) sets the security context but does **not** prevent the filter chain from continuing — so if anything downstream caused a re-evaluation of authentication state, the entry point would fire, bypassing `GlobalExceptionHandler` entirely.

The result: even though the token was valid and the business logic would have thrown `DuplicateResourceException` (409) or `ServiceUnavailableException` (503), those exceptions never ran. The `authenticationEntryPoint` fired first with a raw `401`.

---

## Why `GlobalExceptionHandler` Could Not Help

You already had a proper `GlobalExceptionHandler` with `@RestControllerAdvice` that correctly mapped every custom exception to the right HTTP status. Here is what each class in the `exception` package was responsible for:

| Class | Exception mapped | Expected status |
|---|---|---|
| `GlobalExceptionHandler` | Orchestrates all handlers below | — |
| `DuplicateResourceException` | Already enrolled | `409 Conflict` |
| `InvalidRequestException` | Unpublished / missing course | `400 Bad Request` |
| `ServiceUnavailableException` | Course Service down | `503 Service Unavailable` |
| `ForbiddenOperationException` | Wrong ownership | `403 Forbidden` |
| `ErrorResponse` | Shared JSON body shape | — |

They are all **correct**. The problem was never in the `exception` package.

### The two-layer wall

Spring Boot runs two completely separate processing layers in sequence on every request:

```
Incoming HTTP Request
        │
        ▼
 ┌──────────────────────────────────────────────┐
 │  LAYER 1 — Servlet Filter Chain              │
 │                                              │
 │  • JwtAuthenticationFilter    (your filter)  │
 │  • ExceptionTranslationFilter (Spring's)     │
 │  • Other Spring Security filters             │
 │                                              │
 │  @RestControllerAdvice does NOT exist here.  │
 │  GlobalExceptionHandler CANNOT see in here.  │
 └──────────────────────────┬───────────────────┘
                            │  (only if security fully passes)
                            ▼
 ┌──────────────────────────────────────────────┐
 │  LAYER 2 — Spring MVC DispatcherServlet      │
 │                                              │
 │  • Controller                                │
 │  • Service  ← your exceptions are thrown here│
 │  • GlobalExceptionHandler ← catches them     │
 │                                              │
 └──────────────────────────────────────────────┘
```

`@RestControllerAdvice` is a **Spring MVC concept**. It is wired into the `DispatcherServlet` (Layer 2). It has **zero visibility into Layer 1** — the filter chain runs before the dispatcher even starts.

### The hidden actor: `ExceptionTranslationFilter`

Spring Security installs a built-in filter called `ExceptionTranslationFilter` inside Layer 1. Its job is to watch for two specific exception types bubbling up from anywhere in the chain:

- `AuthenticationException` → calls the configured `authenticationEntryPoint`
- `AccessDeniedException` → calls the configured `accessDeniedHandler`

When it catches either, it **immediately writes the HTTP response and stops processing** — the `DispatcherServlet` is never reached.

### Step-by-step trace for "enroll in same course" (before the fix)

```
1. Request arrives with a valid JWT  ✓
2. JwtAuthenticationFilter validates token → sets SecurityContextHolder  ✓
3. ExceptionTranslationFilter: user is authenticated, continue  ✓
4. DispatcherServlet → EnrollmentController → EnrollmentService.enroll()  ✓
5. isEnrolled() returns true → throws DuplicateResourceException  ✓
6. Exception propagates back up through the MVC layer...
7. ExceptionTranslationFilter wraps it in a security exception internally
8. ExceptionTranslationFilter calls authenticationEntryPoint (old: HttpStatusEntryPoint)
9. HttpStatusEntryPoint writes raw HTTP 401, no body  ✗
10. GlobalExceptionHandler never runs  ✗
```

Step 7 is the key: `ExceptionTranslationFilter` sees an unhandled exception escaping the chain and routes it through the security error path, regardless of what type the original exception was.

**The same trace applies for the other two cases** — the `InvalidRequestException` (400) and `ServiceUnavailableException` (503) are both intercepted at step 7 before `GlobalExceptionHandler` ever sees them.

### One-sentence summary

> `GlobalExceptionHandler` can only catch what the **DispatcherServlet** sees — `ExceptionTranslationFilter` runs *before* the dispatcher, and when it intercepts an exception, the response is written right there in Layer 1, bypassing `@RestControllerAdvice` entirely.

---

## The Solution

### Two custom security handlers were created

Both implement the correct Spring Security interface and write the **same `ErrorResponse` JSON shape** used by `GlobalExceptionHandler` — ensuring a consistent API contract regardless of where the error originates.

#### 1. `CustomAuthenticationEntryPoint`
**Triggered when:** a request arrives with no token, an expired token, or a malformed token.

```java
// security/CustomAuthenticationEntryPoint.java
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        // Writes JSON ErrorResponse with 401 directly to HttpServletResponse
    }
}
```

#### 2. `CustomAccessDeniedHandler`
**Triggered when:** a valid, authenticated user tries to access an endpoint they don't have the required role for (e.g., a STUDENT hitting an INSTRUCTOR-only endpoint).

```java
// security/CustomAccessDeniedHandler.java
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        // Writes JSON ErrorResponse with 403 directly to HttpServletResponse
    }
}
```

#### 3. `SecurityConfig` updated to wire both in

```java
// AFTER — structured JSON responses
.exceptionHandling(exception -> exception
    .authenticationEntryPoint(authenticationEntryPoint)   // custom JSON 401
    .accessDeniedHandler(accessDeniedHandler)             // custom JSON 403
)
```

### Why write directly to `HttpServletResponse`?

Because at the security filter level, the `DispatcherServlet` has not yet been invoked. There is no `ModelAndView`, no controller context, no `@ExceptionHandler` — the only way to write a response is directly via `HttpServletResponse.getWriter()`. The `ObjectMapper` is instantiated inline in each handler because Spring's Jackson `ObjectMapper` bean is not reliably injectable at filter-chain construction time without additional configuration.

---

## Error Flow After the Fix

```
HTTP Request (valid JWT)
    │
    ▼
JwtAuthenticationFilter → sets SecurityContext ✓
    │
    ▼
Controller → EnrollmentService
    │
    ├── already enrolled?         → DuplicateResourceException
    │                                  → GlobalExceptionHandler → 409 ✓
    │
    ├── course unpublished?       → InvalidRequestException
    │                                  → GlobalExceptionHandler → 400 ✓
    │
    └── CourseService down?       → ServiceUnavailableException
                                       → GlobalExceptionHandler → 503 ✓


HTTP Request (missing/invalid JWT)
    │
    ▼
JwtAuthenticationFilter → does NOT set SecurityContext
    │
    ▼
Spring Security → CustomAuthenticationEntryPoint → JSON 401 ✓
```

---

## Why This Matters

### 1. Correct status codes are an API contract
A client receiving `401` is supposed to react by refreshing or re-acquiring a token. A `409` tells it the operation is already done. A `503` tells it to retry later. Wrong codes cause clients to take the wrong action — silently making the API unreliable.

### 2. This bug will appear in every future service
`progress-service`, `notification-service` — any service using `spring-security` with `JwtAuthenticationFilter` will have the same issue if the `SecurityConfig` uses `HttpStatusEntryPoint`. This pattern must be applied consistently.

### 3. Inter-service calls are affected too
When `progress-service` calls `enrollment-service` and gets `401` instead of `503`, its `CourseServiceClient` error handler will misclassify the error (it only catches `HttpClientErrorException.Unauthorized` vs `ResourceAccessException`). The cascade of wrong status codes across services becomes very hard to debug.

### 4. It teaches the two-layer mental model
Understanding that Spring Security's filter chain and Spring MVC's dispatcher are separate execution contexts is fundamental to building secure Spring Boot applications. The fix reinforces this architecture.

---

## Apply This Pattern to All Services

Every future service using Spring Security should have these three components:

```
service/
└── src/main/java/.../security/
    ├── SecurityConfig.java                   ← wires everything together
    ├── CustomAuthenticationEntryPoint.java   ← JSON 401 for missing/bad token
    └── CustomAccessDeniedHandler.java        ← JSON 403 for wrong role
```

> ⚠️ **Do not use `HttpStatusEntryPoint` or `res.sendError()` in production services.**
> These return no body and break any API client that expects a consistent error shape.

---

## Files Changed

| File | Change |
|------|--------|
| `security/CustomAuthenticationEntryPoint.java` | **[NEW]** — JSON 401 entry point |
| `security/CustomAccessDeniedHandler.java` | **[NEW]** — JSON 403 access denied handler |
| `security/SecurityConfig.java` | **[MODIFIED]** — injects and wires both handlers |
