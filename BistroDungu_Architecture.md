# BistroDungu — Arquitectura Enterprise POS
## Principal Software Architect Design Document v1.0

---

# TABLA DE CONTENIDOS

1. Visión General del Sistema
2. Bounded Contexts y Módulos
3. Arquitectura del Monolito Modular
4. Diseño DDD + Clean Architecture
5. Modelo Relacional PostgreSQL
6. Entidades Principales y Agregados
7. Flujos Operacionales Completos
8. Diseño del KDS
9. Diseño de Inventario por Ingredientes
10. Eventos Internos del Sistema
11. Estrategia Redis
12. Comunicación Realtime
13. Diseño Frontend Angular 19
14. APIs REST Principales
15. Seguridad y RBAC
16. Roadmap de Implementación
17. Riesgos Arquitectónicos
18. Estrategia Futura para Microservicios
19. Estructura del Repositorio

---

# 1. VISIÓN GENERAL DEL SISTEMA

## 1.1 Propósito Arquitectónico

BistroDungu es una plataforma POS enterprise para restaurantes modernos casuales, diseñada
como **Monolito Modular** con separación explícita de Bounded Contexts, eventos internos
desacoplados y preparación estructural para extracción futura a microservicios.

**Decisión clave — ¿Por qué Monolito Modular y no Microservicios desde el inicio?**

- El equipo inicial no justifica el overhead operacional de microservicios.
- La latencia de red entre servicios en operaciones POS es inaceptable (ej: cobrar + descontar
  inventario debe ser transaccional o compensado).
- Los límites de dominio no están completamente validados. El monolito modular permite refinar
  fronteras antes de cortarlas.
- Facilita deployment en restaurantes con infraestructura limitada (on-premise + cloud híbrido).
- La extracción futura es viable porque los módulos ya tienen interfaces explícitas y se comunican
  solo por eventos internos o APIs de módulo, nunca por dependencias directas entre capas de dominio.

## 1.2 Principios de Diseño No Negociables

1. **Isolación de dominios**: ningún módulo accede a la capa de dominio de otro módulo directamente.
2. **Consistencia eventual controlada**: operaciones críticas (cobro + inventario) usan sagas o
   transacciones distribuidas compensadas.
3. **Trazabilidad total**: toda mutación de estado registra quién, cuándo, desde qué estado y hacia
   qué estado.
4. **Reversibilidad operacional**: los estados del KDS y del inventario admiten reversión con
   compensación.
5. **Multi-tenant desde el día uno**: todo modelo incluye `tenant_id` para soporte SaaS futuro.
6. **Eventos como contrato**: los módulos se comunican por eventos tipados, no por llamadas directas.

## 1.3 Vista de Alto Nivel

```
┌─────────────────────────────────────────────────────────────────┐
│                        CLIENTES                                  │
│   Angular POS  │  Angular KDS  │  QR Self-Service  │  Admin     │
└────────┬────────────────┬──────────────────┬────────────────────┘
         │ HTTP/REST       │ WebSocket        │ HTTP/REST
┌────────▼────────────────▼──────────────────▼────────────────────┐
│                    API Gateway Layer                              │
│              (Spring Security + JWT + RBAC)                      │
├──────────────────────────────────────────────────────────────────┤
│                   Application Core (Spring Boot)                  │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐           │
│  │  Tables  │ │  Orders  │ │   KDS    │ │Inventory │           │
│  │  Module  │ │  Module  │ │  Module  │ │  Module  │           │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘           │
│       │             │            │              │                 │
│  ┌────▼─────────────▼────────────▼──────────────▼──────────┐    │
│  │              Internal Event Bus (Spring ApplicationEvent) │    │
│  └────────────────────────────────────────────────────────--┘    │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐           │
│  │Reservat. │ │ Billing  │ │Reporting │ │  Menu    │           │
│  │  Module  │ │  Module  │ │  Module  │ │  Module  │           │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘           │
├──────────────────────────────────────────────────────────────────┤
│  Infrastructure Layer                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │  PostgreSQL   │  │    Redis     │  │  WebSocket   │          │
│  │  (Primary)    │  │  (Cache +    │  │  (STOMP/     │          │
│  │              │  │  PubSub +    │  │  SockJS)     │          │
│  │              │  │  Sessions)   │  │              │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
└──────────────────────────────────────────────────────────────────┘
```

---

# 2. BOUNDED CONTEXTS Y MÓDULOS

## 2.1 Mapa de Contextos

```
┌─────────────────────────────────────────────────────────────────┐
│                    BOUNDED CONTEXTS                              │
│                                                                  │
│  ┌───────────────┐    ┌───────────────┐    ┌───────────────┐   │
│  │  FLOOR MGT    │    │  ORDER MGMT   │    │   KITCHEN     │   │
│  │               │    │               │    │               │   │
│  │ Tables        │◄──►│ Orders        │───►│ KDS           │   │
│  │ Zones         │    │ OrderItems    │    │ Stations      │   │
│  │ Capacity      │    │ OrderOrigin   │    │ Prep Times    │   │
│  └───────────────┘    └───────┬───────┘    └───────────────┘   │
│                               │                                  │
│  ┌───────────────┐    ┌───────▼───────┐    ┌───────────────┐   │
│  │  MENU CATALOG │    │    BILLING    │    │  INVENTORY    │   │
│  │               │───►│               │    │               │   │
│  │ Products      │    │ Tickets       │    │ Ingredients   │   │
│  │ Recipes       │    │ Payments      │    │ Stock         │   │
│  │ Modifiers     │    │ Split Bills   │    │ Movements     │   │
│  └───────────────┘    └───────────────┘    └───────────────┘   │
│                                                                  │
│  ┌───────────────┐    ┌───────────────┐    ┌───────────────┐   │
│  │ RESERVATIONS  │    │   REPORTING   │    │  IDENTITY     │   │
│  │               │    │               │    │               │   │
│  │ Bookings      │    │ Metrics       │    │ Users         │   │
│  │ Guests        │    │ Dashboards    │    │ Roles         │   │
│  │ Waitlist      │    │ Analytics     │    │ Tenants       │   │
│  └───────────────┘    └───────────────┘    └───────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

## 2.2 Relaciones entre Contextos

| Relación | Tipo | Mecanismo |
|---|---|---|
| Order → KDS | Downstream | Evento `OrderItemsReadyForKitchen` |
| KDS → Inventory | Downstream | Evento `KdsItemCompleted` |
| Order → Billing | Conformist | Evento `OrderClosed` |
| Menu → Order | Upstream (ACL) | API de módulo (Anti-Corruption Layer) |
| Tables → Order | Partnership | Evento bidireccional |
| Reservations → Tables | Customer/Supplier | Evento `ReservationConfirmed` |

---

# 3. ARQUITECTURA DEL MONOLITO MODULAR

## 3.1 Estructura de Paquetes Java

```
com.bistrodungu/
├── shared/
│   ├── domain/
│   │   ├── event/         # DomainEvent base, EventBus interface
│   │   ├── vo/            # ValueObjects compartidos (Money, TenantId, UserId)
│   │   └── aggregate/     # AggregateRoot base
│   ├── infrastructure/
│   │   ├── event/         # SpringApplicationEventBus implementation
│   │   ├── persistence/   # JPA base config, BaseEntity
│   │   └── security/      # JWT, RBAC filters
│   └── api/
│       └── response/      # ApiResponse wrapper estándar
│
├── modules/
│   ├── identity/          # Bounded Context: Identidad
│   ├── menu/              # Bounded Context: Catálogo
│   ├── tables/            # Bounded Context: Piso/Mesas
│   ├── reservations/      # Bounded Context: Reservas
│   ├── orders/            # Bounded Context: Pedidos (CORE)
│   ├── kds/               # Bounded Context: Cocina
│   ├── inventory/         # Bounded Context: Inventario
│   ├── billing/           # Bounded Context: Facturación
│   └── reporting/         # Bounded Context: Reportes
│
└── application/           # Spring Boot Application, configuración global
    ├── BistroDunguApp.java
    └── config/
```

## 3.2 Estructura Interna de Cada Módulo (Clean Architecture)

```
modules/orders/
├── domain/
│   ├── aggregate/
│   │   └── Order.java              # Aggregate Root
│   ├── entity/
│   │   ├── OrderItem.java
│   │   └── OrderItemModifier.java
│   ├── vo/
│   │   ├── OrderId.java
│   │   ├── OrderStatus.java        # Enum con máquina de estados
│   │   ├── OrderOrigin.java        # QR | WAITER | POS | ONLINE
│   │   └── TableReference.java
│   ├── event/
│   │   ├── OrderCreatedEvent.java
│   │   ├── OrderItemAddedEvent.java
│   │   ├── OrderClosedEvent.java
│   │   └── OrderStatusChangedEvent.java
│   ├── repository/
│   │   └── OrderRepository.java    # Puerto (interface), no JPA
│   └── service/
│       └── OrderDomainService.java # Lógica que involucra múltiples agregados
│
├── application/
│   ├── port/
│   │   ├── in/
│   │   │   ├── CreateOrderUseCase.java
│   │   │   ├── AddItemToOrderUseCase.java
│   │   │   └── CloseOrderUseCase.java
│   │   └── out/
│   │       ├── LoadOrderPort.java
│   │       ├── SaveOrderPort.java
│   │       └── MenuQueryPort.java  # Anti-Corruption Layer hacia Menu
│   └── service/
│       ├── CreateOrderService.java
│       └── CloseOrderService.java
│
├── infrastructure/
│   ├── persistence/
│   │   ├── OrderJpaRepository.java
│   │   ├── OrderEntity.java        # JPA Entity (diferente al Domain Entity)
│   │   └── OrderPersistenceAdapter.java
│   ├── web/
│   │   └── OrderController.java
│   └── event/
│       └── OrderEventPublisher.java
│
└── OrderModuleConfig.java          # @Configuration del módulo
```

**Regla de dependencias (Dependency Rule):**
- `domain` no conoce a nadie.
- `application` conoce solo `domain`.
- `infrastructure` conoce `application` y `domain`.
- Otros módulos NUNCA importan clases de `domain` de otro módulo. Solo usan eventos o APIs de módulo.

---

# 4. DISEÑO DDD + CLEAN ARCHITECTURE

## 4.1 Agregados Principales

### Order (Aggregate Root — Módulo Orders)

```java
public class Order extends AggregateRoot<OrderId> {

    private OrderId id;
    private TenantId tenantId;
    private TableReference tableRef;      // ValueObject, no FK directa
    private OrderOrigin origin;           // QR | WAITER | POS | ONLINE
    private OrderStatus status;
    private List<OrderItem> items;        // Entidades hijas
    private Money subtotal;
    private UserId createdBy;
    private Instant createdAt;
    private Instant updatedAt;
    private List<StatusTransition> statusHistory; // Trazabilidad completa

    // Factory method — válida invariantes en creación
    public static Order create(TenantId tenant, TableReference table,
                               OrderOrigin origin, UserId user) {
        Order order = new Order();
        order.id = OrderId.generate();
        order.status = OrderStatus.DRAFT;
        order.origin = origin;
        // ... asignaciones
        order.registerEvent(new OrderCreatedEvent(order.id, tenant, table, origin));
        return order;
    }

    // Transición de estado — única entrada para cambios de status
    public void transitionTo(OrderStatus newStatus, UserId operatorId) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new InvalidOrderStateTransitionException(this.status, newStatus);
        }
        StatusTransition transition = StatusTransition.of(this.status, newStatus, operatorId);
        this.statusHistory.add(transition);
        OrderStatus previous = this.status;
        this.status = newStatus;
        this.registerEvent(new OrderStatusChangedEvent(id, previous, newStatus, operatorId));
    }

    // Agrega item — valida stock disponible via puerto
    public OrderItem addItem(MenuItemSnapshot menuItem, int quantity,
                             List<ModifierSnapshot> modifiers) {
        if (this.status != OrderStatus.OPEN && this.status != OrderStatus.DRAFT) {
            throw new OrderNotEditableException(this.id);
        }
        OrderItem item = OrderItem.create(menuItem, quantity, modifiers);
        this.items.add(item);
        this.recalculateSubtotal();
        this.registerEvent(new OrderItemAddedEvent(this.id, item));
        return item;
    }
}
```

### Máquina de Estados — Order

```
DRAFT ──────────────────────────────────────────────────────► CANCELLED
  │                                                               ▲
  ▼                                                               │
OPEN ──► SENT_TO_KITCHEN ──► PARTIALLY_READY ──► READY ──► CLOSED
  │              │                   │               │
  └──────────────┴───────────────────┴───────────────┴──► CANCELLED
```

```java
public enum OrderStatus {
    DRAFT, OPEN, SENT_TO_KITCHEN, PARTIALLY_READY, READY, CLOSED, CANCELLED;

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
        DRAFT,           Set.of(OPEN, CANCELLED),
        OPEN,            Set.of(SENT_TO_KITCHEN, CANCELLED),
        SENT_TO_KITCHEN, Set.of(PARTIALLY_READY, READY, CANCELLED),
        PARTIALLY_READY, Set.of(READY, CANCELLED),
        READY,           Set.of(CLOSED),
        CLOSED,          Set.of(),  // Terminal — solo con compensación explícita
        CANCELLED,       Set.of()   // Terminal
    );

    public boolean canTransitionTo(OrderStatus target) {
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }
}
```

## 4.2 ValueObjects Críticos

```java
// Money — nunca usar double para dinero
public record Money(BigDecimal amount, Currency currency) {
    public Money {
        Objects.requireNonNull(amount);
        Objects.requireNonNull(currency);
        if (amount.compareTo(BigDecimal.ZERO) < 0)
            throw new NegativeMoneyException();
    }
    public Money add(Money other) {
        assertSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }
    public Money multiply(int factor) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(factor)), this.currency);
    }
}

// MenuItemSnapshot — copia inmutable del producto al momento del pedido
// Crítico: el precio puede cambiar, el pedido debe conservar el precio original
public record MenuItemSnapshot(
    UUID menuItemId,
    String name,
    Money priceAtOrder,
    UUID recipeId        // Para descuento de inventario posterior
) {}
```

## 4.3 Anti-Corruption Layer (ACL)

El módulo Orders necesita datos del módulo Menu. La ACL evita que el dominio de Orders
conozca los detalles internos de Menu:

```java
// Puerto (interface) definido en Orders application layer
public interface MenuQueryPort {
    MenuItemSnapshot findMenuItemForOrder(UUID menuItemId, TenantId tenant);
}

// Implementación en infrastructure layer de Orders (no en Menu)
@Component
public class MenuQueryAdapter implements MenuQueryPort {
    private final MenuModuleApi menuApi;  // API de módulo, no acceso directo a BD

    @Override
    public MenuItemSnapshot findMenuItemForOrder(UUID menuItemId, TenantId tenant) {
        // Traduce el modelo de Menu al Snapshot que necesita Orders
        MenuItemDto dto = menuApi.getMenuItem(menuItemId, tenant);
        return new MenuItemSnapshot(dto.id(), dto.name(),
            Money.of(dto.price(), dto.currency()), dto.recipeId());
    }
}

// API pública del módulo Menu (única interfaz exportada)
@Component
public class MenuModuleApi {
    // Solo expone lo necesario. No expone repositorios, no expone entidades JPA.
    public MenuItemDto getMenuItem(UUID id, TenantId tenant) { ... }
    public List<MenuItemDto> getActiveMenuItems(TenantId tenant) { ... }
}
```

---

# 5. MODELO RELACIONAL POSTGRESQL

## 5.1 Convenciones del Esquema

- Todos los IDs son `UUID` (mejor para distribución futura, sin hot spots en índices).
- Toda tabla tiene `tenant_id UUID NOT NULL` para multi-tenancy.
- Timestamps en `TIMESTAMPTZ` (con timezone — restaurantes pueden cambiar zona horaria).
- Columnas `created_at`, `updated_at`, `deleted_at` (soft delete donde aplica).
- Historial de estados en tablas separadas de audit, no en la tabla principal.

## 5.2 Esquema Completo

```sql
-- ============================================================
-- SCHEMA: identity
-- ============================================================

CREATE SCHEMA identity;

CREATE TABLE identity.tenants (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    slug            VARCHAR(100) UNIQUE NOT NULL,
    timezone        VARCHAR(50) NOT NULL DEFAULT 'America/Bogota',
    currency        CHAR(3) NOT NULL DEFAULT 'COP',
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    config          JSONB NOT NULL DEFAULT '{}',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE identity.users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES identity.tenants(id),
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(255) NOT NULL,
    role            VARCHAR(50) NOT NULL,  -- ADMIN | MANAGER | WAITER | CASHIER | KITCHEN
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    pin_hash        VARCHAR(255),          -- PIN rápido para POS
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(tenant_id, email)
);

-- ============================================================
-- SCHEMA: floor (Gestión de Piso)
-- ============================================================

CREATE SCHEMA floor;

CREATE TABLE floor.zones (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    name            VARCHAR(100) NOT NULL,
    description     TEXT,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE floor.tables (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    zone_id         UUID NOT NULL REFERENCES floor.zones(id),
    number          VARCHAR(20) NOT NULL,
    capacity        SMALLINT NOT NULL,
    qr_code         VARCHAR(255) UNIQUE,   -- Token único para QR
    status          VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    -- AVAILABLE | OCCUPIED | RESERVED | CLEANING | BLOCKED
    position_x      FLOAT,                 -- Para mapa de planta visual
    position_y      FLOAT,
    current_order_id UUID,                 -- Denormalizado para acceso rápido
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(tenant_id, number)
);

CREATE TABLE floor.table_status_log (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    table_id        UUID NOT NULL REFERENCES floor.tables(id),
    from_status     VARCHAR(30) NOT NULL,
    to_status       VARCHAR(30) NOT NULL,
    changed_by      UUID NOT NULL,
    changed_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    reason          TEXT
);

-- ============================================================
-- SCHEMA: menu
-- ============================================================

CREATE SCHEMA menu;

CREATE TABLE menu.categories (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    name            VARCHAR(100) NOT NULL,
    description     TEXT,
    sort_order      SMALLINT NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE menu.products (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    category_id     UUID NOT NULL REFERENCES menu.categories(id),
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    price           NUMERIC(12,2) NOT NULL,
    tax_rate        NUMERIC(5,4) NOT NULL DEFAULT 0.19,
    image_url       TEXT,
    sku             VARCHAR(100),
    is_available    BOOLEAN NOT NULL DEFAULT TRUE,
    preparation_time_minutes SMALLINT,
    kds_station     VARCHAR(50),     -- grill | cold | bar | dessert
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE menu.modifiers_group (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    product_id      UUID NOT NULL REFERENCES menu.products(id),
    name            VARCHAR(100) NOT NULL,
    is_required     BOOLEAN NOT NULL DEFAULT FALSE,
    min_selections  SMALLINT NOT NULL DEFAULT 0,
    max_selections  SMALLINT NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE menu.modifiers (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id        UUID NOT NULL REFERENCES menu.modifiers_group(id),
    name            VARCHAR(100) NOT NULL,
    price_delta     NUMERIC(12,2) NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE
);

-- Recetas — vincula productos a ingredientes
CREATE TABLE menu.recipes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id      UUID NOT NULL UNIQUE REFERENCES menu.products(id),
    name            VARCHAR(255) NOT NULL,
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE menu.recipe_ingredients (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipe_id       UUID NOT NULL REFERENCES menu.recipes(id),
    ingredient_id   UUID NOT NULL,   -- FK a inventory.ingredients
    quantity        NUMERIC(12,4) NOT NULL,
    unit            VARCHAR(30) NOT NULL,  -- g | kg | ml | l | unit | tbsp | tsp
    waste_factor    NUMERIC(5,4) NOT NULL DEFAULT 0,  -- % de merma
    notes           TEXT,
    UNIQUE(recipe_id, ingredient_id)
);

-- ============================================================
-- SCHEMA: orders
-- ============================================================

CREATE SCHEMA orders;

CREATE TABLE orders.orders (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    table_id        UUID,            -- NULL para pedidos online
    zone_id         UUID,
    table_number    VARCHAR(20),     -- Denormalizado (snapshot)
    origin          VARCHAR(30) NOT NULL,  -- QR | WAITER | POS | ONLINE
    status          VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    -- DRAFT|OPEN|SENT_TO_KITCHEN|PARTIALLY_READY|READY|CLOSED|CANCELLED
    subtotal        NUMERIC(12,2) NOT NULL DEFAULT 0,
    tax_total       NUMERIC(12,2) NOT NULL DEFAULT 0,
    total           NUMERIC(12,2) NOT NULL DEFAULT 0,
    cover_count     SMALLINT NOT NULL DEFAULT 1,
    notes           TEXT,
    qr_session_id   VARCHAR(255),    -- Para pedidos QR
    created_by      UUID NOT NULL,
    waiter_id       UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    closed_at       TIMESTAMPTZ
);

CREATE INDEX idx_orders_tenant_status ON orders.orders(tenant_id, status);
CREATE INDEX idx_orders_table ON orders.orders(table_id) WHERE table_id IS NOT NULL;
CREATE INDEX idx_orders_created ON orders.orders(tenant_id, created_at DESC);

CREATE TABLE orders.order_items (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id        UUID NOT NULL REFERENCES orders.orders(id),
    product_id      UUID NOT NULL,   -- Snapshot: no FK, el producto puede cambiar
    product_name    VARCHAR(255) NOT NULL,
    recipe_id       UUID,            -- Snapshot para descuento de inventario
    unit_price      NUMERIC(12,2) NOT NULL,
    quantity        SMALLINT NOT NULL,
    subtotal        NUMERIC(12,2) NOT NULL,
    tax_amount      NUMERIC(12,2) NOT NULL DEFAULT 0,
    status          VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    -- PENDING|SENT|IN_PREPARATION|READY|DELIVERED|CANCELLED
    kds_station     VARCHAR(50),
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE orders.order_item_modifiers (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_item_id   UUID NOT NULL REFERENCES orders.order_items(id),
    modifier_id     UUID NOT NULL,
    modifier_name   VARCHAR(100) NOT NULL,
    price_delta     NUMERIC(12,2) NOT NULL DEFAULT 0
);

CREATE TABLE orders.order_status_log (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id        UUID NOT NULL REFERENCES orders.orders(id),
    from_status     VARCHAR(30),
    to_status       VARCHAR(30) NOT NULL,
    changed_by      UUID NOT NULL,
    changed_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    reason          TEXT,
    metadata        JSONB NOT NULL DEFAULT '{}'
);

-- ============================================================
-- SCHEMA: kds (Kitchen Display System)
-- ============================================================

CREATE SCHEMA kds;

CREATE TABLE kds.stations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    name            VARCHAR(100) NOT NULL,    -- grill | cold | bar | dessert
    display_name    VARCHAR(100) NOT NULL,
    color_hex       CHAR(7),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE kds.kds_tickets (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    order_id        UUID NOT NULL,
    order_item_id   UUID NOT NULL UNIQUE,
    station_id      UUID NOT NULL REFERENCES kds.stations(id),
    status          VARCHAR(30) NOT NULL DEFAULT 'QUEUED',
    -- QUEUED|IN_PREPARATION|COMPLETED|DELIVERED|CANCELLED
    priority        SMALLINT NOT NULL DEFAULT 5,
    sent_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    started_at      TIMESTAMPTZ,      -- Cuando cocina comienza
    completed_at    TIMESTAMPTZ,
    delivered_at    TIMESTAMPTZ,
    sla_minutes     SMALLINT,         -- SLA configurado para la estación
    operator_id     UUID,             -- Quién lo marcó
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_kds_station_status ON kds.kds_tickets(station_id, status);
CREATE INDEX idx_kds_order ON kds.kds_tickets(order_id);

CREATE TABLE kds.kds_ticket_log (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id       UUID NOT NULL REFERENCES kds.kds_tickets(id),
    from_status     VARCHAR(30),
    to_status       VARCHAR(30) NOT NULL,
    operator_id     UUID NOT NULL,
    changed_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    duration_seconds INT,             -- Tiempo en el estado anterior
    notes           TEXT
);

-- ============================================================
-- SCHEMA: inventory
-- ============================================================

CREATE SCHEMA inventory;

CREATE TABLE inventory.ingredient_categories (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    name            VARCHAR(100) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE inventory.ingredients (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    category_id     UUID REFERENCES inventory.ingredient_categories(id),
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    unit            VARCHAR(30) NOT NULL,     -- Unidad base de almacenamiento
    stock_current   NUMERIC(14,4) NOT NULL DEFAULT 0,
    stock_minimum   NUMERIC(14,4) NOT NULL DEFAULT 0,  -- Para alertas
    stock_maximum   NUMERIC(14,4),
    cost_per_unit   NUMERIC(12,4),
    supplier_id     UUID,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(tenant_id, name)
);

-- Este es el corazón del inventario: cada movimiento es inmutable
CREATE TABLE inventory.inventory_movements (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    ingredient_id   UUID NOT NULL REFERENCES inventory.ingredients(id),
    movement_type   VARCHAR(30) NOT NULL,
    -- PURCHASE | ORDER_DEDUCTION | ORDER_REVERSAL | ADJUSTMENT | WASTE | TRANSFER
    quantity        NUMERIC(14,4) NOT NULL,   -- Positivo = entrada, negativo = salida
    unit            VARCHAR(30) NOT NULL,
    stock_before    NUMERIC(14,4) NOT NULL,   -- Snapshot antes del movimiento
    stock_after     NUMERIC(14,4) NOT NULL,   -- Snapshot después
    reference_id    UUID,                     -- order_id, purchase_id, etc.
    reference_type  VARCHAR(50),              -- ORDER | PURCHASE | MANUAL
    notes           TEXT,
    created_by      UUID NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    -- Columnas para reversión
    reversed_by     UUID,                     -- ID del movimiento que revierte este
    is_reversed     BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_inventory_ingredient_date ON inventory.inventory_movements(ingredient_id, created_at DESC);
CREATE INDEX idx_inventory_reference ON inventory.inventory_movements(reference_id, reference_type);

-- ============================================================
-- SCHEMA: billing
-- ============================================================

CREATE SCHEMA billing;

CREATE TABLE billing.tickets (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    order_id        UUID NOT NULL UNIQUE,
    ticket_number   BIGINT NOT NULL,  -- Número secuencial por tenant
    status          VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    -- OPEN | PARTIAL | PAID | VOID | REFUNDED
    subtotal        NUMERIC(12,2) NOT NULL,
    tax_total       NUMERIC(12,2) NOT NULL,
    discount_total  NUMERIC(12,2) NOT NULL DEFAULT 0,
    total           NUMERIC(12,2) NOT NULL,
    notes           TEXT,
    cashier_id      UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    paid_at         TIMESTAMPTZ
);

-- Secuencia por tenant para números de ticket legibles
CREATE SEQUENCE billing.ticket_seq START 1000;

CREATE TABLE billing.ticket_splits (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id       UUID NOT NULL REFERENCES billing.tickets(id),
    split_type      VARCHAR(20) NOT NULL,  -- EQUAL | BY_ITEM | BY_PERSON
    split_number    SMALLINT NOT NULL,
    label           VARCHAR(100),          -- "Persona 1", "Mesa A"
    amount          NUMERIC(12,2) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE billing.payments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    ticket_id       UUID NOT NULL REFERENCES billing.tickets(id),
    split_id        UUID REFERENCES billing.ticket_splits(id),
    method          VARCHAR(30) NOT NULL,  -- CASH|CARD|TRANSFER|QR_PAY
    amount          NUMERIC(12,2) NOT NULL,
    reference       VARCHAR(255),          -- Referencia transacción externa
    status          VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
    processed_by    UUID NOT NULL,
    processed_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================
-- SCHEMA: reservations
-- ============================================================

CREATE SCHEMA reservations;

CREATE TABLE reservations.reservations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    guest_name      VARCHAR(255) NOT NULL,
    guest_phone     VARCHAR(30),
    guest_email     VARCHAR(255),
    party_size      SMALLINT NOT NULL,
    reserved_date   DATE NOT NULL,
    reserved_time   TIME NOT NULL,
    duration_minutes SMALLINT NOT NULL DEFAULT 90,
    table_id        UUID,
    status          VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    -- PENDING|CONFIRMED|SEATED|COMPLETED|CANCELLED|NO_SHOW
    notes           TEXT,
    created_by      UUID NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_reservations_date ON reservations.reservations(tenant_id, reserved_date, status);
```

---

# 6. ENTIDADES PRINCIPALES Y AGREGADOS

## 6.1 KDS Ticket — Entidad de Dominio

```java
public class KdsTicket extends AggregateRoot<KdsTicketId> {

    private KdsTicketId id;
    private TenantId tenantId;
    private OrderId orderId;
    private OrderItemId orderItemId;
    private KdsStationId stationId;
    private KdsStatus status;
    private Priority priority;
    private Instant sentAt;
    private Instant startedAt;     // nullable
    private Instant completedAt;   // nullable
    private Instant deliveredAt;   // nullable
    private Integer slaMinutes;
    private UserId operatorId;
    private List<KdsStatusTransition> history;

    public void start(UserId operator) {
        assertValidTransition(KdsStatus.IN_PREPARATION);
        this.startedAt = Instant.now();
        this.operatorId = operator;
        recordTransition(this.status, KdsStatus.IN_PREPARATION, operator);
        this.status = KdsStatus.IN_PREPARATION;
        registerEvent(new KdsTicketStartedEvent(this.id, this.orderId, this.orderItemId, operator));
    }

    public void complete(UserId operator) {
        assertValidTransition(KdsStatus.COMPLETED);
        this.completedAt = Instant.now();
        recordTransition(this.status, KdsStatus.COMPLETED, operator);
        this.status = KdsStatus.COMPLETED;
        // Este evento dispara el descuento de inventario
        registerEvent(new KdsTicketCompletedEvent(
            this.id, this.orderId, this.orderItemId,
            this.tenantId, operator, this.completedAt
        ));
    }

    public void revert(KdsStatus targetStatus, UserId operator, String reason) {
        // Reversión explícita — solo COMPLETED → IN_PREPARATION
        if (this.status != KdsStatus.COMPLETED) {
            throw new KdsRevertNotAllowedException(this.id, this.status);
        }
        if (targetStatus != KdsStatus.IN_PREPARATION) {
            throw new KdsRevertNotAllowedException(this.id, targetStatus);
        }
        this.completedAt = null;
        recordTransition(this.status, targetStatus, operator);
        this.status = targetStatus;
        // Este evento revierte el inventario
        registerEvent(new KdsTicketRevertedEvent(
            this.id, this.orderId, this.orderItemId,
            this.tenantId, operator, reason
        ));
    }

    public boolean isOverSla() {
        if (slaMinutes == null || sentAt == null) return false;
        return Duration.between(sentAt, Instant.now()).toMinutes() > slaMinutes;
    }
}
```

## 6.2 Máquina de Estados KDS

```
QUEUED ──────────────────────────────────────────────► CANCELLED
  │                                                        ▲
  ▼                                                        │
IN_PREPARATION ─────────────────────────────────────────►─┤
  │         ▲                                              │
  │         │ (revert, solo con permiso MANAGER+)          │
  ▼         │                                              │
COMPLETED ──┘─────────────────────────────────────────────┤
  │                                                        │
  ▼                                                        │
DELIVERED ──────────────────────────────────────────────►─┘
```

**Reglas de reversión:**
- `COMPLETED → IN_PREPARATION`: permitido con rol MANAGER o KITCHEN_LEAD. Genera `KdsTicketRevertedEvent` que dispara reversión de inventario.
- `IN_PREPARATION → QUEUED`: NO permitido (perdería el tiempo de inicio).
- `DELIVERED → *`: NO permitido (estado terminal de cocina).
- `CANCELLED → *`: NO permitido (estado terminal).

---

# 7. FLUJOS OPERACIONALES COMPLETOS

## 7.1 Flujo: Pedido QR desde mesa

```
Cliente escanea QR de mesa
         │
         ▼
[Frontend QR App]
Valida token QR → GET /api/v1/qr/{token}
         │
         ▼
[Tables Module]
Retorna: tableId, tableNumber, tenantId, menuUrl
         │
         ▼
[Frontend QR App]
Muestra menú → Usuario selecciona ítems
         │
         ▼
POST /api/v1/orders/qr
{
  "qrToken": "abc123",
  "items": [{ "productId": "...", "qty": 2 }],
  "coverCount": 2
}
         │
         ▼
[Orders Module - CreateQrOrderService]
1. Valida QR token → obtiene tableId
2. Verifica mesa disponible (TableModuleApi)
3. Crea Order con origin=QR
4. Asocia tableId
5. Cambia status mesa a OCCUPIED
6. Emite OrderCreatedEvent
7. Emite OrderSentToKitchenEvent (automático para QR)
         │
         ▼
[KDS Module - OrderItemsListener]
Escucha OrderSentToKitchenEvent
Por cada OrderItem:
  1. Determina estación (kds_station del producto)
  2. Crea KdsTicket con status QUEUED
  3. Emite KdsTicketQueuedEvent
         │
         ▼
[WebSocket Broadcast]
Topic: /topic/kds/{tenantId}/{stationId}
Payload: KdsTicketQueuedDto
         │
         ▼
[KDS Frontend]
Muestra ticket en tiempo real en pantalla de cocina
```

## 7.2 Flujo: Descuento de Inventario por ítem completado

```
Cocinero toca "COMPLETADO" en KDS
         │
         ▼
PUT /api/v1/kds/tickets/{ticketId}/complete
         │
         ▼
[KDS Module]
KdsTicket.complete(operatorId)
Emite: KdsTicketCompletedEvent {
  ticketId, orderId, orderItemId,
  tenantId, operatorId, completedAt
}
         │
         ▼
[Inventory Module - KdsTicketCompletedListener]
         │
         ├─ Busca orderItem → obtiene recipeId + quantity
         ├─ Busca recipe → obtiene recipe_ingredients
         │
         ▼
[InventoryDeductionService]
@Transactional  // Transacción DB única para todos los descuentos
Por cada ingredient en recipe:
  1. stockBefore = ingredient.stockCurrent
  2. requiredQty = recipeIngredient.quantity
               * orderItem.quantity
               * (1 + waste_factor)
  3. ingredient.stockCurrent -= requiredQty
  4. if (ingredient.stockCurrent < 0)
         → Lanza InsufficientStockException
         → Guarda movimiento con nota "STOCK_NEGATIVE"
         → Envía alerta a manager (WebSocket)
  5. Persiste InventoryMovement {
         type: ORDER_DEDUCTION,
         quantity: -requiredQty,
         stockBefore, stockAfter,
         referenceId: orderItemId,
         referenceType: ORDER_ITEM
     }
  6. if (ingredient.stockCurrent < ingredient.stockMinimum)
         → Emite LowStockAlertEvent
         │
         ▼
[Reporting Module - LowStockAlertListener]
Registra alerta, notifica vía WebSocket
```

## 7.3 Flujo: Reversión de Inventario

```
Manager toca "REVERTIR" en KDS (COMPLETED → IN_PREPARATION)
         │
         ▼
PUT /api/v1/kds/tickets/{ticketId}/revert
{ "targetStatus": "IN_PREPARATION", "reason": "Error en preparación" }
         │
         ▼
[KDS Module]
Verifica rol MANAGER/KITCHEN_LEAD
KdsTicket.revert(IN_PREPARATION, operatorId, reason)
Emite: KdsTicketRevertedEvent { ticketId, orderItemId, ... }
         │
         ▼
[Inventory Module - KdsTicketRevertedListener]
@Transactional
         │
         ├─ Busca movimiento original (ORDER_DEDUCTION, referenceId = orderItemId)
         ├─ Verifica que no fue ya revertido (is_reversed = false)
         │
         ▼
[InventoryReversalService]
Por cada movimiento de deducción encontrado:
  1. stockBefore = ingredient.stockCurrent
  2. reverseQty = |movimiento.quantity|  // Positivo = reingresa al stock
  3. ingredient.stockCurrent += reverseQty
  4. Persiste InventoryMovement {
         type: ORDER_REVERSAL,
         quantity: +reverseQty,
         stockBefore, stockAfter,
         referenceId: originalMovimiento.id,
         referenceType: REVERSAL
     }
  5. Marca movimiento original: is_reversed = true, reversed_by = nuevoMovimientoId
         │
         ▼
Emite: InventoryReversedEvent (para auditoría y reporting)
```

## 7.4 Flujo: División de Cuenta

```
Mesero abre pantalla de cierre de cuenta
         │
         ▼
GET /api/v1/billing/tickets/{orderId}
Retorna: ticket con items y totales
         │
Mesero selecciona "Dividir por persona"
         │
POST /api/v1/billing/tickets/{ticketId}/split
{
  "splitType": "BY_PERSON",
  "splits": [
    { "label": "Persona 1", "itemIds": ["uuid1", "uuid2"] },
    { "label": "Persona 2", "itemIds": ["uuid3"] }
  ]
}
         │
         ▼
[Billing Module - SplitBillService]
1. Valida que todos los items estén asignados
2. Calcula proporcionales de impuestos/descuentos
3. Crea TicketSplit por persona con su subtotal
4. Retorna splits con QR de pago individual (si aplica)
         │
Cada persona paga su split
POST /api/v1/billing/payments
{ "ticketId": "...", "splitId": "...", "method": "CARD", "amount": 45000 }
         │
[Billing Module - PaymentService]
1. Registra Payment
2. Verifica si todos los splits fueron pagados
3. Si ticket 100% pagado → emite TicketPaidEvent
         │
[Orders Module - TicketPaidListener]
Order.transitionTo(CLOSED)
         │
[Tables Module - OrderClosedListener]
Table.status = AVAILABLE
Emite: TableAvailableEvent
         │
[WebSocket Broadcast]
/topic/floor/{tenantId} → tabla disponible
```

---

# 8. DISEÑO DEL KDS

## 8.1 Filosofía de Diseño Operacional

El KDS está diseñado para **operación en cocina real**:
- Manos sucias → interacción mínima, gestos amplios o botones grandes
- Luz variable → alto contraste, colores con significado semántico
- Ruido → sin feedback de audio obligatorio, confirmación visual clara
- Presión → flujo de un toque para la acción más común
- Error frecuente → reversión accesible pero con confirmación

## 8.2 Diseño de Pantalla KDS

```
┌─────────────────────────────────────────────────────────────────┐
│  🔥 GRILL                              BistroDungu KDS  14:32  │
│  [QUEUED: 3] [EN PREP: 2] [COMPLETADOS: 5]                      │
├─────────────┬─────────────┬─────────────┬─────────────────────┤
│  Mesa 4     │  Mesa 7     │  Mesa 2     │  Mesa 1             │
│  ⏱ 2min    │  ⏱ 8min 🔴 │  ⏱ 4min    │  ⏱ 1min            │
│  ─────────  │  ─────────  │  ─────────  │  ─────────          │
│  2x Lomo    │  1x Costilla│  1x Churrasco│  3x Hamburguesa    │
│  sin sal    │  término 3  │  término 1  │  extra queso x2    │
│             │             │  ─────────  │  sin cebolla x1    │
│             │             │  1x Lomo    │                    │
│  [▶ INICIAR]│  [✓ LISTO] │  [▶ INICIAR]│  [▶ INICIAR]       │
│             │  [↩ REVERT] │             │                    │
└─────────────┴─────────────┴─────────────┴─────────────────────┘
```

**Colores semánticos:**
- `QUEUED`: fondo blanco, borde gris
- `IN_PREPARATION`: fondo amarillo pálido, borde naranja
- `COMPLETED`: fondo verde pálido, borde verde
- SLA excedido: borde rojo parpadeante, badge cronómetro en rojo

## 8.3 API KDS Frontend (WebSocket + REST)

```
WebSocket Topics:
  /topic/kds/{tenantId}/{stationId}          → Nuevos tickets, cambios de estado
  /topic/kds/{tenantId}/alerts               → Alertas SLA, errores

REST:
  GET  /api/v1/kds/stations/{stationId}/tickets     → Estado actual
  PUT  /api/v1/kds/tickets/{id}/start               → QUEUED → IN_PREPARATION
  PUT  /api/v1/kds/tickets/{id}/complete            → IN_PREPARATION → COMPLETED
  PUT  /api/v1/kds/tickets/{id}/deliver             → COMPLETED → DELIVERED
  PUT  /api/v1/kds/tickets/{id}/revert              → COMPLETED → IN_PREPARATION
  PUT  /api/v1/kds/tickets/{id}/cancel              → * → CANCELLED (solo MANAGER)
```

## 8.4 Preparación para Comandos de Voz (Futuro)

```java
// El diseño actual ya prepara la interface para comandos de voz
// La máquina de estados acepta comandos por texto/intención:
public interface KdsCommandHandler {
    void handle(KdsCommand command);  // Polimorfismo de comando
}

public sealed interface KdsCommand permits
    StartPreparationCommand,
    CompleteTicketCommand,
    DeliverTicketCommand,
    RevertTicketCommand;

// Un futuro VoiceCommandAdapter solo necesita:
// 1. Reconocer intención ("listo mesa 4")
// 2. Resolver ticketId desde mesa + estación actual
// 3. Crear el Command y enviarlo al handler
```

---

# 9. DISEÑO DE INVENTARIO POR INGREDIENTES

## 9.1 Principio de Inmutabilidad de Movimientos

Los movimientos de inventario son **append-only** (nunca se borran ni modifican).
La reversión crea un nuevo movimiento compensatorio, no edita el original.

```
Movimiento 1: ORDER_DEDUCTION  -50g  aceite  (orderItemId: X)  ← no se toca
Movimiento 2: ORDER_REVERSAL   +50g  aceite  (reversalOf: M1)  ← compensación
```

Esto garantiza:
- Auditoría forense completa
- Consistencia contable (débitos = créditos)
- Capacidad de reproducir el stock en cualquier punto del tiempo

## 9.2 Inconsistencias y Tolerancia a Errores

**Caso: stock negativo**

```java
// El sistema NO bloquea la operación si hay stock negativo
// (no podemos parar cocina por un error de conteo)
// Pero sí lo registra y alerta:
if (newStock.compareTo(BigDecimal.ZERO) < 0) {
    movement = movement.withFlag(MovementFlag.NEGATIVE_STOCK);
    eventBus.publish(new NegativeStockDetectedEvent(
        ingredientId, newStock, orderId
    ));
    // El manager recibe alerta inmediata en su panel
}
```

**Caso: orden cancelada con ítems ya completados en KDS**

```java
// OrderCancelledEvent → InventoryModule
// Por cada orderItem con status=COMPLETED en KDS:
//   → Busca movimientos ORDER_DEDUCTION para ese orderItemId
//   → Si no reversed → ejecuta ORDER_REVERSAL
//   → Si ya reversed → no-op (idempotente)
```

**Caso: evento duplicado (red/reintentos)**

```java
// Cada movimiento tiene referenceId (orderItemId)
// Antes de deducir, verificar:
boolean alreadyDeducted = movementRepo.existsByReferenceIdAndType(
    orderItemId, MovementType.ORDER_DEDUCTION
);
if (alreadyDeducted) return; // Idempotente
```

## 9.3 Conversión de Unidades

```java
public class UnitConverter {
    // Conversiones base: todo a la unidad SI del ingrediente
    // Ingrediente almacenado en: g
    // Receta dice: 2 tbsp de aceite
    // Converter: 2 tbsp × 14.79 g/tbsp = 29.58g

    private static final Map<UnitPair, BigDecimal> CONVERSION_FACTORS = Map.of(
        new UnitPair(TBSP, G), BigDecimal.valueOf(14.787),
        new UnitPair(TSP, G), BigDecimal.valueOf(4.929),
        new UnitPair(KG, G), BigDecimal.valueOf(1000),
        new UnitPair(L, ML), BigDecimal.valueOf(1000),
        new UnitPair(OZ, G), BigDecimal.valueOf(28.349)
    );

    public BigDecimal convert(BigDecimal quantity, Unit from, Unit to) {
        if (from == to) return quantity;
        // Busca factor directo o inverso
        // Si no existe → lanza UnitConversionException
    }
}
```

---

# 10. EVENTOS INTERNOS DEL SISTEMA

## 10.1 Catálogo de Eventos por Módulo

```java
// Todos los eventos extienden DomainEvent base
public abstract class DomainEvent {
    private final UUID eventId = UUID.randomUUID();
    private final Instant occurredOn = Instant.now();
    private final TenantId tenantId;
    // Versión del esquema para evolución del evento
    private final int schemaVersion = 1;
}

// ─── ORDERS ───────────────────────────────────────────────────
OrderCreatedEvent          { orderId, tableId, origin, items }
OrderItemAddedEvent        { orderId, orderItemId, productId, qty }
OrderSentToKitchenEvent    { orderId, items: List<KitchenItemDto> }
OrderStatusChangedEvent    { orderId, from, to, operatorId }
OrderClosedEvent           { orderId, tableId, totalAmount }
OrderCancelledEvent        { orderId, tableId, items: List<OrderItemId> }

// ─── KDS ──────────────────────────────────────────────────────
KdsTicketQueuedEvent       { ticketId, orderId, orderItemId, stationId }
KdsTicketStartedEvent      { ticketId, orderId, orderItemId, operatorId }
KdsTicketCompletedEvent    { ticketId, orderId, orderItemId, operatorId }
KdsTicketDeliveredEvent    { ticketId, orderId, operatorId }
KdsTicketRevertedEvent     { ticketId, orderId, orderItemId, reason }
KdsTicketCancelledEvent    { ticketId, orderId, operatorId }
KdsSlaExceededEvent        { ticketId, stationId, minutesOver }

// ─── INVENTORY ────────────────────────────────────────────────
InventoryDeductedEvent     { movementIds, orderItemId }
InventoryReversedEvent     { movementIds, reversedMovementIds }
LowStockAlertEvent         { ingredientId, currentStock, minimumStock }
NegativeStockDetectedEvent { ingredientId, currentStock, orderId }

// ─── TABLES ───────────────────────────────────────────────────
TableStatusChangedEvent    { tableId, from, to, orderId }
TableAvailableEvent        { tableId, zoneId }
TableOccupiedEvent         { tableId, orderId, originType }

// ─── BILLING ──────────────────────────────────────────────────
TicketCreatedEvent         { ticketId, orderId, total }
PaymentReceivedEvent       { paymentId, ticketId, method, amount }
TicketPaidEvent            { ticketId, orderId, tableId }

// ─── RESERVATIONS ─────────────────────────────────────────────
ReservationConfirmedEvent  { reservationId, tableId, guestName, time }
ReservationSeatedEvent     { reservationId, tableId }
ReservationCancelledEvent  { reservationId, reason }
```

## 10.2 Implementación del EventBus

```java
// Interface del puerto (en shared/domain)
public interface DomainEventBus {
    void publish(DomainEvent event);
    void publishAll(List<DomainEvent> events);
}

// Implementación con Spring ApplicationEventPublisher
@Component
public class SpringDomainEventBus implements DomainEventBus {
    private final ApplicationEventPublisher publisher;

    @Override
    public void publish(DomainEvent event) {
        publisher.publishEvent(event);
        // También persiste en event_store para replay/auditoría
        eventStore.save(event);
    }
}

// Patrón: los agregados acumulan eventos, el servicio los publica
@Transactional
public Order createOrder(...) {
    Order order = Order.create(...);
    orderRepository.save(order);
    // Publica DESPUÉS de persistir — nunca antes
    order.getDomainEvents().forEach(eventBus::publish);
    order.clearEvents();
    return order;
}

// Listener con @TransactionalEventListener para garantizar
// que el handler corre DESPUÉS del commit principal
@Component
public class KdsTicketCompletedListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("inventoryExecutor")  // Pool separado para no bloquear el hilo principal
    public void onKdsTicketCompleted(KdsTicketCompletedEvent event) {
        inventoryDeductionService.deductForOrderItem(event.getOrderItemId(), event.getTenantId());
    }
}
```

**Decisión de @TransactionalEventListener:**
- El evento se publica DESPUÉS del commit de la transacción KDS.
- Si el descuento de inventario falla, el KDS ya committeó → se maneja como inconsistencia conocida con alerta.
- Alternativa más robusta para producción: Outbox Pattern (guardar eventos en tabla `outbox` dentro de la misma transacción, procesarlos con un scheduled task).

---

# 11. ESTRATEGIA REDIS

## 11.1 Casos de Uso por Espacio de Clave

```
bistrodungu:{tenantId}:session:{sessionId}          → JWT session data (TTL: 8h)
bistrodungu:{tenantId}:table:{tableId}:status       → Estado actual de mesa (TTL: ninguno, se actualiza)
bistrodungu:{tenantId}:order:{orderId}:lock         → Distributed lock para edición concurrente (TTL: 5s)
bistrodungu:{tenantId}:kds:{stationId}:queue        → Cola ordenada de tickets KDS (Sorted Set, score = timestamp)
bistrodungu:{tenantId}:menu:active                  → Menú activo serializado (TTL: 5min, invalida en UPDATE)
bistrodungu:{tenantId}:inventory:low-stock          → Set de ingredientes bajo mínimo
bistrodungu:{tenantId}:qr:{token}                   → Mapping QR token → tableId (TTL: 24h)
bistrodungu:global:rate-limit:{ip}                  → Rate limiting por IP (TTL: 1min)
```

## 11.2 Concurrencia en Mesa (Distributed Lock)

```java
// Problema: dos meseros intentan agregar ítems al mismo pedido simultáneamente
@Service
public class AddItemToOrderService {

    @Override
    public OrderItem addItem(AddItemCommand command) {
        String lockKey = "bistrodungu:" + command.tenantId() + ":order:" + command.orderId() + ":lock";

        // RedisLockRegistry de Spring Integration
        Lock lock = lockRegistry.obtain(lockKey);
        if (!lock.tryLock(3, TimeUnit.SECONDS)) {
            throw new OrderConcurrentModificationException(command.orderId());
        }
        try {
            Order order = orderRepository.findById(command.orderId())
                .orElseThrow(() -> new OrderNotFoundException(command.orderId()));
            // Operación segura bajo lock
            OrderItem item = order.addItem(...);
            orderRepository.save(order);
            return item;
        } finally {
            lock.unlock();
        }
    }
}
```

## 11.3 Cache del Menú

```java
// Invalida caché cuando el menú cambia
@CacheEvict(value = "activeMenu", key = "#tenantId")
public void updateProduct(TenantId tenantId, UpdateProductCommand cmd) { ... }

@Cacheable(value = "activeMenu", key = "#tenantId")
public List<ProductDto> getActiveMenu(TenantId tenantId) { ... }

// Config: Redis como store de caché
@Bean
public RedisCacheConfiguration cacheConfig() {
    return RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(5))
        .serializeValuesWith(RedisSerializationContext.SerializationPair
            .fromSerializer(new GenericJackson2JsonRedisSerializer()));
}
```

---

# 12. COMUNICACIÓN REALTIME

## 12.1 Arquitectura WebSocket con STOMP

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Redis PubSub como broker para escalar horizontalmente
        registry.enableStompBrokerRelay("/topic", "/queue")
                .setRelayHost("redis")
                .setRelayPort(6379);
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
```

## 12.2 Canales por Contexto

```
/topic/kds/{tenantId}/{stationId}         → KDS tickets (solo cocina)
/topic/floor/{tenantId}                   → Estado de mesas (host/manager)
/topic/orders/{tenantId}/{tableId}        → Estado del pedido (QR self-service)
/topic/inventory/{tenantId}/alerts        → Alertas de stock (manager)
/topic/billing/{tenantId}                 → Eventos de pago (caja)
/topic/notifications/{tenantId}/{userId}  → Notificaciones personales
```

## 12.3 Security en WebSocket

```java
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = accessor.getFirstNativeHeader("Authorization");
            Authentication auth = jwtAuthProvider.authenticate(token);
            // Verifica que el usuario pertenece al tenant del topic
            accessor.setUser(auth);
        }
        return message;
    }
}
```

---

# 13. DISEÑO FRONTEND ANGULAR 19

## 13.1 Estructura del Workspace Angular

```
bistrodungu-frontend/
├── apps/
│   ├── pos/              → Aplicación POS principal (mesero + caja)
│   ├── kds/              → Kitchen Display System
│   ├── self-service/     → QR Self-Service (mobile-first)
│   └── admin/            → Panel administrativo
│
├── libs/
│   ├── shared/
│   │   ├── ui/           → Design System (componentes base)
│   │   ├── auth/         → Guards, interceptors, auth state
│   │   ├── websocket/    → WebSocket service compartido
│   │   └── models/       → Interfaces TypeScript (DTOs)
│   │
│   ├── feature-orders/   → State management de pedidos
│   ├── feature-menu/     → State management del menú
│   ├── feature-floor/    → Mapa de piso y mesas
│   ├── feature-kds/      → Lógica KDS compartida
│   └── feature-inventory/→ Inventario
```

## 13.2 State Management con NgRx Signals Store

```typescript
// Angular 19 usa Signals Store (sin RxJS boilerplate pesado)
// libs/feature-orders/src/lib/orders.store.ts

export const OrdersStore = signalStore(
  { providedIn: 'root' },
  withState<OrdersState>({
    orders: [] as Order[],
    activeOrder: null as Order | null,
    loading: false,
    error: null as string | null,
  }),

  withComputed((store) => ({
    openOrders: computed(() =>
      store.orders().filter(o => o.status !== 'CLOSED' && o.status !== 'CANCELLED')
    ),
    totalRevenue: computed(() =>
      store.orders()
        .filter(o => o.status === 'CLOSED')
        .reduce((sum, o) => sum + o.total, 0)
    ),
  })),

  withMethods((store, ordersService = inject(OrdersService)) => ({
    loadOrders: rxMethod<TenantId>(
      pipe(
        switchMap(tenantId => ordersService.getOpenOrders(tenantId)),
        tapResponse({
          next: (orders) => patchState(store, { orders }),
          error: (err) => patchState(store, { error: err.message }),
        })
      )
    ),

    // WebSocket updates
    handleRealTimeUpdate: (update: OrderStatusUpdate) => {
      patchState(store, (state) => ({
        orders: state.orders.map(o =>
          o.id === update.orderId ? { ...o, status: update.newStatus } : o
        )
      }));
    }
  }))
);
```

## 13.3 Aplicación KDS — Componente Principal

```typescript
// apps/kds/src/app/kds-station/kds-station.component.ts
@Component({
  selector: 'kds-station',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="kds-grid" [attr.data-station]="stationId()">
      @for (ticket of tickets(); track ticket.id) {
        <kds-ticket-card
          [ticket]="ticket"
          [class.over-sla]="ticket.isOverSla"
          (onStart)="startTicket($event)"
          (onComplete)="completeTicket($event)"
          (onRevert)="requestRevert($event)"
        />
      }
      @if (tickets().length === 0) {
        <kds-empty-state />
      }
    </div>
  `
})
export class KdsStationComponent {
  private store = inject(KdsStore);
  private ws = inject(KdsWebSocketService);

  stationId = input.required<string>();
  tickets = computed(() =>
    this.store.ticketsByStation(this.stationId())
  );

  ngOnInit() {
    // Suscripción WebSocket en tiempo real
    this.ws.subscribeToStation(this.stationId())
      .pipe(takeUntilDestroyed())
      .subscribe(event => this.store.handleWsEvent(event));
  }

  completeTicket(ticketId: string) {
    // Confirmación háptica o visual antes de completar
    this.store.completeTicket(ticketId);
  }

  requestRevert(ticketId: string) {
    // Solo con confirmación explícita del manager
    this.store.revertTicket(ticketId);
  }
}
```

## 13.4 Lazy Loading y Module Federation

```typescript
// apps/pos/src/app/app.routes.ts
export const routes: Routes = [
  {
    path: 'floor',
    loadChildren: () => import('@bistrodungu/feature-floor')
      .then(m => m.FLOOR_ROUTES),
    canActivate: [AuthGuard],
    data: { roles: ['WAITER', 'MANAGER', 'ADMIN'] }
  },
  {
    path: 'orders/:tableId',
    loadChildren: () => import('@bistrodungu/feature-orders')
      .then(m => m.ORDERS_ROUTES),
    canActivate: [AuthGuard],
    data: { roles: ['WAITER', 'CASHIER', 'MANAGER'] }
  },
  {
    path: 'inventory',
    loadChildren: () => import('@bistrodungu/feature-inventory')
      .then(m => m.INVENTORY_ROUTES),
    canActivate: [AuthGuard],
    data: { roles: ['MANAGER', 'ADMIN'] }
  }
];
```

---

# 14. APIs REST PRINCIPALES

## 14.1 Convenciones

- Versioning: `/api/v1/`
- Content-Type: `application/json`
- Error format estándar: `{ "code": "ORDER_NOT_FOUND", "message": "...", "details": {} }`
- Paginación: `?page=0&size=20&sort=createdAt,desc`
- Multi-tenancy: `X-Tenant-Id` header (validado por JWT)

## 14.2 Endpoints Críticos

```
# ─── ORDERS ─────────────────────────────────────────────────────
POST   /api/v1/orders                          → Crear pedido (mesero)
POST   /api/v1/orders/qr                       → Crear pedido QR (autoservicio)
GET    /api/v1/orders/{id}                     → Detalle de pedido
GET    /api/v1/orders?status=OPEN&tableId=...  → Listar pedidos activos
POST   /api/v1/orders/{id}/items               → Agregar ítem
DELETE /api/v1/orders/{id}/items/{itemId}      → Remover ítem
PUT    /api/v1/orders/{id}/send-to-kitchen     → Enviar a cocina
PUT    /api/v1/orders/{id}/status              → Cambiar estado

# ─── KDS ─────────────────────────────────────────────────────────
GET    /api/v1/kds/stations                    → Listar estaciones
GET    /api/v1/kds/stations/{stationId}/tickets → Tickets activos
PUT    /api/v1/kds/tickets/{id}/start          → Iniciar preparación
PUT    /api/v1/kds/tickets/{id}/complete       → Marcar completo
PUT    /api/v1/kds/tickets/{id}/deliver        → Marcar entregado
PUT    /api/v1/kds/tickets/{id}/revert         → Revertir a IN_PREPARATION
PUT    /api/v1/kds/tickets/{id}/cancel         → Cancelar (MANAGER)

# ─── TABLES ──────────────────────────────────────────────────────
GET    /api/v1/tables                          → Todas las mesas con estado
GET    /api/v1/tables/{id}                     → Detalle de mesa
POST   /api/v1/tables/{id}/occupy              → Ocupar mesa
POST   /api/v1/tables/{id}/release             → Liberar mesa
GET    /api/v1/qr/{token}                      → Resolver token QR

# ─── MENU ────────────────────────────────────────────────────────
GET    /api/v1/menu                            → Menú activo (con caché)
GET    /api/v1/menu/categories                 → Categorías
GET    /api/v1/menu/products/{id}              → Detalle de producto
POST   /api/v1/menu/products                   → Crear producto (ADMIN)
PUT    /api/v1/menu/products/{id}              → Actualizar producto
GET    /api/v1/menu/products/{id}/recipe       → Receta del producto

# ─── INVENTORY ───────────────────────────────────────────────────
GET    /api/v1/inventory/ingredients           → Listar ingredientes
GET    /api/v1/inventory/ingredients/{id}      → Detalle + stock actual
GET    /api/v1/inventory/ingredients/{id}/movements → Historial de movimientos
POST   /api/v1/inventory/ingredients           → Crear ingrediente
PUT    /api/v1/inventory/ingredients/{id}/stock → Ajuste manual de stock
GET    /api/v1/inventory/alerts                → Ingredientes bajo mínimo

# ─── BILLING ─────────────────────────────────────────────────────
GET    /api/v1/billing/tickets/{orderId}       → Ticket de un pedido
POST   /api/v1/billing/tickets/{id}/split      → Dividir cuenta
POST   /api/v1/billing/payments                → Registrar pago
GET    /api/v1/billing/payments/{ticketId}     → Pagos de un ticket

# ─── RESERVATIONS ────────────────────────────────────────────────
GET    /api/v1/reservations?date=2025-12-01    → Reservas del día
POST   /api/v1/reservations                    → Crear reserva
PUT    /api/v1/reservations/{id}/confirm       → Confirmar
PUT    /api/v1/reservations/{id}/seat          → Sentar al cliente
PUT    /api/v1/reservations/{id}/cancel        → Cancelar

# ─── REPORTING ───────────────────────────────────────────────────
GET    /api/v1/reports/dashboard               → KPIs del día
GET    /api/v1/reports/sales?from=...&to=...   → Ventas por período
GET    /api/v1/reports/products/top            → Productos más vendidos
GET    /api/v1/reports/inventory/consumption   → Consumo de ingredientes
GET    /api/v1/reports/kds/performance         → Tiempos promedio KDS
```

---

# 15. SEGURIDAD Y RBAC

## 15.1 Roles y Permisos

```java
public enum Role {
    SUPER_ADMIN,    // Acceso total multi-tenant (solo Bistro team)
    ADMIN,          // Administrador del restaurante
    MANAGER,        // Gerente de turno
    CASHIER,        // Cajero
    WAITER,         // Mesero
    KITCHEN_LEAD,   // Jefe de cocina
    KITCHEN,        // Cocinero
    HOST            // Recepcionista/hostess
}

// Mapa de permisos por operación
Permission.ORDER_CREATE:          [WAITER, CASHIER, MANAGER, ADMIN]
Permission.ORDER_CANCEL:          [MANAGER, ADMIN]
Permission.KDS_START:             [KITCHEN, KITCHEN_LEAD, MANAGER]
Permission.KDS_COMPLETE:          [KITCHEN, KITCHEN_LEAD, MANAGER]
Permission.KDS_REVERT:            [KITCHEN_LEAD, MANAGER, ADMIN]
Permission.KDS_CANCEL:            [MANAGER, ADMIN]
Permission.INVENTORY_ADJUST:      [MANAGER, ADMIN]
Permission.MENU_EDIT:             [MANAGER, ADMIN]
Permission.REPORT_VIEW:           [MANAGER, ADMIN]
Permission.BILLING_VOID:          [MANAGER, ADMIN]
Permission.RESERVATION_MANAGE:    [HOST, MANAGER, ADMIN]
```

## 15.2 Configuración Spring Security

```java
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/qr/**").permitAll()        // QR público
                .requestMatchers("/ws/**").permitAll()               // WebSocket auth por token
                .requestMatchers("/api/v1/auth/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}

// Method security para granularidad fina
@RestController
public class KdsController {

    @PutMapping("/{id}/revert")
    @PreAuthorize("hasAnyRole('KITCHEN_LEAD', 'MANAGER', 'ADMIN') " +
                  "and @tenantGuard.belongsToTenant(#id, authentication)")
    public ResponseEntity<KdsTicketDto> revertTicket(@PathVariable UUID id,
                                                      @RequestBody RevertTicketRequest req) {
        return ResponseEntity.ok(kdsService.revert(id, req));
    }
}
```

## 15.3 JWT con Claims de Tenant

```json
{
  "sub": "user-uuid",
  "tenantId": "tenant-uuid",
  "roles": ["KITCHEN"],
  "stationIds": ["station-uuid-1"],   // Restricción por estación KDS
  "iat": 1700000000,
  "exp": 1700028800
}
```

## 15.4 Tenant Isolation

```java
// Interceptor que inyecta tenantId en el contexto de cada request
@Component
public class TenantContextInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, ...) {
        String tenantId = extractTenantFromJwt(request);
        TenantContext.set(TenantId.of(tenantId));
        return true;
    }
    @Override
    public void afterCompletion(...) {
        TenantContext.clear();  // Siempre limpiar (thread pool reutiliza hilos)
    }
}

// Hibernate Filter para aplicar tenant automáticamente a toda query
@FilterDef(name = "tenantFilter",
           parameters = @ParamDef(name = "tenantId", type = String.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Entity
public class OrderEntity { ... }
```

---

# 16. ROADMAP DE IMPLEMENTACIÓN

## Fase 0 — Fundación (Semanas 1-3)
- Scaffolding del monolito modular (estructura de paquetes, configuración Spring Boot)
- Módulo Identity (autenticación, JWT, RBAC básico)
- Multi-tenancy (TenantContext, Hibernate Filter)
- Módulo Menu (CRUD de productos y categorías)
- CI/CD pipeline básico (GitHub Actions + Docker)
- PostgreSQL schema inicial (migrations con Flyway)

## Fase 1 — Core POS (Semanas 4-8)
- Módulo Tables (CRUD, estados, QR tokens)
- Módulo Orders (crear, agregar ítems, enviar a cocina)
- Módulo KDS (máquina de estados, WebSocket en tiempo real)
- Frontend Angular: Login, Mapa de Piso, Creación de Pedido, Pantalla KDS
- Integración WebSocket (STOMP/SockJS)

## Fase 2 — Inventario + Billing (Semanas 9-13)
- Módulo Inventory (ingredientes, movimientos, descuento automático)
- Vinculación recetas ↔ productos
- Descuento al completar en KDS (con reversión)
- Módulo Billing (tickets, split bill, pagos)
- Cierre de caja básico

## Fase 3 — Self-Service + Reservaciones (Semanas 14-17)
- App Angular QR Self-Service (mobile-first, PWA)
- Módulo Reservations (booking, confirmación, listado del día)
- Dashboard de manager en tiempo real

## Fase 4 — Reporting + Analítica (Semanas 18-21)
- Módulo Reporting (KPIs, ventas, consumo de inventario, desempeño KDS)
- Dashboard ejecutivo con gráficas
- Alertas automáticas (stock bajo, SLA KDS excedido)

## Fase 5 — Hardening + SaaS (Semanas 22-26)
- Onboarding de nuevos tenants (wizard)
- Rate limiting por tenant
- Observabilidad: Micrometer + Prometheus + Grafana
- E2E Tests (Playwright)
- Load testing (Gatling)
- Documentación API (OpenAPI 3 + Swagger UI)

---

# 17. RIESGOS ARQUITECTÓNICOS

## R1 — Consistencia Eventual entre KDS e Inventario
**Riesgo**: El evento `KdsTicketCompletedEvent` se publica después del commit KDS. Si el listener de inventario falla, el stock no se descuenta.
**Mitigación**: Implementar Outbox Pattern. El evento se guarda en tabla `outbox` dentro de la misma transacción KDS. Un worker independiente lee la tabla y publica el evento, con reintentos idempotentes.

## R2 — Concurrencia en Pedidos QR
**Riesgo**: Múltiples comensales de la misma mesa haciendo pedidos QR simultáneamente.
**Mitigación**: Distributed lock por mesa. Máximo un pedido OPEN por mesa (enforced en DB con partial unique index + en dominio).

## R3 — Stock Negativo
**Riesgo**: El sistema no bloquea operaciones en cocina por stock insuficiente (correctamente — no podemos parar el servicio).
**Mitigación**: Alertas inmediatas a manager. Reporte diario de discrepancias. Inventario físico semanal reconciliado contra movimientos del sistema.

## R4 — Crecimiento del Event Store
**Riesgo**: La tabla `inventory_movements` y `order_status_log` crecen indefinidamente.
**Mitigación**: Particionamiento por fecha en PostgreSQL (`PARTITION BY RANGE (created_at)`). Archiving de datos históricos a cold storage (S3) después de 12 meses.

## R5 — WebSocket Escalabilidad
**Riesgo**: Una sola instancia de Spring Boot no escala para múltiples restaurantes concurrentes.
**Mitigación**: Redis PubSub como broker STOMP (ya diseñado). Múltiples instancias del monolito pueden correr en paralelo porque el estado de sesión WebSocket está en Redis.

## R6 — Multi-Tenancy Data Leak
**Riesgo**: Un bug en TenantContext podría exponer datos de otro tenant.
**Mitigación**: Tests de integración con múltiples tenants. Hibernate Filter obligatorio en todas las entidades. Auditoría automática con tenant_id en todas las queries de producción.

---

# 18. ESTRATEGIA FUTURA PARA MICROSERVICIOS

## 18.1 Cuándo Extraer un Módulo

Criterios para extraer un módulo del monolito a un microservicio:

1. **Velocidad de cambio diferente**: el módulo de Inventory necesita cambios más frecuentes que Identity.
2. **Equipos independientes**: cuando el equipo crece y diferentes squads trabajan en diferentes contextos.
3. **Escalabilidad diferenciada**: el KDS necesita más instancias en hora pico que el módulo de Reportes.
4. **Tecnología diferente**: el módulo de Reporting podría beneficiarse de un motor analítico (ClickHouse, etc.).

## 18.2 Ruta de Migración (Strangler Fig Pattern)

```
Paso 1 — El módulo ya tiene:
  ✅ Interfaces explícitas (puertos)
  ✅ Sin dependencias directas de dominio con otros módulos
  ✅ Comunicación solo por eventos o API de módulo
  → LISTO para extraer con mínimo riesgo

Paso 2 — Extraer módulo a servicio separado:
  a. Crear nuevo proyecto Spring Boot con el código del módulo
  b. Cambiar EventBus internal → Kafka/RabbitMQ
  c. Cambiar ModuleApi calls → REST/gRPC
  d. Mantener la base de datos compartida temporalmente (strangler)

Paso 3 — Separar base de datos:
  a. El nuevo servicio tiene su propio schema/DB
  b. Datos históricos migrados
  c. El monolito ya no accede al schema extraído

Paso 4 — Eliminar código del monolito:
  a. Remover el módulo del monolito
  b. Actualizar documentación de APIs
```

## 18.3 Candidatos de Extracción por Prioridad

| Módulo | Prioridad | Motivo |
|---|---|---|
| Inventory | Alta | Crecimiento independiente, posible integración con ERPs |
| KDS | Alta | Posible SaaS independiente, hardware dedicado |
| Reporting | Media | Potencial cambio a motor analítico |
| Reservations | Media | Posible integración con plataformas externas (OpenTable) |
| Identity | Baja | Estable, candidato para solución externa (Keycloak) |

---

# 19. ESTRUCTURA DEL REPOSITORIO

```
bistrodungu/
│
├── backend/                          # Spring Boot Monolito Modular
│   ├── src/main/java/com/bistrodungu/
│   │   ├── shared/
│   │   │   ├── domain/
│   │   │   ├── infrastructure/
│   │   │   └── api/
│   │   ├── modules/
│   │   │   ├── identity/
│   │   │   ├── menu/
│   │   │   ├── tables/
│   │   │   ├── orders/
│   │   │   ├── kds/
│   │   │   ├── inventory/
│   │   │   ├── billing/
│   │   │   ├── reservations/
│   │   │   └── reporting/
│   │   └── application/
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   ├── application-dev.yml
│   │   ├── application-prod.yml
│   │   └── db/migration/            # Flyway migrations
│   │       ├── V1__create_identity_schema.sql
│   │       ├── V2__create_floor_schema.sql
│   │       └── ...
│   ├── src/test/
│   │   ├── unit/                    # Tests unitarios por módulo
│   │   ├── integration/             # Tests de integración con BD real
│   │   └── architecture/            # ArchUnit tests (enforce dependency rules)
│   ├── Dockerfile
│   └── pom.xml
│
├── frontend/                         # Angular 19 Workspace (Nx)
│   ├── apps/
│   │   ├── pos/
│   │   ├── kds/
│   │   ├── self-service/
│   │   └── admin/
│   ├── libs/
│   │   ├── shared/ui/
│   │   ├── shared/auth/
│   │   ├── shared/websocket/
│   │   ├── feature-orders/
│   │   ├── feature-menu/
│   │   ├── feature-floor/
│   │   ├── feature-kds/
│   │   └── feature-inventory/
│   ├── package.json
│   └── nx.json
│
├── infrastructure/
│   ├── docker/
│   │   ├── docker-compose.yml        # Desarrollo local
│   │   ├── docker-compose.prod.yml   # Producción
│   │   └── nginx/
│   │       └── nginx.conf
│   ├── k8s/                          # Kubernetes manifests (futuro)
│   └── terraform/                    # IaC para cloud (futuro)
│
├── docs/
│   ├── architecture/
│   │   ├── ADRs/                     # Architecture Decision Records
│   │   │   ├── ADR-001-modular-monolith.md
│   │   │   ├── ADR-002-event-driven-inventory.md
│   │   │   └── ADR-003-redis-for-sessions.md
│   │   ├── domain-model.md
│   │   └── bounded-contexts.md
│   ├── api/                          # OpenAPI specs
│   └── runbooks/                     # Procedimientos operacionales
│
├── scripts/
│   ├── setup-dev.sh
│   └── seed-data.sql
│
├── .github/
│   └── workflows/
│       ├── ci-backend.yml
│       ├── ci-frontend.yml
│       └── cd-prod.yml
│
└── docker-compose.yml                # Root compose para desarrollo

```

## 19.1 ArchUnit — Enforcing Dependency Rules

```java
// Estos tests fallan el build si alguien viola las reglas de dependencias
@AnalyzeClasses(packages = "com.bistrodungu")
public class ModuleDependencyTest {

    @ArchTest
    ArchRule no_cross_module_domain_access = noClasses()
        .that().resideInAPackage("..modules.orders.domain..")
        .should().dependOnClassesThat()
        .resideInAnyPackage(
            "..modules.inventory.domain..",
            "..modules.kds.domain..",
            "..modules.menu.domain.."
        )
        .because("Los dominios de módulos no deben acoplarse entre sí");

    @ArchTest
    ArchRule domain_no_spring = noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat()
        .resideInAPackage("org.springframework..")
        .because("El dominio no debe depender de Spring");
}
```

---

# APÉNDICE A — Docker Compose Completo (Desarrollo)

```yaml
version: '3.9'

services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: bistrodungu
      POSTGRES_USER: bistro
      POSTGRES_PASSWORD: bistro_dev_2025
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U bistro"]
      interval: 5s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    command: redis-server --appendonly yes
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 3s
      retries: 5

  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/bistrodungu
      SPRING_DATASOURCE_USERNAME: bistro
      SPRING_DATASOURCE_PASSWORD: bistro_dev_2025
      SPRING_REDIS_HOST: redis
      SPRING_REDIS_PORT: 6379
      APP_JWT_SECRET: dev-secret-change-in-prod-min-256-bits
      APP_JWT_EXPIRATION: 28800000
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    volumes:
      - ./backend:/app
    develop:
      watch:
        - action: rebuild
          path: ./backend/src

  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
    volumes:
      - ./infrastructure/docker/nginx/nginx.conf:/etc/nginx/nginx.conf
    depends_on:
      - backend

volumes:
  postgres_data:
  redis_data:
```

---

*BistroDungu Architecture Document — v1.0*
*Generado como guía de implementación enterprise.*
*Todos los diseños deben validarse con el equipo antes de implementación.*
