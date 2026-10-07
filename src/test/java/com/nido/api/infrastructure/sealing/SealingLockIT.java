package com.nido.api.infrastructure.sealing;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.nido.api.IntegrationTestConfig;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTestConfig
class SealingLockIT {

    @Autowired SealingLock lock;
    @Autowired DataSource dataSource;

    @Test
    void an_instance_that_finds_the_lock_taken_says_it_waits_then_works_once_it_is_free() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(SealingLock.class);
        ListAppender<ILoggingEvent> logged = new ListAppender<>();
        logged.start();
        logger.addAppender(logged);
        AtomicBoolean worked = new AtomicBoolean();
        try (Connection otherInstance = dataSource.getConnection(); Statement statement = otherInstance.createStatement()) {
            statement.execute("SELECT pg_advisory_lock(hashtext('nido-sealed-values'))");
            CompletableFuture<Void> waiting = CompletableFuture.runAsync(() -> lock.whileHeld(() -> worked.set(true)));
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (logged.list.isEmpty() && System.nanoTime() < deadline) {
                Thread.onSpinWait();
            }

            assertThat(worked).as("nothing done while the other instance holds the lock").isFalse();
            assertThat(logged.list).extracting(ILoggingEvent::getFormattedMessage)
                .containsExactly("Another instance is sealing values: waiting for it to finish");

            statement.execute("SELECT pg_advisory_unlock(hashtext('nido-sealed-values'))");
            waiting.get(10, TimeUnit.SECONDS);
            assertThat(worked).isTrue();
        } finally {
            logger.detachAppender(logged);
        }
    }

    @Test
    void a_free_lock_is_taken_without_a_word() {
        Logger logger = (Logger) LoggerFactory.getLogger(SealingLock.class);
        ListAppender<ILoggingEvent> logged = new ListAppender<>();
        logged.start();
        logger.addAppender(logged);
        try {
            AtomicBoolean worked = new AtomicBoolean();
            lock.whileHeld(() -> worked.set(true));

            assertThat(worked).isTrue();
            assertThat(logged.list).isEmpty();
        } finally {
            logger.detachAppender(logged);
        }
    }

    @Test
    void work_that_fails_gives_the_lock_back() throws Exception {
        assertThatThrownBy(() -> lock.whileHeld(() -> {
            throw new IllegalStateException("could not seal");
        })).hasMessage("could not seal");

        try (Connection otherInstance = dataSource.getConnection(); Statement statement = otherInstance.createStatement();
             ResultSet taken = statement.executeQuery("SELECT pg_try_advisory_lock(hashtext('nido-sealed-values'))")) {
            taken.next();
            assertThat(taken.getBoolean(1)).as("free for the next instance").isTrue();
            statement.execute("SELECT pg_advisory_unlock(hashtext('nido-sealed-values'))");
        }
    }
}
