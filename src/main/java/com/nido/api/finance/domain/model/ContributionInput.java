package com.nido.api.finance.domain.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * A contributor as submitted by a caller — {@code shareAmount} is null to
 * request an equal split, non-null to fix that contributor's exact share.
 * {@link ContributionSplitter#resolve} turns a list of these into the
 * resolved {@link Contribution} list actually persisted; a list mixing null
 * and non-null shares is rejected there rather than guessed at.
 */
public record ContributionInput(UUID memberId, BigDecimal shareAmount) {
    public ContributionInput {
        Objects.requireNonNull(memberId, "memberId");
    }
}
