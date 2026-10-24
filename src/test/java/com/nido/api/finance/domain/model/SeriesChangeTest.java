package com.nido.api.finance.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** Who a recurring transaction concerns, and whether a change to it changes anything a member sees. */
class SeriesChangeTest {

    private static final UUID SPACE = UUID.randomUUID();
    private static final UUID ALICE = UUID.randomUUID();
    private static final UUID BOB = UUID.randomUUID();
    private static final UUID HOUSING = UUID.randomUUID();
    private static final UUID ID = UUID.randomUUID();

    private static RecurringTransactionSeries rent(String amount, String aliceShare, String bobShare) {
        return new RecurringTransactionSeries(ID, SPACE, "Loyer", new BigDecimal(amount), TransactionType.EXPENSE, HOUSING,
            ALICE, List.of(new Contribution(ALICE, new BigDecimal(aliceShare)), new Contribution(BOB, new BigDecimal(bobShare))),
            RecurrenceInterval.MONTHLY, 1, LocalDate.of(2026, 11, 1), null, null);
    }

    private static RecurringTransactionSeries rent() {
        return rent("1200.00", "600.00", "600.00");
    }

    @Test
    void the_same_content_is_no_change() {
        assertThat(rent().differsFrom(rent())).isFalse();
    }

    @Test
    void amounts_of_another_scale_are_not_a_change() {
        assertThat(rent("1200", "600.0", "600").differsFrom(rent())).isFalse();
    }

    @Test
    void a_new_split_with_the_same_total_is_a_change() {
        assertThat(rent("1200.00", "800.00", "400.00").differsFrom(rent())).isTrue();
    }

    static Stream<Arguments> visibleFields() {
        return Stream.of(
            Arguments.of("label", (UnaryOperator<RecurringTransactionSeries>) s -> copy(s, "Loyer et charges", s.amount(),
                s.payerId(), s.intervalType(), s.intervalCount(), s.anchorDate(), s.endDate(), s.lastMaterializedDate())),
            Arguments.of("amount", (UnaryOperator<RecurringTransactionSeries>) s -> copy(s, s.label(), new BigDecimal("1250.00"),
                s.payerId(), s.intervalType(), s.intervalCount(), s.anchorDate(), s.endDate(), s.lastMaterializedDate())),
            Arguments.of("payer", (UnaryOperator<RecurringTransactionSeries>) s -> copy(s, s.label(), s.amount(),
                BOB, s.intervalType(), s.intervalCount(), s.anchorDate(), s.endDate(), s.lastMaterializedDate())),
            Arguments.of("interval", (UnaryOperator<RecurringTransactionSeries>) s -> copy(s, s.label(), s.amount(),
                s.payerId(), RecurrenceInterval.WEEKLY, s.intervalCount(), s.anchorDate(), s.endDate(), s.lastMaterializedDate())),
            Arguments.of("interval count", (UnaryOperator<RecurringTransactionSeries>) s -> copy(s, s.label(), s.amount(),
                s.payerId(), s.intervalType(), 2, s.anchorDate(), s.endDate(), s.lastMaterializedDate())),
            Arguments.of("start", (UnaryOperator<RecurringTransactionSeries>) s -> copy(s, s.label(), s.amount(),
                s.payerId(), s.intervalType(), s.intervalCount(), LocalDate.of(2026, 12, 1), s.endDate(), s.lastMaterializedDate())),
            Arguments.of("end", (UnaryOperator<RecurringTransactionSeries>) s -> copy(s, s.label(), s.amount(),
                s.payerId(), s.intervalType(), s.intervalCount(), s.anchorDate(), LocalDate.of(2027, 6, 1), s.lastMaterializedDate())));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("visibleFields")
    void each_visible_field_is_a_change(String field, UnaryOperator<RecurringTransactionSeries> change) {
        assertThat(change.apply(rent()).differsFrom(rent())).isTrue();
    }

    @Test
    void materialization_progress_is_no_change() {
        RecurringTransactionSeries caughtUp = copy(rent(), "Loyer", rent().amount(), ALICE, RecurrenceInterval.MONTHLY, 1,
            LocalDate.of(2026, 11, 1), null, LocalDate.of(2026, 11, 1));

        assertThat(caughtUp.differsFrom(rent())).isFalse();
    }

    @Test
    void the_payer_and_the_contributors_are_concerned_once_each() {
        assertThat(rent().concernedMembers()).containsExactly(ALICE, BOB);
    }

    private static RecurringTransactionSeries copy(RecurringTransactionSeries s, String label, BigDecimal amount, UUID payer,
                                                   RecurrenceInterval interval, int count, LocalDate anchor, LocalDate end,
                                                   LocalDate lastMaterialized) {
        return new RecurringTransactionSeries(s.id(), s.spaceId(), label, amount, s.type(), s.categoryId(), payer,
            s.contributors(), interval, count, anchor, end, lastMaterialized);
    }
}
