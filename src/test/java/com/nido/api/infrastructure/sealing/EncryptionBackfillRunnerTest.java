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
import static org.mockito.ArgumentMatchers.eq;
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
    private final RekeyMigration rekeying = mock(RekeyMigration.class);
    private final SpaceSealers sealers = space -> { throw new AssertionError("not used"); };
    private final LegacySpaceOpeners legacy = space -> { throw new AssertionError("not used"); };
    private final SealingLock lock = mock(SealingLock.class);
    private final PendingVacuum pendingVacuum = mock(PendingVacuum.class);
    private final TableVacuum vacuum = mock(TableVacuum.class);
    private final StartKey knownKey = mock(StartKey.class);
    private final StartKey unconfirmedKey = mock(StartKey.class);
    private final EncryptionBackfillRunner runner = runner(knownKey);

    private EncryptionBackfillRunner runner(StartKey key) {
        return new EncryptionBackfillRunner(List.of(SealedColumns.of(titles), SealedColumns.of(items)), List.of(), List.of(finance),
            migration, rekeying, sealers, legacy, lock, vacuum, pendingVacuum, key);
    }

    private final Logger logger = (Logger) LoggerFactory.getLogger(EncryptionBackfillRunner.class);
    private final ListAppender<ILoggingEvent> logged = new ListAppender<>();

    @BeforeEach
    void listen() {
        when(unconfirmedKey.awaitsConfirmation()).thenReturn(true);
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
        verify(migration, never()).migrate(any(), any(), any());
        verifyNoInteractions(vacuum, lock);
        assertThat(logged.list).isEmpty();
    }

    @Test
    void the_key_is_checked_under_the_lock_before_anything_is_sealed() {
        when(migration.pending(titles)).thenReturn(true);
        when(migration.migrate(eq(titles), eq(sealers), any())).thenReturn(4);

        runner.afterSingletonsInstantiated();

        InOrder order = inOrder(lock, finance, migration);
        order.verify(lock).whileHeld(any());
        order.verify(finance).verify();
        order.verify(migration).migrate(eq(titles), eq(sealers), any());
    }

    @Test
    void a_key_that_does_not_decrypt_stops_the_start_before_any_rewrite() {
        when(migration.pending(items)).thenReturn(true);
        doThrow(new IllegalStateException("does not decrypt")).when(finance).verify();

        assertThatThrownBy(runner::afterSingletonsInstantiated).hasMessageContaining("does not decrypt");

        verify(migration, never()).migrate(any(), any(), any());
        verifyNoInteractions(vacuum);
    }

    @Test
    void every_declared_table_is_owed_a_vacuum_and_only_the_rewritten_columns_are_logged() {
        // A start cut short leaves tables sealed then, whose columns in clear 068 has since dropped
        // without rewriting them: their earlier versions are still in their files.
        when(migration.pending(items)).thenReturn(true);
        when(migration.migrate(eq(items), eq(sealers), any())).thenReturn(2);

        runner.afterSingletonsInstantiated();

        verify(pendingVacuum).owe(Set.of("tasks", "shopping_items"));
        assertThat(logged.list).extracting(ILoggingEvent::getFormattedMessage)
            .containsExactly("Sealed 2 values of shopping_items.name_encrypted with the current key");
    }

    @Test
    void work_another_instance_finished_while_this_one_waited_is_not_done_again() {
        when(migration.pending(titles)).thenReturn(true, false);

        runner.afterSingletonsInstantiated();

        verify(migration, never()).migrate(any(), any(), any());
        verifyNoInteractions(vacuum);
    }

    @Test
    void a_key_the_data_has_yet_to_confirm_is_checked_even_with_nothing_left_to_seal_and_confirmed_once_it_opens() {
        runner(unconfirmedKey).afterSingletonsInstantiated();

        InOrder order = inOrder(finance, unconfirmedKey);
        order.verify(finance).verify();
        order.verify(unconfirmedKey).confirm();
        verify(unconfirmedKey, never()).forget();
        verify(migration, never()).migrate(any(), any(), any());
        verifyNoInteractions(vacuum);
    }

    @Test
    void a_key_confirmed_already_is_neither_confirmed_again_nor_taken_back() {
        when(migration.pending(titles)).thenReturn(true);

        runner.afterSingletonsInstantiated();

        verify(knownKey, never()).confirm();
        verify(knownKey, never()).forget();
    }

    @Test
    void an_unconfirmed_key_that_does_not_decrypt_has_its_fingerprint_taken_back_so_the_right_key_can_start_next() {
        doThrow(new IllegalStateException("does not decrypt")).when(finance).verify();

        assertThatThrownBy(runner(unconfirmedKey)::afterSingletonsInstantiated).hasMessageContaining("does not decrypt");

        verify(unconfirmedKey).forget();
        verify(unconfirmedKey, never()).confirm();
    }

    @Test
    void a_known_key_that_does_not_decrypt_keeps_its_fingerprint() {
        when(migration.pending(titles)).thenReturn(true);
        doThrow(new IllegalStateException("does not decrypt")).when(finance).verify();

        assertThatThrownBy(runner::afterSingletonsInstantiated).hasMessageContaining("does not decrypt");

        verify(knownKey, never()).forget();
    }

    @Test
    void the_vacuum_is_owed_before_the_first_row_is_rewritten_and_settled_table_by_table_once_it_ran() {
        when(migration.pending(titles)).thenReturn(true);
        when(pendingVacuum.stillOwed()).thenReturn(List.of("shopping_items", "tasks"));
        when(vacuum.vacuumFull(any())).thenReturn(true);

        runner.afterSingletonsInstantiated();

        InOrder order = inOrder(pendingVacuum, migration, vacuum);
        order.verify(pendingVacuum).owe(Set.of("tasks", "shopping_items"));
        order.verify(migration).migrate(eq(titles), eq(sealers), any());
        order.verify(vacuum).vacuumFull("shopping_items");
        order.verify(pendingVacuum).settle("shopping_items");
        order.verify(vacuum).vacuumFull("tasks");
        order.verify(pendingVacuum).settle("tasks");
    }

    @Test
    void a_vacuum_a_stopped_start_still_owes_is_run_with_nothing_left_to_seal() {
        // A start stopped after its last batch and before its VACUUM: nothing is pending any more.
        when(pendingVacuum.anyOwed()).thenReturn(true);
        when(pendingVacuum.stillOwed()).thenReturn(List.of("tasks"));
        when(vacuum.vacuumFull("tasks")).thenReturn(true);

        runner.afterSingletonsInstantiated();

        InOrder order = inOrder(vacuum, pendingVacuum);
        order.verify(vacuum).vacuumFull("tasks");
        order.verify(pendingVacuum).settle("tasks");
        verify(migration, never()).migrate(any(), any(), any());
        verify(pendingVacuum, never()).owe(any());
    }

    @Test
    void a_start_that_only_owes_a_vacuum_checks_no_key() {
        // Nothing is rewritten and the key is confirmed already: the key check would only cost a scan of every table.
        when(pendingVacuum.anyOwed()).thenReturn(true);
        when(pendingVacuum.stillOwed()).thenReturn(List.of("tasks"));

        runner.afterSingletonsInstantiated();

        verify(finance, never()).verify();
    }

    @Test
    void a_table_the_vacuum_could_not_take_stays_owed_and_the_others_are_settled() {
        when(migration.pending(titles)).thenReturn(true);
        when(pendingVacuum.stillOwed()).thenReturn(List.of("shopping_items", "tasks"));
        when(vacuum.vacuumFull("shopping_items")).thenReturn(false);
        when(vacuum.vacuumFull("tasks")).thenReturn(true);

        runner.afterSingletonsInstantiated();

        verify(pendingVacuum).settle("tasks");
        verify(pendingVacuum, never()).settle("shopping_items");
    }

    @Test
    void a_column_under_a_key_of_its_own_is_owed_a_vacuum_and_re_encrypted_after_the_sealed_ones() {
        RekeyedColumn secrets = RekeyedColumn.of("two_factor_methods", "secret", "user_id", "method = 'APP'", key -> {
            throw new AssertionError("not used");
        });
        EncryptionBackfillRunner withSecrets = new EncryptionBackfillRunner(List.of(SealedColumns.of(titles)),
            List.of(RekeyedColumns.of(secrets)), List.of(finance), migration, rekeying, sealers, legacy, lock, vacuum,
            pendingVacuum, knownKey);
        when(migration.pending(titles)).thenReturn(true);
        when(rekeying.pending(secrets)).thenReturn(true);
        when(rekeying.migrate(secrets)).thenReturn(3);

        withSecrets.afterSingletonsInstantiated();

        InOrder order = inOrder(pendingVacuum, migration, rekeying);
        order.verify(pendingVacuum).owe(Set.of("tasks", "two_factor_methods"));
        order.verify(migration).migrate(eq(titles), eq(sealers), any());
        order.verify(rekeying).migrate(secrets);
        assertThat(logged.list).extracting(ILoggingEvent::getFormattedMessage)
            .contains("Re-encrypted 3 values of two_factor_methods.secret with the current key");
    }

    @Test
    void a_column_under_a_key_of_its_own_alone_pending_is_enough_to_run() {
        RekeyedColumn queue = RekeyedColumn.of("mail_outbox", "payload", "id", null, key -> {
            throw new AssertionError("not used");
        });
        EncryptionBackfillRunner withQueue = new EncryptionBackfillRunner(List.of(SealedColumns.of(titles)),
            List.of(RekeyedColumns.of(queue)), List.of(finance), migration, rekeying, sealers, legacy, lock, vacuum,
            pendingVacuum, knownKey);
        when(rekeying.pending(queue)).thenReturn(true);

        withQueue.afterSingletonsInstantiated();

        verify(finance).verify();
        verify(rekeying).migrate(queue);
    }
}
