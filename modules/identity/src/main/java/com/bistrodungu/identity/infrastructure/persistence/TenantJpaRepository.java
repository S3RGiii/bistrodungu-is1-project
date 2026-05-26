package com.bistrodungu.identity.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantJpaRepository extends JpaRepository<TenantEntity, UUID> {
    Optional<TenantEntity> findBySlug(String slug);
}
