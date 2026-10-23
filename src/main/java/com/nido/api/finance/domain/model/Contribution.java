package com.nido.api.finance.domain.model;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SequencedSet;
import java.util.UUID;
import java.util.stream.Collectors;

/** A contributor's resolved, persisted share of a transaction or series — always a fixed amount, never null. */
public record Contribution(UUID memberId, BigDecimal shareAmount) {
    public Contribution {
        Objects.requireNonNull(memberId, "memberId");
        Objects.requireNonNull(shareAmount, "shareAmount");
    }

    /**
     * Whether two lists hold the same shares: the same members, each owing the same amount — {@code 10.0} and
     * {@code 10.00} alike, whatever the order.
     */
    public static boolean sameShares(List<Contribution> a, List<Contribution> b) {
        if (a.size() != b.size()) {
            return false;
        }
        Map<UUID, BigDecimal> owed = b.stream().collect(Collectors.toMap(Contribution::memberId, Contribution::shareAmount));
        return a.stream().allMatch(share -> {
            BigDecimal other = owed.get(share.memberId());
            return other != null && other.compareTo(share.shareAmount()) == 0;
        });
    }

    /** Whom an amount handled by {@code payerId} and shared by {@code contributors} concerns: the payer, then the others. */
    static SequencedSet<UUID> concerned(UUID payerId, Collection<Contribution> contributors) {
        SequencedSet<UUID> members = new LinkedHashSet<>();
        if (payerId != null) {
            members.add(payerId);
        }
        contributors.forEach(share -> members.add(share.memberId()));
        return members;
    }
}
