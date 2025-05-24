package com.bank.ivr.auth.controller;

import com.bank.ivr.auth.model.request.AuthenticationRequest;
import com.bank.ivr.auth.model.response.AuthenticationResponse;
import com.bank.ivr.auth.service.AuthenticationOrchestrator;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for IVR authentication operations.
 * Handles the main authentication endpoint for customer verification.
 */
@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*") // Configure appropriately for production
public class AuthenticationController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);
    
        private final AuthenticationOrchestrator authenticationOrchestrator;        @Autowired    public AuthenticationController(AuthenticationOrchestrator authenticationOrchestrator) {        this.authenticationOrchestrator = authenticationOrchestrator;
    }
    
    /**
     * Main authentication endpoint for IVR customers.
     * Handles both new authentication attempts and continuation of existing attempts.
     * 
     * @param request the authentication request containing customer identifier and tokens
     * @return authentication response with next steps or final result
     */
    @PostMapping("/customer")
    public ResponseEntity<AuthenticationResponse> authenticateCustomer(@Valid @RequestBody AuthenticationRequest request) {
        logger.info("Received authentication request for session: {}", request.getSessionId());
        
        try {
            AuthenticationResponse response = authenticationOrchestrator.authenticateCustomer(request);
            
            // Log the result (without sensitive data)
            logger.info("Authentication request processed for session: {} with status: {}", 
                       request.getSessionId(), response.getStatus());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid authentication request for session: {}: {}", request.getSessionId(), e.getMessage());
            
            AuthenticationResponse errorResponse = AuthenticationResponse.builder()
                    .attemptId(request.getAttemptId())
                    .status(AuthenticationResponse.AuthStatus.FAILED)
                    .message("Invalid request: " + e.getMessage())
                    .build();
            
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Unexpected error processing authentication request for session: {}", 
                        request.getSessionId(), e);
            
            AuthenticationResponse errorResponse = AuthenticationResponse.builder()
                    .attemptId(request.getAttemptId())
                    .status(AuthenticationResponse.AuthStatus.FAILED)
                    .message("An unexpected error occurred. Please try again.")
                    .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Health check endpoint to verify the service is running.
     * 
     * @return simple health status
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("IVR Authentication Service is healthy");
    }
    
    /**
     * Get available authentication methods endpoint (optional).
     * This could be used by IVR systems to understand what authentication options are available.
     * 
     * @return list of available authentication methods
     */
    @GetMapping("/methods")
    public ResponseEntity<Object> getAuthenticationMethods() {
        // This is a placeholder - in a real implementation, this might return
        // information about available authentication methods based on business rules
        return ResponseEntity.ok("Authentication methods endpoint - implementation pending");
    }
} 