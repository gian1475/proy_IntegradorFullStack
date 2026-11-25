# =====================================================================
# B5 (despliegue): arma UN solo .jar con el frontend adentro y lo ejecuta.
# Lo usa Render (o cualquier servicio que acepte Docker). Guía: docs/DESPLIEGUE.md
# Las pruebas no se corren aquí (necesitan la BD): córranlas antes con ./mvnw test.
# =====================================================================

# 1) Frontend: compila Angular en modo producción (la API queda en "/api", misma dirección)
FROM node:22-alpine AS frontend
WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npx ng build

# 2) Backend: el pom.xml mete ../frontend/dist en static/ del .jar
FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /app/backend
COPY backend/pom.xml ./
RUN mvn -q dependency:go-offline
COPY backend/src ./src
COPY --from=frontend /app/frontend/dist /app/frontend/dist
RUN mvn -q -DskipTests package

# 3) Imagen final: solo Java 21 y el .jar. Las credenciales de Neon llegan como variables de entorno
#    (SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD): nunca van en la imagen.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=backend /app/backend/target/biblioteca-alejandria.jar app.jar
# D25: dentro de un contenedor Java usa por defecto solo 1/4 de la memoria para el heap (en Render, 512 MB = 128 MB)
#      y el sistema ya usa unos 240 MB al arrancar. Con MaxRAMPercentage=75 el heap puede llegar a 3/4 (unos 384 MB);
#      el resto queda para las clases, los hilos y el sistema operativo. Ver docs/MONITOREO.md
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
