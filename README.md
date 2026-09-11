# Sistema de Inventario Multi-Sucursal — OptiPlant

Aplicación para la gestión de inventario de múltiples sucursales de una misma organización: cada sucursal opera de forma autónoma en sus transacciones locales (ventas, compras, movimientos de stock) y comparte visibilidad del inventario con el resto de la red a través de una única base de datos PostgreSQL centralizada — sin sincronización ni sharding entre sucursales.

## Stack

| Capa | Tecnología |
|---|---|
| Frontend | React 18 + TypeScript + Vite + Tailwind CSS + Recharts (servido por Nginx) |
| Backend | Java 21 + Spring Boot 3.3 + Spring Data JPA + Spring Security (JWT) + Flyway |
| Base de datos | PostgreSQL 16 |
| Infraestructura | Docker Compose |

Arquitectura de 3 capas físicamente separadas. El frontend se comunica con el backend exclusivamente por la API REST `/api/v1`; no hay lógica de negocio en el cliente. El backend sigue una organización **package-by-feature** (cada dominio —`producto`, `venta`, `transferencia`, etc.— agrupa sus propios `controller`, `service`, `repository`, `entity` y `dto`) en vez de agrupar por capa técnica, para que el código relacionado con una misma funcionalidad viva junto.

## Puesta en marcha

Requisito único: Docker + Docker Compose.

```bash
docker compose up --build
```

Esto levanta 3 contenedores:

| Servicio | Puerto host | Descripción |
|---|---|---|
| `frontend` | http://localhost:3000 | SPA de React servida por Nginx |
| `backend` | http://localhost:8080 | API REST de Spring Boot (`/actuator/health` para healthcheck) |
| `db` | localhost:5432 | PostgreSQL 16 (volumen `pgdata`, migraciones Flyway aplicadas automáticamente al arrancar) |

No se requiere configuración manual: `docker-compose.yml` incluye valores por defecto para todas las variables. Para sobrescribirlos, copiar `.env.example` a `.env`.

**Credenciales de arranque** (creadas por la migración `V3__seed_admin.sql`):

| Email | Contraseña | Rol |
|---|---|---|
| `admin@optiplant.local` | `Admin123!` | ADMIN_GENERAL |

Desde esa cuenta se pueden crear el resto de usuarios (`GERENTE_SUCURSAL`, `OPERADOR_INVENTARIO`) y sucursales vía la sección de Administración de la aplicación.

Además, el proyecto ya arranca con datos de prueba completos del dominio "ferretería" (4 sucursales, 9 usuarios, 28 productos, compras, ventas, transferencias en todos sus estados y sugerencias de rebalanceo reales) — ver la migración `V19__seed_ferreteria.sql` para el detalle completo y las credenciales de los otros 8 usuarios (contraseña uniforme `Test123!`).

**Documentación interactiva de la API**: con el backend corriendo, Swagger UI queda disponible en `http://localhost:8080/swagger-ui.html` (usa el botón "Authorize" con el token de `POST /api/v1/auth/login`). El spec OpenAPI se sirve en vivo en `http://localhost:8080/v3/api-docs`.

## Estructura del repositorio

```
backend/
  src/main/java/com/optiplant/inventario/
    <dominio>/{controller,service,repository,entity,dto}/   # un paquete por dominio de negocio
    common/            # DTOs y excepciones transversales (GlobalExceptionHandler, PageResponse, etc.)
    security/          # JWT, filtro de autenticación, CurrentUser
    config/            # Spring Security, Jackson
  src/main/resources/db/migration/   # scripts Flyway versionados (V1..V19)
  src/test/java/...    # tests unitarios (JUnit 5 + Mockito), misma estructura por dominio
frontend/
  src/{api,hooks,types,pages,components}/   # un archivo por dominio en cada carpeta
  src/components/ui/   # kit de componentes propio (Button, Card, DataTable, Modal, ...)
docker-compose.yml
```

## Módulos implementados

Todos los módulos del alcance funcional están completos, con backend, tests y frontend integrados.

| Módulo | Alcance |
|---|---|
| 0 | Infraestructura, autenticación JWT, sucursales y administración de usuarios |
| 1 | Gestión de inventario y movimientos de stock (RF-01..RF-07) |
| 2 | Compras a proveedores y recepción de mercancía (RF-08..RF-12) |
| 3 | Ventas y listas de precio (RF-13..RF-16) |
| 4 | Transferencias entre sucursales, con máquina de estados completa (RF-17..RF-21) |
| 5 | Tiempos de envío y reporte de cumplimiento logístico por ruta (RF-22..RF-25) |
| 6 | Dashboard de indicadores operativos (RF-26..RF-30) |
| 7 | Recomendador de rebalanceo de inventario entre sucursales (RF-31..RF-34) |
| 8 | Documentación y cierre |
| 9 | Documentación OpenAPI (Swagger UI) y datos de prueba del dominio ferretería |

## Decisiones de diseño (resumen)

- **Patrones aplicados**: Repository + Service Layer + DTO en todos los dominios; **Facade** para operaciones que orquestan varias tablas en una sola transacción (recepción de compra, registro de venta, ciclo de vida de una transferencia); **Strategy** para el algoritmo de rebalanceo (`RebalanceoStrategy` / `DeficitSuperavitStrategy`, Módulo 7); **Chain of Responsibility** en el filtro de autenticación JWT.
- **Autorización** por rol y método (`@PreAuthorize`) en cada endpoint, con alcance por sucursal reforzado en la capa de Service (`CurrentUser`), no solo en el controlador.
- **Rendimiento (RNF-01)**: todo endpoint de listado es paginado; los reportes de agregación (Dashboard, logística) se resuelven con `SUM`/`COUNT`/`AVG` a nivel de base de datos, nunca trayendo filas a la JVM para sumarlas en Java; relaciones que se listan junto a su entidad padre usan `@EntityGraph` para evitar N+1.
- **Errores (RNF-04)**: un `GlobalExceptionHandler` único traduce cada excepción de negocio a un código HTTP y un mensaje específico (nunca genérico), con el mismo formato de error en toda la API.
- **PostgreSQL y parámetros nulos**: los filtros opcionales en consultas JPQL usan el idioma `coalesce(:param, columna)` en vez de `:param IS NULL`, evitando un error real de inferencia de tipos de PostgreSQL con parámetros nulos aislados (detectado y corregido durante el desarrollo).
- **Documentación de roles en OpenAPI (Módulo 9)**: un `OperationCustomizer` (`RoleDescriptionOperationCustomizer`) lee en tiempo real la anotación `@PreAuthorize` real de cada endpoint y la traduce a una descripción en español dentro del spec — la documentación de "qué rol puede usar este endpoint" nunca puede desincronizarse de la autorización efectiva, porque se genera a partir de ella.

## Tests

```bash
cd backend && mvn test    # 107 tests (JUnit 5 + Mockito)
cd frontend && npm test   # 4 tests (Vitest + Testing Library)
```

Cada módulo se validó, además de con estos tests unitarios, contra una instancia real de PostgreSQL levantada con `docker compose` (migraciones Flyway reales, datos reales, llamadas HTTP con `curl` incluyendo casos de error y restricciones de rol).
