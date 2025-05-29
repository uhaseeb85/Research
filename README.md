# Bank IVR Authentication System - Complete Guide

A comprehensive, enterprise-grade multi-factor authentication system for bank customers via IVR (Interactive Voice Response) systems, built with Spring Boot 3.1.0. Features brand-aware authentication, DNIS support, advanced retry management, trust-based authentication, and comprehensive security controls.

## 🚀 Overview

This system provides secure, stateful authentication for bank customers through IVR systems using dynamic question flows, token validation, and brand-specific configurations. The system is designed to be completely self-contained with **no external database dependencies** - all data is stored in-memory using Java collections.

## ✨ Key Features

### Core Authentication
- **Brand-Aware Authentication**: Different brands with distinct authentication flows, token requirements, and customer messages
- **DNIS Configuration Support**: Phone number-based routing with configurable authentication rules per DNIS
- **Multi-Factor Authentication**: Support for multiple authentication tokens (SSN, Debit Card PIN, Date of Birth, Mother's Maiden Name, Account Number)
- **Dynamic Question Flows**: Intelligent token selection based on customer eligibility, priority, and brand rules
- **Trust Level Integration**: Phone match validation and trust-based authentication decisions
- **Context Integration**: Session-based data retrieval with DNIS and SSN from previous API calls

### Advanced Security & Retry Management
- **Token-Level Retry Strategies**: Configurable retry patterns (immediate, fixed delay, exponential backoff, linear backoff)
- **Brand-Level Retry Policies**: Global retry policies controlling overall retry behavior across tokens
- **Progressive Lockout**: Increasing lockout periods after repeated failures with escalation policies
- **Cross-Token Delay**: Failed attempts on one token introduce delays for subsequent attempts
- **Suspicious Activity Detection**: Rapid successive failures trigger enhanced security measures
- **Time-Based Retry Windows**: Automatic reset of retry counters after configurable periods
- **Post-Validation Rules**: Additional security checks after successful token validation
- **Failed Token Tracking**: Smart re-asking logic prevents repeated requests for failed tokens

### Enterprise Features
- **In-Memory Storage**: No database required - all data stored in Java collections with automatic initialization
- **Stateful Session Management**: Session management with automatic expiration and context preservation
- **Comprehensive Validation**: Input validation, attempt tracking, and security measures
- **Production Ready**: Proper error handling, logging, monitoring, and health checks
- **Rule-Based Architecture**: Extensible system with eligibility rules and token selection rules
- **Trust-Based Authentication**: Advanced conditional logic based on trust levels and phone matching

## 🏗️ Architecture

### Core Components

1. **Model Layer**
   - Request/Response models with comprehensive validation
   - Domain models (CustomerProfile, AuthenticationContext, AuthTokenDefinition, DnisConfiguration)
   - Builder patterns for complex object creation
   - Trust level and phone match status tracking

2. **Repository Layer**
   - In-memory implementations using Java collections
   - JSON-based data sources for session context and customer data
   - No database dependencies
   - Automatic data initialization with test customers and DNIS configurations

3. **Service Layer**
   - `AuthenticationOrchestrator`: Core authentication orchestration
   - `TokenValidationService`: Token validation management with brand-specific validators
   - `BrandAuthConfigurationService`: Brand-specific configuration management
   - `DnisConfigurationService`: DNIS-based routing and configuration
   - `TokenRetryManagementService`: Advanced retry logic and lockout management
   - `PostValidationRuleService`: Additional security rules after token validation
   - `BrandFailurePolicyService`: Brand-specific failure handling policies
   - `SessionContextService`: Session context data management
   - `TokenSelectionService`: Rule-based token selection logic

4. **Rule System**
   - `EligibilityRule`: Determines which tokens are available to customers
   - `TokenSelectionRule`: Decides which token to ask for based on business logic
   - `PostValidationRule`: Additional security checks after successful validation
   - Priority-based rule execution with brand-specific overrides

5. **Controller Layer**
   - REST API with comprehensive error handling and brand context
   - Health check endpoint with DNIS support status
   - DNIS configuration endpoints
   - Brand-specific authentication method endpoints

6. **Security**
   - BCrypt password hashing for sensitive data
   - Masked logging for PII protection
   - Secure token validation with attempt limiting
   - Trust level-based authentication decisions

## 📡 API Endpoints

### Authentication Endpoint
```
POST /api/v1/auth/customer
Content-Type: application/json
```

#### New Authentication Request with DNIS
```json
{
  "sessionId": "session-123",
  "customerIdentifier": {
    "type": "PHONE_NUMBER",
    "value": "+1234567890"
  },
  "brand": "PREMIUM_BANK",
  "trustLevelInfo": {
    "trustLevel": "GREEN",
    "phoneMatchStatus": "SINGLE_MATCH",
    "matchedSsnCount": 1
  }
}
```

Note: `dnis` and `sessionSsn` are now retrieved from session context automatically using the `sessionId`.

#### Continuing Authentication Request
```json
{
  "sessionId": "session-123",
  "customerIdentifier": {
    "type": "PHONE_NUMBER", 
    "value": "+1234567890"
  },
  "attemptId": "uuid-from-previous-response",
  "providedTokens": [
    {
      "tokenName": "SSN",
      "tokenValue": "123456789"
    }
  ],
  "brand": "PREMIUM_BANK"
}
```

#### Enhanced Response Format
```json
{
  "attemptId": "attempt-123",
  "status": "PENDING_MORE_TOKENS",
  "message": "Please also provide your date of birth for additional verification.",
  "primaryTokenToAsk": {
    "name": "DATE_OF_BIRTH",
    "description": "Date of Birth",
    "inputFormatRegex": "^\\d{2}/\\d{2}/\\d{4}$"
  },
  "secondaryTokensAccepted": [
    {
      "name": "MOTHER_MAIDEN_NAME",
      "description": "Mother's Maiden Name"
    }
  ],
  "remainingAttempts": 2,
  "authenticatedTokens": ["SSN"],
  "failedTokens": ["DEBIT_CARD_PIN"]
}
```

### Brand-Specific Endpoints
```
GET /api/v1/auth/methods/{brand}    # Get authentication methods for a brand
GET /api/v1/auth/brands             # Get all supported brands
```

### DNIS Configuration Endpoints
```
GET /api/v1/auth/dnis/{dnis}        # Get DNIS configuration
GET /api/v1/auth/dnis               # Get all DNIS configurations
```

### Health Check
```
GET /api/v1/auth/health             # Health check with DNIS support status
```

## 🏢 Brand Configuration

The system supports multiple brands with distinct configurations:

### Supported Brands
- **PREMIUM_BANK**: High-security multi-factor authentication
- **COMMUNITY_BANK**: Simplified single-factor authentication  
- **ROYAL_BANK**: Enhanced security with trust-based authentication and complex conditional rules

### Brand-Specific Features
- Custom token definitions and priorities
- Maximum overall attempt limits
- Concurrent token authentication settings
- Brand-specific customer messages
- Failure handling policies
- Retry strategies and lockout policies
- Trust-level based authentication rules

## 📞 DNIS Support

### Pre-configured DNIS Numbers
- **18001234567**: Premium banking services (high security)
- **18009876543**: General customer service (standard security)
- **18005551234**: Mobile banking services (medium security)
- **18007778888**: Business banking services (enhanced security)
- **DEFAULT**: Default configuration for unspecified numbers

### DNIS Configuration Features
- Authentication method restrictions per DNIS
- Multi-factor authentication requirements
- Trust level bypass settings
- Phone match validation controls
- Session timeout configurations
- Maximum authentication attempts per DNIS

### DNIS Configuration Structure
```json
{
  "dnis": "18001234567",
  "description": "Premium banking services - high security DNIS",
  "allowSsnAuthentication": true,
  "allowPinAuthentication": true,
  "allowDateOfBirthAuthentication": true,
  "allowMotherMaidenNameAuthentication": false,
  "allowAccountNumberAuthentication": true,
  "requireMultiFactorAuth": true,
  "allowTrustLevelBypass": false,
  "enablePhoneMatchValidation": true,
  "maxAuthenticationAttempts": 2,
  "sessionTimeoutMinutes": 10
}
```

## 🔐 Trust-Based Authentication

### Trust Level System
The system supports sophisticated trust-based authentication with conditional logic:

#### Trust Levels
- **GREEN (High Trust)**: Simplified authentication, single factor may suffice
- **RED (Low Trust)**: Enhanced verification required, full SSN and additional tokens

#### Phone Match Status
- **NOT_MATCHED**: Phone not matched with any SSN
- **SINGLE_MATCH**: Phone matched with exactly one SSN
- **MULTIPLE_MATCHES**: Phone matched with multiple SSNs

#### Trust Level Example Scenarios

**Royal Bank Trust Matrix:**
| Trust Level | Phone Match Status | Initial Token | Failure Escalation |
|-------------|-------------------|---------------|-------------------|
| GREEN | NOT_MATCHED | SSN_LAST_4 | → SSN_FULL |
| GREEN | SINGLE_MATCH | SSN_LAST_4 | → SSN_FULL |
| GREEN | MULTIPLE_MATCHES | SSN_LAST_4 | → SSN_FULL |
| RED | NOT_MATCHED | SSN_FULL | → FAIL |
| RED | SINGLE_MATCH | SSN_FULL | → FAIL |
| RED | MULTIPLE_MATCHES | SSN_FULL | → FAIL |

## 🔄 Rule-Based System

### Two Rule Types

#### 1. Eligibility Rules
- **Purpose**: Determine which authentication methods are available to a customer
- **When**: Called during initial context creation (once per session)
- **Examples**: Check if customer has SSN, PIN, or biometric data on record

#### 2. Token Selection Rules
- **Purpose**: Decide the specific token to request based on complex business logic
- **When**: Called during response building (every time we need to ask for a token)
- **Examples**: Trust-based selection, brand-specific preferences, failure escalation

### Rule Execution Sequence

#### Phase 1: Session Initialization
1. Customer calls in
2. EligibilityService determines available tokens
3. Available tokens stored in AuthenticationContext

#### Phase 2: Token Selection (Every Request)
1. AuthenticationResponseService builds response
2. TokenSelectionService determines next token
3. Rules evaluated BY PRIORITY (highest first)
4. First applicable rule wins

#### Phase 3: Failure Handling
1. Token validation fails
2. Token selection rules evaluated for escalation
3. Fallback to brand failure policies if no rules handle it

### Adding New Rules

#### Example Eligibility Rule
```java
@Component
public class BiometricEligibilityRule implements EligibilityRule {
    @Override
    public boolean isEligible(CustomerProfile customerProfile, String brand) {
        return customerProfile.getBiometricHash() != null 
               && "ACTIVE".equals(customerProfile.getAccountStatus());
    }
    
    @Override
    public String getTokenName() {
        return "BIOMETRIC";
    }
    
    @Override
    public int getPriority() {
        return 150;
    }
}
```

#### Example Token Selection Rule
```java
@Component
public class TechBankMobileFirstRule implements TokenSelectionRule {
    @Override
    public String determineNextToken(AuthenticationContext context, CustomerProfile customerProfile) {
        if (context.getEligibleTokens().contains("MOBILE_PIN")) {
            return "MOBILE_PIN";
        }
        return "SSN"; // Fallback
    }
    
    @Override
    public String getBrand() {
        return "TECH_BANK";
    }
    
    @Override
    public int getPriority() {
        return 200;
    }
}
```

## 🛡️ Post-Validation Rules

After successful token validation, additional security checks can be performed:

### When Post-Validation Rules Trigger
- **Low Trust Scenarios**: When external trust assessment indicates suspicious activity
- **Phone Number Issues**: When phone numbers don't match or match multiple customers
- **High-Value Customers**: When additional security is warranted for premium accounts
- **Risk Indicators**: When customer profile shows recent changes or suspicious activity

### Example Post-Validation Scenarios

#### Low Trust Level (RED)
```java
// After validating SSN_LAST_4 successfully:
// → Rule requires: ["SSN_FULL", "DEBIT_CARD_PIN"]
// → Risk Level: "HIGH"
// → Reason: "Low trust level detected (RED). Additional verification required."
```

#### Multiple Phone Matches
```java
// After validating DEBIT_CARD_PIN successfully:
// → Rule requires: ["SSN_FULL", "DATE_OF_BIRTH"]  
// → Risk Level: "MEDIUM"
// → Reason: "Phone number matches 3 customer accounts. Additional verification required."
```

## 📊 Session Context Integration

The system retrieves session data from context instead of request body:

### Session Context Data
- **DNIS**: Retrieved from session context using sessionId
- **Session SSN List**: List of SSNs from previous API calls
- **Customer Lookup Priority**: Session SSN takes precedence over phone number

### Sample Session Context
```json
{
  "sessionId": "session-12345",
  "dnis": "18001234567",
  "sessionSsn": ["123456789", "987654321"],
  "callerPhoneNumber": "+1234567890",
  "previousApiCallId": "api-call-001",
  "timestamp": 1703097600000
}
```

## 🧪 Test Data

The system comes pre-loaded with comprehensive test data:

### Test Customers
| Customer ID | Phone Number | Account Number | SSN | PIN | Status |
|-------------|--------------|----------------|-----|-----|--------|
| CUST001 | +1234567890 | ACC001 | 123456789 | 1234 | ACTIVE |
| CUST002 | +1987654321 | ACC002 | 987654321 | 1234 | ACTIVE |
| CUST003 | +1555123456 | ACC003 | 555123456 | 1234 | ACTIVE |
| CUST004 | +1444555666 | ACC004 | - | - | INACTIVE |

### Test Scenarios
- Brand-specific authentication flows
- DNIS-based routing scenarios
- Failed token demonstration
- Retry management testing
- Trust level validation
- Context-based authentication

## 🚀 Running the Application

### Prerequisites
- Java 17 or higher
- Maven 3.6 or higher

### Build and Run
```bash
# Clone the repository
git clone <repository-url>
cd ivr

# Build the application
mvn clean compile

# Run comprehensive test suite (170+ tests)
mvn test

# Start the application
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### Testing the API

```bash
# Test health endpoint
curl http://localhost:8080/api/v1/auth/health

# Get supported brands
curl http://localhost:8080/api/v1/auth/brands

# Get DNIS configurations
curl http://localhost:8080/api/v1/auth/dnis

# Start context-based authentication
curl -X POST http://localhost:8080/api/v1/auth/customer \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "session-12345",
    "customerIdentifier": {
      "type": "PHONE_NUMBER",
      "value": "+1234567890"
    },
    "brand": "PREMIUM_BANK",
    "trustLevelInfo": {
      "trustLevel": "GREEN",
      "phoneMatchStatus": "SINGLE_MATCH",
      "matchedSsnCount": 1
    }
  }'

# Continue authentication with token
curl -X POST http://localhost:8080/api/v1/auth/customer \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "session-12345",
    "customerIdentifier": {
      "type": "PHONE_NUMBER",
      "value": "+1234567890"
    },
    "attemptId": "<attempt-id-from-previous-response>",
    "providedTokens": [
      {
        "tokenName": "SSN",
        "tokenValue": "123456789"
      }
    ],
    "brand": "PREMIUM_BANK"
  }'
```

## ⚙️ Configuration

### Application Properties
- `app.auth.default-session-timeout`: Session timeout (default: 15m)
- `app.auth.max-overall-attempts`: Maximum authentication attempts (default: 5)
- `server.port`: Server port (default: 8080)
- `ivr.auth.repository.customer.type`: Repository type (memory/json)

### Profiles
- `default`: Standard configuration
- `test`: Test configuration with reduced timeouts

## 🔐 Authentication Flow

1. **Customer Identification**: Customer provides identifier (phone, account, or customer ID)
2. **Session Context Lookup**: System retrieves DNIS and session SSN from context
3. **Brand & DNIS Validation**: System validates brand support and applies DNIS-specific rules
4. **Trust Level Assessment**: Phone match status and trust level evaluation
5. **Eligibility Check**: System determines available authentication tokens based on customer profile and brand rules
6. **Token Selection**: Rule-based system determines which token to ask for
7. **Token Validation**: Customer provides token value, system validates against stored data
8. **Post-Validation Rules**: Additional security checks based on trust level and context
9. **Retry Management**: Failed attempts trigger retry logic and potential lockouts
10. **Completion Check**: System evaluates if authentication requirements are met
11. **Result**: Authentication succeeds, fails, or continues with additional token requests

## 🛡️ Security Features

- **Encrypted Storage**: Sensitive data (PINs) stored using BCrypt hashing
- **Advanced Attempt Limiting**: Per-token, per-brand, and overall attempt limits
- **Progressive Lockout**: Escalating lockout periods with suspicious activity detection
- **Session Management**: Automatic session expiration and cleanup
- **Input Validation**: Comprehensive validation of all inputs with brand context
- **Audit Logging**: Detailed logging for security monitoring and compliance
- **PII Protection**: Sensitive data masked in logs and responses
- **Trust Level Integration**: Phone match validation and trust-based decisions
- **DNIS Security**: Phone number-based security controls and restrictions
- **Post-Validation Security**: Additional checks after successful authentication

## 🧪 Testing

### Comprehensive Test Suite
- **170+ Tests**: Complete coverage of all functionality
- **Unit Tests**: Service layer, validation, and business logic
- **Integration Tests**: Controller layer with MockMvc
- **Brand-Specific Tests**: Authentication flows for each brand
- **DNIS Tests**: DNIS configuration and routing
- **Retry Management Tests**: Advanced retry logic and lockout scenarios
- **Security Tests**: Failed token handling and suspicious activity detection
- **Rule Tests**: Eligibility and token selection rule validation
- **Trust-Based Tests**: Trust level and phone match scenario validation

### Test Categories
- Authentication controller tests (24 test classes)
- Service layer tests (comprehensive coverage)
- Model validation tests
- Brand configuration tests
- DNIS configuration tests
- Failed token demonstration tests
- Retry management tests
- Context integration tests
- Trust-based authentication tests

## 🔧 Extensibility

### Adding New Authentication Tokens
1. Add token definition to brand configuration
2. Create validator implementing `TokenValidator`
3. Create eligibility rule implementing `EligibilityRule`
4. Update customer profile fields if needed
5. Add token selection logic if custom rules needed

### Adding New Brands
1. Add brand configuration implementing `BrandAuthConfiguration`
2. Define brand-specific token definitions and rules
3. Configure brand-specific messages and policies
4. Create brand-specific token selection rules if needed
5. Update tests for new brand scenarios

### Adding New DNIS Numbers
1. Add DNIS configuration in `dnis-configurations.json`
2. Define DNIS-specific authentication rules
3. Configure security settings and restrictions
4. Test DNIS-based routing scenarios

### Adding New Business Rules
1. **Eligibility Rules**: Implement `EligibilityRule` interface
2. **Token Selection Rules**: Implement `TokenSelectionRule` interface
3. **Post-Validation Rules**: Implement `PostValidationRule` interface
4. Register as Spring bean - rules are automatically discovered

### Extending Trust-Based Authentication
1. Add new trust level conditions to existing rules
2. Create brand-specific trust rules implementing `TokenSelectionRule`
3. Configure rule priorities to control execution order
4. Add comprehensive test scenarios

## 📊 Monitoring and Operations

- **Health Checks**: `/api/v1/auth/health` endpoint with DNIS support status
- **Metrics**: Spring Boot Actuator endpoints
- **Structured Logging**: Comprehensive logging with session tracking
- **Error Handling**: Brand-aware error responses with appropriate HTTP status codes
- **Performance Monitoring**: Processing time tracking and optimization
- **Security Monitoring**: Failed attempt tracking and suspicious activity detection
- **Rule Monitoring**: Rule execution tracking and performance metrics

## 🏗️ Development

### Project Structure
```
src/
├── main/java/com/bank/ivr/auth/
│   ├── config/          # Configuration classes
│   ├── controller/      # REST controllers
│   ├── model/          # Data models (request, response, domain)
│   ├── repository/     # Data access layer (in-memory/JSON)
│   ├── rule/           # Business rules and post-validation
│   ├── service/        # Business logic services
│   ├── util/           # Utility classes
│   └── validator/      # Token validators
├── main/resources/
│   ├── application.yml # Configuration
│   ├── sample-session-contexts.json # Session context data
│   ├── sample-customer-data.json    # Customer data
│   └── dnis-configurations.json     # DNIS configurations
└── test/
    ├── java/           # Comprehensive test classes (170+ tests)
    └── resources/      # Test configuration and data
```

### Key Design Patterns
- **Builder Pattern**: Complex object creation (requests, responses)
- **Strategy Pattern**: Token validation, retry strategies, and business rules
- **Repository Pattern**: Data access abstraction with in-memory/JSON implementation
- **Service Layer**: Business logic encapsulation with clear separation of concerns
- **Factory Pattern**: Brand and DNIS configuration creation
- **Observer Pattern**: Event-driven retry management and security monitoring
- **Rule Pattern**: Extensible rule-based system for eligibility and token selection

## 🏦 Bank Onboarding Guide

### Adding a New Bank

To add a new bank to the system, follow these steps:

#### 1. Define Your Bank's Authentication Strategy
```yaml
Brand: TECH_BANK
Description: "Modern digital bank with mobile-first authentication"

Token Priorities: 
  - Primary (Priority 150): MOBILE_PIN (4-6 digits)
  - Secondary (Priority 120): BIOMETRIC_ID 
  - Fallback (Priority 100): ACCOUNT_NUMBER

Security Policies:
  - Max Overall Attempts: 5
  - Progressive Lockout: Enabled
  - Multi-Factor Preferred: Yes

Trust Level Rules:
  - GREEN: Single factor authentication allowed
  - RED: Enhanced verification required
```

#### 2. Implement Custom Token Validators
```java
@Component
public class TechBankMobilePinValidator implements TokenValidator {
    @Override
    public String getTokenName() {
        return "MOBILE_PIN";
    }
    
    @Override
    public String getBrand() {
        return "TECH_BANK";
    }
    
    @Override
    public boolean validate(String customerId, String providedPin, CustomerProfile profile) {
        // Custom validation logic for Tech Bank mobile PIN
        return EncryptionUtil.verify(providedPin, profile.getMobilePin());
    }
}
```

#### 3. Create Brand Configuration
```java
@Component
public class TechBankAuthConfiguration implements BrandAuthConfiguration {
    @Override
    public String getBrandCode() {
        return "TECH_BANK";
    }
    
    @Override
    public List<AuthTokenDefinition> getTokenDefinitions() {
        return Arrays.asList(
            AuthTokenDefinition.builder()
                .name("MOBILE_PIN")
                .description("Mobile Banking PIN")
                .priority(150)
                .maxAttempts(3)
                .build()
        );
    }
}
```

#### 4. Configure DNIS Support
Add DNIS configuration for your bank's phone numbers:
```json
{
  "dnis": "18005559999",
  "description": "Tech Bank customer service",
  "allowMobilePinAuthentication": true,
  "requireMultiFactorAuth": false,
  "maxAuthenticationAttempts": 5
}
```

#### 5. Add Business Rules (Optional)
```java
@Component
public class TechBankMobileFirstRule implements TokenSelectionRule {
    @Override
    public String determineNextToken(AuthenticationContext context, CustomerProfile customerProfile) {
        // Prefer mobile PIN for Tech Bank customers
        if (context.getEligibleTokens().contains("MOBILE_PIN")) {
            return "MOBILE_PIN";
        }
        return null; // Let default logic handle
    }
    
    @Override
    public String getBrand() {
        return "TECH_BANK";
    }
    
    @Override
    public int getPriority() {
        return 200;
    }
}
```

#### 6. Test Your Implementation
Create comprehensive tests for your brand:
- Brand-specific authentication flows
- Custom token validation
- Business rule behavior
- Failure scenarios

## 🆕 Recent Enhancements

### Version 2.0 - Major Release
- ✅ DNIS support with phone number-based routing
- ✅ Brand-aware authentication with comprehensive configuration
- ✅ Advanced retry management with progressive lockout
- ✅ Trust level integration and phone match validation
- ✅ Failed token tracking with smart re-asking logic
- ✅ Post-validation security rules
- ✅ Comprehensive test suite (170+ tests)
- ✅ Enhanced error handling and logging
- ✅ Security improvements and PII protection
- ✅ Context integration with session-based data retrieval
- ✅ Rule-based architecture with eligibility and token selection rules
- ✅ Trust-based authentication with conditional logic

### Architecture Improvements
- **Enhanced Request Model**: Support for trust level info and session context
- **Service Layer Expansion**: New services for DNIS, retry management, and rules
- **Comprehensive API**: Brand-specific endpoints, DNIS configuration, and health checks
- **Improved Code Quality**: Fixed all compilation issues and updated test expectations
- **Rule System**: Extensible rule-based system for complex business logic

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🎯 Project Capabilities Summary

This enterprise-grade IVR authentication service provides:

### Core Authentication Capabilities
- **Multi-Brand Support**: Distinct authentication flows for different banking brands
- **DNIS Integration**: Phone number-based routing with configurable security rules
- **Advanced Token Management**: Multiple authentication factors with intelligent prioritization
- **Trust Level Integration**: Phone match validation and risk-based authentication decisions
- **Context Integration**: Session-based data retrieval with automatic DNIS and SSN lookup
- **Rule-Based System**: Extensible architecture with eligibility and token selection rules

### Security & Compliance
- **Progressive Security**: Escalating security measures based on failed attempts and suspicious activity
- **Comprehensive Audit Trail**: Detailed logging for compliance and security monitoring
- **PII Protection**: Secure handling and masking of sensitive customer information
- **Configurable Security Policies**: Brand and DNIS-specific security controls
- **Post-Validation Security**: Additional checks after successful authentication
- **Trust-Based Authentication**: Advanced conditional logic for risk assessment

### Enterprise Features
- **High Availability**: In-memory storage with automatic failover capabilities
- **Scalable Architecture**: Stateless design supporting horizontal scaling
- **Comprehensive Testing**: 170+ tests ensuring reliability and correctness
- **Production Ready**: Health checks, monitoring, and operational excellence
- **Session Management**: Context-based session handling with automatic expiration
- **Performance Optimized**: Efficient algorithms and caching strategies

### Developer Experience
- **Self-Contained**: No external dependencies, runs out-of-the-box
- **Extensible Design**: Easy addition of new brands, tokens, and business rules
- **Comprehensive Documentation**: Detailed API documentation and examples
- **Test-Driven Development**: Extensive test coverage for all functionality
- **Rule-Based Extensions**: Simple interfaces for adding complex business logic
- **Clean Architecture**: Well-organized codebase with clear separation of concerns 