# STAGE 1: Build Angular Frontend
FROM node:20-alpine AS frontend-build
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm install
COPY frontend/ ./
RUN npm run build -- --configuration production

# STAGE 2: Build Spring Boot Backend
FROM maven:3.9-eclipse-temurin-17-alpine AS backend-build
WORKDIR /app/backend
# Copy frontend build into backend resources/static
COPY --from=frontend-build /app/frontend/dist/frontend/browser /app/backend/src/main/resources/static
COPY backend/pom.xml ./
RUN mvn dependency:go-offline
COPY backend/src ./src
RUN mvn clean package -DskipTests

# STAGE 3: Run
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=backend-build /app/backend/target/*.jar app.jar
EXPOSE 8080
# Le profil 'prod' active la config MySQL de Railway
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
