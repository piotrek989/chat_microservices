# ETAP 1: Budowanie (używamy obrazu z Mavenem)
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app

# Kopiujemy pliki projektu
COPY pom.xml .
COPY src ./src

# Budujemy JAR-a (pomijamy testy dla szybkości)
RUN mvn clean package -DskipTests

# ETAP 2: Finalny obraz (tylko środowisko uruchomieniowe)
FROM eclipse-temurin:17-jre
WORKDIR /app

# Kopiujemy zbudowany plik z poprzedniego etapu
# Używamy wildcard (*.jar), żeby nie martwić się o wersję w nazwie
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]