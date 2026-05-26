package com.bistrodungu.menu.infrastructure.persistence;

import com.bistrodungu.shared.infrastructure.persistence.BaseEntity;
import com.bistrodungu.shared.domain.vo.TenantId;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "products", schema = "menu")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductEntity extends BaseEntity {
    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private CategoryEntity category;

    @Column(name = "category_id", insertable = false, updatable = false)
    private Long categoryId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(columnDefinition = "jsonb")
    private String metadata; // For additional data (images, tags, etc.)

    public ProductEntity(TenantId tenantId, String name, BigDecimal price) {
        super(tenantId);
        this.name = name;
        this.price = price;
        this.isActive = true;
        this.metadata = "{}";
    }
}
