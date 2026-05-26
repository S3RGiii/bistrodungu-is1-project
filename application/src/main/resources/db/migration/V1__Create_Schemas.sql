-- V1__Create_Tenant_Schema.sql
-- Initial schema creation for multi-tenancy support

CREATE SCHEMA IF NOT EXISTS identity;
CREATE SCHEMA IF NOT EXISTS menu;
CREATE SCHEMA IF NOT EXISTS floor;
CREATE SCHEMA IF NOT EXISTS orders;
CREATE SCHEMA IF NOT EXISTS kds;
CREATE SCHEMA IF NOT EXISTS inventory;
CREATE SCHEMA IF NOT EXISTS billing;
CREATE SCHEMA IF NOT EXISTS reporting;

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Enable JSONB support
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
