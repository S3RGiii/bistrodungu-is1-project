package com.bistrodungu.identity.domain.repository;

import com.bistrodungu.identity.domain.aggregate.Tenant;
import com.bistrodungu.identity.domain.vo.TenantId;
import java.util.Optional;

/**
 * Repository Port for Tenant
 * This is a PORT - the interface contract
 * Implementation is in infrastructure layer
 */
public interface TenantRepository {
    void save(Tenant tenant);

    Optional<Tenant> findById(TenantId id);

    Optional<Tenant> findBySlug(String slug);

    void delete(Tenant tenant);
}
