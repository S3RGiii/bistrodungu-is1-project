package com.bistrodungu.shared.infrastructure.persistence;

import com.bistrodungu.shared.domain.vo.TenantId;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all JPA entities.
 * Provides standard audit fields and multi-tenancy support.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false)
    protected UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    protected UUID tenantId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    protected Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    protected Instant updatedAt;

    @Column(name = "deleted_at")
    protected Instant deletedAt;

    protected BaseEntity() {
    }

    protected BaseEntity(TenantId tenantId) {
        this.tenantId = tenantId.value();
    }

    public TenantId getTenantIdValue() {
        return TenantId.from(this.tenantId);
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    public void markAsDeleted() {
        this.deletedAt = Instant.now();
    }
}
