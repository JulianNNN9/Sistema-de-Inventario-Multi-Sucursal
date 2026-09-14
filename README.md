# Sistema de Inventario Multi-Sucursal — OptiPlant

Aplicación para la gestión de inventario de múltiples sucursales de una misma organización: cada sucursal opera de forma autónoma en sus transacciones locales (ventas, compras, movimientos de stock) y comparte visibilidad del inventario con el resto de la red a través de una única base de datos PostgreSQL centralizada — sin sincronización ni sharding entre sucursales.

La aplicación cubre el ciclo completo de un negocio minorista con varias sucursales: compras a proveedores, ventas con listas de precio, movimientos de inventario, transferencias entre sucursales con su propia máquina de estados, tiempos de envío y cumplimiento logístico, un dashboard de indicadores y un recomendador de rebalanceo de stock. Todo protegido por autenticación JWT y autorización por rol, con tres roles de negocio:

| Rol | Alcance |
|---|---|
| `ADMIN_GENERAL` | Ve y opera sobre todas las sucursales; administra usuarios y sucursales. |
| `GERENTE_SUCURSAL` | Decisiones de su propia sucursal: aprobar/rechazar transferencias, compras, listas de precio. |
| `OPERADOR_INVENTARIO` | Ejecución operativa del día a día en su sucursal: ventas, movimientos de stock, solicitar/despachar/recibir/resolver transferencias. |

## Stack

| Capa | Tecnología |
|---|---|
| Frontend | React 18 + TypeScript + Vite + Tailwind CSS + Recharts (servido por Nginx) |
| Backend | Java 21 + Spring Boot 3.3 + Spring Data JPA + Spring Security (JWT) + Flyway |
| Base de datos | PostgreSQL 16 |
| Infraestructura | Docker Compose |

## Arquitectura

Arquitectura de 3 capas físicamente separadas. El frontend se comunica con el backend exclusivamente por la API REST `/api/v1`; no hay lógica de negocio en el cliente. Tanto el backend como el frontend siguen una organización **package-by-feature** (cada dominio de negocio —`producto`, `venta`, `transferencia`, etc.— agrupa su propio código en vez de dispersarse por capa técnica), para que el código relacionado con una misma funcionalidad viva junto. En el backend cada paquete agrupa `controller`, `service`, `repository`, `entity` y `dto`; en el frontend cada `features/<dominio>/` agrupa su `api`, `hooks`, `types` y `pages`, dejando en `shared/` solo lo transversal (componentes UI genéricos, cliente HTTP, utilidades). Beneficios frente a organizar por capa: mayor cohesión (lo que cambia junto vive junto), menos archivos a tocar por feature, fronteras de dominio más claras y mejor escalabilidad a medida que crecen los módulos.

### Contenedores y flujo de una petición

Los 3 contenedores de `docker-compose.yml` están en la misma red de Docker; solo `frontend` (3000), `backend` (8080) y `db` (5432) publican puerto al host, y el navegador solo necesita hablar con `frontend`:

```
Navegador
  │  http://localhost:3000
  ▼
[frontend] Nginx
  │  sirve la SPA de React (archivos estáticos)
  │  proxy_pass /api/* → http://backend:8080   (nginx.conf, red interna de Docker)
  ▼
[backend] Spring Boot — API REST /api/v1
  │  JwtAuthenticationFilter → @PreAuthorize (rol) → CurrentUser.assertPuedeOperarSobreSucursal (sucursal)
  │  Controller → Service (reglas de negocio + transacción) → Repository (Spring Data JPA)
  ▼
[db] PostgreSQL 16 — esquema versionado por Flyway (V1..V19 al arrancar)
```

Nginx nunca expone el backend directamente al navegador: todo `/api/*` se resuelve dentro de la red de Docker (`proxy_pass http://backend:8080`), así que en producción solo haría falta exponer el puerto del frontend.

### Seguridad (JWT + autorización por rol y por sucursal)

- **Autenticación sin estado**: `POST /api/v1/auth/login` devuelve un JWT (HS256, 8h de expiración) con `sub` (id de usuario), `rol`, `sucursalId` y `tokenVersion` como claims; no hay sesiones en el servidor (`SessionCreationPolicy.STATELESS`).
- **`JwtAuthenticationFilter`** se inserta en la cadena de filtros de Spring Security antes del filtro usuario/contraseña (patrón Chain of Responsibility) y puebla el `SecurityContext` a partir del token.
- **Invalidación de tokens (mitigación de robo/fuga)**: cada usuario tiene un contador `token_version` en base de datos. `JwtAuthenticationFilter` compara el `tokenVersion` del claim contra ese valor actual en cada petición y rechaza la autenticación si no coinciden, igual que un token inválido. `POST /api/v1/auth/logout` (autenticado) incrementa ese contador, invalidando de inmediato *todos* los tokens ya emitidos para ese usuario sin esperar a que expiren por sí solos (hasta 8h). El costo es una consulta a `usuario` por petición autenticada, a cambio de revocabilidad real sobre un esquema que de otro modo sería JWT puro sin estado.
- **Autorización en dos capas, nunca solo una**: el `@PreAuthorize("hasAnyRole(...)")` en cada endpoint del `Controller` decide *qué rol* puede llamar la operación; el `Service` vuelve a verificar con `CurrentUser.assertPuedeOperarSobreSucursal(...)` que el usuario no-admin solo opera sobre *su propia sucursal* — un rol correcto no basta si la sucursal del recurso no es la suya.
- **Secreto JWT (RNF-02)**: no vive hardcodeado en el repo; ver la decisión de diseño correspondiente más abajo.

### Frontend

SPA de React con rutas protegidas (`PrivateRoute`, `RoleGuard` en `src/routes/`) que ocultan tanto la navegación como los controles de acción (botones de aprobar, despachar, etc.) según el rol y la sucursal del usuario autenticado — reflejando en la UI las mismas reglas que ya aplica el backend, nunca como único punto de control. Un cliente Axios centralizado (`src/api/client.ts`) inyecta el JWT en cada request y normaliza errores; cada dominio de negocio tiene su propio módulo de API, sus propios hooks de datos (`useXxxList`, `useMutation`) y su propia página, replicando a propósito la misma organización por dominio que el backend.

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

No se requiere configuración manual: `docker-compose.yml` incluye valores por defecto para todas las variables, salvo el secreto JWT, que se genera solo (ver "Decisiones de diseño" → RNF-02). Para sobrescribir cualquier valor, copiar `.env.example` a `.env`.

**Credenciales de arranque** (creadas por la migración `V3__seed_admin.sql`):

| Email | Contraseña | Rol |
|---|---|---|
| `admin@optiplant.local` | `Admin123!` | ADMIN_GENERAL |

Desde esa cuenta se pueden crear el resto de usuarios (`GERENTE_SUCURSAL`, `OPERADOR_INVENTARIO`) y sucursales vía la sección de Administración de la aplicación.

Además, el proyecto ya arranca con datos de prueba completos del dominio "ferretería" (4 sucursales, 9 usuarios, 28 productos, compras, ventas, transferencias en todos sus estados y sugerencias de rebalanceo reales) — ver la migración `V19__seed_ferreteria.sql` para el detalle completo y las credenciales de los otros 8 usuarios (contraseña uniforme `Test123!`).

**Documentación interactiva de la API**: con el backend corriendo, Swagger UI queda disponible en `http://localhost:8080/swagger-ui.html` (usa el botón "Authorize" con el token de `POST /api/v1/auth/login`). El spec OpenAPI se sirve en vivo en `http://localhost:8080/v3/api-docs`.

### Desarrollo local sin Docker (opcional)

Para iterar con hot-reload en un solo servicio sin reconstruir la imagen completa:

```bash
# Backend (requiere una Postgres accesible, p. ej. la del propio docker compose)
cd backend
export JWT_SECRET=$(openssl rand -hex 32)   # obligatorio: application.yml no trae un valor por defecto (RNF-02)
mvn spring-boot:run

# Frontend (servidor de desarrollo de Vite, con hot-reload)
cd frontend
npm install
npm run dev   # http://localhost:5173, proxy de Vite hacia el backend
```

`SecurityConfig` ya permite CORS desde `http://localhost:5173` además de `http://localhost:3000`, así que el frontend en modo desarrollo puede llamar directamente al backend sin pasar por Nginx.

## Estructura del repositorio

Árbol real del repositorio (resumido a nivel de paquete/carpeta; se omiten `node_modules/`, `target/`, `dist/` y demás artefactos generados):

```
.
├── docker-compose.yml
├── .env.example                  # plantilla de variables para sobrescribir defaults (copiar a .env)
├── .gitattributes                # fuerza eol=lf en *.sh (los scripts corren dentro de contenedores Linux)
├── README.md
│
├── backend/
│   ├── Dockerfile                 # build multi-stage: maven:3.9-eclipse-temurin-21 → eclipse-temurin:21-jre
│   ├── docker-entrypoint.sh        # genera/persiste JWT_SECRET si no llega por entorno (RNF-02)
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/optiplant/inventario/
│       │   │   ├── InventarioApplication.java   # main() de Spring Boot
│       │   │   ├── common/
│       │   │   │   ├── dto/            # ApiErrorResponse, PageResponse<T> (paginación uniforme, RNF-01)
│       │   │   │   └── exception/      # GlobalExceptionHandler + excepciones de negocio (una por caso)
│       │   │   ├── config/             # SecurityConfig, JacksonConfig, OpenApiConfig, RoleDescriptionOperationCustomizer
│       │   │   ├── security/
│       │   │   │   ├── auth/           # AuthController, AuthService, dto/  (POST /api/v1/auth/login, /logout)
│       │   │   │   ├── JwtService.java, JwtAuthenticationFilter.java, JwtPrincipal.java
│       │   │   │   ├── CurrentUser.java            # alcance por sucursal (assertPuedeOperarSobreSucursal)
│       │   │   │   ├── UsuarioDetailsService.java
│       │   │   │   └── RestAuthenticationEntryPoint.java, RestAccessDeniedHandler.java
│       │   │   │
│       │   │   │   # --- un paquete por dominio de negocio (package-by-feature) ---
│       │   │   │   # cada uno con su propio controller/service/repository/entity/dto,
│       │   │   │   # salvo las excepciones anotadas abajo:
│       │   │   ├── sucursal/      {controller, service, repository, entity, dto}
│       │   │   ├── usuario/       {controller, service, repository, entity, dto}
│       │   │   ├── producto/      {controller, service, repository, entity, dto}
│       │   │   ├── inventario/    {controller, service, repository, entity, dto}   # stock por sucursal + movimientos
│       │   │   ├── compra/        {controller, service, repository, entity, dto}   # proveedores + órdenes de compra
│       │   │   ├── venta/         {controller, service, repository, entity, dto}   # ventas + listas de precio
│       │   │   ├── transferencia/ {controller, service, repository, entity, dto}   # máquina de estados (Módulo 4)
│       │   │   ├── logistica/     {controller, service, dto}                        # sin entity/repository: solo agrega datos de `transferencia`
│       │   │   ├── dashboard/     {controller, service, dto}                        # idem: solo agregaciones de solo lectura
│       │   │   └── rebalanceo/    {controller, service, dto, strategy}              # strategy/: RebalanceoStrategy + DeficitSuperavitStrategy
│       │   └── resources/
│       │       ├── application.yml
│       │       └── db/migration/        # scripts Flyway versionados V1..V19 (naming Flyway estándar)
│       └── test/java/com/optiplant/inventario/...   # JUnit 5 + Mockito, misma estructura de paquetes por dominio
│
├── frontend/
│   ├── Dockerfile                 # build multi-stage: node → nginx
│   ├── nginx.conf                 # sirve la SPA + proxy_pass /api/* al backend
│   ├── package.json / vite.config.ts / tailwind.config.js / tsconfig.json
│   └── src/
│       ├── main.tsx, App.tsx
│       │
│       │   # --- un paquete por dominio de negocio (package-by-feature) ---
│       │   # cada uno con su propio api/hooks/types/pages, salvo excepciones anotadas abajo:
│       ├── features/
│       │   ├── auth/          {api, hooks, context, routes, pages, types}   # login, sesión JWT, PrivateRoute/RoleGuard
│       │   ├── dashboard/      {api, hooks, pages, types}
│       │   ├── productos/      {api, hooks, pages, types}
│       │   ├── compras/        {api, hooks, pages, types}                   # incluye proveedores.ts
│       │   ├── ventas/         {api, hooks, pages, types}                   # incluye priceLists.ts
│       │   ├── inventario/     {api, hooks, pages, types}                   # stock por sucursal + movimientos
│       │   ├── logistica/      {api, hooks, pages, types}
│       │   ├── transferencias/ {api, hooks, pages, types}                   # máquina de estados (Módulo 4)
│       │   ├── rebalanceo/     {api, hooks, types}                          # sin pages propias: se consume desde dashboard
│       │   ├── usuarios/       {api, hooks, types}                          # sin pages propias: se consume desde admin
│       │   ├── sucursales/     {api, hooks, types}                         # sin pages propias: se consume desde varias features
│       │   └── admin/          {pages}                                      # compone usuarios + sucursales
│       ├── shared/        # código transversal sin dueño de un solo dominio
│       │   ├── api/       client.ts (Axios + interceptor JWT)
│       │   ├── components/
│       │   │   └── ui/    # kit de componentes propio (Button, Card, DataTable, Modal, Select, ...)
│       │   ├── context/   # ToastContext
│       │   ├── hooks/     # useMutation
│       │   ├── lib/       # helpers (format.ts, cn.ts)
│       │   └── types/     # api.ts (ApiError, PageResponse)
│       └── test/          # setup de Vitest + Testing Library
│
└── graphify-out/          # grafo de conocimiento del repo (generado, no se versiona a mano)
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
- **Secreto JWT sin hardcodear (RNF-02)**: ni `docker-compose.yml` ni `application.yml` tienen un valor de secreto legible (el placeholder `dev_jwt_secret_change_me_...` que existía antes quedaba comprometido por estar en el repo, aunque fuera "solo para dev"). En su lugar, `backend/docker-entrypoint.sh` revisa si `JWT_SECRET` llegó por entorno; si no, genera uno aleatorio con `openssl rand -hex 32` la primera vez que arranca el contenedor y lo persiste en el volumen Docker `jwt_secret` (así un simple `docker compose restart backend` no invalida todos los tokens ya emitidos; solo un `docker compose down -v` lo regenera, igual que pasa con `pgdata`). Esto no rompe RT-03 ("un solo comando, sin configuración manual"): `docker compose up` sigue siendo suficiente, el secreto simplemente se genera solo en vez de venir fijo en el repo. `application.yml` tampoco tiene fallback: `${JWT_SECRET}` sin default, a propósito, para que arrancar el backend fuera de Docker sin definir la variable falle rápido en vez de firmar tokens silenciosamente con un secreto público conocido.
- **Invalidación de JWT vía contador de versión**: un JWT firmado es válido hasta que expira (8h) aunque el servidor "olvide" haberlo emitido — no hay forma de revocarlo antes de tiempo en un esquema stateless puro, lo que es un problema real si un token se filtra o roba. Se agrega `usuario.token_version` (entero, default 0) como mitigación de bajo costo: se incluye como claim adicional al firmar el token, `JwtAuthenticationFilter` lo compara contra el valor actual en base de datos en cada petición y rechaza la autenticación si no coincide, y `POST /api/v1/auth/logout` simplemente incrementa el contador. No es logout selectivo por dispositivo/sesión (invalida *todos* los tokens del usuario a la vez, no solo el que cierra sesión) ni requiere una tabla de tokens revocados; a cambio, agrega una consulta a `usuario` por petición autenticada — un costo aceptado deliberadamente porque prioriza revocación real sobre estado 100% sin BD.

## Tests

```bash
cd backend && mvn test    # 113 tests (JUnit 5 + Mockito)
cd frontend && npm test   # 4 tests (Vitest + Testing Library)
```

Cada módulo se validó, además de con estos tests unitarios, contra una instancia real de PostgreSQL levantada con `docker compose` (migraciones Flyway reales, datos reales, llamadas HTTP con `curl` incluyendo casos de error y restricciones de rol).
