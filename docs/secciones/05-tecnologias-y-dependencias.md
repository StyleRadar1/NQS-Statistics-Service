[⬅ Volver al README Principal](../../README.md)

---

## 5. TECNOLOGÍAS Y DEPENDENCIAS

El stack tecnológico de `NQS-Statistics-Service` cumple con el estándar de DOSW 2026 y refleja exactamente las dependencias declaradas en `pom.xml` y `docker-compose.yml`:

| Capa / Componente | Tecnología | Versión | Justificación Técnica |
| :--- | :--- | :--- | :--- |
| **Lenguaje Core** | Java LTS | `21` | Uso de `record` inmutables, `switch` expressions y tipado estricto en el modelo de dominio. |
| **Framework Empresarial** | Spring Boot | `3.2.9` | Gestión declarativa de Web MVC, Spring Data JPA, Spring AMQP y validación de contratos. |
| **Seguridad & Autenticación** | Spring Security + JWT (`HS256`) | `3.2.9` | Validación criptográfica de tokens (`JwtTokenValidator`), lectura de cabeceras del Gateway y aislamiento por alcance (`AuthenticatedScopeResolver`). |
| **Base de Datos** | PostgreSQL | `16` | Persistencia ACID para series diarias (`metric_daily`), distribuciones (`metric_distribution`) e idempotencia (`processed_event`), con índices optimizados (`V8`). |
| **Migraciones de BD** | Flyway | `10.10.0` | Evolución versionada del esquema SQL (`V1__init_schema.sql` a `V9__cleanup_obsolete_tables_and_metrics.sql`). |
| **Broker de Mensajería** | RabbitMQ | `3.12` (`3.12.14-management-alpine` en local) | Consumo desacoplado en `styleradar.exchange` (`5672` local / `5671` AMQPS TLS compartido en QA y PROD) con DLQ y backoff exponencial. |
| **Observabilidad** | Spring Boot Actuator | `3.2.9` | Exposición de endpoints operativos `/api/v1/actuator/health` e `/api/v1/actuator/info`. |
| **Documentación de API** | SpringDoc OpenAPI Starter | `2.5.0` | Generación automática de especificación OpenAPI 3 (`/api/v1/v3/api-docs`) y Swagger UI con esquema `bearerAuth`. |
| **Productividad & Mapeo** | Project Lombok + MapStruct | `1.18.38` / `1.5.5.Final` | Reducción de código repetitivo manteniendo compatibilidad con Java 21 mediante `lombok-mapstruct-binding`. |
| **Testing Automatizado** | JUnit 5 + Mockito + MockMvc + H2 | Spring Boot Test `3.2.9` | Pruebas unitarias de dominio/aplicación, filtros de seguridad, listeners AMQP y repositorios JPA con H2 en modo PostgreSQL. |
| **Métricas de Cobertura** | JaCoCo Maven Plugin | `0.8.12` | Regla estricta de cobertura de líneas $\ge 85\%$ verificada automáticamente en `./mvnw verify`. |

> [!NOTE]
> **Nota sobre Redis y Testcontainers:** A partir de la refactorización transaccional del consumo de eventos (`refactor/remove-redis`), `ms-estadisticas` ya no utiliza Redis ni dependencias en memoria intermedias; toda actualización e idempotencia ocurre directamente en PostgreSQL 16. Asimismo, aunque `pom.xml` declara `org.testcontainers` (`test`), la suite actual ejecuta de forma rápida y determinista sobre H2 en modo PostgreSQL.

---

[⬅ Volver al README Principal](../../README.md)
