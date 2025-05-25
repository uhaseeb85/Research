package com.bank.ivr.auth.service;

import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.validator.TokenValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Test class for TokenValidationService focusing on brand-aware validation
 * and the enforcement of the "one validator per token per brand" rule.
 */
class TokenValidationServiceTest {
    
    @Mock
    private TokenValidator defaultSsnValidator;
    
    @Mock
    private TokenValidator communityBankSsnValidator;
    
    @Mock
    private TokenValidator duplicateCommunityBankSsnValidator;
    
    private CustomerProfile testProfile;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Setup test customer profile
        testProfile = CustomerProfile.builder()
                .customerId("test123")
                .ssn("123456789")
                .build();
        
        // Setup mock validators
        when(defaultSsnValidator.getTokenName()).thenReturn("SSN");
        when(defaultSsnValidator.getBrand()).thenReturn("DEFAULT");
        when(defaultSsnValidator.getPriority()).thenReturn(100);
        
        when(communityBankSsnValidator.getTokenName()).thenReturn("SSN");
        when(communityBankSsnValidator.getBrand()).thenReturn("COMMUNITY_BANK");
        when(communityBankSsnValidator.getPriority()).thenReturn(100);
        
        when(duplicateCommunityBankSsnValidator.getTokenName()).thenReturn("SSN");
        when(duplicateCommunityBankSsnValidator.getBrand()).thenReturn("COMMUNITY_BANK");
        when(duplicateCommunityBankSsnValidator.getPriority()).thenReturn(90);
    }
    
    @Test
    void shouldAllowDifferentValidatorsForSameTokenAcrossDifferentBrands() {
        // Given: Validators for the same token but different brands
        List<TokenValidator> validators = List.of(defaultSsnValidator, communityBankSsnValidator);
        
        // When: Creating TokenValidationService
        TokenValidationService service = new TokenValidationService(validators);
        
        // Then: Should succeed without throwing exception
        assertTrue(service.hasValidator("DEFAULT", "SSN"));
        assertTrue(service.hasValidator("COMMUNITY_BANK", "SSN"));
        assertFalse(service.hasValidator("PREMIUM_BANK", "SSN"));
    }
    
    @Test
    void shouldThrowExceptionWhenMultipleValidatorsForSameTokenAndBrand() {
        // Given: Two validators for the same token and brand
        List<TokenValidator> validators = List.of(communityBankSsnValidator, duplicateCommunityBankSsnValidator);
        
        // When & Then: Should throw IllegalStateException
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            new TokenValidationService(validators);
        });
        
        assertTrue(exception.getMessage().contains("Multiple validators found for brand 'COMMUNITY_BANK' and token 'SSN'"));
        assertTrue(exception.getMessage().contains("Only one validator per token per brand is allowed"));
    }
    
    @Test
    void shouldReturnCorrectValidatorForBrandTokenCombination() {
        // Given: Validators for different brands
        List<TokenValidator> validators = List.of(defaultSsnValidator, communityBankSsnValidator);
        TokenValidationService service = new TokenValidationService(validators);
        
        // When & Then: Should return correct validators for each brand
        assertEquals(defaultSsnValidator, service.getValidator("DEFAULT", "SSN"));
        assertEquals(communityBankSsnValidator, service.getValidator("COMMUNITY_BANK", "SSN"));
        assertNull(service.getValidator("PREMIUM_BANK", "SSN"));
    }
    
    @Test
    void shouldReturnBrandSpecificTokenNames() {
        // Given: Validators for different brands
        List<TokenValidator> validators = List.of(defaultSsnValidator, communityBankSsnValidator);
        TokenValidationService service = new TokenValidationService(validators);
        
        // When & Then: Should return token names for specific brands
        assertTrue(service.getSupportedTokenNamesForBrand("DEFAULT").contains("SSN"));
        assertTrue(service.getSupportedTokenNamesForBrand("COMMUNITY_BANK").contains("SSN"));
        assertTrue(service.getSupportedTokenNamesForBrand("PREMIUM_BANK").isEmpty());
    }
    
    @Test
    void shouldValidateTokenWithBrandAwareness() {
        // Given: Setup validation behavior
        when(defaultSsnValidator.validate("customer1", "123456789", testProfile)).thenReturn(true);
        when(communityBankSsnValidator.validate("customer1", "123456789", testProfile)).thenReturn(false);
        
        List<TokenValidator> validators = List.of(defaultSsnValidator, communityBankSsnValidator);
        TokenValidationService service = new TokenValidationService(validators);
        
        // When & Then: Different brands should use different validators
        assertTrue(service.validateToken("SSN", "DEFAULT", "customer1", "123456789", testProfile));
        assertFalse(service.validateToken("SSN", "COMMUNITY_BANK", "customer1", "123456789", testProfile));
    }
    
    @Test
    void shouldReturnBrandTokenCombinations() {
        // Given: Validators for different brands
        List<TokenValidator> validators = List.of(defaultSsnValidator, communityBankSsnValidator);
        TokenValidationService service = new TokenValidationService(validators);
        
        // When & Then: Should return all brand+token combinations
        var combinations = service.getSupportedBrandTokenCombinations();
        assertTrue(combinations.contains("DEFAULT:SSN"));
        assertTrue(combinations.contains("COMMUNITY_BANK:SSN"));
        assertEquals(2, combinations.size());
    }
} 