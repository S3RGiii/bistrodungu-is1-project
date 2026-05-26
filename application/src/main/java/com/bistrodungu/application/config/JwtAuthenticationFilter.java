package com.bistrodungu.application.config;

import com.bistrodungu.identity.infrastructure.security.JwtTokenProvider;
import com.bistrodungu.shared.domain.vo.TenantId;
import com.bistrodungu.shared.infrastructure.multitenancy.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

/**
 * JWT Authentication Filter
 * Extracts JWT token from Authorization header and validates it.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = extractJwtFromRequest(request);

            if (jwt != null && jwtTokenProvider.isTokenValid(jwt)) {
                var userId = jwtTokenProvider.extractUserId(jwt);
                var tenantId = jwtTokenProvider.extractTenantId(jwt);
                var role = jwtTokenProvider.extractRole(jwt);

                // Set tenant context for this request
                TenantContext tenantContext = (TenantContext) request.getServletContext()
                        .getAttribute("tenantContext");
                if (tenantContext != null) {
                    tenantContext.setTenant(TenantId.from(tenantId));
                }

                // Create authentication token
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId.toString(),
                                null,
                                new ArrayList<>()
                        );
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // Set tenant ID as header for downstream filters
                request.setAttribute("X-Tenant-ID", tenantId.toString());
                request.setAttribute("X-User-ID", userId.toString());
                request.setAttribute("X-User-Role", role);

                log.debug("JWT token validated for user: {} tenant: {}", userId, tenantId);
            }
        } catch (Exception e) {
            log.error("JWT authentication error: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
