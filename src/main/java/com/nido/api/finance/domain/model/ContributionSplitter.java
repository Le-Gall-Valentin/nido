package com.nido.api.finance.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns the contributor list a caller submits into the fixed shares actually
 * persisted. Every {@link ContributionInput#shareAmount()} in the list must
 * be null (request an equal split) or every one must be non-null (request a
 * custom split) — a request mixing both is rejected rather than guessed at,
 * since there is no sensible default for "some explicit, some not".
 */
public final class ContributionSplitter {

    private ContributionSplitter() {}

    public static List<Contribution> resolve(BigDecimal amount, List<ContributionInput> inputs) {
        if (inputs.isEmpty()) {
            return List.of();
        }
        boolean allAuto = inputs.stream().allMatch(i -> i.shareAmount() == null);
        boolean allManual = inputs.stream().allMatch(i -> i.shareAmount() != null);
        if (allAuto) {
            return splitEqually(amount, inputs);
        }
        if (!allManual) {
            throw new FinanceException.InvalidContributionShares();
        }
        List<Contribution> resolved = inputs.stream().map(i -> new Contribution(i.memberId(), i.shareAmount())).toList();
        BigDecimal sum = resolved.stream().map(Contribution::shareAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.compareTo(amount) != 0) {
            throw new FinanceException.InvalidContributionShares();
        }
        return resolved;
    }

    private static List<Contribution> splitEqually(BigDecimal amount, List<ContributionInput> inputs) {
        int n = inputs.size();
        BigDecimal base = amount.divide(BigDecimal.valueOf(n), 2, RoundingMode.FLOOR);
        BigDecimal remainder = amount.subtract(base.multiply(BigDecimal.valueOf(n)));
        int remainderCents = remainder.movePointRight(2).intValueExact();
        List<Contribution> result = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            BigDecimal share = i < remainderCents ? base.add(new BigDecimal("0.01")) : base;
            result.add(new Contribution(inputs.get(i).memberId(), share));
        }
        return result;
    }
}
