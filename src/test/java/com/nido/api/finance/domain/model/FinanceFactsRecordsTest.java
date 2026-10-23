package com.nido.api.finance.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** What a finance mail is told, and the little it works out for itself. */
class FinanceFactsRecordsTest {

    private static final UUID ALICE = UUID.randomUUID();
    private static final UUID BOB = UUID.randomUUID();
    private static final UUID CAROL = UUID.randomUUID();

    private static final OperationFacts GROCERIES = new OperationFacts("Courses", new BigDecimal("86.40"),
        TransactionType.EXPENSE, "Alimentation", LocalDate.of(2026, 10, 21), ALICE, "alice",
        List.of(new MemberShare(ALICE, "alice", new BigDecimal("43.20")), new MemberShare(BOB, "bob", new BigDecimal("43.20"))));

    @Test
    void an_operation_knows_each_member_s_share_and_who_paid() {
        assertThat(GROCERIES.shareOf(BOB)).isEqualByComparingTo("43.20");
        assertThat(GROCERIES.shareOf(CAROL)).isNull();
        assertThat(GROCERIES.paidBy(ALICE)).isTrue();
        assertThat(GROCERIES.paidBy(BOB)).isFalse();
    }

    @Test
    void a_series_knows_each_member_s_share_and_who_paid() {
        SeriesFacts rent = new SeriesFacts("Loyer", new BigDecimal("1200.00"), TransactionType.EXPENSE, "Logement", BOB, "bob",
            List.of(new MemberShare(ALICE, "alice", new BigDecimal("600.00"))), RecurrenceInterval.MONTHLY, 1,
            LocalDate.of(2026, 11, 1), null);

        assertThat(rent.shareOf(ALICE)).isEqualByComparingTo("600.00");
        assertThat(rent.shareOf(BOB)).isNull();
        assertThat(rent.paidBy(BOB)).isTrue();
    }

    @Test
    void a_deposit_says_how_far_the_goal_is_rounding_down() {
        assertThat(deposit("700.00", "1000.00").percent()).isEqualTo(70);
        assertThat(deposit("100.00", "300.00").percent()).isEqualTo(33);
        assertThat(deposit("1000.00", "1000.00").percent()).isEqualTo(100);
    }

    @Test
    void a_deposit_that_brings_the_total_to_the_target_reaches_the_goal() {
        assertThat(deposit("1000.00", "1000.00").reachesGoal()).isTrue();
        assertThat(deposit("999.99", "1000.00").reachesGoal()).isFalse();
    }

    @Test
    void a_budget_alert_says_how_much_is_spent_rounding_down() {
        assertThat(alert("415.00", "500.00").percent()).isEqualTo(83);
        assertThat(alert("520.00", "500.00").percent()).isEqualTo(104);
        assertThat(alert("0.01", "0.00").percent()).isEqualTo(100);
    }

    @Test
    void involvement_follows_where_the_reader_stands_before_and_after() {
        assertThat(Involvement.of(true, true)).isEqualTo(Involvement.CHANGED);
        assertThat(Involvement.of(false, true)).isEqualTo(Involvement.ADDED);
        assertThat(Involvement.of(true, false)).isEqualTo(Involvement.REMOVED);
        assertThatThrownBy(() -> Involvement.of(false, false)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void goal_facts_are_read_from_the_goal() {
        SavingsGoal goal = new SavingsGoal(UUID.randomUUID(), UUID.randomUUID(), "Vacances", new BigDecimal("1000.00"),
            LocalDate.of(2027, 7, 1), "#c17a5c", "🏖️");

        assertThat(GoalFacts.of(goal)).isEqualTo(new GoalFacts("Vacances", new BigDecimal("1000.00"), LocalDate.of(2027, 7, 1)));
    }

    private static SavingsDeposit deposit(String saved, String target) {
        return new SavingsDeposit(new GoalFacts("Vacances", new BigDecimal(target), null), ALICE, "alice",
            new BigDecimal("100.00"), LocalDate.of(2026, 10, 21), new BigDecimal(saved), List.of());
    }

    private static BudgetAlert alert(String spent, String limit) {
        return new BudgetAlert("Alimentation", YearMonth.of(2026, 10), new BigDecimal(spent), new BigDecimal(limit),
            "Courses", new BigDecimal("35.00"));
    }
}
