package com.bistrodungu.identity.infrastructure.web;

import com.bistrodungu.identity.application.service.AuthenticationService;
import com.bistrodungu.shared.domain.vo.TenantId;
import com.bistrodungu.shared.infrastructure.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthenticationService.AuthTokenResponse>> login(
            @RequestParam UUID tenantId,
            @RequestParam String email,
            @RequestParam String password
    ) {
        try {
            AuthenticationService.AuthTokenResponse response = authenticationService.authenticate(
                    tenantId,
                    email,
                    password
            );
            return ResponseEntity.ok(ApiResponse.ok(response, "Login successful"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(
            @RequestParam UUID tenantId,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String fullName,
            @RequestParam(defaultValue = "WAITER") String role
    ) {
        try {
            authenticationService.createUser(
                    TenantId.from(tenantId),
                    email,
                    password,
                    fullName,
                    role
            );
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.created("User registered successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}
