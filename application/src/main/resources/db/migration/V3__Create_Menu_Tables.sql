-- V3__Create_Menu_Tables.sql
-- Menu module tables: Categories and Products

CREATE TABLE menu.categories (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    name            VARCHAR(100) NOT NULL,
    description     TEXT,
    sort_order      SMALLINT NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ
);

CREATE INDEX idx_categories_tenant_id ON menu.categories(tenant_id);
CREATE INDEX idx_categories_is_active ON menu.categories(is_active);
CREATE INDEX idx_categories_tenant_sort ON menu.categories(tenant_id, sort_order);

CREATE TABLE menu.products (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    category_id     UUID,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    price           NUMERIC(10, 2) NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    metadata        JSONB NOT NULL DEFAULT '{}',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ
);

CREATE INDEX idx_products_tenant_id ON menu.products(tenant_id);
CREATE INDEX idx_products_category_id ON menu.products(category_id);
CREATE INDEX idx_products_is_active ON menu.products(is_active);
CREATE INDEX idx_products_tenant_active ON menu.products(tenant_id, is_active);
