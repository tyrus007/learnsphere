# `CourseServiceClient.java` - Code Explanation


## Component Declaration and Dependencies

```java
@Component
public class CourseServiceClient {

    private static final String COURSE_EXISTS_URI = "/api/internal/courses/{courseId}/exists";

    private static final String COURSE_LESSON_COUNT_URI = "/api/internal/courses/{courseId}/lesson-count";

    private final RestClient restClient;
```

* `@Component` registers the class as a Spring bean so it can be injected wherever needed.
* `COURSE_EXISTS_URI` and `COURSE_LESSON_COUNT_URI` hold the endpoint paths for the two Course Service calls.
* `restClient` is the shared HTTP client used by all methods in this class.

Keeping the URI paths as constants is useful because:

- it avoids repeating string literals
- it reduces typo risk
- it makes endpoint changes easier to manage

## Constructor and Base URL Setup

```java
public CourseServiceClient(
        RestClient.Builder restClientBuilder,
        @Value("${course-service.url}") String courseServiceUrl) {
    this.restClient = restClientBuilder
            .baseUrl(courseServiceUrl)
            .build();
}
```

This constructor prepares the `RestClient` instance once when Spring creates the bean.

### Why `RestClient.Builder` is injected

Spring can provide a preconfigured `RestClient.Builder`, which is a convenient starting point for building outbound HTTP clients. If the application defines shared settings for the builder, this client can inherit them automatically.

### Why `@Value("${course-service.url}")` is used

The Course Service host is not hardcoded. Instead, it is read from configuration so the service can point to different environments:

- local development
- staging
- production

That makes the client more flexible and easier to deploy.

### Why `baseUrl(...)` is used

Setting the base URL once means the rest of the code only needs to supply relative paths like:

`/api/internal/courses/{courseId}/exists`

This keeps each request smaller and avoids repeating the full domain name in multiple places.

## Public API Methods

```java
public InternalCourseExistsResponse getCourseExists(UUID courseId, String authorizationHeader) {
    return executeGet(
            COURSE_EXISTS_URI,
            courseId,
            authorizationHeader,
            InternalCourseExistsResponse.class,
            "Course not found with id: " + courseId);
}

public InternalCourseLessonCountResponse getLessonCount(UUID courseId, String authorizationHeader) {
    return executeGet(
            COURSE_LESSON_COUNT_URI,
            courseId,
            authorizationHeader,
            InternalCourseLessonCountResponse.class,
            "Course not found with id: " + courseId);
}
```

These two methods are thin wrappers around the shared `executeGet(...)` helper.

### Why these methods exist separately

Each method represents a different business use case:

- `getCourseExists(...)` checks whether a course exists and returns its status.
- `getLessonCount(...)` fetches the number of lessons in a course.

The public methods keep the calling code readable while still reusing the same HTTP logic underneath.

### Why the response types are different

The returned DTO depends on the endpoint:

- `InternalCourseExistsResponse` contains fields such as `exists` and `status`
- `InternalCourseLessonCountResponse` contains a `lessonCount`

The client needs a different Java type for each JSON shape.

## Generic `executeGet(...)` Method

```java
private <T> T executeGet(
        String uri,
        UUID courseId,
        String authorizationHeader,
        Class<T> responseType,
        String notFoundMessage) {
```

This is the main reusable method in the class.

### Why it is generic

`<T>` means “this method works for any response type.”

That lets the same code handle multiple endpoints without duplicating:

- request creation
- header handling
- deserialization
- error translation

In this class, `T` becomes:

- `InternalCourseExistsResponse`
- `InternalCourseLessonCountResponse`

### Why `Class<T> responseType` is needed

Java generics are erased at runtime, so `RestClient` needs an actual class object to deserialize the JSON body.

Example:

```java
InternalCourseExistsResponse.class
```

This tells Spring exactly which DTO to build from the HTTP response body.

### Why `notFoundMessage` is passed in

The same helper method is used for multiple endpoints, but each caller can supply a domain-specific not-found message. That keeps the method reusable while still letting the final exception message stay meaningful.

## Request Execution

```java
try {
    T response = restClient.get()
            .uri(uri, courseId)
            .headers(headers -> applyAuthorizationHeader(headers, authorizationHeader))
            .retrieve()
            .body(responseType);
```

This is the actual HTTP call.

### `restClient.get()`

Starts an outbound GET request.

### `.uri(uri, courseId)`

Builds the final request URI by replacing `{courseId}` with the actual UUID.

Example:

`/api/internal/courses/{courseId}/exists` becomes `/api/internal/courses/123e4567-e89b-12d3-a456-426614174000/exists`

### `.headers(...)`

Adds request headers before the call is sent.

This class uses the authorization header from the incoming request so the Course Service receives the same identity context.

### `.retrieve()`

Executes the request and prepares the response for body extraction and error handling.

### `.body(responseType)`

Converts the JSON response body into the target DTO type.

## Null Response Check

```java
if (response == null) {
    throw new CourseServiceClientException(
            "Course Service returned an empty response for course id: " + courseId,
            null);
}
```

The method expects a valid object back from Course Service. If the body is empty or cannot be mapped properly, this check prevents `null` from silently leaking into the rest of the application.

This is a defensive safeguard because returning `null` from a client method often causes harder-to-debug failures later.

## Exception Mapping

```java
} catch (HttpClientErrorException.NotFound ex) {
    throw new ResourceNotFoundException(notFoundMessage, ex);
} catch (ResourceAccessException ex) {
    throw new ServiceUnavailableException("Course Service is unavailable", ex);
} catch (RestClientException ex) {
    throw new CourseServiceClientException("Course Service call failed for course id: " + courseId, ex);
}
```

The class converts low-level HTTP client exceptions into application-specific runtime exceptions.

### `HttpClientErrorException.NotFound`

This means the Course Service returned HTTP 404.

Why map it to `ResourceNotFoundException`:

- callers should think in terms of business meaning, not raw HTTP status codes
- a missing course is a domain problem, not just a transport problem
- the exception message can be tailored using `notFoundMessage`

### `ResourceAccessException`

This usually means the request could not reach Course Service.

Examples:

- connection refused
- timeout
- DNS/network failure

Why map it to `ServiceUnavailableException`:

- the course may exist, but the dependency was unreachable
- this is different from “course not found”
- the application can handle dependency outage separately

### `RestClientException`

This is the broader fallback for other Spring client failures.

Examples:

- serialization/deserialization issues
- unexpected HTTP client problems
- malformed responses

Why wrap it in `CourseServiceClientException`:

- it creates one consistent failure type for generic client errors
- it keeps the rest of the application from dealing with low-level Spring exceptions directly

## Authorization Header Helper

```java
private void applyAuthorizationHeader(HttpHeaders headers, String authorizationHeader) {
    if (StringUtils.hasText(authorizationHeader)) {
        headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
    }
}
```

This helper adds the `Authorization` header only when the input has actual text.

### Why `StringUtils.hasText(...)` is used

It returns `true` only when the string is not:

- `null`
- empty
- whitespace only

That prevents sending blank auth headers downstream.

### Why the header is set conditionally

Not every outbound call may have an auth token available. This method avoids adding invalid headers and keeps request construction safe.

## DTOs Returned by the Client

```java
public record InternalCourseExistsResponse(
        Boolean exists,
        CourseStatus status
) {
}
```

```java
public record InternalCourseLessonCountResponse(Long lessonCount) {
}
```

These records are small response models used only for deserializing Course Service responses.

They are good fits because they are:

- immutable
- simple
- focused on data transfer

## Overall Design Intent

This class is intentionally small and focused. It acts as an adapter between the Enrollment Service and Course Service.

The main design choices are:

- use one reusable HTTP helper method
- use generics to support multiple response types
- forward authorization to the downstream service
- translate transport errors into domain-friendly exceptions
- keep the base URL in configuration instead of hardcoding it

That makes the client easier to maintain and easier for the rest of the application to use.
