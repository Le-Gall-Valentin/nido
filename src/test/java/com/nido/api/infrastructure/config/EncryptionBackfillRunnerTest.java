package com.nido.api.infrastructure.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.nido.api.instance.application.port.in.ConfirmEncryptionKeyUseCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EncryptionBackfillRunnerTest {

    private final EncryptionBackfill shopping = mock(EncryptionBackfill.class);
    private final EncryptionBackfill tasks = mock(EncryptionBackfill.class);
    private final ExistingCiphertextCheck finance = mock(ExistingCiphertextCheck.class);
    private final TableVacuum vacuum = mock(TableVacuum.class);
    private final ConfirmEncryptionKeyUseCase key = mock(ConfirmEncryptionKeyUseCase.class);
    private final EncryptionBackfillRunner runner =
        new EncryptionBackfillRunner(List.of(shopping, tasks), List.of(finance), vacuum, key);

    private final Logger logger = (Logger) LoggerFactory.getLogger(EncryptionBackfillRunner.class);
    private final ListAppender<ILoggingEvent> logged = new ListAppender<>();

    @BeforeEach
    void listen() {
        logged.start();
        logger.addAppender(logged);
    }

    @AfterEach
    void stopListening() {
        logger.detachAppender(logged);
    }

    @Test
    void with_nothing_left_in_clear_it_checks_nothing_and_rewrites_nothing() {
        runner.afterSingletonsInstantiated();

        verify(finance, never()).verify();
        verify(shopping, never()).run();
        verify(tasks, never()).run();
        verifyNoInteractions(vacuum);
        assertThat(logged.list).isEmpty();
    }

    @Test
    void the_key_is_checked_before_anything_is_encrypted() {
        when(tasks.pending()).thenReturn(true);
        when(tasks.run()).thenReturn(Map.of("tasks", 3));

        runner.afterSingletonsInstantiated();

        InOrder order = inOrder(finance, shopping, tasks, vacuum);
        order.verify(finance).verify();
        order.verify(shopping).run();
        order.verify(tasks).run();
        order.verify(vacuum).vacuumFull(Set.of("tasks"));
    }

    @Test
    void a_key_that_does_not_decrypt_stops_the_start_before_any_rewrite() {
        when(shopping.pending()).thenReturn(true);
        doThrow(new IllegalStateException("does not decrypt")).when(finance).verify();

        assertThatThrownBy(runner::afterSingletonsInstantiated).hasMessageContaining("does not decrypt");

        verify(shopping, never()).run();
        verify(tasks, never()).run();
        verifyNoInteractions(vacuum);
    }

    @Test
    void only_the_tables_it_rewrote_are_vacuumed_and_logged() {
        when(shopping.pending()).thenReturn(true);
        when(shopping.run()).thenReturn(Map.of("shopping_items", 2, "shopping_categories", 0));

        runner.afterSingletonsInstantiated();

        verify(vacuum).vacuumFull(Set.of("shopping_items"));
        assertThat(logged.list).extracting(ILoggingEvent::getFormattedMessage)
            .containsExactly("Encrypted 2 rows of shopping_items that earlier versions stored in clear");
    }

    @Test
    void a_key_new_to_the_installation_is_checked_even_with_nothing_left_in_clear() {
        when(key.recordedAtThisStart()).thenReturn(true);

        runner.afterSingletonsInstantiated();

        verify(finance).verify();
        verify(shopping, never()).run();
        verify(tasks, never()).run();
        verifyNoInteractions(vacuum);
    }

    @Test
    void a_new_key_that_does_not_decrypt_has_its_fingerprint_taken_back_so_the_right_key_can_start_next() {
        when(key.recordedAtThisStart()).thenReturn(true);
        doThrow(new IllegalStateException("does not decrypt")).when(finance).verify();

        assertThatThrownBy(runner::afterSingletonsInstantiated).hasMessageContaining("does not decrypt");

        verify(key).forgetFingerprintRecordedAtThisStart();
    }

    @Test
    void a_known_key_that_does_not_decrypt_keeps_its_fingerprint() {
        when(shopping.pending()).thenReturn(true);
        doThrow(new IllegalStateException("does not decrypt")).when(finance).verify();

        assertThatThrownBy(runner::afterSingletonsInstantiated).hasMessageContaining("does not decrypt");

        verify(key, never()).forgetFingerprintRecordedAtThisStart();
    }
}
