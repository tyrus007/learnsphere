# AuthService.java - Code Explanation

This document provides a block-by-block explanation of the `AuthService.java` file in the Identity Service.

## Package and Imports
```java
package com.ashique.identity_service.service;

import com.ashique.identity_service.dto.AuthResponse;
import com.ashique.identity_service.dto.LoginRequest;
import com.ashique.identity_service.dto.RegisterRequest;
import com.ashique.identity_service.entity.User;
import com.ashique.identity_service.exception.DuplicateResourceException;
import com.ashique.identity_service.exception.ResourceNotFoundException;
import com.ashique.identity_service.repository.UserRepository;
import com.ashique.learnsphere.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
```
*   **Package Declaration**: Declares that this class is part of the `service` layer within the `identity_service`.
*   **Imports Needed**:
    *   **DTOs**: `AuthResponse`, `LoginRequest`, `RegisterRequest` are used to structure data sent to and received from clients.
    *   **Entities**: `User` represents the structure of the user in the database.
    *   **Exceptions**: `DuplicateResourceException` and `ResourceNotFoundException` (custom exceptions) and `BadCredentialsException` (Spring Security) help manage edge cases like trying to create an existing user or failing a login attempt.
    *   **Repository Component**: `UserRepository` interacts with the database for user persistence.
    *   **JWT Component**: `JwtUtil` handles parsing, validating, and generating JSON Web Tokens.
    *   **Lombok**: `RequiredArgsConstructor` lets Lombok generate a constructor automatically for dependency injection.
    *   **Spring Security & Framework**: Annotations and classes from Spring to register the class as a Bean (`@Service`) and hash passwords.

## Class Declaration and Dependencies
```java
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
```
*   **`@Service`**: Marks this class as a Spring Service component. This allows Spring's Inversion of Control (IoC) container to automatically detect it during component scanning, instantiate it, and manage its lifecycle. Service classes typically hold the core business logic.
*   **`@RequiredArgsConstructor`**: A Lombok annotation that automatically generates a constructor with arguments for all `final` fields. This promotes **Constructor Injection** for the dependencies, which is a best practice in Spring.
*   **Dependencies**: 
    *   `UserRepository`: Provides CRUD operations for the `User` entity, communicating with the database.
    *   `PasswordEncoder`: Responsible for securely hashing passwords before saving and verifying passwords during login.
    *   `JwtUtil`: A utility class (from a shared `jwt-common` module) that creates JSON Web Tokens for authenticated users.

## User Registration Flow
```java
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email is already taken");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();

        user = userRepository.save(user);

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
```
*   **Email Uniqueness Check**: The `register` method first checks if a user with the given email already exists using the repository. If so, it halts the operation and throws a `DuplicateResourceException`.
*   **User Object Construction**: Utilizing the Builder pattern (typically provided by Lombok's `@Builder` on the `User` entity), a new user instance is constructed based on the data sent inside `RegisterRequest`.
*   **Password Hashing**: Before constructing the `User`, the plaintext password is encrypted using `passwordEncoder.encode(...)`. This ensures that even if the database is compromised, passwords remain safe.
*   **Persistence**: Once the user object is valid and safe, `userRepository.save(user)` is called to write it to the database. The returned object contains any database-generated values like an auto-incrementing ID.
*   **Token Generation**: After a successful creation, `jwtUtil.generateToken(...)` creates a secure token containing standard user claims: ID, email, and role. 
*   **Response Mapping**: Finally, the method wraps the created token and user information in an `AuthResponse` object to return to the caller (e.g., the web controller).

## User Login Flow
```java
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid password");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
```
*   **User Lookup**: The method attempts to find a user by their provided email using `findByEmail(...)`. This method typically returns an `Optional<User>`. If the user is missing, it immediately throws a `ResourceNotFoundException`.
*   **Password Verification**: Once a user is verified to exist, the incoming plaintext password from `LoginRequest` is compared against the stored, hashed version from the database using `passwordEncoder.matches(...)`.
*   **Invalid Credentials Handling**: If the entered password evaluates as incorrect against the hash, a Spring Security `BadCredentialsException` is thrown, blocking entry to the system.
*   **Token Generation & Return**: Following a successful verification, the login behaves identically to the end of the registration process: a new JWT is issued via `JwtUtil` and mapped to an `AuthResponse` containing the user details, effectively authenticating the user for ongoing session requests.
