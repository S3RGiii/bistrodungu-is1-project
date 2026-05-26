# BistroDungu - Enterprise POS Platform

## Visión General

BistroDungu es una plataforma **Enterprise POS** (Point of Sale) diseñada para restaurantes modernos casuales como sistema modular con separación explícita de Bounded Contexts.

## Fase 0 - Completada ✅

La Fase 0 - Fundación ha sido completada con éxito. Esta fase incluye:

### 1. **Scaffolding del Monolito Modular** ✅
   - Estructura Maven multi-módulo completa
   - Separación clara de `shared`, `modules`, y `application`
   - Cada módulo con su propio pom.xml y estructura de Clean Architecture

### 2. **Módulo Identity** ✅
   - **Entidades**: Tenant, User
   - **Funcionalidades**:
     - Autenticación con JWT
     - Gestión de usuarios por tenant
     - Control de acceso basado en roles (RBAC)
     - Endpoints:
       - `POST /api/v1/auth/login` - Login
       - `POST /api/v1/auth/register` - Registro de usuario

### 3. **Multi-tenancy** ✅
   - `TenantContext` para mantener el tenant en ThreadLocal
   - `TenantRequestFilter` para extraer tenant del request
   - Todas las entidades incluyen `tenant_id` como campo obligatorio
   - Aislación de datos por tenant en todos los módulos

### 4. **Módulo Menu** ✅
   - **Entidades**: Category, Product
   - **Funcionalidades**:
     - CRUD de categorías
     - CRUD de productos
     - Gestión de precios
     - Endpoints:
       - `GET/POST /api/v1/menu/categories`
       - `GET/POST /api/v1/menu/products`
       - `PUT/DELETE /api/v1/menu/{id}`

### 5. **PostgreSQL Schema Inicial** ✅
   - Migraciones Flyway:
     - `V1__Create_Schemas.sql` - Creación de esquemas
     - `V2__Create_Identity_Tables.sql` - Tablas de Identity
     - `V3__Create_Menu_Tables.sql` - Tablas de Menu
   - Índices para performance en queries multi-tenant
   - Soft delete (`deleted_at`) en todas las entidades

### 6. **CI/CD Pipeline Básico** ✅
   - GitHub Actions workflow en `.github/workflows/build.yml`
   - Compilación automática en cada push
   - Testing automático
   - Build de Docker image

### 7. **Infraestructura** ✅
   - **Dockerfile** - Multi-stage build para optimización
   - **docker-compose.yml** - Desarrollo local con PostgreSQL, Redis, y App
   - **application.yml** - Configuración de aplicación

## Estructura del Proyecto

```
bistro-dungu-is1-project/
├── pom.xml                          # POM padre (multi-módulo)
├── Dockerfile                       # Docker build
├── docker-compose.yml              # Local development
├── .github/
│   └── workflows/
│       └── build.yml               # CI/CD pipeline
├── shared/                         # Código compartido
│   ├── domain/
│   │   ├── event/                 # DomainEvent, EventBus
│   │   ├── aggregate/             # AggregateRoot
│   │   ├── vo/                    # ValueObjects: TenantId, UserId, Money
│   │   └── exception/             # Excepciones base
│   ├── infrastructure/
│   │   ├── event/                 # SpringApplicationEventBus
│   │   ├── persistence/           # BaseEntity, JPA config
│   │   ├── multitenancy/          # TenantContext, TenantRequestFilter
│   │   └── api/                   # ApiResponse
│   └── pom.xml
├── modules/
│   ├── identity/
│   │   ├── domain/
│   │   ├── application/           # AuthenticationService
│   │   ├── infrastructure/
│   │   │   ├── persistence/       # TenantEntity, UserEntity, Repositories
│   │   │   ├── security/          # JwtTokenProvider
│   │   │   └── web/               # AuthenticationController
│   │   └── pom.xml
│   ├── menu/
│   │   ├── domain/
│   │   ├── application/           # CategoryService, ProductService
│   │   ├── infrastructure/
│   │   │   ├── persistence/       # CategoryEntity, ProductEntity, Repositories
│   │   │   ├── web/               # CategoryController, ProductController
│   │   │   └── dto/               # CategoryDTO, ProductDTO
│   │   └── pom.xml
│   ├── tables/                    # Placeholder (Phase 1)
│   ├── reservations/              # Placeholder (Phase 3)
│   ├── orders/                    # Placeholder (Phase 1)
│   ├── kds/                       # Placeholder (Phase 1)
│   ├── inventory/                 # Placeholder (Phase 2)
│   ├── billing/                   # Placeholder (Phase 2)
│   └── reporting/                 # Placeholder (Phase 4)
├── application/
│   ├── src/main/java/
│   │   └── com/bistrodungu/
│   │       ├── BistroDunguApplication.java
│   │       └── config/
│   │           ├── SecurityConfig.java
│   │           └── JwtAuthenticationFilter.java
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/
│   │       ├── V1__Create_Schemas.sql
│   │       ├── V2__Create_Identity_Tables.sql
│   │       └── V3__Create_Menu_Tables.sql
│   └── pom.xml
├── BistroDungu_Architecture.md     # Documentación de arquitectura
└── README.md                       # Este archivo
```

## Requisitos para Ejecutar

### Local con Docker Compose (Recomendado)
```bash
docker-compose up -d
```

Esto levanta:
- PostgreSQL en puerto 5432
- Redis en puerto 6379
- Aplicación en puerto 8080

### Local sin Docker

**Requisitos:**
- Java 21+
- Maven 3.9+
- PostgreSQL 15+
- Redis 7+

**Pasos:**
1. Clonar repositorio
2. Configurar PostgreSQL y Redis
3. Actualizar `application.yml` con credenciales
4. `mvn clean install`
5. `mvn spring-boot:run -pl application`

## Endpoints Disponibles (Fase 0)

### Authentication (Identity Module)
- `POST /api/v1/auth/login` - Login con email/password
- `POST /api/v1/auth/register` - Registro de usuario

### Menu Management
- `GET /api/v1/menu/categories` - Listar categorías
- `POST /api/v1/menu/categories` - Crear categoría
- `GET /api/v1/menu/categories/{id}` - Obtener categoría
- `PUT /api/v1/menu/categories/{id}` - Actualizar categoría
- `DELETE /api/v1/menu/categories/{id}` - Eliminar categoría

- `GET /api/v1/menu/products` - Listar productos
- `POST /api/v1/menu/products` - Crear producto
- `GET /api/v1/menu/products/{id}` - Obtener producto
- `PUT /api/v1/menu/products/{id}` - Actualizar producto
- `DELETE /api/v1/menu/products/{id}` - Eliminar producto

## Próximos Pasos (Fase 1)

La Fase 1 incluirá:
- Módulo Tables (CRUD, estados, QR tokens)
- Módulo Orders (crear, agregar ítems, enviar a cocina)
- Módulo KDS (máquina de estados, WebSocket)
- Frontend Angular básico (Login, Mapa de Piso, POS)
- Integración WebSocket

## Principios Arquitectónicos

1. **Aislación de dominios**: Ningún módulo accede directamente a la capa de dominio de otro
2. **Comunicación por eventos**: Los módulos se comunican vía DomainEvents y EventBus
3. **Consistencia eventual**: Operaciones críticas usan sagas o transacciones compensadas
4. **Trazabilidad total**: Toda mutación registra quién, cuándo, desde/hacia qué estado
5. **Reversibilidad operacional**: Estados admiten reversión con compensación
6. **Multi-tenancy desde el inicio**: Todo modelo incluye `tenant_id`

## Tecnología

- **Backend**: Spring Boot 3.2.4, Spring Security, Spring Data JPA
- **Base de datos**: PostgreSQL 15, Flyway migrations
- **Cache/PubSub**: Redis 7
- **Autenticación**: JWT (JJWT)
- **Build**: Maven 3.9
- **Containerización**: Docker, Docker Compose
- **CI/CD**: GitHub Actions

## Documentación

Ver [BistroDungu_Architecture.md](BistroDungu_Architecture.md) para la documentación completa de arquitectura, diseño DDD, modelo relacional, y roadmap de fases.

---

**Última actualización**: Mayo 2026  
**Estado**: ✅ Fase 0 Completada
