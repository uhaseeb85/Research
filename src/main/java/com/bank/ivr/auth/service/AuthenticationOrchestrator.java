package com.bank.ivr.auth.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bank.ivr.auth.model.domain.AuthenticationContext;
import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.model.domain.DnisConfiguration;
import com.bank.ivr.auth.model.request.AuthenticationRequest;
import com.bank.ivr.auth.model.response.AuthenticationResponse;
import com.bank.ivr.auth.model.response.AuthenticationResponse.AuthStatus;
import com.bank.ivr.auth.repository.CustomerProfileRepository;

/**
 * Main authentication orchestrator that coordinates the IVR authentication flow.
 * This service delegates specific responsibilities to specialized services and is fully brand-aware.
 * Now supports context-based DNIS and session SSN retrieval with DNIS-specific authentication logic.
 */
@Service
public class AuthenticationOrchestrator {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationOrchestrator.class);
    
    private final CustomerProfileRepository customerProfileRepository;
    private final AuthenticationContextService contextService;
    private final TokenProcessingService tokenProcessingService;
    private final AuthenticationResponseService responseService;
    private final BrandAuthConfigurationService brandConfigService;
    private final CustomerLookupService customerLookupService;
    private final DnisConfigurationService dnisConfigService;
    
    @Autowired
    public AuthenticationOrchestrator(
            CustomerProfileRepository customerProfileRepository,
            AuthenticationContextService contextService,
            TokenProcessingService tokenProcessingService,
            AuthenticationResponseService responseService,
            BrandAuthConfigurationService brandConfigService,
            CustomerLookupService customerLookupService,
            DnisConfigurationService dnisConfigService) {
        this.customerProfileRepository = customerProfileRepository;
        this.contextService = contextService;
        this.tokenProcessingService = tokenProcessingService;
        this.responseService = responseService;
        this.brandConfigService = brandConfigService;
        this.customerLookupService = customerLookupService;
        this.dnisConfigService = dnisConfigService;
    }
    
    /**
     * Main authentication method that handles both new and continuing authentication attempts.
     * Now fully brand-aware across the entire authentication flow and supports context-based data.
     * 
     * @param request the authentication request containing brand information
     * @param dnis the DNIS retrieved from session context (can be null)
     * @param sessionSsnList the list of SSNs retrieved from session context (can be null)
     * @return the authentication response
     */
    public AuthenticationResponse authenticateCustomer(AuthenticationRequest request, String dnis, List<String> sessionSsnList) {
        String brand = request.getBrand();
        
        // Get DNIS configuration for this call
        DnisConfiguration dnisConfig = dnisConfigService.getDnisConfiguration(dnis);
        
        logger.info("Processing brand-aware authentication request for session: {}, brand: {}, dnis: {}, sessionSsnCount: {}, dnisConfig: {}", 
                   request.getSessionId(), brand, dnis, sessionSsnList != null ? sessionSsnList.size() : 0, dnisConfig.getDescription());
        
        try {
            // Validate brand support before processing
            if (!brandConfigService.isBrandSupported(brand)) {
                logger.warn("Unsupported brand '{}' in authentication request for session: {}", 
                           brand, request.getSessionId());
                return AuthenticationResponse.builder()
                        .attemptId(request.getAttemptId())
                        .status(AuthStatus.FAILED)
                        .message("Brand '" + brand + "' is not supported")
                        .build();
            }
            
            if (request.isNewAttempt()) {
                return handleNewAuthenticationAttempt(request, dnis, sessionSsnList, dnisConfig);
            } else {
                return handleContinuingAuthenticationAttempt(request, dnis, sessionSsnList, dnisConfig);
            }
        } catch (Exception e) {
            logger.error("Authentication error for session: {}, brand: {}, dnis: {}", 
                        request.getSessionId(), brand, dnis, e);
            
            String brandMessage = brandConfigService.getBrandMessage(brand, "failure");
            return AuthenticationResponse.builder()
                    .attemptId(request.getAttemptId())
                    .status(AuthStatus.FAILED)
                    .message(brandMessage != null ? brandMessage : "An unexpected error occurred. Please try again.")
                    .build();
        }
    }
    
    /**
     * Backward compatibility method for existing calls without context parameters.
     */
    public AuthenticationResponse authenticateCustomer(AuthenticationRequest request) {
        return authenticateCustomer(request, null, null);
    }
    
    /**
     * Handles a new authentication attempt with full brand awareness and context support.
     */
    private AuthenticationResponse handleNewAuthenticationAttempt(AuthenticationRequest request, String dnis, List<String> sessionSsnList, DnisConfiguration dnisConfig) {
        String brand = request.getBrand();
        logger.debug("Handling new brand-aware authentication attempt for session: {}, brand: {}, dnis: {}", 
                    request.getSessionId(), brand, dnis);
        
        // Enhanced customer lookup using session SSN if available
        Optional<CustomerProfile> customerProfileOpt = findCustomerWithContext(request, sessionSsnList);
        
        if (!customerProfileOpt.isPresent()) {
            logger.warn("Customer not found for identifier: {}, brand: {}, sessionSsnCount: {}", 
                       request.getCustomerIdentifier(), brand, sessionSsnList != null ? sessionSsnList.size() : 0);
            
            // Use brand-specific message for customer not found
            String errorMessage = brandConfigService.getBrandMessage(brand, "customer_not_found");
            if (errorMessage == null) {
                errorMessage = "Customer not found. Please verify your information.";
            }
            
            return AuthenticationResponse.builder()
                    .status(AuthStatus.FAILED)
                    .message(errorMessage)
                    .build();
        }
        
        CustomerProfile customerProfile = customerProfileOpt.get();
        
        // Generate new attempt ID
        String attemptId = contextService.generateAttemptId();
        
        // Create brand-aware authentication context with DNIS configuration
        AuthenticationContext context = contextService.createInitialContextWithDnis(attemptId, request, customerProfile, dnisConfig);
        
        // Save context
        contextService.saveContext(context);
        
        // Build and return brand-aware response with DNIS considerations
        return responseService.buildResponseWithDnis(context, customerProfile, brand, dnisConfig);
    }
    
    /**
     * Handles a continuing authentication attempt with brand awareness, context support, and DNIS configuration.
     */
    private AuthenticationResponse handleContinuingAuthenticationAttempt(AuthenticationRequest request, String dnis, List<String> sessionSsnList, DnisConfiguration dnisConfig) {
        String brand = request.getBrand();
        logger.debug("Handling continuing brand-aware authentication attempt: {}, brand: {}, dnis: {}", 
                    request.getAttemptId(), brand, dnis);
        
        // Retrieve existing context
        Optional<AuthenticationContext> contextOpt = contextService.getContextByAttemptId(request.getAttemptId());
        
        if (!contextOpt.isPresent()) {
            logger.warn("Authentication context not found for attempt: {}, brand: {}", 
                       request.getAttemptId(), brand);
            
            // Use brand-specific message for expired session
            String errorMessage = brandConfigService.getBrandMessage(brand, "session_expired");
            if (errorMessage == null) {
                errorMessage = "Authentication session expired. Please start over.";
            }
            
            return AuthenticationResponse.builder()
                    .attemptId(request.getAttemptId())
                    .status(AuthStatus.FAILED)
                    .message(errorMessage)
                    .build();
        }
        
        AuthenticationContext context = contextOpt.get();
        
        // Validate brand consistency
        if (!brand.equals(context.getBrand())) {
            logger.warn("Brand mismatch - Request: {}, Context: {}, Attempt: {}", 
                       brand, context.getBrand(), request.getAttemptId());
            return AuthenticationResponse.builder()
                    .attemptId(request.getAttemptId())
                    .status(AuthStatus.FAILED)
                    .message("Brand validation failed. Please start over.")
                    .build();
        }
        
        // Retrieve customer profile
        Optional<CustomerProfile> customerProfileOpt = customerProfileRepository.findByCustomerIdentifier(request.getCustomerIdentifier());
        
        if (!customerProfileOpt.isPresent()) {
            logger.error("Customer profile missing during continuing authentication for attempt: {}", request.getAttemptId());
            return AuthenticationResponse.builder()
                    .attemptId(request.getAttemptId())
                    .status(AuthStatus.FAILED)
                    .message("Customer profile not found. Please start over.")
                    .build();
        }
        
        CustomerProfile customerProfile = customerProfileOpt.get();
        
        // Process provided tokens with DNIS configuration
        if (request.getProvidedTokens() != null && !request.getProvidedTokens().isEmpty()) {
            context = tokenProcessingService.processTokensWithDnis(request.getProvidedTokens(), context, customerProfile, brand, dnisConfig);
            contextService.saveContext(context);
        }
        
        // Build and return brand-aware response with DNIS considerations
        return responseService.buildResponseWithDnis(context, customerProfile, brand, dnisConfig);
    }
    
    /**
     * Enhanced customer lookup that prioritizes session SSN if available.
     */
    private Optional<CustomerProfile> findCustomerWithContext(AuthenticationRequest request, List<String> sessionSsnList) {
        // Priority 1: Try session SSN list if available
        if (sessionSsnList != null && !sessionSsnList.isEmpty()) {
            for (String sessionSsn : sessionSsnList) {
                Optional<CustomerProfile> customer = customerLookupService.lookupCustomerBySessionSsn(sessionSsn);
                if (customer.isPresent()) {
                    logger.info("Customer found using session SSN for session: {}", request.getSessionId());
                    return customer;
                }
            }
            logger.debug("No customer found using session SSN list for session: {}", request.getSessionId());
        }
        
        // Priority 2: Fall back to standard customer identifier lookup
        Optional<CustomerProfile> customer = customerProfileRepository.findByCustomerIdentifier(request.getCustomerIdentifier());
        if (customer.isPresent()) {
            logger.info("Customer found using standard identifier for session: {}", request.getSessionId());
        } else {
            logger.debug("No customer found using standard identifier for session: {}", request.getSessionId());
        }
        
        return customer;
    }
} 