package com.bistrodungu.identity.application.port.in;

/**
 * Create Tenant Use Case - Input Port
 * Defines the interface for creating a tenant
 */
public interface CreateTenantUseCase {
    CreateTenantCommand execute(CreateTenantCommand command);

    record CreateTenantCommand(
            String name,
            String slug,
            String timezone,
            String currency
    ) {}

    record CreateTenantResult(
            String id,
            String name,
            String slug
    ) {}
}
