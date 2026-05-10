FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /app
ENV LANG=C.utf8
ENV LC_ALL=C.utf8

COPY gradle/ gradle/
COPY gradlew build.gradle.kts settings.gradle.kts ./
RUN chmod +x gradlew && sed -i 's/\r$//' gradlew

# Download dependencies (cached layer before copying source)
RUN ./gradlew dependencies --no-daemon -q 2>/dev/null; exit 0

COPY src/ src/
RUN ./gradlew build -x test --no-daemon -Pvaadin.productionMode=true

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=builder /app/build/libs/the-chinese-room-0.0.1-SNAPSHOT.jar app.jar
ENV LANG=C.utf8
ENV LC_ALL=C.utf8
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
