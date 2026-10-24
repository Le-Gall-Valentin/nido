package com.nido.api.finance.domain.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * A member's part of an amount, as a mail shows it.
 *
 * @param memberName null for someone who can no longer be named — a former member of the space, or an anonymised
 *                   account: the mail calls them a former member
 */
public record MemberShare(UUID memberId, String memberName, BigDecimal amount) {
    public MemberShare {
        Objects.requireNonNull(memberId, "memberId");
        Objects.requireNonNull(amount, "amount");
    }
}
