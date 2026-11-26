[⬅ Volver al README Principal](../../README.md)

---

## 7. VARIABLES DE ENTORNO

Todas las configuraciones dependientes de entorno se gestionan mediante variables externalizadas conformes al estándar **Twelve-Factor App**.

> [!IMPORTANT]
> **Configuración de Conexión según el Entorno donde corre Spring Boot:**
> * **Si Spring se ejecuta desde el equipo con `./mvnw` (Modo Host Local con perfil `dev`):**
>   - **PostgreSQL (`styleradar_statistics`):** Mapeado en `localhost:5435` (`DB_HOST=localhost`, `DB_PORT=5435`).
>   - **RabbitMQ:** Mapeado en `localhost:5672` sin TLS (`RABBITMQ_HOST=localhost`, `RABBITMQ_PORT=5672`, `RABBITMQ_SSL_ENABLED=false`).
> * **Si Spring se ejecuta dentro de Docker Compose (Red interna `styleradar-net`):**
>   - **PostgreSQL (`styleradar_statistics`):** Se resuelve internamente en `postgres-estadisticas:5432` (`DB_HOST=postgres-estadisticas`, `DB_PORT=5432`).
>   - **RabbitMQ:** Se resuelve internamente en `rabbitmq-estadisticas:5672` (`RABBITMQ_HOST=rabbitmq-estadisticas`, `RABBITMQ_PORT=5672`, `RABBITMQ_SSL_ENABLED=false`).
> * **En Azure QA / PROD (Perfil base `application.yml`):**
>   - **RabbitMQ:** Usa TLS sobre el puerto `5671` (`RABBITMQ_SSL_ENABLED=true`) y el TrustStore `/app/rabbitmq-truststore.jks` empaquetado en la imagen durante el build de CI/CD.
>   - *Nota sobre `Dockerfile`:* La imagen define por defecto `SPRING_PROFILES_ACTIVE=docker`; al no existir un archivo `application-docker.yml` separado, Spring carga `application.yml` y sobreescribe valores mediante variables de entorno (en `docker-compose.yml` se fija `SPRING_PROFILES_ACTIVE=dev`).

| Variable | Descripción | Valor Local con `./mvnw` (`dev`) | Valor en Docker Compose | Requerido en QA / PROD |
| :--- | :--- | :--- | :--- | :---: |
| `SERVER_PORT` | Puerto HTTP del microservicio | `8420` | `8420` | Sí |
| `SPRING_PROFILES_ACTIVE` | Perfil activo de Spring Boot (`dev`, o base en cloud) | `dev` | `dev` | Sí |
| `DB_HOST` | Hostname o IP del servidor PostgreSQL 16 | `localhost` | `postgres-estadisticas` | Sí |
| `DB_PORT` | Puerto de conexión a PostgreSQL | `5435` | `5432` | Sí |
| `DB_NAME` | Nombre de la base de datos aislada | `styleradar_statistics` | `styleradar_statistics` | Sí |
| `DB_USER` | Usuario de base de datos PostgreSQL | `styleradar_user` | `styleradar_user` | Sí |
| `DB_PASSWORD` | Contraseña de base de datos PostgreSQL | `styleradar_secret` | `styleradar_secret` | Sí |
| `RABBITMQ_HOST` | Hostname del broker RabbitMQ | `localhost` | `rabbitmq-estadisticas` | Sí |
| `RABBITMQ_PORT` | Puerto AMQP/AMQPS de RabbitMQ (`5672` local / `5671` TLS) | `5672` | `5672` | Sí |
| `RABBITMQ_USER` | Usuario principal de RabbitMQ *(fallback: `RABBITMQ_USERNAME`)* | `guest` | `guest` | Sí |
| `RABBITMQ_PASSWORD` | Contraseña de conexión RabbitMQ | `guest` | `guest` | Sí |
| `RABBITMQ_VHOST` | Virtual Host de RabbitMQ | `/` | `/` | Sí |
| `RABBITMQ_SSL_ENABLED` | Habilita TLS/AMQPS hacia RabbitMQ | `false` | `false` | Sí (`true`) |
| `RABBITMQ_TRUSTSTORE` | Ruta del TrustStore PKCS12 con la CA de RabbitMQ | *(No requerido en `dev`)* | *(No requerido en `dev`)* | Sí (`file:/app/rabbitmq-truststore.jks`) |
| `RABBITMQ_TRUSTSTORE_PASSWORD` | Contraseña del TrustStore PKCS12 | *(No requerido en `dev`)* | *(No requerido en `dev`)* | Sí (`changeit`) |
| `RABBITMQ_TRUSTSTORE_TYPE` | Tipo de keystore para TLS de RabbitMQ | *(No requerido en `dev`)* | *(No requerido en `dev`)* | Sí (`PKCS12`) |
| `RABBITMQ_SSL_VERIFY_HOSTNAME` | Verificación de hostname del certificado TLS | *(No requerido en `dev`)* | *(No requerido en `dev`)* | Sí (`true`) |
| `JWT_SECRET` | Clave secreta HMAC-SHA256 en Base64 compartida con `ms-auth` | *(Inyectada por `.env`)* | *(Inyectada por `.env`)* | Sí |

<details>
<summary><b>Click para ver la Plantilla de Archivo .env.example Completa</b></summary>

<br>

```dotenv
# ==============================================================================
# STYLERADAR · VARIABLES DE ENTORNO LOCALES (.env)
# Microservicio: NQS-Statistics-Service (ms-estadisticas) · Puerto 8420
# ==============================================================================
SERVER_PORT=8420
SPRING_PROFILES_ACTIVE=dev

# ------------------------------------------------------------------------------
# Persistencia PostgreSQL (styleradar_statistics)
# Si ejecutas con ./mvnw -> DB_HOST=localhost, DB_PORT=5435
# Si ejecutas con Docker Compose -> DB_HOST=postgres-estadisticas, DB_PORT=5432
# ------------------------------------------------------------------------------
DB_HOST=localhost
DB_PORT=5435
DB_NAME=styleradar_statistics
DB_USER=styleradar_user
DB_PASSWORD=styleradar_secret

# ------------------------------------------------------------------------------
# Broker de Eventos (RabbitMQ)
# Si ejecutas con ./mvnw -> RABBITMQ_HOST=localhost, RABBITMQ_PORT=5672, SSL=false
# Si ejecutas con Docker Compose -> RABBITMQ_HOST=rabbitmq-estadisticas, RABBITMQ_PORT=5672, SSL=false
# En QA/PROD (Azure) -> RABBITMQ_PORT=5671, RABBITMQ_SSL_ENABLED=true
# ------------------------------------------------------------------------------
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USER=guest
RABBITMQ_PASSWORD=guest
RABBITMQ_VHOST=/
RABBITMQ_SSL_ENABLED=false
# RABBITMQ_TRUSTSTORE=file:/app/rabbitmq-truststore.jks
# RABBITMQ_TRUSTSTORE_PASSWORD=changeit
# RABBITMQ_TRUSTSTORE_TYPE=PKCS12
# RABBITMQ_SSL_VERIFY_HOSTNAME=true

# ------------------------------------------------------------------------------
# Seguridad JWT Bearer (HS256 compartido con ms-auth)
# ------------------------------------------------------------------------------
JWT_SECRET=dGVzdC1zZWNyZXQta2V5LXRlc3Qtc2VjcmV0LWtleS10ZXN0LXNlY3JldA==
```

</details>

---

[⬅ Volver al README Principal](../../README.md)
