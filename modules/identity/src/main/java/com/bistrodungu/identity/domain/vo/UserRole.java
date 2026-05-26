package com.bistrodungu.identity.domain.vo;

import java.util.Set;

/**
 * Value Object representing User Role
 */
public enum UserRole {
    ADMIN, MANAGER, WAITER, CASHIER, KITCHEN;

    public static final Set<UserRole> ALL_ROLES = Set.of(values());

    public boolean canManageUsers() {
        return this == ADMIN || this == MANAGER;
    }

    public boolean canAccessKDS() {
        return this == KITCHEN || this == MANAGER || this == ADMIN;
    }

    public boolean canAccessPOS() {
        return this != KITCHEN;
    }
}
