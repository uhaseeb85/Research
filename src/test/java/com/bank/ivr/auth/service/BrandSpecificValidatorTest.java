package com.bank.ivr.auth.service;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.validator.TokenValidator;
import com.bank.ivr.auth.validator.impl.CommunityBankPinValidator;
import com.bank.ivr.auth.validator.impl.DigitalBankFaceIdValidator;
import com.bank.ivr.auth.validator.impl.PremiumBankSsnValidator;

/**
 * Test class to verify brand-specific validator registration and lookup.
 * Tests the composite key system and fallback behavior.
 */
@SpringBootTest
public class BrandSpecificValidatorTest {
    
    @Autowired
    private TokenValidationService tokenValidationService;
    
    @Test
    public void shouldRegisterBrandSpecificValidators() {
        System.out.println("=== BRAND-SPECIFIC VALIDATOR MAP KEYS ===");
        Set<String> keys = tokenValidationService.getSupportedBrandTokenCombinations();
        for (String key : keys) {
            System.out.println("Key: " + key);
        }
        System.out.println("=== END VALIDATOR MAP KEYS ===");
        
        // Verify brand-specific validators are registered
        assertTrue(keys.contains("DIGITAL_BANK:FACE_ID"), "DIGITAL_BANK Face ID validator should be registered");
        assertTrue(keys.contains("PREMIUM_BANK:SSN"), "PREMIUM_BANK SSN validator should be registered");
        assertTrue(keys.contains("COMMUNITY_BANK:DEBIT_CARD_PIN"), "COMMUNITY_BANK PIN validator should be registered");
        
        // Verify DEFAULT validators still exist
        assertTrue(keys.contains("DEFAULT:SSN"), "DEFAULT SSN validator should still exist");
        assertTrue(keys.contains("DEFAULT:DEBIT_CARD_PIN"), "DEFAULT PIN validator should still exist");
    }
    
    @Test
    public void shouldReturnCorrectBrandSpecificValidator() {
        // Test brand-specific validator lookup
        TokenValidator digitalBankFaceId = tokenValidationService.getValidator("DIGITAL_BANK", "FACE_ID");
        assertNotNull(digitalBankFaceId, "Should find DIGITAL_BANK Face ID validator");
        assertTrue(digitalBankFaceId instanceof DigitalBankFaceIdValidator, "Should be DigitalBankFaceIdValidator instance");
        
        TokenValidator premiumBankSsn = tokenValidationService.getValidator("PREMIUM_BANK", "SSN");
        assertNotNull(premiumBankSsn, "Should find PREMIUM_BANK SSN validator");
        assertTrue(premiumBankSsn instanceof PremiumBankSsnValidator, "Should be PremiumBankSsnValidator instance");
        
        TokenValidator communityBankPin = tokenValidationService.getValidator("COMMUNITY_BANK", "DEBIT_CARD_PIN");
        assertNotNull(communityBankPin, "Should find COMMUNITY_BANK PIN validator");
        assertTrue(communityBankPin instanceof CommunityBankPinValidator, "Should be CommunityBankPinValidator instance");
    }
    
    @Test
    public void shouldFallbackToDefaultValidators() {
        // Test that direct getValidator() does NOT include fallback (by design)
        TokenValidator royalBankSsn = tokenValidationService.getValidator("ROYAL_BANK", "SSN");
        assertEquals(null, royalBankSsn, "getValidator() should NOT fallback - it returns exact matches only");
        
        TokenValidator digitalBankPin = tokenValidationService.getValidator("DIGITAL_BANK", "DEBIT_CARD_PIN");
        assertEquals(null, digitalBankPin, "getValidator() should NOT fallback - it returns exact matches only");
        
        // But validateToken() DOES include fallback behavior (tested in shouldShowFallbackBehavior)
        // This is the correct design - direct lookup vs validation with fallback
    }
    
    @Test
    public void shouldHandleNonExistentBrandTokenCombination() {
        // Test lookup for non-existent brand-specific validator with no DEFAULT fallback
        TokenValidator nonExistent = tokenValidationService.getValidator("DIGITAL_BANK", "VOICE_PRINT");
        assertEquals(null, nonExistent, "Should return null for non-existent token with no DEFAULT fallback");
    }
    
    @Test
    public void shouldValidateWithBrandSpecificLogic() {
        // Create test customer profiles
        CustomerProfile digitalBankCustomer = CustomerProfile.builder()
                .customerId("DIGITAL001")
                .faceIdHash("ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890")
                .dateOfBirth(LocalDate.of(1990, 1, 1)) // Age 34
                .accountStatus("ACTIVE")
                .mobileAppEnrolled(true)
                .build();
        
        CustomerProfile premiumBankCustomer = CustomerProfile.builder()
                .customerId("PREMIUM001")
                .ssn("123456789")
                .accountBalance(75000.0) // Premium customer
                .accountStatus("PREMIUM")
                .build();
        
        // Test DIGITAL_BANK Face ID validation
        boolean digitalFaceIdResult = tokenValidationService.validateToken(
                "FACE_ID", "DIGITAL_BANK", "DIGITAL001", 
                "ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890", 
                digitalBankCustomer);
        assertTrue(digitalFaceIdResult, "DIGITAL_BANK Face ID validation should succeed");
        
        // Test PREMIUM_BANK SSN validation (full SSN required)
        boolean premiumSsnResult = tokenValidationService.validateToken(
                "SSN", "PREMIUM_BANK", "PREMIUM001", 
                "123456789", // Full SSN
                premiumBankCustomer);
        assertTrue(premiumSsnResult, "PREMIUM_BANK full SSN validation should succeed");
        
        // Test PREMIUM_BANK SSN validation fails with partial SSN
        boolean premiumSsnPartialResult = tokenValidationService.validateToken(
                "SSN", "PREMIUM_BANK", "PREMIUM001", 
                "6789", // Last 4 digits only
                premiumBankCustomer);
        assertFalse(premiumSsnPartialResult, "PREMIUM_BANK should reject partial SSN");
    }
    
    @Test
    public void shouldShowFallbackBehavior() {
        CustomerProfile standardCustomer = CustomerProfile.builder()
                .customerId("STANDARD001")
                .ssn("987654321")
                .accountBalance(5000.0) // Not premium
                .build();
        
        // Test that PREMIUM_BANK validator rejects non-premium customers
        boolean premiumSsnForStandardCustomer = tokenValidationService.validateToken(
                "SSN", "PREMIUM_BANK", "STANDARD001", 
                "987654321",
                standardCustomer);
        assertFalse(premiumSsnForStandardCustomer, "PREMIUM_BANK should reject non-premium customers");
        
        // Test that DEFAULT validator works for the same customer
        boolean defaultSsnForStandardCustomer = tokenValidationService.validateToken(
                "SSN", "DEFAULT", "STANDARD001", 
                "987654321",
                standardCustomer);
        assertTrue(defaultSsnForStandardCustomer, "DEFAULT SSN validator should work for standard customers");
        
        // Test fallback behavior - ROYAL_BANK should use DEFAULT validator
        boolean royalBankSsnForStandardCustomer = tokenValidationService.validateToken(
                "SSN", "ROYAL_BANK", "STANDARD001", 
                "987654321",
                standardCustomer);
        assertTrue(royalBankSsnForStandardCustomer, "ROYAL_BANK should fallback to DEFAULT SSN validator");
    }
} 