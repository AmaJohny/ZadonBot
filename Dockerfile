FROM maven:3.9.9-eclipse-temurin-21 as builder
WORKDIR /app
COPY . /app/.
RUN mvn -f /app/pom.xml clean package -Dmaven.test.skip=true

FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY --from=builder /app/target/*.jar /app/*.jar
#EXPOSE 8181
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=PROM", "/app/*.jar" ]