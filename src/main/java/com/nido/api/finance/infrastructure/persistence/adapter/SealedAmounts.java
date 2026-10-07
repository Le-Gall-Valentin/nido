package com.nido.api.finance.infrastructure.persistence.adapter;

import com.nido.api.infrastructure.sealing.SealedColumn;
import com.nido.api.infrastructure.sealing.SpaceSealer;

import java.math.BigDecimal;
import java.util.UUID;

/** How finance stores an amount: its plain decimal text, sealed — the one place that turns one into the other. */
final class SealedAmounts {

    private SealedAmounts() {}

    static String seal(SpaceSealer sealer, SealedColumn column, UUID rowId, BigDecimal amount) {
        return sealer.seal(column, rowId, amount.toPlainString());
    }

    static BigDecimal open(SpaceSealer sealer, SealedColumn column, UUID rowId, String stored) {
        return new BigDecimal(sealer.open(column, rowId, stored));
    }
}
