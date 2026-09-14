package com.nido.api;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * The Postgres and Redis every integration test talks to — one pair for the whole run.
 *
 * <p>Each test class used to declare its own. Because the fields were {@code static} per class, that
 * meant a fresh pair per class: measured on a single run, <b>83 containers</b>, and worse, a
 * different JDBC URL per class. Spring caches a test context by its configuration, so a URL that
 * changes every class means the cache never hits: <b>41 contexts</b> booted, <b>2911 Liquibase
 * changesets</b> replayed, 76 seconds spent on startup alone — and, on a machine under load, the
 * last classes alphabetically failing with connection refused because Docker had run out.
 *
 * <p>Sharing the containers makes the configuration identical, so the context is built once and
 * reused, which is where most of the time goes. The containers are never stopped: Ryuk removes them
 * when the JVM exits, and stopping them between classes would defeat the point.
 *
 * <p><b>What this asks of the tests.</b> They no longer get a fresh database per class, so each one
 * is responsible for the state it needs. Every class that asserts on the whole table — "no
 * invitations remain", "no memberships remain" — already clears it in {@code @BeforeEach}, and the
 * classes that do not clear anything scope everything to a space they create per test. That was
 * checked class by class before this change, not assumed, and the suite is run in reverse and random
 * order afterwards to catch what reading missed.
 */
public class SharedContainers {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @ServiceConnection
    @SuppressWarnings("resource")
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
}
