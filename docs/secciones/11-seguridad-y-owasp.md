[⬅ Volver al README Principal](../../README.md)

---

## 11. SEGURIDAD Y ANÁLISIS OWASP TOP 10:2025

El microservicio `NQS-Statistics-Service` implementa controles de autenticación sin estado, aislamiento estricto por alcance (*multi-tenant scope isolation*), idempotencia transaccional y protección de transporte conforme al análisis **OWASP Top 10:2025**.

* **Documento Técnico Oficial:** [`../security/SR_NQS_Seguridad_y_OWASP_2025.md`](../security/SR_NQS_Seguridad_y_OWASP_2025.md)

### Puntos de Control y Cumplimiento Normativo

1. **Autenticación Sin Estado y Validación JWT (`A04` / `A07`):**
   * `SecurityConfig` protege `/statistics` y `/statistics/**` en modo `STATELESS`, retornando **`401 Unauthorized`** cuando no existe identidad autenticada o el token es inválido.
   * `JwtTokenValidator` valida exclusivamente tokens **`HS256`** firmados con `JWT_SECRET` (Base64) mediante comparación en tiempo constante (`MessageDigest.isEqual`), verificando `exp`, `nbf`, `sub` y rol normalizado.
   * Como respaldo detrás del API Gateway, `GatewayHeaderAuthenticationFilter` admite cabeceras autoritativas (`X-User-Role`, `X-Store-Id`, `X-Foundation-Id`) únicamente cuando el actor está completo para su rol.

2. **Control de Acceso Autoritativo por Alcance (`A01` · Broken Access Control):**
   * `AuthenticatedScopeResolver` cruza el rol del actor autenticado con el `ScopeType` de cada `MetricCode` (`STORE`, `FOUNDATION`, `PLATFORM`).
   * Un `STORE_ADMIN` solo accede a métricas `STORE` de su propio `storeId`; un `FOUNDATION_ADMIN` solo a métricas `FOUNDATION` de su propio `foundationId`; y únicamente `PLATFORM_ADMIN`/`STYLERADAR_ADMIN` accede a métricas `PLATFORM`. Roles como `BUYER`/`COMPRADOR` reciben **`403 Forbidden`** (`code: ACCESS_DENIED`).

3. **Privacidad por Diseño (Zero-PII):**
   * `ms-estadisticas` únicamente persiste y expone conteos y series agregadas en `metric_daily` y `metric_distribution`, sin almacenar nombres, correos, documentos ni teléfonos de usuarios.

4. **Trazabilidad, Sanitización y Manejo de Errores (`A09` / `A10`):**
   * `CorrelationIdFilter` gestiona `X-Correlation-Id` / `X-Request-Id` en `MDC` filtrando longitud ($\le 128$) y caracteres de control para prevenir *Log Injection*.
   * `GlobalExceptionHandler` normaliza las excepciones de dominio, validación y enrutamiento bajo formato estándar RFC 7807 sin filtrar trazas de pila al cliente.

5. **Seguridad en Mensajería AMQPS e Idempotencia (`A04` / `A06`):**
   * En QA y PROD la conexión a RabbitMQ utiliza **TLS (`5671`)** con TrustStore PKCS12 (`/app/rabbitmq-truststore.jks`) generado en build desde `RABBITMQ_CA_PEM_BASE64` y `verify-hostname: true`.
   * La tabla `processed_event` garantiza dentro de la transacción ACID que eventos repetidos no alteren los conteos, y los mensajes malformados se aíslan en `styleradar.estadisticas.dlq`.

---

[⬅ Volver al README Principal](../../README.md)
