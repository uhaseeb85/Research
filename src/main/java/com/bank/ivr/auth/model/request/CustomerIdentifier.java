package com.bank.ivr.auth.model.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CustomerIdentifier {
    
    public enum IdentifierType {
        PHONE_NUMBER, ACCOUNT_NUMBER, CUSTOMER_ID
    }
    
    @NotNull(message = "Identifier type is required")
    private final IdentifierType type;
    
    @NotBlank(message = "Identifier value is required")
    private final String value;
    
    @JsonCreator
    public CustomerIdentifier(
            @JsonProperty("type") IdentifierType type,
            @JsonProperty("value") String value) {
        this.type = type;
        this.value = value;
    }
    
    public IdentifierType getType() {
        return type;
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public String toString() {
        return "CustomerIdentifier{" +
               "type=" + type +
               ", value='" + value + '\'' +
               '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        CustomerIdentifier that = (CustomerIdentifier) o;
        
        if (type != that.type) return false;
        return value != null ? value.equals(that.value) : that.value == null;
    }
    
    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }
} 