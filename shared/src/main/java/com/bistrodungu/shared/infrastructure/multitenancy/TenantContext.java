package com.bistrodungu.shared.infrastructure.multitenancy;

import com.bistrodungu.shared.domain.vo.TenantId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * TenantContext holds the current tenant information for the request.
 * This is stored in ThreadLocal to ensure each request sees only its tenant's data.
 */
@Slf4j
@Component
public class TenantContext {
    private static final ThreadLocal<TenantId> TENANT_ID = new ThreadLocal<>();

    public void setTenant(TenantId tenantId) {
        log.debug("Setting tenant context: {}", tenantId);
        TENANT_ID.set(tenantId);
    }

    public TenantId getTenant() {
        TenantId tenantId = TENANT_ID.get();
        if (tenantId == null) {
            throw new IllegalStateException("No tenant context set for current request");
        }
        return tenantId;
    }

    public TenantId getTenantOrNull() {
        return TENANT_ID.get();
    }

    public void clear() {
        TENANT_ID.remove();
    }

    public boolean isSet() {
        return TENANT_ID.get() != null;
    }
}
