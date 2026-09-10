package com.nido.api.finance.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BalanceCalculatorTest {

    private static final UUID ALICE = UUID.randomUUID();
    private static final UUID BOB = UUID.randomUUID();
    private static final UUID CARL = UUID.randomUUID();

    private static SplitTransaction sharedExpense(UUID payer, BigDecimal amount, List<Contribution> contributors) {
        return new SplitTransaction(payer, amount, contributors, TransactionType.EXPENSE);
    }

    @Test
    void a_transaction_with_no_contributors_does_not_affect_any_balance() {
        // The repository filters these out before they reach here, so this covers the
        // calculator's own guard — the one that keeps a wrong filter from folding nonsense
        // into a figure nobody can eyeball.
        SplitTransaction personalExpense = sharedExpense(ALICE, new BigDecimal("50.00"), List.of());

        Balances balances = BalanceCalculator.calculate(List.of(personalExpense), List.of());

        assertThat(balances.netByMember()).isEmpty();
    }

    @Test
    void the_payer_is_owed_by_each_contributor_for_their_share_when_split_two_ways() {
        SplitTransaction dinner = sharedExpense(ALICE, new BigDecimal("40.00"),
            List.of(new Contribution(ALICE, new BigDecimal("20.00")), new Contribution(BOB, new BigDecimal("20.00"))));

        Balances balances = BalanceCalculator.calculate(List.of(dinner), List.of());

        assertThat(balances.netByMember()).containsExactlyInAnyOrder(
            new MemberBalance(ALICE, new BigDecimal("20.00")), new MemberBalance(BOB, new BigDecimal("-20.00")));
        assertThat(balances.suggestedTransfers()).containsExactly(new SuggestedTransfer(BOB, ALICE, new BigDecimal("20.00")));
    }

    @Test
    void three_members_with_a_custom_split_net_out_correctly() {
        SplitTransaction rent = sharedExpense(ALICE, new BigDecimal("100.00"), List.of(
            new Contribution(ALICE, new BigDecimal("50.00")),
            new Contribution(BOB, new BigDecimal("30.00")),
            new Contribution(CARL, new BigDecimal("20.00"))));

        Balances balances = BalanceCalculator.calculate(List.of(rent), List.of());

        assertThat(balances.netByMember()).containsExactlyInAnyOrder(
            new MemberBalance(ALICE, new BigDecimal("50.00")),
            new MemberBalance(BOB, new BigDecimal("-30.00")),
            new MemberBalance(CARL, new BigDecimal("-20.00")));
    }

    @Test
    void a_settlement_reduces_what_the_payer_of_that_settlement_still_owes() {
        SplitTransaction dinner = sharedExpense(ALICE, new BigDecimal("40.00"),
            List.of(new Contribution(ALICE, new BigDecimal("20.00")), new Contribution(BOB, new BigDecimal("20.00"))));
        SettlementRecord settlement = new SettlementRecord(UUID.randomUUID(), UUID.randomUUID(), BOB, ALICE,
            new BigDecimal("20.00"), LocalDate.of(2026, 1, 2));

        Balances balances = BalanceCalculator.calculate(List.of(dinner), List.of(settlement));

        assertThat(balances.netByMember()).containsExactlyInAnyOrder(
            new MemberBalance(ALICE, BigDecimal.ZERO.setScale(2)), new MemberBalance(BOB, BigDecimal.ZERO.setScale(2)));
        assertThat(balances.suggestedTransfers()).isEmpty();
    }

    @Test
    void suggested_transfers_settle_every_member_to_zero_with_the_fewest_transfers() {
        // Alice paid 90 alone for a 3-way split of 30 each: Bob and Carl each owe Alice 30.
        SplitTransaction outing = sharedExpense(ALICE, new BigDecimal("90.00"), List.of(
            new Contribution(ALICE, new BigDecimal("30.00")),
            new Contribution(BOB, new BigDecimal("30.00")),
            new Contribution(CARL, new BigDecimal("30.00"))));

        Balances balances = BalanceCalculator.calculate(List.of(outing), List.of());

        assertThat(balances.suggestedTransfers()).containsExactlyInAnyOrder(
            new SuggestedTransfer(BOB, ALICE, new BigDecimal("30.00")),
            new SuggestedTransfer(CARL, ALICE, new BigDecimal("30.00")));
    }
    // ─── revenus partagés ──────────────────────────────────────────────────

    private static SplitTransaction sharedIncome(UUID receiver, BigDecimal amount, List<Contribution> beneficiaries) {
        return new SplitTransaction(receiver, amount, beneficiaries, TransactionType.INCOME);
    }

    @Test
    void a_shared_income_leaves_its_receiver_owing_the_others_their_share() {
        // Alice receives a 300 tax refund belonging to the household, split evenly. She is
        // holding 150 that is Bob's, so she owes him — the mirror image of an expense she
        // fronted. Folded as an expense, the app told Bob to pay Alice 150 instead.
        SplitTransaction refund = sharedIncome(ALICE, new BigDecimal("300.00"),
            List.of(new Contribution(ALICE, new BigDecimal("150.00")),
                    new Contribution(BOB, new BigDecimal("150.00"))));

        Balances balances = BalanceCalculator.calculate(List.of(refund), List.of());

        assertThat(balances.netByMember()).containsExactlyInAnyOrder(
            new MemberBalance(ALICE, new BigDecimal("-150.00")),
            new MemberBalance(BOB, new BigDecimal("150.00")));
        assertThat(balances.suggestedTransfers()).containsExactly(
            new SuggestedTransfer(ALICE, BOB, new BigDecimal("150.00")));
    }

    @Test
    void an_income_its_receiver_keeps_entirely_moves_no_one() {
        SplitTransaction salary = sharedIncome(ALICE, new BigDecimal("2000.00"),
            List.of(new Contribution(ALICE, new BigDecimal("2000.00"))));

        Balances balances = BalanceCalculator.calculate(List.of(salary), List.of());

        assertThat(balances.netByMember()).containsExactly(
            new MemberBalance(ALICE, BigDecimal.ZERO.setScale(2)));
        assertThat(balances.suggestedTransfers()).isEmpty();
    }

    @Test
    void a_shared_income_cancels_an_expense_of_the_same_shape() {
        // Alice fronts 100 that Bob half-owes, then receives 100 that Bob half-owns. The two
        // cancel — which only holds if they are folded in opposite directions.
        SplitTransaction expense = sharedExpense(ALICE, new BigDecimal("100.00"),
            List.of(new Contribution(ALICE, new BigDecimal("50.00")),
                    new Contribution(BOB, new BigDecimal("50.00"))));
        SplitTransaction income = sharedIncome(ALICE, new BigDecimal("100.00"),
            List.of(new Contribution(ALICE, new BigDecimal("50.00")),
                    new Contribution(BOB, new BigDecimal("50.00"))));

        Balances balances = BalanceCalculator.calculate(List.of(expense, income), List.of());

        assertThat(balances.netByMember()).containsExactlyInAnyOrder(
            new MemberBalance(ALICE, BigDecimal.ZERO.setScale(2)),
            new MemberBalance(BOB, BigDecimal.ZERO.setScale(2)));
        assertThat(balances.suggestedTransfers()).isEmpty();
    }

    @Test
    void a_settlement_offsets_a_shared_income_the_same_way_it_offsets_an_expense() {
        SplitTransaction refund = sharedIncome(ALICE, new BigDecimal("300.00"),
            List.of(new Contribution(ALICE, new BigDecimal("150.00")),
                    new Contribution(BOB, new BigDecimal("150.00"))));
        SettlementRecord alicePaysBob = new SettlementRecord(UUID.randomUUID(), UUID.randomUUID(), ALICE, BOB,
            new BigDecimal("150.00"), LocalDate.of(2026, 1, 2));

        Balances balances = BalanceCalculator.calculate(List.of(refund), List.of(alicePaysBob));

        assertThat(balances.netByMember()).containsExactlyInAnyOrder(
            new MemberBalance(ALICE, BigDecimal.ZERO.setScale(2)),
            new MemberBalance(BOB, BigDecimal.ZERO.setScale(2)));
    }
}
