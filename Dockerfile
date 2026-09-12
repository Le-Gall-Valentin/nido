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
# The management port is deliberately not exposed: the probe below runs inside the
# container and reaches it over loopback, so nothing outside ever needs a route to it.
#
# start-period covers the boot: Liquibase runs the migrations before the first request is served,
# and a container reported unhealthy while it is legitimately starting gets restarted into the same
# wait, forever. The interval is what decides how long a wedged process keeps taking traffic.
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -fsS http://localhost:${NIDO_MANAGEMENT_PORT:-8081}/actuator/health || exit 1
# MaxRAMPercentage, not -Xmx: the JVM defaults to a quarter of the container limit, which
# wastes most of a small container and silently shrinks if the limit is lowered. A percentage
# tracks whatever the orchestrator grants, leaving room for the metaspace, thread stacks and
# direct buffers the heap figure does not cover.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
