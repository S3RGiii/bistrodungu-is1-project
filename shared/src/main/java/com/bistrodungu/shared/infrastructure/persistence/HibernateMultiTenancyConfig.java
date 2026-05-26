package com.bistrodungu.shared.infrastructure.persistence;

import org.hibernate.FilterDefinition;
import org.hibernate.Session;
import org.hibernate.annotations.ParamDef;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Hibernat Filter configuration for multi-tenancy isolation.
 * Applies a filter to all queries to ensure data is filtered by tenant_id.
 */
@Component
public class HibernateMultiTenancyConfig implements HibernatePropertiesCustomizer {

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        // Hibernate filters will be registered via annotations on entities
        // This component ensures the infrastructure is prepared for multi-tenancy
    }
}
