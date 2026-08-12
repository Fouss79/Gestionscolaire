# =========================================================
# ÉTAPE 1 : BUILD
# =========================================================

FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Maven Wrapper
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

RUN chmod +x mvnw && ./mvnw clean package -DskipTests

# =========================================================
# ÉTAPE 2 : RUNTIME
# =========================================================

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]