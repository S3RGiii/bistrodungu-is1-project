package com.bistrodungu.identity.infrastructure.persistence;

import com.bistrodungu.shared.infrastructure.persistence.BaseEntity;
import com.bistrodungu.shared.domain.vo.TenantId;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * User Entity - represents a user within a tenant.
 * Contains authentication and role information.
 */
@Entity
@Table(name = "users", schema = "identity", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tenant_id", "email"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity extends BaseEntity {
    @Column(nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String role; // ADMIN | MANAGER | WAITER | CASHIER | KITCHEN

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "pin_hash")
    private String pinHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", insertable = false, updatable = false)
    private TenantEntity tenant;

    public UserEntity(TenantId tenantId, String email, String passwordHash, String fullName, String role) {
        super(tenantId);
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
        this.isActive = true;
    }

    public enum Role {
        ADMIN, MANAGER, WAITER, CASHIER, KITCHEN
    }
}
