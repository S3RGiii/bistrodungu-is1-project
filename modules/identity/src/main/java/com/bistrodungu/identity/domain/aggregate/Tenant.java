package com.bistrodungu.identity.domain.aggregate;

import com.bistrodungu.identity.domain.vo.TenantId;
import com.bistrodungu.shared.domain.aggregate.AggregateRoot;

/**
 * Tenant Aggregate Root
 * Represents a restaurant/organization in BistroDungu
 * ROOT: Tenant (no children aggregates in this context)
 */
public class Tenant extends AggregateRoot<TenantId> {
    private TenantId id;
    private String name;
    private String slug;
    private String timezone;
    private String currency;
    private boolean isActive;
    private String config;

    protected Tenant() {
    }

    private Tenant(TenantId id, String name, String slug, String timezone, String currency) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.timezone = timezone;
        this.currency = currency;
        this.isActive = true;
        this.config = "{}";
    }

    /**
     * Factory method to create a new Tenant
     */
    public static Tenant create(String name, String slug, String timezone, String currency) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tenant name cannot be blank");
        }
        if (slug == null || slug.isBlank()) {
            throw new IllegalArgumentException("Tenant slug cannot be blank");
        }
        return new Tenant(TenantId.generate(), name, slug, timezone, currency);
    }

    /**
     * Restore a Tenant from database (non-factory constructor)
     */
    public static Tenant restore(TenantId id, String name, String slug, String timezone,
                                String currency, boolean isActive, String config) {
        Tenant tenant = new Tenant(id, name, slug, timezone, currency);
        tenant.isActive = isActive;
        tenant.config = config;
        return tenant;
    }

    @Override
    public TenantId getId() {
        return id;
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

    public boolean isActive() {
        return isActive;
    }

    public String getConfig() {
        return config;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void activate() {
        this.isActive = true;
    }

    public void updateConfig(String newConfig) {
        if (newConfig == null || newConfig.isBlank()) {
            throw new IllegalArgumentException("Config cannot be blank");
        }
        this.config = newConfig;
    }
}
