# ETAP 1: Budowanie (używamy obrazu z Mavenem)
# docker pobiera gotowy obraz z Mavenem i JDK 17, który jest potrzebny do budowania naszej aplikacji
FROM maven:3.8.5-openjdk-17 AS build 
#nazwanie tego etapu "build", dzięki czemu później możemy się do niego odwołać
WORKDIR /app
#ustawia katalog roboczy i teraz w /app komendy sie wkonują

# Kopiujemy pliki projektu
COPY pom.xml .
COPY src ./src
#kopiujemy z komputera do kontenera

# Budujemy JAR-a (pomijamy testy dla szybkości)
RUN mvn clean package -DskipTests

# ETAP 2: Finalny obraz (tylko środowisko uruchomieniowe)
FROM eclipse-temurin:17-jre
WORKDIR /app

# Kopiujemy zbudowany plik z poprzedniego etapu
# Używamy wildcard (*.jar), żeby nie martwić się o wersję w nazwie
COPY --from=build /app/target/*.jar app.jar
#skoupiuj jara z etapu build do tego etapu i nazwij go app.jar

#appka dziala na porcie 8080
EXPOSE 8080

#odpalenie Spring Boota
#glowny proces  uruchomiony w momencie startu kontenera
ENTRYPOINT ["java", "-jar", "app.jar"]