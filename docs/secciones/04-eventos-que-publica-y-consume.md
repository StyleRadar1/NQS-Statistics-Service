[⬅ Volver al README Principal](../../README.md)

---

## 4. EVENTOS QUE PUBLICA Y CONSUME

La comunicación asíncrona hacia `ms-estadisticas` se realiza a través de **RabbitMQ 3.12** (`5672` local / `5671` AMQPS TLS en QA y PROD) utilizando un Topic Exchange durable. El microservicio actúa **exclusivamente como consumidor** y no publica eventos en el bus.

* **Exchange:** `styleradar.exchange` (`topic`, `durable = true`)
* **Cola Consumidora:** `styleradar.estadisticas.eventos-dominio.q` (`durable = true`)
* **Dead Letter Queue (DLQ):** `styleradar.estadisticas.dlq` (enlazada mediante default exchange `""` con `x-dead-letter-routing-key`)
* **Política de Reintentos:** Backoff exponencial (`1s` inicial, multiplicador `2.0`, máx. `10s`, hasta `4` intentos). Errores de deserialización o validación (`MessageConversionException`, `IllegalArgumentException`, `AmqpRejectAndDontRequeueException`) se rechazan directo a DLQ sin reencolar.
* **Contrato Completo con Productores:** [`../contratos-eventos-microservicios.md`](../contratos-eventos-microservicios.md)

### Matriz de Eventos AMQP

| Tipo | Routing Key | Cola (Queue) | Microservicio Emisor &rarr; Receptor | Cuándo se Dispara / Acción en `ms-estadisticas` |
| :---: | :--- | :--- | :--- | :--- |
| **CONSUME** | `styleradar.usuarios.usuario.registrado` | `styleradar.estadisticas.eventos-dominio.q` | `ms-usuarios` (`:8120`) &rarr; `ms-estadisticas` | Incrementa registros diarios (`PLATFORM_USER_REGISTRATIONS`); si el rol es comprador (`BUYER`/`COMPRADOR`), suma `+1` a `PLAN_RADAR` en `PLATFORM_BUYERS_BY_PLAN`. |
| **CONSUME** | `styleradar.inventario.actualizado` *(alias `styleradar.inventario.stock.actualizado`)* | `styleradar.estadisticas.eventos-dominio.q` | `ms-inventario` (`:8200`) &rarr; `ms-estadisticas` | Clasifica `stockDisponible` (`0` = `OUT_OF_STOCK`, `1..3` = `LOW_STOCK`, `>=4` = `AVAILABLE`) y transiciona contadores de `STORE_INVENTORY_BY_STATE` por tienda (`tiendaId`/`almacenId`/`sedeId`). |
| **CONSUME** | `styleradar.catalogo.prenda.publicada` | `styleradar.estadisticas.eventos-dominio.q` | `ms-item-catalogo` (`:8220`) &rarr; `ms-estadisticas` | Incrementa el acumulado diario de publicaciones de prendas en `PLATFORM_GARMENT_PUBLICATIONS`. |
| **CONSUME** | `styleradar.playlist.creada` | `styleradar.estadisticas.eventos-dominio.q` | `ms-playlist` (`:8260`) &rarr; `ms-estadisticas` | Incrementa el acumulado diario de playlists creadas en `PLATFORM_PLAYLISTS_CREATED`. |
| **CONSUME** | `styleradar.pagos.pago.confirmado` | `styleradar.estadisticas.eventos-dominio.q` | `ms-pagos-suscripciones` (`:8400`) &rarr; `ms-estadisticas` | Si el pago está `APPROVED`/`APROBADO` y `monto > 0`, suma el valor en COP a `PLATFORM_SUBSCRIPTION_REVENUE`. |
| **CONSUME** | `styleradar.pagos.suscripcion.plan.actualizado` | `styleradar.estadisticas.eventos-dominio.q` | `ms-pagos-suscripciones` (`:8400`) &rarr; `ms-estadisticas` | Actualiza la transición `planAnterior` &rarr; `planActual` en `PLATFORM_BUYERS_BY_PLAN` o `PLATFORM_STORES_BY_PLAN` según `tipoSuscriptor`. |
| **CONSUME** | `styleradar.donaciones.completada` | `styleradar.estadisticas.eventos-dominio.q` | `ms-segunda-vida-donaciones` (`:8520`) &rarr; `ms-estadisticas` | Incrementa `FOUNDATION_GARMENTS_RECEIVED` (en `cantidad`, o `1` por defecto) para la fundación y suma `+1` en `PLATFORM_MONTHLY_DONATIONS`. |
| **PUBLICA** | `N/A` | — | — | `ms-estadisticas` no publica eventos de dominio hacia RabbitMQ (`adapter/out/messaging/` vacío). |

### Tipos de Evento (`DomainEventType`) e Idempotencia Transaccional

`DomainEventMapper` traduce cada routing key o campo `eventType` a uno de los 7 tipos internos de `DomainEventType`:

* `USER_REGISTERED`, `INVENTORY_STATE_CHANGED`, `GARMENT_PUBLISHED`, `PLAYLIST_CREATED`, `SUBSCRIPTION_PAYMENT_PROCESSED`, `SUBSCRIPTION_PLAN_CHANGED`, `DONATION_COMPLETED`.
* **Zona Horaria:** Todos los timestamps (`occurredAt`, `timestamp`, `fechaEvento`, `fechaCreacion`, `occurredOn`) se normalizan a `America/Bogota` antes de agrupar en `metric_daily`.
* **Idempotencia (`processed_event`):** Antes de procesar, `DomainEventListener` verifica `processedEventRepository.existsById(event.eventId())` y, dentro de un bloque `TransactionTemplate`, inserta el registro en `processed_event` junto con la actualización de métricas en PostgreSQL. Mensajes duplicados o concurrentes (`DataIntegrityViolationException`) se descartan de forma segura sin duplicar conteos.

<details>
<summary><b>Click para ver Esquemas JSON de Eventos Consumidos (DomainEventPayload)</b></summary>

<br>

#### Ejemplo 1: Cambio de Inventario (`styleradar.inventario.actualizado`)

```json
{
  "eventId": "9c2e8a74-1f3b-4c5d-8e9a-0b1c2d3e4f5a",
  "eventType": "styleradar.inventario.actualizado",
  "timestamp": "2026-11-24T23:00:00Z",
  "data": {
    "tiendaId": "store-17",
    "sedeId": "fc942335-ab30-350a-b6a5-577cf0ca2fb5",
    "stockAnterior": null,
    "stockDisponible": 10
  }
}
```

#### Ejemplo 2: Pago Confirmado (`styleradar.pagos.pago.confirmado`)

```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "eventType": "styleradar.pagos.pago.confirmado",
  "timestamp": "2026-11-24T15:00:00Z",
  "data": {
    "estado": "APPROVED",
    "monto": 14900,
    "moneda": "COP"
  }
}
```

#### Ejemplo 3: Donación Completada (`styleradar.donaciones.completada`)

```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440010",
  "eventType": "styleradar.donaciones.completada",
  "occurredAt": "2026-11-24T15:00:00Z",
  "data": {
    "fundacionId": "3",
    "cantidad": 4
  }
}
```

</details>

---

[⬅ Volver al README Principal](../../README.md)
