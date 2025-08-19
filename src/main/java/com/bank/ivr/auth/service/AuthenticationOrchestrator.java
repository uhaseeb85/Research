package com.bank.ivr.auth.service;

import java.util.List;
import java.util.Optional;

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
     * Main authentication endpoint that coordinates the entire IVR authentication flow.
     * 
     * This is the primary entry point for all authentication requests and handles both
     * new authentication attempts and continuation of existing authentication sessions.
     * 
     * AUTHENTICATION FLOW OVERVIEW:
     * 1. Validates brand support and request parameters
     * 2. Retrieves DNIS configuration for call-specific rules
     * 3. Routes to new attempt or continuing attempt handlers
     * 4. Applies brand-specific authentication policies throughout
     * 5. Returns appropriate authentication response with next steps
     * 
     * BRAND AWARENESS:
     * - Validates brand is supported before processing
     * - Applies brand-specific token priorities and rules
     * - Uses brand-specific error messages and responses
     * - Enforces brand-specific security policies
     * 
     * CONTEXT INTEGRATION:
     * - Retrieves DNIS from session context for call routing rules
     * - Uses session SSN list for enhanced customer lookup
     * - Applies DNIS-specific authentication limits and policies
     * - Supports context-based customer identification
     * 
     * ERROR HANDLING:
     * - Comprehensive validation of all input parameters
     * - Brand-specific error messages for better user experience
     * - Graceful degradation when context data unavailable
     * - Full exception handling with appropriate logging
     * 
     * SECURITY FEATURES:
     * - Brand consistency validation across session
     * - Session-based customer lookup for enhanced security
     * - DNIS-based call validation and routing
     * - Comprehensive audit logging for compliance
     * 
     * @param request the authentication request containing customer identifier, tokens,
     *               session ID, brand code, and trust level information
     * @param dnis the DNIS code retrieved from session context for call-specific rules
     *            (null if not available in session)
     * @param sessionSsnList list of SSNs retrieved from session context for enhanced
     *                      customer lookup (null if not available)
     * @return AuthenticationResponse containing next authentication steps, token requests,
     *         success/failure status, and brand-specific messaging
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
     * Handles the creation and setup of a brand new authentication attempt.
     * 
     * This method is called when a customer initiates a fresh authentication session
     * and is responsible for setting up the complete authentication context.
     * 
     * NEW ATTEMPT FLOW:
     * 1. Enhanced customer lookup using session context (SSN list priority)
     * 2. Customer profile validation and brand compatibility checks
     * 3. Generation of unique attempt ID for session tracking
     * 4. Creation of brand-aware authentication context with DNIS rules
     * 5. Determination of eligible authentication tokens for customer
     * 6. Initial response building with first token request
     * 
     * CUSTOMER LOOKUP PRIORITY:
     * - Primary: Session SSN list (if available from session context)
     * - Fallback: Standard customer identifier from request
     * - Enhanced security through multiple lookup methods
     * 
     * CONTEXT CREATION:
     * - Brand-specific token eligibility evaluation
     * - DNIS-specific authentication limits and rules
     * - Trust level information integration
     * - Initial token state and attempt tracking setup
     * 
     * ERROR SCENARIOS:
     * - Customer not found in any lookup method
     * - Brand validation failures
     * - Context creation failures
     * - Returns brand-specific error messages
     * 
     * SECURITY FEATURES:
     * - Multiple customer identification methods
     * - Brand consistency validation
     * - Session context integration for enhanced security
     * - Comprehensive audit logging
     * 
     * @param request the authentication request with customer identifier and brand
     * @param dnis the DNIS code for call-specific authentication rules
     * @param sessionSsnList list of SSNs from session for enhanced customer lookup
     * @param dnisConfig the DNIS configuration object with call-specific rules
     * @return AuthenticationResponse with initial authentication steps or error status
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
     * Handles the continuation of an existing authentication attempt with token processing.
     * 
     * This method processes authentication attempts where a customer is providing tokens
     * in response to previous authentication requests, maintaining session continuity.
     * 
     * CONTINUING ATTEMPT FLOW:
     * 1. Context retrieval and validation using attempt ID
     * 2. Brand consistency verification across session
     * 3. Customer profile re-validation for security
     * 4. Token processing with DNIS-specific rules
     * 5. Context state updates and persistence
     * 6. Response building with next steps or completion
     * 
     * CONTEXT VALIDATION:
     * - Attempt ID must exist in persistent storage
     * - Brand must match original authentication request
     * - Customer profile must still be accessible
     * - Session integrity checks for security
     * 
     * TOKEN PROCESSING:
     * - Validates provided tokens against customer data
     * - Applies DNIS-specific validation rules
     * - Updates authentication state and progress
     * - Handles both successful and failed validations
     * 
     * STATE MANAGEMENT:
     * - Updates attempt counts and token states
     * - Tracks authentication progress
     * - Manages session lifecycle
     * - Applies business rules for completion
     * 
     * ERROR SCENARIOS:
     * - Context not found (expired or invalid attempt ID)
     * - Brand mismatch across session
     * - Customer profile unavailable
     * - Returns appropriate error responses with brand messaging
     * 
     * SECURITY FEATURES:
     * - Session integrity validation
     * - Brand consistency enforcement
     * - Comprehensive state tracking
     * - DNIS-based validation rules
     * 
     * @param request the authentication request with tokens and attempt continuation data
     * @param dnis the DNIS code for call-specific validation rules
     * @param sessionSsnList list of SSNs from session (used for context validation)
     * @param dnisConfig the DNIS configuration object with call-specific rules
     * @return AuthenticationResponse with next steps, completion status, or error information
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
