# bistrodungu-is1-project

BistroDungu es una plataforma transaccional diseñada para optimizar la operación de restaurantes modernos. Integra en un solo ecosistema la gestión de mesas físicas, un motor de reservaciones con validación de disponibilidad en tiempo real y un módulo de pedidos eCommerce para clientes externos.

## Arquitectura

El proyecto está dividido en dos partes:

### Backend (`/backend`)
- **Framework**: Spring Boot 3.x (Java 17)
- **Base de datos**: PostgreSQL
- **Caché**: Redis
- **Contenerización**: Docker

#### Estructura del Backend
```
backend/
├── src/main/java/com/bistrodungu/
│   ├── BistroDunguApplication.java   # Clase principal
│   ├── config/
│   │   ├── RedisConfig.java          # Configuración de Redis
│   │   └── SecurityConfig.java       # Configuración de seguridad y CORS
│   ├── controller/
│   │   ├── OrderController.java      # Endpoints de pedidos
│   │   ├── ReservationController.java# Endpoints de reservaciones
│   │   └── RestaurantTableController.java # Endpoints de mesas
│   ├── model/
│   │   ├── Order.java
│   │   ├── OrderItem.java
│   │   ├── Reservation.java
│   │   └── RestaurantTable.java
│   ├── repository/
│   │   ├── OrderRepository.java
│   │   ├── ReservationRepository.java
│   │   └── RestaurantTableRepository.java
│   └── service/
│       ├── OrderService.java
│       ├── ReservationService.java
│       └── RestaurantTableService.java
└── src/main/resources/
    └── application.yml               # Configuración de la aplicación
```

#### API Endpoints
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/reservations` | Listar reservaciones |
| POST | `/api/reservations` | Crear reservación |
| PUT | `/api/reservations/{id}` | Actualizar reservación |
| DELETE | `/api/reservations/{id}` | Eliminar reservación |
| GET | `/api/tables` | Listar mesas |
| GET | `/api/tables/available` | Mesas disponibles |
| POST | `/api/tables` | Crear mesa |
| GET | `/api/orders` | Listar pedidos |
| POST | `/api/orders` | Crear pedido |
| PATCH | `/api/orders/{id}/status` | Actualizar estado de pedido |

### Frontend (`/frontend`)
- **Framework**: Angular 19
- **Lenguaje**: TypeScript
- **Estilos**: SCSS
- **Servidor**: Nginx (en producción)

#### Estructura del Frontend
```
frontend/src/app/
├── app.ts                    # Componente raíz
├── app.config.ts             # Configuración de la aplicación
├── app.routes.ts             # Rutas de la aplicación
├── components/
│   ├── navbar/               # Barra de navegación
│   ├── reservations/         # Vista de reservaciones
│   ├── tables/               # Vista de mesas
│   └── orders/               # Vista de pedidos
└── services/
    ├── reservation.ts         # Servicio HTTP para reservaciones
    ├── table.ts               # Servicio HTTP para mesas
    └── order.ts               # Servicio HTTP para pedidos
```

## Ejecución con Docker Compose

Para levantar todos los servicios (PostgreSQL, Redis, Backend, Frontend):

```bash
docker-compose up --build
```

Los servicios estarán disponibles en:
- **Frontend**: http://localhost:4200
- **Backend API**: http://localhost:8080
- **PostgreSQL**: localhost:5432
- **Redis**: localhost:6379

## Desarrollo Local

### Backend
```bash
cd backend
mvn spring-boot:run
```
> Requiere PostgreSQL y Redis corriendo localmente.

### Frontend
```bash
cd frontend
npm install
ng serve
```
> La aplicación estará disponible en http://localhost:4200

## Variables de Entorno (Backend)

| Variable | Descripción | Valor por defecto |
|----------|-------------|-------------------|
| `DB_HOST` | Host de PostgreSQL | `localhost` |
| `DB_PORT` | Puerto de PostgreSQL | `5432` |
| `DB_NAME` | Nombre de la base de datos | `bistrodungu` |
| `DB_USER` | Usuario de PostgreSQL | `bistrodungu` |
| `DB_PASSWORD` | Contraseña de PostgreSQL | `bistrodungu` |
| `REDIS_HOST` | Host de Redis | `localhost` |
| `REDIS_PORT` | Puerto de Redis | `6379` |
