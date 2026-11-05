# Backend DapurKita. Tes tidak dijalankan di sini karena butuh Docker; jalankan `mvn test` sebelum deploy.
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre
RUN useradd --system --create-home --uid 1001 dapurkita
WORKDIR /app
COPY --from=build /app/target/dapurkita-backend-*.jar app.jar
USER dapurkita
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
