package com.bank.ivr.auth.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import com.bank.ivr.auth.model.domain.DnisConfiguration;
import com.bank.ivr.auth.model.request.AuthenticationRequest;
import com.bank.ivr.auth.model.response.AuthenticationResponse;
import com.bank.ivr.auth.service.AuthenticationOrchestrator;
import com.bank.ivr.auth.service.BrandAuthConfigurationService;
import com.bank.ivr.auth.service.DnisConfigurationService;
import com.bank.ivr.auth.service.SessionContextService;
import com.bank.ivr.auth.util.LoggingUtil;

import jakarta.validation.Valid;

/**
 * REST controller for IVR authentication operations.
 * Handles brand-aware authentication endpoint for customer verification.
 * Now includes DNIS configuration support and session context integration.
 */
@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*") // Configure appropriately for production
public class AuthenticationController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);
    
    private final AuthenticationOrchestrator authenticationOrchestrator;
    private final BrandAuthConfigurationService brandConfigService;
    private final DnisConfigurationService dnisConfigService;
    private final SessionContextService sessionContextService;
    
    @Autowired
    public AuthenticationController(AuthenticationOrchestrator authenticationOrchestrator,
                                   BrandAuthConfigurationService brandConfigService,
                                   DnisConfigurationService dnisConfigService,
                                   SessionContextService sessionContextService) {
        this.authenticationOrchestrator = authenticationOrchestrator;
        this.brandConfigService = brandConfigService;
        this.dnisConfigService = dnisConfigService;
        this.sessionContextService = sessionContextService;
        logger.info("AUTH_CONTROLLER_INITIALIZED - Brand-aware AuthenticationController with DNIS support and session context started successfully");
    }
    
    /**
     * Main authentication endpoint for IVR customers.
     * Handles both new authentication attempts and continuation of existing attempts.
     * Now supports brand-specific authentication rules, token priorities, and DNIS configurations.
     * DNIS and sessionSsn are retrieved from session context instead of request body.
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
            
            // Retrieve DNIS from session context
            Optional<String> dnisOpt = sessionContextService.getDnisFromSession(sessionId);
            String dnis = dnisOpt.orElse(null);
            
            // Retrieve session SSN list from session context
            Optional<List<String>> sessionSsnListOpt = sessionContextService.getSessionSsnFromSession(sessionId);
            List<String> sessionSsnList = sessionSsnListOpt.orElse(null);
            
            // Log context information
            if (dnis != null) {
                logger.info("DNIS retrieved from session context: {} for session: {}", dnis, sessionId);
                DnisConfiguration dnisConfig = dnisConfigService.getDnisConfiguration(dnis);
                logger.info("Using DNIS configuration: {} - {}", dnisConfig.getDnis(), dnisConfig.getDescription());
            } else {
                logger.debug("No DNIS found in session context for session: {}", sessionId);
            }
            
            if (sessionSsnList != null && !sessionSsnList.isEmpty()) {
                logger.info("Session SSN list retrieved from context for session: {} (count: {})", sessionId, sessionSsnList.size());
            } else {
                logger.debug("No session SSN found in context for session: {}", sessionId);
            }
        
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
        
            // Pass the context information to the orchestrator
            AuthenticationResponse response = authenticationOrchestrator.authenticateCustomer(request, dnis, sessionSsnList);
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
     * Get DNIS configuration endpoint.
     * Returns the configuration for a specific DNIS number.
     * 
     * @param dnis the DNIS number
     * @return DNIS configuration
     */
    @GetMapping("/dnis/{dnis}")
    public ResponseEntity<Object> getDnisConfiguration(@PathVariable String dnis) {
        logger.debug("Getting DNIS configuration for: {}", dnis);
        
        try {
            DnisConfiguration config = dnisConfigService.getDnisConfiguration(dnis);
            
            Map<String, Object> response = new HashMap<>();
            response.put("dnis", config.getDnis());
            response.put("description", config.getDescription());
            response.put("allowSsnAuthentication", config.isAllowSsnAuthentication());
            response.put("allowPinAuthentication", config.isAllowPinAuthentication());
            response.put("allowDateOfBirthAuthentication", config.isAllowDateOfBirthAuthentication());
            response.put("allowMotherMaidenNameAuthentication", config.isAllowMotherMaidenNameAuthentication());
            response.put("allowAccountNumberAuthentication", config.isAllowAccountNumberAuthentication());
            response.put("requireMultiFactorAuth", config.isRequireMultiFactorAuth());
            response.put("allowTrustLevelBypass", config.isAllowTrustLevelBypass());
            response.put("enablePhoneMatchValidation", config.isEnablePhoneMatchValidation());
            response.put("allowAlternativeTokens", config.isAllowAlternativeTokens());
            response.put("enableStrictValidation", config.isEnableStrictValidation());
            response.put("allowRetryOnFailure", config.isAllowRetryOnFailure());
            response.put("enableAuditLogging", config.isEnableAuditLogging());
            response.put("maxAuthenticationAttempts", config.getMaxAuthenticationAttempts());
            response.put("sessionTimeoutMinutes", config.getSessionTimeoutMinutes());
            
            logger.info("Retrieved DNIS configuration for: {} - {}", config.getDnis(), config.getDescription());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error retrieving DNIS configuration for: {}", dnis, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to retrieve DNIS configuration for '" + dnis + "': " + e.getMessage());
        }
    }
    
    /**
     * Get all DNIS configurations endpoint.
     * 
     * @return list of all DNIS configurations
     */
    @GetMapping("/dnis")
    public ResponseEntity<Object> getAllDnisConfigurations() {
        logger.debug("Getting all DNIS configurations");
        
        try {
            Map<String, DnisConfiguration> allConfigs = dnisConfigService.getAllDnisConfigurations();
            List<Map<String, Object>> configList = new ArrayList<>();
            
            for (DnisConfiguration config : allConfigs.values()) {
                Map<String, Object> configMap = new HashMap<>();
                configMap.put("dnis", config.getDnis());
                configMap.put("description", config.getDescription());
                configMap.put("allowSsnAuthentication", config.isAllowSsnAuthentication());
                configMap.put("allowPinAuthentication", config.isAllowPinAuthentication());
                configMap.put("requireMultiFactorAuth", config.isRequireMultiFactorAuth());
                configMap.put("maxAuthenticationAttempts", config.getMaxAuthenticationAttempts());
                configMap.put("sessionTimeoutMinutes", config.getSessionTimeoutMinutes());
                configList.add(configMap);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("dnisConfigurations", configList);
            response.put("count", configList.size());
            
            logger.info("Retrieved {} DNIS configurations", configList.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error retrieving all DNIS configurations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to retrieve DNIS configurations: " + e.getMessage());
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
            return ResponseEntity.ok("IVR Authentication Service is healthy (Brand-aware with DNIS support)");
        } catch (Exception e) {
            logger.error("Health check failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Health check failed: " + e.getMessage());
        }
    }
} 