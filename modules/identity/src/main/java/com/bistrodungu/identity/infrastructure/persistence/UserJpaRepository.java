package com.bistrodungu.identity.infrastructure.persistence;

import com.bistrodungu.shared.domain.vo.TenantId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmailAndTenantId(String email, UUID tenantId);

    Optional<UserEntity> findByIdAndTenantId(UUID id, UUID tenantId);
}
