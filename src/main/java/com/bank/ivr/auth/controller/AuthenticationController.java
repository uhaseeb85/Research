package com.bank.ivr.auth.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bank.ivr.auth.model.domain.AuthTokenDefinition;
import com.bank.ivr.auth.model.request.AuthenticationRequest;
import com.bank.ivr.auth.model.response.AuthenticationResponse;
import com.bank.ivr.auth.service.AuthenticationOrchestrator;
import com.bank.ivr.auth.service.BrandAuthConfigurationService;
import com.bank.ivr.auth.util.LoggingUtil;

import jakarta.validation.Valid;

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
            
            List<AuthTokenDefinition> tokenDefinitions = brandConfigService.getTokenDefinitionsForBrand(brand);
            int maxAttempts = brandConfigService.getMaxOverallAttemptsForBrand(brand);
            boolean concurrentAuthAllowed = brandConfigService.isConcurrentTokenAuthAllowed(brand);
            
            // Convert token definitions to maps without using streams
            List<Map<String, Object>> tokenMaps = new ArrayList<>();
            for (AuthTokenDefinition token : tokenDefinitions) {
                Map<String, Object> tokenMap = new HashMap<>();
                tokenMap.put("name", token.getName());
                tokenMap.put("description", token.getDescription());
                tokenMap.put("priority", token.getPriority());
                tokenMap.put("maxAttempts", token.getMaxAttempts());
                tokenMaps.add(tokenMap);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("brand", brand);
            response.put("tokenDefinitions", tokenMaps);
            response.put("maxOverallAttempts", maxAttempts);
            response.put("concurrentAuthAllowed", concurrentAuthAllowed);
            
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
            Set<String> supportedBrandsSet = brandConfigService.getAvailableBrands();
            List<String> supportedBrands = new ArrayList<>(supportedBrandsSet);
            logger.info("Retrieved {} supported brands", supportedBrands.size());
            
            Map<String, Object> response = new HashMap<>();
            response.put("supportedBrands", supportedBrands);
            response.put("count", supportedBrands.size());
            
            return ResponseEntity.ok(response);
            
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