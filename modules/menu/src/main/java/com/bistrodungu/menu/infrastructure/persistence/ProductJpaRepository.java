package com.bistrodungu.menu.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductJpaRepository extends JpaRepository<ProductEntity, UUID> {
    List<ProductEntity> findByTenantIdAndIsActiveTrue(UUID tenantId);

    List<ProductEntity> findByCategoryIdAndTenantIdAndIsActiveTrue(UUID categoryId, UUID tenantId);

    List<ProductEntity> findByTenantId(UUID tenantId);
}
