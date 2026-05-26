package com.bistrodungu.identity.domain.repository;

import com.bistrodungu.identity.domain.entity.User;
import com.bistrodungu.identity.domain.vo.UserId;
import com.bistrodungu.identity.domain.vo.TenantId;
import java.util.Optional;

/**
 * Repository Port for User
 * This is a PORT - the interface contract
 * Implementation is in infrastructure layer
 */
public interface UserRepository {
    void save(User user);

    Optional<User> findById(UserId id, TenantId tenantId);

    Optional<User> findByEmail(String email, TenantId tenantId);

    void delete(User user);
}
