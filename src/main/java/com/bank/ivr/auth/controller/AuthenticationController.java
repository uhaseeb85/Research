package com.bank.ivr.auth.controller;

import com.bank.ivr.auth.model.AuthenticationRequest;
import com.bank.ivr.auth.model.AuthenticationResponse;
import com.bank.ivr.auth.model.Brand;
import com.bank.ivr.auth.model.TokenMethod;
import com.bank.ivr.auth.service.AuthenticationService;
import com.bank.ivr.auth.service.BrandService;
import com.bank.ivr.auth.service.HealthCheckService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AuthenticationController {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private BrandService brandService;

    @Autowired
    private HealthCheckService healthCheckService;

    @PostMapping("/customer")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @Valid @RequestBody AuthenticationRequest request) {
        logger.info("Received authentication request for brand: {}", request.getBrand());
        
        if (request.getBrand() == null) {
            logger.error("Brand is required in authentication request");
            return ResponseEntity.badRequest().build();
        }

        try {
            AuthenticationResponse response = authenticationService.authenticate(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid request parameters: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Authentication error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        try {
            boolean isHealthy = healthCheckService.isHealthy();
            if (isHealthy) {
                return ResponseEntity.ok("Service is healthy");
            } else {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Service is unhealthy");
            }
        } catch (Exception e) {
            logger.error("Health check error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/brands")
    public ResponseEntity<List<Brand>> getSupportedBrands() {
        try {
            List<Brand> brands = brandService.getSupportedBrands();
            return ResponseEntity.ok(brands);
        } catch (Exception e) {
            logger.error("Error retrieving supported brands: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/methods/{brand}")
    public ResponseEntity<List<TokenMethod>> getAuthenticationMethods(
            @PathVariable String brand) {
        try {
            if (!brandService.isBrandSupported(brand)) {
                logger.error("Unsupported brand requested: {}", brand);
                return ResponseEntity.badRequest().build();
            }

            List<TokenMethod> methods = brandService.getAuthenticationMethods(brand);
            return ResponseEntity.ok(methods);
        } catch (Exception e) {
            logger.error("Error retrieving authentication methods for brand {}: {}", brand, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @RequestMapping(method = RequestMethod.OPTIONS)
    public ResponseEntity<?> handleOptions() {
        return ResponseEntity.ok().build();
    }
}
