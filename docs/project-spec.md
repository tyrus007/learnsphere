# LearnSphere — Project Specification

---

## 1. Project Overview

**LearnSphere** is a backend-only, microservices-based e-learning platform. It enables instructors to create and publish courses, and enables students to enroll in courses, track lesson-level progress, and receive notifications. The system is composed of independently deployable services that communicate over HTTP, all exposed through a single API Gateway.

---

## 2. Project Goals

- Demonstrate database-per-service isolation across all domain services.
- Implement synchronous service-to-service REST communication with JWT forwarding.
- Differentiate critical dependencies from non-critical, fire-and-forget side effects.
- Provide a single public entry point via API Gateway while keeping internal APIs inaccessible from outside.
- Apply observability patterns: structured logging, correlation IDs, health checks, and request timeouts.

---

## 3. Technology Stack

| Layer              | Technology                                          |
|--------------------|-----------------------------------------------------|
| Language           | Java 21                                             |
| Framework          | Spring Boot 3.x                                     |
| Security           | Spring Security + JWT (JJWT 0.12.x)                |
| Database           | PostgreSQL 16                                       |
| ORM                | Spring Data JPA (Hibernate)                         |
| Migrations         | Flyway                                              |
| HTTP Client        | Spring `RestClient` (Spring 6.1+)                   |
| Gateway            | Spring Cloud Gateway (WebFlux-based)                |
| Containerization   | Docker + Docker Compose                             |
| Build Tool         | Maven                                               |
| Validation         | Jakarta Bean Validation (`@NotBlank`, `@Email`, etc.) |
| Testing / API      | Postman + Newman CLI                                |
| Shared Module      | `jwt-common` Maven JAR (installed to local `.m2`)   |

---

## 4. Repository & Directory Structure

```
learnsphere/
├── docker-compose.yml
├── .env
├── init-databases.sql
├── docs/
│   └── project-spec.md
├── postman/
│   ├── LearnSphere.postman_collection.json
│   └── local.postman_environment.json
├── common/
│   └── jwt-common/
│       ├── pom.xml
│       └── src/
│           └── main/java/com/ashique/learnsphere/
│               ├── JwtUtil.java
│               ├── JwtAuthenticationFilter.java
│               └── AuthenticatedUser.java
├── identity-service/
├── course-service/
├── enrollment-service/
├── progress-service/
├── notification-service/
└── gateway-service/
```

---

## 5. Shared Module — `jwt-common`

**Maven coordinates:**
- `groupId`: `com.ashique.learnsphere`
- `artifactId`: `jwt-common`
- `packaging`: `jar`

**Dependencies:**
- `spring-boot-starter-security`
- `jjwt-api` (0.12.x)
- `jjwt-impl`
- `jjwt-jackson`

**Classes:**

### `JwtUtil`
| Method | Description |
|--------|-------------|
| `generateToken(userId, email, role)` | Creates a signed JWT with userId, email, and role claims |
| `extractClaims(token)` | Parses and returns all claims from a token |
| `isTokenValid(token)` | Returns true if the token is well-formed, signed, and not expired |

### `JwtAuthenticationFilter`
- Extends `OncePerRequestFilter`
- Reads `Authorization: Bearer <token>` header
- Validates token via `JwtUtil`
- Sets `UsernamePasswordAuthenticationToken` containing `userId`, `email`, `role` into `SecurityContextHolder`

### `AuthenticatedUser`
- A simple record/class
- Fields: `userId`, `email`, `role`
- Populated from the Security Context by downstream service code

**Constraint:** This module must contain only JWT parsing and the security filter. No domain objects, business logic, or service-specific code.

---

## 6. Infrastructure

### 6.1 PostgreSQL (Docker Compose)

- Image: `postgres:16`
- Container name: `learnsphere-postgres`
- Port: `5432:5432`
- Credentials via `.env`: `POSTGRES_USER=learnsphere`, `POSTGRES_PASSWORD=learnsphere`
- Named volume: `pgdata`

**Databases (created via `init-databases.sql`):**

```sql
CREATE DATABASE identity_db;
CREATE DATABASE course_db;
CREATE DATABASE enrollment_db;
CREATE DATABASE progress_db;
CREATE DATABASE notification_db;
```

### 6.2 Docker Compose Services

| Service               | Port  | Database         |
|-----------------------|-------|------------------|
| `postgres`            | 5432  | —                |
| `identity-service`    | 8081  | `identity_db`    |
| `course-service`      | 8082  | `course_db`      |
| `enrollment-service`  | 8083  | `enrollment_db`  |
| `progress-service`    | 8084  | `progress_db`    |
| `notification-service`| 8085  | `notification_db`|
| `gateway-service`     | 8080  | —                |

Each service uses a multi-stage Dockerfile (`eclipse-temurin:21-jdk` for build, `eclipse-temurin:21-jre` for runtime).

---

## 7. Services

---

### 7.1 Identity Service

**Port:** `8081`  
**Database:** `identity_db`  
**Role:** Owns user registration, login, and JWT issuance. All other services consume its JWTs but never access its database.

#### Database Schema

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'STUDENT',
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);
```

**Flyway migration:** `V1__create_users_table.sql`

#### Entities & Repositories

- `User` — JPA entity matching the schema above
- `UserRepository` — extends `JpaRepository<User, UUID>`

#### DTOs

| DTO | Fields |
|-----|--------|
| `RegisterRequest` | `fullName`, `email`, `password`, `role` (optional, default `STUDENT`) |
| `LoginRequest` | `email`, `password` |
| `AuthResponse` | `token`, `userId`, `email`, `role` |
| `UserProfileResponse` | `userId`, `fullName`, `email`, `role`, `createdAt` |

#### Service — `AuthService`

| Method | Behaviour |
|--------|-----------|
| `register(RegisterRequest)` | Validates email uniqueness (`409` if exists), hashes password with BCrypt, saves user, issues JWT, returns `AuthResponse` |
| `login(LoginRequest)` | Finds user by email (`401` if missing), verifies password (`401` if wrong), issues JWT, returns `AuthResponse` |

#### REST Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/api/auth/register` | None | Register a new user |
| `POST` | `/api/auth/login` | None | Login and receive JWT |
| `GET` | `/api/users/me` | Bearer Token | Return authenticated user's profile |

#### Security Config

- Permit all: `/api/auth/**`, `/actuator/health`
- Require authentication: all other endpoints
- Register `JwtAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`
- CSRF disabled, sessions STATELESS

#### Error Handling — `GlobalExceptionHandler`

`ErrorResponse` shape: `timestamp`, `status`, `error`, `message`, `path`

| Exception | HTTP Status |
|-----------|-------------|
| `MethodArgumentNotValidException` | `400` |
| `ResourceNotFoundException` | `404` |
| `DuplicateResourceException` | `409` |
| `AuthenticationException` | `401` |
| `Exception` (generic) | `500` |

---

### 7.2 Course Service

**Port:** `8082`  
**Database:** `course_db`  
**Role:** Owns the course catalog. Source of truth for courses, modules, and lessons. No other service stores or duplicates this data.

#### Database Schema

```sql
CREATE TABLE courses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    instructor_id UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    level VARCHAR(20) NOT NULL DEFAULT 'BEGINNER',
    category VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE modules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    position INT NOT NULL
);

CREATE TABLE lessons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    module_id UUID NOT NULL REFERENCES modules(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    content_type VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    content_url_or_body TEXT,
    position INT NOT NULL,
    is_preview BOOLEAN NOT NULL DEFAULT false
);
```

**Flyway migration:** `V1__create_course_tables.sql`

#### Entities, Enums & Repositories

- `Course` — `@OneToMany` to `Module`
- `Module` — `@OneToMany` to `Lesson`
- `Lesson`
- Enums: `CourseStatus` (`DRAFT`, `PUBLISHED`, `ARCHIVED`), `ContentType` (`TEXT`, `VIDEO`, `LINK`), `CourseLevel` (`BEGINNER`, `INTERMEDIATE`, `ADVANCED`)
- Repositories: `CourseRepository`, `ModuleRepository`, `LessonRepository`

> `instructor_id` is stored as a UUID with no FK to the `users` table. Instructor identity is validated via JWT claims only.

#### DTOs

| DTO | Fields |
|-----|--------|
| `CreateCourseRequest` | `title`, `description`, `level`, `category` |
| `UpdateCourseRequest` | `title`, `description`, `level`, `category` |
| `CourseResponse` | `id`, `instructorId`, `title`, `description`, `level`, `category`, `status`, `createdAt` |
| `CourseDetailResponse` | All `CourseResponse` fields + list of `ModuleResponse` |
| `CreateModuleRequest` | `title`, `position` |
| `ModuleResponse` | `id`, `courseId`, `title`, `position`, list of `LessonResponse` |
| `CreateLessonRequest` | `title`, `contentType`, `contentUrlOrBody`, `position`, `isPreview` |
| `LessonResponse` | `id`, `moduleId`, `title`, `contentType`, `contentUrlOrBody`, `position`, `isPreview` |

#### Service Methods

**`CourseService`:**

| Method | Behaviour |
|--------|-----------|
| `createCourse(userId, request)` | Sets `instructorId` from JWT, status = `DRAFT` |
| `updateCourse(userId, courseId, request)` | Verifies ownership (`403` if mismatch), updates fields |
| `publishCourse(userId, courseId)` | Verifies ownership, sets status to `PUBLISHED` |
| `unpublishCourse(userId, courseId)` | Verifies ownership, sets status to `DRAFT` |
| `getPublishedCourses()` | Returns all courses where `status = PUBLISHED` |
| `getCourseDetail(courseId)` | Returns course with full modules and lessons |
| `getCourseById(courseId)` | Returns course (used by internal calls) |
| `getLessonCountByCourseId(courseId)` | Count of all lessons across all modules |

#### REST Endpoints — Public/Authenticated

| Method | Path | Auth | Role | Description |
|--------|------|------|------|-------------|
| `POST` | `/api/courses` | Bearer | INSTRUCTOR | Create a course |
| `PUT` | `/api/courses/{courseId}` | Bearer | INSTRUCTOR + owner | Update a course |
| `PATCH` | `/api/courses/{courseId}/publish` | Bearer | INSTRUCTOR + owner | Publish a course |
| `PATCH` | `/api/courses/{courseId}/unpublish` | Bearer | INSTRUCTOR + owner | Unpublish a course |
| `GET` | `/api/courses` | None | — | List all published courses |
| `GET` | `/api/courses/{courseId}` | Optional | — | Get course detail (owner can see drafts) |
| `POST` | `/api/courses/{courseId}/modules` | Bearer | INSTRUCTOR + owner | Add a module |
| `GET` | `/api/courses/{courseId}/modules` | None | — | List modules with lessons |
| `POST` | `/api/modules/{moduleId}/lessons` | Bearer | INSTRUCTOR + owner | Add a lesson |

#### REST Endpoints — Internal (service-to-service only, not routed through Gateway)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/internal/courses/{courseId}/exists` | Returns `{ "exists": true/false, "status": "PUBLISHED" }` |
| `GET` | `/api/internal/courses/{courseId}/lesson-count` | Returns `{ "lessonCount": 12 }` |
| `GET` | `/api/internal/lessons/{lessonId}/exists` | Returns `{ "exists": true/false, "courseId": "..." }` |

---

### 7.3 Enrollment Service

**Port:** `8083`  
**Database:** `enrollment_db`  
**Role:** Manages student enrollments. Calls Course Service to validate that a course exists and is published before allowing enrollment.

#### Database Schema

```sql
CREATE TABLE enrollments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL,
    course_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ENROLLED',
    enrolled_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(student_id, course_id)
);
```

**Flyway migration:** `V1__create_enrollments_table.sql`

#### Entities, Enums & Repositories

- `Enrollment` — JPA entity
- `EnrollmentStatus` — `ENROLLED`, `CANCELLED`, `COMPLETED`
- `EnrollmentRepository`

#### Service Clients

**`CourseServiceClient`** (calls `http://course-service:8082`):

| Method | Calls | Behaviour on error |
|--------|-------|--------------------|
| `getCourseExists(courseId, jwtToken)` | `GET /api/internal/courses/{courseId}/exists` | `404` from Course → domain exception; unreachable → `ServiceUnavailableException` |
| `getLessonCount(courseId, jwtToken)` | `GET /api/internal/courses/{courseId}/lesson-count` | Same error handling |

The JWT token is always forwarded in the `Authorization` header.

**`NotificationServiceClient`** (calls `http://notification-service:8085`):

| Method | Calls |
|--------|-------|
| `sendEnrollmentNotification(userId, courseId, token)` | `POST /api/notifications` |

Call is wrapped in try-catch. Failure logs a warning but does **not** fail the enrollment.

#### DTOs

| DTO | Fields |
|-----|--------|
| `EnrollRequest` | `courseId` |
| `EnrollmentResponse` | `id`, `studentId`, `courseId`, `status`, `enrolledAt` |

#### Service Methods — `EnrollmentService`

| Method | Behaviour |
|--------|-----------|
| `enroll(userId, request)` | Validates course exists + is published (`400` if not), checks for duplicate (`409`), saves enrollment, fires notification (non-critical) |
| `getMyEnrollments(userId)` | Returns all enrollments where `studentId = userId` |
| `getEnrollmentsForCourse(courseId, userId)` | Verifies caller is the course instructor, returns enrolled student list |
| `isEnrolled(studentId, courseId)` | Returns boolean |

#### REST Endpoints

| Method | Path | Auth | Role | Description |
|--------|------|------|------|-------------|
| `POST` | `/api/enrollments` | Bearer | STUDENT | Enroll in a course |
| `GET` | `/api/enrollments/me` | Bearer | Any | Get current user's enrollments |
| `GET` | `/api/courses/{courseId}/enrollments` | Bearer | INSTRUCTOR | Get all students enrolled in a course |
| `GET` | `/api/enrollments/check?studentId={}&courseId={}` | Bearer | Any | Check enrollment status (used internally by Progress Service) |

---

### 7.4 Progress Service

**Port:** `8084`  
**Database:** `progress_db`  
**Role:** Tracks lesson-level completion per student. Calls both Course Service (to validate lesson existence and get total lesson count) and Enrollment Service (to verify enrollment before allowing progress tracking).

#### Database Schema

```sql
CREATE TABLE lesson_progress (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL,
    course_id UUID NOT NULL,
    lesson_id UUID NOT NULL,
    completed_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(student_id, lesson_id)
);
```

**Flyway migration:** `V1__create_lesson_progress_table.sql`

#### Entities & Repositories

- `LessonProgress` — JPA entity
- `LessonProgressRepository`

#### Service Clients

**`CourseServiceClient`** (calls `http://course-service:8082`):

| Method | Calls |
|--------|-------|
| `getLessonExists(lessonId, token)` | `GET /api/internal/lessons/{lessonId}/exists` |
| `getLessonCount(courseId, token)` | `GET /api/internal/courses/{courseId}/lesson-count` |

**`EnrollmentServiceClient`** (calls `http://enrollment-service:8083`):

| Method | Calls |
|--------|-------|
| `isEnrolled(studentId, courseId, token)` | `GET /api/enrollments/check?studentId={}&courseId={}` |

#### DTOs

| DTO | Fields |
|-----|--------|
| `MarkLessonCompleteRequest` | `courseId` |
| `LessonProgressResponse` | `lessonId`, `completedAt` |
| `CourseProgressResponse` | `courseId`, `completedLessons`, `totalLessons`, `percentage` |

#### Service Methods — `ProgressService`

| Method | Behaviour |
|--------|-----------|
| `markLessonComplete(userId, lessonId, courseId, token)` | Verifies enrollment (`403` if not enrolled), verifies lesson exists (`404` if not), idempotent save (returns existing if already completed) |
| `getCourseProgress(userId, courseId, token)` | Counts completed lessons, fetches total from Course Service, calculates percentage |
| `getMyDashboard(userId)` | Aggregates progress across all enrolled courses |

#### REST Endpoints

| Method | Path | Auth | Role | Description |
|--------|------|------|------|-------------|
| `POST` | `/api/progress/lessons/{lessonId}/complete` | Bearer | STUDENT | Mark a lesson as complete |
| `GET` | `/api/progress/courses/{courseId}` | Bearer | Any | Get progress for a course |
| `GET` | `/api/progress/me` | Bearer | Any | Get dashboard progress across all courses |

---

### 7.5 Notification Service

**Port:** `8085`  
**Database:** `notification_db`  
**Role:** Creates and stores in-app notifications. Called by Enrollment Service in a fire-and-forget manner. Its failure must not affect any calling service's primary operation.

#### Database Schema

```sql
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT,
    is_read BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
```

**Flyway migration:** `V1__create_notifications_table.sql`

#### Entities & Repositories

- `Notification` — JPA entity
- `NotificationRepository`

#### DTOs

| DTO | Fields |
|-----|--------|
| `CreateNotificationRequest` | `userId`, `type`, `title`, `message` |
| `NotificationResponse` | `id`, `type`, `title`, `message`, `isRead`, `createdAt` |

#### Service Methods — `NotificationService`

| Method | Behaviour |
|--------|-----------|
| `createNotification(request)` | Saves and returns the notification |
| `getMyNotifications(userId)` | Returns notifications for `userId`, ordered by `createdAt DESC` |
| `markAsRead(notificationId, userId)` | Verifies ownership, sets `isRead = true` |

#### REST Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/api/notifications` | Bearer | Create a notification (called by other services) |
| `GET` | `/api/notifications/me` | Bearer | Get authenticated user's notifications |
| `PATCH` | `/api/notifications/{id}/read` | Bearer | Mark a notification as read |

---

### 7.6 API Gateway Service

**Port:** `8080`  
**Framework:** Spring Cloud Gateway (WebFlux — no `spring-boot-starter-web`)  
**Role:** Single public entry point for all client requests. Routes requests to the appropriate downstream service. Internal (`/api/internal/**`) endpoints are never routed through the Gateway.

#### Route Definitions (`application.yml`)

| Route ID | URI | Path Predicate |
|----------|-----|----------------|
| `identity-service` | `http://identity-service:8081` | `/api/auth/**`, `/api/users/**` |
| `course-service` | `http://course-service:8082` | `/api/courses/**`, `/api/modules/**` |
| `enrollment-service` | `http://enrollment-service:8083` | `/api/enrollments/**` |
| `progress-service` | `http://progress-service:8084` | `/api/progress/**` |
| `notification-service` | `http://notification-service:8085` | `/api/notifications/**` |

#### Correlation ID Global Filter

- Reads `X-Correlation-Id` from the incoming request header
- If absent, generates a UUID
- Propagates it to all downstream request headers
- Adds it to the response headers

#### Request/Response Logging Filter

- Logs: HTTP method, path, response status code, duration (ms), correlation ID
- Log level: `INFO`

---

## 8. Cross-Cutting Concerns

### 8.1 Authentication & Authorization

- All services include `jwt-common` as a dependency
- Every protected endpoint validates the `Authorization: Bearer <token>` header via `JwtAuthenticationFilter`
- Services extract `userId`, `email`, `role` from the token via `AuthenticatedUser`
- Role-based access enforced at the controller level (`STUDENT`, `INSTRUCTOR`)
- When making service-to-service calls, the original JWT is always forwarded in the `Authorization` header

### 8.2 Structured Logging

- Each service has a `logback-spring.xml`
- Log format: `[timestamp] [service-name] [correlation-id] [level] [logger] — message`
- A `CorrelationIdFilter` in each non-Gateway service reads `X-Correlation-Id` from the incoming header and places it in `MDC` for the duration of the request

### 8.3 Health Checks

- All services expose `/actuator/health`
- Docker Compose health check per service:
  ```yaml
  healthcheck:
    test: ["CMD", "curl", "-f", "http://localhost:<PORT>/actuator/health"]
    interval: 30s
    timeout: 5s
    retries: 3
  ```

### 8.4 Request Timeouts (RestClient)

- All `*ServiceClient` classes configure:
  - Connection timeout: 5 seconds
  - Read timeout: 5 seconds

### 8.5 Inter-Service Error Mapping

| Downstream Response | Translated Exception |
|---------------------|----------------------|
| Connection refused | `503 Service Unavailable` |
| Timeout | `504 Gateway Timeout` |
| `404` | Domain-specific `ResourceNotFoundException` |
| `4xx` | Log and translate to meaningful domain error |
| `5xx` | `502 Bad Gateway` |

### 8.6 Global Error Response Shape

Every service returns errors in the same `ErrorResponse` format:

```json
{
  "timestamp": "2026-03-15T01:36:39Z",
  "status": 404,
  "error": "Not Found",
  "message": "Course with id <id> not found",
  "path": "/api/courses/<id>"
}
```

---

## 9. Data Design Conventions

- **Primary keys:** All entities use `UUID` (PostgreSQL `gen_random_uuid()`).
- **No cross-service foreign keys:** Services reference entities in other services by storing only the UUID. No actual database-level FK constraints cross service boundaries.
- **Timestamps:** All tables include `created_at`. Tables with mutable records also include `updated_at`.
- **Enum storage:** Stored as `VARCHAR` in PostgreSQL, mapped to Java enum types in JPA.
- **Idempotency:** `markLessonComplete` is idempotent — if the record already exists, the existing record is returned rather than creating a duplicate or throwing an error.

---

## 10. Service Dependency Map

```
Client
  └── Gateway (8080)
        ├── Identity Service (8081)          [no upstream service deps]
        ├── Course Service (8082)            [no upstream service deps]
        ├── Enrollment Service (8083)   ───► Course Service
        │                               ───► Notification Service (fire-and-forget)
        ├── Progress Service (8084)     ───► Course Service
        │                               ───► Enrollment Service
        └── Notification Service (8085)      [no upstream service deps]
```

---

## 11. Project Phases Summary

| Phase | Deliverable | Core Concept |
|-------|-------------|--------------|
| 1 | Root structure, Docker Compose PostgreSQL, `jwt-common` | Database-per-service, shared auth module |
| 2 | Identity Service | Auth ownership, JWT issuance, secure endpoint config |
| 3 | Course Service | Bounded contexts, internal vs external APIs |
| 4 | Enrollment Service | Cross-service REST calls, JWT forwarding, `503` handling |
| 5 | Progress Service + Notification Service | Critical vs non-critical dependencies, fire-and-forget |
| 6 | API Gateway | Single public entry point, internal endpoint protection |
| 7 | Logging, health checks, timeouts, Postman collection | Observability and resilience |

---

## 12. Postman Collection Structure

**Collection name:** `LearnSphere`  
**File:** `postman/LearnSphere.postman_collection.json`  
**Environment file:** `postman/local.postman_environment.json`

**Folders:**
- `Auth` — register, login
- `Courses` — CRUD, publish/unpublish
- `Enrollment` — enroll, view enrollments, check enrollment
- `Progress` — mark complete, get course progress, dashboard
- `Notifications` — list, mark as read

**Environment variables:**
- `gateway_url` → `http://localhost:8080`
- `student_token` — auto-set by pre-request script after login
- `instructor_token` — auto-set by pre-request script after login

**Newman CLI:**
```bash
npx newman run postman/LearnSphere.postman_collection.json \
  --environment postman/local.postman_environment.json
```

---

## 13. Full Docker Compose Stack Commands

```bash
# Start everything
docker compose up -d --build

# Verify all services healthy
docker compose ps

# View logs for a specific service
docker compose logs --tail=20 identity-service
docker compose logs --tail=20 gateway-service

# Failure scenario: stop a service to test 503 handling
docker compose stop course-service
docker compose start course-service

# Failure scenario: stop notification service (enrollment should still succeed)
docker compose stop notification-service
docker compose start notification-service

# Full reset (removes volumes and databases)
docker compose down -v
```
