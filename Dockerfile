# ─── Etapa 1: Compilar con Maven ───────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Copiar pom.xml primero para cachear dependencias
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copiar código fuente y compilar
COPY src ./src
RUN mvn clean package -DskipTests -q

# ─── Etapa 2: Imagen final ligera ──────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copiar solo el JAR de la etapa anterior
COPY --from=builder /app/target/*.jar app.jar

# Instalar certificado público de la CA de RabbitMQ y generar truststore PKCS12
# cuando se suministra RABBITMQ_CA_PEM_BASE64 en tiempo de build (CI/CD)
ARG RABBITMQ_CA_PEM_BASE64=""
RUN if [ -n "$RABBITMQ_CA_PEM_BASE64" ]; then \
        printf '%s' "$RABBITMQ_CA_PEM_BASE64" | base64 -d > /tmp/rabbitmq-ca.pem && \
        keytool -importcert -trustcacerts -cacerts -storepass changeit -noprompt -alias styleradar-rabbitmq-ca -file /tmp/rabbitmq-ca.pem && \
        keytool -importcert -keystore /app/rabbitmq-truststore.jks -storetype PKCS12 -storepass changeit -noprompt -alias styleradar-rabbitmq-ca -file /tmp/rabbitmq-ca.pem && \
        rm -f /tmp/rabbitmq-ca.pem; \
    fi

# Variables de entorno con valores por defecto (se sobreescriben en Compose)
ENV SPRING_PROFILES_ACTIVE=docker
ENV SERVER_PORT=8420

EXPOSE 8420

# Arrancar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]
