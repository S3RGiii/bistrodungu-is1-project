package com.bistrodungu.identity.application.service;

import com.bistrodungu.identity.infrastructure.persistence.UserEntity;
import com.bistrodungu.identity.infrastructure.persistence.UserJpaRepository;
import com.bistrodungu.identity.infrastructure.persistence.TenantEntity;
import com.bistrodungu.identity.infrastructure.persistence.TenantJpaRepository;
import com.bistrodungu.identity.infrastructure.security.JwtTokenProvider;
import com.bistrodungu.shared.domain.vo.TenantId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Authentication service - handles user login, registration, and token generation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserJpaRepository userRepository;
    private final TenantJpaRepository tenantRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    public AuthTokenResponse authenticate(UUID tenantId, String email, String password) {
        UserEntity user = userRepository.findByEmailAndTenantId(email, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!user.isActive()) {
            throw new IllegalArgumentException("User account is inactive");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        String token = jwtTokenProvider.generateToken(
                user.getId(),
                tenantId,
                user.getEmail(),
                user.getRole()
        );

        log.info("User {} authenticated successfully", email);
        return new AuthTokenResponse(token, user.getId(), email, user.getFullName(), user.getRole());
    }

    public void createUser(TenantId tenantId, String email, String password, String fullName, String role) {
        TenantEntity tenant = tenantRepository.findById(tenantId.value())
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));

        if (userRepository.findByEmailAndTenantId(email, tenantId.value()).isPresent()) {
            throw new IllegalArgumentException("User with this email already exists");
        }

        UserEntity user = new UserEntity(
                tenantId,
                email,
                passwordEncoder.encode(password),
                fullName,
                role
        );

        userRepository.save(user);
        log.info("User {} created for tenant {}", email, tenantId);
    }

    public record AuthTokenResponse(
            String token,
            UUID userId,
            String email,
            String fullName,
            String role
    ) {}
}
