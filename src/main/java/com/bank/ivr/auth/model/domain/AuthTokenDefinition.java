package com.bank.ivr.auth.model.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthTokenDefinition {
    
    @JsonProperty("name")
    private final String name;
    
    @JsonProperty("description")
    private final String description;
    
    private final int priority;
    
    @JsonProperty("inputFormatRegex")
    private final String inputFormatRegex;
    
    private final int maxAttempts;
    
    @JsonCreator
    public AuthTokenDefinition(
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("priority") int priority,
            @JsonProperty("inputFormatRegex") String inputFormatRegex,
            @JsonProperty("maxAttempts") int maxAttempts) {
        this.name = name;
        this.description = description;
        this.priority = priority;
        this.inputFormatRegex = inputFormatRegex;
        this.maxAttempts = maxAttempts;
    }
    
    // Builder pattern constructor
    private AuthTokenDefinition(Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.priority = builder.priority;
        this.inputFormatRegex = builder.inputFormatRegex;
        this.maxAttempts = builder.maxAttempts;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getPriority() {
        return priority;
    }
    

    
    public String getInputFormatRegex() {
        return inputFormatRegex;
    }
    
    public int getMaxAttempts() {
        return maxAttempts;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String name;
        private String description;
        private int priority = 50; // default priority
        private String inputFormatRegex;
        private int maxAttempts = 3; // default max attempts
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }
        
        public Builder inputFormatRegex(String inputFormatRegex) {
            this.inputFormatRegex = inputFormatRegex;
            return this;
        }
        
        public Builder maxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }
        
        public AuthTokenDefinition build() {
            if (name == null || description == null) {
                throw new IllegalArgumentException("Name and description are required");
            }
            return new AuthTokenDefinition(this);
        }
    }
    
    @Override
    public String toString() {
        return "AuthTokenDefinition{" +
               "name='" + name + '\'' +
               ", description='" + description + '\'' +
               ", priority=" + priority +
               ", inputFormatRegex='" + inputFormatRegex + '\'' +
               ", maxAttempts=" + maxAttempts +
               '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        AuthTokenDefinition that = (AuthTokenDefinition) o;
        
        return name != null ? name.equals(that.name) : that.name == null;
    }
    
    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
} 