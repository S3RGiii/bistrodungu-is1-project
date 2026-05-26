package com.bistrodungu.identity.domain.vo;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object representing a User ID
 */
public record UserId(UUID value) {
    public UserId {
        Objects.requireNonNull(value, "User ID cannot be null");
    }

    public static UserId generate() {
        return new UserId(UUID.randomUUID());
    }

    public static UserId from(UUID uuid) {
        return new UserId(uuid);
    }

    public static UserId from(String uuidString) {
        return new UserId(UUID.fromString(uuidString));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
