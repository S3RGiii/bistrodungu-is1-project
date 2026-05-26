package com.bistrodungu.identity.infrastructure.persistence;

import com.bistrodungu.shared.infrastructure.persistence.BaseEntity;
import com.bistrodungu.shared.domain.vo.TenantId;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Tenant Entity - represents a restaurant/organization.
 * Multi-tenancy foundation for BistroDungu.
 */
@Entity
@Table(name = "tenants", schema = "identity")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantEntity extends BaseEntity {
    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String slug;

    @Column(nullable = false)
    private String timezone;

    @Column(length = 3, nullable = false)
    private String currency;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(columnDefinition = "jsonb")
    private String config;

    public TenantEntity(String name, String slug, String timezone, String currency) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.slug = slug;
        this.timezone = timezone;
        this.currency = currency;
        this.isActive = true;
        this.config = "{}";
    }
}
