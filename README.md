# LearnSphere
LearnSphere is a Spring Boot microservices backend that goes beyond CRUD, enforcing database-per-service isolation, stateless JWT auth, and role-based access rules across identity, course, and enrollment workflows. Every design decision reflects how these problems are solved in real systems, not just in tutorials.
## Architecture Snapshot

LearnSphere is structured as independently deployable services with clear ownership boundaries. Each service owns its own PostgreSQL schema and evolves it with Flyway migrations, which avoids cross-database joins and keeps service contracts explicit. Authentication is stateless: the Identity Service issues JWTs, and downstream services validate them through a shared `jwt-common` library rather than duplicating security code. The environment is containerized with Docker Compose, including PostgreSQL initialization, per-service configuration, and health-based startup dependencies.

## Implemented Services

| Service | What is implemented |
| --- | --- |
| Identity Service | User registration and login, BCrypt password hashing, JWT issuance, persisted user records, and authenticated profile lookup (`/api/users/me`). |
| Course Service | Course, module, and lesson domain management; instructor-only create/update flows; publish/unpublish workflow; public access to published catalog; owner-only access to draft content. |
| Enrollment Service | Student enrollment flow, duplicate-enrollment prevention, learner enrollment history, instructor access to course rosters, and validation against Course Service before writing enrollment data. |
| `jwt-common` | Shared JWT generation/validation, request filtering, and authenticated principal extraction reused by all implemented services. |

## Key Engineering Highlights

- Database-per-service isolation is enforced in both schema design and code. Course data, user data, and enrollment data live separately, with no cross-service table access or foreign keys into another service’s database.
- The security model is consistent across services: JWTs are issued once by Identity, then validated at request time by a reusable filter that populates a typed authenticated principal for downstream authorization logic.
- Role-based access is implemented at the API layer. `STUDENT` and `INSTRUCTOR` permissions are enforced with Spring Security and method-level authorization, rather than left to controller convention.
- Ownership rules are explicit in the Course Service. Instructors can only modify their own courses, and unpublished course content is restricted to the owning instructor.
- Enrollment demonstrates real synchronous inter-service communication. Before persisting an enrollment, the Enrollment Service calls the Course Service to confirm course existence, publication state, and instructor ownership where needed.
- JWT forwarding is implemented for service-to-service calls. The upstream authorization header is propagated so downstream services can authenticate and authorize internal requests without sharing databases or session state.
- Error handling is defensive rather than incidental. Validation failures, duplicate resources, forbidden actions, missing records, and downstream service failures are translated into controlled HTTP responses, including `503 Service Unavailable` and `502 Bad Gateway` style handling for Course Service call failures.
- The Docker setup reflects production-style thinking for a local microservices environment: containerized services, per-service configuration, database bootstrap, and health checks for service dependency ordering.

## Planned / Not Yet Implemented

- Progress Service for lesson completion and learning-state tracking
- Notification Service for non-critical side effects
- API Gateway as the single external entry point
- Observability improvements such as correlation IDs and structured logging
- Resilience patterns such as timeouts, retries, and circuit breakers

This project demonstrates practical backend engineering skills in service decomposition, security design, domain ownership, synchronous service collaboration, and production-oriented operational setup. It is intentionally built as a microservices system first, with the current implementation ending at the Enrollment Service boundary.
