package com.bistrodungu.identity.domain.vo;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object representing a Tenant ID
 */
public record TenantId(UUID value) {
    public TenantId {
        Objects.requireNonNull(value, "Tenant ID cannot be null");
    }

    public static TenantId generate() {
        return new TenantId(UUID.randomUUID());
    }

    public static TenantId from(UUID uuid) {
        return new TenantId(uuid);
    }

    public static TenantId from(String uuidString) {
        return new TenantId(UUID.fromString(uuidString));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
