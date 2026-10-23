package com.nido.api.finance.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** Who an operation concerns, and whether a change to it changes anything a member sees. */
class OperationChangeTest {

    private static final UUID SPACE = UUID.randomUUID();
    private static final UUID ALICE = UUID.randomUUID();
    private static final UUID BOB = UUID.randomUUID();
    private static final UUID CAROL = UUID.randomUUID();
    private static final UUID GROCERIES = UUID.randomUUID();
    private static final UUID ID = UUID.randomUUID();

    private static Transaction groceries(String amount, UUID payer, List<Contribution> shares) {
        return new Transaction(ID, SPACE, "Courses", new BigDecimal(amount), TransactionType.EXPENSE, GROCERIES,
            LocalDate.of(2026, 10, 21), payer, shares, null, Instant.parse("2026-10-21T10:00:00Z"));
    }

    private static Transaction shared() {
        return groceries("20.00", ALICE, List.of(new Contribution(ALICE, new BigDecimal("10.00")),
            new Contribution(BOB, new BigDecimal("10.00"))));
    }

    private static Transaction with(Transaction t, UnaryOperator<Transaction> change) {
        return change.apply(t);
    }

    @Test
    void the_same_content_is_no_change() {
        assertThat(shared().differsFrom(shared())).isFalse();
    }

    @Test
    void amounts_of_another_scale_are_not_a_change() {
        Transaction coarse = groceries("20.0", ALICE, List.of(new Contribution(ALICE, new BigDecimal("10.0")),
            new Contribution(BOB, new BigDecimal("10"))));

        assertThat(coarse.differsFrom(shared())).isFalse();
    }

    static Stream<Arguments> visibleFields() {
        return Stream.of(
            Arguments.of("label", (UnaryOperator<Transaction>) t -> new Transaction(t.id(), t.spaceId(), "Marché",
                t.amount(), t.type(), t.categoryId(), t.date(), t.payerId(), t.contributors(), null, t.createdAt())),
            Arguments.of("amount", (UnaryOperator<Transaction>) t -> new Transaction(t.id(), t.spaceId(), t.label(),
                new BigDecimal("20.01"), t.type(), t.categoryId(), t.date(), t.payerId(), t.contributors(), null, t.createdAt())),
            Arguments.of("type", (UnaryOperator<Transaction>) t -> new Transaction(t.id(), t.spaceId(), t.label(),
                t.amount(), TransactionType.INCOME, t.categoryId(), t.date(), t.payerId(), t.contributors(), null, t.createdAt())),
            Arguments.of("category", (UnaryOperator<Transaction>) t -> new Transaction(t.id(), t.spaceId(), t.label(),
                t.amount(), t.type(), UUID.randomUUID(), t.date(), t.payerId(), t.contributors(), null, t.createdAt())),
            Arguments.of("date", (UnaryOperator<Transaction>) t -> new Transaction(t.id(), t.spaceId(), t.label(),
                t.amount(), t.type(), t.categoryId(), t.date().plusDays(1), t.payerId(), t.contributors(), null, t.createdAt())),
            Arguments.of("payer", (UnaryOperator<Transaction>) t -> new Transaction(t.id(), t.spaceId(), t.label(),
                t.amount(), t.type(), t.categoryId(), t.date(), BOB, t.contributors(), null, t.createdAt())));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("visibleFields")
    void each_visible_field_is_a_change(String field, UnaryOperator<Transaction> change) {
        assertThat(with(shared(), change).differsFrom(shared())).isTrue();
    }

    @Test
    void a_new_split_with_the_same_total_is_a_change() {
        Transaction resplit = groceries("20.00", ALICE, List.of(new Contribution(ALICE, new BigDecimal("15.00")),
            new Contribution(BOB, new BigDecimal("5.00"))));

        assertThat(resplit.differsFrom(shared())).isTrue();
    }

    @Test
    void another_contributor_is_a_change() {
        Transaction withCarol = groceries("20.00", ALICE, List.of(new Contribution(ALICE, new BigDecimal("10.00")),
            new Contribution(CAROL, new BigDecimal("10.00"))));

        assertThat(withCarol.differsFrom(shared())).isTrue();
    }

    @Test
    void shares_in_another_order_are_no_change() {
        Transaction reordered = groceries("20.00", ALICE, List.of(new Contribution(BOB, new BigDecimal("10.00")),
            new Contribution(ALICE, new BigDecimal("10.00"))));

        assertThat(reordered.differsFrom(shared())).isFalse();
    }

    @Test
    void the_payer_and_the_contributors_are_concerned_once_each() {
        assertThat(shared().concernedMembers()).containsExactly(ALICE, BOB);
    }

    @Test
    void a_payer_without_a_split_is_concerned() {
        assertThat(groceries("20.00", BOB, List.of()).concernedMembers()).containsExactly(BOB);
    }

    @Test
    void nobody_is_concerned_by_an_unshared_operation() {
        assertThat(groceries("20.00", null, List.of()).concernedMembers()).isEmpty();
    }
}
