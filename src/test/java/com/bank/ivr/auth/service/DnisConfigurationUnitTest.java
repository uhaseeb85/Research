package com.bank.ivr.auth.service;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.bank.ivr.auth.model.domain.CustomerProfile;
import com.bank.ivr.auth.model.domain.DnisConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("DNIS Configuration Unit Tests")
public class DnisConfigurationUnitTest {

    @Mock
    private ResourceLoader resourceLoader;
    
    @Mock
    private Resource resource;
    
    @Mock
    private BrandAuthConfigurationService brandConfigService;
    
    private DnisConfigurationService dnisConfigService;
    private EligibilityService eligibilityService;
    
    private CustomerProfile testCustomer;
    private DnisConfiguration restrictiveDnisConfig;
    private DnisConfiguration permissiveDnisConfig;
    
    @BeforeEach
    void setUp() {
        // Mock the resource loader to return a non-null resource that doesn't exist
        // This will trigger the fallback to default configurations
        // Use anyString() because @Value annotation may be null in test context
        when(resourceLoader.getResource(any())).thenReturn(resource);
        when(resource.exists()).thenReturn(false);
        
        // Initialize services
        dnisConfigService = new DnisConfigurationService(resourceLoader, new ObjectMapper());
        // Manually trigger the @PostConstruct method since it won't be called in unit tests
        dnisConfigService.loadDnisConfigurations();
        
        eligibilityService = new EligibilityService(Arrays.asList(), brandConfigService);
        
        // Set up test customer
        testCustomer = CustomerProfile.builder()
            .customerId("CUST001")
            .phoneNumber("+1234567890")
            .accountNumber("ACC001")
            .ssn("123456789")
            .hashedPin("$2a$10$N9qo8uLOickgx2ZMRZoMye")
            .dateOfBirth(java.time.LocalDate.of(1985, 6, 15))
            .motherMaidenName("Smith")
            .fullName("John Doe")
            .email("john.doe@email.com")
            .accountStatus("ACTIVE")
            .build();
        
        // Set up DNIS configurations
        setupDnisConfigurations();
    }
    
    private void setupDnisConfigurations() {
        // Restrictive DNIS - only allows SSN
        restrictiveDnisConfig = new DnisConfiguration(
            "18001111111",
            "Restrictive DNIS - SSN only",
            true,  // allowSsnAuthentication
            false, // allowPinAuthentication
            false, // allowDateOfBirthAuthentication
            false, // allowMotherMaidenNameAuthentication
            false, // allowAccountNumberAuthentication
            false, // requireMultiFactorAuth
            false, // allowTrustLevelBypass
            true,  // enablePhoneMatchValidation
            false, // allowAlternativeTokens
            true,  // enableStrictValidation
            true,  // allowRetryOnFailure
            true,  // enableAuditLogging
            3,     // maxAuthenticationAttempts
            15     // sessionTimeoutMinutes
        );
            
        // Permissive DNIS - allows all tokens
        permissiveDnisConfig = new DnisConfiguration(
            "18002222222",
            "Permissive DNIS - all tokens allowed",
            true,  // allowSsnAuthentication
            true,  // allowPinAuthentication
            true,  // allowDateOfBirthAuthentication
            true,  // allowMotherMaidenNameAuthentication
            true,  // allowAccountNumberAuthentication
            false, // requireMultiFactorAuth
            true,  // allowTrustLevelBypass
            true,  // enablePhoneMatchValidation
            true,  // allowAlternativeTokens
            false, // enableStrictValidation
            true,  // allowRetryOnFailure
            true,  // enableAuditLogging
            5,     // maxAuthenticationAttempts
            20     // sessionTimeoutMinutes
        );
    }

    @Nested
    @DisplayName("DNIS Configuration Service Tests")
    class DnisConfigurationServiceTests {
        
        @Test
        @DisplayName("Should load default DNIS configuration for unknown DNIS")
        void shouldLoadDefaultDnisConfiguration() {
            DnisConfiguration config = dnisConfigService.getDnisConfiguration("UNKNOWN_DNIS");
            
            assertNotNull(config);
            assertEquals("DEFAULT", config.getDnis());
            assertTrue(config.isAllowSsnAuthentication());
            assertTrue(config.isAllowPinAuthentication());
            assertTrue(config.isAllowDateOfBirthAuthentication());
        }
        
        @Test
        @DisplayName("Should load specific DNIS configuration")
        void shouldLoadSpecificDnisConfiguration() {
            DnisConfiguration config = dnisConfigService.getDnisConfiguration("18001234567");
            
            assertNotNull(config);
            assertEquals("18001234567", config.getDnis());
            assertEquals("Premium banking services - high security", config.getDescription());
            assertTrue(config.isRequireMultiFactorAuth());
            assertEquals(2, config.getMaxAuthenticationAttempts());
        }
        
        @Test
        @DisplayName("Should handle null DNIS input")
        void shouldHandleNullDnis() {
            DnisConfiguration config = dnisConfigService.getDnisConfiguration(null);
            
            assertNotNull(config);
            assertEquals("DEFAULT", config.getDnis());
        }
        
        @Test
        @DisplayName("Should handle empty DNIS string")
        void shouldHandleEmptyDnisString() {
            DnisConfiguration config = dnisConfigService.getDnisConfiguration("");
            
            assertNotNull(config);
            assertEquals("DEFAULT", config.getDnis());
        }
        
        @Test
        @DisplayName("Should handle malformed DNIS")
        void shouldHandleMalformedDnis() {
            DnisConfiguration config = dnisConfigService.getDnisConfiguration("invalid-dnis-123");
            
            assertNotNull(config);
            assertEquals("DEFAULT", config.getDnis());
        }
    }

    @Nested
    @DisplayName("DNIS Configuration Properties Tests")
    class DnisConfigurationPropertiesTests {
        
        @Test
        @DisplayName("Restrictive DNIS should have correct properties")
        void restrictiveDnisShouldHaveCorrectProperties() {
            assertEquals("18001111111", restrictiveDnisConfig.getDnis());
            assertEquals("Restrictive DNIS - SSN only", restrictiveDnisConfig.getDescription());
            
            // Token allowances
            assertTrue(restrictiveDnisConfig.isAllowSsnAuthentication());
            assertFalse(restrictiveDnisConfig.isAllowPinAuthentication());
            assertFalse(restrictiveDnisConfig.isAllowDateOfBirthAuthentication());
            assertFalse(restrictiveDnisConfig.isAllowAccountNumberAuthentication());
            assertFalse(restrictiveDnisConfig.isAllowMotherMaidenNameAuthentication());
            
            // Other settings
            assertFalse(restrictiveDnisConfig.isRequireMultiFactorAuth());
            assertTrue(restrictiveDnisConfig.isEnableStrictValidation());
            assertTrue(restrictiveDnisConfig.isAllowRetryOnFailure());
            assertEquals(3, restrictiveDnisConfig.getMaxAuthenticationAttempts());
        }
        
        @Test
        @DisplayName("Permissive DNIS should have correct properties")
        void permissiveDnisShouldHaveCorrectProperties() {
            assertEquals("18002222222", permissiveDnisConfig.getDnis());
            assertEquals("Permissive DNIS - all tokens allowed", permissiveDnisConfig.getDescription());
            
            // Token allowances
            assertTrue(permissiveDnisConfig.isAllowSsnAuthentication());
            assertTrue(permissiveDnisConfig.isAllowPinAuthentication());
            assertTrue(permissiveDnisConfig.isAllowDateOfBirthAuthentication());
            assertTrue(permissiveDnisConfig.isAllowAccountNumberAuthentication());
            assertTrue(permissiveDnisConfig.isAllowMotherMaidenNameAuthentication());
            
            // Other settings
            assertFalse(permissiveDnisConfig.isRequireMultiFactorAuth());
            assertFalse(permissiveDnisConfig.isEnableStrictValidation());
            assertTrue(permissiveDnisConfig.isAllowRetryOnFailure());
            assertEquals(5, permissiveDnisConfig.getMaxAuthenticationAttempts());
        }
    }

    @Nested
    @DisplayName("DNIS Service Utility Methods Tests")
    class DnisServiceUtilityMethodsTests {
        
        @Test
        @DisplayName("Should correctly check token allowance for DNIS")
        void shouldCorrectlyCheckTokenAllowanceForDnis() {
            // Test with restrictive DNIS (falls back to DEFAULT since 18001111111 doesn't exist)
            assertTrue(dnisConfigService.isTokenAllowedForDnis("18001111111", "SSN"));
            assertTrue(dnisConfigService.isTokenAllowedForDnis("18001111111", "DEBIT_CARD_PIN"));
            assertTrue(dnisConfigService.isTokenAllowedForDnis("18001111111", "DATE_OF_BIRTH"));
            
            // Test with permissive DNIS  
            assertTrue(dnisConfigService.isTokenAllowedForDnis("18002222222", "SSN"));
            assertTrue(dnisConfigService.isTokenAllowedForDnis("18002222222", "DEBIT_CARD_PIN"));
            assertTrue(dnisConfigService.isTokenAllowedForDnis("18002222222", "DATE_OF_BIRTH"));
        }
        
        @Test
        @DisplayName("Should correctly check multi-factor auth requirement")
        void shouldCorrectlyCheckMultiFactorAuthRequirement() {
            assertFalse(dnisConfigService.isMultiFactorAuthRequired("18001111111"));
            assertFalse(dnisConfigService.isMultiFactorAuthRequired("18002222222"));
            
            // Test with a DNIS that requires multi-factor auth (from config file)
            assertTrue(dnisConfigService.isMultiFactorAuthRequired("18001234567"));
        }
        
        @Test
        @DisplayName("Should correctly get max authentication attempts")
        void shouldCorrectlyGetMaxAuthenticationAttempts() {
            assertEquals(3, dnisConfigService.getMaxAuthenticationAttempts("18001111111"));
            assertEquals(3, dnisConfigService.getMaxAuthenticationAttempts("18002222222"));
            
            // Test with a DNIS from config file
            assertEquals(2, dnisConfigService.getMaxAuthenticationAttempts("18001234567"));
        }
        
        @Test
        @DisplayName("Should correctly check alternative tokens allowance")
        void shouldCorrectlyCheckAlternativeTokensAllowance() {
            assertTrue(dnisConfigService.areAlternativeTokensAllowed("18001111111"));
            assertTrue(dnisConfigService.areAlternativeTokensAllowed("18002222222"));
            
            // Test with a DNIS from config file
            assertFalse(dnisConfigService.areAlternativeTokensAllowed("18001234567"));
        }
    }

    @Nested
    @DisplayName("DNIS Configuration Edge Cases")
    class DnisConfigurationEdgeCasesTests {
        
        @Test
        @DisplayName("Should handle unknown token types gracefully")
        void shouldHandleUnknownTokenTypesGracefully() {
            // Unknown tokens should be allowed by default
            assertTrue(dnisConfigService.isTokenAllowedForDnis("18001111111", "UNKNOWN_TOKEN"));
            assertTrue(dnisConfigService.isTokenAllowedForDnis("18002222222", "UNKNOWN_TOKEN"));
        }
        
        @Test
        @DisplayName("Should handle case insensitive token names")
        void shouldHandleCaseInsensitiveTokenNames() {
            assertTrue(dnisConfigService.isTokenAllowedForDnis("18002222222", "ssn"));
            assertTrue(dnisConfigService.isTokenAllowedForDnis("18002222222", "SSN"));
            assertTrue(dnisConfigService.isTokenAllowedForDnis("18002222222", "pin"));
            assertTrue(dnisConfigService.isTokenAllowedForDnis("18002222222", "DEBIT_CARD_PIN"));
        }
        
        @Test
        @DisplayName("Should clean DNIS numbers correctly")
        void shouldCleanDnisNumbersCorrectly() {
            // DNIS with formatting should be cleaned
            DnisConfiguration config1 = dnisConfigService.getDnisConfiguration("1-800-123-4567");
            DnisConfiguration config2 = dnisConfigService.getDnisConfiguration("18001234567");
            
            // Both should return the same configuration
            assertEquals(config1.getDnis(), config2.getDnis());
            assertEquals(config1.getDescription(), config2.getDescription());
        }
    }

    @Nested
    @DisplayName("DNIS Configuration Consistency Tests")
    class DnisConfigurationConsistencyTests {
        
        @Test
        @DisplayName("Default DNIS should have sensible defaults")
        void defaultDnisShouldHaveSensibleDefaults() {
            DnisConfiguration defaultConfig = dnisConfigService.getDnisConfiguration("DEFAULT");
            
            assertNotNull(defaultConfig);
            assertEquals("DEFAULT", defaultConfig.getDnis());
            
            // Should allow most authentication methods
            assertTrue(defaultConfig.isAllowSsnAuthentication());
            assertTrue(defaultConfig.isAllowPinAuthentication());
            assertTrue(defaultConfig.isAllowDateOfBirthAuthentication());
            
            // Should have reasonable limits
            assertTrue(defaultConfig.getMaxAuthenticationAttempts() > 0);
            assertTrue(defaultConfig.getSessionTimeoutMinutes() > 0);
        }
        
        @Test
        @DisplayName("All DNIS configurations should be internally consistent")
        void allDnisConfigurationsShouldBeInternallyConsistent() {
            List<String> testDnisNumbers = Arrays.asList(
                "DEFAULT",
                "18001234567",
                "18009876543",
                "18005551234",
                "18007778888"
            );
            
            for (String dnis : testDnisNumbers) {
                DnisConfiguration config = dnisConfigService.getDnisConfiguration(dnis);
                assertNotNull(config, "Configuration should not be null for DNIS: " + dnis);
                assertNotNull(config.getDnis(), "DNIS should not be null");
                assertNotNull(config.getDescription(), "Description should not be null");
                assertTrue(config.getMaxAuthenticationAttempts() > 0, "Max attempts should be positive");
                assertTrue(config.getSessionTimeoutMinutes() > 0, "Session timeout should be positive");
            }
        }
    }
} 