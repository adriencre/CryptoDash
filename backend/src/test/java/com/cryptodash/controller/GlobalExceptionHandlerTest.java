package com.cryptodash.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleBadRequest_shouldReturn400() {
        ResponseEntity<Map<String, String>> response = handler.handleBadRequest(new IllegalArgumentException("Error"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Error", response.getBody().get("message"));
    }

    @Test
    void handleStateError_shouldReturn400() {
        ResponseEntity<Map<String, String>> response = handler
                .handleStateError(new IllegalStateException("State Error"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("State Error", response.getBody().get("message"));
    }
}
