package com.taskflow.exception;

import com.taskflow.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
    }

    @Test
    void handleResourceNotFoundException_ShouldReturn404AndErrorDetails() {
        // Arrange
        String errorMessage = "Workflow not found with ID: 123";
        ResourceNotFoundException ex = new ResourceNotFoundException(errorMessage);
        when(request.getRequestURI()).thenReturn("/api/v1/workflows/123");

        // Act
        ResponseEntity<ErrorResponse> responseEntity = exceptionHandler.handleResourceNotFoundException(ex, request);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        
        ErrorResponse errorResponse = responseEntity.getBody();
        assertNotNull(errorResponse);
        assertEquals(HttpStatus.NOT_FOUND.value(), errorResponse.status());
        assertEquals("Not Found", errorResponse.error());
        assertEquals(errorMessage, errorResponse.message());
        assertEquals("/api/v1/workflows/123", errorResponse.path());
        assertNotNull(errorResponse.timestamp());
    }
}
