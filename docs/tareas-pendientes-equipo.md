# Backlog de Tareas Pendientes — `ms-estadisticas` (`:8420`)

> **Contexto:** Guía operativa para el equipo de `ms-estadisticas` (Arquitectura Hexagonal, Spring Boot 3.2.9, Java 21, PostgreSQL 16, Redis, RabbitMQ).  
> **Objetivo:** Cerrar las integraciones por eventos con los 9 microservicios productores/transversales, asegurar el quality gate y habilitar el despliegue a **Producción (`master`)**.

---

## Bloque 0: Puesta a punto antes de repartir ramas (Prioridad Inmediata)

Actualmente existen cambios locales sin commitear sobre la rama `develop` (32 archivos modificados/eliminados y 1 archivo nuevo). Antes de que el equipo cree ramas nuevas desde `develop`:

- [ ] **Consolidar y subir PR a `develop` con los cambios locales actuales:**
  - Soporte del sobre `data` de `ms-inventario` y `ms-pagos-suscripciones` en `DomainEventPayload.java` y `DomainEventMapper.java`.
  - Soporte de `SUBSCRIPTION_SUSPENDED`.
  - Botón `Authorize` (Bearer JWT) en `OpenApiConfig.java`.
  - Guía de contratos en `docs/contratos-integracion-estadisticas.html`.
- [ ] **Confirmar decisión de alcance sobre `SR-RF-80.2` (Ranking de prendas guardadas):**
  - En los cambios locales se eliminaron `GARMENT_SAVED_IN_PLAYLIST`, `STORE_GARMENTS_BY_SAVES`, `QueryRankingUseCase` y `RankingRestController`.
  - Confirmar en equipo si el requerimiento salió oficialmente del alcance o si debe restaurarse antes del merge.
- [ ] **Limpiar artefactos locales y revisar `.gitignore`:**
  - Asegurar que no se suban logs locales (`app.log`, `app_dev.log`, `hs_err_pid*.log`, `replay_pid*.log`).

---

## Bloque 1: Integración con los 9 Microservicios (RabbitMQ + Seguridad)

> **Estado base:** `ms-estadisticas` declara la cola durable `styleradar.estadisticas.eventos-dominio.q` (con DLQ `styleradar.estadisticas.dlq`, reintentos con backoff exponencial e idempotencia en PostgreSQL) enlazada a `styleradar.exchange` en `RabbitMqConfig.java`.

### Transversal RabbitMQ (Proteger la DLQ)
- [ ] **Revisar bindings wildcard (`styleradar.<servicio>.#`) en `RabbitMqConfig.java`:**
  - Hoy cualquier routing key bajo un servicio que `DomainEventMapper` no reconozca lanza excepción y se envía a `styleradar.estadisticas.dlq`.
  - Definir si se restringen los bindings a routing keys exactos o si `DomainEventMapper` / `DomainEventListener` debe ignorar con log eventos informativos que no alimentan métricas.

---

### 1. `ms-auth` (`:8100`) + `api-gateway` (`:8080`) — *Seguridad Transversal y Sesiones*
- [ ] **Unificar nombres de roles JWT:**
  - `JwtTokenValidator.java` acepta `STORE_ADMIN`/`ADMIN_ALMACEN`, `FOUNDATION_ADMIN`/`FOUNDATION_REPRESENTATIVE`/`REPRESENTANTE_FUNDACION`/`FOUNDATION`, y `PLATFORM_ADMIN`/`STYLERADAR_ADMIN`/`ADMIN_STYLERADAR`/`ADMIN` (pero **no** `STORE` ni `TIENDA`, que aparecen en `docs/contratos-integracion-estadisticas.html`). Alinear código y guía con lo que realmente firma `ms-auth`.
- [ ] **Unificar cabeceras HTTP propagadas por `api-gateway`:**
  - `GatewayHeaderAuthenticationFilter.java` lee `X-User-Role`, `X-Store-Id` y `X-Foundation-Id`, mientras que la guía HTML menciona `X-Authenticated-*`. Confirmar con el equipo de `api-gateway` cuáles cabeceras inyecta realmente.
- [ ] **Validar secreto y claims en QA:**
  - Verificar que `JWT_SECRET` (Base64 HS256) sea idéntico entre `ms-auth` y `ms-estadisticas`, y que los tokens incluyan `storeId` (o `nit`) y `foundationId`.
- [ ] **Definir emisor de sesiones en tiempo real:**
  - Acordar si los inicios/cierres/expiraciones de sesión (`styleradar.usuarios.sesion.*` → `USER_SESSION_CHANGED`) los publica `ms-auth` o `ms-usuarios`.

### 2. `ms-usuarios` (`:8120`) — *Binding: `styleradar.usuarios.#`*
- [ ] **Definir evento para registro de usuarios (`PLATFORM_USER_REGISTRATIONS`):**
  - El código de métrica `PLATFORM_USER_REGISTRATIONS` existe en `MetricCode.java`, pero actualmente no tiene `DomainEventType` ni routing key mapeado en `DomainEventMapper` / `ProcessDomainEventUseCaseImpl`. Implementar si aplica a `SR-RF-86`.
- [ ] **Pruebas E2E en QA de presencia y fundaciones:**
  - Validar emisión y consumo de `styleradar.usuarios.sesion.actualizada` (`delta: +1/-1` → `PLATFORM_REALTIME_USERS`, `PLATFORM_ACTIVE_USERS` — `SR-RF-86.1`, `SR-RF-90`).
  - Validar emisión y consumo de `styleradar.usuarios.fundacion.estado` (`PLATFORM_ACTIVE_FOUNDATIONS` — `SR-RF-86.3`).

### 3. `ms-tiendas` (`:8140`) — *Binding: `styleradar.tiendas.#`*
- [ ] **Validar contrato de visitas (`styleradar.tiendas.visita.registrada`):**
  - Probar en QA que `ms-tiendas` envíe `tiendaId`/`storeId` consistente con el claim del JWT para alimentar `STORE_PAGE_VISITS` (`SR-RF-76.1`, `SR-RF-77`, `SR-RF-79`) y `delta` (`+1`/`-1`) para `STORE_REALTIME_VISITORS` (`SR-RF-78` / `SR-RF-81`).
- [ ] **Validar transiciones de verificación (`styleradar.tiendas.verificacion.actualizada`):**
  - Probar alta inicial (`estadoAnterior: null`, `estado: PENDING`) y aprobación (`PENDING` → `APPROVED`) hacia `PLATFORM_STORES_BY_VERIFICATION` (`SR-RF-86.2`).

### 4. `ms-chat` (`:8320`) — *Binding: `styleradar.chat.#`*
- [ ] **Alinear payload con el equipo de `ms-chat`:**
  - Confirmar si envían payload plano o sobre `data: { ... }` en `styleradar.chat.mensaje.recibido` (`STORE_MESSAGE_RECEIVED` → `STORE_MESSAGES_RECEIVED`, `SR-RF-76.2`, `SR-RF-77`). Si usan sobre `data`, agregar extracción de `data.tiendaId` en `DomainEventPayload`/`DomainEventMapper`.
- [ ] **Prueba E2E en QA:**
  - Enviar un mensaje de comprador a tienda y verificar el incremento en el resumen con variación y serie diaria (7/30/90 días).

### 5. `ms-inventario` (`:8200`) — *Binding: `styleradar.inventario.#`*
- [ ] **Resolver identidad `sedeId` vs `storeId`:**
  - Actualmente `ms-inventario` publica `data.sedeId` y `ms-estadisticas` lo guarda como `scopeId` de `ScopeType.STORE`.
  - Verificar con `ms-tiendas` y `ms-inventario` si una tienda maneja múltiples sedes; si es así, `storeId != sedeId` haría que el administrador de tienda consulte datos vacíos y habría que exigir `tiendaId`/`storeId` en el evento.
- [ ] **Prueba E2E en QA (`SR-RF-78` / `SR-RF-81`):**
  - Verificar que `styleradar.inventario.actualizado` actualice `STORE_INVENTORY_BY_STATE` (`AVAILABLE`, `LOW_STOCK`, `OUT_OF_STOCK`) y que las alertas `styleradar.inventario.stock.ultimas-unidades` y `styleradar.inventario.stock.agotado` se procesen sin duplicar transiciones ni ir a DLQ.

### 6. `ms-item-catalogo` (`:8220`) — *Binding: `styleradar.catalogo.#`*
- [ ] **Alinear formato (plano vs `data`) y probar los 3 eventos en QA:**
  - `styleradar.catalogo.prenda.publicada` → `PLATFORM_GARMENT_PUBLICATIONS` (`SR-RF-86.4`).
  - `styleradar.catalogo.prenda.moderacion.pendiente` → `MODERATION_PUBLICATIONS` (`SR-RF-87`, requiere `tipoPublicador`, `publicadorId`, `publicadorNombre`).
  - `styleradar.catalogo.busqueda.ejecutada` → `PLATFORM_SEARCHES` (`SR-RF-88.1`).
- [ ] **Definir cierre del flujo de moderación (`POST /api/v1/statistics/moderation/decision`):**
  - Acordar con `ms-item-catalogo` si la aprobación/rechazo ocurre en catálogo (emitiendo `delta: -1` hacia estadísticas) o si `ms-estadisticas` debe notificar la decisión de vuelta a `ms-item-catalogo`.

### 7. `ms-segunda-vida-donaciones` (`:8520`) — *Binding: `styleradar.donaciones.#`*
- [ ] **Alinear payloads y probar los 3 eventos en QA:**
  - `styleradar.donaciones.solicitud.creada` → `FOUNDATION_REQUESTS_FROM_USERS` / `FOUNDATION_REQUESTS_FROM_STORES` (`SR-RF-83`) + estado inicial en `FOUNDATION_REQUESTS_BY_STATE`.
  - `styleradar.donaciones.solicitud.estado.actualizado` → transición `estadoAnterior` → `estado` en `FOUNDATION_REQUESTS_BY_STATE` (`SR-RF-85.2`).
  - `styleradar.donaciones.completada` → `FOUNDATION_GARMENTS_RECEIVED` (`SR-RF-82.1`, `SR-RF-84`), `FOUNDATION_DONATIONS_BY_ORIGIN` (`SR-RF-85.1`), `FOUNDATION_GARMENTS_BY_TYPE` y `PLATFORM_MONTHLY_DONATIONS` (`SR-RF-86.6`).

### 8. `ms-pagos-suscripciones` (`:8400`) — *Binding: `styleradar.pagos.#` (Mayor deuda técnica)*
- [ ] **Desbloquear cálculo de ingresos (`PLATFORM_SUBSCRIPTION_REVENUE` — `SR-RF-89.1`, `SR-RF-89.2`):**
  - El evento real `styleradar.pagos.pago.confirmado` no incluye `monto`. Acordar con `ms-pagos-suscripciones` una de dos soluciones:
    - *Opción A (Eventos):* Incluir `data.monto` y moneda en `pago.confirmado` y mapearlo en `DomainEventPayload`/`DomainEventMapper`.
    - *Opción B (REST):* Implementar adaptador de salida HTTP hacia `GET /internal/statistics/payments/summary` (o cruzar `planId` con `GET /api/v1/plans`).
- [ ] **Desbloquear cuentas por plan (`PLATFORM_BUYERS_BY_PLAN` y `PLATFORM_STORES_BY_PLAN` — `SR-RF-91.1`, `SR-RF-91.2`):**
  - Actualmente no existe evento de alta o cambio de plan (y cualquier routing key distinto a `pago.confirmado` o `suscripcion.suspendida` cae a DLQ).
  - Acordar evento de suscripción creada / plan cambiado o endpoint de snapshot actual de planes.
- [ ] **Validar `styleradar.pagos.suscripcion.suspendida` en QA:**
  - Confirmar el conteo mensual en `PLATFORM_SUBSCRIPTION_SUSPENSIONS` usando zona horaria `America/Bogota`.

### 9. `ms-playlist` (`:8260`) — *Binding: `styleradar.playlist.#`*
- [ ] **Validar `styleradar.playlist.creada` en QA:**
  - Confirmar incremento en `PLATFORM_PLAYLISTS_CREATED` (`SR-RF-86.5`).
- [ ] **Cerrar definición sobre prendas guardadas (`SR-RF-80`):**
  - *Si sigue en alcance:* Restaurar o rediseñar el consumo de `styleradar.playlist.prenda.guardada` y su endpoint de consulta.
  - *Si salió de alcance:* Actualizar `docs/contratos-integracion-estadisticas.html` y `README.md`, y cambiar el binding de `styleradar.playlist.#` a `styleradar.playlist.creada` (o ignorar eventos no estadísticos) para evitar mensajes en DLQ.

---

## Bloque 2: Despliegue a Producción (`PROD` / rama `master`) e Infraestructura

> **Estado actual:** `.github/workflows/ci-cd.yml` ya incluye el job `deploy-prod` que construye la imagen `acrstyleradarestadisticas.azurecr.io/sr-ms-estadisticas:prod` y reinicia `sr-ms-estadisticas-prod` en `rg-styleradar-estadisticas-prod` al hacer push/merge a `master` o `main`.

- [ ] **Verificar aprovisionamiento de recursos en Azure PROD:**
  - Confirmar permisos del Service Principal (`AZURE_CREDENTIALS`) sobre `rg-styleradar-estadisticas-prod` y el App Service `sr-ms-estadisticas-prod` conectado al ACR `acrstyleradarestadisticas` (tag `:prod`).
  - Aprovisionar/verificar PostgreSQL 16 aislado para PROD (`styleradar_statistics`), Redis de PROD y acceso al broker RabbitMQ de PROD (`5671` AMQPS).
- [ ] **Revisar certificados TLS de RabbitMQ para PROD:**
  - Verificar en `Dockerfile` si `rabbitmq-ca.pem` y `rabbitmq-truststore.jks` corresponden también al broker de PROD o parametrizar el truststore por entorno si PROD usa otra CA.
- [ ] **Configurar App Settings (Variables de Entorno) en `sr-ms-estadisticas-prod`:**
  - `SERVER_PORT=8420` / `WEBSITES_PORT=8420`
  - `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`
  - `REDIS_HOST`, `REDIS_PORT` (y validar soporte de password/SSL si Azure Cache for Redis en PROD usa puerto `6380`)
  - `RABBITMQ_HOST`, `RABBITMQ_PORT=5671`, `RABBITMQ_USER`, `RABBITMQ_PASSWORD`, `RABBITMQ_VHOST`, `RABBITMQ_SSL_ENABLED=true`
  - `JWT_SECRET` (secreto productivo compartido con `ms-auth`)
- [ ] **Habilitar Health Check real de RabbitMQ y Actuator:**
  - Quitar `management.health.rabbit.enabled: false` de `src/main/resources/application.yml` (o moverlo solo a perfil de test) y configurar el Health Check de Azure App Service hacia `/api/v1/actuator/health` o `/api/v1/health`.
- [ ] **Gobernanza de GitHub y primer Release a `master`:**
  - Configurar *Branch Protection Rules* en `develop` y `master` (PR obligatorio, al menos 1 revisor y check `Build, Test, Coverage & Docker Packaging` en verde con JaCoCo ≥ 85%).
  - Configurar GitHub Environment `production` con aprobación manual si aplica.
  - Ejecutar PR de release `develop` → `master` y realizar *Smoke Tests* en PROD (Swagger UI, `/api/v1/health`, JWT y conexión a colas RabbitMQ PROD).

---

## Bloque 3: Calidad, Pruebas y Documentación

- [ ] **Integrar SonarQube en `.github/workflows/ci-cd.yml`:**
  - Añadir el paso de análisis estático y Quality Gate documentado en el `README.md`.
- [ ] **Actualizar `README.md` y `.env.example`:**
  - Actualizar URLs reales de QA/PROD y tabla real de endpoints (`/api/v1/statistics/...`).
  - Actualizar sección 5 (topología real de eventos consumidos, actualmente indica *"Pendiente de definir"*).
  - Actualizar sección 8 (`JWT_SECRET` HS256 en lugar de `JWT_PUBLIC_KEY`, variables TLS de RabbitMQ y Redis).
  - Adjuntar captura faltante de cobertura (`docs/evidencias/cobertura-jacoco.png`).
- [ ] **Mantener actualizado `docs/contratos-integracion-estadisticas.html`:**
  - Corregir el contrato de roles JWT y el estado de `ms-playlist` antes de compartirlo de nuevo con los otros equipos.

---

## Sugerencia de Reparto de Trabajo (Equipo de 4 personas)

| Rol / Frente | Responsabilidades Sugeridas |
| :--- | :--- |
| **Backend 1 (Mensajería Transaccional)** | `ms-pagos-suscripciones` (ingresos y planes), `ms-inventario` (`sedeId`/`storeId`), `ms-playlist` (`SR-RF-80`) y protección de DLQ en `RabbitMqConfig`/`DomainEventMapper`. |
| **Backend 2 (Comunidad & Catálogo)** | Contratos y pruebas E2E en QA con `ms-item-catalogo` (incluyendo flujo de moderación), `ms-segunda-vida-donaciones` y `ms-chat`. |
| **Líder / DevOps & Seguridad** | Alinear `ms-auth` + `api-gateway` + `ms-usuarios` (JWT, cabeceras, sesiones), habilitar health check de RabbitMQ, SonarQube y ejecutar la salida a **PROD (`master`)** en Azure. |
| **Frontend / QA Integración** | Conectar tableros (Tienda, Fundación, Admin Plataforma) contra QA (`:8420`), validar respuestas reales en Swagger y actualizar `README.md` + evidencias. |
