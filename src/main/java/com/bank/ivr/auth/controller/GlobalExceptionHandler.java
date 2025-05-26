package com.bank.ivr.auth.controller;

import com.bank.ivr.auth.model.response.AuthenticationResponse;
import com.bank.ivr.auth.service.BrandAuthConfigurationService;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Global exception handler for the IVR Authentication API.
 * Provides centralized error handling with brand-aware error messages.
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    private final BrandAuthConfigurationService brandConfigService;
    
    @Autowired
    public GlobalExceptionHandler(BrandAuthConfigurationService brandConfigService) {
        this.brandConfigService = brandConfigService;
    }
    
    /**
     * Handles validation errors from @Valid annotations.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AuthenticationResponse> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        String sessionId = extractSessionId(request);
        String brand = extractBrand(request);
        
        logger.warn("VALIDATION_ERROR - SessionId: {}, Brand: {}, ValidationErrors: {}", 
                   sessionId, brand, ex.getBindingResult().getFieldErrors().size());
        
        String errorMessage = "Invalid request data";
        if (!ex.getBindingResult().getFieldErrors().isEmpty()) {
            errorMessage = ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        }
        
        String brandMessage = getBrandSpecificMessage(brand, "validation_error", errorMessage);
        
        AuthenticationResponse errorResponse = AuthenticationResponse.builder()
                .status(AuthenticationResponse.AuthStatus.FAILED)
                .message(brandMessage)
                .build();
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * Handles constraint violation exceptions.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<AuthenticationResponse> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {
        
        String sessionId = extractSessionId(request);
        String brand = extractBrand(request);
        
        logger.warn("CONSTRAINT_VIOLATION - SessionId: {}, Brand: {}, Violations: {}", 
                   sessionId, brand, ex.getConstraintViolations().size());
        
        String brandMessage = getBrandSpecificMessage(brand, "validation_error", 
                "Request validation failed");
        
        AuthenticationResponse errorResponse = AuthenticationResponse.builder()
                .status(AuthenticationResponse.AuthStatus.FAILED)
                .message(brandMessage)
                .build();
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * Handles malformed JSON requests.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<AuthenticationResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, WebRequest request) {
        
        String sessionId = extractSessionId(request);
        String brand = extractBrand(request);
        
        logger.warn("MALFORMED_REQUEST - SessionId: {}, Brand: {}, Error: {}", 
                   sessionId, brand, ex.getMessage());
        
        String brandMessage = getBrandSpecificMessage(brand, "malformed_request", 
                "Invalid request format");
        
        AuthenticationResponse errorResponse = AuthenticationResponse.builder()
                .status(AuthenticationResponse.AuthStatus.FAILED)
                .message(brandMessage)
                .build();
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * Handles method argument type mismatch exceptions.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<AuthenticationResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex, WebRequest request) {
        
        String sessionId = extractSessionId(request);
        String brand = extractBrand(request);
        
        logger.warn("ARGUMENT_TYPE_MISMATCH - SessionId: {}, Brand: {}, Parameter: {}, ExpectedType: {}", 
                   sessionId, brand, ex.getName(), ex.getRequiredType());
        
        String brandMessage = getBrandSpecificMessage(brand, "invalid_parameter", 
                "Invalid parameter value");
        
        AuthenticationResponse errorResponse = AuthenticationResponse.builder()
                .status(AuthenticationResponse.AuthStatus.FAILED)
                .message(brandMessage)
                .build();
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * Handles business logic exceptions (IllegalArgumentException).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<AuthenticationResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        
        String sessionId = extractSessionId(request);
        String brand = extractBrand(request);
        
        logger.warn("BUSINESS_LOGIC_ERROR - SessionId: {}, Brand: {}, Error: {}", 
                   sessionId, brand, ex.getMessage());
        
        String brandMessage = getBrandSpecificMessage(brand, "failure", ex.getMessage());
        
        AuthenticationResponse errorResponse = AuthenticationResponse.builder()
                .status(AuthenticationResponse.AuthStatus.FAILED)
                .message(brandMessage)
                .build();
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * Handles illegal state exceptions.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<AuthenticationResponse> handleIllegalStateException(
            IllegalStateException ex, WebRequest request) {
        
        String sessionId = extractSessionId(request);
        String brand = extractBrand(request);
        
        logger.error("ILLEGAL_STATE_ERROR - SessionId: {}, Brand: {}, Error: {}", 
                    sessionId, brand, ex.getMessage(), ex);
        
        String brandMessage = getBrandSpecificMessage(brand, "system_error", 
                "System is in an invalid state. Please try again.");
        
        AuthenticationResponse errorResponse = AuthenticationResponse.builder()
                .status(AuthenticationResponse.AuthStatus.FAILED)
                .message(brandMessage)
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    /**
     * Handles all other unexpected exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<AuthenticationResponse> handleGenericException(
            Exception ex, WebRequest request) {
        
        String sessionId = extractSessionId(request);
        String brand = extractBrand(request);
        
        logger.error("UNEXPECTED_ERROR - SessionId: {}, Brand: {}, ExceptionType: {}, Error: {}", 
                    sessionId, brand, ex.getClass().getSimpleName(), ex.getMessage(), ex);
        
        String brandMessage = getBrandSpecificMessage(brand, "failure", 
                "An unexpected error occurred. Please try again.");
        
        AuthenticationResponse errorResponse = AuthenticationResponse.builder()
                .status(AuthenticationResponse.AuthStatus.FAILED)
                .message(brandMessage)
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    /**
     * Extracts session ID from the request for logging purposes.
     */
    private String extractSessionId(WebRequest request) {
        String sessionId = request.getParameter("sessionId");
        return sessionId != null ? sessionId : "unknown";
    }
    
    /**
     * Extracts brand from the request for brand-specific error handling.
     * Falls back to request parameters for GET requests.
     */
    private String extractBrand(WebRequest request) {
        // Get brand from request parameters (for GET requests)
        String brand = request.getParameter("brand");
        return brand != null ? brand : "DEFAULT";
    }
    
    /**
     * Gets brand-specific error message or falls back to default message.
     */
    private String getBrandSpecificMessage(String brand, String messageType, String defaultMessage) {
        if (brand != null && brandConfigService.isBrandSupported(brand)) {
            String brandMessage = brandConfigService.getBrandMessage(brand, messageType);
            if (brandMessage != null) {
                return brandMessage;
            }
        }
        return defaultMessage;
    }
} 