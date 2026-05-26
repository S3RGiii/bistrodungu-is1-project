package com.bistrodungu.identity.domain.entity;

import com.bistrodungu.identity.domain.vo.UserId;
import com.bistrodungu.identity.domain.vo.UserRole;
import com.bistrodungu.identity.domain.vo.TenantId;
import java.time.Instant;

/**
 * User Entity (Domain Entity, not JPA Entity)
 * Belongs to Tenant Aggregate but can exist independently
 */
public class User {
    private final UserId id;
    private final TenantId tenantId;
    private String email;
    private String passwordHash;
    private String fullName;
    private UserRole role;
    private boolean isActive;
    private String pinHash;
    private Instant createdAt;
    private Instant updatedAt;

    protected User() {
        this.id = null;
        this.tenantId = null;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    private User(UserId id, TenantId tenantId, String email, String passwordHash,
                String fullName, UserRole role) {
        this.id = id;
        this.tenantId = tenantId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
        this.isActive = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Factory method to create a new User
     */
    public static User create(TenantId tenantId, String email, String passwordHash,
                             String fullName, UserRole role) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be blank");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("Password hash cannot be blank");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Full name cannot be blank");
        }
        return new User(UserId.generate(), tenantId, email, passwordHash, fullName, role);
    }

    /**
     * Restore User from persistence
     */
    public static User restore(UserId id, TenantId tenantId, String email, String passwordHash,
                              String fullName, UserRole role, boolean isActive, String pinHash,
                              Instant createdAt, Instant updatedAt) {
        User user = new User(id, tenantId, email, passwordHash, fullName, role);
        user.isActive = isActive;
        user.pinHash = pinHash;
        user.createdAt = createdAt;
        user.updatedAt = updatedAt;
        return user;
    }

    public UserId getId() {
        return id;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public UserRole getRole() {
        return role;
    }

    public boolean isActive() {
        return isActive;
    }

    public String getPinHash() {
        return pinHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void updateProfile(String fullName, String email) {
        if (fullName != null && !fullName.isBlank()) {
            this.fullName = fullName;
        }
        if (email != null && !email.isBlank()) {
            this.email = email;
        }
        this.updatedAt = Instant.now();
    }

    public void updatePassword(String newPasswordHash) {
        if (newPasswordHash == null || newPasswordHash.isBlank()) {
            throw new IllegalArgumentException("Password hash cannot be blank");
        }
        this.passwordHash = newPasswordHash;
        this.updatedAt = Instant.now();
    }

    public void setPin(String pinHash) {
        this.pinHash = pinHash;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.isActive = false;
        this.updatedAt = Instant.now();
    }

    public void activate() {
        this.isActive = true;
        this.updatedAt = Instant.now();
    }

    public void changeRole(UserRole newRole) {
        if (newRole == null) {
            throw new IllegalArgumentException("Role cannot be null");
        }
        this.role = newRole;
        this.updatedAt = Instant.now();
    }
}
