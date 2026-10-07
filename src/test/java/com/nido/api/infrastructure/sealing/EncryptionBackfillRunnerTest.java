package com.nido.api.infrastructure.sealing;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EncryptionBackfillRunnerTest {

    private final SealedColumn titles = SealedColumn.ofSpace("tasks", "title_encrypted");
    private final SealedColumn items = SealedColumn.ofSpace("shopping_items", "name_encrypted");
    private final ExistingCiphertextCheck finance = mock(ExistingCiphertextCheck.class);
    private final SealedValueMigration migration = mock(SealedValueMigration.class);
    private final SpaceSealers sealers = space -> { throw new AssertionError("not used"); };
    private final SealingLock lock = mock(SealingLock.class);
    private final PendingVacuum pendingVacuum = mock(PendingVacuum.class);
    private final TableVacuum vacuum = mock(TableVacuum.class);
    private final StartKey knownKey = mock(StartKey.class);
    private final StartKey newKey = mock(StartKey.class);
    private final EncryptionBackfillRunner runner = runner(knownKey);

    private EncryptionBackfillRunner runner(StartKey key) {
        return new EncryptionBackfillRunner(List.of(SealedColumns.of(titles), SealedColumns.of(items)), List.of(finance), migration,
            sealers, lock, vacuum, pendingVacuum, key);
    }

    private final Logger logger = (Logger) LoggerFactory.getLogger(EncryptionBackfillRunner.class);
    private final ListAppender<ILoggingEvent> logged = new ListAppender<>();

    @BeforeEach
    void listen() {
        when(newKey.fingerprintRecordedAtThisStart()).thenReturn(true);
        logged.start();
        logger.addAppender(logged);
        doAnswer(call -> {
            ((Runnable) call.getArgument(0)).run();
            return null;
        }).when(lock).whileHeld(any());
    }

    @AfterEach
    void stopListening() {
        logger.detachAppender(logged);
    }

    @Test
    void with_nothing_left_to_seal_it_checks_nothing_and_rewrites_nothing() {
        runner.afterSingletonsInstantiated();

        verify(finance, never()).verify();
        verify(migration, never()).migrate(any(), any());
        verifyNoInteractions(vacuum, lock);
        assertThat(logged.list).isEmpty();
    }

    @Test
    void the_key_is_checked_under_the_lock_before_anything_is_sealed() {
        when(migration.pending(titles)).thenReturn(true);
        when(migration.migrate(titles, sealers)).thenReturn(4);

        runner.afterSingletonsInstantiated();

        InOrder order = inOrder(lock, finance, migration, vacuum);
        order.verify(lock).whileHeld(any());
        order.verify(finance).verify();
        order.verify(migration).migrate(titles, sealers);
        order.verify(vacuum).vacuumFull(Set.of("tasks", "shopping_items"));
    }

    @Test
    void a_key_that_does_not_decrypt_stops_the_start_before_any_rewrite() {
        when(migration.pending(items)).thenReturn(true);
        doThrow(new IllegalStateException("does not decrypt")).when(finance).verify();

        assertThatThrownBy(runner::afterSingletonsInstantiated).hasMessageContaining("does not decrypt");

        verify(migration, never()).migrate(any(), any());
        verifyNoInteractions(vacuum);
    }

    @Test
    void every_declared_table_is_vacuumed_and_only_the_rewritten_columns_are_logged() {
        // A start cut short leaves tables sealed then, whose columns in clear 068 has since dropped
        // without rewriting them: their earlier versions are still in their files.
        when(migration.pending(items)).thenReturn(true);
        when(migration.migrate(items, sealers)).thenReturn(2);
        when(vacuum.vacuumFull(any())).thenReturn(true);

        runner.afterSingletonsInstantiated();

        verify(vacuum).vacuumFull(Set.of("tasks", "shopping_items"));
        assertThat(logged.list).extracting(ILoggingEvent::getFormattedMessage)
            .containsExactly("Sealed 2 values of shopping_items.name_encrypted that earlier versions stored");
    }

    @Test
    void work_another_instance_finished_while_this_one_waited_is_not_done_again() {
        when(migration.pending(titles)).thenReturn(true, false);

        runner.afterSingletonsInstantiated();

        verify(migration, never()).migrate(any(), any());
        verifyNoInteractions(vacuum);
    }

    @Test
    void a_key_new_to_the_installation_is_checked_even_with_nothing_left_to_seal() {
        runner(newKey).afterSingletonsInstantiated();

        verify(finance).verify();
        verify(migration, never()).migrate(any(), any());
        verifyNoInteractions(vacuum);
    }

    @Test
    void a_new_key_that_does_not_decrypt_has_its_fingerprint_taken_back_so_the_right_key_can_start_next() {
        doThrow(new IllegalStateException("does not decrypt")).when(finance).verify();

        assertThatThrownBy(runner(newKey)::afterSingletonsInstantiated).hasMessageContaining("does not decrypt");

        verify(newKey).forgetFingerprintRecordedAtThisStart();
    }

    @Test
    void a_known_key_that_does_not_decrypt_keeps_its_fingerprint() {
        when(migration.pending(titles)).thenReturn(true);
        doThrow(new IllegalStateException("does not decrypt")).when(finance).verify();

        assertThatThrownBy(runner::afterSingletonsInstantiated).hasMessageContaining("does not decrypt");

        verify(knownKey, never()).forgetFingerprintRecordedAtThisStart();
    }

    @Test
    void the_vacuum_is_owed_before_the_first_row_is_rewritten_and_settled_once_it_ran() {
        when(migration.pending(titles)).thenReturn(true);
        when(vacuum.vacuumFull(any())).thenReturn(true);

        runner.afterSingletonsInstantiated();

        InOrder order = inOrder(pendingVacuum, migration, vacuum);
        order.verify(pendingVacuum).owe();
        order.verify(migration).migrate(titles, sealers);
        order.verify(vacuum).vacuumFull(Set.of("tasks", "shopping_items"));
        order.verify(pendingVacuum).settle();
    }

    @Test
    void a_vacuum_a_stopped_start_still_owes_is_run_with_nothing_left_to_seal() {
        // A start stopped after its last batch and before its VACUUM: nothing is pending any more.
        when(pendingVacuum.isOwed()).thenReturn(true);
        when(vacuum.vacuumFull(any())).thenReturn(true);

        runner.afterSingletonsInstantiated();

        InOrder order = inOrder(vacuum, pendingVacuum);
        order.verify(vacuum).vacuumFull(Set.of("tasks", "shopping_items"));
        order.verify(pendingVacuum).settle();
        verify(migration, never()).migrate(any(), any());
    }

    @Test
    void a_vacuum_that_could_not_run_on_every_table_stays_owed() {
        when(migration.pending(titles)).thenReturn(true);
        when(vacuum.vacuumFull(any())).thenReturn(false);

        runner.afterSingletonsInstantiated();

        verify(pendingVacuum).owe();
        verify(pendingVacuum, never()).settle();
    }
}
