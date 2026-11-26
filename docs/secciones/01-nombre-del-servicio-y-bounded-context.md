[⬅ Volver al README Principal](../../README.md)

---

## 1. NOMBRE DEL SERVICIO Y SU BOUNDED CONTEXT

* **Identificador de Servicio:** `NQS-Statistics-Service` (`ms-estadisticas`)
* **Paquete Arquitectónico:** Paquete 4: Pagos y Estadísticas
* **Puerto de Escucha Oficial:** `8420` (con `context-path: /api/v1`)
* **Base de Datos Asignada:** `PostgreSQL 16` (`styleradar_statistics` en puerto host `5435`), aislada bajo el patrón *Database-per-Service*.
* **Broker de Mensajería:** `RabbitMQ 3.12` (puerto host `5672` / `5671` AMQPS TLS en QA y PROD) en rol exclusivo de **consumidor** de eventos de dominio.
* **Bounded Context:** `Estadísticas y Analítica`

### Responsabilidades del Dominio

Este microservicio encapsula exclusivamente la consolidación transaccional de métricas diarias, series temporales, totales acumulados y distribuciones porcentuales de **StyleRadar**, a partir de los eventos publicados por los microservicios productores en RabbitMQ (`styleradar.exchange`), exponiendo consultas autenticadas por alcance (`STORE`, `FOUNDATION`, `PLATFORM`).

| Qué HACE este Microservicio (Alcance) | Qué NO HACE este Microservicio (Límites) |
| :--- | :--- |
| • Consume eventos de dominio desde 6 microservicios productores vía RabbitMQ garantizando idempotencia transaccional (`processed_event`). | • NO autentica usuarios ni emite tokens JWT (delegado a `ms-auth` en `:8100`). |
| • Consolida métricas diarias y distribuciones porcentuales en `America/Bogota` para los 9 indicadores vigentes (`SR-RF-81` a `SR-RF-91.2`). | • NO publica eventos de dominio hacia otros microservicios (rol 100% consumidor). |
| • Expone totales acumulados y mensuales de donaciones, prendas publicadas y playlists (`SR-RF-82.1`, `SR-RF-86.4`, `SR-RF-86.5`, `SR-RF-86.6`). | • NO gestiona inventario, catálogo, playlists ni pasarelas de pago (delegado a `ms-inventario :8200`, `ms-item-catalogo :8220`, `ms-playlist :8260`, `ms-pagos-suscripciones :8400`). |
| • Expone series temporales diarias y mensuales con relleno de ceros en intervalos sin actividad (`SR-RF-88.2`, `SR-RF-89.1`). | • NO almacena datos personales (PII) como nombres, correos, documentos o teléfonos. |
| • Expone distribuciones porcentuales de inventario por estado y cuentas por plan (`SR-RF-81`, `SR-RF-91.1`, `SR-RF-91.2`). | • NO consulta ni modifica directamente bases de datos de otros microservicios (*Database-per-Service*). |
| • Valida JWT Bearer (`HS256`) y cabeceras del Gateway aislando los datos al alcance autorizado (`STORE_ADMIN`, `FOUNDATION_ADMIN`, `PLATFORM_ADMIN`). | • NO permite consultas de estadísticas a usuarios con rol `BUYER` / `COMPRADOR` (retorna `403 Forbidden`). |

### Catálogo de Requerimientos Funcionales Oficiales (Versión 5.0)

Tras la refactorización a los **9 indicadores core** (`MetricCode`), el microservicio `ms-estadisticas` implementa los siguientes requerimientos funcionales oficiales:

1. **`SR-RF-81`** · *Consultar inventario por estado (`STORE_INVENTORY_BY_STATE`)*: `GET /api/v1/statistics/distributions/STORE_INVENTORY_BY_STATE` (Alcance `STORE`).
2. **`SR-RF-82.1`** · *Consultar total acumulado de prendas recibidas por fundación (`FOUNDATION_GARMENTS_RECEIVED`)*: `GET /api/v1/statistics/summary/totals/FOUNDATION_GARMENTS_RECEIVED` (Alcance `FOUNDATION`).
3. **`SR-RF-86.4`** · *Consultar total acumulado de prendas publicadas (`PLATFORM_GARMENT_PUBLICATIONS`)*: `GET /api/v1/statistics/summary/totals/PLATFORM_GARMENT_PUBLICATIONS` (Alcance `PLATFORM`).
4. **`SR-RF-86.5`** · *Consultar total acumulado de playlists creadas (`PLATFORM_PLAYLISTS_CREATED`)*: `GET /api/v1/statistics/summary/totals/PLATFORM_PLAYLISTS_CREATED` (Alcance `PLATFORM`).
5. **`SR-RF-86.6`** · *Consultar donaciones del mes en plataforma (`PLATFORM_MONTHLY_DONATIONS`)*: `GET /api/v1/statistics/summary/totals/PLATFORM_MONTHLY_DONATIONS` (Alcance `PLATFORM`).
6. **`SR-RF-88.2`** · *Consultar registros diarios de usuarios (`PLATFORM_USER_REGISTRATIONS`)*: `GET /api/v1/statistics/time-series/PLATFORM_USER_REGISTRATIONS` (Alcance `PLATFORM`).
7. **`SR-RF-89.1`** · *Consultar histórico mensual de ingresos por suscripciones (`PLATFORM_SUBSCRIPTION_REVENUE`)*: `GET /api/v1/statistics/time-series/PLATFORM_SUBSCRIPTION_REVENUE` (Alcance `PLATFORM`).
8. **`SR-RF-91.1`** · *Consultar cuentas de usuarios compradores por plan (`PLATFORM_BUYERS_BY_PLAN`)*: `GET /api/v1/statistics/distributions/PLATFORM_BUYERS_BY_PLAN` (Alcance `PLATFORM`).
9. **`SR-RF-91.2`** · *Consultar cuentas de almacenes por plan (`PLATFORM_STORES_BY_PLAN`)*: `GET /api/v1/statistics/distributions/PLATFORM_STORES_BY_PLAN` (Alcance `PLATFORM`).

> [!NOTE]
> **Consolidación del Alcance:** Con el fin de eliminar métricas redundantes y garantizar contratos estables en integración, el microservicio se consolidó en 3 familias de consulta (`/summary/totals`, `/time-series`, `/distributions`) sobre los 9 indicadores anteriores y 7 routing keys exactas de RabbitMQ.

### Arquetipos de Moda y Personajes de StyleRadar

Como servicio transversal de analítica para tiendas, fundaciones y administración de plataforma, `ms-estadisticas` articula la lectura agregada del ecosistema alrededor de los perfiles comerciales y solidarios de StyleRadar (**Antonia — Sastre** para gestión de tiendas e inventario y **Tomás — Esencial** para fundaciones y economía circular), brindando visibilidad operativa a cada actor sin exponer información personal de los compradores.

---

[⬅ Volver al README Principal](../../README.md)
