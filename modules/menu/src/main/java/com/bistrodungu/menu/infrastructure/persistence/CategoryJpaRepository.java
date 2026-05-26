package com.bistrodungu.menu.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CategoryJpaRepository extends JpaRepository<CategoryEntity, UUID> {
    List<CategoryEntity> findByTenantIdAndIsActiveTrue(UUID tenantId);

    List<CategoryEntity> findByTenantIdOrderBySortOrder(UUID tenantId);
}
