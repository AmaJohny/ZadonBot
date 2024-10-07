FROM maven:3.9.9-eclipse-temurin-21 as builder
WORKDIR /app
COPY . /app/.
RUN mvn -f /app/pom.xml clean package -Dmaven.test.skip=true

FROM eclipse-temurin:21-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar /app/*.jar
RUN apk add --no-cache git
RUN apk add --no-cache bash
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=PROM", "/app/*.jar" ]