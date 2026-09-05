package com.nido.api.finance.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContributionSplitterTest {

    private static final UUID ALICE = UUID.randomUUID();
    private static final UUID BOB = UUID.randomUUID();
    private static final UUID CARL = UUID.randomUUID();

    @Test
    void an_empty_contributor_list_resolves_to_an_empty_list() {
        assertThat(ContributionSplitter.resolve(new BigDecimal("42.00"), List.of())).isEmpty();
    }

    @Test
    void auto_split_divides_evenly_when_the_amount_divides_exactly() {
        List<Contribution> result = ContributionSplitter.resolve(new BigDecimal("30.00"),
            List.of(new ContributionInput(ALICE, null), new ContributionInput(BOB, null), new ContributionInput(CARL, null)));

        assertThat(result).containsExactly(
            new Contribution(ALICE, new BigDecimal("10.00")),
            new Contribution(BOB, new BigDecimal("10.00")),
            new Contribution(CARL, new BigDecimal("10.00")));
    }

    @Test
    void auto_split_distributes_the_leftover_cents_to_the_first_contributors() {
        // 10.00 / 3 = 3.33 with 0.01 left over, given to the first contributor.
        List<Contribution> result = ContributionSplitter.resolve(new BigDecimal("10.00"),
            List.of(new ContributionInput(ALICE, null), new ContributionInput(BOB, null), new ContributionInput(CARL, null)));

        assertThat(result).containsExactly(
            new Contribution(ALICE, new BigDecimal("3.34")),
            new Contribution(BOB, new BigDecimal("3.33")),
            new Contribution(CARL, new BigDecimal("3.33")));
        assertThat(result.stream().map(Contribution::shareAmount).reduce(BigDecimal.ZERO, BigDecimal::add))
            .isEqualByComparingTo("10.00");
    }

    @Test
    void manual_shares_that_sum_to_the_total_are_accepted_as_is() {
        List<Contribution> result = ContributionSplitter.resolve(new BigDecimal("100.00"),
            List.of(new ContributionInput(ALICE, new BigDecimal("70.00")), new ContributionInput(BOB, new BigDecimal("30.00"))));

        assertThat(result).containsExactly(
            new Contribution(ALICE, new BigDecimal("70.00")),
            new Contribution(BOB, new BigDecimal("30.00")));
    }

    @Test
    void manual_shares_that_do_not_sum_to_the_total_are_rejected() {
        assertThatThrownBy(() -> ContributionSplitter.resolve(new BigDecimal("100.00"),
            List.of(new ContributionInput(ALICE, new BigDecimal("70.00")), new ContributionInput(BOB, new BigDecimal("20.00")))))
            .isInstanceOf(FinanceException.InvalidContributionShares.class);
    }

    @Test
    void mixing_automatic_and_manual_shares_in_the_same_request_is_rejected() {
        assertThatThrownBy(() -> ContributionSplitter.resolve(new BigDecimal("100.00"),
            List.of(new ContributionInput(ALICE, new BigDecimal("70.00")), new ContributionInput(BOB, null))))
            .isInstanceOf(FinanceException.InvalidContributionShares.class);
    }
}
