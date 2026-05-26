package com.bistrodungu.menu.infrastructure.persistence;

import com.bistrodungu.shared.infrastructure.persistence.BaseEntity;
import com.bistrodungu.shared.domain.vo.TenantId;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categories", schema = "menu")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryEntity extends BaseEntity {
    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    public CategoryEntity(TenantId tenantId, String name) {
        super(tenantId);
        this.name = name;
        this.sortOrder = 0;
        this.isActive = true;
    }
}
