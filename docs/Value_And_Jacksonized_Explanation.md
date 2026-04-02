# `@Value` and `@Jacksonized` in Lombok

This note explains what `@Value` and `@Jacksonized` do, when they work well together, and how they relate to constructors such as `@AllArgsConstructor` and `@NoArgsConstructor`.

## 1. What `@Value` does

`@Value` is Lombok's immutable data-class annotation.

When you put `@Value` on a class, Lombok typically generates:

- `private final` fields
- getters for all fields
- `equals()` and `hashCode()`
- `toString()`
- an all-arguments constructor
- a class designed to be immutable

Example:

```java
import lombok.Value;

@Value
public class AuthResponse {
    String token;
    String email;
}
```

This behaves roughly like:

```java
public final class AuthResponse {
    private final String token;
    private final String email;

    public AuthResponse(String token, String email) {
        this.token = token;
        this.email = email;
    }

    public String getToken() { return token; }
    public String getEmail() { return email; }
}
```

## 2. Why `@Value` is good for DTOs

`@Value` fits DTOs well because DTOs are usually:

- simple data containers
- not supposed to change after creation
- passed between controller, service, and client layers

This is why `@Value` works well for request and response DTOs such as:

- `AuthResponse`
- `LoginRequest`
- `RegisterRequest`

The main benefit is predictable immutable state.

## 3. Why `@Value` is usually bad for JPA entities

`@Value` is usually a poor fit for JPA/Hibernate entities such as `Course`, `Module`, and `Lesson`.

Reasons:

- JPA usually expects a no-args constructor.
- Hibernate often needs to mutate fields after object creation.
- generated IDs are assigned after construction
- lazy-loaded relationships are managed by Hibernate
- lifecycle callbacks may update fields such as timestamps
- collections in entity associations are mutable by design

If fields become `final`, that conflicts with how JPA manages entities.

So:

- use `@Value` for DTOs
- avoid `@Value` for JPA entities

## 4. What `@Builder` adds

`@Builder` gives a fluent builder API for creating objects.

Example:

```java
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LoginRequest {
    String email;
    String password;
}
```

Usage:

```java
LoginRequest request = LoginRequest.builder()
    .email("user@example.com")
    .password("secret")
    .build();
```

This is especially useful with immutable classes because there are no setters.

## 5. What `@Jacksonized` does

`@Jacksonized` lets Jackson deserialize JSON through the Lombok builder.

Normally, Jackson likes one of these patterns:

- a no-args constructor plus setters
- a constructor annotated for JSON creation

That works for mutable POJOs, but not naturally for immutable `@Value` classes.

When you combine:

- `@Value`
- `@Builder`
- `@Jacksonized`

Jackson can read JSON and build the object through the generated builder.

Example:

```java
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class RegisterRequest {
    String fullName;
    String email;
    String password;
}
```

JSON like:

```json
{
  "fullName": "Ashique",
  "email": "ashique@example.com",
  "password": "secret123"
}
```

can be deserialized by Jackson using the builder instead of requiring:

- setters
- mutable fields
- a no-args constructor

## 6. Why `@Jacksonized` matters for request DTOs

In Spring, request DTOs are often used like this:

```java
public ResponseEntity<?> login(@RequestBody LoginRequest request) {
    ...
}
```

Spring uses Jackson to convert the incoming JSON body into `LoginRequest`.

If `LoginRequest` is immutable and built with Lombok, `@Jacksonized` helps Jackson understand how to create it correctly.

This makes `@Jacksonized` especially useful for:

- request DTOs
- immutable API models
- builder-based DTOs

## 7. Constructor interactions

### `@Value` and `@AllArgsConstructor`

These do not strongly conflict, but they overlap.

Why:

- `@Value` already generates an all-args constructor
- adding `@AllArgsConstructor` is usually redundant

So this is usually unnecessary:

```java
@Value
@AllArgsConstructor
public class AuthResponse { ... }
```

In most DTOs, `@Value` already covers that constructor need.

### `@Value` and `@NoArgsConstructor`

These do conflict conceptually and often technically.

Why:

- `@Value` makes fields `final`
- final fields must be initialized
- `@NoArgsConstructor` creates an object with no arguments

A no-args constructor cannot properly initialize all final fields in the normal immutable model.

So this is usually a bad combination:

```java
@Value
@NoArgsConstructor
public class LoginRequest { ... }
```

The contradiction is simple:

- `@Value` says "all state is fixed at construction time"
- `@NoArgsConstructor` says "create the object without providing state"

Those two intentions do not match.

### `@NoArgsConstructor` and mutable classes

This is fine when the class is mutable.

Example:

```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private String email;
}
```

This works because:

- fields are not `final`
- the object can be created empty
- values can be assigned later

## 8. Common combinations and whether they make sense

### Good for immutable DTOs

```java
@Value
@Builder
@Jacksonized
public class LoginRequest { ... }
```

Good when:

- the class is a DTO
- JSON needs to be deserialized into it
- immutability is desired

### Good for mutable DTOs

```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse { ... }
```

Good when:

- mutability is acceptable
- a framework may prefer a default constructor
- you do not need immutable semantics

### Bad for JPA entities

```java
@Entity
@Value
public class Course { ... }
```

Avoid this because JPA/Hibernate typically need:

- a no-args constructor
- mutable fields
- non-final entity state

## 9. Practical rules

Use `@Value` when:

- the class is a DTO
- the object should be immutable
- you do not need setters

Use `@Jacksonized` when:

- the class also uses `@Builder`
- Jackson needs to deserialize JSON into that class
- the class is immutable and should not rely on a no-args constructor

Avoid `@Value` when:

- the class is a JPA entity
- the framework needs to mutate fields
- you need a normal no-args constructor with later field assignment

## 10. Short summary

- `@Value` makes a class immutable and already provides an all-args constructor.
- `@AllArgsConstructor` with `@Value` is usually redundant.
- `@NoArgsConstructor` conflicts with `@Value` because immutable final fields need initialization.
- `@Builder` is a natural partner for `@Value`.
- `@Jacksonized` lets Jackson deserialize JSON through the Lombok builder.
- `@Value + @Builder + @Jacksonized` is a strong pattern for Spring request DTOs.
- `@Value` should generally not be used for JPA entities.
