package com.nido.api.finance.domain.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Reading one member's share out of the shares a mail is told. */
final class Shares {

    private Shares() {}

    static BigDecimal of(List<MemberShare> shares, UUID memberId) {
        return shares.stream()
            .filter(share -> share.memberId().equals(memberId))
            .map(MemberShare::amount)
            .findFirst()
            .orElse(null);
    }
}
