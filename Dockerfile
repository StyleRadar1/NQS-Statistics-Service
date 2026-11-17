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

# Instalar certificado público de la CA de RabbitMQ y truststore
COPY rabbitmq-truststore.jks /app/rabbitmq-truststore.jks
COPY rabbitmq-ca.pem /tmp/rabbitmq-ca.pem
RUN keytool -importcert -trustcacerts -cacerts -storepass changeit -noprompt -alias styleradar-rabbitmq-ca -file /tmp/rabbitmq-ca.pem \
    && rm -f /tmp/rabbitmq-ca.pem

# Variables de entorno con valores por defecto (se sobreescriben en Compose)
ENV SPRING_PROFILES_ACTIVE=docker
ENV SERVER_PORT=8420

EXPOSE 8420

# Arrancar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]