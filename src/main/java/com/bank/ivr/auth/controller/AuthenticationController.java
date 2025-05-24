package com.bank.ivr.auth.controller;

import com.bank.ivr.auth.model.request.AuthenticationRequest;
import com.bank.ivr.auth.model.response.AuthenticationResponse;
import com.bank.ivr.auth.service.AuthenticationOrchestrator;
import com.bank.ivr.auth.service.BrandAuthConfigurationService;
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
        
        logger.info("AUTH_REQUEST_STARTED - SessionId: {}, AttemptId: {}, Brand: {}, CustomerRef: {}, IsNewAttempt: {}", 
                   sessionId, attemptId, brand, request.getCustomerIdentifier(), request.isNewAttempt());
        
        // Validate brand support
        if (!brandConfigService.isBrandSupported(brand)) {
            logger.warn("BRAND_NOT_SUPPORTED - SessionId: {}, Brand: {}, SupportedBrands: {}", 
                       sessionId, brand, brandConfigService.getAvailableBrands());
            
            AuthenticationResponse errorResponse = AuthenticationResponse.builder()
                    .attemptId(attemptId)
                    .status(AuthenticationResponse.AuthStatus.FAILED)
                    .message("Brand '" + brand + "' is not supported")
                    .build();
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        logger.debug("AUTH_REQUEST_DETAILS - SessionId: {}, Brand: {}, RequestType: {}, HasProvidedTokens: {}, TokenCount: {}", 
                    sessionId, brand, request.getClass().getSimpleName(), 
                    request.getProvidedTokens() != null && !request.getProvidedTokens().isEmpty(),
                    request.getProvidedTokens() != null ? request.getProvidedTokens().size() : 0);
        
        // Log brand-specific configuration being used
        logger.debug("BRAND_CONFIG_APPLIED - SessionId: {}, Brand: {}, MaxOverallAttempts: {}, RequiredTokens: {}, ConcurrentAuthAllowed: {}", 
                    sessionId, brand, 
                    brandConfigService.getMaxOverallAttemptsForBrand(brand),
                    brandConfigService.getRequiredTokensForBrand(brand),
                    brandConfigService.isConcurrentTokenAuthAllowed(brand));
        
        try {
            logger.debug("AUTH_ORCHESTRATOR_CALL_START - SessionId: {}, Brand: {}", sessionId, brand);
            AuthenticationResponse response = authenticationOrchestrator.authenticateCustomer(request);
            long processingTime = System.currentTimeMillis() - startTime;
            
            // Log successful authentication with metrics
            logger.info("AUTH_REQUEST_SUCCESS - SessionId: {}, AttemptId: {}, Brand: {}, Status: {}, ProcessingTimeMs: {}", 
                       sessionId, attemptId, brand, response.getStatus(), processingTime);
            
            // Log additional response details for monitoring
            if (response.getRemainingAttempts() != null && !response.getRemainingAttempts().isEmpty()) {
                logger.debug("AUTH_ATTEMPTS_REMAINING - SessionId: {}, Brand: {}, RemainingAttempts: {}", 
                           sessionId, brand, response.getRemainingAttempts());
            }
            
            if (response.getPrimaryTokenToAsk() != null) {
                logger.debug("AUTH_PRIMARY_TOKEN_REQUESTED - SessionId: {}, Brand: {}, TokenType: {}, Priority: {}", 
                           sessionId, brand, response.getPrimaryTokenToAsk().getName(), response.getPrimaryTokenToAsk().getPriority());
            }
            
            if (response.getAuthenticatedTokens() != null && !response.getAuthenticatedTokens().isEmpty()) {
                logger.debug("AUTH_TOKENS_AUTHENTICATED - SessionId: {}, Brand: {}, AuthenticatedTokens: {}", 
                           sessionId, brand, response.getAuthenticatedTokens());
            }
            
            if (response.getRequiredTokensRemaining() != null && !response.getRequiredTokensRemaining().isEmpty()) {
                logger.debug("AUTH_TOKENS_REMAINING - SessionId: {}, Brand: {}, TokensRemaining: {}", 
                           sessionId, brand, response.getRequiredTokensRemaining());
            }
            
            logger.debug("AUTH_REQUEST_COMPLETED - SessionId: {}, Brand: {}, HttpStatus: 200", sessionId, brand);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            long processingTime = System.currentTimeMillis() - startTime;
            logger.warn("AUTH_REQUEST_VALIDATION_ERROR - SessionId: {}, AttemptId: {}, Brand: {}, ProcessingTimeMs: {}, Error: {}", 
                       sessionId, attemptId, brand, processingTime, e.getMessage());
            logger.debug("AUTH_VALIDATION_ERROR_DETAILS - SessionId: {}, Brand: {}, Exception: {}", 
                        sessionId, brand, e.getClass().getSimpleName());
            
            String brandMessage = brandConfigService.getBrandMessage(brand, "failure");
            
            AuthenticationResponse errorResponse = AuthenticationResponse.builder()
                    .attemptId(attemptId)
                    .status(AuthenticationResponse.AuthStatus.FAILED)
                    .message("Invalid request: " + e.getMessage())
                    .build();
            
            logger.debug("AUTH_REQUEST_COMPLETED - SessionId: {}, Brand: {}, HttpStatus: 400", sessionId, brand);
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            logger.error("AUTH_REQUEST_SYSTEM_ERROR - SessionId: {}, AttemptId: {}, Brand: {}, ProcessingTimeMs: {}, ExceptionType: {}", 
                        sessionId, attemptId, brand, processingTime, e.getClass().getSimpleName(), e);
            
            String brandMessage = brandConfigService.getBrandMessage(brand, "failure");
            
            AuthenticationResponse errorResponse = AuthenticationResponse.builder()
                    .attemptId(attemptId)
                    .status(AuthenticationResponse.AuthStatus.FAILED)
                    .message(brandMessage != null ? brandMessage : "An unexpected error occurred. Please try again.")
                    .build();
            
            logger.debug("AUTH_REQUEST_COMPLETED - SessionId: {}, Brand: {}, HttpStatus: 500", sessionId, brand);
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
        long startTime = System.currentTimeMillis();
        
        logger.debug("AUTH_METHODS_REQUEST - Brand: {}, AuthenticationMethodsEndpointAccessed", brand);
        
        try {
            if (!brandConfigService.isBrandSupported(brand)) {
                logger.warn("BRAND_NOT_SUPPORTED_METHODS - Brand: {}, SupportedBrands: {}", 
                           brand, brandConfigService.getAvailableBrands());
                return ResponseEntity.badRequest()
                        .body("Brand '" + brand + "' is not supported. Supported brands: " + 
                              brandConfigService.getAvailableBrands());
            }
            
            var tokenDefinitions = brandConfigService.getTokenDefinitionsForBrand(brand);
            var requiredTokens = brandConfigService.getRequiredTokensForBrand(brand);
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
                "requiredTokens", requiredTokens,
                "maxOverallAttempts", maxAttempts,
                "concurrentAuthAllowed", concurrentAuthAllowed
            );
            
            long processingTime = System.currentTimeMillis() - startTime;
            logger.info("AUTH_METHODS_SUCCESS - Brand: {}, TokenCount: {}, ProcessingTimeMs: {}", 
                       brand, tokenDefinitions.size(), processingTime);
            logger.debug("AUTH_METHODS_COMPLETED - Brand: {}, HttpStatus: 200", brand);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            logger.error("AUTH_METHODS_ERROR - Brand: {}, ProcessingTimeMs: {}, ExceptionType: {}", 
                        brand, processingTime, e.getClass().getSimpleName(), e);
            
            logger.debug("AUTH_METHODS_COMPLETED - Brand: {}, HttpStatus: 500", brand);
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
        long startTime = System.currentTimeMillis();
        
        logger.debug("BRANDS_REQUEST - SupportedBrandsEndpointAccessed");
        
        try {
            var supportedBrands = brandConfigService.getAvailableBrands();
            long processingTime = System.currentTimeMillis() - startTime;
            
            logger.info("BRANDS_SUCCESS - SupportedBrandCount: {}, ProcessingTimeMs: {}", 
                       supportedBrands.size(), processingTime);
            logger.debug("BRANDS_COMPLETED - HttpStatus: 200");
            
            return ResponseEntity.ok(Map.of(
                "supportedBrands", supportedBrands,
                "count", supportedBrands.size()
            ));
            
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            logger.error("BRANDS_ERROR - ProcessingTimeMs: {}, ExceptionType: {}", 
                        processingTime, e.getClass().getSimpleName(), e);
            
            logger.debug("BRANDS_COMPLETED - HttpStatus: 500");
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
        long startTime = System.currentTimeMillis();
        
        logger.debug("HEALTH_CHECK_REQUEST - HealthCheckEndpointAccessed");
        
        try {
            String healthStatus = "IVR Authentication Service is healthy (Brand-aware)";
            long processingTime = System.currentTimeMillis() - startTime;
            
            logger.info("HEALTH_CHECK_SUCCESS - Status: healthy, ProcessingTimeMs: {}", processingTime);
            logger.debug("HEALTH_CHECK_COMPLETED - HttpStatus: 200");
            
            return ResponseEntity.ok(healthStatus);
            
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            logger.error("HEALTH_CHECK_ERROR - ProcessingTimeMs: {}, ExceptionType: {}", 
                        processingTime, e.getClass().getSimpleName(), e);
            
            logger.debug("HEALTH_CHECK_COMPLETED - HttpStatus: 500");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Health check failed: " + e.getMessage());
        }
    }
} 