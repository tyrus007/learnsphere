package com.ashique.identity_service.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Test
    void duplicateResourceBuildsConflictResponseThatSerializes() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/register");

        GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler(request);

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleDuplicateResource(
                new DuplicateResourceException("Email is already taken"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().getStatus());
        assertEquals("Conflict", response.getBody().getError());
        assertEquals("Email is already taken", response.getBody().getMessage());
        assertEquals("/api/auth/register", response.getBody().getPath());

        String json = assertDoesNotThrow(() -> objectMapper.writeValueAsString(response.getBody()));
        assertTrue(json.contains("\"status\":409"));
        assertTrue(json.contains("\"error\":\"Conflict\""));
        assertTrue(json.contains("\"message\":\"Email is already taken\""));
    }
}
