# DNIS Integration Guide

## Overview

This guide explains the DNIS (Dialed Number Identification Service) integration in the IVR Authentication System. DNIS support allows the system to apply different authentication rules and configurations based on the phone number that customers dial to reach the IVR system.

## Key Features

### 1. DNIS-Specific Authentication Rules
- **Token Allowance**: Configure which authentication tokens (SSN, PIN, Date of Birth, etc.) are allowed for specific DNIS numbers
- **Multi-Factor Authentication**: Enable or disable MFA requirements per DNIS
- **Trust Level Bypass**: Allow trust level bypass for customer service DNIS numbers
- **Validation Settings**: Configure strict validation, phone match validation, and retry policies per DNIS

### 2. Session SSN Integration
- **Session-Based Lookup**: Support for SSN retrieved from previous API calls and stored in session
- **Priority Customer Lookup**: Enhanced customer lookup that prioritizes session SSN over other identifiers
- **Secure Handling**: Proper masking and secure handling of SSN data in logs and responses

### 3. Flexible Configuration
- **JSON-Based Configuration**: DNIS rules stored in easily maintainable JSON files
- **Default Fallback**: Automatic fallback to default configuration for unspecified DNIS numbers
- **Runtime Loading**: Configuration loaded at startup with graceful fallback to defaults

## API Enhancements

### Enhanced Authentication Request

The authentication request now supports additional fields:

```json
{
  "sessionId": "session-12345",
  "customerIdentifier": {
    "type": "PHONE_NUMBER",
    "value": "+1234567890"
  },
  "attemptId": null,
  "providedTokens": [],
  "brand": "PREMIUM_BANK",
  "trustLevelInfo": {
    "trustLevel": "GREEN",
    "phoneMatchStatus": "SINGLE_MATCH",
    "matchedSsnCount": 1
  },
  "dnis": "18001234567",
  "sessionSsn": "123456789"
}
```

#### New Fields:
- **`dnis`** (optional): The phone number dialed by the customer to reach the IVR system
- **`sessionSsn`** (optional): SSN retrieved from session data from a previous API call

### New API Endpoints

#### Get DNIS Configuration
```
GET /api/v1/auth/dnis/{dnis}
```

Returns the configuration for a specific DNIS number:

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
  "allowAlternativeTokens": false,
  "enableStrictValidation": true,
  "allowRetryOnFailure": false,
  "enableAuditLogging": true,
  "maxAuthenticationAttempts": 2,
  "sessionTimeoutMinutes": 10
}
```

#### Get All DNIS Configurations
```
GET /api/v1/auth/dnis
```

Returns all configured DNIS configurations:

```json
{
  "dnisConfigurations": [
    {
      "dnis": "DEFAULT",
      "description": "Default DNIS configuration for all unspecified numbers",
      "allowSsnAuthentication": true,
      "allowPinAuthentication": true,
      "requireMultiFactorAuth": false,
      "maxAuthenticationAttempts": 3,
      "sessionTimeoutMinutes": 15
    },
    {
      "dnis": "18001234567",
      "description": "Premium banking services - high security DNIS",
      "allowSsnAuthentication": true,
      "allowPinAuthentication": true,
      "requireMultiFactorAuth": true,
      "maxAuthenticationAttempts": 2,
      "sessionTimeoutMinutes": 10
    }
  ],
  "count": 2
}
```

## DNIS Configuration

### Configuration File Location
DNIS configurations are stored in: `src/main/resources/dnis-configurations.json`

### Configuration Structure

```json
[
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
    "allowAlternativeTokens": false,
    "enableStrictValidation": true,
    "allowRetryOnFailure": false,
    "enableAuditLogging": true,
    "maxAuthenticationAttempts": 2,
    "sessionTimeoutMinutes": 10
  }
]
```

### Configuration Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `dnis` | String | Required | The DNIS number (use "DEFAULT" for fallback) |
| `description` | String | Required | Human-readable description of the DNIS |
| `allowSsnAuthentication` | Boolean | true | Allow SSN-based authentication |
| `allowPinAuthentication` | Boolean | true | Allow PIN-based authentication |
| `allowDateOfBirthAuthentication` | Boolean | true | Allow date of birth authentication |
| `allowMotherMaidenNameAuthentication` | Boolean | true | Allow mother's maiden name authentication |
| `allowAccountNumberAuthentication` | Boolean | true | Allow account number authentication |
| `requireMultiFactorAuth` | Boolean | false | Require multiple authentication factors |
| `allowTrustLevelBypass` | Boolean | false | Allow bypassing trust level requirements |
| `enablePhoneMatchValidation` | Boolean | true | Enable phone number matching validation |
| `allowAlternativeTokens` | Boolean | true | Allow alternative authentication tokens |
| `enableStrictValidation` | Boolean | false | Enable strict validation mode |
| `allowRetryOnFailure` | Boolean | true | Allow retry attempts on authentication failure |
| `enableAuditLogging` | Boolean | true | Enable detailed audit logging |
| `maxAuthenticationAttempts` | Integer | 3 | Maximum authentication attempts allowed |
| `sessionTimeoutMinutes` | Integer | 15 | Session timeout in minutes |

## Authentication Flow with DNIS

### 1. Initial Request (New Authentication)

```bash
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
    },
    "dnis": "18001234567",
    "sessionSsn": "123456789"
  }'
```

### 2. System Processing

1. **DNIS Configuration Lookup**: System retrieves configuration for DNIS "18001234567"
2. **Customer Lookup**: System attempts to find customer using session SSN first, then falls back to phone number
3. **Token Eligibility**: System determines available tokens based on DNIS configuration and customer profile
4. **Response Generation**: System returns appropriate token request based on DNIS rules

### 3. Continuing Authentication

```bash
curl -X POST http://localhost:8080/api/v1/auth/customer \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "session-12345",
    "customerIdentifier": {
      "type": "PHONE_NUMBER",
      "value": "+1234567890"
    },
    "attemptId": "attempt-uuid-from-previous-response",
    "providedTokens": [
      {
        "tokenName": "DEBIT_CARD_PIN",
        "tokenValue": "1234"
      }
    ],
    "brand": "PREMIUM_BANK",
    "dnis": "18001234567"
  }'
```

## Use Cases

### 1. Premium Banking Services (High Security)
- **DNIS**: 18001234567
- **Features**: Multi-factor auth required, strict validation, limited retry attempts
- **Use Case**: High-value transactions, premium customer services

### 2. General Customer Service (Standard Security)
- **DNIS**: 18009876543
- **Features**: Trust level bypass allowed, relaxed validation, more retry attempts
- **Use Case**: General inquiries, customer support

### 3. Mobile Banking (Medium Security)
- **DNIS**: 18005551234
- **Features**: Account number auth disabled, standard MFA, phone match validation
- **Use Case**: Mobile app support, quick authentication

### 4. Business Banking (Enhanced Security)
- **DNIS**: 18007778888
- **Features**: Limited auth methods, MFA required, strict validation
- **Use Case**: Business customers, corporate banking

## Configuration Management

### Adding New DNIS Configuration

1. Edit `src/main/resources/dnis-configurations.json`
2. Add new configuration object to the array
3. Restart the application (configuration is loaded at startup)

Example:
```json
{
  "dnis": "18001111111",
  "description": "New service line - custom security",
  "allowSsnAuthentication": true,
  "allowPinAuthentication": false,
  "requireMultiFactorAuth": true,
  "maxAuthenticationAttempts": 1,
  "sessionTimeoutMinutes": 5
}
```

### Configuration Validation

The system validates configurations at startup and logs any issues. Invalid configurations will fall back to defaults.

### Environment-Specific Configuration

Use Spring profiles to manage different configurations:

```yaml
# application-prod.yml
app:
  dnis:
    config-file: classpath:dnis-configurations-prod.json

# application-dev.yml
app:
  dnis:
    config-file: classpath:dnis-configurations-dev.json
```

## Security Considerations

### 1. SSN Handling
- Session SSN is masked in all log outputs
- SSN data is never returned in API responses
- Secure storage and transmission practices should be followed

### 2. DNIS Validation
- DNIS numbers are cleaned (non-numeric characters removed)
- Invalid DNIS numbers fall back to default configuration
- All DNIS operations are logged for audit purposes

### 3. Configuration Security
- DNIS configuration files should be protected with appropriate file permissions
- Consider encrypting sensitive configuration data in production
- Regular review of DNIS configurations for security compliance

## Monitoring and Logging

### Key Log Events
- DNIS configuration loading at startup
- DNIS-specific authentication attempts
- Session SSN usage for customer lookup
- Configuration fallbacks to defaults

### Log Examples

```
INFO  - Loaded DNIS configuration for: 18001234567 - Premium banking services - high security
INFO  - DNIS provided in request: 18001234567 for session: session-12345
INFO  - Using DNIS configuration: 18001234567 - Premium banking services - high security
INFO  - Customer lookup successful using session SSN
```

## Testing

### Test Files Provided

1. **`test_request_with_dnis_and_session_ssn.json`**: Full DNIS and session SSN example
2. **`test_request_with_dnis_only.json`**: DNIS without session SSN
3. **`test_request.json`**: Original format (still supported)

### Testing DNIS Endpoints

```bash
# Test DNIS configuration retrieval
curl http://localhost:8080/api/v1/auth/dnis/18001234567

# Test all DNIS configurations
curl http://localhost:8080/api/v1/auth/dnis

# Test authentication with DNIS
curl -X POST http://localhost:8080/api/v1/auth/customer \
  -H "Content-Type: application/json" \
  -d @test_request_with_dnis_and_session_ssn.json
```

## Troubleshooting

### Common Issues

1. **DNIS Configuration Not Found**
   - Check if DNIS number exists in configuration file
   - Verify DNIS number format (numeric only)
   - Check application logs for configuration loading errors

2. **Session SSN Not Working**
   - Verify SSN format (9 digits)
   - Check if customer exists with provided SSN
   - Review customer lookup logs

3. **Authentication Rules Not Applied**
   - Confirm DNIS configuration is loaded correctly
   - Check if DNIS-specific rules are being applied
   - Verify token eligibility based on DNIS configuration

### Debug Mode

Enable debug logging for DNIS operations:

```yaml
logging:
  level:
    com.bank.ivr.auth.service.DnisConfigurationService: DEBUG
    com.bank.ivr.auth.service.CustomerLookupService: DEBUG
```

## Future Enhancements

### Planned Features
1. **Dynamic Configuration Updates**: Hot-reload of DNIS configurations without restart
2. **Advanced Routing**: Route to different authentication flows based on DNIS
3. **Analytics Integration**: DNIS-specific authentication metrics and reporting
4. **External Configuration**: Support for external configuration sources (database, config server)

### Integration Opportunities
1. **Call Center Integration**: Integration with call center systems for DNIS detection
2. **Fraud Detection**: DNIS-based fraud detection and prevention
3. **Customer Analytics**: DNIS-based customer behavior analysis
4. **Load Balancing**: DNIS-based load balancing and routing 