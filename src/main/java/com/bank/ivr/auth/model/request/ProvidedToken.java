package com.bank.ivr.auth.model.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public class ProvidedToken {
    
    @NotBlank(message = "Token name is required")
    private final String tokenName;
    
    @NotBlank(message = "Token value is required")
    private final String tokenValue;
    
    @JsonCreator
    public ProvidedToken(
            @JsonProperty("tokenName") String tokenName,
            @JsonProperty("tokenValue") String tokenValue) {
        this.tokenName = tokenName;
        this.tokenValue = tokenValue;
    }
    
    public String getTokenName() {
        return tokenName;
    }
    
    public String getTokenValue() {
        return tokenValue;
    }
    
    @Override
    public String toString() {
        return "ProvidedToken{" +
               "tokenName='" + tokenName + '\'' +
               ", tokenValue='[MASKED]'" +
               '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        ProvidedToken that = (ProvidedToken) o;
        
        if (tokenName != null ? !tokenName.equals(that.tokenName) : that.tokenName != null) return false;
        return tokenValue != null ? tokenValue.equals(that.tokenValue) : that.tokenValue == null;
    }
    
    @Override
    public int hashCode() {
        int result = tokenName != null ? tokenName.hashCode() : 0;
        result = 31 * result + (tokenValue != null ? tokenValue.hashCode() : 0);
        return result;
    }
} 