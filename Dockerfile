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
# MaxRAMPercentage, not -Xmx: the JVM defaults to a quarter of the container limit, which
# wastes most of a small container and silently shrinks if the limit is lowered. A percentage
# tracks whatever the orchestrator grants, leaving room for the metaspace, thread stacks and
# direct buffers the heap figure does not cover.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
