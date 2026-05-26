package com.bistrodungu.identity.domain.event;

import com.bistrodungu.identity.domain.vo.TenantId;
import com.bistrodungu.identity.domain.vo.UserId;
import com.bistrodungu.identity.domain.vo.UserRole;
import com.bistrodungu.shared.domain.event.DomainEvent;

/**
 * UserCreatedEvent
 */
public class UserCreatedEvent extends DomainEvent {
    private final UserId userId;
    private final TenantId tenantId;
    private final String email;
    private final String fullName;
    private final UserRole role;

    public UserCreatedEvent(UserId userId, TenantId tenantId, String email, String fullName, UserRole role) {
        super();
        this.userId = userId;
        this.tenantId = tenantId;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
    }

    public UserId getUserId() {
        return userId;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public UserRole getRole() {
        return role;
    }
}
