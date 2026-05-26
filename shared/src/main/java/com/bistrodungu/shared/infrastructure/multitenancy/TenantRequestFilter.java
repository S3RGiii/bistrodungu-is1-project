package com.bistrodungu.shared.infrastructure.multitenancy;

import com.bistrodungu.shared.domain.vo.TenantId;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Filter to set the current tenant in TenantContext for each request.
 * The tenant_id should come from JWT claims or request header.
 * This is extracted in the security layer.
 */
@Slf4j
@Component
public class TenantRequestFilter extends OncePerRequestFilter {
    @Autowired
    private TenantContext tenantContext;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // Extract tenant from request (this is set by SecurityFilter after JWT verification)
            String tenantIdHeader = request.getHeader("X-Tenant-ID");
            if (tenantIdHeader != null) {
                tenantContext.setTenant(TenantId.from(tenantIdHeader));
                log.debug("Tenant context set from header: {}", tenantIdHeader);
            }
            filterChain.doFilter(request, response);
        } finally {
            tenantContext.clear();
        }
    }
}
