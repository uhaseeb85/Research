package com.bank.ivr.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class IvrAuthenticationApplication {
    public static void main(String[] args) {
        SpringApplication.run(IvrAuthenticationApplication.class, args);
    }
} 