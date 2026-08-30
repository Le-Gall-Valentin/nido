# ---- Build stage ----
# frontend-maven-plugin downloads its own pinned Node during the build,
# so no Node base image is needed here.
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app
COPY . .
RUN bash mvnw -B -DskipTests package

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app
RUN addgroup --system nido && adduser --system --ingroup nido nido
COPY --from=build /app/target/*.jar app.jar
USER nido
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
