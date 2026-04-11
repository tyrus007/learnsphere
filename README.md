# LearnSphere

LearnSphere is a backend-only learning platform I built to move beyond small CRUD projects and understand how a real system changes when it is split into services. It solves a simple product problem: instructors can create and publish courses, students can enroll and track progress, and each part of the system has a clear owner.

## What I Built

I built five Spring Boot services around one learning flow:

1. an **Identity Service** for registration, login, password hashing, and JWT-based authentication 
2. a **Course Service** for courses, modules, lessons, draft content, and publish/unpublish rules 
3. an **Enrollment Service** that checks course state before letting a student enroll 
4. a **Progress Service** that records lesson completion and calculates course progress 
5. a **Notification Service** for in-app notifications after important actions like enrollment

I also created a shared `jwt-common` module so authentication logic did not have to be copied into every service.

## Technical Decisions I Made

- I kept a separate PostgreSQL database for each service. That made the boundaries real and forced me to think about data ownership instead of relying on cross-service joins.
- I used stateless JWT authentication so each service could verify the same user without shared sessions.
- I added internal service endpoints for things like course validation, lesson checks, and lesson counts, because some workflows needed trusted cross-service communication.
- I treated notifications as a non-critical side effect. Enrollment should still succeed even if notification delivery fails.
- I used Flyway migrations and Docker Compose so schema changes and multi-service local runs stayed consistent.

## Depth of Work

This project taught me that microservices are not just "more APIs." The hard part was deciding what each service should own, how services should trust each other, and what should happen when one service is unavailable.

One example is progress tracking: before marking a lesson complete, the system checks whether the student is enrolled and whether the lesson belongs to the right course, then stores only the progress data in the Progress Service. Another example is course visibility: published courses are public, but draft content stays private to the instructor who owns it.

## Why I Used These Tools

| Tool | Why I used it |
| --- | --- |
| Java 21 + Spring Boot | It gave me a solid structure for building multiple services without spending my time on low-level setup. |
| Spring Security + JWT | I wanted to learn stateless auth in a multi-service system. |
| PostgreSQL | The data is relational, and it was a good fit for users, courses, enrollments, and progress records. |
| Flyway | It made database changes explicit and versioned for each service. |
| Spring `RestClient` | It was a straightforward way to handle synchronous service-to-service calls. |
| Docker Compose | It made the whole backend easier to run and think about as one system. |
| Postman | I used it to test complete flows across services, not just single endpoints. |

## What I Learned

The most interesting part was seeing how quickly a "simple" backend becomes a design problem once services are separated. I learned a lot about service boundaries, authorization rules, failure handling, and how much more deliberate backend work becomes when each service has one job and one database.

## Future Work
- Deploy it in cloud
- create a simple frontend for this backend 
