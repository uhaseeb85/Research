# Bank IVR Authentication System

A comprehensive multi-factor authentication system for bank customers via IVR (Interactive Voice Response) systems, built with Spring Boot 3.1.0.

## Overview

This system provides secure, stateful authentication for bank customers through IVR systems using dynamic question flows and token validation. The system is designed to be completely self-contained with **no external database dependencies** - all data is stored in-memory using Java collections.

## Key Features

- **In-Memory Storage**: No database required - all customer data and authentication contexts stored in Java collections
- **Multi-Factor Authentication**: Support for multiple authentication tokens (SSN, Debit Card PIN, Date of Birth, etc.)
- **Dynamic Question Flows**: Intelligent token selection based on customer eligibility and priority
- **Stateful Authentication**: Session management with automatic expiration
- **Extensible Architecture**: Easy to add new authentication tokens and business rules
- **RESTful API**: Single endpoint handling both new and continuing authentication attempts
- **Comprehensive Validation**: Input validation, attempt tracking, and security measures
- **Production Ready**: Proper error handling, logging, and monitoring capabilities

## Architecture

### Core Components

1. **Model Layer**
   - Request/Response models with validation
   - Domain models (CustomerProfile, AuthenticationContext, AuthTokenDefinition)
   - Builder patterns for complex object creation

2. **Repository Layer**
   - In-memory implementations using Java collections
   - No database dependencies
   - Automatic data initialization with test customers

3. **Service Layer**
   - `AuthenticationService`: Core authentication orchestration
   - `TokenValidationService`: Token validation management
   - Business rule evaluation and token eligibility determination

4. **Controller Layer**
   - REST API with comprehensive error handling
   - Health check endpoint
   - Proper HTTP status codes and responses

5. **Security**
   - BCrypt password hashing for sensitive data
   - Masked logging for PII
   - Secure token validation

## API Endpoints

### Authentication Endpoint
```
POST /api/v1/auth/customer
Content-Type: application/json
```

#### New Authentication Request
```json
{
  "sessionId": "session-123",
  "customerIdentifier": {
    "type": "PHONE_NUMBER",
    "value": "+1234567890"
  }
}
```

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
  ]
}
```

#### Response Format
```json
{
  "attemptId": "uuid",
  "status": "PENDING_PRIMARY_TOKEN|PENDING_MORE_TOKENS|AUTHENTICATED|FAILED",
  "message": "Please provide your Social Security Number.",
  "primaryTokenToAsk": {
    "name": "SSN",
    "description": "Social Security Number",
    "priority": 100,
    "maxAttempts": 3
  },
  "secondaryTokensAccepted": [...],
  "remainingAttempts": {
    "SSN": 3,
    "OVERALL": 5
  },
  "requiredTokensRemaining": ["SSN"],
  "authenticatedTokens": []
}
```

### Health Check
```
GET /api/v1/auth/health
```

## Test Data

The system comes pre-loaded with test customer data:

| Customer ID | Phone Number | Account Number | SSN | PIN | Status |
|-------------|--------------|----------------|-----|-----|--------|
| CUST001 | +1234567890 | ACC001 | 123456789 | 1234 | ACTIVE |
| CUST002 | +1987654321 | ACC002 | 987654321 | 1234 | ACTIVE |
| CUST003 | +1555123456 | ACC003 | 555123456 | 1234 | ACTIVE |
| CUST004 | +1444555666 | ACC004 | - | - | INACTIVE |

## Running the Application

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

# Run tests
mvn test

# Start the application
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### Testing the API

```bash
# Test health endpoint
curl http://localhost:8080/api/v1/auth/health

# Start authentication
curl -X POST http://localhost:8080/api/v1/auth/customer \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "test-session-123",
    "customerIdentifier": {
      "type": "PHONE_NUMBER",
      "value": "+1234567890"
    }
  }'

# Continue authentication with SSN
curl -X POST http://localhost:8080/api/v1/auth/customer \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "test-session-123",
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
    ]
  }'
```

## Configuration

### Application Properties
- `app.auth.default-session-timeout`: Session timeout (default: 15m)
- `app.auth.max-overall-attempts`: Maximum authentication attempts (default: 5)
- `server.port`: Server port (default: 8080)

### Profiles
- `default`: Standard configuration
- `test`: Test configuration with reduced timeouts

## Authentication Flow

1. **Customer Identification**: Customer provides identifier (phone, account, or customer ID)
2. **Eligibility Check**: System determines available authentication tokens based on customer profile
3. **Token Request**: System asks for highest priority available token
4. **Token Validation**: Customer provides token value, system validates against stored data
5. **Completion Check**: System evaluates if authentication requirements are met
6. **Result**: Authentication succeeds, fails, or continues with additional token requests

## Security Features

- **Encrypted Storage**: Sensitive data (PINs) stored using BCrypt hashing
- **Attempt Limiting**: Per-token and overall attempt limits prevent brute force attacks
- **Session Management**: Automatic session expiration and cleanup
- **Input Validation**: Comprehensive validation of all inputs
- **Audit Logging**: Detailed logging for security monitoring
- **PII Protection**: Sensitive data masked in logs and responses

## Extensibility

### Adding New Authentication Tokens
1. Add token definition to `AuthTokenConfig`
2. Create validator implementing `TokenValidator`
3. Update eligibility logic in `AuthenticationService`
4. Add customer profile fields if needed

### Adding New Business Rules
1. Implement `AuthenticationRule` interface
2. Register as Spring bean
3. Rules are automatically discovered and applied

## Monitoring and Operations

- **Health Checks**: `/api/v1/auth/health` endpoint
- **Metrics**: Spring Boot Actuator endpoints
- **Logging**: Structured logging with configurable levels
- **Error Handling**: Comprehensive error responses with appropriate HTTP status codes

## Development

### Project Structure
```
src/
├── main/java/com/bank/ivr/auth/
│   ├── config/          # Configuration classes
│   ├── controller/      # REST controllers
│   ├── model/          # Data models
│   ├── repository/     # Data access layer
│   ├── rule/           # Business rules
│   ├── service/        # Business logic
│   ├── util/           # Utility classes
│   └── validator/      # Token validators
├── main/resources/
│   └── application.yml # Configuration
└── test/
    ├── java/           # Test classes
    └── resources/      # Test configuration
```

### Key Design Patterns
- **Builder Pattern**: Complex object creation
- **Strategy Pattern**: Token validation and business rules
- **Repository Pattern**: Data access abstraction
- **Service Layer**: Business logic encapsulation

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Project Capabilities

This project provides a robust and flexible IVR authentication service with the following key capabilities:

*   **Brand-aware Authentication**: The core functionality is to authenticate IVR customers based on brand-specific rules. Different brands can have distinct authentication flows, token requirements, and customer-facing messages.
*   **Token-based Authentication**:
    *   Supports multiple types of authentication tokens (e.g., PIN, OTP, security question).
    *   Allows configuration of token priorities to determine the order in which tokens are requested.
    *   Enables setting maximum attempt limits for each token type, specific to each brand.
*   **Session Management**:
    *   Manages authentication sessions, distinguishing between new and continuing attempts.
    *   Maintains authentication context across multiple interactions within a session.
*   **Configurable Branding**:
    *   Allows defining unique authentication configurations for multiple brands.
    *   Supports customization of token definitions, required tokens, maximum overall attempts, and concurrent authentication settings per brand.
    *   Provides brand-specific messaging for various authentication scenarios (e.g., success, failure, customer not found, session expired).
*   **API Endpoints**:
    *   `POST /api/v1/auth/customer`: The primary endpoint for authenticating customers. It accepts customer identifiers, provided tokens, and the brand to initiate or continue an authentication attempt.
    *   `GET /api/v1/auth/methods/{brand}`: Retrieves the available authentication methods and their configurations (e.g., token definitions, required tokens, max attempts) for a specified brand.
    *   `GET /api/v1/auth/brands`: Lists all supported brands configured in the system.
    *   `GET /api/v1/auth/health`: A standard health check endpoint to verify the service's operational status.
*   **Comprehensive Logging**:
    *   Detailed logging throughout the authentication process for monitoring, auditing, and troubleshooting.
    *   Logs include session IDs, attempt IDs, brand information, processing times, and specific event details.
*   **Caching**:
    *   Utilizes Spring's caching mechanism to improve performance, likely for frequently accessed configurations or context data.
*   **Error Handling**:
    *   Provides clear error responses, including brand-specific messages where appropriate.
    *   Handles various error conditions such as invalid input, unsupported brand, session expiry, and system errors. 