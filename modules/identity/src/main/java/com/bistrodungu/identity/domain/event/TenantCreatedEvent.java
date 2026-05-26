package com.bistrodungu.identity.domain.event;

import com.bistrodungu.identity.domain.vo.TenantId;
import com.bistrodungu.shared.domain.event.DomainEvent;

/**
 * TenantCreatedEvent
 */
public class TenantCreatedEvent extends DomainEvent {
    private final TenantId tenantId;
    private final String name;
    private final String slug;
    private final String timezone;
    private final String currency;

    public TenantCreatedEvent(TenantId tenantId, String name, String slug, String timezone, String currency) {
        super();
        this.tenantId = tenantId;
        this.name = name;
        this.slug = slug;
        this.timezone = timezone;
        this.currency = currency;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public String getTimezone() {
        return timezone;
    }

    public String getCurrency() {
        return currency;
    }
}
