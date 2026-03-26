# Internal API Alignment Notes

## Why this change was needed

The Enrollment Service was using two different styles of communication with the Course Service:

- Internal API calls for machine-oriented checks such as:
  - `GET /api/internal/courses/{courseId}/exists`
  - `GET /api/internal/courses/{courseId}/lesson-count`
- A public API call for ownership lookup:
  - `GET /api/courses/{courseId}`

That public call was a design mismatch.

The public course detail endpoint is meant for user-facing reads. It returns a richer response and applies public visibility rules. The enrollment flow only needed a small internal fact: `instructorId`.

For a learning project, the simplest good fix is:

- keep public APIs for clients
- keep internal APIs for service-to-service facts
- do not make one service depend on another service's public response shape when a smaller internal contract is enough

## What was changed

### 1. Added a dedicated internal summary endpoint in Course Service

New endpoint:

```http
GET /api/internal/courses/{courseId}/summary
```

Response shape:

```json
{
  "exists": true,
  "status": "PUBLISHED",
  "instructorId": "uuid"
}
```

This was added so other services can ask for a minimal internal summary without depending on the public course detail endpoint.

### 2. Enrollment Service now uses only internal Course Service endpoints

The Enrollment Service no longer calls:

```http
GET /api/courses/{courseId}
```

Instead, it now calls:

```http
GET /api/internal/courses/{courseId}/summary
```

This internal summary is used for:

- checking whether a course exists
- checking whether the course is published
- checking whether the current instructor owns the course

### 3. Removed the internal dependency on the public course detail DTO

The old internal DTO `CourseInstructorResponse` existed only because the Enrollment Service was reading a public endpoint and extracting one field from it.

That DTO is no longer needed and was removed.

## Why this is better

### Better boundary

The Course Service now exposes a small internal contract for small internal needs.

That means:

- public API shape can evolve separately
- enrollment logic does not depend on course detail payload shape
- internal calls are easier to reason about

### Smaller payload

The old public course detail endpoint contains more data than the enrollment flow needs.

For example, course detail includes module data. An ownership check should not require fetching a user-facing course detail payload.

### Easier learning model

For a beginner-friendly microservice project, this creates a clear rule:

- public endpoints are for external/client use
- internal endpoints are for service-to-service fact lookups

This keeps the project understandable without introducing advanced infrastructure.

## Internal calls that should be allowed in this project

These are good internal calls because they fetch facts from the service that owns them:

- `Enrollment -> Course`
  - course summary
  - lesson count
- `Progress -> Course`
  - lesson existence
  - lesson count
  - course summary if needed
- `Progress -> Enrollment`
  - enrollment check

These calls are acceptable because the caller needs data it does not own and the callee is the source of truth.

## Internal calls that should be avoided

### Avoid public endpoints for service-to-service logic

Do not use public endpoints like:

```http
GET /api/courses/{courseId}
```

for internal authorization or validation when a smaller internal endpoint can answer the question.

### Avoid large payload lookups for tiny checks

Do not fetch full course detail, module lists, or lesson content just to answer:

- does this course exist?
- who owns this course?
- how many lessons are in this course?

### Avoid sharing repositories or databases

Each service should keep owning its own data. Other services should call internal APIs instead of reading another service's database or repository.

### Avoid extra auth complexity for now

In a production-grade system, `/api/internal/**` would usually use stronger service-to-service authentication such as:

- service JWTs
- mTLS
- internal network policy

That was intentionally **not** added here because this project is for learning and the current goal was to fix the API boundary with minimal complexity.

## What was intentionally not changed

### 1. Existing `/exists` endpoint was kept

The project already had:

```http
GET /api/internal/courses/{courseId}/exists
```

It was left in place to avoid unnecessary churn. The new `/summary` endpoint complements it and is more useful for ownership-related internal checks.

### 2. Security model was not made more advanced

The current project already forwards the user JWT for internal calls. That is enough for now to keep the learning flow simple.

If the project grows later, the next upgrade would be:

- separate user authentication from service authentication
- protect `/api/internal/**` with service-level credentials

But that would add complexity that is not necessary for the current learning stage.

## Practical rule to follow from now on

Before adding a new internal call, ask:

1. Does the caller need a fact only the other service owns?
2. Can that fact be returned in a very small internal DTO?
3. Is this clearly an internal API and not a reused public endpoint?

If the answer to all three is yes, the internal call is probably appropriate.

If the caller needs a full user-facing payload just to extract one field, that is a sign the boundary is wrong and a smaller internal endpoint should be created instead.
