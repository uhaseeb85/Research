package com.bank.ivr.auth.controller;

import com.bank.ivr.auth.model.request.AuthenticationRequest;
import com.bank.ivr.auth.model.response.AuthenticationResponse;
import com.bank.ivr.auth.service.AuthenticationOrchestrator;
import com.bank.ivr.auth.service.BrandAuthConfigurationService;
import com.bank.ivr.auth.util.LoggingUtil;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for IVR authentication operations.
 * Handles brand-aware authentication endpoint for customer verification.
 */
@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*") // Configure appropriately for production
public class AuthenticationController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);
    
    private final AuthenticationOrchestrator authenticationOrchestrator;
    private final BrandAuthConfigurationService brandConfigService;
    
    @Autowired
    public AuthenticationController(AuthenticationOrchestrator authenticationOrchestrator,
                                   BrandAuthConfigurationService brandConfigService) {
        this.authenticationOrchestrator = authenticationOrchestrator;
        this.brandConfigService = brandConfigService;
        logger.info("AUTH_CONTROLLER_INITIALIZED - Brand-aware AuthenticationController started successfully");
    }
    
    /**
     * Main authentication endpoint for IVR customers.
     * Handles both new authentication attempts and continuation of existing attempts.
     * Now supports brand-specific authentication rules and token priorities.
     * 
     * @param request the authentication request containing customer identifier, tokens, and brand
     * @return authentication response with next steps or final result
     */
    @PostMapping("/customer")
    public ResponseEntity<AuthenticationResponse> authenticateCustomer(@Valid @RequestBody AuthenticationRequest request) {
        long startTime = System.currentTimeMillis();
        String sessionId = request.getSessionId();
        String attemptId = request.getAttemptId();
        String brand = request.getBrand();
        
        try {
            LoggingUtil.logAuthStart(logger, sessionId, brand, request.getCustomerIdentifier().toString());
        
            // Validate brand support
            if (!brandConfigService.isBrandSupported(brand)) {
                logger.warn("Unsupported brand: {} - Supported brands: {}", brand, brandConfigService.getAvailableBrands());
                
                AuthenticationResponse errorResponse = AuthenticationResponse.builder()
                        .attemptId(attemptId)
                        .status(AuthenticationResponse.AuthStatus.FAILED)
                        .message("Brand '" + brand + "' is not supported")
                        .build();
                
                return ResponseEntity.badRequest().body(errorResponse);
            }
        
            AuthenticationResponse response = authenticationOrchestrator.authenticateCustomer(request);
            long processingTime = System.currentTimeMillis() - startTime;
            
            LoggingUtil.logAuthComplete(logger, sessionId, brand, response.getStatus().toString(), processingTime);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid authentication request - SessionId: {}, Brand: {}, Error: {}", 
                       sessionId, brand, e.getMessage());
            
            String brandMessage = brandConfigService.getBrandMessage(brand, "failure");
            AuthenticationResponse errorResponse = AuthenticationResponse.builder()
                    .attemptId(attemptId)
                    .status(AuthenticationResponse.AuthStatus.FAILED)
                    .message(brandMessage != null ? brandMessage : "Invalid request: " + e.getMessage())
                    .build();
            
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Authentication error - SessionId: {}, Brand: {}", sessionId, brand, e);
            
            String brandMessage = brandConfigService.getBrandMessage(brand, "failure");
            AuthenticationResponse errorResponse = AuthenticationResponse.builder()
                    .attemptId(attemptId)
                    .status(AuthenticationResponse.AuthStatus.FAILED)
                    .message(brandMessage != null ? brandMessage : "An unexpected error occurred. Please try again.")
                    .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Get brand-specific authentication methods endpoint.
     * Returns available authentication methods for a specific brand.
     * 
     * @param brand the brand code
     * @return brand-specific authentication methods
     */
    @GetMapping("/methods/{brand}")
    public ResponseEntity<Object> getAuthenticationMethodsForBrand(@PathVariable String brand) {
        logger.debug("Getting authentication methods for brand: {}", brand);
        
        try {
            if (!brandConfigService.isBrandSupported(brand)) {
                logger.warn("Unsupported brand: {}", brand);
                return ResponseEntity.badRequest()
                        .body("Brand '" + brand + "' is not supported. Supported brands: " + 
                              brandConfigService.getAvailableBrands());
            }
            
            var tokenDefinitions = brandConfigService.getTokenDefinitionsForBrand(brand);

            var maxAttempts = brandConfigService.getMaxOverallAttemptsForBrand(brand);
            var concurrentAuthAllowed = brandConfigService.isConcurrentTokenAuthAllowed(brand);
            
            var response = Map.of(
                "brand", brand,
                "tokenDefinitions", tokenDefinitions.stream().map(token -> Map.of(
                    "name", token.getName(),
                    "description", token.getDescription(),
                    "priority", token.getPriority(),
                    "maxAttempts", token.getMaxAttempts()
                )).toList(),

                "maxOverallAttempts", maxAttempts,
                "concurrentAuthAllowed", concurrentAuthAllowed
            );
            
            logger.info("Retrieved authentication methods for brand: {} ({} tokens)", brand, tokenDefinitions.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error retrieving authentication methods for brand: {}", brand, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to retrieve authentication methods for brand '" + brand + "': " + e.getMessage());
        }
    }
    
    /**
     * Get all supported brands endpoint.
     * 
     * @return list of supported brand codes
     */
    @GetMapping("/brands")
    public ResponseEntity<Object> getSupportedBrands() {
        logger.debug("Getting supported brands");
        
        try {
            var supportedBrands = brandConfigService.getAvailableBrands();
            logger.info("Retrieved {} supported brands", supportedBrands.size());
            
            return ResponseEntity.ok(Map.of(
                "supportedBrands", supportedBrands,
                "count", supportedBrands.size()
            ));
            
        } catch (Exception e) {
            logger.error("Error retrieving supported brands", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to retrieve supported brands: " + e.getMessage());
        }
    }
    
    /**
     * Health check endpoint to verify the service is running.
     * 
     * @return simple health status
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        try {
            return ResponseEntity.ok("IVR Authentication Service is healthy (Brand-aware)");
        } catch (Exception e) {
            logger.error("Health check failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Health check failed: " + e.getMessage());
        }
    }
} 