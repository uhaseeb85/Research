# Context Integration Guide

## Overview

This guide documents the changes made to integrate session context data into the IVR Authentication System. The system now retrieves `dnis` and `sessionSsn` from session context instead of the request body, simulating a real-world scenario where a previous API call populates this information.

## Key Changes

### 1. Session Context Service

A new `SessionContextService` has been created to manage session context data:

- **Location**: `src/main/java/com/bank/ivr/auth/service/SessionContextService.java`
- **Purpose**: Retrieves DNIS and session SSN list from context (simulated via JSON file)
- **Real Implementation**: In production, this would query Redis or another session store

#### Key Methods:
- `getDnisFromSession(String sessionId)`: Retrieves DNIS for a session
- `getSessionSsnFromSession(String sessionId)`: Retrieves list of SSNs for a session
- `getSessionContext(String sessionId)`: Retrieves full session context

### 2. Modified Authentication Request

The `AuthenticationRequest` class has been updated:

**Removed Fields:**
- `dnis` - Now retrieved from session context
- `sessionSsn` - Now retrieved from session context as a list

**Remaining Fields:**
- `sessionId` - Used to lookup context data
- `customerIdentifier` - Customer identification
- `attemptId` - For continuing authentication
- `providedTokens` - Authentication tokens
- `brand` - Brand information
- `trustLevelInfo` - Trust level data

### 3. Enhanced Customer Lookup

The authentication flow now prioritizes customer lookup using session SSN:

1. **Priority 1**: Session SSN list (from context)
2. **Priority 2**: Standard customer identifier (phone, account, etc.)

This ensures that if a previous API call identified the customer via SSN, that information takes precedence.

### 4. JSON-Based Data Sources

#### Session Context Data
- **File**: `src/main/resources/sample-session-contexts.json`
- **Purpose**: Simulates session context data that would be populated by a previous API
- **Structure**:
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

#### Customer Data
- **File**: `src/main/resources/sample-customer-data.json`
- **Purpose**: Replaces database calls with JSON-based customer data
- **Implementation**: `JsonCustomerProfileRepository` (marked as `@Primary`)

## API Changes

### Authentication Request Format

**Before (with dnis and sessionSsn in request):**
```json
{
  "sessionId": "session-12345",
  "customerIdentifier": {
    "type": "PHONE_NUMBER",
    "value": "+1234567890"
  },
  "brand": "PREMIUM_BANK",
  "dnis": "18001234567",
  "sessionSsn": "123456789"
}
```

**After (context-based):**
```json
{
  "sessionId": "session-12345",
  "customerIdentifier": {
    "type": "PHONE_NUMBER",
    "value": "+1234567890"
  },
  "brand": "PREMIUM_BANK"
}
```

### Session Context Lookup

The system now automatically:
1. Retrieves DNIS from session context using `sessionId`
2. Retrieves session SSN list from session context
3. Uses this information for customer lookup and DNIS configuration

## Testing

### Test Files

1. **`test_request_context_based.json`** - New authentication request
2. **`test_request_context_based_with_tokens.json`** - Continuing authentication
3. **`sample-session-contexts.json`** - Session context data
4. **`sample-customer-data.json`** - Customer data

### Test Scenarios

#### Scenario 1: New Authentication with Context
```bash
curl -X POST http://localhost:8080/api/v1/auth/customer \
  -H "Content-Type: application/json" \
  -d @test_request_context_based.json
```

**Expected Behavior:**
1. System retrieves DNIS "18001234567" from session context
2. System retrieves session SSN list ["123456789", "987654321"]
3. Customer lookup prioritizes session SSN
4. DNIS configuration applied based on retrieved DNIS

#### Scenario 2: Customer Lookup Priority
For session "session-12345":
1. **First**: Try to find customer using SSN "123456789"
2. **Second**: Try to find customer using SSN "987654321"  
3. **Fallback**: Use phone number "+1234567890" from request

### Sample Session Context Data

The system includes sample data for testing:

| Session ID | DNIS | Session SSN | Description |
|------------|------|-------------|-------------|
| session-12345 | 18001234567 | ["123456789", "987654321"] | Premium banking |
| session-67890 | 18009876543 | ["555123456"] | Customer service |
| session-11111 | 18005551234 | ["123456789"] | Mobile banking |
| session-22222 | 18007778888 | ["987654321", "555123456"] | Business banking |

## Configuration

### Repository Selection

By default, the system uses JSON-based repositories. To switch back to in-memory:

```properties
# application.properties
ivr.auth.repository.customer.type=memory
```

### Session Context

In production, update `SessionContextService` to:
- Connect to Redis or session store
- Implement proper session management
- Add session expiration handling

## Implementation Notes

### Backward Compatibility

The `AuthenticationOrchestrator` maintains backward compatibility:
- New method: `authenticateCustomer(request, dnis, sessionSsnList)`
- Legacy method: `authenticateCustomer(request)` - calls new method with null values

### Error Handling

- Missing session context: System logs warning and continues with standard lookup
- Invalid session data: System gracefully falls back to request-based identification
- JSON loading failures: System falls back to minimal test data

### Security Considerations

- Session SSN data is masked in logs
- Context data includes timestamp for session validation
- Previous API call ID tracked for audit purposes

## Migration Path

### Phase 1: Context Integration (Current)
- ✅ Session context service implemented
- ✅ JSON-based data sources
- ✅ Enhanced customer lookup
- ✅ Backward compatibility maintained

### Phase 2: Production Integration (Future)
- [ ] Redis/session store integration
- [ ] Session expiration handling
- [ ] Enhanced security measures
- [ ] Performance optimization

### Phase 3: Legacy Cleanup (Future)
- [ ] Remove backward compatibility methods
- [ ] Deprecate old request format
- [ ] Full context-based implementation

## Troubleshooting

### Common Issues

1. **Session context not found**
   - Check if session ID exists in `sample-session-contexts.json`
   - Verify SessionContextService is loading data correctly

2. **Customer not found**
   - Verify session SSN matches customer data in `sample-customer-data.json`
   - Check fallback to standard customer identifier

3. **DNIS configuration issues**
   - Ensure DNIS from context matches configured DNIS values
   - Check DNIS configuration loading

### Debug Logging

Enable debug logging for context operations:
```properties
logging.level.com.bank.ivr.auth.service.SessionContextService=DEBUG
logging.level.com.bank.ivr.auth.service.CustomerLookupService=DEBUG
```

## Benefits

1. **Realistic Architecture**: Simulates real-world session-based data flow
2. **Enhanced Customer Lookup**: Prioritizes session-based customer identification
3. **Flexible Testing**: JSON-based data sources for easy test scenario creation
4. **Maintainable**: Clear separation of concerns with dedicated services
5. **Scalable**: Foundation for production session store integration 